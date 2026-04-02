package com.oasystem.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oasystem.dto.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 自定义认证入口点
 * 处理未认证或认证失败的情况，返回HTTP 200 + 业务码的响应格式
 */
@Slf4j
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        // 从请求属性中获取具体的token错误信息（由JwtAuthenticationFilter设置）
        String tokenError = (String) request.getAttribute("token_error");

        Result<Void> result;
        if (tokenError != null) {
            // 根据具体的token错误类型返回不同的业务码
            switch (tokenError) {
                case "token_expired":
                    result = Result.error(401, "Token已过期，请重新登录");
                    log.warn("Token已过期, URI: {}", request.getRequestURI());
                    break;
                case "token_invalid":
                    result = Result.error(401, "Token无效，请重新登录");
                    log.warn("Token无效, URI: {}", request.getRequestURI());
                    break;
                case "token_missing":
                    result = Result.error(401, "请求未携带Token，请先登录");
                    log.warn("请求未携带Token, URI: {}", request.getRequestURI());
                    break;
                default:
                    result = Result.unauthorized("认证失败，请重新登录");
                    log.warn("认证失败: {}, URI: {}", tokenError, request.getRequestURI());
            }
        } else {
            // 默认未认证情况
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || authHeader.isEmpty()) {
                result = Result.error(401, "请求未携带Token，请先登录");
                log.warn("请求未携带Authorization头, URI: {}", request.getRequestURI());
            } else {
                result = Result.unauthorized("认证失败，请重新登录");
                log.warn("认证失败: {}, URI: {}", authException.getMessage(), request.getRequestURI());
            }
        }

        // 设置响应头：HTTP 200 + JSON格式
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // 写入响应体
        objectMapper.writeValue(response.getOutputStream(), result);
    }
}
