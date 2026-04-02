package com.oasystem.statemachine;

import com.oasystem.dto.ApprovalActionCmd;
import com.oasystem.entity.Approval;
import com.oasystem.enums.ApprovalEvent;
import com.oasystem.enums.ApprovalStatus;
import com.oasystem.entity.ApprovalHistory;
import com.oasystem.mapper.ApprovalHistoryMapper;
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
 * 审批状态机测试类
 * 验证状态转换规则
 */
@SpringBootTest
class ApprovalStateMachineTest {

    @Autowired
    private ApprovalStateMachineHelper stateMachineHelper;

    @MockBean
    private ApprovalHistoryMapper approvalHistoryMapper;

    @BeforeEach
    void setUp() {
        // 模拟历史记录保存
        when(approvalHistoryMapper.insert(any(ApprovalHistory.class))).thenReturn(1);
    }

    /**
     * 创建测试用审批工单
     */
    private Approval createTestApproval(Long id, ApprovalStatus status, Long applicantId, Long currentApproverId) {
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
    @DisplayName("检查表单完整性：表单数据不为空时通过")
    void checkFormComplete_ShouldReturnTrue_WhenFormDataNotEmpty() {
        // Given
        Approval approval = createTestApproval(1L, ApprovalStatus.DRAFT, 100L, null);
        approval.setFormData("{\"field\": \"value\"}");
        ApprovalContext context = new ApprovalContext(approval, null, 100L);

        // When
        boolean result = stateMachineHelper.checkFormComplete(context);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("检查表单完整性：表单数据为空时失败")
    void checkFormComplete_ShouldReturnFalse_WhenFormDataEmpty() {
        // Given
        Approval approval = createTestApproval(1L, ApprovalStatus.DRAFT, 100L, null);
        approval.setFormData(null);
        ApprovalContext context = new ApprovalContext(approval, null, 100L);

        // When
        boolean result = stateMachineHelper.checkFormComplete(context);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("检查审批权限：当前用户是审批人时通过")
    void checkApproverPermission_ShouldReturnTrue_WhenUserIsApprover() {
        // Given
        Approval approval = createTestApproval(1L, ApprovalStatus.PROCESSING, 100L, 200L);
        ApprovalContext context = new ApprovalContext(approval, null, 200L);

        // When
        boolean result = stateMachineHelper.checkApproverPermission(context);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("检查审批权限：当前用户不是审批人时失败")
    void checkApproverPermission_ShouldReturnFalse_WhenUserIsNotApprover() {
        // Given
        Approval approval = createTestApproval(1L, ApprovalStatus.PROCESSING, 100L, 200L);
        ApprovalContext context = new ApprovalContext(approval, null, 999L);

        // When
        boolean result = stateMachineHelper.checkApproverPermission(context);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("检查申请人身份：当前用户是申请人时通过")
    void checkIsApplicant_ShouldReturnTrue_WhenUserIsApplicant() {
        // Given
        Approval approval = createTestApproval(1L, ApprovalStatus.PROCESSING, 100L, 200L);
        ApprovalContext context = new ApprovalContext(approval, null, 100L);

        // When
        boolean result = stateMachineHelper.checkIsApplicant(context);

        // Then
        assertTrue(result);
    }

    @Test
    @DisplayName("检查申请人身份：当前用户不是申请人时失败")
    void checkIsApplicant_ShouldReturnFalse_WhenUserIsNotApplicant() {
        // Given
        Approval approval = createTestApproval(1L, ApprovalStatus.PROCESSING, 100L, 200L);
        ApprovalContext context = new ApprovalContext(approval, null, 999L);

        // When
        boolean result = stateMachineHelper.checkIsApplicant(context);

        // Then
        assertFalse(result);
    }

    @Test
    @DisplayName("执行提交动作：工单状态变为审批中")
    void doSubmit_ShouldChangeStatusToProcessing() {
        // Given
        Approval approval = createTestApproval(1L, ApprovalStatus.DRAFT, 100L, null);
        approval.setFormData("{}");
        ApprovalActionCmd cmd = new ApprovalActionCmd();
        cmd.setNextApproverId(200L);
        ApprovalContext context = new ApprovalContext(approval, cmd, 100L);

        // When
        stateMachineHelper.doSubmit(ApprovalStatus.DRAFT, ApprovalStatus.PROCESSING,
                ApprovalEvent.SUBMIT, context);

        // Then
        assertEquals(ApprovalStatus.PROCESSING.getCode(), approval.getStatus());
        assertEquals(200L, approval.getCurrentApproverId());
    }

    @Test
    @DisplayName("执行审批通过动作：工单状态变为已通过，当前审批人清空")
    void doApprove_ShouldChangeStatusToApproved_AndClearApprover() {
        // Given
        Approval approval = createTestApproval(1L, ApprovalStatus.PROCESSING, 100L, 200L);
        ApprovalActionCmd cmd = new ApprovalActionCmd();
        cmd.setComment("同意申请");
        ApprovalContext context = new ApprovalContext(approval, cmd, 200L);

        // When
        stateMachineHelper.doApprove(ApprovalStatus.PROCESSING, ApprovalStatus.APPROVED,
                ApprovalEvent.APPROVE, context);

        // Then
        assertEquals(ApprovalStatus.APPROVED.getCode(), approval.getStatus());
        assertNull(approval.getCurrentApproverId());
    }

    @Test
    @DisplayName("执行审批拒绝动作：工单状态变为已打回，当前审批人清空")
    void doReject_ShouldChangeStatusToReturned_AndClearApprover() {
        // Given
        Approval approval = createTestApproval(1L, ApprovalStatus.PROCESSING, 100L, 200L);
        ApprovalActionCmd cmd = new ApprovalActionCmd();
        cmd.setComment("材料不全");
        ApprovalContext context = new ApprovalContext(approval, cmd, 200L);

        // When
        stateMachineHelper.doReject(ApprovalStatus.PROCESSING, ApprovalStatus.RETURNED,
                ApprovalEvent.REJECT, context);

        // Then
        assertEquals(ApprovalStatus.RETURNED.getCode(), approval.getStatus());
        assertNull(approval.getCurrentApproverId());
    }

    @Test
    @DisplayName("执行撤销动作：工单状态变为草稿，当前审批人清空")
    void doRevoke_ShouldChangeStatusToDraft_AndClearApprover() {
        // Given
        Approval approval = createTestApproval(1L, ApprovalStatus.PROCESSING, 100L, 200L);
        ApprovalActionCmd cmd = new ApprovalActionCmd();
        ApprovalContext context = new ApprovalContext(approval, cmd, 100L);

        // When
        stateMachineHelper.doRevoke(ApprovalStatus.PROCESSING, ApprovalStatus.DRAFT,
                ApprovalEvent.REVOKE, context);

        // Then
        assertEquals(ApprovalStatus.DRAFT.getCode(), approval.getStatus());
        assertNull(approval.getCurrentApproverId());
    }

    @Test
    @DisplayName("执行重新编辑动作：已通过的工单变为草稿")
    void doReedit_FromApproved_ShouldChangeStatusToDraft() {
        // Given
        Approval approval = createTestApproval(1L, ApprovalStatus.APPROVED, 100L, null);
        ApprovalContext context = new ApprovalContext(approval, null, 100L);

        // When
        stateMachineHelper.doReedit(ApprovalStatus.APPROVED, ApprovalStatus.DRAFT,
                ApprovalEvent.REEDIT, context);

        // Then
        assertEquals(ApprovalStatus.DRAFT.getCode(), approval.getStatus());
    }

    @Test
    @DisplayName("执行重新编辑动作：已打回的工单变为草稿")
    void doReedit_FromReturned_ShouldChangeStatusToDraft() {
        // Given
        Approval approval = createTestApproval(1L, ApprovalStatus.RETURNED, 100L, null);
        ApprovalContext context = new ApprovalContext(approval, null, 100L);

        // When
        stateMachineHelper.doReedit(ApprovalStatus.RETURNED, ApprovalStatus.DRAFT,
                ApprovalEvent.REEDIT, context);

        // Then
        assertEquals(ApprovalStatus.DRAFT.getCode(), approval.getStatus());
    }
}
