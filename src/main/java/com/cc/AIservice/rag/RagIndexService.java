package com.cc.AIservice.rag;

import com.cc.config.RagConfig;
import com.cc.entity.CommunityPost;
import com.cc.entity.KnowledgeArticle;
import com.cc.service.CommunityService;
import com.cc.service.KnowledgeArticleService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 向量索引（SimpleVectorStore，内存索引 + 本地 JSON 持久化）
 *
 * <p>索引来源与可见性口径一致：仅已发布文章（status=1）与正常帖子（status=0 且非危机帖），
 * 且只索引标题与正文纯文本，不写入作者、图片等字段——索引是"影子数据"，泄露风险面最小。</p>
 *
 * <p>检索侧：索引未构建/文件缺失/embedding 不可用时 {@link #search} 返回空列表，
 * 调用方回退关键词检索，保证 AI 对话永不因检索失败而中断。</p>
 *
 * <p>ponytail: 定长滑动窗口切分 + 全量重建，毕设规模（≤1000 文档）够用；
 * 数据量或更新频率上来后改成增量更新 + 语义切分。</p>
 *
 * @author cc
 * @date 2026-09-27
 */
@Slf4j
@Service
public class RagIndexService {

    /** 元数据键：来源（knowledge/community） */
    public static final String META_SOURCE = "source";
    /** 元数据键：业务主键 */
    public static final String META_ID = "id";

    /** 切分窗口（字符） */
    private static final int CHUNK_SIZE = 500;
    /** 相邻窗口重叠（字符），避免句子被切断后语义丢失 */
    private static final int CHUNK_OVERLAP = 50;
    /** 建索引的文档上限（文章/帖子各一），防止一次性灌爆内存与 embedding 额度 */
    private static final int MAX_ARTICLES = 500;
    private static final int MAX_POSTS = 500;
    /** 正文/摘要入索引的截断长度（标题另按 200 截断），防止单篇超长内容灌爆索引与 embedding 额度 */
    private static final int BODY_TEXT_MAX = 3000;
    /** 标题入索引的截断长度 */
    private static final int TITLE_MAX = 200;
    /** 相似度阈值，低于该值的召回视为不相关 */
    private static final double SIMILARITY_THRESHOLD = 0.35;
    /** 文档数未知（索引从文件加载后无法反查） */
    private static final int UNKNOWN_DOCUMENTS = -1;

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private RagConfig ragConfig;

    @Resource
    private KnowledgeArticleService knowledgeArticleService;

    @Resource
    private CommunityService communityService;

    /** 索引快照；null 表示尚未构建/加载，此时向量检索不可用 */
    private volatile SimpleVectorStore vectorStore;
    private volatile LocalDateTime lastBuiltAt;
    private volatile int documentCount;

    /**
     * 向量检索（只返回命中文档的 source/id/title 元数据）
     *
     * <p>只读取 {@code source}/{@code id} 元数据，不读取正文；索引不可用时返回空列表，由调用方降级。</p>
     *
     * @return 命中文档；索引不可用时返回空列表，由调用方降级
     */
    public List<Document> search(String query, int topK) {
        SimpleVectorStore store = ensureLoaded();
        if (store == null || topK <= 0 || !StringUtils.hasText(query)) {
            return List.of();
        }
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(SIMILARITY_THRESHOLD)
                .build();
        return store.similaritySearch(request);
    }

    /**
     * 全量重建索引并落盘（管理端触发）
     *
     * @return {documents, costMs, indexFile, builtAt}
     */
    public Map<String, Object> rebuild() {
        long start = System.currentTimeMillis();
        List<Document> documents = new ArrayList<>();
        documents.addAll(articleDocuments());
        documents.addAll(postDocuments());

        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();
        if (!documents.isEmpty()) {
            store.add(documents);
        }

        String indexFile = ragConfig.getIndexFile();
        if (save(store, indexFile)) {
            this.vectorStore = store;
        }
        this.documentCount = documents.size();
        this.lastBuiltAt = LocalDateTime.now();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("documents", documents.size());
        result.put("costMs", System.currentTimeMillis() - start);
        result.put("indexFile", indexFile);
        result.put("builtAt", lastBuiltAt);
        log.info("RAG 索引重建完成: documents={}, costMs={}", documents.size(), result.get("costMs"));
        return result;
    }

    /** 索引状态（管理端可观测） */
    public Map<String, Object> status() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("indexBuilt", vectorStore != null);
        status.put("documents", documentCount == UNKNOWN_DOCUMENTS ? null : documentCount);
        status.put("lastBuiltAt", lastBuiltAt);
        status.put("indexFile", ragConfig.getIndexFile());
        status.put("fileExists", new File(ragConfig.getIndexFile()).isFile());
        return status;
    }

    // ==================== 内部实现 ====================

    /** 首次检索时尝试从磁盘加载索引；文件不存在则保持不可用状态（不自动重建，避免对话路径产生 embedding 费用） */
    private SimpleVectorStore ensureLoaded() {
        if (vectorStore != null) {
            return vectorStore;
        }
        synchronized (this) {
            if (vectorStore != null) {
                return vectorStore;
            }
            File file = new File(ragConfig.getIndexFile());
            if (!file.isFile()) {
                return null;
            }
            try {
                SimpleVectorStore loaded = SimpleVectorStore.builder(embeddingModel).build();
                loaded.load(file);
                this.vectorStore = loaded;
                // 从文件加载的索引无法反查文档数，标记为未知（UNKNOWN_DOCUMENTS），由 status() 输出 null
                this.documentCount = UNKNOWN_DOCUMENTS;
                log.info("RAG 索引已从文件加载: {}", file.getAbsolutePath());
                return loaded;
            } catch (Exception e) {
                log.warn("RAG 索引加载失败，向量检索降级为关键词检索: {}", e.getMessage());
                return null;
            }
        }
    }

    private boolean save(SimpleVectorStore store, String indexFile) {
        try {
            File file = new File(indexFile);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                log.warn("RAG 索引目录创建失败: {}", parent.getAbsolutePath());
            }
            store.save(file);
            return true;
        } catch (Exception e) {
            // 落盘失败不影响本次会话内检索（内存索引仍可用）
            log.warn("RAG 索引落盘失败（内存索引仍可用）: {}", e.getMessage());
            return true;
        }
    }

    private List<Document> articleDocuments() {
        List<Document> documents = new ArrayList<>();
        for (KnowledgeArticle article : knowledgeArticleService.listAllPublished(MAX_ARTICLES)) {
            if (article == null || !StringUtils.hasText(article.getId())) {
                continue;
            }
            String text = RagSearchService.cleanText(article.getTitle(), TITLE_MAX) + "。"
                    + RagSearchService.cleanText(article.getSummary(), BODY_TEXT_MAX) + " "
                    + RagSearchService.cleanText(article.getContent(), BODY_TEXT_MAX);
            for (String chunk : chunkText(text, CHUNK_SIZE, CHUNK_OVERLAP)) {
                documents.add(document(RagSearchService.SOURCE_KNOWLEDGE, article.getId(), chunk));
            }
        }
        return documents;
    }

    private List<Document> postDocuments() {
        List<Document> documents = new ArrayList<>();
        for (CommunityPost post : communityService.listAllNormalForIndex(MAX_POSTS)) {
            if (post == null || post.getId() == null) {
                continue;
            }
            String text = RagSearchService.cleanText(post.getTitle(), TITLE_MAX) + "。"
                    + RagSearchService.cleanText(post.getContent(), BODY_TEXT_MAX);
            for (String chunk : chunkText(text, CHUNK_SIZE, CHUNK_OVERLAP)) {
                documents.add(document(RagSearchService.SOURCE_COMMUNITY, String.valueOf(post.getId()), chunk));
            }
        }
        return documents;
    }

    private static Document document(String source, String id, String chunk) {
        return Document.builder()
                .text(chunk)
                .metadata(Map.of(META_SOURCE, source, META_ID, id))
                .build();
    }

    /**
     * 定长滑动窗口切分：相邻窗口重叠 {@code overlap} 字符，保证不丢内容
     *
     * @param text      原文（空白已压缩）
     * @param chunkSize 窗口大小
     * @param overlap   重叠字符数（须小于 chunkSize）
     */
    static List<String> chunkText(String text, int chunkSize, int overlap) {
        List<String> chunks = new ArrayList<>();
        if (!StringUtils.hasText(text) || chunkSize <= 0) {
            return chunks;
        }
        String normalized = text.replaceAll("\\s+", " ").trim();
        int step = Math.max(1, chunkSize - Math.max(0, overlap));
        for (int start = 0; start < normalized.length(); start += step) {
            int end = Math.min(start + chunkSize, normalized.length());
            chunks.add(normalized.substring(start, end));
            if (end >= normalized.length()) {
                break;
            }
        }
        return chunks;
    }
}
