package com.oasystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Token刷新响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenResponse {

    private String token;
    private String refreshToken;
    private Long expiresIn;
    private Long refreshExpiresAt;
}
