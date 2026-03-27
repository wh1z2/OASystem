package com.oasystem.enums;

import lombok.Getter;

/**
 * 优先级枚举
 */
@Getter
public enum Priority {

    LOW(0, "低"),
    NORMAL(1, "普通"),
    HIGH(2, "紧急");

    private final Integer code;
    private final String label;

    Priority(Integer code, String label) {
        this.code = code;
        this.label = label;
    }

    public static Priority fromCode(Integer code) {
        for (Priority priority : values()) {
            if (priority.code.equals(code)) {
                return priority;
            }
        }
        return NORMAL;
    }
}
