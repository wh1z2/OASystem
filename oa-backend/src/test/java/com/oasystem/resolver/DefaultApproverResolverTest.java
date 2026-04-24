package com.oasystem.resolver;

import com.oasystem.dto.ResolverResult;
import com.oasystem.entity.ApproverRule;
import com.oasystem.enums.ApproverStrategyType;
import com.oasystem.enums.ApproverType;
import com.oasystem.mapper.ApproverRuleMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 默认审批人解析引擎单元测试
 * 测试各种策略匹配、兜底策略、自审跳过等核心逻辑
 */
@SpringBootTest
@Transactional
class DefaultApproverResolverTest {

    @Autowired
    private DefaultApproverResolver resolver;

    @Autowired
    private ApproverRuleMapper ruleMapper;

    // 测试用户ID（来自实际数据库中的测试数据）
    private static final Long USER_ID_ADMIN = 1L;         // 系统管理员, 系统管理部(dept=4), role=admin
    private static final Long USER_ID_MANAGER = 2L;       // 张经理, 技术部(dept=1), role=manager
    private static final Long USER_ID_LISI = 3L;          // 李四, 财务部(dept=2), role=employee
    private static final Long USER_ID_ZHANGSAN = 4L;      // 张三, 财务部(dept=2), role=employee
    private static final Long USER_ID_WANGWU = 5L;        // 王五, 人事部(dept=3), role=employee
    private static final Long USER_ID_LIU_MANAGER = 6L;   // 刘经理, 财务部(dept=2), role=manager
    private static final Long USER_ID_LI_MANAGER = 7L;    // 李经理, 人事部(dept=3), role=manager
    private static final Long USER_ID_ZHAO_MANAGER = 8L;  // 赵经理, 系统管理部(dept=4), role=manager
    private static final Long USER_ID_NON_EXIST = 9999L;

    // 审批类型（字符串编码，与前端及 ApprovalCreateRequest.type 一致）
    private static final String TYPE_LEAVE = "LEAVE_FORM";       // 请假
    private static final String TYPE_EXPENSE = "EXPENSE_FORM";   // 报销
    private static final String TYPE_PURCHASE = "PURCHASE_FORM"; // 采购
    private static final String TYPE_OVERTIME = "OVERTIME_FORM"; // 加班
    private static final String TYPE_TRAVEL = "TRAVEL_FORM";     // 出差

    @Test
    @DisplayName("申请人不存在时应返回失败")
    void testResolveApplicantNotFound() {
        ResolverResult result = resolver.resolve(USER_ID_NON_EXIST, TYPE_LEAVE);

        assertNotNull(result);
        assertFalse(result.isSuccess());
        assertNull(result.getApproverId());
        assertTrue(result.getMessage().contains("申请人不存在"));
    }

    @Test
    @DisplayName("系统管理部员工请假应匹配规则并按部门角色解析到部门经理")
    void testResolveDeptRoleStrategy_SpecificRole() {
        // 系统管理员(系统管理部) 请假，匹配规则5(系统管理部规则)
        // 规则5 approverType=SPECIFIC_ROLE, approverValue=[1](admin角色)
        // 系统管理部中 role_id=1 的是系统管理员本人(自审) -> 跳过 -> 兜底 -> 赵经理
        ResolverResult result = resolver.resolve(USER_ID_ADMIN, TYPE_LEAVE);

        assertNotNull(result);
        assertTrue(result.isSuccess(), "解析应成功，实际消息：" + result.getMessage());
        assertEquals(USER_ID_ZHAO_MANAGER, result.getApproverId());
    }

    @Test
    @DisplayName("系统管理部员工加班应匹配规则并解析到部门经理")
    void testResolveDeptRoleStrategy_Overtime() {
        ResolverResult result = resolver.resolve(USER_ID_ADMIN, TYPE_OVERTIME);

        assertNotNull(result);
        assertTrue(result.isSuccess(), "解析应成功，实际消息：" + result.getMessage());
        assertEquals(USER_ID_ZHAO_MANAGER, result.getApproverId());
    }

    @Test
    @DisplayName("财务部员工报销应匹配规则2：固定人员解析到张经理")
    void testResolveFixedUserStrategy() {
        // 李四(财务部) 报销
        ResolverResult result = resolver.resolve(USER_ID_LISI, TYPE_EXPENSE);

        assertNotNull(result);
        assertTrue(result.isSuccess(), "解析应成功，实际消息：" + result.getMessage());
        assertEquals(USER_ID_MANAGER, result.getApproverId());
    }

