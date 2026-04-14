package com.oasystem.controller;

import com.oasystem.dto.PageResult;
import com.oasystem.dto.Result;
import com.oasystem.dto.RoleCreateRequest;
import com.oasystem.dto.RoleUpdateRequest;
import com.oasystem.entity.Role;
import com.oasystem.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 */
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * 查询所有角色列表
     */
    @GetMapping("/all")
    @PreAuthorize("hasAnyAuthority('role_manage', 'all')")
    public Result<List<Role>> listAllRoles() {
        List<Role> roles = roleService.listAllRoles();
        return Result.success(roles);
    }

    /**
     * 分页查询角色列表
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('role_manage', 'all')")
    public Result<PageResult<Role>> listRoles(
            @RequestParam(defaultValue = "1") Integer current,
            @RequestParam(defaultValue = "10") Integer size) {
        PageResult<Role> result = roleService.listRoles(current, size);
        return Result.success(result);
    }

    /**
     * 根据ID查询角色详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('role_manage', 'all')")
    public Result<Role> getRoleById(@PathVariable Long id) {
        Role role = roleService.getRoleById(id);
        return Result.success(role);
    }

    /**
     * 创建角色
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('role_manage', 'all')")
    public Result<Long> createRole(@RequestBody @Valid RoleCreateRequest request) {
        Long id = roleService.createRole(request);
        return Result.success(id);
    }

    /**
     * 更新角色
     */
    @PostMapping("/{id}/update")
    @PreAuthorize("hasAnyAuthority('role_manage', 'all')")
    public Result<Boolean> updateRole(@PathVariable Long id, @RequestBody @Valid RoleUpdateRequest request) {
        Boolean success = roleService.updateRole(id, request);
        return Result.success(success);
    }

    /**
     * 删除角色
     */
    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAnyAuthority('role_manage', 'all')")
    public Result<Boolean> deleteRole(@PathVariable Long id) {
        Boolean success = roleService.deleteRole(id);
        return Result.success(success);
    }
}
