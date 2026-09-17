package com.cc.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cc.common.Result;
import com.cc.dto.request.BrowseHistoryRecordDTO;
import com.cc.dto.request.FavoriteOperateDTO;
import com.cc.dto.request.PasswordResetDTO;
import com.cc.dto.request.PasswordUpdateDTO;
import com.cc.dto.request.PhoneBindDTO;
import com.cc.dto.request.ProfileUpdateDTO;
import com.cc.dto.request.SetPasswordDTO;
import com.cc.dto.response.BrowseHistoryResponseDTO;
import com.cc.dto.response.FavoriteResponseDTO;
import com.cc.dto.response.ProfileResponseDTO;
import com.cc.service.BrowseHistoryService;
import com.cc.service.FavoriteService;
import com.cc.service.ProfileService;
import com.cc.service.UserService;
import com.cc.util.CurrentUserUtils;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 用户个人中心控制器，提供资料、账号安全、收藏、浏览历史、注销等接口
 *
 * <p>除 password/reset（忘记密码，免登录）外，其余接口均要求登录态。</p>
 *
 * @author cc
 * @date 2026-09-07
 */
@RestController
@RequestMapping("/api/user")
public class UserCenterController {

    @Resource
    private ProfileService profileService;

    @Resource
    private FavoriteService favoriteService;

    @Resource
    private BrowseHistoryService browseHistoryService;

    @Resource
    private UserService userService;

    // ==================== 个人资料 ====================

    // 获取当前用户资料（含个人简介）
    @GetMapping("/profile")
    public Result<ProfileResponseDTO> getProfile() {
        Long userId = CurrentUserUtils.getCurrentUserId();
        return Result.ok(profileService.getProfile(userId));
    }

    // 更新个人资料（昵称/头像/性别/生日/简介）
    @PutMapping("/profile")
    public Result<ProfileResponseDTO> updateProfile(@Valid @RequestBody ProfileUpdateDTO updateDTO) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        return Result.ok(profileService.updateProfile(userId, updateDTO));
    }

    // ==================== 账号安全 ====================

    // 修改密码（登录态，校验旧密码）
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody PasswordUpdateDTO passwordDTO) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        profileService.changePassword(userId, passwordDTO);
        return Result.ok();
    }

    // 首次设置密码（仅无密码用户可用，已有密码则拒绝）
    @PutMapping("/password/set")
    public Result<Void> setPassword(@Valid @RequestBody SetPasswordDTO setPasswordDTO) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        userService.setPassword(userId, setPasswordDTO);
        return Result.ok();
    }

    // 忘记密码重置（免登录，凭邮箱验证码）
    @PutMapping("/password/reset")
    public Result<Void> resetPassword(@Valid @RequestBody PasswordResetDTO resetDTO) {
        profileService.resetPassword(resetDTO);
        return Result.ok();
    }

    // 向当前用户邮箱发送验证码（用于绑定/换绑手机号身份校验）
    @PostMapping("/phone/send-code")
    public Result<Void> sendPhoneBindCode() {
        Long userId = CurrentUserUtils.getCurrentUserId();
        profileService.sendPhoneBindCode(userId);
        return Result.ok();
    }

    // 绑定/换绑手机号
    @PutMapping("/phone")
    public Result<Void> bindPhone(@Valid @RequestBody PhoneBindDTO bindDTO) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        profileService.bindPhone(userId, bindDTO);
        return Result.ok();
    }

    // ==================== 我的收藏 ====================

    // 收藏文章
    @PostMapping("/favorite")
    public Result<Void> addFavorite(@Valid @RequestBody FavoriteOperateDTO operateDTO) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        favoriteService.addFavorite(userId, operateDTO.getArticleId());
        return Result.ok();
    }

    // 分页查询收藏列表
    @GetMapping("/favorites")
    public Result<Map<String, Object>> getFavorites(@RequestParam(value = "page", defaultValue = "1") Long page,
                                                      @RequestParam(value = "size", defaultValue = "10") Long size) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        IPage<FavoriteResponseDTO> favoritePage = favoriteService.getFavoritePage(userId, page, size);
        return Result.ok(Map.of("records", favoritePage.getRecords(), "total", favoritePage.getTotal()));
    }

    // 取消收藏
    @DeleteMapping("/favorites/{articleId}")
    public Result<Void> removeFavorite(@PathVariable String articleId) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        favoriteService.removeFavorite(userId, articleId);
        return Result.ok();
    }

    // 检查是否已收藏某篇文章（文章详情页使用）
    @GetMapping("/favorites/check/{articleId}")
    public Result<Map<String, Object>> checkFavorite(@PathVariable String articleId) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        boolean favorited = favoriteService.isFavorited(userId, articleId);
        return Result.ok(Map.of("favorited", favorited));
    }

    // ==================== 浏览历史 ====================

    // 记录浏览历史（用户访问文章详情时上报）
    @PostMapping("/history")
    public Result<Void> recordHistory(@Valid @RequestBody BrowseHistoryRecordDTO recordDTO) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        browseHistoryService.recordHistory(userId, recordDTO.getArticleId());
        return Result.ok();
    }

    // 分页查询浏览历史
    @GetMapping("/history")
    public Result<Map<String, Object>> getHistory(@RequestParam(value = "page", defaultValue = "1") Long page,
                                                  @RequestParam(value = "size", defaultValue = "10") Long size) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        IPage<BrowseHistoryResponseDTO> historyPage = browseHistoryService.getHistoryPage(userId, page, size);
        return Result.ok(Map.of("records", historyPage.getRecords(), "total", historyPage.getTotal()));
    }

    // 删除单条浏览历史
    @DeleteMapping("/history/{id}")
    public Result<Void> deleteHistory(@PathVariable Long id) {
        Long userId = CurrentUserUtils.getCurrentUserId();
        browseHistoryService.deleteHistory(userId, id);
        return Result.ok();
    }

    // 清空浏览历史
    @DeleteMapping("/history")
    public Result<Void> clearHistory() {
        Long userId = CurrentUserUtils.getCurrentUserId();
        browseHistoryService.clearHistory(userId);
        return Result.ok();
    }

    // ==================== 账号注销 ====================

    // 注销账号（软删除）
    @DeleteMapping("/account")
    public Result<Void> deactivateAccount() {
        Long userId = CurrentUserUtils.getCurrentUserId();
        profileService.deactivateAccount(userId);
        return Result.ok();
    }
}
