package com.oasystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 更新审批规则请求DTO
 */
@Data
public class ApproverRuleUpdateRequest {

    @NotBlank(message = "规则名称不能为空")
    private String name;

    @NotNull(message = "策略类型不能为空")
    private Integer strategyType;

    /**
     * 匹配条件：{"deptIds":[1], "types":[1,2], "roleIds":[2]}
     */
    private Map<String, Object> matchConditions;

    @NotNull(message = "审批人类型不能为空")
    private Integer approverType;

    /**
     * 审批人值：用户ID列表或角色ID列表
     */
    @NotNull(message = "审批人值不能为空")
    private String approverValue;

    private Integer priority = 100;

    private Integer status = 1;

    private String description;

    @NotNull(message = "作用范围不能为空")
    private Integer scopeType;
}
