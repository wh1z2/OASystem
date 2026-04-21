package com.oasystem.service;

import com.oasystem.dto.ApprovalCreateRequest;
import com.oasystem.entity.Approval;
import com.oasystem.enums.ApprovalStatus;
import com.oasystem.enums.ApprovalType;
import com.oasystem.exception.BusinessException;
import com.oasystem.mapper.ApprovalMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 审批服务回归测试
 * 重点验证默认审批人自动解析机制集成后，原有功能不受影响，新功能正常工作
 */
@SpringBootTest
@Transactional
class ApprovalServiceRegressionTest {

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private ApprovalMapper approvalMapper;

    // 测试用户ID
    private static final Long USER_ID_ADMIN = 1L;      // admin, 技术部
    private static final Long USER_ID_MANAGER = 2L;    // 张经理, 技术部, 有审批权限
    private static final Long USER_ID_LISI = 3L;       // 李四, 财务部
    private static final Long USER_ID_ZHANGSAN = 4L;   // 张三, 财务部
    private static final Long USER_ID_WANGWU = 5L;     // 王五, 人事部

    private static final Long USER_ID_NON_EXIST = 9999L;

    // 审批类型
    private static final Integer TYPE_LEAVE = 1;       // 请假（技术部有规则）
    private static final Integer TYPE_EXPENSE = 2;     // 报销（全局固定人员规则）
    private static final Integer TYPE_OVERTIME = 4;    // 加班（技术部有规则）

    /**
     * 原有功能：手动指定审批人时，优先使用手动指定，不走自动解析
     */
    @Test
    @DisplayName("create: 手动指定审批人时优先使用手动指定")
    void testCreateWithManualApprover() {
        ApprovalCreateRequest request = new ApprovalCreateRequest();
        request.setTitle("手动指定审批人测试");
        request.setType(TYPE_LEAVE);
        request.setContent("测试内容");
        request.setCurrentApproverId(USER_ID_MANAGER); // 手动指定张经理

        Long id = approvalService.create(request, USER_ID_LISI);

        assertNotNull(id);
        Approval approval = approvalMapper.selectById(id);
        assertNotNull(approval);
        assertEquals(USER_ID_MANAGER, approval.getCurrentApproverId());
    }

