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
     * 审批类型（对应表单模板编码，如 LEAVE_FORM）
     */
    private String type;

    /**
     * 状态：0草稿，1审批中，2已通过，3已打回，4已撤销
     */
    private Integer status;

    /**
     * 申请人ID
     */
    private Long applicantId;

    /**
     * 当前页码（与前端保持一致，使用current）
     */
    private Integer current = 1;

    /**
     * 每页大小（与前端保持一致，使用size）
     */
    private Integer size = 10;

    /**
     * 获取当前页码（兼容旧代码）
     */
    public Integer getPageNum() {
        return current;
    }

    /**
     * 获取每页大小（兼容旧代码）
     */
    public Integer getPageSize() {
        return size;
    }
}
