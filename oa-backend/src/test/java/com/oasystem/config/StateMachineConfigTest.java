package com.oasystem.config;

import com.alibaba.cola.statemachine.StateMachine;
import com.oasystem.dto.ApprovalActionCmd;
import com.oasystem.entity.Approval;
import com.oasystem.enums.ApprovalEvent;
import com.oasystem.enums.ApprovalStatus;
import com.oasystem.entity.ApprovalHistory;
import com.oasystem.mapper.ApprovalHistoryMapper;
import com.oasystem.mapper.UserMapper;
import com.oasystem.statemachine.ApprovalContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 状态机配置集成测试
 * 验证完整的状态流转流程
 */
@SpringBootTest
class StateMachineConfigTest {

    @Autowired
    private StateMachine<ApprovalStatus, ApprovalEvent, ApprovalContext> approvalStateMachine;

    @MockBean
    private ApprovalHistoryMapper approvalHistoryMapper;

    @MockBean
    private UserMapper userMapper;

    @BeforeEach
    void setUp() {
        // 模拟历史记录保存
        when(approvalHistoryMapper.insert(any(ApprovalHistory.class))).thenReturn(1);

        // 模拟用户查询（权限检查需要）
        // 200L - 管理员用户（有全部权限）
        com.oasystem.entity.User adminUser = new com.oasystem.entity.User();
        adminUser.setId(200L);
        adminUser.setRoleId(1L);
        adminUser.setRoleName("admin");
        adminUser.setDeptId(1L);
        adminUser.setPermissions("[\"all\", \"approval:execute\", \"approval:execute:all\"]");
        when(userMapper.selectByIdWithRole(200L)).thenReturn(adminUser);

        // 999L - 普通员工（无代审批权限）- 用于测试无权限场景
        com.oasystem.entity.User normalUser = new com.oasystem.entity.User();
        normalUser.setId(999L);
        normalUser.setRoleId(3L);
        normalUser.setRoleName("employee");
        normalUser.setDeptId(2L);
        normalUser.setPermissions("[]"); // 无权限
        when(userMapper.selectByIdWithRole(999L)).thenReturn(normalUser);
    }

    private Approval createApproval(Long id, ApprovalStatus status, Long applicantId, Long currentApproverId) {
        Approval approval = new Approval();
        approval.setId(id);
        approval.setStatus(status.getCode());
        approval.setApplicantId(applicantId);
        approval.setCurrentApproverId(currentApproverId);
        approval.setTitle("测试工单");
        approval.setFormData("{}");
        return approval;
    }

    @Test
    @DisplayName("验证状态机Bean已正确创建")
    void stateMachine_ShouldBeCreated() {
        assertNotNull(approvalStateMachine);
        assertEquals(StateMachineConfig.STATE_MACHINE_ID, approvalStateMachine.getMachineId());
    }

    @Test
    @DisplayName("状态流转：草稿 -> 提交 -> 审批中")
    void transition_DraftToProcessing_ShouldSucceed() {
        // Given
        Approval approval = createApproval(1L, ApprovalStatus.DRAFT, 100L, null);
        ApprovalActionCmd cmd = new ApprovalActionCmd();
        cmd.setNextApproverId(200L);
        ApprovalContext context = new ApprovalContext(approval, cmd, 100L);

        // When
        ApprovalStatus result = approvalStateMachine.fireEvent(ApprovalStatus.DRAFT, ApprovalEvent.SUBMIT, context);

        // Then
        assertEquals(ApprovalStatus.PROCESSING, result);
        assertEquals(ApprovalStatus.PROCESSING.getCode(), approval.getStatus());
        assertEquals(200L, approval.getCurrentApproverId());
    }

