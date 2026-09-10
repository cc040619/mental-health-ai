package com.cc.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.dto.request.EmailLoginRequestDTO;
import com.cc.dto.request.UserLoginRequestDTO;
import com.cc.dto.request.UserRegisterCommandDTO;
import com.cc.dto.response.UserLoginResponseDTO;
import com.cc.entity.User;
import com.cc.enums.UserType;
import com.cc.exception.BusinessException;
import com.cc.mapper.UserMapper;
import com.cc.service.convert.UserConvert;
import com.cc.util.JwtTokenUtil;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 用户业务层，处理登录、注册、查询用户等业务逻辑
 *
 * @author cc
 * @date 2026-09-01
 */
@Service
public class UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private TokenService tokenService;

    @Resource
    private EmailService emailService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /**
     * 登录接口逻辑
     */
    public UserLoginResponseDTO login(UserLoginRequestDTO userDTO) {
        //构建查询器
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        //查询条件
        queryWrapper.eq(User::getUsername, userDTO.getUsername())
                .or().eq(User::getPassword, userDTO.getPassword());
        //调用api查询
        User user = userMapper.selectOne(queryWrapper);

        //判断用户是否存在
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 验证密码
        String inputPassword = userDTO.getPassword().trim();
        if (!passwordEncoder.matches(inputPassword, user.getPassword())) {
            throw new BusinessException("密码错误");
        }

        // 检查用户的状态
        if (!user.isActive()) {
            throw new BusinessException("用户已被禁用，请联系管理员");
        }

        // 生成JWT token
        String token = JwtTokenUtil.generateToken(user.getId(), user.getUsername(), user.getUserType());

        // 将 token 写入 Redis 白名单（TTL 与 JWT 过期时间保持一致）
        tokenService.saveLoginToken(user.getId(), token);

        //构建响应体
        UserLoginResponseDTO.UserDetailResponseDTO userInfo = UserConvert.entityToDetailResponse(user);
        return UserConvert.entityToLoginResponse(token, userInfo);
    }

    /**
     * 登出接口逻辑
     *
     * <p>从 Redis 白名单移除当前用户的 token，使其旧 token 立即失效。</p>
     */
    public void logout() {
        // 从当前请求中提取 token 并解析出用户ID
        String token = JwtTokenUtil.getCurrentToken();
        if (token == null) {
            throw new BusinessException("无法获取当前登录信息");
        }
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();

        // 从 Redis 白名单移除该用户的 token
        tokenService.removeLoginToken(userId);
    }

    public UserLoginResponseDTO.UserDetailResponseDTO register(UserRegisterCommandDTO commandDTO) {
        // 验证密码是否一致
        if (!commandDTO.getPassword().equals(commandDTO.getConfirmPassword())) {
            throw new BusinessException("两次输入密码不一致");
        }

        // 检查用户名是否存在
        LambdaQueryWrapper<User> userNameQuery =  new LambdaQueryWrapper<>();
        userNameQuery.eq(User::getUsername, commandDTO.getUsername());
        if (userMapper.selectCount(userNameQuery) > 0) {
            throw new BusinessException("用户名已存在");
        }

        // 检查邮箱是否存在
        LambdaQueryWrapper<User> emailQuery =  new LambdaQueryWrapper<>();
        emailQuery.eq(User::getEmail, commandDTO.getEmail());
        if (userMapper.selectCount(emailQuery) > 0) {
            throw new BusinessException("邮箱已存在");
        }

        // 用户类型
        if (!UserType.isValidCode(commandDTO.getUserType())) {
            throw new BusinessException("无效的用户类型");
        }

        // 创建用户
        String password = commandDTO.getPassword().trim();
        String encodedPassword = passwordEncoder.encode(password);
        User user = UserConvert.registerCommandToEntity(commandDTO, encodedPassword);

        // 插入数据库
        userMapper.insert(user);

        return UserConvert.entityToDetailResponse(user);
    }

    public UserLoginResponseDTO.UserDetailResponseDTO getUserById(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return UserConvert.entityToDetailResponse(user);
    }

    /**
     * 发送邮箱登录验证码
     *
     * <p>不论邮箱是否已注册都会调用邮件服务：若邮箱未注册则不会真正发送验证码，
     * 但为防止账户枚举攻击，统一抛出业务异常时使用同一提示语。</p>
     *
     * @param email 目标邮箱
     */
    public void sendEmailLoginCode(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BusinessException("邮箱不能为空");
        }
        // 校验邮箱是否已注册
        LambdaQueryWrapper<User> emailQuery = new LambdaQueryWrapper<>();
        emailQuery.eq(User::getEmail, email);
        if (userMapper.selectCount(emailQuery) == 0) {
            throw new BusinessException("该邮箱尚未注册，请先注册");
        }
        // 调用邮件服务发送验证码
        emailService.sendLoginCode(email);
    }

    /**
     * 邮箱验证码登录逻辑
     *
     * <p>流程：校验验证码 -> 查询用户 -> 检查状态 -> 生成 JWT -> 写入 Redis 白名单 -> 返回响应</p>
     *
     * @param loginDTO 邮箱 + 验证码
     * @return 登录响应（含 token 与用户详情）
     */
    public UserLoginResponseDTO emailLogin(EmailLoginRequestDTO loginDTO) {
        // 校验验证码
        emailService.verifyLoginCode(loginDTO.getEmail(), loginDTO.getCode());

        // 查询用户
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getEmail, loginDTO.getEmail());
        User user = userMapper.selectOne(queryWrapper);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 检查用户的状态
        if (!user.isActive()) {
            throw new BusinessException("用户已被禁用，请联系管理员");
        }

        // 生成 JWT token
        String token = JwtTokenUtil.generateToken(user.getId(), user.getUsername(), user.getUserType());

        // 将 token 写入 Redis 白名单（TTL 与 JWT 过期时间保持一致）
        tokenService.saveLoginToken(user.getId(), token);

        // 构建响应体
        UserLoginResponseDTO.UserDetailResponseDTO userInfo = UserConvert.entityToDetailResponse(user);
        return UserConvert.entityToLoginResponse(token, userInfo);
    }
}
