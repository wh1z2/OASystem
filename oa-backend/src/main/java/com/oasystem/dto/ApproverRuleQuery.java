package com.oasystem.dto;

import lombok.Data;

/**
 * 审批规则查询参数
 */
@Data
public class ApproverRuleQuery {

    /**
     * 按规则名称模糊搜索
     */
    private String keyword;

    /**
     * 按策略类型筛选
     */
    private Integer strategyType;

    /**
     * 按状态筛选
     */
    private Integer status;

    private Integer current = 1;
    private Integer size = 10;

    /**
     * 排序字段
     */
    private String orderBy;

    /**
     * 排序方向：asc / desc
     */
    private String orderDirection;

    public Integer getPageNum() {
        return current;
    }

    public Integer getPageSize() {
        return size;
    }
}
