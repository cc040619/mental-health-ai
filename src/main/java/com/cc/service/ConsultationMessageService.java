package com.cc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.response.ConsultationMessageResponseDTO;
import com.cc.dto.response.FavoriteMessageResponseDTO;
import com.cc.entity.ConsultationMessage;
import com.cc.entity.ConsultationSession;
import com.cc.exception.BusinessException;
import com.cc.mapper.ConsultationMessageMapper;
import com.cc.mapper.ConsultationSessionMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ConsultationMessageService {
    @Resource
    private ConsultationMessageMapper consultationMessageMapper;

    // 归属校验与收藏分页需要查询会话表（仅依赖 Mapper，避免 Service 间循环依赖）
    @Resource
    private ConsultationSessionMapper consultationSessionMapper;

    public ConsultationMessage saveUserMessage(Long sessionId, String content, String emotion_tag) {
        // 构建用户消息实体
         ConsultationMessage userMessage = ConsultationMessage.builder()
            .sessionId(sessionId)
            .senderType(1)
            .messageType(1)
            .content(content)
            .emotionTag(emotion_tag)
            .isFavorite(0)
            .createdAt(LocalDateTime.now())
            .build();

        consultationMessageMapper.insert(userMessage);
        return userMessage;
    }

    public ConsultationMessage saveAimessage(Long sessionId, String content, String aiModel) {
        ConsultationMessage message = ConsultationMessage.builder()
                .sessionId(sessionId)
                .senderType(2)
                .messageType(1)
                .content(content)
                .aiModel(aiModel)
                .isFavorite(0)
                .createdAt(LocalDateTime.now())
                .build();
        // 插入数据库
        consultationMessageMapper.insert(message);
        return message;
    }

    public Integer getMessageCountBySessionId(Long sessionId) {
        LambdaQueryWrapper<ConsultationMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConsultationMessage::getSessionId, sessionId);

       Long count = consultationMessageMapper.selectCount(queryWrapper);
       return count.intValue();
    }

    // 获取会话的最后一条消息
    public ConsultationMessageResponseDTO getLastMessageBySessionId(Long sessionId) {
        LambdaQueryWrapper<ConsultationMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConsultationMessage::getSessionId, sessionId)
                .orderByDesc(ConsultationMessage::getCreatedAt)
                .last("limit 1");

        ConsultationMessage lastMessage = consultationMessageMapper.selectOne(queryWrapper);
        return lastMessage != null ? convertToResponseDTO(lastMessage) : null;

    }

    /**
     * 查询会话的全部消息（按时间升序），供会话消息接口与情绪分析使用
     *
     * @param sessionId 会话ID
     * @return 消息响应列表
     */
    public List<ConsultationMessageResponseDTO> listBySessionId(Long sessionId) {
        LambdaQueryWrapper<ConsultationMessage> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ConsultationMessage::getSessionId, sessionId)
                .orderByAsc(ConsultationMessage::getCreatedAt)
                .orderByAsc(ConsultationMessage::getId);

        return consultationMessageMapper.selectList(queryWrapper).stream()
                .map(this::convertToResponseDTO)
                .toList();
    }

    /**
     * 切换消息收藏状态（仅允许收藏 AI 回复），返回切换后的收藏状态
     *
     * <p>归属校验：消息所在会话必须属于当前用户（管理员可查看全部）。</p>
     *
     * @param messageId 消息ID
     * @param userId    当前登录用户ID
     * @param isAdmin   是否为管理员
     * @return true=已收藏 false=已取消
     */
    public Boolean toggleFavorite(Long messageId, Long userId, boolean isAdmin) {
        ConsultationMessage message = consultationMessageMapper.selectById(messageId);
        if (message == null) {
            throw new BusinessException("消息不存在");
        }
        // 校验消息所在会话的归属权
        ConsultationSession session = consultationSessionMapper.selectById(message.getSessionId());
        if (session == null) {
            throw new BusinessException("会话不存在");
        }
        if (!isAdmin && !session.getUserId().equals(userId)) {
            throw new BusinessException("无权操作该消息");
        }
        // 仅 AI 回复可收藏
        if (message.getSenderType() == null || message.getSenderType() != 2) {
            throw new BusinessException("只能收藏AI助手的回复");
        }

        boolean nowFavorite = message.getIsFavorite() == null || message.getIsFavorite() == 0;
        ConsultationMessage update = ConsultationMessage.builder()
                .id(messageId)
                .isFavorite(nowFavorite ? 1 : 0)
                .favoritedAt(nowFavorite ? LocalDateTime.now() : null)
                .build();
        consultationMessageMapper.updateById(update);
        return nowFavorite;
    }

    /**
     * 分页查询当前用户收藏的 AI 回复
     *
     * <p>先单表查用户名下会话ID集合，再单表分页查收藏消息（走 idx_session_favorite 索引），
     * 最后内存拼装会话标题，避免大结果集联表。</p>
     *
     * @param userId   当前登录用户ID
     * @param pageNum  页码
     * @param pageSize 每页条数（调用方已限制上限）
     * @return 收藏消息分页
     */
    public Page<FavoriteMessageResponseDTO> pageFavorites(Long userId, long pageNum, long pageSize) {
        Page<FavoriteMessageResponseDTO> resultPage = new Page<>(pageNum, pageSize);

        // 当前用户的全部会话（只取 id 与标题，轻量查询）
        LambdaQueryWrapper<ConsultationSession> sessionQuery = new LambdaQueryWrapper<>();
        sessionQuery.eq(ConsultationSession::getUserId, userId)
                .select(ConsultationSession::getId, ConsultationSession::getSessionTitle);
        List<ConsultationSession> sessions = consultationSessionMapper.selectList(sessionQuery);
        if (sessions.isEmpty()) {
            resultPage.setRecords(List.of());
            resultPage.setTotal(0);
            return resultPage;
        }
        Map<Long, String> titleMap = sessions.stream()
                .collect(Collectors.toMap(ConsultationSession::getId,
                        s -> s.getSessionTitle() != null ? s.getSessionTitle() : "心理咨询会话"));
        List<Long> sessionIds = new ArrayList<>(titleMap.keySet());

        // 分页查询收藏的 AI 消息（按收藏时间倒序）
        Page<ConsultationMessage> messagePage = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<ConsultationMessage> messageQuery = new LambdaQueryWrapper<>();
        messageQuery.eq(ConsultationMessage::getIsFavorite, 1)
                .eq(ConsultationMessage::getSenderType, 2)
                .in(ConsultationMessage::getSessionId, sessionIds)
                .orderByDesc(ConsultationMessage::getFavoritedAt)
                .orderByDesc(ConsultationMessage::getId);
        Page<ConsultationMessage> favoritePage = consultationMessageMapper.selectPage(messagePage, messageQuery);

        List<FavoriteMessageResponseDTO> records = favoritePage.getRecords().stream()
                .map(message -> FavoriteMessageResponseDTO.builder()
                        .id(message.getId())
                        .content(message.getContent())
                        .sessionId(message.getSessionId())
                        .sessionTitle(titleMap.get(message.getSessionId()))
                        .favoritedAt(message.getFavoritedAt() != null ? message.getFavoritedAt() : message.getCreatedAt())
                        .build())
                .toList();

        resultPage.setRecords(records);
        resultPage.setTotal(favoritePage.getTotal());
        return resultPage;
    }

    private ConsultationMessageResponseDTO convertToResponseDTO(ConsultationMessage message) {
        if (message == null) {
            return null;
        }

        // 手动逐字段赋值，确保转换的准确性和可控性
        ConsultationMessageResponseDTO responseDTO = new ConsultationMessageResponseDTO();
        responseDTO.setId(message.getId());
        responseDTO.setSessionId(message.getSessionId());
        responseDTO.setSenderType(message.getSenderType());
        responseDTO.setMessageType(message.getMessageType());
        responseDTO.setContent(message.getContent());
        responseDTO.setEmotionTag(message.getEmotionTag());
        responseDTO.setAiModel(message.getAiModel());
        responseDTO.setIsFavorite(message.getIsFavorite() != null ? message.getIsFavorite() : 0);
        responseDTO.setCreatedAt(message.getCreatedAt());

        // 设置描述字段（通过实体方法获取）
        responseDTO.setSenderTypeDesc(message.getSenderTypeDesc());
        responseDTO.setMessageTypeDesc(message.getMessageTypeDesc());

        // 计算消息长度
        responseDTO.calculateContentLength();

        return responseDTO;
    }
}
