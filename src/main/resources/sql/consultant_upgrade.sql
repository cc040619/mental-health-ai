-- ----------------------------
-- 咨询师管理模块升级（一次性迁移）
-- 1. 状态语义迁移：旧 0=停用 / 1=上架 → 新 0=待审核 / 1=正常 / 2=禁用
--    旧停用(0) → 禁用(2)；旧上架(1) 语义即正常(1)，无需变更
-- 2. 新增审核流字段（资质图片/驳回原因/审核时间/入驻时间）、排序权重、咨询价格（付费预留）
-- 3. 新记录默认待审核(status=0)
-- 执行前请备份 consultant 表；仅需执行一次
-- ----------------------------

ALTER TABLE `consultant`
    ADD COLUMN `qualification_images` json        NULL COMMENT '资质证明图片URL数组(JSON)',
    ADD COLUMN `reject_reason`        varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '驳回原因(非空=已驳回待修改)',
    ADD COLUMN `reviewed_at`          datetime     NULL DEFAULT NULL COMMENT '最近审核时间',
    ADD COLUMN `joined_at`            datetime     NULL DEFAULT NULL COMMENT '入驻时间(审核通过时间)',
    ADD COLUMN `sort_weight`          int          NOT NULL DEFAULT 0 COMMENT '排序权重(越大越靠前)',
    ADD COLUMN `price`                decimal(10, 2) NULL DEFAULT NULL COMMENT '咨询价格(元/小时，付费预留)';

-- 存量状态迁移：旧 0=停用 → 2=禁用（旧 1=上架 保持 1=正常）
UPDATE `consultant`
SET `status` = 2
WHERE `status` = 0;

-- 存量已上架咨询师补入驻时间，避免列表「入驻时间」为空
UPDATE `consultant`
SET `joined_at` = COALESCE(`joined_at`, `created_at`)
WHERE `status` = 1;

-- 新记录默认待审核
ALTER TABLE `consultant`
    MODIFY COLUMN `status` tinyint NOT NULL DEFAULT 0 COMMENT '0-待审核 1-正常 2-禁用';

-- 管理端列表检索：状态筛选 + 创建时间排序
ALTER TABLE `consultant`
    ADD INDEX `idx_status_created` (`status` ASC, `created_at` ASC) USING BTREE;

-- 1) knowledge_article 加 3 个冗余计数列 + 索引
ALTER TABLE `knowledge_article`
    ADD COLUMN `like_count`     int NOT NULL DEFAULT 0 COMMENT '点赞数(冗余字段,article_like 维护)',
    ADD COLUMN `comment_count`  int NOT NULL DEFAULT 0 COMMENT '已通过评论数(冗余字段,article_comment 维护)',
    ADD COLUMN `favorite_count` int NOT NULL DEFAULT 0 COMMENT '收藏数(冗余字段,user_favorite 维护)',
    ADD INDEX `idx_like_count` (`like_count`),
    ADD INDEX `idx_comment_count` (`comment_count`),
    ADD INDEX `idx_favorite_count` (`favorite_count`);

-- 2) user_browse_history 加阅读时长列
ALTER TABLE `user_browse_history`
    ADD COLUMN `duration_seconds` int NOT NULL DEFAULT 0 COMMENT '累计阅读时长(秒)';

-- 3) knowledge_category 加图标列
ALTER TABLE `knowledge_category`
    ADD COLUMN `icon` varchar(200) NULL DEFAULT NULL COMMENT '分类图标(emoji或图片URL)';

-- 4) 新表：文章点赞
CREATE TABLE `article_like` (
                                `id`         bigint                                                       NOT NULL AUTO_INCREMENT,
                                `article_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                                `user_id`    bigint                                                       NOT NULL,
                                `created_at` datetime                                                     NULL DEFAULT CURRENT_TIMESTAMP,
                                PRIMARY KEY (`id`),
                                UNIQUE INDEX `uk_user_article` (`user_id`, `article_id`),
                                INDEX `idx_article` (`article_id`),
                                CONSTRAINT `fk_al_article` FOREIGN KEY (`article_id`) REFERENCES `knowledge_article` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
                                CONSTRAINT `fk_al_user`    FOREIGN KEY (`user_id`)    REFERENCES `user` (`id`)             ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章点赞表';

-- 5) 新表：文章评论（含审核流）
CREATE TABLE `article_comment` (
                                   `id`         bigint                                                        NOT NULL AUTO_INCREMENT,
                                   `article_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci  NOT NULL,
                                   `user_id`    bigint                                                        NOT NULL,
                                   `content`    varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
                                   `status`     tinyint                                                       NOT NULL DEFAULT 0 COMMENT '0:待审核 1:已通过 2:已拒绝',
                                   `created_at` datetime                                                      NULL DEFAULT CURRENT_TIMESTAMP,
                                   PRIMARY KEY (`id`),
                                   INDEX `idx_article_status` (`article_id`, `status`, `created_at`),
                                   CONSTRAINT `fk_ac_article` FOREIGN KEY (`article_id`) REFERENCES `knowledge_article` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
                                   CONSTRAINT `fk_ac_user`    FOREIGN KEY (`user_id`)    REFERENCES `user` (`id`)             ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='文章评论表';