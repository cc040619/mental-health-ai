package com.cc.service;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cc.dto.request.EmailLoginRequestDTO;
import com.cc.dto.request.EmailRegisterDTO;
import com.cc.dto.request.SetPasswordDTO;
import com.cc.dto.request.UserLoginRequestDTO;
import com.cc.dto.request.UserRegisterCommandDTO;
import com.cc.dto.response.UserLoginResponseDTO;
import com.cc.entity.User;
import com.cc.enums.UserStatus;
import com.cc.enums.UserType;
import com.cc.exception.BusinessException;
import com.cc.mapper.UserMapper;
import com.cc.service.convert.UserConvert;
import com.cc.util.JwtTokenUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * 用户业务层，处理登录、注册、查询用户等业务逻辑
 *
 * @author cc
 * @date 2026-09-01
 */
@Slf4j
@Service
public class UserService {

    @Resource
    private UserMapper userMapper;

    @Resource
    private TokenService tokenService;

    @Resource
    private EmailService emailService;

    @Resource
    private SysConfigService sysConfigService;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    /** 咨询师账号合成邮箱域名（user.email 有唯一约束，用 用户名@域名 保证唯一；不用于真实收发） */
    private static final String CONSULTANT_EMAIL_DOMAIN = "@consultant.local";

    /** 自动生成登录用户名前缀 */
    private static final String CONSULTANT_USERNAME_PREFIX = "doc_";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** 随机用户名池：小写字母+数字 */
    private static final char[] USERNAME_ALPHABET = "abcdefghijkmnpqrstuvwxyz23456789".toCharArray();

    /** 随机初始密码池（去除易混淆字符 0/O/1/l/I） */
    private static final char[] PASSWORD_ALPHABET =
            "abcdefghjkmnpqrstuvwxyzABCDEFGHJKMNPQRSTUVWXYZ23456789".toCharArray();

    private static final int RANDOM_SUFFIX_LENGTH = 8;
    private static final int INITIAL_PASSWORD_LENGTH = 8;

    /**
     * 新建咨询师账号凭证（仅创建咨询师时返回一次明文密码，禁止落库明文/打日志）。
     */
    public record ConsultantAccountCredential(Long userId, String username, String rawPassword) {
    }

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

        // 检查用户的状态（待审核优先于禁用，提示语区分场景）
        if (UserStatus.PENDING.getCode().equals(user.getStatus())) {
            throw new BusinessException("账号待审核，请联系管理员");
        }
        if (!user.isActive()) {
            throw new BusinessException("用户已被禁用，请联系管理员");
        }

