-- =====================================================================
-- 课程内容管理模块升级脚本
-- 配合后台管理端新增：状态五态化 + 审核流 + 章节视频 + 评论回复 + 学习数据
-- 执行顺序：先 ALTER 加列 → 再 UPDATE 状态迁移 → 最后加索引
-- 已有 idx_course_created 索引（init.sql 1070 行），不重复添加
-- @author cc
-- @date 2026-09-15
-- =====================================================================

-- 1. course 表新增字段：讲师/适合人群/审核驳回原因/审核时间/发布时间
ALTER TABLE `course`
    ADD COLUMN `lecturer`        varchar(50)  NULL COMMENT '讲师姓名',
    ADD COLUMN `target_audience` varchar(200) NULL COMMENT '适合人群',
    ADD COLUMN `reject_reason`   varchar(500) NULL COMMENT '审核驳回原因',
    ADD COLUMN `reviewed_at`     datetime     NULL COMMENT '审核时间',
    ADD COLUMN `published_at`    datetime     NULL COMMENT '发布时间';

-- 2. 状态迁移：原 1上架 → 2已发布，原 0下架 → 4已下架
--    新状态枚举：0草稿/1待审核/2已发布/3已驳回/4已下架
UPDATE `course` SET `status` = 2 WHERE `status` = 1;
UPDATE `course` SET `status` = 4 WHERE `status` = 0;
-- 已发布课程补 published_at（用 updated_at 兜底）
UPDATE `course` SET `published_at` = COALESCE(`updated_at`, `created_at`) WHERE `status` = 2;

-- 3. 状态查询索引（status + created_at）
ALTER TABLE `course` ADD INDEX `idx_status_created` (`status` ASC, `created_at` DESC);

-- 4. course_chapter 表新增视频地址（音频为主，视频可选预留）
ALTER TABLE `course_chapter`
    ADD COLUMN `video_url` varchar(500) NULL COMMENT '视频地址(可选)' AFTER `audio_url`;

-- 5. course_comment 表新增管理员回复字段（单级回复，前台展示）
ALTER TABLE `course_comment`
    ADD COLUMN `reply_content` varchar(500) NULL COMMENT '管理员回复',
    ADD COLUMN `reply_at`      datetime     NULL COMMENT '回复时间';
