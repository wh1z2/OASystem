package com.oasystem.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.oasystem.dto.*;
import com.oasystem.entity.Department;
import com.oasystem.entity.Role;
import com.oasystem.entity.User;
import com.oasystem.exception.BusinessException;
import com.oasystem.mapper.DepartmentMapper;
import com.oasystem.mapper.RoleMapper;
import com.oasystem.mapper.UserMapper;
import com.oasystem.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final RoleMapper roleMapper;
    private final DepartmentMapper departmentMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<UserInfoResponse> listUsers(UserQuery query) {
        Page<User> page = new Page<>(query.getPageNum(), query.getPageSize());
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(w -> w.like(User::getUsername, query.getKeyword())
                    .or()
                    .like(User::getName, query.getKeyword())
                    .or()
                    .like(User::getEmail, query.getKeyword()));
        }
        if (query.getRoleId() != null) {
            wrapper.eq(User::getRoleId, query.getRoleId());
        }
        if (query.getDeptId() != null) {
            wrapper.eq(User::getDeptId, query.getDeptId());
        }
        if (query.getStatus() != null) {
            wrapper.eq(User::getStatus, query.getStatus());
        }

        wrapper.orderByDesc(User::getCreateTime);
        Page<User> result = userMapper.selectPage(page, wrapper);

        List<UserInfoResponse> records = result.getRecords().stream()
                .map(this::convertToUserInfo)
                .collect(Collectors.toList());

        return PageResult.of(records, (Long) result.getTotal(), (int) result.getCurrent(), (int) result.getSize());
    }

    @Override
    public UserInfoResponse getUserById(Long id) {
        User user = userMapper.selectByIdWithRole(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return convertToUserInfo(user);
    }

    @Override
    public Long createUser(UserCreateRequest request) {
        // 检查用户名是否已存在
        User existUser = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, request.getUsername()));
        if (existUser != null) {
            throw new BusinessException("用户名已存在");
        }

        User user = new User();
        BeanUtils.copyProperties(request, user);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user.getId();
    }

    @Override
    public Boolean updateUser(Long id, UserUpdateRequest request) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        BeanUtils.copyProperties(request, user);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        return true;
    }

    @Override
    public Boolean deleteUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if ("admin".equals(user.getUsername())) {
            throw new BusinessException("不能删除系统管理员账号");
        }
        userMapper.deleteById(id);
        return true;
    }

    @Override
    public Boolean changePassword(Long userId, PasswordChangeRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException("旧密码错误");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        return true;
    }

    @Override
    public Boolean updateProfile(Long userId, ProfileUpdateRequest request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        BeanUtils.copyProperties(request, user);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        return true;
    }

    private UserInfoResponse convertToUserInfo(User user) {
        UserInfoResponse response = new UserInfoResponse();
        BeanUtils.copyProperties(user, response);

        // 填充角色信息
        if (StringUtils.hasText(user.getRoleName())) {
            response.setRoleName(user.getRoleName());
            response.setRoleLabel(user.getRoleLabel());
        } else if (user.getRoleId() != null) {
            Role role = roleMapper.selectById(user.getRoleId());
            if (role != null) {
                response.setRoleName(role.getName());
                response.setRoleLabel(role.getLabel());
            }
        }

        // 填充部门信息
        if (user.getDeptId() != null) {
            Department dept = departmentMapper.selectById(user.getDeptId());
            if (dept != null) {
                response.setDepartment(dept.getName());
            }
        }

        return response;
    }
}
