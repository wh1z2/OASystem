package com.oasystem.enums;

import lombok.Getter;

/**
 * 审批状态枚举
 */
@Getter
public enum ApprovalStatus {

    DRAFT(0, "草稿"),
    PROCESSING(1, "审批中"),
    APPROVED(2, "已通过"),
    RETURNED(3, "已打回"),
    REVOKED(4, "已撤销");

    private final Integer code;
    private final String label;

    ApprovalStatus(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    public static ApprovalStatus fromCode(Integer code) {
        for (ApprovalStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        return null;
    }
}
