package com.oasystem.statemachine;

import com.oasystem.dto.ApprovalActionCmd;
import com.oasystem.entity.Approval;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 状态机上下文
 * 在状态转换过程中传递业务数据
 */
@Data
@NoArgsConstructor
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

    /**
     * 权限检查结果
     * 用于记录代审批相关信息
     */
    private ApprovalPermissionResult permissionResult;

    /**
     * 构造方法（不包含权限结果）
     *
     * @param approval   审批工单实体
     * @param cmd        操作命令
     * @param operatorId 当前操作人ID
     */
    public ApprovalContext(Approval approval, ApprovalActionCmd cmd, Long operatorId) {
        this.approval = approval;
        this.cmd = cmd;
        this.operatorId = operatorId;
    }
}
