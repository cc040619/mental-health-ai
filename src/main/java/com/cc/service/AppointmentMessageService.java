package com.cc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cc.dto.response.AppointmentMessageResponseDTO;
import com.cc.entity.Appointment;
import com.cc.entity.AppointmentMessage;
import com.cc.mapper.AppointmentMessageMapper;
import com.cc.service.convert.AppointmentConvert;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * 预约留言存取：WebSocket 与 HTTP 兜底共用同一落库逻辑。
 * 消息实时性要求高，不分缓存；历史分页命中 (appointment_id, created_at) 索引。
 *
 * @author cc
 * @date 2026-09-14
 */
@Slf4j
@Service
public class AppointmentMessageService {

    /** 历史消息单页上限：进入留言室默认拉最近 50 条 */
    private static final long MAX_PAGE_SIZE = 50L;

    @Resource
    private AppointmentMessageMapper appointmentMessageMapper;
    @Resource
    private AppointmentConvert appointmentConvert;

    /**
     * 保存一条留言
     *
     * @param appointment 归属预约（调用方已完成鉴权）
     * @param senderId    发送者ID
     * @param senderType  1-用户 2-咨询师/管理员
     * @param content     留言内容（调用方保证非空且 ≤1000 字，不打印日志）
     * @return 落库后的留言 DTO（含 id/创建时间）
     */
    public AppointmentMessageResponseDTO save(Appointment appointment, Long senderId, Integer senderType, String content) {
        AppointmentMessage message = AppointmentMessage.builder()
                .appointmentId(appointment.getId())
                .senderId(senderId)
                .senderType(senderType)
                .content(content.trim())
                .build();
        appointmentMessageMapper.insert(message);
        log.info("预约留言已保存: appointmentId={}, senderType={}", appointment.getId(), senderType);
        return appointmentConvert.toMessageDTO(message);
    }

    /**
     * 留言历史：按创建时间倒序分页（最近在前），前端展示时自行反序
     */
    public Map<String, Object> pageHistory(Long appointmentId, Long page, Long size) {
        long cur = (page != null && page > 0) ? page : 1L;
        long sz = (size != null && size > 0) ? Math.min(size, MAX_PAGE_SIZE) : MAX_PAGE_SIZE;
        IPage<AppointmentMessage> result = appointmentMessageMapper.selectPage(new Page<>(cur, sz),
                new LambdaQueryWrapper<AppointmentMessage>()
                        .eq(AppointmentMessage::getAppointmentId, appointmentId)
                        .orderByDesc(AppointmentMessage::getCreatedAt)
                        .orderByDesc(AppointmentMessage::getId));
        List<AppointmentMessageResponseDTO> records = result.getRecords().stream()
                .map(appointmentConvert::toMessageDTO).toList();
        Map<String, Object> data = new HashMap<>();
        data.put("records", records);
        data.put("total", result.getTotal());
        return data;
    }
}
