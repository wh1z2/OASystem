package com.oasystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oasystem.dto.LoginRequest;
import com.oasystem.dto.LoginResponse;
import com.oasystem.dto.UserInfoResponse;
import com.oasystem.security.UserDetailsImpl;
import com.oasystem.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * R3 前端权限控制修复 - 后端数据支持验证测试
 * 验证 /auth/login 和 /auth/info 接口是否正确返回用户 permissions 列表
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthPermissionTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("登录响应应包含用户权限列表")
    void testLoginResponseContainsPermissions() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                1L, "admin", "管理员", "",
                "admin", "系统管理员", "技术部",
                List.of("all", "approval:execute", "approval:execute:all")
        );
        LoginResponse response = new LoginResponse(
                "mock-token", "Bearer", 3600L, userInfo
        );

        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.user.permissions").isArray())
                .andExpect(jsonPath("$.data.user.permissions[0]").value("all"))
                .andExpect(jsonPath("$.data.user.permissions[1]").value("approval:execute"))
                .andExpect(jsonPath("$.data.user.permissions[2]").value("approval:execute:all"));
    }

    @Test
    @DisplayName("用户信息接口应返回权限列表")
    void testUserInfoResponseContainsPermissions() throws Exception {
        UserInfoResponse userInfo = new UserInfoResponse();
        userInfo.setId(2L);
        userInfo.setUsername("manager");
        userInfo.setName("部门经理");
        userInfo.setRoleName("manager");
        userInfo.setPermissions(List.of("approval:execute", "user_view", "report"));

        when(authService.getCurrentUserInfo()).thenReturn(userInfo);

        UserDetailsImpl mockUser = com.oasystem.security.UserDetailsImpl.build(
                new com.oasystem.entity.User(), "manager", "销售部",
                List.of("approval:execute")
        );
        mockUser.setId(2L);

        mockMvc.perform(get("/auth/info").with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.permissions").isArray())
                .andExpect(jsonPath("$.data.permissions[0]").value("approval:execute"))
                .andExpect(jsonPath("$.data.permissions[1]").value("user_view"))
                .andExpect(jsonPath("$.data.permissions[2]").value("report"));
    }

    @Test
    @DisplayName("普通员工登录响应应包含apply和personal权限")
    void testEmployeeLoginResponsePermissions() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("user");
        request.setPassword("user123");

        LoginResponse.UserInfo userInfo = new LoginResponse.UserInfo(
                3L, "user", "普通员工", "",
                "employee", "普通员工", "技术部",
                List.of("apply", "personal")
        );
        LoginResponse response = new LoginResponse(
                "mock-token", "Bearer", 3600L, userInfo
        );

        when(authService.login(any())).thenReturn(response);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.user.permissions").isArray())
                .andExpect(jsonPath("$.data.user.permissions[0]").value("apply"))
                .andExpect(jsonPath("$.data.user.permissions[1]").value("personal"));
    }
}
