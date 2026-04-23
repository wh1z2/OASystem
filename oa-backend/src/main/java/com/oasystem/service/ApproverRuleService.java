package com.oasystem.service;

import com.oasystem.dto.*;

import java.util.List;

/**
 * 审批规则服务接口
 */
public interface ApproverRuleService {

    /**
     * 创建规则
     */
    Long create(ApproverRuleCreateRequest request);

    /**
     * 更新规则
     */
    Boolean update(Long id, ApproverRuleUpdateRequest request);

    /**
     * 删除规则
     */
    Boolean delete(Long id);

    /**
     * 根据ID查询
     */
    ApproverRuleResponse getById(Long id);

    /**
     * 分页查询规则列表
     */
    PageResult<ApproverRuleResponse> list(ApproverRuleQuery query);

    /**
     * 规则效果预览
     */
    ResolverResult preview(Long applicantId, String type);
}
