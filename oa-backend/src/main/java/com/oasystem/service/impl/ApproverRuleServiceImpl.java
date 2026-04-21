package com.oasystem.service.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.oasystem.dto.*;
import com.oasystem.entity.ApproverRule;
import com.oasystem.entity.User;
import com.oasystem.enums.ApproverStrategyType;
import com.oasystem.enums.ApproverType;
import com.oasystem.exception.BusinessException;
import com.oasystem.mapper.ApproverRuleMapper;
import com.oasystem.mapper.UserMapper;
import com.oasystem.resolver.DefaultApproverResolver;
import com.oasystem.service.ApproverRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 审批规则服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApproverRuleServiceImpl implements ApproverRuleService {

    private final ApproverRuleMapper ruleMapper;
    private final UserMapper userMapper;
    private final DefaultApproverResolver defaultApproverResolver;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ApproverRuleCreateRequest request) {
        ApproverRule rule = new ApproverRule();
        rule.setName(request.getName());
        rule.setStrategyType(request.getStrategyType());
        if (request.getMatchConditions() != null) {
            rule.setMatchConditions(JSON.toJSONString(request.getMatchConditions()));
        }
        rule.setApproverType(request.getApproverType());
        rule.setApproverValue(request.getApproverValue());
        rule.setPriority(request.getPriority());
        rule.setStatus(request.getStatus());
        rule.setDescription(request.getDescription());
        rule.setScopeType(request.getScopeType());

        ruleMapper.insert(rule);
        log.info("创建审批规则成功：id={}, name={}", rule.getId(), rule.getName());
        return rule.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean update(Long id, ApproverRuleUpdateRequest request) {
        ApproverRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new BusinessException("审批规则不存在");
        }

        rule.setName(request.getName());
        rule.setStrategyType(request.getStrategyType());
        if (request.getMatchConditions() != null) {
            rule.setMatchConditions(JSON.toJSONString(request.getMatchConditions()));
        }
        rule.setApproverType(request.getApproverType());
        rule.setApproverValue(request.getApproverValue());
        rule.setPriority(request.getPriority());
        rule.setStatus(request.getStatus());
        rule.setDescription(request.getDescription());
        rule.setScopeType(request.getScopeType());

        int rows = ruleMapper.updateById(rule);
        log.info("更新审批规则成功：id={}", id);
        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(Long id) {
        ApproverRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new BusinessException("审批规则不存在");
        }

        int rows = ruleMapper.deleteById(id);
        log.info("删除审批规则成功：id={}", id);
        return rows > 0;
    }

    @Override
    public ApproverRuleResponse getById(Long id) {
        ApproverRule rule = ruleMapper.selectById(id);
        if (rule == null) {
            throw new BusinessException("审批规则不存在");
        }
        return convertToResponse(rule);
    }

    @Override
    public PageResult<ApproverRuleResponse> list(ApproverRuleQuery query) {
        LambdaQueryWrapper<ApproverRule> wrapper = Wrappers.lambdaQuery();

        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.like(ApproverRule::getName, query.getKeyword());
        }
        if (query.getStrategyType() != null) {
            wrapper.eq(ApproverRule::getStrategyType, query.getStrategyType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(ApproverRule::getStatus, query.getStatus());
        }

        // 动态排序
        if ("priority".equals(query.getOrderBy())) {
            if ("asc".equalsIgnoreCase(query.getOrderDirection())) {
                wrapper.orderByAsc(ApproverRule::getPriority);
            } else {
                wrapper.orderByDesc(ApproverRule::getPriority);
            }
        } else if ("name".equals(query.getOrderBy())) {
            if ("asc".equalsIgnoreCase(query.getOrderDirection())) {
                wrapper.orderByAsc(ApproverRule::getName);
            } else {
                wrapper.orderByDesc(ApproverRule::getName);
            }
        } else {
            wrapper.orderByDesc(ApproverRule::getCreateTime);
        }

        List<ApproverRule> rules = ruleMapper.selectList(wrapper);
        long total = ruleMapper.selectCount(wrapper);

        List<ApproverRuleResponse> responses = rules.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());

        return PageResult.of(responses, total, query.getPageNum(), query.getPageSize());
    }

    @Override
    public ResolverResult preview(Long applicantId, Integer type) {
        User applicant = userMapper.selectByIdWithRole(applicantId);
        if (applicant == null) {
            throw new BusinessException("申请人不存在");
        }
        return defaultApproverResolver.resolve(applicantId, type);
    }

    /**
     * 转换为响应DTO
     */
    private ApproverRuleResponse convertToResponse(ApproverRule rule) {
        ApproverRuleResponse response = new ApproverRuleResponse();
        response.setId(rule.getId());
        response.setName(rule.getName());
        response.setStrategyType(rule.getStrategyType());

        ApproverStrategyType strategyType = ApproverStrategyType.fromCode(rule.getStrategyType());
        response.setStrategyTypeName(strategyType != null ? strategyType.getLabel() : "");

        if (rule.getMatchConditions() != null) {
            response.setMatchConditions(JSON.parseObject(rule.getMatchConditions()));
        }

        response.setApproverType(rule.getApproverType());

        ApproverType approverType = ApproverType.fromCode(rule.getApproverType());
        response.setApproverTypeName(approverType != null ? approverType.getLabel() : "");

        response.setApproverValue(rule.getApproverValue());
        response.setPriority(rule.getPriority());
        response.setStatus(rule.getStatus());
        response.setDescription(rule.getDescription());
        response.setScopeType(rule.getScopeType());
        response.setCreateTime(rule.getCreateTime());
        response.setUpdateTime(rule.getUpdateTime());
        return response;
    }
}
