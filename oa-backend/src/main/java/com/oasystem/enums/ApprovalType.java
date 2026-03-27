package com.oasystem.enums;

import lombok.Getter;

/**
 * 审批类型枚举
 */
@Getter
public enum ApprovalType {

    LEAVE(1, "请假申请"),
    EXPENSE(2, "报销申请"),
    PURCHASE(3, "采购申请"),
    OVERTIME(4, "加班申请"),
    TRAVEL(5, "出差申请");

    private final Integer code;
    private final String label;

    ApprovalType(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    public static ApprovalType fromCode(Integer code) {
        for (ApprovalType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
