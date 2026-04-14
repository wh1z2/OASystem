package com.oasystem.service;

import com.oasystem.dto.*;
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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 审批工单服务测试类
 * 测试阶段六：审批流程核心功能
 */
@SpringBootTest
@Transactional
class ApprovalServiceTest {

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private ApprovalMapper approvalMapper;

    // 测试用户ID（来自init.sql中的测试数据）
    private static final Long USER_ID_ZHANGSAN = 4L;  // 张三
    private static final Long USER_ID_LISI = 3L;      // 李四
    private static final Long USER_ID_MANAGER = 2L;   // 张经理

    /**
     * 步骤一：测试创建审批工单
     */
    @Test
    void testCreateApproval() {
        ApprovalCreateRequest request = new ApprovalCreateRequest();
        request.setTitle("测试创建审批");
        request.setType(ApprovalType.LEAVE.getCode());
        request.setContent("测试内容");
        request.setFormData(Map.of("reason", "测试"));
        request.setCurrentApproverId(USER_ID_MANAGER);

        Long id = approvalService.create(request, USER_ID_LISI);

        assertNotNull(id);

        Approval approval = approvalMapper.selectById(id);
        assertNotNull(approval);
        assertEquals("测试创建审批", approval.getTitle());
        assertEquals(ApprovalStatus.DRAFT.getCode(), approval.getStatus());
        assertEquals(USER_ID_LISI, approval.getApplicantId());
    }

    /**
     * 步骤二：测试查询工单详情
     */
    @Test
    void testGetApprovalById() {
        // 先创建一个工单
        Long id = createTestApproval("测试查询", USER_ID_LISI);

        ApprovalDetailResponse response = approvalService.getById(id);

        assertNotNull(response);
        assertEquals(id, response.getId());
        assertEquals("测试查询", response.getTitle());
        assertEquals(ApprovalStatus.DRAFT.getCode(), response.getStatus());
    }

    /**
     * 步骤二：测试分页查询工单列表
     */
    @Test
    void testListApprovals() {
        // 创建多个工单
        createTestApproval("查询测试1", USER_ID_LISI);
        createTestApproval("查询测试2", USER_ID_LISI);

        ApprovalQuery query = new ApprovalQuery();
        query.setApplicantId(USER_ID_LISI);
        query.setCurrent(1);
        query.setSize(10);

        PageResult<ApprovalDetailResponse> result = approvalService.list(query);

        assertNotNull(result);
        assertTrue(result.getTotal() >= 2);
        assertNotNull(result.getRecords());
    }

    /**
     * 步骤三：测试提交申请
     */
    @Test
    void testSubmitApproval() {
        // 创建草稿工单
        Long id = createTestApproval("测试提交", USER_ID_LISI);
        Approval approval = approvalMapper.selectById(id);
        assertEquals(ApprovalStatus.DRAFT.getCode(), approval.getStatus());

        // 提交申请
        Boolean success = approvalService.submit(id, USER_ID_LISI);
        assertTrue(success);

        // 验证状态变更
        approval = approvalMapper.selectById(id);
        assertEquals(ApprovalStatus.PROCESSING.getCode(), approval.getStatus());
    }

