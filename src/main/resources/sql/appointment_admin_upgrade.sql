-- =====================================================================
-- 预约管理模块升级脚本（4.8）
-- 新增：预约取消原因 + 咨询师评价管理员回复字段
-- @author cc
-- @date 2026-09-15
-- =====================================================================

-- 1. 预约取消原因（管理员/用户取消时填写）
ALTER TABLE `appointment`
    ADD COLUMN `cancel_reason` varchar(500) NULL COMMENT '取消原因' AFTER `problem_desc`;

-- 2. 咨询师评价管理员回复（单级回复）
ALTER TABLE `consultant_review`
    ADD COLUMN `reply_content` varchar(500) NULL COMMENT '管理员/咨询师回复',
    ADD COLUMN `reply_at` datetime NULL COMMENT '回复时间';
