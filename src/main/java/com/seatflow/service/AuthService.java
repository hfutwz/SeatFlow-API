package com.seatflow.service;

import com.seatflow.dto.request.LoginRequest;
import com.seatflow.dto.response.LoginResponse;
import com.seatflow.dto.response.UserInfoResponse;

public interface AuthService {

    /**
     * 用户登录，返回 JWT Token
     */
    LoginResponse login(LoginRequest request);

    /**
     * 获取当前登录用户信息（从 SecurityContext 获取）
     */
    UserInfoResponse getCurrentUserInfo();

    /**
     * 根据用户 ID 获取用户信息（测试/Service 层调用）
     */
    UserInfoResponse getCurrentUser(Long userId);
}