    /**
     * 原有功能：手动指定无权限的审批人时应抛出异常
     */
    @Test
    @DisplayName("create: 手动指定无权限的审批人应抛出异常")
    void testCreateWithUnauthorizedApprover() {
        ApprovalCreateRequest request = new ApprovalCreateRequest();
        request.setTitle("无权限审批人测试");
        request.setType(TYPE_LEAVE);
        request.setContent("测试内容");
        request.setCurrentApproverId(USER_ID_ZHANGSAN); // 张三没有审批权限

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            approvalService.create(request, USER_ID_LISI);
        });

        assertTrue(exception.getMessage().contains("无审批权限"));
    }

    /**
     * 新功能：未指定审批人时，自动解析（技术部请假 → 部门经理）
     */
    @Test
    @DisplayName("create: 未指定审批人时自动解析（技术部请假→张经理）")
    void testCreateWithAutoResolve_DeptRole() {
        ApprovalCreateRequest request = new ApprovalCreateRequest();
        request.setTitle("自动解析审批人测试");
        request.setType(TYPE_LEAVE);
        request.setContent("测试内容");
        // 不设置 currentApproverId

        Long id = approvalService.create(request, USER_ID_ADMIN);

        assertNotNull(id);
        Approval approval = approvalMapper.selectById(id);
        assertNotNull(approval);
        assertEquals(USER_ID_MANAGER, approval.getCurrentApproverId());
    }

    /**
     * 新功能：未指定审批人时，自动解析（报销 → 固定人员张经理）
     */
    @Test
    @DisplayName("create: 未指定审批人时自动解析（报销→张经理）")
    void testCreateWithAutoResolve_FixedUser() {
        ApprovalCreateRequest request = new ApprovalCreateRequest();
        request.setTitle("自动解析固定人员测试");
        request.setType(TYPE_EXPENSE);
        request.setContent("测试内容");
        // 不设置 currentApproverId

        Long id = approvalService.create(request, USER_ID_LISI);

        assertNotNull(id);
        Approval approval = approvalMapper.selectById(id);
        assertNotNull(approval);
        assertEquals(USER_ID_MANAGER, approval.getCurrentApproverId());
    }

    /**
     * 新功能：自动解析失败时，创建草稿仍成功（currentApproverId 为 null），但提交时会抛出异常
     */
    @Test
    @DisplayName("create: 自动解析失败时允许创建草稿，但提交时抛出异常")
    void testCreateWithAutoResolveFailure() {
        ApprovalCreateRequest request = new ApprovalCreateRequest();
        request.setTitle("自动解析失败测试");
        request.setType(TYPE_LEAVE);
        request.setContent("测试内容");
        request.setFormData(Map.of("test", true));
        // 不设置 currentApproverId

        // 王五(人事部) 请假，无人事部规则且无部门经理，创建草稿时应成功
        Long id = approvalService.create(request, USER_ID_WANGWU);
        assertNotNull(id);

        Approval approval = approvalMapper.selectById(id);
        assertNull(approval.getCurrentApproverId());

        // 提交时应因无法解析审批人而失败
        BusinessException exception = assertThrows(BusinessException.class, () -> {
            approvalService.submit(id, USER_ID_WANGWU);
        });

        assertTrue(exception.getMessage().contains("未找到匹配的审批规则")
                || exception.getMessage().contains("无法定位部门负责人"));
    }

    /**
     * 新功能：submit 时 currentApproverId 为 null，自动解析
     */
    @Test
    @DisplayName("submit: currentApproverId为null时自动解析")
    void testSubmitWithAutoResolve() {
        // 创建一个草稿工单，不指定审批人（此时数据库中 current_approver_id 应为 null）
        Long id = createDraftApprovalWithoutApprover("提交时自动解析", USER_ID_LISI, TYPE_EXPENSE);

        Approval before = approvalMapper.selectById(id);
        assertNull(before.getCurrentApproverId());

        // 提交时应自动解析审批人
        Boolean success = approvalService.submit(id, USER_ID_LISI);
        assertTrue(success);

        Approval after = approvalMapper.selectById(id);
        assertEquals(ApprovalStatus.PROCESSING.getCode(), after.getStatus());
        assertEquals(USER_ID_MANAGER, after.getCurrentApproverId());
    }

    /**
     * 原有功能：submit 时 currentApproverId 已存在，不应重新解析
     */
    @Test
    @DisplayName("submit: currentApproverId已存在时不重新解析")
    void testSubmitWithExistingApprover() {
        // 创建并指定审批人
        Long id = createDraftApproval("提交时不改变审批人", USER_ID_LISI, USER_ID_MANAGER);

        Approval before = approvalMapper.selectById(id);
        assertEquals(USER_ID_MANAGER, before.getCurrentApproverId());

        Boolean success = approvalService.submit(id, USER_ID_LISI);
        assertTrue(success);

        Approval after = approvalMapper.selectById(id);
        assertEquals(ApprovalStatus.PROCESSING.getCode(), after.getStatus());
        assertEquals(USER_ID_MANAGER, after.getCurrentApproverId());
    }

    /**
     * 原有功能：非申请人不能提交
     */
    @Test
    @DisplayName("submit: 非申请人不能提交")
    void testSubmitByNonApplicant() {
        Long id = createDraftApproval("权限测试", USER_ID_LISI, USER_ID_MANAGER);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            approvalService.submit(id, USER_ID_ZHANGSAN);
        });

        assertEquals("只有申请人可以提交工单", exception.getMessage());
    }

    /**
     * 原有功能：更新工单时可以修改审批人
     */
    @Test
    @DisplayName("update: 可以修改审批人")
    void testUpdateApprover() {
        Long id = createDraftApproval("更新审批人测试", USER_ID_LISI, USER_ID_MANAGER);

        com.oasystem.dto.ApprovalUpdateRequest request = new com.oasystem.dto.ApprovalUpdateRequest();
        request.setTitle("更新后标题");
        request.setCurrentApproverId(USER_ID_ADMIN); // 更换审批人为 admin

        Boolean success = approvalService.update(id, request);
        assertTrue(success);

        Approval approval = approvalMapper.selectById(id);
        assertEquals(USER_ID_ADMIN, approval.getCurrentApproverId());
    }

    /**
     * 原有功能：更新无权限的审批人应抛出异常
     */
    @Test
    @DisplayName("update: 更新为无权限的审批人应抛出异常")
    void testUpdateWithUnauthorizedApprover() {
        Long id = createDraftApproval("更新审批人权限测试", USER_ID_LISI, USER_ID_MANAGER);

        com.oasystem.dto.ApprovalUpdateRequest request = new com.oasystem.dto.ApprovalUpdateRequest();
        request.setCurrentApproverId(USER_ID_ZHANGSAN); // 张三无审批权限

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            approvalService.update(id, request);
        });

        assertTrue(exception.getMessage().contains("无审批权限"));
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建草稿工单（不指定审批人，用于测试自动解析）
     */
    private Long createDraftApprovalWithoutApprover(String title, Long applicantId, Integer type) {
        Approval approval = new Approval();
        approval.setTitle(title);
        approval.setType(type);
        approval.setApplicantId(applicantId);
        approval.setStatus(ApprovalStatus.DRAFT.getCode());
        approval.setPriority(1);
        approval.setContent("测试内容");
        approval.setFormData("{\"test\":true}"); // 状态机要求表单数据非空
        approvalMapper.insert(approval);
        return approval.getId();
    }

    /**
     * 创建草稿工单（通过 service，指定审批人）
     */
    private Long createDraftApproval(String title, Long applicantId, Long approverId) {
        ApprovalCreateRequest request = new ApprovalCreateRequest();
        request.setTitle(title);
        request.setType(TYPE_LEAVE);
        request.setContent("测试内容");
        request.setFormData(Map.of("test", true));
        request.setCurrentApproverId(approverId);
        return approvalService.create(request, applicantId);
    }
}
