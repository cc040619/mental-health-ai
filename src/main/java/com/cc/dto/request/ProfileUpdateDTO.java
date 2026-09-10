package com.cc.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

/**
 * 个人资料更新请求 DTO
 *
 * @author cc
 * @date 2026-09-07
 */
@Data
public class ProfileUpdateDTO {

    // 昵称
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    // 头像URL
    @Size(max = 500, message = "头像路径长度不能超过500个字符")
    private String avatar;

    // 性别 0:未知 1:男 2:女
    private Integer gender;

    // 生日
    private LocalDate birthday;

    // 个人简介
    @Size(max = 500, message = "个人简介长度不能超过500个字符")
    private String bio;
}
