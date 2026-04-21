package com.oasystem.enums;

import lombok.Getter;

/**
 * 审批人类型枚举
 * v1.0 仅使用 SPECIFIC_USER(1) 和 SPECIFIC_ROLE(2)
 */
@Getter
public enum ApproverType {

    SPECIFIC_USER(1, "指定用户"),
    SPECIFIC_ROLE(2, "指定角色");

    private final Integer code;
    private final String label;

    ApproverType(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    public static ApproverType fromCode(Integer code) {
        for (ApproverType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
