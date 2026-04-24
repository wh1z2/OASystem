package com.oasystem.service;

import com.oasystem.dto.LoginRequest;
import com.oasystem.dto.LoginResponse;
import com.oasystem.dto.RefreshTokenResponse;
import com.oasystem.dto.UserInfoResponse;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 用户登录
     */
    LoginResponse login(LoginRequest request);

    /**
     * 获取当前登录用户信息
     */
    UserInfoResponse getCurrentUserInfo();

    /**
     * 使用 Refresh Token 刷新 Access Token
     */
    RefreshTokenResponse refreshAccessToken(String refreshToken);

    /**
     * 用户登出，撤销当前用户的 Refresh Token
     */
    void logout(Long userId);
}
