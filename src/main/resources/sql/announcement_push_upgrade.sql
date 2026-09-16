-- ============================================================
-- 公告消息管理完善 迁移脚本（2026-09-15）
-- 执行前请备份 announcement 表
-- 幂等性说明：MySQL ALTER 无 IF NOT EXISTS，重复执行会报错，仅需执行一次
-- ============================================================

-- 1. 公告表：类型 / 发布范围 / 定时发布 + 定时扫描索引
ALTER TABLE `announcement`
  ADD COLUMN `type` varchar(20) NOT NULL DEFAULT 'SYSTEM' COMMENT 'SYSTEM-系统公告 ACTIVITY-活动通知 TIP-温馨提示' AFTER `content`,
  ADD COLUMN `target_type` varchar(10) NOT NULL DEFAULT 'ALL' COMMENT 'ALL-全体 USERS-指定用户' AFTER `type`,
  ADD COLUMN `target_user_ids` json NULL COMMENT '指定用户ID数组(JSON)' AFTER `target_type`,
  ADD COLUMN `publish_at` datetime NULL COMMENT '定时发布时间(空=非定时)' AFTER `target_user_ids`,
  ADD INDEX `idx_status_publish` (`status`, `publish_at`);

-- 2. 状态三态迁移：旧 0(已下架)→2(已撤回)；旧 1(发布中)→1(已发布) 不变
UPDATE `announcement` SET `status` = 2 WHERE `status` = 0;

-- 3. 消息推送批次表（站内消息落库即送达，送达率=100%；阅读数实时统计不冗余）
CREATE TABLE `notification_push` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `type` varchar(30) NOT NULL COMMENT '推送类型(通知类型枚举)',
  `title` varchar(200) NOT NULL,
  `content` varchar(1000) NOT NULL,
  `target_type` varchar(10) NOT NULL DEFAULT 'ALL' COMMENT 'ALL-全体 USERS-指定用户',
  `target_count` int NOT NULL COMMENT '目标用户数',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) COMMENT='消息推送批次记录';

-- 4. 首页轮播图表
CREATE TABLE `banner` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(100) NULL COMMENT '标题',
  `image_url` varchar(500) NOT NULL COMMENT '图片地址(OSS)',
  `link_url` varchar(500) NULL COMMENT '跳转链接(仅http/https)',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序(小在前)',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0-停用 1-启用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) COMMENT='首页轮播图';
