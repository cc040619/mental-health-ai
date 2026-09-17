package com.cc.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.cc.dto.request.EmailLoginRequestDTO;
import com.cc.dto.request.EmailRegisterDTO;
import com.cc.dto.request.SendCodeRequestDTO;
import com.cc.dto.request.UserLoginRequestDTO;
import com.cc.dto.request.UserRegisterCommandDTO;
import com.cc.dto.response.UserLoginResponseDTO;
import com.cc.common.Result;
import com.cc.service.OperationLogService;
import com.cc.service.UserService;
import com.cc.util.CurrentUserUtils;
import com.cc.util.JwtTokenUtil;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户相关接口控制器
 *
 * @author cc
 * @date 2026-09-01
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private OperationLogService operationLogService;

    // 用户登录接口
    @PostMapping("/login")
    public Result<UserLoginResponseDTO> login(@Valid @RequestBody UserLoginRequestDTO userDTO){
        UserLoginResponseDTO result = userService.login(userDTO);
        // 登录成功显式记操作日志（不在 /api/admin 前缀内，AOP 不覆盖）
        operationLogService.record(result.getUserInfo().getId(), result.getUserInfo().getUsername(),
                "LOGIN", "账号密码登录", CurrentUserUtils.getCurrentClientIp());
        return  Result.ok(result);
    }

    // 发送邮箱登录验证码接口
    @PostMapping("/email/send-code")
    public Result<Void> sendEmailCode(@Valid @RequestBody SendCodeRequestDTO requestDTO) {
        userService.sendEmailLoginCode(requestDTO.getEmail());
        return Result.ok();
    }

    // 邮箱验证码登录接口
    @PostMapping("/email/login")
    public Result<UserLoginResponseDTO> emailLogin(@Valid @RequestBody EmailLoginRequestDTO requestDTO) {
        UserLoginResponseDTO result = userService.emailLogin(requestDTO);
        operationLogService.record(result.getUserInfo().getId(), result.getUserInfo().getUsername(),
                "LOGIN", "邮箱验证码登录", CurrentUserUtils.getCurrentClientIp());
        return Result.ok(result);
    }

    // 发送邮箱注册验证码（仅未注册邮箱实际发送，已注册邮箱返回成功但不发送——防枚举）
    @PostMapping("/email/send-register-code")
    public Result<Void> sendRegisterCode(@Valid @RequestBody SendCodeRequestDTO requestDTO) {
        userService.sendRegisterCode(requestDTO.getEmail());
        return Result.ok();
    }

    // 邮箱验证码注册并登录（审核开启时返回待审核标记，不签发 token）
    @PostMapping("/email/register")
    public Result<UserLoginResponseDTO> registerByEmail(@Valid @RequestBody EmailRegisterDTO requestDTO) {
        UserLoginResponseDTO result = userService.registerByEmail(requestDTO);
        // 仅在成功签发 token 时记操作日志（审核待通过不记登录日志）
        if (result.getToken() != null && result.getUserInfo() != null) {
            operationLogService.record(result.getUserInfo().getId(), result.getUserInfo().getUsername(),
                    "REGISTER", "邮箱注册并登录", CurrentUserUtils.getCurrentClientIp());
        }
        return Result.ok(result);
    }

    // 用户登出接口
    @PostMapping("/logout")
    public Result<Void> logout() {
        userService.logout();
        try {
            operationLogService.record(CurrentUserUtils.getCurrentUserId(),
                    CurrentUserUtils.getCurrentUsername(), "LOGOUT", "退出登录",
                    CurrentUserUtils.getCurrentClientIp());
        } catch (Exception e) {
            // token 已失效时跳过日志，不影响登出主流程
            log.warn("登出操作日志记录失败: {}", e.getMessage());
        }
        return Result.ok();
    }

    // 用户注册接口
    @PostMapping("/add")
    public Result<UserLoginResponseDTO.UserDetailResponseDTO> register(@Valid @RequestBody UserRegisterCommandDTO commandDTO) {
        UserLoginResponseDTO.UserDetailResponseDTO result = userService.register(commandDTO);
        return Result.ok(result);
    }

    // 获取当前用户
    @GetMapping("/current")
    public Result<UserLoginResponseDTO.UserDetailResponseDTO> getCurrentUser() {
        // 如何从token中解析出用户的id
        String token = JwtTokenUtil.getCurrentToken();
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
        // 调用service层获取用户详情
        UserLoginResponseDTO.UserDetailResponseDTO result = userService.getUserById(userId);
        return Result.ok(result);
    }
}
