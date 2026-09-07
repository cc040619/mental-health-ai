package com.cc.util;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.cc.enums.UserType;
import com.cc.exception.BusinessException;

/**
 * 当前登录用户工具类，从请求的 JWT token 中提取用户身份信息
 *
 * @author cc
 * @date 2026-09-06
 */
public class CurrentUserUtils {

    private CurrentUserUtils() {
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
}
