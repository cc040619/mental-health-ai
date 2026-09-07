package com.cc.service;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.request.ConsultationSessionCreateDTO;
import com.cc.dto.response.ConsultationSessionResponseDTO;
import com.cc.dto.response.EmotionAnalysisDTO;
import com.cc.entity.ConsultationMessage;
import com.cc.entity.ConsultationSession;
import com.cc.entity.User;
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

    public ConsultationSession createSession(Long userId, ConsultationSessionCreateDTO createDTO) {
        // 验证用户是否存在
        User user =userMapper.selectById(userId);
        if (user != null) {
            // 创建会话记录
             ConsultationSession session = ConsultationSession.builder()
                    .userId(userId)
                    .sessionTitle(createDTO.getSessionTitle())
                    .startedAt(LocalDateTime.now())
                    .build();
            // 如果未提供标题
            if (StrUtil.isBlank(createDTO.getSessionTitle())) {
                session.setSessionTitle(String.format("小聪AI助手 - " + DateUtil.format(LocalDateTime.now(), "MM-dd HH:mm")));
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
     * @return 情绪分析结果
     */
    public EmotionAnalysisDTO getSessionEmotion(String sessionIdParam) {
        Long sessionId = parseSessionId(sessionIdParam);
        if (sessionId == null) {
            throw new BusinessException("会话ID格式错误");
        }
        ConsultationSession session = consultationSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException("会话不存在");
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
        return dto;
    }
}
