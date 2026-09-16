package com.cc.dto.response;

import lombok.Data;

/**
 * 咨询师新增/编辑结果 DTO。
 * 仅「新增」时一次性返回自动创建的登录用户名与明文初始密码（管理员线下转交，后端不再保留明文）；
 * 编辑场景 username/initialPassword 为 null。
 *
 * @author cc
 * @date 2026-09-17
 */
@Data
public class ConsultantSaveResultDTO {

    private Long id;

    /** 登录用户名（仅新增返回一次） */
    private String username;

    /** 明文初始密码（仅新增返回一次，忘记密码走重置/找回流程） */
    private String initialPassword;

    public static ConsultantSaveResultDTO of(Long id, String username, String initialPassword) {
        ConsultantSaveResultDTO dto = new ConsultantSaveResultDTO();
        dto.setId(id);
        dto.setUsername(username);
        dto.setInitialPassword(initialPassword);
        return dto;
    }

    public static ConsultantSaveResultDTO of(Long id) {
        return of(id, null, null);
    }
}
