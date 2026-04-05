package com.oasystem.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oasystem.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据用户名查询用户（包含角色信息）
     */
    @Select("SELECT u.*, r.name as role_name, r.label as role_label, r.permissions " +
            "FROM sys_user u LEFT JOIN sys_role r ON u.role_id = r.id " +
            "WHERE u.username = #{username}")
    User selectByUsername(@Param("username") String username);

    /**
     * 根据ID查询用户（包含角色信息）
     * 用于权限检查时获取用户的角色和权限
     *
     * @param id 用户ID
     * @return 包含角色信息的用户实体
     */
    @Select("SELECT u.*, r.name as role_name, r.label as role_label, r.permissions " +
            "FROM sys_user u LEFT JOIN sys_role r ON u.role_id = r.id " +
            "WHERE u.id = #{id}")
    User selectByIdWithRole(@Param("id") Long id);
}
