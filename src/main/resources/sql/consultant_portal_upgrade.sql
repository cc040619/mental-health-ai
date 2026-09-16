-- ----------------------------
-- 咨询师端（user_type=3）升级（一次性迁移）
-- consultant 增加 user_id 关联登录账号（user.user_type=3 咨询师），唯一约束保证一账号一咨询师。
-- 账号生命周期：管理员创建咨询师时同步建号（user.status=0 禁用，审核通过前不可登录）；
--              审核通过启用账号；禁用咨询师同步禁用账号并踢下线。
-- 执行前请备份 consultant / user 表；仅需执行一次
-- ----------------------------

ALTER TABLE `consultant`
    ADD COLUMN `user_id` bigint NULL COMMENT '关联登录账号(user_type=3)' AFTER `id`,
    ADD UNIQUE KEY `uk_user_id` (`user_id`);
