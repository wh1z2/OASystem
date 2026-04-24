package com.oasystem.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类
 */
@Slf4j
@Component
public class JwtTokenUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:1800000}")
    private Long expiration;

    @Value("${jwt.refresh-expiration:604800000}")
    private Long refreshExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成JWT Token
     *
     * @param userId   用户ID
     * @param username 用户名
     * @return Token字符串
     */
    public String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);

        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 生成 Refresh Token
     *
     * @param userId   用户ID
     * @param username 用户名
     * @return Refresh Token字符串
     */
    public String generateRefreshToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("type", "refresh");

        return Jwts.builder()
                .claims(claims)
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 获取 Access Token 绝对过期时间戳（毫秒）
     */
    public Long getAccessTokenExpiresAt(Long userId, String username) {
        return System.currentTimeMillis() + expiration;
    }

    /**
     * 获取 Refresh Token 绝对过期时间戳（毫秒）
     */
    public Long getRefreshTokenExpiresAt(Long userId, String username) {
        return System.currentTimeMillis() + refreshExpiration;
    }

    /**
     * 从Token中提取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims != null && claims.get("userId") != null) {
            return Long.valueOf(claims.get("userId").toString());
        }
        return null;
    }

    /**
     * 从Token中提取用户名
     */
    public String getUsernameFromToken(String token) {
        Claims claims = parseToken(token);
        if (claims != null && claims.get("username") != null) {
            return claims.get("username").toString();
        }
        return null;
    }

    /**
     * 验证Token是否有效
     */
    public boolean validateToken(String token) {
        try {
            Claims claims = parseToken(token);
            return claims != null && !isTokenExpired(claims);
        } catch (Exception e) {
            log.warn("JWT验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证Token并返回具体错误信息
     * @return null表示验证通过，否则返回错误信息（token_expired/token_invalid）
     */
    public String validateTokenWithReason(String token) {
        if (!StringUtils.hasText(token)) {
            return "token_missing";
        }
        try {
            Claims claims = parseTokenForValidation(token);
            if (claims == null) {
                return "token_invalid";
            }
            if (isTokenExpired(claims)) {
                return "token_expired";
            }
            return null; // 验证通过
        } catch (ExpiredJwtException e) {
            log.warn("JWT已过期");
            return "token_expired";
        } catch (Exception e) {
            log.warn("JWT验证失败: {}", e.getMessage());
            return "token_invalid";
        }
    }

    /**
     * 解析Token（用于验证，会抛出异常）
     */
    private Claims parseTokenForValidation(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 解析Token
     */
    private Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("JWT已过期");
            return e.getClaims();
        } catch (Exception e) {
            log.warn("JWT解析失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 判断Token是否过期
     */
    private boolean isTokenExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration != null && expiration.before(new Date());
    }

    /**
     * 获取Token剩余有效时间（毫秒）
     */
    public Long getExpirationDate(String token) {
        Claims claims = parseToken(token);
        if (claims != null && claims.getExpiration() != null) {
            return claims.getExpiration().getTime() - System.currentTimeMillis();
        }
        return 0L;
    }
}
