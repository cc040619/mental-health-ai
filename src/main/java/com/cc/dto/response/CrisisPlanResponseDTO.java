package com.cc.dto.response;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 个人安全计划响应 DTO（无记录时返回空模板，不报错）
 *
 * @author cc
 * @date 2026-09-14
 */
@Data
public class CrisisPlanResponseDTO {

    private Long id;

    private List<LovedOne> lovedOnes;

    private List<String> copingStrategies;

    private String emergencyContact;

    private LocalDateTime updatedAt;

    /**
     * 可信赖的人条目
     */
    @Data
    public static class LovedOne {
        private String name;
        private String relation;
        private String phone;
    }
}
