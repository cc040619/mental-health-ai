package com.cc.AIservice.rag;

import cn.hutool.http.HtmlUtil;
import com.cc.dto.response.RagSearchResultDTO;
import com.cc.entity.CommunityPost;
import com.cc.entity.KnowledgeArticle;
import com.cc.exception.BusinessException;
import com.cc.service.CommunityService;
import com.cc.service.KnowledgeArticleService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 站内资料检索：AI 对话工具、{@code /api/rag/search} 与 MCP 工具的唯一实现
 *
 * <p>可见性口径与前台列表一致：知识库仅 status=1（已发布，管理员同样只看已发布）、社区仅 status=0（正常）且排除危机帖。
 * 出参只保留白名单字段（标题/纯文本摘要/站内链接/时间），不返回正文、作者与图片；匿名帖天然不暴露身份。</p>
 *
 * <p>链接一律由服务端按 ID 正则白名单拼装，非法 ID 直接丢弃，模型无法通过伪造 ID 产生站外伪链接。</p>
 *
 * <p>检索档位：先走向量语义召回（{@link RagIndexService}），不足再用关键词召回补齐并去重；
 * 索引缺失/embedding 不可用时向量分支返回空，自动退化为纯关键词检索，对话不因检索失败中断。</p>
 *
 * @author cc
 * @date 2026-09-27
 */
@Slf4j
@Service
public class RagSearchService {

    /** 检索类型：知识库文章 */
    public static final String TYPE_ARTICLE = "article";
    /** 检索类型：社区帖子 */
    public static final String TYPE_POST = "post";
    /** 检索类型：两者都查 */
    public static final String TYPE_ALL = "all";

    /** 单次返回条数上限（工具与接口共用，防止一次灌入过多上下文） */
    public static final int MAX_TOP_K = 5;

    public static final String SOURCE_KNOWLEDGE = "knowledge";
    public static final String SOURCE_COMMUNITY = "community";

    private static final int DEFAULT_TOP_K = 3;
    private static final int TITLE_MAX = 200;
    private static final int SUMMARY_MAX = 120;
    private static final int MAX_KEYWORD_LENGTH = 50;
    /** 取回窗口 = topK × 该倍数，留给可见性过滤与去重，保证过滤后仍能凑够 topK */
    private static final int WINDOW_FACTOR = 3;

    /** 文章ID白名单（UUID） */
    private static final Pattern ARTICLE_ID_PATTERN =
            Pattern.compile("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}");
    /** 帖子ID白名单（纯数字，≤18 位避免 Long 溢出） */
    private static final Pattern POST_ID_PATTERN = Pattern.compile("\\d{1,18}");
    /** 控制字符（含换行/制表）：关键词与文本统一剔除，兼作提示词注入的最小清洗 */
    private static final Pattern CONTROL_CHARS = Pattern.compile("[\\p{Cntrl}]");

    @Resource
    private KnowledgeArticleService knowledgeArticleService;

    @Resource
    private CommunityService communityService;

    @Resource
    private RagIndexService ragIndexService;

    /**
     * 检索站内资料（身份不参与过滤：文章与帖子均为站内公开内容，匿名字段本就不出参）
     *
     * @param keyword 关键词，1-50 字，必填
     * @param type    article / post / all，非法值按 all
     * @param topK    返回条数 1-5，空取 3
     * @return 白名单字段结果；无命中返回空列表
     */
    public List<RagSearchResultDTO> search(String keyword, String type, Integer topK) {
        String kw = normalizeKeyword(keyword);
        int limit = normalizeTopK(topK);
        String target = normalizeType(type);

        List<RagSearchResultDTO> semantic = semanticResults(kw, target, limit);
        if (semantic.size() >= limit) {
            return semantic;
        }
        return merge(semantic, keywordResults(kw, target, limit), limit);
    }

    // ==================== 向量召回 ====================

