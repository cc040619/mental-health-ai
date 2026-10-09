-- ----------------------------
-- sensitive_word 敏感词初始化数据
-- 来源：classpath: sensitive-words.txt（与 SensitiveWordFilter 的文件兜底词库一致）
-- 背景：init.sql 只建了 sensitive_word 表结构，未带任何初始词条，
--       导致全新环境部署后敏感词树节点数为 0、社区发帖违规词拦截实际失效。
--       SensitiveWordFilter#reload() 仅在「查表异常」时回退文件，
--       「表查询成功但返回 0 行」不会触发兜底，故必须显式初始化。
-- 幂等：利用 uk_word 唯一索引，重复执行只刷新状态，不产生重复行。
-- ----------------------------

USE mental_health_assistant;

INSERT INTO `sensitive_word` (`word`, `category`, `status`)
VALUES ('赌博', 'OTHER', 1),
       ('色情', 'VULGAR', 1),
       ('毒品', 'VIOLENT', 1),
       ('诈骗', 'AD', 1),
       ('枪支', 'VIOLENT', 1),
       ('弹药', 'VIOLENT', 1),
       ('炸药', 'VIOLENT', 1),
       ('雷管', 'VIOLENT', 1),
       ('传销', 'AD', 1),
       ('卖淫', 'VULGAR', 1),
       ('嫖娼', 'VULGAR', 1),
       ('代开发票', 'AD', 1),
       ('六合彩', 'AD', 1),
       ('私彩', 'AD', 1),
       ('澳门赌场', 'AD', 1),
       ('暴恐', 'VIOLENT', 1),
       ('血腥', 'VIOLENT', 1),
       ('裸体', 'VULGAR', 1),
       ('性交', 'VULGAR', 1),
       ('一夜情', 'VULGAR', 1),
       ('违禁品', 'VIOLENT', 1),
       ('假证', 'AD', 1),
       ('黑车', 'AD', 1),
       ('邪教', 'POLITICAL', 1),
       ('贩毒', 'VIOLENT', 1),
       ('吸毒', 'VIOLENT', 1),
       ('制毒', 'VIOLENT', 1),
       ('冰毒', 'VIOLENT', 1),
       ('海洛因', 'VIOLENT', 1),
       ('大麻', 'VIOLENT', 1),
       ('摇头丸', 'VIOLENT', 1),
       ('麻将作弊', 'AD', 1),
       ('办证刻章', 'AD', 1),
       ('枪支迷药', 'VIOLENT', 1)
ON DUPLICATE KEY UPDATE `status` = VALUES(`status`);
