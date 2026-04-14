package com.oasystem.controller;

import com.oasystem.dto.*;
import com.oasystem.security.UserDetailsImpl;
import com.oasystem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 用户管理控制器
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 查询用户列表
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('user_view', 'user_manage', 'all')")
    public Result<PageResult<UserInfoResponse>> listUsers(UserQuery query) {
        PageResult<UserInfoResponse> result = userService.listUsers(query);
        return Result.success(result);
    }

    /**
     * 根据ID查询用户详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('user_view', 'user_manage', 'all')")
    public Result<UserInfoResponse> getUserById(@PathVariable Long id) {
        UserInfoResponse user = userService.getUserById(id);
        return Result.success(user);
    }

    /**
     * 创建用户
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('user_manage', 'all')")
    public Result<Long> createUser(@RequestBody @Valid UserCreateRequest request) {
        Long id = userService.createUser(request);
        return Result.success(id);
    }

    /**
     * 更新用户
     */
    @PostMapping("/{id}/update")
    @PreAuthorize("hasAnyAuthority('user_manage', 'all')")
    public Result<Boolean> updateUser(@PathVariable Long id, @RequestBody @Valid UserUpdateRequest request) {
        Boolean success = userService.updateUser(id, request);
        return Result.success(success);
    }

    /**
     * 删除用户
     */
    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAnyAuthority('user_manage', 'all')")
    public Result<Boolean> deleteUser(@PathVariable Long id) {
        Boolean success = userService.deleteUser(id);
        return Result.success(success);
    }

    /**
     * 修改密码
     */
    @PostMapping("/password")
    @PreAuthorize("isAuthenticated()")
    public Result<Boolean> changePassword(@RequestBody @Valid PasswordChangeRequest request) {
        Long userId = getCurrentUserId();
        Boolean success = userService.changePassword(userId, request);
        return Result.success(success);
    }

    /**
     * 更新个人资料
     */
    @PostMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public Result<Boolean> updateProfile(@RequestBody @Valid ProfileUpdateRequest request) {
        Long userId = getCurrentUserId();
        Boolean success = userService.updateProfile(userId, request);
        return Result.success(success);
    }

    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetailsImpl) {
            return ((UserDetailsImpl) principal).getId();
        }
        return null;
    }
}
