package com.oasystem.security;

import com.oasystem.util.JwtTokenUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String jwt = getJwtFromRequest(request);

            // 检查是否携带了token
            if (!StringUtils.hasText(jwt)) {
                // 未携带token，设置标记供AuthenticationEntryPoint使用
                request.setAttribute("token_error", "token_missing");
                log.debug("请求未携带JWT Token, URI: {}", request.getRequestURI());
            } else {
                // 使用新的方法验证token并获取具体错误原因
                String validationError = jwtTokenUtil.validateTokenWithReason(jwt);

                if (validationError == null) {
                    // 验证通过
                    Long userId = jwtTokenUtil.getUserIdFromToken(jwt);
                    String username = jwtTokenUtil.getUsernameFromToken(jwt);

                    if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        log.debug("JWT认证成功, 用户: {}", username);
                    }
                } else {
                    // 验证失败，设置具体错误原因
                    request.setAttribute("token_error", validationError);
                    log.debug("JWT验证失败: {}, URI: {}", validationError, request.getRequestURI());
                }
            }
        } catch (Exception e) {
            log.error("JWT认证过滤器异常: ", e);
            request.setAttribute("token_error", "token_error:" + e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求头中提取JWT Token
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
