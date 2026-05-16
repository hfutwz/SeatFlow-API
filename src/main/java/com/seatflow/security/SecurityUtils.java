package com.seatflow.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Security 工具类：获取当前登录用户信息
 */
@Component
public class SecurityUtils {

    /**
     * 获取当前登录用户，未登录返回 null
     */
    public static CustomUserDetails getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return (CustomUserDetails) principal;
        }
        return null;
    }

    /**
     * 获取当前登录用户 ID，未登录抛出异常
     */
    public static Long getCurrentUserId() {
        CustomUserDetails user = getCurrentUser();
        if (user == null) {
            throw new IllegalStateException("未登录或登录已过期");
        }
        return user.getId();
    }

    /**
     * 判断当前用户是否拥有指定权限
     */
    public static boolean hasPermission(String permission) {
        CustomUserDetails user = getCurrentUser();
        if (user == null) {
            return false;
        }
        return user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(permission));
    }

    /**
     * 判断当前用户是否为管理员（ADMIN 类型）
     */
    public static boolean isAdmin() {
        CustomUserDetails user = getCurrentUser();
        return user != null && "ADMIN".equals(user.getUserType());
    }

    /**
     * 判断当前用户是否为学生（STUDENT 类型）
     */
    public static boolean isStudent() {
        CustomUserDetails user = getCurrentUser();
        return user != null && "STUDENT".equals(user.getUserType());
    }
}
