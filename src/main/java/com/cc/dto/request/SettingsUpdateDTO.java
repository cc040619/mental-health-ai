package com.cc.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * 系统配置批量保存 DTO（key 白名单校验在 Service 层）
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class SettingsUpdateDTO {

    @Valid
    @Size(max = 100, message = "单次保存配置项不能超过 100 条")
    private List<ConfigItem> items;

    @Data
    public static class ConfigItem {

        @NotBlank(message = "配置键不能为空")
        @Size(max = 50, message = "配置键长度不能超过 50")
        private String configKey;

        @Size(max = 1000, message = "配置值长度不能超过 1000")
        private String configValue;
    }
}
