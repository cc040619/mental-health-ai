package com.cc.controller;

import com.cc.common.Result;
import com.cc.dto.response.CrisisConfigResponseDTO;
import com.cc.dto.response.PublicSettingsResponseDTO;
import com.cc.service.SysConfigService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开配置接口（无需登录）：系统展示信息 + 危机干预配置
 *
 * <p>只暴露白名单 key：settings/public 不含任何开关；crisis/config 只含热线与文案。
 * 数据走 sys_config 分组缓存，前端有常量兜底，接口异常不影响页面。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@RestController
public class PublicConfigController {

    @Resource
    private SysConfigService sysConfigService;

    /** 公开系统设置（名称/Logo/备案号/客服/简介，白名单 key） */
    @GetMapping("/api/settings/public")
    public Result<PublicSettingsResponseDTO> publicSettings() {
        return Result.ok(sysConfigService.getPublicSettings());
    }

    /** 危机干预公开配置（热线数组 + 提示文案） */
    @GetMapping("/api/crisis/config")
    public Result<CrisisConfigResponseDTO> crisisConfig() {
        return Result.ok(sysConfigService.getCrisisConfig());
    }
}
