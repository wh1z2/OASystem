package com.oasystem.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 审批工单详情响应DTO
 */
@Data
public class ApprovalDetailResponse {

    /**
     * 工单ID
     */
    private Long id;

    /**
     * 审批标题
     */
    private String title;

    /**
     * 审批类型：1请假，2报销，3采购，4加班，5出差
     */
    private Integer type;

    /**
     * 审批类型名称
     */
    private String typeName;

    /**
     * 申请人ID
     */
    private Long applicantId;

    /**
     * 申请人姓名
     */
    private String applicantName;

    /**
     * 当前审批人ID
     */
    private Long currentApproverId;

    /**
     * 当前审批人姓名
     */
    private String currentApproverName;

    /**
     * 状态：0草稿，1审批中，2已通过，3已打回，4已撤销
     */
    private Integer status;

    /**
     * 状态名称
     */
    private String statusName;

    /**
     * 优先级：0低，1普通，2紧急
     */
    private Integer priority;

    /**
     * 优先级名称
     */
    private String priorityName;

    /**
     * 申请内容
     */
    private String content;

    /**
     * 表单数据（JSON格式）
     */
    private String formData;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 部门名称
     */
    private String deptName;
}
