package com.cc.AIservice.rag;

import cn.hutool.json.JSONUtil;
import com.cc.dto.response.RagSearchResultDTO;
import com.cc.exception.BusinessException;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MCP 出口工具：把站内资料检索暴露给外部 MCP 客户端（Claude Desktop / Cursor 等）
 *
 * <p>与 AI 对话工具（{@link RagTools}）复用同一份检索实现，只读、无写操作、无用户入参；
 * 出参与对话工具一致（标题/摘要/站内链接/时间），不含正文、作者与图片。</p>
 *
 * <p>鉴权在出口层完成：{@code McpApiKeyFilter} 对 MCP 端点做 API Key 校验与限频，未通过校验的请求
 * 到不了工具层；因此这里不取身份，也拿不到 HTTP 上下文（WebMvc 传输的 transport context 为空）。</p>
 *
 * <p>工具内部兜底异常：返回一句提示而不抛出，避免一次检索失败变成 MCP 协议级错误。</p>
 *
 * @author cc
 * @date 2026-09-27
 */
@Slf4j
@Component
public class ApiMcpTools {

    /** MCP 客户端固定返回条数（外部调用无法指定 topK，避免一次拉走过多内容） */
    private static final int MCP_TOP_K = 3;

    @Resource
    private RagSearchService ragSearchService;

    @McpTool(name = "search_knowledge_articles",
            description = "检索心理健康知识库中已发布的科普文章，返回标题、摘要与站内跳转链接。"
                    + "keyword 传简短关键词（1-20字，如：社交焦虑、失眠、正念呼吸）。",
                    annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
                            idempotentHint = true, openWorldHint = false))
    public String searchKnowledgeArticles(
            @McpToolParam(description = "检索关键词，1-20字，如：社交焦虑") String keyword) {
        return search("search_knowledge_articles", RagSearchService.TYPE_ARTICLE, keyword);
    }

    @McpTool(name = "search_community_posts",
            description = "检索暖心社区中已发布的帖子（匿名帖不暴露作者），返回标题、摘要与站内跳转链接。"
                    + "keyword 传简短关键词（1-20字，如：室友矛盾、考研压力）。",
                    annotations = @McpTool.McpAnnotations(readOnlyHint = true, destructiveHint = false,
                            idempotentHint = true, openWorldHint = false))
    public String searchCommunityPosts(
            @McpToolParam(description = "检索关键词，1-20字，如：考研压力") String keyword) {
        return search("search_community_posts", RagSearchService.TYPE_POST, keyword);
    }

    /** 检索 → JSON 字符串；参数异常与系统异常都降级为提示文本 */
    private String search(String toolName, String type, String keyword) {
        try {
            List<RagSearchResultDTO> results = ragSearchService.search(keyword, type, MCP_TOP_K);
            log.info("MCP 检索工具: tool={}, hits={}", toolName, results.size());
            return JSONUtil.toJsonStr(results);
        } catch (BusinessException e) {
            log.warn("MCP 检索工具参数异常: tool={}, msg={}", toolName, e.getMessage());
            return "检索未执行：" + e.getMessage();
        } catch (Exception e) {
            log.error("MCP 检索工具执行失败: tool={}", toolName, e);
            return "检索失败：系统繁忙，请稍后重试";
        }
    }
}