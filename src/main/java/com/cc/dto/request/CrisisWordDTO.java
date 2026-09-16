package com.cc.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 危机词新增 DTO
 *
 * @author cc
 * @date 2026-09-15
 */
@Data
public class CrisisWordDTO {

    @NotBlank(message = "危机词不能为空")
    @Size(max = 50, message = "危机词长度不能超过 50")
    private String word;
}
