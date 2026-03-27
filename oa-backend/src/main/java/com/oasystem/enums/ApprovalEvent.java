package com.oasystem.enums;

import lombok.Getter;

/**
 * 审批事件枚举（状态机事件）
 */
@Getter
public enum ApprovalEvent {

    SUBMIT(0, "提交"),
    APPROVE(1, "审批同意"),
    REJECT(2, "审批不同意"),
    REEDIT(3, "重新编辑"),
    REVOKE(4, "撤销");

    private final Integer code;
    private final String label;

    ApprovalEvent(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    public static ApprovalEvent fromCode(Integer code) {
        for (ApprovalEvent event : values()) {
            if (event.code.equals(code)) {
                return event;
            }
        }
        return null;
    }
}
