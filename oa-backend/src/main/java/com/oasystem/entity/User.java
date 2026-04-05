package com.oasystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体类
 */
@Data
@TableName("sys_user")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码（BCrypt加密）
     */
    private String password;

    /**
     * 真实姓名
     */
    private String name;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 角色ID
     */
    private Long roleId;

    /**
     * 部门ID
     */
    private Long deptId;

    /**
     * 状态：0禁用，1启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    // ========== 非数据库字段（关联查询时填充）==========

    /**
     * 角色标识（非持久化字段）
     * 用于接收 selectByIdWithRole 查询结果
     */
    @TableField(exist = false)
    private String roleName;

    /**
     * 角色显示名（非持久化字段）
     * 用于接收 selectByIdWithRole 查询结果
     */
    @TableField(exist = false)
    private String roleLabel;

    /**
     * 角色权限JSON（非持久化字段）
     * 用于接收 selectByIdWithRole 查询结果
     */
    @TableField(exist = false)
    private String permissions;
}
