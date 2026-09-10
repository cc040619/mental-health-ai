package com.cc.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.AIservice.ConsultationSummaryService;
import com.cc.dto.request.ConsultationSessionCreateDTO;
import com.cc.dto.request.SessionFeedbackDTO;
import com.cc.dto.response.ConsultationSessionResponseDTO;
import com.cc.dto.response.EmotionAnalysisDTO;
import com.cc.dto.response.HumanHandoffResponseDTO;
import com.cc.dto.response.SessionSummaryResponseDTO;
import com.cc.entity.ConsultationMessage;
import com.cc.entity.ConsultationSession;
import com.cc.entity.User;
import com.cc.enums.SessionScene;
import com.cc.exception.BusinessException;
import com.cc.mapper.ConsultationMessageMapper;
import com.cc.mapper.ConsultationSessionMapper;
import com.cc.mapper.UserMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ConsultationSessionService {
    @Resource
    private UserMapper userMapper;

    @Resource
    private ConsultationSessionMapper consultationSessionMapper;

    @Resource
    private ConsultationMessageMapper consultationMessageMapper;

    @Resource
    private ConsultationSummaryService consultationSummaryService;

    public ConsultationSession createSession(Long userId, ConsultationSessionCreateDTO createDTO) {
        // 验证用户是否存在
        User user =userMapper.selectById(userId);
        if (user != null) {
            // 校验咨询场景合法值
            SessionScene scene = SessionScene.fromCode(createDTO.getScene());
            if (scene == null) {
                throw new BusinessException("请选择有效的咨询场景");
            }
            // 创建会话记录
             ConsultationSession session = ConsultationSession.builder()
                    .userId(userId)
                    .sessionTitle(createDTO.getSessionTitle())
                    .scene(scene.getCode())
                    .startedAt(LocalDateTime.now())
                    .build();
            // 如果未提供标题，默认标题带场景前缀
            if (StrUtil.isBlank(createDTO.getSessionTitle())) {
                session.setSessionTitle(String.format("【%s】小聪AI助手 - %s",
                        scene.getLabel(), DateUtil.format(LocalDateTime.now(), "MM-dd HH:mm")));
            }

            // 插入记录
            consultationSessionMapper.insert(session);
            return session;
        }

        return null;
    }

    /**
     * 分页查询会话列表（含消息统计与用户昵称）
     *
     * <p>管理员查询全部用户的会话，普通用户仅查询自己的会话。</p>
     *
     * @param userId   当前登录用户ID
     * @param isAdmin  是否为管理员
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    public Page<ConsultationSessionResponseDTO> pageSessions(Long userId, boolean isAdmin, long pageNum, long pageSize) {
        Page<ConsultationSession> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ConsultationSession> queryWrapper = new LambdaQueryWrapper<>();
        if (!isAdmin) {
            queryWrapper.eq(ConsultationSession::getUserId, userId);
        }
        queryWrapper.orderByDesc(ConsultationSession::getStartedAt);
        Page<ConsultationSession> sessionPage = consultationSessionMapper.selectPage(page, queryWrapper);

        List<ConsultationSessionResponseDTO> records = sessionPage.getRecords().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
        if (!records.isEmpty()) {
            fillSessionStats(records);
        }

        Page<ConsultationSessionResponseDTO> resultPage = new Page<>(sessionPage.getCurrent(), sessionPage.getSize(), sessionPage.getTotal());
        resultPage.setRecords(records);
        return resultPage;
    }

    /**
     * 删除会话（消息记录通过外键级联删除）
     *
     * @param sessionId 会话ID
     * @param userId    当前登录用户ID
     * @param isAdmin   是否为管理员
     */
    public void deleteSession(Long sessionId, Long userId, boolean isAdmin) {
        getSessionOwned(sessionId, userId, isAdmin);
        consultationSessionMapper.deleteById(sessionId);
    }

    /**
     * 获取会话并校验归属权：会话不存在时按原ID重建后返回（前端页面可能仍持有已删除会话的引用，
     * 重建可让当前对话无缝继续）；会话存在但不属于当前用户（且非管理员）时抛出业务异常
     *
     * @param sessionId 会话ID
     * @param userId    当前登录用户ID
     * @param isAdmin   是否为管理员
     * @return 会话实体（存在或重建后）
     */
    public ConsultationSession getSessionOrRebuild(Long sessionId, Long userId, boolean isAdmin) {
        ConsultationSession session = consultationSessionMapper.selectById(sessionId);
        if (session == null) {
            // 按原ID重建会话；并发请求重复插入时主键冲突，忽略即可
            ConsultationSession rebuilt = ConsultationSession.builder()
                    .id(sessionId)
                    .userId(userId)
                    .sessionTitle(String.format("小聪AI助手 - " + DateUtil.format(LocalDateTime.now(), "MM-dd HH:mm")))
                    .scene(SessionScene.OTHER.getCode())
                    .startedAt(LocalDateTime.now())
                    .build();
            try {
                consultationSessionMapper.insertWithId(rebuilt);
            } catch (Exception e) {
                log.debug("并发重建会话冲突, sessionId={}", sessionId);
            }
            return consultationSessionMapper.selectById(sessionId);
        }
        if (!isAdmin && !session.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该会话");
        }
        return session;
    }

    /**
     * 获取会话并校验归属权：会话不存在或非本人（且非管理员）时抛出业务异常
     *
     * @param sessionId 会话ID
     * @param userId    当前登录用户ID
     * @param isAdmin   是否为管理员
     * @return 会话实体
     */
    public ConsultationSession getSessionOwned(Long sessionId, Long userId, boolean isAdmin) {
        ConsultationSession session = consultationSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        if (!isAdmin && !session.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该会话");
        }
        return session;
    }

    /**
     * 获取会话情绪分析结果，返回最近一次存储的分析 JSON；无分析数据时返回默认值
     *
     * @param sessionIdParam 会话ID参数，支持 "session_1" 或纯数字 "1"
     * @param userId         当前登录用户ID（归属校验）
     * @param isAdmin        是否为管理员
     * @return 情绪分析结果
     */
    public EmotionAnalysisDTO getSessionEmotion(String sessionIdParam, Long userId, boolean isAdmin) {
        Long sessionId = parseSessionId(sessionIdParam);
        if (sessionId == null) {
            throw new BusinessException("会话ID格式错误");
        }
        ConsultationSession session = consultationSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        // 归属校验：仅会话所属用户（或管理员）可查看情绪分析
        if (!isAdmin && !session.getUserId().equals(userId)) {
            throw new BusinessException("无权查看该会话");
        }
        if (StringUtils.hasText(session.getLastEmotionAnalysis())) {
            try {
                EmotionAnalysisDTO analysis = JSONUtil.toBean(session.getLastEmotionAnalysis(), EmotionAnalysisDTO.class);
                if (analysis != null && StringUtils.hasText(analysis.getPrimaryEmotion())) {
                    return analysis;
                }
            } catch (Exception e) {
                // 历史数据格式异常时降级返回默认值
            }
        }
        return EmotionAnalysisDTO.defaultAnalysis();
    }

    /**
     * 触发会话总结：已有总结直接返回；否则异步生成并立即返回"生成中"状态
     *
     * @param sessionId 会话ID
     * @param userId    当前登录用户ID
     * @param isAdmin   是否为管理员
     * @return 总结内容（READY）或生成中状态（GENERATING）
     */
    public SessionSummaryResponseDTO triggerSummary(Long sessionId, Long userId, boolean isAdmin) {
        ConsultationSession session = getSessionOwned(sessionId, userId, isAdmin);
        SessionSummaryResponseDTO existing = parseSummary(session);
        if (existing != null) {
            log.info("会话总结已存在，直接返回, sessionId={}", sessionId);
            return existing;
        }
        // 异步生成，接口立即返回，不阻塞 HTTP 响应
        consultationSummaryService.generateSummaryAsync(sessionId);
        log.info("会话总结生成已触发, sessionId={}", sessionId);
        return generatingSummary();
    }

    /**
     * 获取会话总结：无总结时返回"生成中"状态
     *
     * @param sessionId 会话ID
     * @param userId    当前登录用户ID
     * @param isAdmin   是否为管理员
     * @return 总结内容（READY）或生成中状态（GENERATING）
     */
    public SessionSummaryResponseDTO getSummary(Long sessionId, Long userId, boolean isAdmin) {
        ConsultationSession session = getSessionOwned(sessionId, userId, isAdmin);
        SessionSummaryResponseDTO existing = parseSummary(session);
        return existing != null ? existing : generatingSummary();
    }

    /**
     * 保存会话评价反馈（同一会话重复提交覆盖更新，幂等）
     *
     * @param sessionId 会话ID
     * @param userId    当前登录用户ID
     * @param isAdmin   是否为管理员
     * @param feedbackDTO 评分与反馈内容
     */
    public void saveFeedback(Long sessionId, Long userId, boolean isAdmin, SessionFeedbackDTO feedbackDTO) {
        getSessionOwned(sessionId, userId, isAdmin);
        ConsultationSession update = ConsultationSession.builder()
                .id(sessionId)
                .rating(feedbackDTO.getRating())
                .feedback(StringUtils.hasText(feedbackDTO.getFeedback()) ? feedbackDTO.getFeedback().trim() : null)
                .build();
        consultationSessionMapper.updateById(update);
        log.info("会话评价已保存, sessionId={}", sessionId);
    }

    /**
     * 转人工引导（模板实现）：校验归属后返回温和的共情文案与专业求助资源
     *
     * @param sessionId 会话ID
     * @param userId    当前登录用户ID
     * @param isAdmin   是否为管理员
     * @return 转人工引导内容
     */
    public HumanHandoffResponseDTO requestHumanHandoff(Long sessionId, Long userId, boolean isAdmin) {
        getSessionOwned(sessionId, userId, isAdmin);
        log.info("用户请求转人工引导, sessionId={}", sessionId);
        return HumanHandoffResponseDTO.builder()
                .handoffAvailable(false)
                .message("谢谢你愿意把感受告诉我，我会一直在这里陪伴你。你的情况值得更专业的支持，和专业人员聊聊会很有帮助。")
                .hotline("全国心理援助热线 12356（号码信息请以官方渠道公布为准）")
                .suggestion("建议预约学校心理中心或正规医院心理科，与专业咨询师面对面聊聊")
                .build();
    }

    /**
     * 解析会话实体中存储的总结 JSON，无效或不存在时返回 null
     */
    private SessionSummaryResponseDTO parseSummary(ConsultationSession session) {
        if (!StringUtils.hasText(session.getSummary())) {
            return null;
        }
        try {
            SessionSummaryResponseDTO dto = JSONUtil.toBean(session.getSummary(), SessionSummaryResponseDTO.class);
            if (dto != null && StringUtils.hasText(dto.getSummary())) {
                dto.setStatus("READY");
                if (dto.getCoreIssues() == null) {
                    dto.setCoreIssues(List.of());
                }
                if (dto.getSuggestions() == null) {
                    dto.setSuggestions(List.of());
                }
                return dto;
            }
        } catch (Exception e) {
            log.warn("会话总结解析失败, sessionId={}", session.getId());
        }
        return null;
    }

    /**
     * 构建"生成中"状态的总结响应
     */
    private SessionSummaryResponseDTO generatingSummary() {
        return SessionSummaryResponseDTO.builder()
                .status("GENERATING")
                .summary("")
                .coreIssues(List.of())
                .suggestions(List.of())
                .build();
    }

    /**
     * 解析会话ID参数，支持 "session_1" 前缀格式与纯数字格式
     */
    private Long parseSessionId(String sessionIdParam) {
        if (!StringUtils.hasText(sessionIdParam)) {
            return null;
        }
        try {
            if (sessionIdParam.startsWith("session_")) {
                return Long.parseLong(sessionIdParam.substring("session_".length()));
            }
            return Long.parseLong(sessionIdParam);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 批量填充会话的消息统计（消息数、最后一条消息内容与时间）和用户昵称
     */
    private void fillSessionStats(List<ConsultationSessionResponseDTO> records) {
        List<Long> sessionIds = records.stream()
                .map(ConsultationSessionResponseDTO::getId)
                .collect(Collectors.toList());

        // 消息数与最后消息时间（按会话分组聚合）
        QueryWrapper<ConsultationMessage> statQuery = new QueryWrapper<>();
        statQuery.select("session_id", "COUNT(*) AS message_count", "MAX(created_at) AS last_message_time")
                .in("session_id", sessionIds)
                .groupBy("session_id");
        Map<Long, Map<String, Object>> statMap = new HashMap<>();
        for (Map<String, Object> row : consultationMessageMapper.selectMaps(statQuery)) {
            Object sessionId = row.get("session_id");
            if (sessionId instanceof Number number) {
                statMap.put(number.longValue(), row);
            }
        }

        // 用户昵称
        List<Long> userIds = records.stream()
                .map(ConsultationSessionResponseDTO::getUserId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, String> nicknameMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, user ->
                        user.getNickname() != null && !user.getNickname().trim().isEmpty() ? user.getNickname() : user.getUsername()));

        for (ConsultationSessionResponseDTO record : records) {
            Map<String, Object> stat = statMap.get(record.getId());
            if (stat != null) {
                Object messageCount = stat.get("message_count");
                if (messageCount instanceof Number number) {
                    record.setMessageCount(number.intValue());
                }
                Object lastMessageTime = stat.get("last_message_time");
                if (lastMessageTime instanceof LocalDateTime time) {
                    record.setLastMessageTime(time);
                }
            }
            // 最后一条消息内容（利用会话消息索引，逐会话取最新一条）
            QueryWrapper<ConsultationMessage> lastQuery = new QueryWrapper<>();
            lastQuery.select("content")
                    .eq("session_id", record.getId())
                    .orderByDesc("id")
                    .last("limit 1");
            List<Map<String, Object>> lastRows = consultationMessageMapper.selectMaps(lastQuery);
            if (!lastRows.isEmpty() && lastRows.get(0).get("content") != null) {
                record.setLastMessageContent(lastRows.get(0).get("content").toString());
            }
            record.setUserNickname(nicknameMap.get(record.getUserId()));
        }
    }

    /**
     * 会话实体转换为响应 DTO
     */
    private ConsultationSessionResponseDTO convertToResponseDTO(ConsultationSession session) {
        ConsultationSessionResponseDTO dto = new ConsultationSessionResponseDTO();
        dto.setId(session.getId());
        dto.setUserId(session.getUserId());
        dto.setSessionTitle(session.getSessionTitle());
        dto.setStartedAt(session.getStartedAt());
        // 场景信息（历史数据 scene 为空时按 OTHER 兜底）
        SessionScene scene = SessionScene.fromCode(session.getScene());
        if (scene == null) {
            scene = SessionScene.OTHER;
        }
        dto.setScene(scene.getCode());
        dto.setSceneLabel(scene.getLabel());
        dto.setSceneIcon(scene.getIcon());
        // 评价与总结状态
        dto.setRating(session.getRating());
        dto.setHasSummary(StringUtils.hasText(session.getSummary()));
        return dto;
    }
}
