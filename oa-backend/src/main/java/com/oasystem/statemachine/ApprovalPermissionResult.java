package com.oasystem.statemachine;

import com.oasystem.enums.ApprovalActionType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 审批权限检查结果类
 * 封装权限检查的详细结果，包括是否允许、审批类型、原审批人等信息
 */
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApprovalPermissionResult {

    /**
     * 是否允许执行审批操作
     */
    private final boolean granted;

    /**
     * 提示信息（允许或拒绝的原因）
     */
    private final String message;

    /**
     * 审批操作类型（直接审批/代审批）
     */
    private final ApprovalActionType approvalActionType;

    /**
     * 原指定审批人ID（代审批时记录）
     */
    private final Long originalApproverId;

    /**
     * 允许直接审批
     *
     * @param type    审批类型
     * @param message 提示信息
     * @return 权限检查结果
     */
    public static ApprovalPermissionResult granted(ApprovalActionType type, String message) {
        return new ApprovalPermissionResult(true, message, type, null);
    }

    /**
     * 允许代审批
     *
     * @param type               审批类型
     * @param message            提示信息
     * @param originalApproverId 原指定审批人ID
     * @return 权限检查结果
     */
    public static ApprovalPermissionResult granted(ApprovalActionType type, String message, Long originalApproverId) {
        return new ApprovalPermissionResult(true, message, type, originalApproverId);
    }

    /**
     * 拒绝审批
     *
     * @param message 拒绝原因
     * @return 权限检查结果
     */
    public static ApprovalPermissionResult denied(String message) {
        return new ApprovalPermissionResult(false, message, null, null);
    }

    /**
     * 判断是否为代审批
     *
     * @return true表示是代审批
     */
    public boolean isProxyApproval() {
        return approvalActionType != null && approvalActionType.isProxy();
    }

    /**
     * 获取审批类型编码
     *
     * @return 审批类型编码，如为null则返回null
     */
    public String getApprovalTypeCode() {
        return approvalActionType != null ? approvalActionType.getCode() : null;
    }

    /**
     * 获取审批类型标签
     *
     * @return 审批类型标签，如为null则返回null
     */
    public String getApprovalTypeLabel() {
        return approvalActionType != null ? approvalActionType.getLabel() : null;
    }
}
