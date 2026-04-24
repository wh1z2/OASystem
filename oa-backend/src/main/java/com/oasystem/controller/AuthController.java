package com.oasystem.controller;

import com.oasystem.dto.LoginRequest;
import com.oasystem.dto.LoginResponse;
import com.oasystem.dto.RefreshTokenResponse;
import com.oasystem.dto.Result;
import com.oasystem.dto.UserInfoResponse;
import com.oasystem.security.UserDetailsImpl;
import com.oasystem.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

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

    /**
     * 使用 Refresh Token 刷新 Access Token
     */
    @PostMapping("/refresh")
    public Result<RefreshTokenResponse> refresh(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.isEmpty()) {
            return Result.unauthorized("Refresh Token不能为空");
        }
        return Result.success(authService.refreshAccessToken(refreshToken));
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public Result<Void> logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserDetailsImpl userDetails) {
            authService.logout(userDetails.getId());
        }
        return Result.success();
    }
}
