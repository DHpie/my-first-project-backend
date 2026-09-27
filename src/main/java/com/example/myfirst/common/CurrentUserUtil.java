package com.example.myfirst.common;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 临时用户身份工具类。
 * 当前阶段通过请求头 X-User-Id 传递用户 ID，后续接入正式认证后替换。
 */
public final class CurrentUserUtil {

    private static final String HEADER_USER_ID = "X-User-Id";

    private CurrentUserUtil() {
    }

    /**
     * 从请求头中提取当前用户 ID
     *
     * @return 用户 ID，未登录时返回 null
     */
    public static Long getUserId(HttpServletRequest request) {
        String value = request.getHeader(HEADER_USER_ID);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 从请求头中获取当前用户 ID，未登录时抛出异常
     */
    public static Long requireUserId(HttpServletRequest request) {
        Long userId = getUserId(request);
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        return userId;
    }
}
