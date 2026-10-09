package com.cc.AIservice.rag;

import cn.hutool.json.JSONUtil;
import com.cc.dto.response.RagSearchResultDTO;
import com.cc.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI 对话可调用的站内资料检索工具
 *
 * <p>身份取自 Spring AI {@link ToolContext}（请求侧由 ChatClient.toolContext 写入），入参中不含 userId，
 * 模型无法伪造身份。工具在响应式线程执行，取不到请求线程上下文，因此这里禁止改用 CurrentUserUtils。</p>
 *
 * <p>工具内部兜底异常：检索失败返回一句提示而不抛出，避免整条 SSE 对话因一次工具调用失败而中断。</p>
 *
 * @author cc
 * @date 2026-09-27
 */
@Slf4j
@Component
public class RagTools {

    /** ToolContext 键：当前用户ID（Long） */
    public static final String CTX_USER_ID = "userId";

    @Resource
    private RagSearchService ragSearchService;

    @Tool(name = "search_knowledge_articles",
            description = "检索站内知识库的已发布科普文章。用户想找文章/科普内容，或让你推荐、搜索相关文章时调用。"
                    + "keyword 传简短关键词（1-20字，如：社交焦虑、失眠、正念呼吸）；没找到结果时换更短的关键词再试一次。")
    public String searchKnowledgeArticles(
            @ToolParam(description = "检索关键词，1-20字，如：社交焦虑") String keyword,
            @ToolParam(description = "返回条数，1-5，默认3", required = false) Integer topK,
            ToolContext toolContext) {
        return search("search_knowledge_articles", RagSearchService.TYPE_ARTICLE, keyword, topK, toolContext);
    }

    @Tool(name = "search_community_posts",
            description = "检索站内暖心社区的帖子。用户问社区里有没有人聊过某个话题、有没有类似经历分享时调用。"
                    + "keyword 传简短关键词（1-20字，如：室友矛盾、考研压力、失眠）。")
    public String searchCommunityPosts(
            @ToolParam(description = "检索关键词，1-20字，如：考研压力") String keyword,
            @ToolParam(description = "返回条数，1-5，默认3", required = false) Integer topK,
            ToolContext toolContext) {
        return search("search_community_posts", RagSearchService.TYPE_POST, keyword, topK, toolContext);
    }

    /**
     * 取身份（信任边界校验 + 审计日志）→ 检索 → JSON 返回；失败降级为提示文本，不中断对话
     *
     * <p>检索结果本身与身份无关（只返回站内公开的已发布文章 / 正常帖），身份仅用于确认调用来自已登录会话。</p>
     */
    private String search(String toolName, String type, String keyword, Integer topK, ToolContext toolContext) {
        try {
            Long userId = requireUserId(toolContext);
            List<RagSearchResultDTO> results = ragSearchService.search(keyword, type, topK);
            log.info("RAG 检索工具: tool={}, userId={}, hits={}", toolName, userId, results.size());
            return JSONUtil.toJsonStr(results);
        } catch (BusinessException e) {
            // 参数类异常：把原因回传模型，由模型向用户解释后重试
            log.warn("RAG 检索工具参数异常: tool={}, msg={}", toolName, e.getMessage());
            return "检索未执行：" + e.getMessage();
        } catch (Exception e) {
            log.error("RAG 检索工具执行失败: tool={}", toolName, e);
            return "检索失败：系统繁忙，请稍后重试";
        }
    }

    private static Long requireUserId(ToolContext toolContext) {
        Object value = toolContext == null ? null : toolContext.getContext().get(CTX_USER_ID);
        if (value instanceof Long userId) {
            return userId;
        }
        throw new BusinessException("无法获取当前登录信息");
    }
}