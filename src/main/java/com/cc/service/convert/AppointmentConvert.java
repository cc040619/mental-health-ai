package com.cc.service.convert;

import com.cc.dto.response.AppointmentMessageResponseDTO;
import com.cc.dto.response.AppointmentResponseDTO;
import com.cc.dto.response.ConsultationRecordResponseDTO;
import com.cc.entity.Appointment;
import com.cc.entity.AppointmentMessage;
import com.cc.entity.ConsultationRecord;
import com.cc.enums.AppointmentStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * 预约模块转换器：状态标签映射、时间规范化（HH:mm）、操作权限位计算。
 *
 * @author cc
 * @date 2026-09-14
 */
@Component
public class AppointmentConvert {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    /** 预约 Entity → DTO（详情场景，咨询师信息由 service 补充） */
    public AppointmentResponseDTO toDTO(Appointment a) {
        if (a == null) {
            return null;
        }
        AppointmentResponseDTO dto = new AppointmentResponseDTO();
        dto.setId(a.getId());
        dto.setUserId(a.getUserId());
        dto.setConsultantId(a.getConsultantId());
        dto.setSlotId(a.getSlotId());
        dto.setAppointmentDate(a.getAppointmentDate());
        dto.setStartTime(a.getStartTime().format(TIME_FORMATTER));
        dto.setEndTime(a.getEndTime().format(TIME_FORMATTER));
        dto.setProblemDesc(a.getProblemDesc());
        dto.setCancelReason(a.getCancelReason());
        dto.setStatus(a.getStatus());
        AppointmentStatus status = AppointmentStatus.valueOf(a.getStatus());
        dto.setStatusLabel(status.getLabel());
        dto.setEvaluated(Integer.valueOf(1).equals(a.getEvaluated()));
        dto.setCreatedAt(a.getCreatedAt());
        return dto;
    }

    /**
     * 列表 DTO（SQL 投影）补全状态标签与操作权限位。
     *
     * @param dto   投影 DTO（时间已由 DATE_FORMAT 规范化为 HH:mm）
     * @param owner 当前查看者是否为预约本人（后台管理员为 false）
     * @param now   当前时间
     */
    public void fillLabelsAndFlags(AppointmentResponseDTO dto, boolean owner, LocalDateTime now) {
        AppointmentStatus status = AppointmentStatus.fromCode(dto.getStatus());
        dto.setStatusLabel(status.getLabel());
        boolean beforeStart = LocalDateTime.of(dto.getAppointmentDate(),
                LocalTime.parse(dto.getStartTime(), TIME_FORMATTER)).isAfter(now);
        // PENDING 可直接取消；CONFIRMED 仅开始前可取消
        dto.setCanCancel(owner && (status == AppointmentStatus.PENDING
                || (status == AppointmentStatus.CONFIRMED && beforeStart)));
        // COMPLETED 且未评价，本人可评
        dto.setCanReview(owner && status == AppointmentStatus.COMPLETED
                && !Boolean.TRUE.equals(dto.getEvaluated()));
    }

    public AppointmentMessageResponseDTO toMessageDTO(AppointmentMessage m) {
        AppointmentMessageResponseDTO dto = new AppointmentMessageResponseDTO();
        dto.setId(m.getId());
        dto.setSenderType(m.getSenderType());
        dto.setContent(m.getContent());
        dto.setCreatedAt(m.getCreatedAt());
        return dto;
    }

    /** 咨询记录 Entity → DTO（咨询师本人读写 + 管理员只读，用户端不暴露） */
    public ConsultationRecordResponseDTO toRecordDTO(ConsultationRecord r) {
        if (r == null) {
            return null;
        }
        ConsultationRecordResponseDTO dto = new ConsultationRecordResponseDTO();
        dto.setId(r.getId());
        dto.setAppointmentId(r.getAppointmentId());
        dto.setChiefComplaint(r.getChiefComplaint());
        dto.setAnalysis(r.getAnalysis());
        dto.setInterventionPlan(r.getInterventionPlan());
        dto.setNextSuggestion(r.getNextSuggestion());
        dto.setCreatedAt(r.getCreatedAt());
        dto.setUpdatedAt(r.getUpdatedAt());
        return dto;
    }
}
