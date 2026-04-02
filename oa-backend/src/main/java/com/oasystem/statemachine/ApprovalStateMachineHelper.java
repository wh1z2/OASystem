package com.oasystem.statemachine;

import com.oasystem.entity.Approval;
import com.oasystem.entity.ApprovalHistory;
import com.oasystem.enums.ApprovalEvent;
import com.oasystem.enums.ApprovalStatus;
import com.oasystem.mapper.ApprovalHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 审批状态机辅助类
 * 定义状态转换的条件（Condition）和动作（Action）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalStateMachineHelper {

    private final ApprovalHistoryMapper approvalHistoryMapper;

    /**
     * 检查条件：检查表单数据完整性
     * 提交申请时调用
     */
    public boolean checkFormComplete(ApprovalContext context) {
        Approval approval = context.getApproval();
        // 检查表单数据是否为空
        boolean valid = approval.getFormData() != null && !approval.getFormData().isEmpty();
        if (!valid) {
            log.warn("工单 {} 表单数据不完整，无法提交", approval.getId());
        }
        return valid;
    }

    /**
     * 检查条件：检查当前用户是否有审批权限
     * 审批通过/拒绝时调用
     */
    public boolean checkApproverPermission(ApprovalContext context) {
        Approval approval = context.getApproval();
        Long currentUserId = context.getOperatorId();
        Long approverId = approval.getCurrentApproverId();

        boolean valid = currentUserId != null && currentUserId.equals(approverId);
        if (!valid) {
            log.warn("用户 {} 无权审批工单 {}，当前审批人应为 {}",
                    currentUserId, approval.getId(), approverId);
        }
        return valid;
    }

    /**
     * 检查条件：检查当前用户是否为工单申请人
     * 撤销申请和重新编辑时调用
     */
    public boolean checkIsApplicant(ApprovalContext context) {
        Approval approval = context.getApproval();
        Long currentUserId = context.getOperatorId();
        Long applicantId = approval.getApplicantId();

        boolean valid = currentUserId != null && currentUserId.equals(applicantId);
        if (!valid) {
            log.warn("用户 {} 不是工单 {} 的申请人，无权执行此操作",
                    currentUserId, approval.getId());
        }
        return valid;
    }

    /**
     * 执行动作：提交申请
     * 状态：草稿 -> 审批中
     */
    public void doSubmit(ApprovalStatus from, ApprovalStatus to, ApprovalEvent event, ApprovalContext context) {
        log.info("执行提交动作：工单 {} 从 {} 变为 {}",
                context.getApproval().getId(), from, to);

        Approval approval = context.getApproval();
        approval.setStatus(to.getCode());

        // 如果指定了下一审批人，设置当前审批人
        if (context.getCmd() != null && context.getCmd().getNextApproverId() != null) {
            approval.setCurrentApproverId(context.getCmd().getNextApproverId());
        }

        // 记录审批历史
        saveHistory(approval.getId(), context.getOperatorId(),
                ApprovalEvent.SUBMIT.getCode(), "提交申请");
    }

    /**
     * 执行动作：审批通过
     * 状态：审批中 -> 已通过
     */
    public void doApprove(ApprovalStatus from, ApprovalStatus to, ApprovalEvent event, ApprovalContext context) {
        log.info("执行审批通过动作：工单 {} 从 {} 变为 {}",
                context.getApproval().getId(), from, to);

        Approval approval = context.getApproval();
        approval.setStatus(to.getCode());
        approval.setCurrentApproverId(null);

        // 记录审批历史
        String comment = context.getCmd() != null ? context.getCmd().getComment() : "同意";
        saveHistory(approval.getId(), context.getOperatorId(),
                ApprovalEvent.APPROVE.getCode(), comment);
    }

    /**
     * 执行动作：审批拒绝
     * 状态：审批中 -> 已打回
     */
    public void doReject(ApprovalStatus from, ApprovalStatus to, ApprovalEvent event, ApprovalContext context) {
        log.info("执行审批拒绝动作：工单 {} 从 {} 变为 {}",
                context.getApproval().getId(), from, to);

        Approval approval = context.getApproval();
        approval.setStatus(to.getCode());
        approval.setCurrentApproverId(null);

        // 记录审批历史
        String comment = context.getCmd() != null ? context.getCmd().getComment() : "拒绝";
        saveHistory(approval.getId(), context.getOperatorId(),
                ApprovalEvent.REJECT.getCode(), comment);
    }

    /**
     * 执行动作：重新编辑
     * 状态：已通过/已打回 -> 草稿
     */
    public void doReedit(ApprovalStatus from, ApprovalStatus to, ApprovalEvent event, ApprovalContext context) {
        log.info("执行重新编辑动作：工单 {} 从 {} 变为 {}",
                context.getApproval().getId(), from, to);

        Approval approval = context.getApproval();
        approval.setStatus(to.getCode());

        // 记录审批历史
        saveHistory(approval.getId(), context.getOperatorId(),
                ApprovalEvent.REEDIT.getCode(), "重新编辑");
    }

    /**
     * 执行动作：撤销申请
     * 状态：审批中 -> 草稿
     */
    public void doRevoke(ApprovalStatus from, ApprovalStatus to, ApprovalEvent event, ApprovalContext context) {
        log.info("执行撤销动作：工单 {} 从 {} 变为 {}",
                context.getApproval().getId(), from, to);

        Approval approval = context.getApproval();
        approval.setStatus(to.getCode());
        approval.setCurrentApproverId(null);

        // 记录审批历史
        String comment = context.getCmd() != null ? context.getCmd().getComment() : "撤销申请";
        saveHistory(approval.getId(), context.getOperatorId(),
                ApprovalEvent.REVOKE.getCode(), comment);
    }

    /**
     * 保存审批历史记录
     */
    private void saveHistory(Long approvalId, Long approverId, Integer action, String comment) {
        try {
            ApprovalHistory history = new ApprovalHistory();
            history.setApprovalId(approvalId);
            history.setApproverId(approverId);
            history.setAction(action);
            history.setComment(comment);
            history.setCreateTime(LocalDateTime.now());
            approvalHistoryMapper.insert(history);
        } catch (Exception e) {
            log.error("保存审批历史记录失败：approvalId={}, action={}", approvalId, action, e);
        }
    }
}