    @Test
    @DisplayName("状态流转：审批中 -> 审批通过 -> 已通过")
    void transition_ProcessingToApproved_ShouldSucceed() {
        // Given
        Approval approval = createApproval(1L, ApprovalStatus.PROCESSING, 100L, 200L);
        ApprovalActionCmd cmd = new ApprovalActionCmd();
        cmd.setComment("同意");
        ApprovalContext context = new ApprovalContext(approval, cmd, 200L);

        // When
        ApprovalStatus result = approvalStateMachine.fireEvent(ApprovalStatus.PROCESSING, ApprovalEvent.APPROVE, context);

        // Then
        assertEquals(ApprovalStatus.APPROVED, result);
        assertEquals(ApprovalStatus.APPROVED.getCode(), approval.getStatus());
        assertNull(approval.getCurrentApproverId());
    }

    @Test
    @DisplayName("状态流转：审批中 -> 审批拒绝 -> 已打回")
    void transition_ProcessingToReturned_ShouldSucceed() {
        // Given
        Approval approval = createApproval(1L, ApprovalStatus.PROCESSING, 100L, 200L);
        ApprovalActionCmd cmd = new ApprovalActionCmd();
        cmd.setComment("材料不全");
        ApprovalContext context = new ApprovalContext(approval, cmd, 200L);

        // When
        ApprovalStatus result = approvalStateMachine.fireEvent(ApprovalStatus.PROCESSING, ApprovalEvent.REJECT, context);

        // Then
        assertEquals(ApprovalStatus.RETURNED, result);
        assertEquals(ApprovalStatus.RETURNED.getCode(), approval.getStatus());
        assertNull(approval.getCurrentApproverId());
    }

    @Test
    @DisplayName("状态流转：审批中 -> 撤销 -> 草稿")
    void transition_ProcessingToDraftByRevoke_ShouldSucceed() {
        // Given
        Approval approval = createApproval(1L, ApprovalStatus.PROCESSING, 100L, 200L);
        ApprovalContext context = new ApprovalContext(approval, new ApprovalActionCmd(), 100L);

        // When
        ApprovalStatus result = approvalStateMachine.fireEvent(ApprovalStatus.PROCESSING, ApprovalEvent.REVOKE, context);

        // Then
        assertEquals(ApprovalStatus.DRAFT, result);
        assertEquals(ApprovalStatus.DRAFT.getCode(), approval.getStatus());
        assertNull(approval.getCurrentApproverId());
    }

    @Test
    @DisplayName("状态流转：已通过 -> 重新编辑 -> 草稿")
    void transition_ApprovedToDraft_ShouldSucceed() {
        // Given
        Approval approval = createApproval(1L, ApprovalStatus.APPROVED, 100L, null);
        ApprovalContext context = new ApprovalContext(approval, null, 100L);

        // When
        ApprovalStatus result = approvalStateMachine.fireEvent(ApprovalStatus.APPROVED, ApprovalEvent.REEDIT, context);

        // Then
        assertEquals(ApprovalStatus.DRAFT, result);
        assertEquals(ApprovalStatus.DRAFT.getCode(), approval.getStatus());
    }

    @Test
    @DisplayName("状态流转：已打回 -> 重新编辑 -> 草稿")
    void transition_ReturnedToDraft_ShouldSucceed() {
        // Given
        Approval approval = createApproval(1L, ApprovalStatus.RETURNED, 100L, null);
        ApprovalContext context = new ApprovalContext(approval, null, 100L);

        // When
        ApprovalStatus result = approvalStateMachine.fireEvent(ApprovalStatus.RETURNED, ApprovalEvent.REEDIT, context);

        // Then
        assertEquals(ApprovalStatus.DRAFT, result);
        assertEquals(ApprovalStatus.DRAFT.getCode(), approval.getStatus());
    }

    @Test
    @DisplayName("非法流转：审批中 -> 提交（应失败）")
    void illegalTransition_ProcessingToSubmit_ShouldFail() {
        // Given
        Approval approval = createApproval(1L, ApprovalStatus.PROCESSING, 100L, 200L);
        approval.setFormData("{}");
        ApprovalContext context = new ApprovalContext(approval, null, 100L);

        // When
        ApprovalStatus result = approvalStateMachine.fireEvent(ApprovalStatus.PROCESSING, ApprovalEvent.SUBMIT, context);

        // Then
        assertEquals(ApprovalStatus.PROCESSING, result); // COLA 5.0: 非法流转返回源状态
        assertEquals(ApprovalStatus.PROCESSING.getCode(), approval.getStatus()); // 状态不变
    }