    /**
     * 步骤三：测试非申请人不能提交
     */
    @Test
    void testSubmitByNonApplicant() {
        Long id = createTestApproval("测试提交权限", USER_ID_LISI);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            approvalService.submit(id, USER_ID_ZHANGSAN);
        });

        assertEquals("只有申请人可以提交工单", exception.getMessage());
    }

    /**
     * 步骤四：测试审批通过
     */
    @Test
    void testApproveApproval() {
        // 创建并提交工单
        Long id = createAndSubmitApproval("测试通过", USER_ID_LISI, USER_ID_MANAGER);

        // 审批通过
        ApprovalActionCmd cmd = new ApprovalActionCmd();
        cmd.setComment("同意");

        Boolean success = approvalService.approve(id, cmd, USER_ID_MANAGER);
        assertTrue(success);

        // 验证状态变更
        Approval approval = approvalMapper.selectById(id);
        assertEquals(ApprovalStatus.APPROVED.getCode(), approval.getStatus());
    }

    /**
     * 步骤四：测试审批拒绝
     */
    @Test
    void testRejectApproval() {
        // 创建并提交工单
        Long id = createAndSubmitApproval("测试拒绝", USER_ID_LISI, USER_ID_MANAGER);

        // 审批拒绝
        ApprovalActionCmd cmd = new ApprovalActionCmd();
        cmd.setComment("信息不完整");

        Boolean success = approvalService.reject(id, cmd, USER_ID_MANAGER);
        assertTrue(success);

        // 验证状态变更
        Approval approval = approvalMapper.selectById(id);
        assertEquals(ApprovalStatus.RETURNED.getCode(), approval.getStatus());
    }

    /**
     * 步骤四：测试非审批人不能审批
     */
    @Test
    void testApproveByNonApprover() {
        // 创建并提交工单，指定经理为审批人
        Long id = createAndSubmitApproval("测试审批权限", USER_ID_LISI, USER_ID_MANAGER);

        // 张三尝试审批
        ApprovalActionCmd cmd = new ApprovalActionCmd();
        cmd.setComment("同意");

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            approvalService.approve(id, cmd, USER_ID_ZHANGSAN);
        });

        assertEquals("无权执行审批操作，您不是当前审批人", exception.getMessage());
    }

    /**
     * 步骤五：测试重新编辑
     */
    @Test
    void testReeditApproval() {
        // 创建、提交、审批通过
        Long id = createSubmitAndApprove("测试重新编辑", USER_ID_LISI, USER_ID_MANAGER);

        // 重新编辑
        Boolean success = approvalService.reedit(id, USER_ID_LISI);
        assertTrue(success);

        // 验证状态变更
        Approval approval = approvalMapper.selectById(id);
        assertEquals(ApprovalStatus.DRAFT.getCode(), approval.getStatus());
    }

    /**
     * 步骤五：测试非申请人不能重新编辑
     */
    @Test
    void testReeditByNonApplicant() {
        // 创建、提交、审批通过
        Long id = createSubmitAndApprove("测试重新编辑权限", USER_ID_LISI, USER_ID_MANAGER);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            approvalService.reedit(id, USER_ID_ZHANGSAN);
        });

        assertEquals("当前状态不允许重新编辑", exception.getMessage());
    }

    /**
     * 步骤六：测试撤销申请
     */
    @Test
    void testRevokeApproval() {
        // 创建并提交工单
        Long id = createAndSubmitApproval("测试撤销", USER_ID_LISI, USER_ID_MANAGER);

        // 撤销申请
        Boolean success = approvalService.revoke(id, USER_ID_LISI);
        assertTrue(success);

        // 验证状态变更
        Approval approval = approvalMapper.selectById(id);
        assertEquals(ApprovalStatus.DRAFT.getCode(), approval.getStatus());
    }

    /**
     * 步骤六：测试非申请人不能撤销
     */
    @Test
    void testRevokeByNonApplicant() {
        // 创建并提交工单
        Long id = createAndSubmitApproval("测试撤销权限", USER_ID_LISI, USER_ID_MANAGER);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            approvalService.revoke(id, USER_ID_ZHANGSAN);
        });

        assertEquals("无权撤销该工单，您不是申请人", exception.getMessage());
    }

    /**
     * 步骤六：测试非审批中状态不能撤销
     */
    @Test
    void testRevokeNonProcessingApproval() {
        // 创建草稿工单（未提交）
        Long id = createTestApproval("测试撤销状态限制", USER_ID_LISI);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            approvalService.revoke(id, USER_ID_LISI);
        });

        assertEquals("只有审批中的工单可以撤销", exception.getMessage());
    }

    /**
     * 步骤七：测试获取待办列表
     */
    @Test
    void testGetTodoList() {
        // 创建并提交工单
        createAndSubmitApproval("待办测试1", USER_ID_LISI, USER_ID_MANAGER);
        createAndSubmitApproval("待办测试2", USER_ID_ZHANGSAN, USER_ID_MANAGER);

        ApprovalQuery query = new ApprovalQuery();
        query.setCurrent(1);
        query.setSize(10);

        PageResult<ApprovalDetailResponse> result = approvalService.getTodoList(USER_ID_MANAGER, query);

        assertNotNull(result);
        assertTrue(result.getTotal() >= 2);
    }

    /**
     * 步骤八：测试获取已办列表
     */
    @Test
    void testGetDoneList() {
        // 创建、提交、审批
        Long id1 = createSubmitAndApprove("已办测试1", USER_ID_LISI, USER_ID_MANAGER);

        ApprovalQuery query = new ApprovalQuery();
        query.setCurrent(1);
        query.setSize(10);

        PageResult<ApprovalDetailResponse> result = approvalService.getDoneList(USER_ID_MANAGER, query);

        assertNotNull(result);
        assertTrue(result.getTotal() >= 1);
    }

    /**
     * 步骤九：测试获取审批历史
     */
    @Test
    void testGetHistory() {
        // 创建、提交、审批通过
        Long id = createSubmitAndApprove("历史记录测试", USER_ID_LISI, USER_ID_MANAGER);

        List<ApprovalHistoryResponse> history = approvalService.getHistory(id);

        assertNotNull(history);
        assertEquals(2, history.size()); // 提交 + 审批通过
    }

    /**
     * 测试获取我的申请列表
     */
    @Test
    void testGetMyApprovals() {
        createTestApproval("我的申请1", USER_ID_LISI);
        createTestApproval("我的申请2", USER_ID_LISI);

        ApprovalQuery query = new ApprovalQuery();
        query.setCurrent(1);
        query.setSize(10);

        PageResult<ApprovalDetailResponse> result = approvalService.getMyApprovals(USER_ID_LISI, query);

        assertNotNull(result);
        assertTrue(result.getTotal() >= 2);
    }

    /**
     * 测试更新工单
     */
    @Test
    void testUpdateApproval() {
        Long id = createTestApproval("更新前", USER_ID_LISI);

        ApprovalUpdateRequest request = new ApprovalUpdateRequest();
        request.setTitle("更新后");
        request.setContent("更新内容");
        request.setFormData(Map.of("updated", true));

        Boolean success = approvalService.update(id, request);
        assertTrue(success);

        Approval approval = approvalMapper.selectById(id);
        assertEquals("更新后", approval.getTitle());
        assertEquals("更新内容", approval.getContent());
    }

    /**
     * 测试非草稿状态不能更新
     */
    @Test
    void testUpdateNonDraftApproval() {
        Long id = createAndSubmitApproval("测试更新限制", USER_ID_LISI, USER_ID_MANAGER);

        ApprovalUpdateRequest request = new ApprovalUpdateRequest();
        request.setTitle("尝试更新");

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            approvalService.update(id, request);
        });

        assertEquals("只有草稿状态的工单可以编辑", exception.getMessage());
    }

    /**
     * 测试删除工单
     */
    @Test
    void testDeleteApproval() {
        Long id = createTestApproval("测试删除", USER_ID_LISI);

        Boolean success = approvalService.delete(id, USER_ID_LISI);
        assertTrue(success);

        Approval approval = approvalMapper.selectById(id);
        assertNull(approval);
    }

    /**
     * 测试非草稿状态不能删除
     */
    @Test
    void testDeleteNonDraftApproval() {
        Long id = createAndSubmitApproval("测试删除限制", USER_ID_LISI, USER_ID_MANAGER);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            approvalService.delete(id, USER_ID_LISI);
        });

        assertEquals("只有草稿状态的工单可以删除", exception.getMessage());
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
        request.setFormData(Map.of("test", true));
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

    /**
     * 创建、提交并审批通过
     */
    private Long createSubmitAndApprove(String title, Long applicantId, Long approverId) {
        Long id = createAndSubmitApproval(title, applicantId, approverId);

        ApprovalActionCmd cmd = new ApprovalActionCmd();
        cmd.setComment("同意");
        approvalService.approve(id, cmd, approverId);

        return id;
    }
}
