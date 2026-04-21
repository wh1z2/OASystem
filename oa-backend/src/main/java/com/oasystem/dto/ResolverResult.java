package com.oasystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 审批人解析结果
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResolverResult {

    /**
     * 是否解析成功
     */
    private boolean success;

    /**
     * 解析出的审批人ID
     */
    private Long approverId;

    /**
     * 匹配到的规则ID
     */
    private Long ruleId;

    /**
     * 匹配到的规则名称
     */
    private String ruleName;

    /**
     * 提示信息
     */
    private String message;

    public static ResolverResult success(Long approverId, Long ruleId, String ruleName) {
        return new ResolverResult(true, approverId, ruleId, ruleName, "匹配成功");
    }

    public static ResolverResult failed(String message) {
        return new ResolverResult(false, null, null, null, message);
    }
}
