package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 咨询记录响应 DTO（咨询师本人读写 + 管理员只读，用户端不暴露）。
 *
 * <p>可见性边界：咨询师本人可读可写；管理员只读（4.8 预约详情联动展示）；
 * 用户端无任何咨询记录接口。一单一记录（appointment_id 唯一）。</p>
 *
 * @author cc
 * @date 2026-09-17
 */
@Data
public class ConsultationRecordResponseDTO {

    private Long id;

    private Long appointmentId;

    /** 主诉 */
    private String chiefComplaint;

    /** 分析 */
    private String analysis;

    /** 干预方案 */
    private String interventionPlan;

    /** 下次建议 */
    private String nextSuggestion;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
