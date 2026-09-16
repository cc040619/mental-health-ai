-- ============================================================
-- 4.7 社区管理模块升级（后台管理端）
-- 执行前请备份；community_post 已有 idx_status_created(status, created_at) 索引，无需新增
-- ============================================================

-- 1. 帖子表：置顶 / 加精 / 驳回原因
ALTER TABLE `community_post`
    ADD COLUMN `is_top`       tinyint      NOT NULL DEFAULT 0 COMMENT '0-否 1-置顶（置顶不设时长，如需到期自动取消可加 top_expire_at + 定时任务）',
    ADD COLUMN `is_featured`  tinyint      NOT NULL DEFAULT 0 COMMENT '0-否 1-加精',
    ADD COLUMN `reject_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '审核驳回原因（仅 REJECTED 状态有值）';

-- 2. 评论表：状态加 2-待审核（先审后发）
ALTER TABLE `community_comment`
    MODIFY COLUMN `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态 0:正常 1:已删除 2:待审核(先审后发)';

-- 3. 举报表：处理状态加 3-已警告
ALTER TABLE `community_report`
    MODIFY COLUMN `status` tinyint NOT NULL DEFAULT 0 COMMENT '处理状态 0:待处理 1:已下架 2:已忽略 3:已警告';

-- 4. 板块表（枚举转表驱动，存量帖子 board 字符串不变）
CREATE TABLE `community_board`
(
    `id`           bigint                                                       NOT NULL AUTO_INCREMENT COMMENT '板块ID',
    `code`         varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '板块编码(对应 post.board)',
    `name`         varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '板块名称',
    `icon`         varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '板块图标',
    `description`  varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '板块描述',
    `sort_order`   int                                                          NOT NULL DEFAULT 0 COMMENT '排序(越小越靠前)',
    `moderator_id` bigint                                                       NULL DEFAULT NULL COMMENT '版主用户ID(可选)',
    `status`       tinyint                                                      NOT NULL DEFAULT 1 COMMENT '0-停用 1-启用',
    `created_at`   datetime                                                     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_code` (`code`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '社区板块表'
  ROW_FORMAT = DYNAMIC;

-- 初始化六个板块（对应现有 CommunityBoard 枚举）
INSERT INTO `community_board` (`code`, `name`, `sort_order`, `status`) VALUES
    ('TREES',        '情绪树洞', 1, 1),
    ('ACADEMIC',     '学业压力', 2, 1),
    ('RELATIONSHIP', '人际关系', 3, 1),
    ('SLEEP',        '睡眠困扰', 4, 1),
    ('RECOVERY',     '康复分享', 5, 1),
    ('HELP',         '互助问答', 6, 1);

-- 5. 社区黑名单（屏蔽发帖/评论，不影响登录；历史内容保留仅禁新发）
CREATE TABLE `community_blacklist`
(
    `id`         bigint       NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`    bigint       NOT NULL COMMENT '被屏蔽用户ID',
    `reason`     varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '屏蔽原因',
    `created_at` datetime     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_user` (`user_id`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '社区黑名单(屏蔽发帖评论)'
  ROW_FORMAT = DYNAMIC;

-- 6. 敏感词库（仅违规词表驱动；危机词保持 classpath 文件，不纳入管理端防误删——安全底线）
CREATE TABLE `sensitive_word`
(
    `id`         bigint                                                      NOT NULL AUTO_INCREMENT COMMENT '主键',
    `word`       varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '敏感词',
    `category`   varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'OTHER' COMMENT '分类:POLITICAL/VIOLENT/VULGAR/AD/OTHER',
    `status`     tinyint                                                     NOT NULL DEFAULT 1 COMMENT '0-停用 1-启用',
    `created_at` datetime                                                    NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_word` (`word`) USING BTREE
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '敏感词库(违规词)'
  ROW_FORMAT = DYNAMIC;
