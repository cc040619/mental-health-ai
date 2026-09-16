-- ============================================================
-- 咨询记录管理增强 迁移脚本（2026-09-15）
-- 执行前请备份 consultation_session / consultation_message 两表
-- 幂等性说明：MySQL ALTER 无 IF NOT EXISTS，重复执行会报错，仅需执行一次
-- ============================================================

-- 1. 会话表：情绪标签冗余 + 运营标记（高质量/危机）
ALTER TABLE `consultation_session`
  ADD COLUMN `emotion_tag` varchar(50) NULL COMMENT '最近主情绪标签(冗余，情绪分析同步写入)' AFTER `last_emotion_updated_at`,
  ADD COLUMN `is_high_quality` tinyint NOT NULL DEFAULT 0 COMMENT '0-否 1-高质量(AI训练参考，人工运营标记)' AFTER `emotion_tag`,
  ADD COLUMN `is_crisis` tinyint NOT NULL DEFAULT 0 COMMENT '0-否 1-危机会话(运营关注标记，与用户端自动危机识别解耦)' AFTER `is_high_quality`,
  ADD COLUMN `mark_note` varchar(500) NULL COMMENT '标记备注' AFTER `is_crisis`;

-- 2. 消息表：情绪值（0-100，情绪分析完成后回写该回合最新用户消息，历史消息为 NULL）
ALTER TABLE `consultation_message`
  ADD COLUMN `emotion_value` decimal(5,2) NULL COMMENT '情绪值(0-100，情绪分析写路径同步写入)' AFTER `emotion_tag`;

-- 3. 列表筛选索引
ALTER TABLE `consultation_session` ADD INDEX `idx_started` (`started_at`);
ALTER TABLE `consultation_session` ADD INDEX `idx_scene` (`scene`);
