package com.cc.controller;

import com.cc.common.Result;
import com.cc.entity.Banner;
import com.cc.service.BannerService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 首页轮播图用户端控制器：启用中按 sortOrder 升序（Redis 缓存 TTL 5min）
 *
 * @author cc
 * @date 2026-09-15
 */
@RestController
@RequestMapping("/api/banners")
public class BannerController {

    @Resource
    private BannerService bannerService;

    @GetMapping
    public Result<List<Banner>> list() {
        return Result.ok(bannerService.listForUser());
    }
}
