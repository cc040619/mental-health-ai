package com.cc.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * AI 配置批量保存 DTO
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class AiConfigSaveDTO {

    @NotEmpty(message = "配置项不能为空")
    @Valid
    private List<ConfigItem> items;

    @Data
    public static class ConfigItem {

        @NotBlank(message = "配置键不能为空")
        @Size(max = 50, message = "配置键长度不能超过50")
        private String configKey;

        @Size(max = 500, message = "配置值长度不能超过500")
        private String configValue;
    }
}