    /**
     * 语义召回：命中 ID 白名单过滤 → 按 ID 回查复核可见性（索引可能落后于库）→ 裁剪出参
     */
    private List<RagSearchResultDTO> semanticResults(String keyword, String target, int limit) {
        List<Document> hits = ragIndexService.search(keyword, limit * WINDOW_FACTOR);
        if (hits.isEmpty()) {
            return List.of();
        }
        List<Map.Entry<String, String>> ordered = new ArrayList<>();
        List<String> articleIds = new ArrayList<>();
        List<String> postIds = new ArrayList<>();
        for (Document hit : hits) {
            String source = String.valueOf(hit.getMetadata().get(RagIndexService.META_SOURCE));
            String id = String.valueOf(hit.getMetadata().get(RagIndexService.META_ID));
            if (SOURCE_KNOWLEDGE.equals(source) && includes(target, TYPE_ARTICLE) && isValidId(id, ARTICLE_ID_PATTERN)) {
                ordered.add(Map.entry(SOURCE_KNOWLEDGE, id));
                articleIds.add(id);
            } else if (SOURCE_COMMUNITY.equals(source) && includes(target, TYPE_POST) && isValidId(id, POST_ID_PATTERN)) {
                ordered.add(Map.entry(SOURCE_COMMUNITY, id));
                postIds.add(id);
            }
        }
        if (ordered.isEmpty()) {
            return List.of();
        }
        Map<String, KnowledgeArticle> articles = new LinkedHashMap<>();
        for (KnowledgeArticle article : knowledgeArticleService.listPublishedByIds(articleIds)) {
            articles.put(article.getId(), article);
        }
        Map<String, CommunityPost> posts = new LinkedHashMap<>();
        for (CommunityPost post : communityService.listNormalByIds(postIds.stream().map(Long::valueOf).toList())) {
            posts.put(String.valueOf(post.getId()), post);
        }

        List<RagSearchResultDTO> results = new ArrayList<>();
        for (Map.Entry<String, String> entry : ordered) {
            if (results.size() >= limit) {
                break;
            }
            RagSearchResultDTO result = SOURCE_KNOWLEDGE.equals(entry.getKey())
                    ? toArticleResult(articles.get(entry.getValue()))
                    : toPostResult(posts.get(entry.getValue()));
            if (result != null) {
                results.add(result);
            }
        }
        return results;
    }

    // ==================== 关键词召回 ====================

    /** 关键词降级路径：复用服务层只读查询（已按可见性过滤），仅用于补齐向量召回不足的部分 */
    private List<RagSearchResultDTO> keywordResults(String keyword, String target, int limit) {
        List<RagSearchResultDTO> results = new ArrayList<>();
        if (includes(target, TYPE_ARTICLE)) {
            results.addAll(toArticleResults(
                    knowledgeArticleService.searchPublishedByKeyword(keyword, limit * WINDOW_FACTOR), limit));
        }
        if (includes(target, TYPE_POST)) {
            results.addAll(toPostResults(
                    communityService.searchNormalByKeyword(keyword, limit * WINDOW_FACTOR), limit));
        }
        return results;
    }

    /** 向量优先、关键词补齐，按站内链接去重（同一篇只出现一次） */
    private static List<RagSearchResultDTO> merge(List<RagSearchResultDTO> semantic,
                                                 List<RagSearchResultDTO> keyword,
                                                 int limit) {
        Map<String, RagSearchResultDTO> merged = new LinkedHashMap<>();
        for (RagSearchResultDTO result : semantic) {
            merged.putIfAbsent(result.getUrl(), result);
        }
        for (RagSearchResultDTO result : keyword) {
            merged.putIfAbsent(result.getUrl(), result);
        }
        List<RagSearchResultDTO> results = new ArrayList<>(merged.values());
        return results.size() > limit ? new ArrayList<>(results.subList(0, limit)) : results;
    }

    // ==================== 出参裁剪 ====================

    /** 文章实体 → 白名单结果（非法 ID / 空标题丢弃，标题摘要去标签并截断） */
    static RagSearchResultDTO toArticleResult(KnowledgeArticle row) {
        if (row == null || !isValidId(row.getId(), ARTICLE_ID_PATTERN)) {
            return null;
        }
        String title = cleanText(row.getTitle(), TITLE_MAX);
        if (title.isEmpty()) {
            return null;
        }
        String summary = cleanText(row.getSummary(), SUMMARY_MAX);
        if (summary.isEmpty()) {
            summary = cleanText(row.getContent(), SUMMARY_MAX);
        }
        return RagSearchResultDTO.builder()
                .source(SOURCE_KNOWLEDGE)
                .title(title)
                .summary(summary)
                .url("/knowledge/article/" + row.getId())
                .publishedAt(row.getPublishedAt())
                .build();
    }

