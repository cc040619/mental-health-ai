package com.cc.common;

/**
 * 社区模块公共常量
 *
 * @author cc
 * @date 2026-09-15
 */
public final class CommunityConstants {

    private CommunityConstants() {
    }

    /**
     * 先审后发开关：false=现状（发帖/评论即正常可见）；true=新帖/新评论进待审核，
     * 管理端审核通过后可见、驳回不可见（用户端"我的"可见驳回原因）。
     * ponytail: 编译期常量足够当前阶段；需要运营动态切换时升级为 sys_config 表 + 缓存
     */
    public static final boolean REVIEW_BEFORE_PUBLISH = false;
}
