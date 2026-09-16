package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 社区帖子响应 DTO（信息流卡片 / 帖子详情共用）
 *
 * <p>信息流场景 content 为空（仅 summary 摘要）；详情场景 content 为清洗后富文本。
 * 作者信息为真实身份，匿名屏蔽由 Service 层按访问者（作者本人/管理员可见真实）统一处理。</p>
 *
 * @author cc
 * @date 2026-09-10
 */
@Data
public class PostResponseDTO {

    private Long id;

    // 板块编码与中文名
    private String board;
    private String boardLabel;

    private String title;

    // 列表摘要（去 HTML 标签后的纯文本前 100 字）
    private String summary;

    // 正文（富文本，仅详情接口返回；列表为 null）
    private String content;

    // 图片URL数组
    private List<String> images;

    // 作者信息（匿名帖子对外被屏蔽为 匿名用户/null）
    private Long authorId;
    private String authorName;
    private String avatar;
    private Integer isAnonymous;

    // 当前访问者视角标记
    private Boolean isMine;
    private Boolean isLiked;
    private Boolean isFavorited;

    // 计数
    private Integer likeCount;
    private Integer commentCount;
    private Integer favoriteCount;
    private Integer viewCount;

    // 危机标记（1 时前端展示干预提示卡片）
    private Integer isCrisis;

    // 危机干预提示（isCrisis=1 时返回共情文案与援助热线，与访问者无关可缓存）
    private Object crisisTip;

    // 帖子状态（我的帖子/后台场景使用）
    private Integer status;

    // 置顶/加精标记（用户端列表置顶标识 + 管理端）
    private Integer isTop;
    private Integer isFeatured;

    // 审核驳回原因（REJECTED 状态时"我的帖子"可见）
    private String rejectReason;

    private LocalDateTime createdAt;
}
