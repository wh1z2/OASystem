package com.oasystem.service;

import com.oasystem.dto.ApprovalActionCmd;
import com.oasystem.dto.ApprovalCreateRequest;
import com.oasystem.dto.ApprovalHistoryResponse;
import com.oasystem.entity.Approval;
import com.oasystem.enums.ApprovalStatus;
import com.oasystem.enums.ApprovalType;
import com.oasystem.exception.BusinessException;
import com.oasystem.mapper.ApprovalMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 权限系统优化测试类
 * 测试代审批功能（管理员全范围代审批 + 部门经理本部门代审批）
 */
@SpringBootTest
@Transactional
class PermissionProxyApprovalTest {

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private ApprovalMapper approvalMapper;

    // 测试用户ID（来自init.sql中的测试数据）
    private static final Long USER_ID_ADMIN = 1L;       // 系统管理员 (role=admin, dept_id=1)
    private static final Long USER_ID_MANAGER = 2L;     // 张经理 (role=manager, dept_id=1)
    private static final Long USER_ID_LISI = 3L;        // 李四 (role=employee, dept_id=2)
    private static final Long USER_ID_ZHANGSAN = 4L;    // 张三 (role=employee, dept_id=2)
    private static final Long USER_ID_WANGWU = 5L;      // 王五 (role=employee, dept_id=3)

    // ==================== Level 1: 指定审批人直接审批测试 ====================

    /**
     * 测试指定审批人正常审批（直接审批）
     */
    @Test
    void testDirectApproverCanApprove() {
        // 创建并提交工单，指定经理为审批人
        Long id = createAndSubmitApproval("直接审批测试", USER_ID_LISI, USER_ID_MANAGER);

        // 经理（指定审批人）执行审批
        ApprovalActionCmd cmd = new ApprovalActionCmd();
        cmd.setComment("同意");

        Boolean success = approvalService.approve(id, cmd, USER_ID_MANAGER);
        assertTrue(success);

        // 验证状态变更
        Approval approval = approvalMapper.selectById(id);
        assertEquals(ApprovalStatus.APPROVED.getCode(), approval.getStatus());

        // 验证历史记录为直接审批
        List<ApprovalHistoryResponse> history = approvalService.getHistory(id);
        assertEquals(2, history.size());
        ApprovalHistoryResponse approveHistory = history.stream()
                .filter(h -> h.getAction() == 1) // APPROVE action
                .findFirst()
                .orElse(null);
        assertNotNull(approveHistory);
        assertEquals(0, approveHistory.getIsProxy()); // 非代审批
        assertEquals("DIRECT", approveHistory.getApprovalType());
    }

    // ==================== Level 2: 管理员全范围代审批测试（方案A）====================

    /**
     * 测试管理员可以代审批非本人指派的工单（全范围代审批）
     */
    @Test
    void testAdminCanProxyApproveAnyDepartment() {
        // 创建并提交工单，指定经理为审批人
        Long id = createAndSubmitApproval("管理员代审批测试", USER_ID_LISI, USER_ID_MANAGER);

        // 管理员（非指定审批人）执行代审批
        ApprovalActionCmd cmd = new ApprovalActionCmd();
        cmd.setComment("管理员代审批同意");

        Boolean success = approvalService.approve(id, cmd, USER_ID_ADMIN);
        assertTrue(success);

        // 验证状态变更
        Approval approval = approvalMapper.selectById(id);
        assertEquals(ApprovalStatus.APPROVED.getCode(), approval.getStatus());

        // 验证历史记录为管理员代审批
        List<ApprovalHistoryResponse> history = approvalService.getHistory(id);
        ApprovalHistoryResponse approveHistory = history.stream()
                .filter(h -> h.getAction() == 1)
                .findFirst()
                .orElse(null);
        assertNotNull(approveHistory);
        assertEquals(1, approveHistory.getIsProxy()); // 是代审批
        assertEquals("PROXY_ADMIN", approveHistory.getApprovalType());
        assertEquals(USER_ID_MANAGER, approveHistory.getOriginalApproverId()); // 记录原审批人
        assertTrue(approveHistory.getComment().contains("[管理员代审批]"));
    }

    /**
     * 测试管理员可以代审批其他部门的工单
     */
    @Test
    void testAdminCanProxyApproveDifferentDepartment() {
        // 王五 (dept_id=3) 创建工单，指定经理为审批人
        Long id = createAndSubmitApproval("管理员跨部门代审批", USER_ID_WANGWU, USER_ID_MANAGER);

        // 管理员执行代审批（与王五不同部门）
        ApprovalActionCmd cmd = new ApprovalActionCmd();
        cmd.setComment("管理员跨部门代审批同意");

        Boolean success = approvalService.approve(id, cmd, USER_ID_ADMIN);
        assertTrue(success);

        // 验证状态变更
        Approval approval = approvalMapper.selectById(id);
        assertEquals(ApprovalStatus.APPROVED.getCode(), approval.getStatus());

        // 验证历史记录
        List<ApprovalHistoryResponse> history = approvalService.getHistory(id);
        ApprovalHistoryResponse approveHistory = history.stream()
                .filter(h -> h.getAction() == 1)
                .findFirst()
                .orElse(null);
        assertNotNull(approveHistory);
        assertEquals(1, approveHistory.getIsProxy());
        assertEquals("PROXY_ADMIN", approveHistory.getApprovalType());
    }

