-- ============================================================
-- 系统设置模块迁移脚本（执行前请备份：sys_config / crisis_word / admin_operation_log 均为新表）
-- 说明：sys_config 初始行与 crisis_word 初始词库由程序启动时自动播种（服务 @PostConstruct），
--       本脚本只建表，不插数据。
-- ============================================================

CREATE TABLE `sys_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config_key` varchar(50) NOT NULL,
  `config_value` varchar(1000) NULL,
  `group_name` varchar(20) NOT NULL COMMENT 'BASIC/REGISTER/CONTENT/CRISIS',
  `description` varchar(200) NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key` (`config_key`)
) COMMENT='系统配置KV';

CREATE TABLE `crisis_word` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `word` varchar(50) NOT NULL,
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '0-停用 1-启用',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_word` (`word`)
) COMMENT='危机词库(表驱动,安全底线:启用词<5时删除/停用被拒绝)';

CREATE TABLE `admin_operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `operator_id` bigint NULL,
  `operator_name` varchar(50) NULL,
  `action_type` varchar(20) NOT NULL COMMENT 'CREATE/UPDATE/DELETE/LOGIN/LOGOUT',
  `action_content` varchar(1000) NULL COMMENT '方法名+参数摘要(敏感字段已脱敏)',
  `ip` varchar(50) NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_created` (`created_at`),
  KEY `idx_operator_created` (`operator_id`, `created_at`)
) COMMENT='管理员操作日志(AOP异步写入,敏感参数脱敏)';
