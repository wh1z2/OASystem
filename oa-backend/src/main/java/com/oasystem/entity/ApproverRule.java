package com.oasystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 默认审批人规则实体
 */
@Data
@TableName("oa_approver_rule")
public class ApproverRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 规则名称
     */
    private String name;

    /**
     * 策略类型：1=按部门角色, 3=固定人员
     */
    private Integer strategyType;

    /**
     * 匹配条件（JSON格式）
     */
    private String matchConditions;

    /**
     * 审批人类型：1=指定用户, 2=指定角色
     */
    private Integer approverType;

    /**
     * 审批人值（JSON格式）
     */
    private String approverValue;

    /**
     * 优先级（数字越小优先级越高）
     */
    private Integer priority;

    /**
     * 状态：0=禁用, 1=启用
     */
    private Integer status;

    /**
     * 规则描述
     */
    private String description;

    /**
     * 作用范围：1=全局, 2=指定部门, 3=指定角色
     */
    private Integer scopeType;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    // ========== 非数据库字段 ==========

    /**
     * 解析后的部门ID列表
     */
    @TableField(exist = false)
    private List<Long> matchDeptIds;

    /**
     * 解析后的审批类型列表
     */
    @TableField(exist = false)
    private List<Integer> matchTypes;

    /**
     * 解析后的角色ID列表
     */
    @TableField(exist = false)
    private List<Long> matchRoleIds;

    /**
     * 策略类型名称（用于前端展示）
     */
    @TableField(exist = false)
    private String strategyTypeName;

    /**
     * 审批人类型名称（用于前端展示）
     */
    @TableField(exist = false)
    private String approverTypeName;
}