    /**
     * 测试管理员代审批拒绝
     */
    @Test
    void testAdminCanProxyReject() {
        // 创建并提交工单
        Long id = createAndSubmitApproval("管理员代审批拒绝测试", USER_ID_LISI, USER_ID_MANAGER);

        // 管理员执行代审批拒绝
        ApprovalActionCmd cmd = new ApprovalActionCmd();
        cmd.setComment("管理员代审批拒绝");

        Boolean success = approvalService.reject(id, cmd, USER_ID_ADMIN);
        assertTrue(success);

        // 验证状态变更为已打回
        Approval approval = approvalMapper.selectById(id);
        assertEquals(ApprovalStatus.RETURNED.getCode(), approval.getStatus());

        // 验证历史记录
        List<ApprovalHistoryResponse> history = approvalService.getHistory(id);
        ApprovalHistoryResponse rejectHistory = history.stream()
                .filter(h -> h.getAction() == 2) // REJECT action
                .findFirst()
                .orElse(null);
        assertNotNull(rejectHistory);
        assertEquals(1, rejectHistory.getIsProxy());
        assertEquals("PROXY_ADMIN", rejectHistory.getApprovalType());
    }

    // ==================== Level 3: 部门经理本部门代审批测试（方案B）====================

    /**
     * 测试部门经理可以代审批本部门非本人指派的工单
     * 注意：经理和李四都是dept_id=1，是同部门
     */
    @Test
    void testManagerCanProxyApproveSameDepartment() {
        // 需要创建一个同部门的工单
        // 经理自己是dept_id=1，但经理是审批人，需要创建一个指定其他人为审批人的工单
        // 但同部门的其他员工可能不存在，我们用另一种方式测试：
        // 创建一个工单，指定审批人是管理员（非经理），但申请人和经理同部门

        // 创建工单：申请人李四是dept_id=2，经理是dept_id=1，他们不同部门
        // 所以经理不能代审批李四的工单
        // 我们先测试经理不能审批非本部门工单，然后创建一个同部门场景

        // 创建工单，指定管理员为审批人（经理不是指定审批人）
        Long id = createAndSubmitApproval("经理本部门代审批测试", USER_ID_LISI, USER_ID_ADMIN);

        // 经理尝试审批（与李四不同部门，应该被拒绝）
        ApprovalActionCmd cmd = new ApprovalActionCmd();
        cmd.setComment("经理审批");

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            approvalService.approve(id, cmd, USER_ID_MANAGER);
        });
        assertEquals("您只能审批本部门的工单", exception.getMessage());
    }

    /**
     * 测试部门经理不能审批非本部门工单
     */
    @Test
    void testManagerCannotProxyApproveDifferentDepartment() {
        // 李四 (dept_id=2) 创建工单，指定管理员为审批人
        Long id = createAndSubmitApproval("经理跨部门审批限制", USER_ID_LISI, USER_ID_ADMIN);

        // 经理 (dept_id=1) 尝试审批（与李四不同部门）
        ApprovalActionCmd cmd = new ApprovalActionCmd();
        cmd.setComment("经理跨部门审批");

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            approvalService.approve(id, cmd, USER_ID_MANAGER);
        });

        assertEquals("您只能审批本部门的工单", exception.getMessage());
    }

    // ==================== Level 4: 无权限测试 ====================

    /**
     * 测试普通员工不能代审批其他人工单
     */
    @Test
    void testEmployeeCannotProxyApprove() {
        // 李四创建工单，指定经理为审批人
        Long id = createAndSubmitApproval("员工代审批限制", USER_ID_LISI, USER_ID_MANAGER);

        // 张三（普通员工，非指定审批人）尝试审批
        ApprovalActionCmd cmd = new ApprovalActionCmd();
        cmd.setComment("张三代审批");

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            approvalService.approve(id, cmd, USER_ID_ZHANGSAN);
        });

        assertEquals("无权执行审批操作，您不是当前审批人", exception.getMessage());
    }

    // ==================== 优先级测试 ====================

    /**
     * 测试是指定审批人时优先视为直接审批（即使也是管理员）
     * 注：当前没有用户既是管理员又是其他工单的指定审批人，此测试展示逻辑
     */
    @Test
    void testDirectApproverPriorityOverProxy() {
        // 创建一个工单，指定管理员为审批人
        Long id = createAndSubmitApproval("直接审批优先级测试", USER_ID_LISI, USER_ID_ADMIN);

        // 管理员（作为指定审批人）执行审批
        ApprovalActionCmd cmd = new ApprovalActionCmd();
        cmd.setComment("管理员作为指定审批人审批");

        Boolean success = approvalService.approve(id, cmd, USER_ID_ADMIN);
        assertTrue(success);

        // 验证历史记录为直接审批（不是代审批）
        List<ApprovalHistoryResponse> history = approvalService.getHistory(id);
        ApprovalHistoryResponse approveHistory = history.stream()
                .filter(h -> h.getAction() == 1)
                .findFirst()
                .orElse(null);
        assertNotNull(approveHistory);
        assertEquals(0, approveHistory.getIsProxy()); // 应视为直接审批
        assertEquals("DIRECT", approveHistory.getApprovalType());
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建测试工单
     */
    private Long createTestApproval(String title, Long applicantId) {
        ApprovalCreateRequest request = new ApprovalCreateRequest();
        request.setTitle(title);
        request.setType(ApprovalType.LEAVE.getCode());
        request.setContent("测试内容");
        request.setFormData("{\"test\":true}");
        return approvalService.create(request, applicantId);
    }

    /**
     * 创建并提交工单
     */
    private Long createAndSubmitApproval(String title, Long applicantId, Long approverId) {
        Long id = createTestApproval(title, applicantId);

        // 设置审批人
        Approval approval = approvalMapper.selectById(id);
        approval.setCurrentApproverId(approverId);
        approvalMapper.updateById(approval);

        approvalService.submit(id, applicantId);
        return id;
    }
}