    /** 帖子实体 → 白名单结果（危机帖不作为推荐阅读推送，干预走 CrisisSupportCard 既有链路） */
    static RagSearchResultDTO toPostResult(CommunityPost row) {
        if (row == null || Integer.valueOf(1).equals(row.getIsCrisis())) {
            return null;
        }
        String id = row.getId() == null ? null : String.valueOf(row.getId());
        if (!isValidId(id, POST_ID_PATTERN)) {
            return null;
        }
        String title = cleanText(row.getTitle(), TITLE_MAX);
        if (title.isEmpty()) {
            return null;
        }
        return RagSearchResultDTO.builder()
                .source(SOURCE_COMMUNITY)
                .title(title)
                .summary(cleanText(row.getContent(), SUMMARY_MAX))
                .url("/community/post/" + id)
                .publishedAt(row.getCreatedAt())
                .build();
    }

    static List<RagSearchResultDTO> toArticleResults(List<KnowledgeArticle> rows, int topK) {
        List<RagSearchResultDTO> results = new ArrayList<>();
        if (rows == null) {
            return results;
        }
        for (KnowledgeArticle row : rows) {
            if (results.size() >= topK) {
                break;
            }
            RagSearchResultDTO result = toArticleResult(row);
            if (result != null) {
                results.add(result);
            }
        }
        return results;
    }

    static List<RagSearchResultDTO> toPostResults(List<CommunityPost> rows, int topK) {
        List<RagSearchResultDTO> results = new ArrayList<>();
        if (rows == null) {
            return results;
        }
        for (CommunityPost row : rows) {
            if (results.size() >= topK) {
                break;
            }
            RagSearchResultDTO result = toPostResult(row);
            if (result != null) {
                results.add(result);
            }
        }
        return results;
    }

    // ==================== 参数规范化 ====================

    /** 关键词规范化：必填、剔除控制字符、长度上限（与社区搜索口径一致） */
    static String normalizeKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            throw new BusinessException("请输入检索关键词");
        }
        String kw = CONTROL_CHARS.matcher(keyword).replaceAll("").trim();
        if (kw.isEmpty()) {
            throw new BusinessException("请输入检索关键词");
        }
        if (kw.length() > MAX_KEYWORD_LENGTH) {
            throw new BusinessException("检索关键词不能超过50个字符");
        }
        return kw;
    }

    /** 条数上限：越界/为空取默认值，最大 5 条 */
    static int normalizeTopK(Integer topK) {
        if (topK == null || topK < 1) {
            return DEFAULT_TOP_K;
        }
        return Math.min(topK, MAX_TOP_K);
    }

    /** 检索类型规范化：article / post / all，非法值按 all */
    static String normalizeType(String type) {
        if (!StringUtils.hasText(type)) {
            return TYPE_ALL;
        }
        String target = type.trim().toLowerCase();
        return TYPE_ARTICLE.equals(target) || TYPE_POST.equals(target) ? target : TYPE_ALL;
    }

    private static boolean includes(String target, String type) {
        return TYPE_ALL.equals(target) || type.equals(target);
    }

    private static boolean isValidId(String id, Pattern pattern) {
        return id != null && pattern.matcher(id).matches();
    }

    /**
     * 纯文本化：去标签 + 去转义 + 压缩空白 + 截断
     *
     * <p>索引侧（{@link RagIndexService}）复用同一口径，保证入库文本与检索文本一致。</p>
     */
    public static String cleanText(String raw, int maxLength) {
        if (!StringUtils.hasText(raw)) {
            return "";
        }
        String text = HtmlUtil.unescape(HtmlUtil.cleanHtmlTag(raw)).replaceAll("\\s+", " ").trim();
        text = CONTROL_CHARS.matcher(text).replaceAll(" ").trim();
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }
}