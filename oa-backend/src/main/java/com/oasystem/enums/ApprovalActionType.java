package com.oasystem.enums;

import lombok.Getter;

/**
 * 审批操作类型枚举
 * 用于标识是直接审批还是代审批
 */
@Getter
public enum ApprovalActionType {

    /**
     * 直接审批 - 指定审批人审批自己的工单
     */
    DIRECT("DIRECT", "直接审批", false),

    /**
     * 管理员代审批 - 系统管理员审批非本人指派的工单
     */
    PROXY_ADMIN("PROXY_ADMIN", "管理员代审批", true),

    /**
     * 部门经理代审批 - 部门经理审批本部门非本人指派的工单
     */
    PROXY_MANAGER("PROXY_MANAGER", "部门经理代审批", true);

    private final String code;
    private final String label;
    private final boolean proxy;

    ApprovalActionType(String code, String label, boolean proxy) {
        this.code = code;
        this.label = label;
        this.proxy = proxy;
    }

    /**
     * 根据编码获取枚举
     */
    public static ApprovalActionType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (ApprovalActionType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 是否为代审批类型
     */
    public boolean isProxy() {
        return proxy;
    }
}
