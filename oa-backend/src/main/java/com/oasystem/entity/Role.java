package com.oasystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 角色实体类
 */
@Data
@TableName("sys_role")
public class Role {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 角色标识
     */
    private String name;

    /**
     * 角色显示名
     */
    private String label;

    /**
     * 角色描述
     */
    private String description;

    /**
     * 权限编码集合（JSON格式）
     */
    private String permissions;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;
}