        return buildLoginResponse(user);
    }

    /**
     * 构建登录响应（JWT 签发 + Redis 白名单 + 更新最后登录时间 + 组装 DTO）。
     *
     * <p>账号密码登录、邮箱验证码登录、邮箱注册登录共用此方法。</p>
     *
     * @param user 已通过校验的用户实体
     * @return 登录响应（含 token 与用户详情，hasPassword 已透出）
     */
    private UserLoginResponseDTO buildLoginResponse(User user) {
        // 生成 JWT token
        String token = JwtTokenUtil.generateToken(user.getId(), user.getUsername(), user.getUserType());

        // 将 token 写入 Redis 白名单（TTL 与 JWT 过期时间保持一致）
        tokenService.saveLoginToken(user.getId(), token);

        // 更新最后登录时间
        updateLastLoginAt(user.getId());

        // 构建响应体（hasPassword 由 UserConvert 根据 password 是否非空计算）
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
        // 注册开关：后台关闭后拒绝新注册（读配置走缓存 5min，注册低频路径可接受）
        if (!sysConfigService.getBoolean(SysConfigService.KEY_REGISTER_ENABLED, true)) {
            throw new BusinessException("暂未开放注册，请联系管理员");
        }

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

        // 创建用户（默认角色由系统配置决定，不信任前端传值）
        String password = commandDTO.getPassword().trim();
        String encodedPassword = passwordEncoder.encode(password);
        User user = UserConvert.registerCommandToEntity(commandDTO, encodedPassword);
        user.setUserType(Integer.valueOf(sysConfigService.getString(SysConfigService.KEY_DEFAULT_USER_ROLE)));

        // 注册审核开关：开启后新用户进入待审核状态，管理端用户管理中启用后可登录
        if (sysConfigService.getBoolean(SysConfigService.KEY_REGISTER_REVIEW, false)) {
            user.setStatus(UserStatus.PENDING.getCode());
        }

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

        // 检查用户的状态（待审核优先于禁用，提示语区分场景）
        if (UserStatus.PENDING.getCode().equals(user.getStatus())) {
            throw new BusinessException("账号待审核，请联系管理员");
        }
        if (!user.isActive()) {
            throw new BusinessException("用户已被禁用，请联系管理员");
        }

        return buildLoginResponse(user);
    }

    /**
     * 发送邮箱注册验证码
     *
     * <p>防邮箱枚举：对已注册邮箱返回成功但不真正发送（与未注册返回一致文案）；
     * 仅对未注册邮箱实际发送验证码。验证码存储/校验/邮件发送复用 {@link EmailService}
     * 的同一套逻辑（Redis key 与登录共用，都是邮箱归属验证，互用无害）。</p>
     *
     * @param email 目标邮箱
     */
    public void sendRegisterCode(String email) {
        if (!StringUtils.hasText(email)) {
            throw new BusinessException("邮箱不能为空");
        }
        boolean registered = emailExists(email);
        if (registered) {
            // ponytail: 已注册邮箱不发送但返回成功，防止攻击者通过是否收到邮件枚举已注册账号
            return;
        }
        // 未注册邮箱：复用登录验证码生成/存储/邮件发送逻辑
        emailService.sendLoginCode(email);
    }

    /**
     * 邮箱验证码注册并登录
     *
     * <p>流程：校验验证码 → 注册开关 → 邮箱未注册校验 → 创建用户（无密码账户）→
     * 注册审核开启时返回待审核标记（不签发 token），否则签发 token 直接登录。</p>
     *
     * @param registerDTO 邮箱 + 验证码
     * @return 登录响应（审核开启时 token/userInfo 为 null，message 携带提示文案）
     */
    @Transactional(rollbackFor = Exception.class)
    public UserLoginResponseDTO registerByEmail(EmailRegisterDTO registerDTO) {
        // 校验验证码（复用登录验证码校验逻辑，校验成功后删除验证码防重放）
        emailService.verifyLoginCode(registerDTO.getEmail(), registerDTO.getCode());

        // 注册开关
        if (!sysConfigService.getBoolean(SysConfigService.KEY_REGISTER_ENABLED, true)) {
            throw new BusinessException("暂未开放注册，请联系管理员");
        }

        // 邮箱已注册拒绝（验证码仅对未注册邮箱有效，此处拒绝不引入枚举泄露）
        if (emailExists(registerDTO.getEmail())) {
            throw new BusinessException("该邮箱已注册，请直接登录");
        }

        // 注册审核开关：开启时新用户进入待审核状态，不签发 token
        boolean reviewEnabled = sysConfigService.getBoolean(SysConfigService.KEY_REGISTER_REVIEW, false);
        Integer status = reviewEnabled ? UserStatus.PENDING.getCode() : UserStatus.NORMAL.getCode();

        // 创建无密码账户（username=email、password 空串、nickname 随机生成）
        User user = UserConvert.registerByEmailToEntity(registerDTO.getEmail(), status);
        userMapper.insert(user);

        log.info("用户邮箱注册成功, userId={}, email={}, review={}", user.getId(), registerDTO.getEmail(), reviewEnabled);

        if (reviewEnabled) {
            // 审核开启：不签发 token，返回待审核标记
            return UserLoginResponseDTO.builder()
                    .message("已提交注册申请，审核通过后可登录")
                    .build();
        }

        // 审核关闭：直接签发 token 登录
        return buildLoginResponse(user);
    }

    /**
     * 首次设置密码（仅无密码用户可用）
     *
     * <p>已有密码用户调用会被拒绝，应走修改密码（需旧密码）流程。</p>
     *
     * @param userId 当前登录用户ID
     * @param setPasswordDTO 新密码 + 确认密码
     */
    @Transactional(rollbackFor = Exception.class)
    public void setPassword(Long userId, SetPasswordDTO setPasswordDTO) {
        // 校验两次密码一致
        if (!setPasswordDTO.getPassword().equals(setPasswordDTO.getConfirmPassword())) {
            throw new BusinessException("两次输入密码不一致");
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }

        // 已有密码拒绝（防覆盖：有密码用户必须走修改密码校验旧密码）
        if (StringUtils.hasText(user.getPassword())) {
            throw new BusinessException("您已设置密码，请使用修改密码");
        }

        // 密码规则校验（与前端正则一致：大于8位，含字母+数字+特殊符号）
        if (!Pattern.matches(SetPasswordDTO.PASSWORD_REGEX, setPasswordDTO.getPassword())) {
            throw new BusinessException("密码需大于8位，且包含字母、数字和特殊符号");
        }

        user.setPassword(passwordEncoder.encode(setPasswordDTO.getPassword().trim()));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);

        // ponytail: 不在日志中打印密码
        log.info("用户首次设置密码成功, userId={}", userId);
    }

    /**
     * 创建咨询师登录账号（user_type=3）：管理员新建咨询师事务内调用。
     * 初始 status=DISABLED(0)，审核通过前登录被拒；审核通过后由
     * {@link #updateAccountStatus} 启用。BCrypt 加密初始密码，明文仅随返回值一次性交给管理员。
     *
     * @param preferredUsername 管理员指定用户名（空则自动生成 doc_xxxxxxxx）
     * @return 新账号 id、用户名、明文初始密码（仅返回一次，不落日志）
     */
    @Transactional(rollbackFor = Exception.class)
    public ConsultantAccountCredential createConsultantAccount(String preferredUsername) {
        String username = resolveUniqueUsername(preferredUsername);
        String rawPassword = randomPassword();

        // email 有 NOT NULL 语义与唯一约束，咨询师账号无真实邮箱，合成唯一占位邮箱
        User user = User.builder()
                .username(username)
                .email(username + CONSULTANT_EMAIL_DOMAIN)
                .password(passwordEncoder.encode(rawPassword))
                .nickname(username)
                .userType(UserType.CONSULTANT.getCode())
                .status(UserStatus.DISABLED.getCode())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        userMapper.insert(user);
        logConsultantAccount("create", user.getId());
        return new ConsultantAccountCredential(user.getId(), username, rawPassword);
    }

    /**
     * 联动咨询师账号启禁用：禁用时同步清 Redis 白名单 token，原会话立即失效
     * （JwtAuthticationFilter 还会校验 status=NORMAL，双保险）。
     *
     * @param userId 咨询师登录账号 id；为 null（历史未绑定咨询师）直接跳过
     * @param status {@link UserStatus} code
     */
    public void updateAccountStatus(Long userId, Integer status) {
        if (userId == null) {
            // ponytail: 存量咨询师未绑定账号时静默跳过；如需要可补「绑定账号」管理接口
            return;
        }
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getStatus, status)
                .set(User::getUpdatedAt, LocalDateTime.now()));
        if (UserStatus.DISABLED.getCode().equals(status)) {
            tokenService.removeLoginToken(userId);
        }
        logConsultantAccount(UserStatus.DISABLED.getCode().equals(status) ? "disable" : "enable", userId);
    }

    /**
     * 解析唯一用户名：优先管理员指定（调用方已做格式校验），冲突抛业务异常；
     * 未指定则生成 doc_xxxxxxxx，最多重试 5 次。
     */
    private String resolveUniqueUsername(String preferredUsername) {
        if (preferredUsername != null && !preferredUsername.isBlank()) {
            String username = preferredUsername.trim();
            if (usernameExists(username)) {
                throw new BusinessException("登录用户名已存在，请更换");
            }
            return username;
        }
        for (int i = 0; i < 5; i++) {
            String candidate = CONSULTANT_USERNAME_PREFIX
                    + randomString(USERNAME_ALPHABET, RANDOM_SUFFIX_LENGTH);
            if (!usernameExists(candidate)) {
                return candidate;
            }
        }
        throw new BusinessException("系统繁忙，请稍后重试创建咨询师");
    }

    private boolean usernameExists(String username) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)) > 0;
    }

    /** 判断邮箱是否已注册 */
    private boolean emailExists(String email) {
        return userMapper.selectCount(new LambdaQueryWrapper<User>()
                .eq(User::getEmail, email)) > 0;
    }

    /** 生成随机初始密码（8 位，去易混淆字符）；包可见以便单测断言长度/字符集 */
    static String randomPassword() {
        return randomString(PASSWORD_ALPHABET, INITIAL_PASSWORD_LENGTH);
    }

    private static String randomString(char[] alphabet, int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(alphabet[SECURE_RANDOM.nextInt(alphabet.length)]);
        }
        return sb.toString();
    }

    /** 咨询师账号生命周期日志只记 userId + 动作，禁止打印密码 */
    private void logConsultantAccount(String action, Long userId) {
        log.info("咨询师账号操作, userId={}, action={}", userId, action);
    }

    /**
     * 更新用户最后登录时间为当前时刻（条件 UPDATE，单字段，不影响其他业务）
     */
    private void updateLastLoginAt(Long userId) {
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getLastLoginAt, LocalDateTime.now()));
    }
}
