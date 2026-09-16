package com.cc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.common.RedisConstants;
import com.cc.dto.request.BannerSaveDTO;
import com.cc.entity.Banner;
import com.cc.exception.BusinessException;
import com.cc.mapper.BannerMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 首页轮播图业务层
 *
 * <p>用户端启用列表缓存 Redis（TTL 5min，fail-open），后台任何写操作主动删缓存保证首页即时生效；
 * 管理端列表不缓存（管理即时性）。linkUrl 白名单仅 http/https，拒绝 javascript:/data: 等伪协议（防 XSS 跳转）。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@Slf4j
@Service
public class BannerService {

    @Resource
    private BannerMapper bannerMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private ObjectMapper objectMapper;

    // ==================== 用户端 ====================

    /**
     * 首页轮播列表：启用中按 sortOrder 升序（缓存 TTL 5min，后台变更删缓存）
     */
    public List<Banner> listForUser() {
        try {
            String cached = stringRedisTemplate.opsForValue().get(RedisConstants.NOTIFY_BANNER_LIST_KEY);
            if (cached != null && !cached.isBlank()) {
                return objectMapper.readValue(cached, new TypeReference<List<Banner>>() {
                });
            }
        } catch (Exception e) {
            log.warn("轮播图缓存读取异常，回源DB: {}", e.getMessage());
        }
        List<Banner> list = bannerMapper.selectList(new LambdaQueryWrapper<Banner>()
                .eq(Banner::getStatus, 1)
                .orderByAsc(Banner::getSortOrder)
                .orderByAsc(Banner::getId));
        try {
            stringRedisTemplate.opsForValue().set(RedisConstants.NOTIFY_BANNER_LIST_KEY,
                    objectMapper.writeValueAsString(list),
                    RedisConstants.NOTIFY_BANNER_LIST_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("轮播图缓存写入失败（fail-open）: {}", e.getMessage());
        }
        return list;
    }

    // ==================== 管理端 ====================

    /**
     * 管理端列表：含停用，按 sortOrder 升序（不缓存）
     */
    public List<Banner> listAdmin() {
        return bannerMapper.selectList(new LambdaQueryWrapper<Banner>()
                .orderByAsc(Banner::getSortOrder)
                .orderByAsc(Banner::getId));
    }

    /**
     * 新增轮播图（图片为 OSS 上传产物，前端 el-upload 直传后回填 URL）
     */
    public void create(BannerSaveDTO dto) {
        validateLinkUrl(dto.getLinkUrl());
        Banner banner = Banner.builder()
                .title(trimToNull(dto.getTitle()))
                .imageUrl(dto.getImageUrl().trim())
                .linkUrl(trimToNull(dto.getLinkUrl()))
                .sortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder())
                .status(1)
                .build();
        bannerMapper.insert(banner);
        evictListCache();
        log.info("新增轮播图: bannerId={}, operator=admin", banner.getId());
    }

    /**
     * 编辑轮播图（校验存在）
     */
    public void update(Long id, BannerSaveDTO dto) {
        Banner existing = bannerMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("轮播图不存在");
        }
        validateLinkUrl(dto.getLinkUrl());
        bannerMapper.updateById(Banner.builder()
                .id(id)
                .title(trimToNull(dto.getTitle()))
                .imageUrl(dto.getImageUrl().trim())
                .linkUrl(trimToNull(dto.getLinkUrl()))
                .sortOrder(dto.getSortOrder() == null ? 0 : dto.getSortOrder())
                .build());
        evictListCache();
        log.info("编辑轮播图: bannerId={}, operator=admin", id);
    }

    /**
     * 启用 / 停用（停用后首页立即不可见）
     */
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException("状态不正确");
        }
        Banner existing = bannerMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("轮播图不存在");
        }
        existing.setStatus(status);
        bannerMapper.updateById(existing);
        evictListCache();
        log.info("轮播图启停: bannerId={}, status={}, operator=admin", id, status);
    }

    /**
     * 删除轮播图（校验存在）
     */
    public void delete(Long id) {
        Banner existing = bannerMapper.selectById(id);
        if (existing == null) {
            throw new BusinessException("轮播图不存在");
        }
        bannerMapper.deleteById(id);
        evictListCache();
        log.info("删除轮播图: bannerId={}, operator=admin", id);
    }

    // ==================== 内部方法 ====================

    /** linkUrl 白名单：仅 http/https（小写比较防 JavaScript: 大小写绕过），空视为不跳转 */
    private void validateLinkUrl(String linkUrl) {
        if (linkUrl == null || linkUrl.isBlank()) {
            return;
        }
        String lower = linkUrl.trim().toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            throw new BusinessException("跳转链接仅支持 http/https 地址");
        }
    }

    private String trimToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private void evictListCache() {
        try {
            stringRedisTemplate.delete(RedisConstants.NOTIFY_BANNER_LIST_KEY);
        } catch (Exception e) {
            log.warn("删除轮播图缓存失败（fail-open）: {}", e.getMessage());
        }
    }
}
