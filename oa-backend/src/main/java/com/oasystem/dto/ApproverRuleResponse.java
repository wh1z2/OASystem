package com.oasystem.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 审批规则响应DTO
 */
@Data
public class ApproverRuleResponse {

    private Long id;
    private String name;
    private Integer strategyType;
    private String strategyTypeName;
    private Map<String, Object> matchConditions;
    private Integer approverType;
    private String approverTypeName;
    private String approverValue;
    private Integer priority;
    private Integer status;
    private String description;
    private Integer scopeType;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
