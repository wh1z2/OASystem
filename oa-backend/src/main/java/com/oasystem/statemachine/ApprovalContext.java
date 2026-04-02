package com.oasystem.statemachine;

import com.oasystem.dto.ApprovalActionCmd;
import com.oasystem.entity.Approval;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 状态机上下文
 * 在状态转换过程中传递业务数据
 */
@Data
@AllArgsConstructor
public class ApprovalContext {

    /**
     * 审批工单实体
     */
    private Approval approval;

    /**
     * 操作命令（包含审批意见等）
     */
    private ApprovalActionCmd cmd;

    /**
     * 当前操作人ID
     */
    private Long operatorId;
}
