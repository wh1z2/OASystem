package com.oasystem.service;

import com.oasystem.dto.PageResult;
import com.oasystem.dto.RoleCreateRequest;
import com.oasystem.dto.RoleUpdateRequest;
import com.oasystem.entity.Role;

import java.util.List;

/**
 * 角色服务接口
 */
public interface RoleService {

    /**
     * 查询所有角色列表
     */
    List<Role> listAllRoles();

    /**
     * 分页查询角色列表
     */
    PageResult<Role> listRoles(Integer current, Integer size);

    /**
     * 根据ID查询角色详情
     */
    Role getRoleById(Long id);

    /**
     * 创建角色
     */
    Long createRole(RoleCreateRequest request);

    /**
     * 更新角色
     */
    Boolean updateRole(Long id, RoleUpdateRequest request);

    /**
     * 删除角色
     */
    Boolean deleteRole(Long id);
}
