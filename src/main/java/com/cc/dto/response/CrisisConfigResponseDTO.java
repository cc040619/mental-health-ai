package com.cc.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 危机干预公开配置响应 DTO（热线数组 + 干预提示文案，不含用户数据）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
@Builder
public class CrisisConfigResponseDTO {

    private List<Hotline> hotlines;

    private String promptText;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Hotline {
        private String name;
        private String phone;
        private String note;
    }
}
