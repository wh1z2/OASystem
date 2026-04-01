package com.oasystem.service;

import com.oasystem.dto.LoginRequest;
import com.oasystem.dto.LoginResponse;
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
}
