package com.oasystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oasystem.dto.*;
import com.oasystem.security.UserDetailsImpl;
import com.oasystem.service.ApprovalService;
import com.oasystem.service.AuthService;
import com.oasystem.service.RoleService;
import com.oasystem.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 方法级权限控制测试
 * 验证 @PreAuthorize 注解是否正确生效
 *
 * 注意：本项目安全处理器返回 HTTP 200 + 业务码格式，因此需检查响应体中的 code 字段
 */
@SpringBootTest
@AutoConfigureMockMvc
class MethodSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ApprovalService approvalService;

    @MockBean
    private UserService userService;

    @MockBean
    private RoleService roleService;

    @MockBean
    private AuthService authService;

    // ==================== AuthController ====================

    @Test
    @DisplayName("未认证用户访问 /auth/info 应返回业务码 403")
    void testUnauthenticatedAccessAuthInfo() throws Exception {
        mockMvc.perform(get("/auth/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @WithMockUser
    @DisplayName("已认证用户访问 /auth/info 应返回业务码 200")
    void testAuthenticatedAccessAuthInfo() throws Exception {
        when(authService.getCurrentUserInfo()).thenReturn(new UserInfoResponse());
        mockMvc.perform(get("/auth/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== ApprovalController - apply 权限 ====================

    @Test
    @WithMockUser(authorities = {})
    @DisplayName("无 apply 权限用户创建工单应返回业务码 403")
    void testNoApplyPermissionCannotCreateApproval() throws Exception {
        ApprovalCreateRequest request = new ApprovalCreateRequest();
        request.setTitle("测试申请");
        request.setType(1);

        mockMvc.perform(post("/approvals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @WithMockUser(authorities = {"apply"})
    @DisplayName("有 apply 权限用户创建工单应返回业务码 200")
    void testApplyPermissionCanCreateApproval() throws Exception {
        ApprovalCreateRequest request = new ApprovalCreateRequest();
        request.setTitle("测试申请");
        request.setType(1);

        when(approvalService.create(any(), anyLong())).thenReturn(1L);

        mockMvc.perform(post("/approvals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(authorities = {"apply"})
    @DisplayName("有 apply 权限用户更新工单应返回业务码 200")
    void testApplyPermissionCanUpdateApproval() throws Exception {
        ApprovalUpdateRequest request = new ApprovalUpdateRequest();
        request.setTitle("更新标题");

        when(approvalService.update(anyLong(), any())).thenReturn(true);

        mockMvc.perform(post("/approvals/1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(authorities = {})
    @DisplayName("无 apply 权限用户删除工单应返回业务码 403")
    void testNoApplyPermissionCannotDeleteApproval() throws Exception {
        mockMvc.perform(post("/approvals/1/delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @WithMockUser(authorities = {"apply"})
    @DisplayName("有 apply 权限用户提交工单应返回业务码 200")
    void testApplyPermissionCanSubmitApproval() throws Exception {
        when(approvalService.submit(anyLong(), anyLong())).thenReturn(true);
        mockMvc.perform(post("/approvals/1/submit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(authorities = {})
    @DisplayName("无 apply 权限用户重新编辑工单应返回业务码 403")
    void testNoApplyPermissionCannotReeditApproval() throws Exception {
        mockMvc.perform(post("/approvals/1/reedit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @WithMockUser(authorities = {"apply"})
    @DisplayName("有 apply 权限用户撤销工单应返回业务码 200")
    void testApplyPermissionCanRevokeApproval() throws Exception {
        when(approvalService.revoke(anyLong(), anyLong())).thenReturn(true);
        mockMvc.perform(post("/approvals/1/revoke"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== ApprovalController - approval 权限 ====================

    @Test
    @WithMockUser(authorities = {})
    @DisplayName("无 approval 权限用户审批通过应返回业务码 403")
    void testNoApprovalPermissionCannotApprove() throws Exception {
        mockMvc.perform(post("/approvals/1/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApprovalActionCmd())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @WithMockUser(authorities = {"approval"})
    @DisplayName("有 approval 权限用户审批通过应返回业务码 200")
    void testApprovalPermissionCanApprove() throws Exception {
        when(approvalService.approve(anyLong(), any(), anyLong())).thenReturn(true);
        mockMvc.perform(post("/approvals/1/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApprovalActionCmd())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(authorities = {})
    @DisplayName("无 approval 权限用户审批拒绝应返回业务码 403")
    void testNoApprovalPermissionCannotReject() throws Exception {
        mockMvc.perform(post("/approvals/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApprovalActionCmd())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @WithMockUser(authorities = {"approval"})
    @DisplayName("有 approval 权限用户访问待办列表应返回业务码 200")
    void testApprovalPermissionCanAccessTodoList() throws Exception {
        when(approvalService.getTodoList(anyLong(), any())).thenReturn(PageResult.of(Collections.emptyList(), 0L, 1, 10));
        mockMvc.perform(get("/approvals/todo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(authorities = {})
    @DisplayName("无 approval 权限用户访问已办列表应返回业务码 403")
    void testNoApprovalPermissionCannotAccessDoneList() throws Exception {
        mockMvc.perform(get("/approvals/done"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    // ==================== ApprovalController - 通用认证 ====================

    @Test
    @WithMockUser
    @DisplayName("已认证用户访问我的申请应返回业务码 200")
    void testAuthenticatedCanAccessMyApprovals() throws Exception {
        when(approvalService.getMyApprovals(anyLong(), any())).thenReturn(PageResult.of(Collections.emptyList(), 0L, 1, 10));
        mockMvc.perform(get("/approvals/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("已认证用户访问审批统计应返回业务码 200")
    void testAuthenticatedCanAccessStatistics() throws Exception {
        when(approvalService.getDashboardStatistics(anyLong())).thenReturn(new DashboardStatisticsResponse());

        UserDetailsImpl mockUser = UserDetailsImpl.build(
                new com.oasystem.entity.User(), "employee", "技术部",
                List.of("apply")
        );
        mockUser.setId(1L);

        mockMvc.perform(get("/approvals/statistics").with(user(mockUser)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== UserController ====================

    @Test
    @WithMockUser(authorities = {})
    @DisplayName("无 user_view/user_manage/all 权限用户访问用户列表应返回业务码 403")
    void testNoUserPermissionCannotListUsers() throws Exception {
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @WithMockUser(authorities = {"user_view"})
    @DisplayName("有 user_view 权限用户访问用户列表应返回业务码 200")
    void testUserViewPermissionCanListUsers() throws Exception {
        when(userService.listUsers(any())).thenReturn(PageResult.of(Collections.emptyList(), 0L, 1, 10));
        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(authorities = {"user_view"})
    @DisplayName("有 user_view 权限用户查看用户详情应返回业务码 200")
    void testUserViewPermissionCanGetUserDetail() throws Exception {
        when(userService.getUserById(anyLong())).thenReturn(new UserInfoResponse());
        mockMvc.perform(get("/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(authorities = {"user_view"})
    @DisplayName("无 user_manage 权限用户创建用户应返回业务码 403")
    void testUserViewPermissionCannotCreateUser() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        request.setPassword("123456");
        request.setName("测试用户");
        request.setEmail("test@test.com");
        request.setRoleId(1L);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @WithMockUser(authorities = {"user_manage"})
    @DisplayName("有 user_manage 权限用户创建用户应返回业务码 200")
    void testUserManagePermissionCanCreateUser() throws Exception {
        UserCreateRequest request = new UserCreateRequest();
        request.setUsername("testuser");
        request.setPassword("123456");
        request.setName("测试用户");
        request.setEmail("test@test.com");
        request.setRoleId(1L);

        when(userService.createUser(any())).thenReturn(1L);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(authorities = {"user_manage"})
    @DisplayName("有 user_manage 权限用户更新用户应返回业务码 200")
    void testUserManagePermissionCanUpdateUser() throws Exception {
        UserUpdateRequest request = new UserUpdateRequest();
        request.setName("更新名称");
        request.setEmail("update@test.com");
        request.setRoleId(1L);

        when(userService.updateUser(anyLong(), any())).thenReturn(true);

        mockMvc.perform(post("/users/1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(authorities = {"user_manage"})
    @DisplayName("有 user_manage 权限用户删除用户应返回业务码 200")
    void testUserManagePermissionCanDeleteUser() throws Exception {
        when(userService.deleteUser(anyLong())).thenReturn(true);
        mockMvc.perform(post("/users/1/delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser
    @DisplayName("已认证用户修改密码应返回业务码 200")
    void testAuthenticatedCanChangePassword() throws Exception {
        PasswordChangeRequest request = new PasswordChangeRequest();
        request.setOldPassword("old123");
        request.setNewPassword("new123");

        when(userService.changePassword(anyLong(), any())).thenReturn(true);

        mockMvc.perform(post("/users/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser
    @DisplayName("已认证用户更新个人资料应返回业务码 200")
    void testAuthenticatedCanUpdateProfile() throws Exception {
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setName("新名字");
        request.setEmail("new@test.com");

        when(userService.updateProfile(anyLong(), any())).thenReturn(true);

        mockMvc.perform(post("/users/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== RoleController ====================

    @Test
    @WithMockUser(authorities = {})
    @DisplayName("无 role_manage/all 权限用户访问角色列表应返回业务码 403")
    void testNoRolePermissionCannotListRoles() throws Exception {
        mockMvc.perform(get("/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @WithMockUser(authorities = {"role_manage"})
    @DisplayName("有 role_manage 权限用户访问角色列表应返回业务码 200")
    void testRoleManagePermissionCanListRoles() throws Exception {
        when(roleService.listRoles(anyInt(), anyInt())).thenReturn(PageResult.of(Collections.emptyList(), 0L, 1, 10));
        mockMvc.perform(get("/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(authorities = {"role_manage"})
    @DisplayName("有 role_manage 权限用户查看角色详情应返回业务码 200")
    void testRoleManagePermissionCanGetRoleDetail() throws Exception {
        when(roleService.getRoleById(anyLong())).thenReturn(new com.oasystem.entity.Role());
        mockMvc.perform(get("/roles/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(authorities = {"role_manage"})
    @DisplayName("有 role_manage 权限用户创建角色应返回业务码 200")
    void testRoleManagePermissionCanCreateRole() throws Exception {
        RoleCreateRequest request = new RoleCreateRequest();
        request.setName("test_role");
        request.setLabel("测试角色");

        when(roleService.createRole(any())).thenReturn(1L);

        mockMvc.perform(post("/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(authorities = {"role_manage"})
    @DisplayName("有 role_manage 权限用户更新角色应返回业务码 200")
    void testRoleManagePermissionCanUpdateRole() throws Exception {
        RoleUpdateRequest request = new RoleUpdateRequest();
        request.setLabel("更新角色名");

        when(roleService.updateRole(anyLong(), any())).thenReturn(true);

        mockMvc.perform(post("/roles/1/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @WithMockUser(authorities = {"role_manage"})
    @DisplayName("有 role_manage 权限用户删除角色应返回业务码 200")
    void testRoleManagePermissionCanDeleteRole() throws Exception {
        when(roleService.deleteRole(anyLong())).thenReturn(true);
        mockMvc.perform(post("/roles/1/delete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    // ==================== all 权限通配 ====================

    @Test
    @WithMockUser(authorities = {"all"})
    @DisplayName("有 all 权限用户可访问所有管理接口")
    void testAllPermissionCanAccessManagementApis() throws Exception {
        when(userService.listUsers(any())).thenReturn(PageResult.of(Collections.emptyList(), 0L, 1, 10));
        when(roleService.listRoles(anyInt(), anyInt())).thenReturn(PageResult.of(Collections.emptyList(), 0L, 1, 10));
        when(approvalService.create(any(), anyLong())).thenReturn(1L);
        when(approvalService.approve(anyLong(), any(), anyLong())).thenReturn(true);

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(get("/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ApprovalCreateRequest createRequest = new ApprovalCreateRequest();
        createRequest.setTitle("测试申请");
        createRequest.setType(1);
        mockMvc.perform(post("/approvals")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(post("/approvals/1/approve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ApprovalActionCmd())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
