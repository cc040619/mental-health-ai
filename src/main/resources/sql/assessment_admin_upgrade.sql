-- =====================================================================
-- 心理测评管理模块升级脚本
-- 新增：通用题目库表 + 测评记录干预字段
-- @author cc
-- @date 2026-09-15
-- =====================================================================

-- 1. 通用题目库（素材池，量表以快照引用，题库题修改不影响已建量表）
CREATE TABLE `assessment_question` (
    `id`           bigint       NOT NULL AUTO_INCREMENT COMMENT '题目ID',
    `stem`         varchar(500) NOT NULL COMMENT '题干',
    `type`         varchar(20)  NOT NULL COMMENT '题型：SINGLE-单选 MULTIPLE-多选 SCALE-量表题',
    `options`      json         NOT NULL COMMENT '选项JSON [{label,text,score}]',
    `dimension_tag` varchar(50) NULL COMMENT '默认维度标签(可选)',
    `category`     varchar(30)  NULL COMMENT '题目库分类',
    `created_at`   datetime     NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`   datetime     NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_type` (`type`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '通用题目库(素材池)';

-- 2. 测评记录干预字段（高危预警流转）
ALTER TABLE `assessment_record`
    ADD COLUMN `intervention_status` tinyint     NOT NULL DEFAULT 0 COMMENT '干预状态 0-未处理 1-已干预 2-已处理',
    ADD COLUMN `intervention_note`   varchar(500) NULL COMMENT '干预备注',
    ADD COLUMN `intervened_at`       datetime     NULL COMMENT '干预时间';

-- 3. 预警检索索引：level + intervention_status
ALTER TABLE `assessment_record` ADD INDEX `idx_level_status` (`level`, `intervention_status`);
