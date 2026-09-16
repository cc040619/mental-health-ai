package com.cc.dto.response;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 咨询师列表卡片 DTO
 *
 * @author cc
 * @date 2026-09-14
 */
@Data
public class ConsultantListResponseDTO {

    private Long id;

    private String name;

    private String avatar;

    private String title;

    /** 擅长领域，逗号分隔 */
    private String specialties;

    private Integer yearsOfPractice;

    private BigDecimal rating;

    private Integer reviewCount;

    private Integer consultCount;
}
