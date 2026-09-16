package com.cc.controller;

import com.cc.common.Result;
import com.cc.dto.request.AdminUserStatusDTO;
import com.cc.dto.response.AdminUserDetailResponseDTO;
import com.cc.dto.response.AdminUserStatsResponseDTO;
import com.cc.service.AdminUserService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * 管理端用户管理控制器（管理员）
 *
 * <p>前缀 /api/admin/user，全部接口仅管理员（user_type=2）可访问。
 * 强制下线/禁用/重置密码统一调 {@link com.cc.service.TokenService#removeLoginToken(Long)}
 * 让 Redis 白名单 token 立即失效。</p>
 *
 * @author cc
 * @date 2026-09-15
 */
@RestController
@RequestMapping("/api/admin/user")
public class AdminUserController {

    @Resource
    private AdminUserService adminUserService;

    /** 用户列表：keyword/phone/status/startDate/endDate/page/size，手机号脱敏 */
    @GetMapping("/list")
    @PreAuthorize("hasRole('2')")
    public Result<Map<String, Object>> list(@RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String phone,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(required = false) String startDate,
                                            @RequestParam(required = false) String endDate,
                                            @RequestParam(required = false, defaultValue = "1") Long page,
                                            @RequestParam(required = false, defaultValue = "10") Long size) {
        return Result.ok(adminUserService.listUsers(keyword, phone, status, startDate, endDate, page, size));
    }

    /** 用户详情：基本信息（完整手机号/邮箱）+ 简要统计 */
    @GetMapping("/{id}/detail")
    @PreAuthorize("hasRole('2')")
    public Result<AdminUserDetailResponseDTO> detail(@PathVariable Long id) {
        return Result.ok(adminUserService.getUserDetail(id));
    }

    /** 咨询记录（懒加载） */
    @GetMapping("/{id}/consultations")
    @PreAuthorize("hasRole('2')")
    public Result<Map<String, Object>> consultations(@PathVariable Long id,
                                                     @RequestParam(required = false, defaultValue = "1") Long page,
                                                     @RequestParam(required = false, defaultValue = "10") Long size) {
        return Result.ok(adminUserService.listUserConsultations(id, page, size));
    }

    /** 测评记录（懒加载） */
    @GetMapping("/{id}/assessments")
    @PreAuthorize("hasRole('2')")
    public Result<Map<String, Object>> assessments(@PathVariable Long id,
                                                    @RequestParam(required = false, defaultValue = "1") Long page,
                                                    @RequestParam(required = false, defaultValue = "10") Long size) {
        return Result.ok(adminUserService.listUserAssessments(id, page, size));
    }

    /** 情绪日记（懒加载） */
    @GetMapping("/{id}/diaries")
    @PreAuthorize("hasRole('2')")
    public Result<Map<String, Object>> diaries(@PathVariable Long id,
                                               @RequestParam(required = false, defaultValue = "1") Long page,
                                               @RequestParam(required = false, defaultValue = "10") Long size) {
        return Result.ok(adminUserService.listUserDiaries(id, page, size));
    }

    /** 发帖记录（懒加载） */
    @GetMapping("/{id}/posts")
    @PreAuthorize("hasRole('2')")
    public Result<Map<String, Object>> posts(@PathVariable Long id,
                                             @RequestParam(required = false, defaultValue = "1") Long page,
                                             @RequestParam(required = false, defaultValue = "10") Long size) {
        return Result.ok(adminUserService.listUserPosts(id, page, size));
    }

    /** 预约记录（懒加载） */
    @GetMapping("/{id}/appointments")
    @PreAuthorize("hasRole('2')")
    public Result<Map<String, Object>> appointments(@PathVariable Long id,
                                                    @RequestParam(required = false, defaultValue = "1") Long page,
                                                    @RequestParam(required = false, defaultValue = "10") Long size) {
        return Result.ok(adminUserService.listUserAppointments(id, page, size));
    }

    /** 禁用/启用：禁用时立即清 Redis 白名单 token；禁止管理员对自己操作 */
    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('2')")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody AdminUserStatusDTO dto) {
        adminUserService.updateStatus(id, dto.getStatus());
        return Result.ok();
    }

    /** 重置密码：BCrypt 加密存储默认密码 + 立即清 Redis 白名单 token */
    @PutMapping("/{id}/password")
    @PreAuthorize("hasRole('2')")
    public Result<Void> resetPassword(@PathVariable Long id) {
        adminUserService.resetPassword(id);
        return Result.ok();
    }

    /** 强制下线：删除 Redis 白名单 token，原会话立即失效 */
    @PostMapping("/{id}/force-logout")
    @PreAuthorize("hasRole('2')")
    public Result<Void> forceLogout(@PathVariable Long id) {
        adminUserService.forceLogout(id);
        return Result.ok();
    }

    /** 用户统计：性别/年龄段/注册趋势（近30天），走 Redis 缓存 TTL 10 分钟 */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('2')")
    public Result<AdminUserStatsResponseDTO> stats() {
        return Result.ok(adminUserService.getUserStats());
    }
}