    @Test
    @DisplayName("财务部员工采购应匹配规则2")
    void testResolveFixedUserStrategy_Purchase() {
        // 张三(财务部) 采购
        ResolverResult result = resolver.resolve(USER_ID_ZHANGSAN, TYPE_PURCHASE);

        assertNotNull(result);
        assertTrue(result.isSuccess(), "解析应成功，实际消息：" + result.getMessage());
        assertEquals(USER_ID_MANAGER, result.getApproverId());
    }

    @Test
    @DisplayName("人事部员工请假无匹配规则，兜底策略成功（返回部门经理）")
    void testResolveFallbackFails() {
        // 清理所有现有规则，避免其他测试插入的全局兜底规则干扰
        ruleMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>());

        // 王五(人事部, dept=3) 请假，人事部部门经理是李经理(id=7)
        ResolverResult result = resolver.resolve(USER_ID_WANGWU, TYPE_LEAVE);

        assertNotNull(result);
        assertTrue(result.isSuccess(), "兜底策略应成功，实际消息：" + result.getMessage());
        assertEquals(USER_ID_LI_MANAGER, result.getApproverId());
    }

    @Test
    @DisplayName("禁用状态的规则不应被匹配")
    void testDisabledRuleNotMatched() {
        // 创建一条禁用的规则，匹配所有报销，指定审批人为 admin
        ApproverRule disabledRule = new ApproverRule();
        disabledRule.setName("禁用规则测试");
        disabledRule.setStrategyType(ApproverStrategyType.FIXED_USER.getCode());
        disabledRule.setMatchConditions("{\"types\":[\"2\"]}");
        disabledRule.setApproverType(ApproverType.SPECIFIC_USER.getCode());
        disabledRule.setApproverValue("[" + USER_ID_ADMIN + "]");
        disabledRule.setPriority(5);
        disabledRule.setStatus(0); // 禁用
        disabledRule.setScopeType(1);
        ruleMapper.insert(disabledRule);

        // 李四报销，应该仍然匹配到规则2（张经理），而不是禁用的规则
        ResolverResult result = resolver.resolve(USER_ID_LISI, TYPE_EXPENSE);

        assertTrue(result.isSuccess());
        assertEquals(USER_ID_MANAGER, result.getApproverId());
    }

    @Test
    @DisplayName("优先级高的规则应优先被匹配")
    void testPriorityOrdering() {
        // 创建一条高优先级的规则：匹配所有报销，指定审批人为 admin
        ApproverRule highPriorityRule = new ApproverRule();
        highPriorityRule.setName("高优先级规则");
        highPriorityRule.setStrategyType(ApproverStrategyType.FIXED_USER.getCode());
        highPriorityRule.setMatchConditions("{\"types\":[\"2\"]}");
        highPriorityRule.setApproverType(ApproverType.SPECIFIC_USER.getCode());
        highPriorityRule.setApproverValue("[" + USER_ID_ADMIN + "]");
        highPriorityRule.setPriority(1); // 更高优先级
        highPriorityRule.setStatus(1);
        highPriorityRule.setScopeType(1);
        ruleMapper.insert(highPriorityRule);

        // 李四报销，应该匹配到高优先级规则（admin）
        ResolverResult result = resolver.resolve(USER_ID_LISI, TYPE_EXPENSE);

        assertTrue(result.isSuccess());
        assertEquals(USER_ID_ADMIN, result.getApproverId());
        assertEquals(highPriorityRule.getId(), result.getRuleId());
    }

    @Test
    @DisplayName("策略1 + 指定用户：直接返回指定用户")
    void testResolveDeptRoleStrategy_SpecificUser() {
        // 创建规则：技术部请假，直接指定 admin 为审批人
        ApproverRule rule = new ApproverRule();
        rule.setName("技术部请假直接指定");
        rule.setStrategyType(ApproverStrategyType.DEPT_ROLE.getCode());
        rule.setMatchConditions("{\"deptIds\":[1],\"types\":[\"1\"]}");
        rule.setApproverType(ApproverType.SPECIFIC_USER.getCode());
        rule.setApproverValue("[" + USER_ID_ADMIN + "]");
        rule.setPriority(1);
        rule.setStatus(1);
        rule.setScopeType(1);
        ruleMapper.insert(rule);

        ResolverResult result = resolver.resolve(USER_ID_MANAGER, TYPE_LEAVE);

        assertTrue(result.isSuccess());
        assertEquals(USER_ID_ADMIN, result.getApproverId());
    }

    @Test
    @DisplayName("自审防护：如果解析出的审批人是申请人本人，应跳过并继续匹配")
    void testSelfReviewPrevention() {
        // 创建一条系统管理部规则，把请假审批人指定为申请人自己（admin）
        ApproverRule selfRule = new ApproverRule();
        selfRule.setName("自审规则测试");
        selfRule.setStrategyType(ApproverStrategyType.DEPT_ROLE.getCode());
        selfRule.setMatchConditions("{\"deptIds\":[4],\"types\":[\"1\"]}");
        selfRule.setApproverType(ApproverType.SPECIFIC_USER.getCode());
        selfRule.setApproverValue("[" + USER_ID_ADMIN + "]"); // 指定admin自己
        selfRule.setPriority(1); // 最高优先级
        selfRule.setStatus(1);
        selfRule.setScopeType(1);
        ruleMapper.insert(selfRule);

        // 系统管理员(系统管理部) 请假，应该跳过自审规则，兜底返回部门经理赵经理
        ResolverResult result = resolver.resolve(USER_ID_ADMIN, TYPE_LEAVE);

        assertTrue(result.isSuccess());
        assertEquals(USER_ID_ZHAO_MANAGER, result.getApproverId());
    }

    @Test
    @DisplayName("无匹配条件（空JSON）表示匹配所有")
    void testEmptyMatchConditionMatchesAll() {
        // 创建一条无任何匹配条件的规则，匹配所有申请
        ApproverRule catchAllRule = new ApproverRule();
        catchAllRule.setName("全局兜底规则");
        catchAllRule.setStrategyType(ApproverStrategyType.FIXED_USER.getCode());
        catchAllRule.setMatchConditions("{}");
        catchAllRule.setApproverType(ApproverType.SPECIFIC_USER.getCode());
        catchAllRule.setApproverValue("[" + USER_ID_MANAGER + "]");
        catchAllRule.setPriority(100);
        catchAllRule.setStatus(1);
        catchAllRule.setScopeType(1);
        ruleMapper.insert(catchAllRule);

        // 王五(人事部) 请假，之前无匹配会失败，现在应该匹配到全局规则
        ResolverResult result = resolver.resolve(USER_ID_WANGWU, TYPE_LEAVE);

        assertTrue(result.isSuccess());
        assertEquals(USER_ID_MANAGER, result.getApproverId());
    }

    @Test
    @DisplayName("matchConditions为null表示匹配所有")
    void testNullMatchConditionMatchesAll() {
        // 创建一条 matchConditions 为 null 的规则
        ApproverRule catchAllRule = new ApproverRule();
        catchAllRule.setName("全局兜底规则-null条件");
        catchAllRule.setStrategyType(ApproverStrategyType.FIXED_USER.getCode());
        catchAllRule.setMatchConditions(null);
        catchAllRule.setApproverType(ApproverType.SPECIFIC_USER.getCode());
        catchAllRule.setApproverValue("[" + USER_ID_MANAGER + "]");
        catchAllRule.setPriority(100);
        catchAllRule.setStatus(1);
        catchAllRule.setScopeType(1);
        ruleMapper.insert(catchAllRule);

        ResolverResult result = resolver.resolve(USER_ID_WANGWU, TYPE_LEAVE);

        assertTrue(result.isSuccess());
        assertEquals(USER_ID_MANAGER, result.getApproverId());
    }

    @Test
    @DisplayName("固定人员策略：指定的用户不存在或已禁用时应返回null并继续匹配")
    void testFixedUserWithInvalidUser() {
        // 创建规则：指定一个不存在的用户
        ApproverRule invalidUserRule = new ApproverRule();
        invalidUserRule.setName("无效用户规则");
        invalidUserRule.setStrategyType(ApproverStrategyType.FIXED_USER.getCode());
        invalidUserRule.setMatchConditions("{\"types\":[\"2\"]}");
        invalidUserRule.setApproverType(ApproverType.SPECIFIC_USER.getCode());
        invalidUserRule.setApproverValue("[9999]"); // 不存在的用户
        invalidUserRule.setPriority(1);
        invalidUserRule.setStatus(1);
        invalidUserRule.setScopeType(1);
        ruleMapper.insert(invalidUserRule);

        // 李四报销，匹配到无效用户规则后无法解析，应继续匹配规则2（张经理）
        ResolverResult result = resolver.resolve(USER_ID_LISI, TYPE_EXPENSE);

        assertTrue(result.isSuccess());
        assertEquals(USER_ID_MANAGER, result.getApproverId());
    }
}
