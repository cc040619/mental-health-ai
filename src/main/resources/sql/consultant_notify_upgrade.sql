-- 咨询师回复通知：user_notify_setting 新增 consultant_on 列（默认开启）
-- 已存在的用户记录默认值为 1（开启），保持与新用户一致
ALTER TABLE `user_notify_setting`
    ADD COLUMN `consultant_on` tinyint NOT NULL DEFAULT 1 COMMENT '咨询师回复开关 0:关 1:开' AFTER `assessment_on`;
