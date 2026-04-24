package com.oasystem.service.impl;

import com.oasystem.dto.LoginRequest;
import com.oasystem.dto.LoginResponse;
import com.oasystem.dto.RefreshTokenResponse;
import com.oasystem.dto.UserInfoResponse;
import com.oasystem.entity.RefreshToken;
import com.oasystem.mapper.RefreshTokenMapper;
import com.oasystem.security.UserDetailsImpl;
import com.oasystem.service.AuthService;
import com.oasystem.util.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final RefreshTokenMapper refreshTokenMapper;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        String accessToken = jwtTokenUtil.generateToken(userDetails.getId(), userDetails.getUsername());
        String refreshToken = jwtTokenUtil.generateRefreshToken(userDetails.getId(), userDetails.getUsername());
        Long refreshExpiresAt = jwtTokenUtil.getRefreshTokenExpiresAt(userDetails.getId(), userDetails.getUsername());

        // 保存 Refresh Token 到数据库
        saveRefreshToken(userDetails.getId(), refreshToken, refreshExpiresAt);

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getName(),
                userDetails.getAvatar(),
                userDetails.getRoleName(),
                userDetails.getRoleLabel(),
                userDetails.getDeptName(),
                userDetails.getPermissions()
        );

        return new LoginResponse(
                accessToken,
                "Bearer",
                jwtTokenUtil.getExpirationDate(accessToken),
                refreshToken,
                refreshExpiresAt,
                userInfo
        );
    }

    @Override
    public UserInfoResponse getCurrentUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailsImpl)) {
            return null;
        }

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        UserInfoResponse response = new UserInfoResponse();
        BeanUtils.copyProperties(userDetails, response);
        response.setRoleName(userDetails.getRoleName());
        response.setRoleLabel(userDetails.getRoleLabel());
        response.setDepartment(userDetails.getDeptName());
        return response;
    }

    @Override
    @Transactional
    public RefreshTokenResponse refreshAccessToken(String refreshToken) {
        // 1. 校验 refresh token 格式和签名
        String validationError = jwtTokenUtil.validateTokenWithReason(refreshToken);
        if (validationError != null) {
            log.warn("Refresh Token 校验失败: {}", validationError);
            throw new com.oasystem.exception.BusinessException(401, "Refresh Token已过期或无效，请重新登录");
        }

        // 2. 查询数据库验证是否存在且未过期、未撤销
        RefreshToken storedToken = refreshTokenMapper.findByToken(refreshToken);
        if (storedToken == null || storedToken.getRevoked() == 1) {
            log.warn("Refresh Token 不存在或已被撤销");
            throw new com.oasystem.exception.BusinessException(401, "Refresh Token已过期或无效，请重新登录");
        }
        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            log.warn("Refresh Token 已过期: {}", storedToken.getId());
            throw new com.oasystem.exception.BusinessException(401, "Refresh Token已过期或无效，请重新登录");
        }

        // 3. 从 token 中获取用户信息
        Long userId = jwtTokenUtil.getUserIdFromToken(refreshToken);
        String username = jwtTokenUtil.getUsernameFromToken(refreshToken);
        if (userId == null || username == null) {
            throw new com.oasystem.exception.BusinessException(401, "Refresh Token已过期或无效，请重新登录");
        }

        // 4. 将旧 refresh token 标记为已撤销
        storedToken.setRevoked(1);
        refreshTokenMapper.updateById(storedToken);

        // 5. 生成新的 token 对
        String newAccessToken = jwtTokenUtil.generateToken(userId, username);
        String newRefreshToken = jwtTokenUtil.generateRefreshToken(userId, username);
        Long newRefreshExpiresAt = jwtTokenUtil.getRefreshTokenExpiresAt(userId, username);

        // 6. 保存新的 refresh token
        saveRefreshToken(userId, newRefreshToken, newRefreshExpiresAt);

        return new RefreshTokenResponse(
                newAccessToken,
                newRefreshToken,
                jwtTokenUtil.getExpirationDate(newAccessToken),
                newRefreshExpiresAt
        );
    }

    @Override
    @Transactional
    public void logout(Long userId) {
        int count = refreshTokenMapper.revokeByUserId(userId);
        log.info("用户 {} 登出，撤销 {} 个 Refresh Token", userId, count);
    }

    /**
     * 保存 Refresh Token 到数据库
     */
    private void saveRefreshToken(Long userId, String token, Long expiresAtMillis) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setToken(token);
        refreshToken.setExpiresAt(
                LocalDateTime.ofInstant(Instant.ofEpochMilli(expiresAtMillis), ZoneId.systemDefault())
        );
        refreshToken.setRevoked(0);
        refreshTokenMapper.insert(refreshToken);
    }
}
