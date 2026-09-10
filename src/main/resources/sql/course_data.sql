-- ----------------------------
-- 心理课程 / 冥想模块 种子数据
-- 音频统一使用已上传至阿里云 OSS 的测试资源；封面留空由前端按类别 emoji 兜底
-- 执行前请先执行 init.sql 中的 course 系列 DDL
-- ----------------------------
USE mental_health_assistant;

-- 幂等：重复执行时先清空旧数据（按外键依赖倒序）
DELETE FROM `practice_checkin`;
DELETE FROM `course_comment`;
DELETE FROM `course_progress`;
DELETE FROM `course_favorite`;
DELETE FROM `course_chapter`;
DELETE FROM `course`;

-- 音频资源（OSS 测试文件）
SET @audio = 'https://mental-health-ai.oss-cn-beijing.aliyuncs.com/bussiness/MENTAL_VEDIO/%E5%BF%83%E7%90%86%E5%81%A5%E5%BA%B7%E5%8A%A8%E7%94%BB%E7%9F%AD%E8%A7%86%E9%A2%91.mp4';

-- ----------------------------
-- 课程：6 门，覆盖 5 个类别
-- ----------------------------
INSERT INTO `course` (`id`, `title`, `category`, `description`, `cover_url`, `total_duration`, `learn_count`, `status`) VALUES
(1, '正念呼吸入门', 'MEDITATION', '从最简单的呼吸开始，带你认识正念。每天 5 分钟，学会把注意力带回当下，缓解焦虑与压力。', NULL, 720, 128, 1),
(2, '身体扫描冥想', 'MEDITATION', '通过从头到脚的身体扫描，唤醒对身体感受的觉察，放松紧绷的肌肉，进入深度平静。', NULL, 900, 86, 1),
(3, '睡前放松引导', 'SLEEP', '专为入睡困难设计。柔和的语言引导，配合呼吸节奏，帮助你缓缓进入梦乡。', NULL, 1080, 203, 1),
(4, '情绪调节练习', 'EMOTION', '当你情绪上头时，这三步练习能帮你识别、命名并温和地调节情绪，避免冲动行事。', NULL, 600, 75, 1),
(5, '认知重构入门', 'CBT', '认知行为疗法（CBT）基础练习。识别自动思维、挑战负性认知、建立更平衡的想法。', NULL, 840, 64, 1),
(6, '人际沟通练习', 'INTERPERSONAL', '通过非暴力沟通四步法练习表达与倾听，改善亲密关系与同伴互动。', NULL, 780, 52, 1);

-- ----------------------------
-- 章节：每门课 3-5 章
-- ----------------------------
-- 课程 1：正念呼吸入门（4 章）
INSERT INTO `course_chapter` (`id`, `course_id`, `title`, `audio_url`, `duration`, `intro`, `sort_order`) VALUES
(1, 1, '什么是正念', @audio, 180, '正念是有意识地、不评判地关注当下。本章带你理解正念的核心概念。', 1),
(2, 1, '找到你的呼吸', @audio, 180, '把注意力放在呼吸上，感受空气进出身体的感觉，这是正念练习的锚点。', 2),
(3, 1, '5 分钟呼吸练习', @audio, 300, '跟随引导完成一次完整的 5 分钟呼吸练习，体验回到当下的平静。', 3),
(4, 1, '把正念带入生活', @audio, 60, '正念不仅在 cushion 上，更在每一次走路、吃饭、洗碗中。', 4);

-- 课程 2：身体扫描冥想（3 章）
INSERT INTO `course_chapter` (`id`, `course_id`, `title`, `audio_url`, `duration`, `intro`, `sort_order`) VALUES
(5, 2, '准备与放松', @audio, 180, '找一个舒适姿势，闭眼，做几次深呼吸，让身体开始放松。', 1),
(6, 2, '从头部到肩颈', @audio, 360, '觉察头皮、面部、颈部和肩膀的感受，释放那里储存的紧张。', 2),
(7, 2, '从胸腹到脚趾', @audio, 360, '继续向下扫描，感受胸腔的起伏、四肢的重量，直到脚趾。', 3);

-- 课程 3：睡前放松引导（5 章）
INSERT INTO `course_chapter` (`id`, `course_id`, `title`, `audio_url`, `duration`, `intro`, `sort_order`) VALUES
(8,  3, '放下今天', @audio, 180, '回顾今天发生的三件小事，然后轻柔地放下它们。', 1),
(9,  3, '呼吸放缓', @audio, 240, '把呼吸节奏放慢到每分钟 6 次左右，激活副交感神经。', 2),
(10, 3, '身体松弛', @audio, 240, '从脚趾到头部，依次收紧再放松每组肌肉。', 3),
(11, 3, '想象安全场所', @audio, 240, '在心里描绘一个让你感到安全和放松的场景。', 4),
(12, 3, '进入梦乡', @audio, 180, '在柔和的引导中慢慢沉入睡眠，无需刻意，允许自己睡去。', 5);

-- 课程 4：情绪调节练习（3 章）
INSERT INTO `course_chapter` (`id`, `course_id`, `title`, `audio_url`, `duration`, `intro`, `sort_order`) VALUES
(13, 4, '识别你的情绪', @audio, 180, '情绪来了，先别急着反应。给它一个准确的名字。', 1),
(14, 4, '允许情绪存在', @audio, 240, '情绪没有对错。允许它在这里，像对待一位访客。', 2),
(15, 4, '选择如何回应', @audio, 180, '在情绪和行动之间，留出一个选择的空隙。', 3);

-- 课程 5：认知重构入门（4 章）
INSERT INTO `course_chapter` (`id`, `course_id`, `title`, `audio_url`, `duration`, `intro`, `sort_order`) VALUES
(16, 5, '什么是自动思维', @audio, 180, '自动思维是脑海里冒出来的、未经审视的想法，常常决定我们的情绪。', 1),
(17, 5, '捕捉负性思维', @audio, 240, '练习记录让你难过的想法，找出其中的认知扭曲。', 2),
(18, 5, '挑战与重构', @audio, 240, '问自己：这个想法有证据吗？有没有更平衡的看待方式？', 3),
(19, 5, '建立新思维', @audio, 180, '用一句更平衡、更善意的话，替代原本的负性思维。', 4);

-- 课程 6：人际沟通练习（4 章）
INSERT INTO `course_chapter` (`id`, `course_id`, `title`, `audio_url`, `duration`, `intro`, `sort_order`) VALUES
(20, 6, '观察而非评判', @audio, 180, '非暴力沟通第一步：说出你观察到的事实，不加评判。', 1),
(21, 6, '表达感受与需要', @audio, 240, '区分感受与想法，并找到感受背后的需要。', 2),
(22, 6, '提出具体请求', @audio, 240, '请求要具体、可执行、可被回应，而不是命令。', 3),
(23, 6, '倾听他人', @audio, 120, '把同样的四步用在倾听上，理解对方的观察、感受、需要和请求。', 4);
