-- =====================================================================
-- 咨询师端「咨询会话管理」升级脚本
-- 新增：咨询记录表 consultation_record（一单一记录，appointment_id 唯一）
-- 可见性：咨询师本人读写 + 管理员只读；用户端无任何接口暴露。
-- 幂等：CREATE TABLE IF NOT EXISTS，重复执行不报错。
-- @author cc
-- @date 2026-09-17
-- =====================================================================

CREATE TABLE IF NOT EXISTS `consultation_record`
(
    `id`                 bigint                                                        NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `appointment_id`     bigint                                                        NOT NULL COMMENT '预约ID(一单一记录)',
    `user_id`            bigint                                                        NOT NULL COMMENT '来访者ID',
    `consultant_id`      bigint                                                        NOT NULL COMMENT '咨询师ID',
    `chief_complaint`    varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '主诉',
    `analysis`           varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '分析',
    `intervention_plan`  varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '干预方案',
    `next_suggestion`    varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '下次建议',
    `created_at`         datetime                                                      NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`         datetime                                                      NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_appointment` (`appointment_id` ASC) USING BTREE,
    INDEX `idx_user` (`user_id` ASC) USING BTREE,
    INDEX `idx_consultant` (`consultant_id` ASC) USING BTREE,
    CONSTRAINT `fk_cr_appointment` FOREIGN KEY (`appointment_id`) REFERENCES `appointment` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `fk_cr_user` FOREIGN KEY (`user_id`) REFERENCES `user` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
    CONSTRAINT `fk_cr_consultant` FOREIGN KEY (`consultant_id`) REFERENCES `consultant` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB
  AUTO_INCREMENT = 1
  CHARACTER SET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT = '咨询记录(仅咨询师本人与管理员可见)'
  ROW_FORMAT = DYNAMIC;
