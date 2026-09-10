package com.cc.controller;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.cc.dto.request.EmailLoginRequestDTO;
import com.cc.dto.request.SendCodeRequestDTO;
import com.cc.dto.request.UserLoginRequestDTO;
import com.cc.dto.request.UserRegisterCommandDTO;
import com.cc.dto.response.UserLoginResponseDTO;
import com.cc.common.Result;
import com.cc.service.UserService;
import com.cc.util.JwtTokenUtil;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
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
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Resource
    private UserService userService;

    // 用户登录接口
    @PostMapping("/login")
    public Result<UserLoginResponseDTO> login(@Valid @RequestBody UserLoginRequestDTO userDTO){
        UserLoginResponseDTO result = userService.login(userDTO);
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
        return Result.ok(result);
    }

    // 用户登出接口
    @PostMapping("/logout")
    public Result<Void> logout() {
        userService.logout();
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
