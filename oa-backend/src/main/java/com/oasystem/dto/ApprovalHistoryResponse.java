package com.oasystem.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批历史记录响应DTO
 */
@Data
public class ApprovalHistoryResponse {

    /**
     * 历史记录ID
     */
    private Long id;

    /**
     * 工单ID
     */
    private Long approvalId;

    /**
     * 审批人ID
     */
    private Long approverId;

    /**
     * 审批人姓名
     */
    private String approverName;

    /**
     * 操作类型：0提交，1同意，2拒绝，3重新编辑，4撤销
     */
    private Integer action;

    /**
     * 操作类型名称
     */
    private String actionName;

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
     * 原指定审批人姓名
     */
    private String originalApproverName;

    /**
     * 代审批原因/备注
     */
    private String proxyReason;
}
