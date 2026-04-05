package com.oasystem.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批历史记录实体类
 * 记录审批工单的每一次状态变更
 */
@Data
@TableName("oa_approval_history")
public class ApprovalHistory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 工单ID（关联oa_approval表）
     */
    private Long approvalId;

    /**
     * 审批人ID（关联sys_user表）
     */
    private Long approverId;

    /**
     * 操作类型：0提交，1审批同意，2审批不同意，3重新编辑，4撤销
     */
    private Integer action;

    /**
     * 审批意见
     */
    private String comment;

    /**
     * 操作时间
     */
    private LocalDateTime createTime;

    // ========== 代审批相关字段（权限系统优化新增）==========

    /**
     * 审批类型：DIRECT直接审批/PROXY_ADMIN管理员代审批/PROXY_MANAGER经理代审批
     */
    private String approvalType;

    /**
     * 是否为代审批：0否 1是
     */
    private Integer isProxy;

    /**
     * 原指定审批人ID（代审批时记录）
     */
    private Long originalApproverId;

    /**
     * 代审批原因/备注
     */
    private String proxyReason;
}
