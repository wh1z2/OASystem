package com.oasystem.security;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.oasystem.entity.Department;
import com.oasystem.entity.Role;
import com.oasystem.entity.User;
import com.oasystem.mapper.DepartmentMapper;
import com.oasystem.mapper.RoleMapper;
import com.oasystem.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * UserDetailsService实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final DepartmentMapper departmentMapper;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            log.warn("用户不存在: {}", username);
            throw new UsernameNotFoundException("用户不存在: " + username);
        }

        Role role = roleMapper.selectById(user.getRoleId());
        String roleName = role != null ? role.getName() : "";
        String roleLabel = role != null ? role.getLabel() : "";

        Department dept = departmentMapper.selectById(user.getDeptId());
        String deptName = dept != null ? dept.getName() : "";

        List<String> permissions = Collections.emptyList();
        if (role != null && role.getPermissions() != null && !role.getPermissions().isEmpty()) {
            try {
                permissions = JSON.parseArray(role.getPermissions(), String.class);
            } catch (Exception e) {
                log.warn("解析角色权限失败: {}", e.getMessage());
            }
        }

        UserDetailsImpl userDetails = UserDetailsImpl.build(user, roleName, deptName, permissions);
        userDetails.setRoleLabel(roleLabel);
        return userDetails;
    }
}
