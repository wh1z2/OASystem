package com.oasystem.service.impl;

import com.oasystem.dto.LoginRequest;
import com.oasystem.dto.LoginResponse;
import com.oasystem.dto.UserInfoResponse;
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

/**
 * 认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;

    @Override
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getUsername(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        String token = jwtTokenUtil.generateToken(userDetails.getId(), userDetails.getUsername());

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getName(),
                userDetails.getAvatar(),
                userDetails.getRoleName(),
                userDetails.getRoleLabel()
        );

        return new LoginResponse(
                token,
                "Bearer",
                jwtTokenUtil.getExpirationDate(token),
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
        return response;
    }
}
