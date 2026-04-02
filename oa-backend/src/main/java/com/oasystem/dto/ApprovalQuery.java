package com.oasystem.dto;

import lombok.Data;

/**
 * 审批工单查询条件DTO
 */
@Data
public class ApprovalQuery {

    /**
     * 审批标题（模糊查询）
     */
    private String title;

    /**
     * 审批类型：1请假，2报销，3采购，4加班，5出差
     */
    private Integer type;

    /**
     * 状态：0草稿，1审批中，2已通过，3已打回，4已撤销
     */
    private Integer status;

    /**
     * 申请人ID
     */
    private Long applicantId;

    /**
     * 当前页码
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;
}
