package com.oasystem.enums;

import lombok.Getter;

/**
 * 审批人策略类型枚举
 * v1.0 仅实现 DEPT_ROLE(1) 和 FIXED_USER(3)
 */
@Getter
public enum ApproverStrategyType {

    DEPT_ROLE(1, "按部门角色"),
    FIXED_USER(3, "固定人员");

    private final Integer code;
    private final String label;

    ApproverStrategyType(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    public static ApproverStrategyType fromCode(Integer code) {
        for (ApproverStrategyType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
