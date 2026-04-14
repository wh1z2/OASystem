package com.oasystem.service;

import com.oasystem.dto.*;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 分页查询用户列表
     */
    PageResult<UserInfoResponse> listUsers(UserQuery query);

    /**
     * 根据ID查询用户详情
     */
    UserInfoResponse getUserById(Long id);

    /**
     * 创建用户
     */
    Long createUser(UserCreateRequest request);

    /**
     * 更新用户
     */
    Boolean updateUser(Long id, UserUpdateRequest request);

    /**
     * 删除用户
     */
    Boolean deleteUser(Long id);

    /**
     * 修改密码
     */
    Boolean changePassword(Long userId, PasswordChangeRequest request);

    /**
     * 更新个人资料
     */
    Boolean updateProfile(Long userId, ProfileUpdateRequest request);
}
