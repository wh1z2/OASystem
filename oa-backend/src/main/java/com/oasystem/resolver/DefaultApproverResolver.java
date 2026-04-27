package com.oasystem.resolver;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.oasystem.dto.ResolverResult;
import com.oasystem.entity.ApproverRule;
import com.oasystem.entity.User;
import com.oasystem.enums.ApproverStrategyType;
import com.oasystem.enums.ApproverType;
import com.oasystem.mapper.ApproverRuleMapper;
import com.oasystem.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 默认审批人解析引擎
 * v1.0 仅实现策略1（按部门角色）和策略3（固定人员）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultApproverResolver {

    private final ApproverRuleMapper ruleMapper;
    private final UserMapper userMapper;

    /**
     * 解析默认审批人
     *
     * @param applicantId  申请人ID
     * @param approvalType 审批类型
     * @return 解析结果
     */
    public ResolverResult resolve(Long applicantId, String approvalType) {
        // 1. 获取申请人信息
        User applicant = userMapper.selectByIdWithRole(applicantId);
        if (applicant == null) {
            log.warn("自动解析审批人失败：申请人不存在，applicantId={}", applicantId);
            return ResolverResult.failed("申请人不存在");
        }

        // 2. 查询所有启用的规则，按优先级排序
        List<ApproverRule> rules = ruleMapper.selectEnabledRulesOrderedByPriority();
        if (CollectionUtils.isEmpty(rules)) {
            log.warn("自动解析审批人失败：没有启用的审批规则");
            return fallbackResolve(applicant);
        }

        // 3. 依次匹配规则
        for (ApproverRule rule : rules) {
            if (isMatch(rule, applicant, approvalType)) {
                Long approverId = resolveApproverId(rule, applicant);
                if (approverId != null) {
                    // 防止自审
                    if (approverId.equals(applicantId)) {
                        log.warn("规则匹配成功但审批人是申请人本人，跳过：ruleId={}, applicantId={}", rule.getId(), applicantId);
                        continue;
                    }
                    String approverName = getApproverName(approverId);
                    log.info("自动解析审批人成功：ruleId={}, ruleName={}, approverId={}, applicantId={}",
                            rule.getId(), rule.getName(), approverId, applicantId);
                    return ResolverResult.success(approverId, rule.getId(), rule.getName(), approverName);
                } else {
                    log.warn("规则匹配成功但无法解析到有效审批人，继续匹配下一条：ruleId={}", rule.getId());
                }
            }
        }

        // 4. 无匹配规则时的兜底策略
        return fallbackResolve(applicant);
    }

    /**
     * 判断规则是否匹配当前申请
     */
    private boolean isMatch(ApproverRule rule, User applicant, String approvalType) {
        if (rule.getMatchConditions() == null || rule.getMatchConditions().isEmpty()) {
            // 无匹配条件表示匹配所有
            return true;
        }

        MatchCondition condition = JSON.parseObject(rule.getMatchConditions(), MatchCondition.class);

        // 校验部门匹配
        if (!CollectionUtils.isEmpty(condition.getDeptIds())) {
            if (applicant.getDeptId() == null || !condition.getDeptIds().contains(applicant.getDeptId())) {
                return false;
            }
        }

        // 校验审批类型匹配（兼容字符串编码和数字两种格式）
        if (!typeMatches(condition.getTypes(), approvalType)) {
            return false;
        }

        // 校验角色匹配
        if (!CollectionUtils.isEmpty(condition.getRoleIds())) {
            if (applicant.getRoleId() == null || !condition.getRoleIds().contains(applicant.getRoleId())) {
                return false;
            }
        }

        return true;
    }

    /**
     * 审批类型是否匹配
     * 兼容规则中存储的数字（"1"-"5"）和字符串编码（LEAVE_FORM 等）两种格式
     * 注意：数据库中 types 可能是 JSON 数字数组，FastJSON2 反序列化后元素类型为 Integer
     */
    private boolean typeMatches(List<?> ruleTypes, String approvalType) {
        if (CollectionUtils.isEmpty(ruleTypes)) {
            return true;
        }
        if (approvalType == null) {
            return false;
        }
        String numericType = mapTypeCodeToNumber(approvalType);
        for (Object ruleType : ruleTypes) {
            String rt = String.valueOf(ruleType);
            if (approvalType.equals(rt) || numericType.equals(rt)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 将审批类型的字符串编码映射为数字字符串
     */
    private String mapTypeCodeToNumber(String typeCode) {
        switch (typeCode) {
            case "LEAVE_FORM":
                return "1";
            case "EXPENSE_FORM":
                return "2";
            case "PURCHASE_FORM":
                return "3";
            case "OVERTIME_FORM":
                return "4";
            case "TRAVEL_FORM":
                return "5";
            default:
                return typeCode;
        }
    }

    /**
     * 根据规则解析出具体的审批人ID
     */
    private Long resolveApproverId(ApproverRule rule, User applicant) {
        ApproverStrategyType strategyType = ApproverStrategyType.fromCode(rule.getStrategyType());
        if (strategyType == null) {
            log.warn("未知的策略类型：strategyType={}", rule.getStrategyType());
            return null;
        }

        switch (strategyType) {
            case DEPT_ROLE:
                return resolveDeptRole(rule, applicant);
            case FIXED_USER:
                return resolveFixedUser(rule);
            default:
                log.warn("未实现的策略类型：{}", strategyType);
                return null;
        }
    }

    /**
     * 策略1：按部门角色解析审批人
     */
    private Long resolveDeptRole(ApproverRule rule, User applicant) {
        List<Long> approverIds = JSON.parseArray(rule.getApproverValue(), Long.class);
        if (CollectionUtils.isEmpty(approverIds)) {
            return null;
        }

        ApproverType approverType = ApproverType.fromCode(rule.getApproverType());
        if (approverType == null) {
            return null;
        }

        switch (approverType) {
            case SPECIFIC_USER:
                // 直接返回第一个有效用户
                for (Long userId : approverIds) {
                    User user = userMapper.selectById(userId);
                    if (user != null && user.getStatus() != null && user.getStatus() == 1) {
                        return user.getId();
                    }
                }
                return null;

            case SPECIFIC_ROLE:
                // 查找申请人所在部门下拥有指定角色的用户
                Long roleId = approverIds.get(0);
                LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
                wrapper.eq(User::getDeptId, applicant.getDeptId())
                        .eq(User::getRoleId, roleId)
                        .eq(User::getStatus, 1)
                        .last("LIMIT 1");
                User found = userMapper.selectOne(wrapper);
                return found != null ? found.getId() : null;

            default:
                return null;
        }
    }

    /**
     * 策略3：按固定人员解析审批人
     */
    private Long resolveFixedUser(ApproverRule rule) {
        List<Long> approverIds = JSON.parseArray(rule.getApproverValue(), Long.class);
        if (CollectionUtils.isEmpty(approverIds)) {
            return null;
        }

        // 返回第一个有效的用户
        for (Long userId : approverIds) {
            User user = userMapper.selectById(userId);
            if (user != null && user.getStatus() != null && user.getStatus() == 1) {
                return user.getId();
            }
        }
        return null;
    }

    /**
     * 兜底策略：查找申请人所在部门下 role_id = 2 (manager) 的用户
     */
    private ResolverResult fallbackResolve(User applicant) {
        if (applicant.getDeptId() == null) {
            return ResolverResult.failed("未找到匹配的审批规则，且申请人未分配部门，请联系管理员配置审批规则");
        }

        LambdaQueryWrapper<User> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(User::getDeptId, applicant.getDeptId())
                .eq(User::getRoleId, 2L)
                .eq(User::getStatus, 1)
                .last("LIMIT 1");
        User deptManager = userMapper.selectOne(wrapper);

        if (deptManager != null) {
            log.info("兜底策略生效：使用部门经理作为审批人，deptId={}, managerId={}",
                    applicant.getDeptId(), deptManager.getId());
            return ResolverResult.success(deptManager.getId(), null, "默认部门负责人兜底策略", deptManager.getName());
        }

        return ResolverResult.failed("未找到匹配的审批规则，且无法定位部门负责人，请联系管理员配置审批规则");
    }

    /**
     * 获取审批人姓名
     */
    private String getApproverName(Long approverId) {
        if (approverId == null) {
            return null;
        }
        User user = userMapper.selectById(approverId);
        return user != null ? user.getName() : null;
    }

    /**
     * 匹配条件内部类
     */
    private static class MatchCondition {
        private List<Long> deptIds;
        private List<String> types;
        private List<Long> roleIds;

        public List<Long> getDeptIds() {
            return deptIds;
        }

        public void setDeptIds(List<Long> deptIds) {
            this.deptIds = deptIds;
        }

        public List<String> getTypes() {
            return types;
        }

        public void setTypes(List<String> types) {
            this.types = types;
        }

        public List<Long> getRoleIds() {
            return roleIds;
        }

        public void setRoleIds(List<Long> roleIds) {
            this.roleIds = roleIds;
        }
    }
}
