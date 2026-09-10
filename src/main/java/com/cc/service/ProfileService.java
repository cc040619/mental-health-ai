package com.cc.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.dto.request.PasswordResetDTO;
import com.cc.dto.request.PasswordUpdateDTO;
import com.cc.dto.request.PhoneBindDTO;
import com.cc.dto.request.ProfileUpdateDTO;
import com.cc.dto.response.ProfileResponseDTO;
import com.cc.entity.User;
import com.cc.enums.UserStatus;
import com.cc.exception.BusinessException;
import com.cc.mapper.UserMapper;
import com.cc.service.convert.UserConvert;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 个人中心业务层，处理资料更新、密码管理、手机号绑定与账号注销
 *
 * @author cc
 * @date 2026-09-07
 */
@Slf4j
@Service
public class ProfileService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private TokenService tokenService;

    @Resource
    private EmailService emailService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 分页大小上限，防止超大 pageSize */
    private static final int MAX_PAGE_SIZE = 50;

    /**
     * 获取当前用户个人资料（含个人简介）
     *
     * @param userId 当前登录用户ID
     * @return 个人资料响应
     */
    public ProfileResponseDTO getProfile(Long userId) {
        User user = getUserById(userId);
        return UserConvert.entityToProfileResponse(user);
    }

    /**
     * 更新个人资料（昵称/头像/性别/生日/简介）
     *
     * @param userId    当前登录用户ID
     * @param updateDTO 更新参数
     * @return 更新后的个人资料
     */
    @Transactional(rollbackFor = Exception.class)
    public ProfileResponseDTO updateProfile(Long userId, ProfileUpdateDTO updateDTO) {
        User user = getUserById(userId);

        // 按字段更新，仅更新非空字段
        if (updateDTO.getNickname() != null) {
            user.setNickname(updateDTO.getNickname().trim());
        }
        if (updateDTO.getAvatar() != null) {
            user.setAvatar(updateDTO.getAvatar());
        }
        if (updateDTO.getGender() != null) {
            user.setGender(updateDTO.getGender());
        }
        if (updateDTO.getBirthday() != null) {
            user.setBirthday(updateDTO.getBirthday());
        }
        if (updateDTO.getBio() != null) {
            user.setBio(updateDTO.getBio());
        }
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("用户资料更新成功, userId={}", userId);
        return UserConvert.entityToProfileResponse(user);
    }

    /**
     * 修改密码（登录态，校验旧密码）
     *
     * <p>修改成功后清除 Redis 中的 token 白名单，前端需引导用户重新登录。</p>
     *
     * @param userId      当前登录用户ID
     * @param passwordDTO 旧密码 + 新密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, PasswordUpdateDTO passwordDTO) {
        User user = getUserById(userId);

        // 校验旧密码
        if (!passwordEncoder.matches(passwordDTO.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        // 新旧密码不能相同
        if (passwordEncoder.matches(passwordDTO.getNewPassword(), user.getPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }

        String encodedPassword = passwordEncoder.encode(passwordDTO.getNewPassword().trim());
        user.setPassword(encodedPassword);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        // 清除 token 白名单，使旧 token 立即失效，强制重新登录
        tokenService.removeLoginToken(userId);
        log.info("用户修改密码成功, userId={}", userId);
    }

    /**
     * 忘记密码重置（免登录，凭邮箱验证码）
     *
     * @param resetDTO 邮箱 + 验证码 + 新密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(PasswordResetDTO resetDTO) {
        // 校验邮箱验证码（验证成功后会删除验证码，防止重复使用）
        boolean valid = emailService.verifyLoginCode(resetDTO.getEmail(), resetDTO.getCode());
        if (!valid) {
            throw new BusinessException("验证码错误");
        }

        // 查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getEmail, resetDTO.getEmail());
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new BusinessException("该邮箱尚未注册");
        }

        String encodedPassword = passwordEncoder.encode(resetDTO.getNewPassword().trim());
        user.setPassword(encodedPassword);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        // 若用户当前有登录态，清除 token 白名单
        tokenService.removeLoginToken(user.getId());
        log.info("用户重置密码成功, userId={}", user.getId());
    }

    /**
     * 向当前用户绑定的邮箱发送验证码（用于绑定/换绑手机号身份校验）
     *
     * @param userId 当前登录用户ID
     */
    public void sendPhoneBindCode(Long userId) {
        User user = getUserById(userId);
        if (!StringUtils.hasText(user.getEmail())) {
            throw new BusinessException("当前账号未绑定邮箱，无法发送验证码");
        }
        // 复用邮箱验证码机制，向当前用户邮箱发送验证码
        emailService.sendLoginCode(user.getEmail());
    }

    /**
     * 绑定/换绑手机号（凭当前用户邮箱验证码验证身份）
     *
     * @param userId    当前登录用户ID
     * @param bindDTO   验证码 + 新手机号
     */
    @Transactional(rollbackFor = Exception.class)
    public void bindPhone(Long userId, PhoneBindDTO bindDTO) {
        User user = getUserById(userId);
        if (!StringUtils.hasText(user.getEmail())) {
            throw new BusinessException("当前账号未绑定邮箱，无法验证身份");
        }

        // 校验邮箱验证码
        boolean valid = emailService.verifyLoginCode(user.getEmail(), bindDTO.getCode());
        if (!valid) {
            throw new BusinessException("验证码错误");
        }

        // 校验手机号是否已被其他用户绑定
        LambdaQueryWrapper<User> phoneQuery = new LambdaQueryWrapper<>();
        phoneQuery.eq(User::getPhone, bindDTO.getPhone()).ne(User::getId, userId);
        if (userMapper.selectCount(phoneQuery) > 0) {
            throw new BusinessException("该手机号已被其他账号绑定");
        }

        user.setPhone(bindDTO.getPhone());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        log.info("用户绑定手机号成功, userId={}", userId);
    }

    /**
     * 注销账号（软删除：状态置为禁用，清除 token 白名单）
     *
     * <p>不物理删除用户记录，避免外键级联删除用户的日记、会话等全部数据。</p>
     *
     * @param userId 当前登录用户ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void deactivateAccount(Long userId) {
        User user = getUserById(userId);
        user.setStatus(UserStatus.DISABLED.getCode());
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        // 清除 token 白名单，使该用户所有 token 立即失效
        tokenService.removeLoginToken(userId);
        log.info("用户注销账号成功（软删除）, userId={}", userId);
    }

    /**
     * 根据 ID 查询用户，不存在抛业务异常
     */
    private User getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }
}
