package com.oasystem.controller;

import com.oasystem.dto.LoginRequest;
import com.oasystem.dto.LoginResponse;
import com.oasystem.dto.Result;
import com.oasystem.dto.UserInfoResponse;
import com.oasystem.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody @Valid LoginRequest request) {
        return Result.success(authService.login(request));
    }

    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/info")
    @PreAuthorize("isAuthenticated()")
    public Result<UserInfoResponse> getCurrentUserInfo() {
        UserInfoResponse userInfo = authService.getCurrentUserInfo();
        if (userInfo == null) {
            return Result.unauthorized("请先登录");
        }
        return Result.success(userInfo);
    }
}
