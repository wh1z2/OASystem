package com.oasystem.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oasystem.dto.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 自定义访问拒绝处理器
 * 处理权限不足的情况，返回HTTP 200 + 业务码的响应格式
 */
@Slf4j
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {

        log.warn("权限不足，拒绝访问: {} {}, 原因: {}",
                request.getMethod(), request.getRequestURI(), accessDeniedException.getMessage());

        // 构建统一的响应格式，HTTP 200 + 业务码403
        Result<Void> result = Result.forbidden("无权访问该资源，权限不足");

        // 设置响应头：HTTP 200 + JSON格式
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // 写入响应体
        objectMapper.writeValue(response.getOutputStream(), result);
    }
}
