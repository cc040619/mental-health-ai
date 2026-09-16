package com.cc.util;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cc.entity.Consultant;
import com.cc.enums.UserType;
import com.cc.exception.BusinessException;
import com.cc.mapper.ConsultantMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 当前登录用户工具类，从请求的 JWT token 中提取用户身份信息。
 *
 * <p>咨询师身份需回查 consultant 绑定关系：沿用 {@link JwtTokenUtil} 的 ApplicationContextAware
 * 静态取 Bean 模式（util 不注入业务 Bean，仅通过容器按需取用 Mapper）。</p>
 *
 * @author cc
 * @date 2026-09-06
 */
@Component
public class CurrentUserUtils implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    private CurrentUserUtils() {
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        CurrentUserUtils.applicationContext = applicationContext;
    }

    /**
     * 获取当前登录用户ID
     *
     * @return 用户ID
     */
    public static Long getCurrentUserId() {
        String token = JwtTokenUtil.getCurrentToken();
        if (token == null) {
            throw new BusinessException("无法获取当前登录信息");
        }
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Long userId = jwt.getClaim("userId").asLong();
        if (userId == null) {
            throw new BusinessException("无法获取当前登录信息");
        }
        return userId;
    }

    /**
     * 获取当前登录用户名（JWT username claim）
     *
     * @return 用户名，未登录/解析失败返回 null
     */
    public static String getCurrentUsername() {
        String token = JwtTokenUtil.getCurrentToken();
        if (token == null) {
            return null;
        }
        try {
            return JwtTokenUtil.verifyToken(token).getClaim("username").asString();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 判断当前登录用户是否为管理员
     *
     * @return true=管理员
     */
    public static boolean isAdmin() {
        String token = JwtTokenUtil.getCurrentToken();
        if (token == null) {
            return false;
        }
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Integer roleType = jwt.getClaim("roleType").asInt();
        return UserType.ADMIN.getCode().equals(roleType);
    }

    /**
     * 判断当前登录用户是否为咨询师（仅校验 JWT 角色，不校验是否已绑定 consultant）
     *
     * @return true=咨询师账号
     */
    public static boolean isConsultant() {
        String token = JwtTokenUtil.getCurrentToken();
        if (token == null) {
            return false;
        }
        DecodedJWT jwt = JwtTokenUtil.verifyToken(token);
        Integer roleType = jwt.getClaim("roleType").asInt();
        return UserType.CONSULTANT.getCode().equals(roleType);
    }

    /**
     * 获取当前登录咨询师的 consultant id（咨询师端所有接口的信任边界入口）。
     * 角色非咨询师 → 无权限；未绑定 consultant → 业务异常「账号未绑定咨询师」。
     *
     * <p>ponytail: 咨询师端请求低频，直接按 user_id 回查；后续 QPS 上升可在请求上下文
     * （一次请求缓存）或 Redis 中缓存 user_id→consultant_id 映射。</p>
     *
     * @return 当前登录咨询师的 consultant id
     */
    public static Long getCurrentConsultantId() {
        if (!isConsultant()) {
            throw new BusinessException("A0301", "无咨询师权限", null);
        }
        Long userId = getCurrentUserId();
        Consultant consultant = applicationContext.getBean(ConsultantMapper.class)
                .selectOne(new LambdaQueryWrapper<Consultant>()
                        .select(Consultant::getId)
                        .eq(Consultant::getUserId, userId)
                        .last("LIMIT 1"));
        if (consultant == null) {
            throw new BusinessException("账号未绑定咨询师，请联系管理员");
        }
        return consultant.getId();
    }

    /**
     * 获取当前请求客户端 IP：X-Forwarded-For 首段优先，其次 remoteAddr
     * <p>必须在调用方线程（请求线程）调用后再传入异步方法，ThreadLocal 不跨线程传递</p>
     *
     * @return 客户端 IP，无请求上下文返回 null
     */
    public static String getCurrentClientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) {
                return null;
            }
            HttpServletRequest request = attrs.getRequest();
            String xff = request.getHeader("X-Forwarded-For");
            if (StringUtils.hasText(xff)) {
                return xff.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}
