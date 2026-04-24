package com.oasystem.service;

import com.oasystem.dto.*;
import com.oasystem.enums.ApproverStrategyType;
import com.oasystem.enums.ApproverType;
import com.oasystem.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 审批规则服务单元测试
 * 测试规则的增删改查及预览功能
 */
@SpringBootTest
@Transactional
class ApproverRuleServiceTest {

    @Autowired
    private ApproverRuleService ruleService;

    // 测试用户ID（来自实际数据库中的测试数据）
    private static final Long USER_ID_ADMIN = 1L;         // 系统管理员, 系统管理部(dept=4)
    private static final Long USER_ID_MANAGER = 2L;       // 张经理, 技术部(dept=1)
    private static final Long USER_ID_LISI = 3L;          // 李四, 财务部(dept=2)
    private static final Long USER_ID_ZHAO_MANAGER = 8L;  // 赵经理, 系统管理部(dept=4)

    private static final String TYPE_LEAVE = "LEAVE_FORM";
    private static final String TYPE_EXPENSE = "EXPENSE_FORM";

    @Test
    @DisplayName("创建审批规则")
    void testCreateRule() {
        ApproverRuleCreateRequest request = buildCreateRequest("测试创建规则");

        Long id = ruleService.create(request);

        assertNotNull(id);

        ApproverRuleResponse response = ruleService.getById(id);
        assertNotNull(response);
        assertEquals("测试创建规则", response.getName());
        assertEquals(ApproverStrategyType.DEPT_ROLE.getCode(), response.getStrategyType());
        assertEquals(ApproverType.SPECIFIC_ROLE.getCode(), response.getApproverType());
        assertEquals("[2]", response.getApproverValue());
        assertEquals(100, response.getPriority());
        assertEquals(1, response.getStatus());
    }

    @Test
    @DisplayName("更新审批规则")
    void testUpdateRule() {
        // 先创建
        Long id = ruleService.create(buildCreateRequest("更新前名称"));

        // 再更新
        ApproverRuleUpdateRequest request = new ApproverRuleUpdateRequest();
        request.setName("更新后名称");
        request.setStrategyType(ApproverStrategyType.FIXED_USER.getCode());
        request.setMatchConditions(Map.of("types", new String[]{"EXPENSE_FORM", "PURCHASE_FORM"}));
        request.setApproverType(ApproverType.SPECIFIC_USER.getCode());
        request.setApproverValue("[1]");
        request.setPriority(50);
        request.setStatus(0);
        request.setDescription("更新后的描述");
        request.setScopeType(1);

        Boolean success = ruleService.update(id, request);
        assertTrue(success);

        ApproverRuleResponse response = ruleService.getById(id);
        assertEquals("更新后名称", response.getName());
        assertEquals(ApproverStrategyType.FIXED_USER.getCode(), response.getStrategyType());
        assertEquals(ApproverType.SPECIFIC_USER.getCode(), response.getApproverType());
        assertEquals(50, response.getPriority());
        assertEquals(0, response.getStatus());
        assertEquals("更新后的描述", response.getDescription());
    }

