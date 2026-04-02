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
}
