package com.cc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 站内资料检索结果（AI 工具与 /api/rag/search 共用出参）
 *
 * <p>只暴露白名单字段：标题、纯文本摘要、站内跳转链接、来源与时间。检索结果会进入模型上下文
 * 并回显到对话，因此不含正文、作者真实身份、图片等字段。</p>
 *
 * @author cc
 * @date 2026-09-27
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RagSearchResultDTO {

    /** 来源：knowledge=知识库文章 community=社区帖子 */
    private String source;

    /** 标题（已去标签与控制字符） */
    private String title;

    /** 纯文本摘要，≤120 字 */
    private String summary;

    /** 站内跳转链接，服务端白名单拼装（/knowledge/article/{id} 或 /community/post/{id}） */
    private String url;

    /** 发布时间（帖子取创建时间） */
    private LocalDateTime publishedAt;
}