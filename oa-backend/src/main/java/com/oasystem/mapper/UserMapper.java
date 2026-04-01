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
}
