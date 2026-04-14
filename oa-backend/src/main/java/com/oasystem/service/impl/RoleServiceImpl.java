package com.oasystem.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oasystem.dto.PageResult;
import com.oasystem.dto.RoleCreateRequest;
import com.oasystem.dto.RoleUpdateRequest;
import com.oasystem.entity.Role;
import com.oasystem.exception.BusinessException;
import com.oasystem.mapper.RoleMapper;
import com.oasystem.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 角色服务实现
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleMapper roleMapper;

    @Override
    public List<Role> listAllRoles() {
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Role::getId);
        return roleMapper.selectList(wrapper);
    }

    @Override
    public PageResult<Role> listRoles(Integer current, Integer size) {
        Page<Role> page = new Page<>(current, size);
        LambdaQueryWrapper<Role> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Role::getId);
        Page<Role> result = roleMapper.selectPage(page, wrapper);
        return PageResult.of(result.getRecords(), (Long) result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public Role getRoleById(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        return role;
    }

    @Override
    public Long createRole(RoleCreateRequest request) {
        Role exist = roleMapper.selectOne(
                new LambdaQueryWrapper<Role>().eq(Role::getName, request.getName()));
        if (exist != null) {
            throw new BusinessException("角色标识已存在");
        }

        Role role = new Role();
        BeanUtils.copyProperties(request, role);
        if (request.getPermissions() != null) {
            role.setPermissions(JSON.toJSONString(request.getPermissions()));
        } else {
            role.setPermissions("[]");
        }
        role.setCreateTime(LocalDateTime.now());
        roleMapper.insert(role);
        return role.getId();
    }

    @Override
    public Boolean updateRole(Long id, RoleUpdateRequest request) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        if ("admin".equals(role.getName())) {
            throw new BusinessException("不能修改系统管理员角色");
        }
        BeanUtils.copyProperties(request, role);
        if (request.getPermissions() != null) {
            role.setPermissions(JSON.toJSONString(request.getPermissions()));
        }
        roleMapper.updateById(role);
        return true;
    }

    @Override
    public Boolean deleteRole(Long id) {
        Role role = roleMapper.selectById(id);
        if (role == null) {
            throw new BusinessException("角色不存在");
        }
        if ("admin".equals(role.getName())) {
            throw new BusinessException("不能删除系统管理员角色");
        }
        roleMapper.deleteById(id);
        return true;
    }
}
