-- ============================================================
-- AI 模型配置模块增量迁移（在现有库上执行一次，勿重复执行）
-- 五表：ai_config / ai_prompt_template / ai_block_record / ai_quality_sample / ai_call_log
-- 执行前建议备份
-- ============================================================

-- 1. AI 配置 KV（模型参数 / 过滤开关 / 单价）
CREATE TABLE `ai_config` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `config_key` varchar(50) NOT NULL,
  `config_value` varchar(500) NULL,
  `description` varchar(200) NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key` (`config_key`)
) COMMENT='AI 配置KV(模型参数/过滤开关/单价)';

-- 初始配置行（单价为百炼 deepseek-v3 参考价：输入 0.5 元/百万token、输出 2.0 元/百万token，按实际价调整）
INSERT INTO `ai_config` (`config_key`, `config_value`, `description`) VALUES
('MODEL', 'deepseek-v3', '模型名(百炼兼容模式)'),
('TEMPERATURE', '0.7', '温度 0-2'),
('MAX_TOKENS', '2048', '单次最大输出 token 1-8192'),
('TOP_P', '0.9', '核采样 0-1'),
('INPUT_PRICE', '0.5', '输入单价(元/百万token)'),
('OUTPUT_PRICE', '2.0', '输出单价(元/百万token)'),
('FILTER_AI_REPLY', 'OFF', 'AI回复敏感词过滤开关 ON/OFF'),
('FILTER_ACTION', 'LOG_ONLY', '过滤动作 BLOCK/REPLACE/LOG_ONLY');

-- 2. AI 提示词模板（key 维度多版本，同 key 仅一行 status=1 生效）
-- 无初始行：启动后 DB 无记录时回退 PromptManage 静态常量（零风险兜底），首次"保存草稿"即产生 v1
CREATE TABLE `ai_prompt_template` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `prompt_key` varchar(50) NOT NULL,
  `name` varchar(100) NOT NULL COMMENT '模板名称',
  `content` text NOT NULL,
  `version` int NOT NULL COMMENT '版本号(同key自增)',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0-历史/草稿 1-生效(同key仅一行)',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_key_version` (`prompt_key`, `version`)
) COMMENT='AI 提示词模板(多版本)';

-- 3. AI 回复拦截记录（只存摘要不存全文——心理健康对话属敏感数据）
CREATE TABLE `ai_block_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NULL COMMENT '会话ID(可空)',
  `trigger_word` varchar(50) NOT NULL COMMENT '命中词',
  `content_summary` varchar(200) NULL COMMENT '内容摘要(≤200字,不存全文)',
  `action` varchar(20) NOT NULL COMMENT 'BLOCK/REPLACE/LOG_ONLY',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_created` (`created_at`)
) COMMENT='AI 回复拦截记录';

-- 4. 对话质量抽检
CREATE TABLE `ai_quality_sample` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `session_id` bigint NOT NULL,
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0-待标注 1-好 2-一般 3-坏',
  `score` tinyint NULL COMMENT '评分1-5',
  `note` varchar(500) NULL COMMENT '标注备注',
  `annotator_id` bigint NULL COMMENT '标注管理员ID',
  `annotated_at` datetime NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_status` (`status`),
  KEY `idx_session` (`session_id`)
) COMMENT='对话质量抽检';

-- 5. AI 调用日志（AOP 埋点异步写入；token 尽力提取无则 NULL）
CREATE TABLE `ai_call_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `purpose` varchar(50) NOT NULL COMMENT '调用用途(STREAM_CHAT/EMOTION_ANALYSIS/SUMMARY/AI_TEST)',
  `model` varchar(50) NULL,
  `input_tokens` int NULL COMMENT '尽力提取,无则null',
  `output_tokens` int NULL,
  `duration_ms` int NOT NULL,
  `success` tinyint NOT NULL DEFAULT 1,
  `error_msg` varchar(200) NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_created` (`created_at`)
) COMMENT='AI 调用日志';