    @Test
    @DisplayName("更新不存在的规则应抛出异常")
    void testUpdateNonExistentRule() {
        ApproverRuleUpdateRequest request = new ApproverRuleUpdateRequest();
        request.setName("测试");
        request.setStrategyType(ApproverStrategyType.FIXED_USER.getCode());
        request.setApproverType(ApproverType.SPECIFIC_USER.getCode());
        request.setApproverValue("[1]");
        request.setScopeType(1);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            ruleService.update(9999L, request);
        });

        assertEquals("审批规则不存在", exception.getMessage());
    }

    @Test
    @DisplayName("删除审批规则")
    void testDeleteRule() {
        Long id = ruleService.create(buildCreateRequest("待删除规则"));

        Boolean success = ruleService.delete(id);
        assertTrue(success);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            ruleService.getById(id);
        });
        assertEquals("审批规则不存在", exception.getMessage());
    }

    @Test
    @DisplayName("删除不存在的规则应抛出异常")
    void testDeleteNonExistentRule() {
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            ruleService.delete(9999L);
        });

        assertEquals("审批规则不存在", exception.getMessage());
    }

    @Test
    @DisplayName("根据ID查询规则")
    void testGetById() {
        Long id = ruleService.create(buildCreateRequest("查询测试"));

        ApproverRuleResponse response = ruleService.getById(id);

        assertNotNull(response);
        assertEquals(id, response.getId());
        assertEquals("查询测试", response.getName());
        assertNotNull(response.getStrategyTypeName());
        assertNotNull(response.getApproverTypeName());
    }

    @Test
    @DisplayName("分页查询规则列表")
    void testListRules() {
        // 创建几条规则
        ruleService.create(buildCreateRequest("列表测试1"));
        ruleService.create(buildCreateRequest("列表测试2"));

        ApproverRuleQuery query = new ApproverRuleQuery();
        query.setKeyword("列表测试");
        query.setCurrent(1);
        query.setSize(10);

        PageResult<ApproverRuleResponse> result = ruleService.list(query);

        assertNotNull(result);
        assertNotNull(result.getRecords());
        assertTrue(result.getTotal() >= 2);
        assertTrue(result.getRecords().stream().anyMatch(r -> r.getName().equals("列表测试1")));
        assertTrue(result.getRecords().stream().anyMatch(r -> r.getName().equals("列表测试2")));
    }

    @Test
    @DisplayName("按策略类型筛选")
    void testListRulesByStrategyType() {
        ruleService.create(buildCreateRequest("策略筛选测试"));

        ApproverRuleQuery query = new ApproverRuleQuery();
        query.setStrategyType(ApproverStrategyType.DEPT_ROLE.getCode());
        query.setCurrent(1);
        query.setSize(10);

        PageResult<ApproverRuleResponse> result = ruleService.list(query);

        assertNotNull(result);
        assertTrue(result.getRecords().stream().allMatch(r ->
            r.getStrategyType().equals(ApproverStrategyType.DEPT_ROLE.getCode())));
    }

    @Test
    @DisplayName("按状态筛选")
    void testListRulesByStatus() {
        ApproverRuleCreateRequest request = buildCreateRequest("状态筛选测试");
        request.setStatus(0);
        ruleService.create(request);

        ApproverRuleQuery query = new ApproverRuleQuery();
        query.setStatus(0);
        query.setCurrent(1);
        query.setSize(10);

        PageResult<ApproverRuleResponse> result = ruleService.list(query);

        assertNotNull(result);
        assertTrue(result.getRecords().stream().allMatch(r -> r.getStatus().equals(0)));
    }

    @Test
    @DisplayName("规则效果预览：系统管理部请假应解析到部门经理")
    void testPreview() {
        // 系统管理员(系统管理部) 请假，匹配系统管理部规则后自审跳过，兜底到赵经理
        ResolverResult result = ruleService.preview(USER_ID_ADMIN, TYPE_LEAVE);

        assertNotNull(result);
        assertTrue(result.isSuccess(), "预览应成功，实际消息：" + result.getMessage());
        assertEquals(USER_ID_ZHAO_MANAGER, result.getApproverId());
    }

    @Test
    @DisplayName("规则效果预览：申请人不存在应抛出异常")
    void testPreviewApplicantNotFound() {
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            ruleService.preview(9999L, TYPE_LEAVE);
        });

        assertEquals("申请人不存在", exception.getMessage());
    }

    @Test
    @DisplayName("规则效果预览：无匹配规则时走兜底策略，返回部门经理")
    void testPreviewNoMatch() {
        // 王五(人事部) 请假，无人事部专属规则，但人事部现有部门经理，兜底策略应成功
        ResolverResult result = ruleService.preview(5L, TYPE_LEAVE);

        assertNotNull(result);
        assertTrue(result.isSuccess(), "兜底策略应成功，实际消息：" + result.getMessage());
        assertNotNull(result.getApproverId());
    }

    @Test
    @DisplayName("枚举名称映射正确")
    void testEnumNameMapping() {
        Long id = ruleService.create(buildCreateRequest("枚举映射测试"));

        ApproverRuleResponse response = ruleService.getById(id);

        assertEquals(ApproverStrategyType.DEPT_ROLE.getLabel(), response.getStrategyTypeName());
        assertEquals(ApproverType.SPECIFIC_ROLE.getLabel(), response.getApproverTypeName());
    }

    // ==================== 辅助方法 ====================

    private ApproverRuleCreateRequest buildCreateRequest(String name) {
        ApproverRuleCreateRequest request = new ApproverRuleCreateRequest();
        request.setName(name);
        request.setStrategyType(ApproverStrategyType.DEPT_ROLE.getCode());
        request.setMatchConditions(Map.of("deptIds", new Long[]{1L}, "types", new String[]{"LEAVE_FORM", "OVERTIME_FORM"}));
        request.setApproverType(ApproverType.SPECIFIC_ROLE.getCode());
        request.setApproverValue("[2]");
        request.setPriority(100);
        request.setStatus(1);
        request.setDescription("测试规则");
        request.setScopeType(1);
        return request;
    }
}