    @Test
    @DisplayName("非法流转：草稿 -> 审批同意（应失败）")
    void illegalTransition_DraftToApprove_ShouldFail() {
        // Given
        Approval approval = createApproval(1L, ApprovalStatus.DRAFT, 100L, null);
        ApprovalContext context = new ApprovalContext(approval, null, 100L);

        // When
        ApprovalStatus result = approvalStateMachine.fireEvent(ApprovalStatus.DRAFT, ApprovalEvent.APPROVE, context);

        // Then
        assertEquals(ApprovalStatus.DRAFT, result); // COLA 5.0: 非法流转返回源状态
        assertEquals(ApprovalStatus.DRAFT.getCode(), approval.getStatus());
    }

    @Test
    @DisplayName("条件不满足：草稿表单为空 -> 提交（应失败）")
    void transition_WithEmptyForm_ShouldFail() {
        // Given
        Approval approval = createApproval(1L, ApprovalStatus.DRAFT, 100L, null);
        approval.setFormData(null); // 空表单
        ApprovalContext context = new ApprovalContext(approval, null, 100L);

        // When
        ApprovalStatus result = approvalStateMachine.fireEvent(ApprovalStatus.DRAFT, ApprovalEvent.SUBMIT, context);

        // Then
        assertEquals(ApprovalStatus.DRAFT, result); // COLA 5.0: 条件不满足返回源状态
        assertEquals(ApprovalStatus.DRAFT.getCode(), approval.getStatus()); // 状态不变
    }

    @Test
    @DisplayName("条件不满足：非审批人 -> 审批同意（应失败）")
    void transition_NonApproverToApprove_ShouldFail() {
        // Given
        Approval approval = createApproval(1L, ApprovalStatus.PROCESSING, 100L, 200L);
        ApprovalContext context = new ApprovalContext(approval, new ApprovalActionCmd(), 999L); // 999不是审批人

        // When
        ApprovalStatus result = approvalStateMachine.fireEvent(ApprovalStatus.PROCESSING, ApprovalEvent.APPROVE, context);

        // Then
        assertEquals(ApprovalStatus.PROCESSING, result); // COLA 5.0: 条件不满足返回源状态
        assertEquals(ApprovalStatus.PROCESSING.getCode(), approval.getStatus());
    }

    @Test
    @DisplayName("条件不满足：非申请人 -> 撤销（应失败）")
    void transition_NonApplicantToRevoke_ShouldFail() {
        // Given
        Approval approval = createApproval(1L, ApprovalStatus.PROCESSING, 100L, 200L);
        ApprovalContext context = new ApprovalContext(approval, new ApprovalActionCmd(), 999L); // 999不是申请人

        // When
        ApprovalStatus result = approvalStateMachine.fireEvent(ApprovalStatus.PROCESSING, ApprovalEvent.REVOKE, context);

        // Then
        assertEquals(ApprovalStatus.PROCESSING, result); // COLA 5.0: 条件不满足返回源状态
        assertEquals(ApprovalStatus.PROCESSING.getCode(), approval.getStatus());
    }

    @Test
    @DisplayName("条件不满足：非申请人 -> 重新编辑（应失败）")
    void transition_NonApplicantToReedit_ShouldFail() {
        // Given
        Approval approval = createApproval(1L, ApprovalStatus.APPROVED, 100L, null);
        ApprovalContext context = new ApprovalContext(approval, null, 999L); // 999不是申请人

        // When
        ApprovalStatus result = approvalStateMachine.fireEvent(ApprovalStatus.APPROVED, ApprovalEvent.REEDIT, context);

        // Then
        assertEquals(ApprovalStatus.APPROVED, result); // COLA 5.0: 条件不满足返回源状态
        assertEquals(ApprovalStatus.APPROVED.getCode(), approval.getStatus());
    }
}
