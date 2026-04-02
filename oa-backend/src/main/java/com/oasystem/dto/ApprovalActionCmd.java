package com.oasystem.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 审批操作命令 DTO
 * 用于接收审批操作请求参数
 */
@Data
public class ApprovalActionCmd {

    /**
     * 审批意见
     */
    private String comment;

    /**
     * 下一审批人ID（当审批通过需要转交下一级时使用，目前暂不实现多级审批）
     */
    private Long nextApproverId;

    /**
     * 审批事件类型：0提交，1审批同意，2审批不同意，3重新编辑，4撤销
     * 后端内部使用，不传也可以
     */
    private Integer actionType;
}
