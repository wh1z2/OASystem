package com.oasystem.service;

import com.oasystem.dto.ApprovalCreateRequest;
import com.oasystem.dto.ApprovalDetailResponse;
import com.oasystem.dto.ApprovalQuery;
import com.oasystem.dto.DashboardStatisticsResponse;
import com.oasystem.dto.PageResult;
import com.oasystem.entity.Approval;
import com.oasystem.enums.ApprovalType;
import com.oasystem.mapper.ApprovalMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Admin 权限增强测试类
 * 测试 admin 可查看/审批全系统待办工单的数据查询维度
 */
@SpringBootTest
@Transactional
class AdminPermissionEnhancementTest {

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private ApprovalMapper approvalMapper;

    // 测试用户ID
    private static final Long USER_ID_ADMIN = 1L;       // 系统管理员
    private static final Long USER_ID_MANAGER = 2L;     // 张经理
    private static final Long USER_ID_LISI = 3L;        // 李四
    private static final Long USER_ID_ZHANGSAN = 4L;    // 张三

    // ==================== 待办列表查询测试 ====================

    /**
     * admin 查询待办列表，应返回全系统所有 PROCESSING 工单（含非指派给自己的）
     */
    @Test
    void testAdminCanSeeAllTodoApprovals() {
        // 创建并提交 3 个工单，指派给不同审批人
        createAndSubmitApproval("admin待办测试1", USER_ID_LISI, USER_ID_MANAGER);
        createAndSubmitApproval("admin待办测试2", USER_ID_ZHANGSAN, USER_ID_ADMIN);
        createAndSubmitApproval("admin待办测试3", USER_ID_LISI, USER_ID_ZHANGSAN);

        ApprovalQuery query = new ApprovalQuery();
        query.setCurrent(1);
        query.setSize(10);

        PageResult<ApprovalDetailResponse> result = approvalService.getTodoList(USER_ID_ADMIN, query);

        assertNotNull(result);
        // 由于真实数据库中可能已有 PROCESSING 工单，不断言总数
        // 只验证 admin 能看到非指派给自己的工单
        boolean hasManagerApproval = result.getRecords().stream()
                .anyMatch(r -> r.getTitle().equals("admin待办测试1"));
        boolean hasZhangsanApproval = result.getRecords().stream()
                .anyMatch(r -> r.getTitle().equals("admin待办测试3"));
        assertTrue(hasManagerApproval, "admin 应该看到指派给经理的待办");
        assertTrue(hasZhangsanApproval, "admin 应该看到指派给张三的待办");
    }

    /**
     * 经理查询待办列表，只能返回指派给自己的 PROCESSING 工单
     */
    @Test
    void testManagerCanOnlySeeAssignedTodoApprovals() {
        // 创建并提交 2 个工单：一个指派给经理，一个指派给 admin
        createAndSubmitApproval("经理待办测试1", USER_ID_LISI, USER_ID_MANAGER);
        createAndSubmitApproval("经理待办测试2", USER_ID_ZHANGSAN, USER_ID_ADMIN);

        ApprovalQuery query = new ApprovalQuery();
        query.setCurrent(1);
        query.setSize(10);

        PageResult<ApprovalDetailResponse> result = approvalService.getTodoList(USER_ID_MANAGER, query);

        assertNotNull(result);
        // 验证经理能看到指派给自己的工单
        boolean hasAssignedApproval = result.getRecords().stream()
                .anyMatch(r -> r.getTitle().equals("经理待办测试1"));
        assertTrue(hasAssignedApproval, "经理应该看到指派给自己的待办");

        // 验证经理看不到指派给 admin 的工单
        boolean hasAdminApproval = result.getRecords().stream()
                .anyMatch(r -> r.getTitle().equals("经理待办测试2"));
        assertFalse(hasAdminApproval, "经理不应该看到指派给 admin 的待办");
    }

    /**
     * 普通员工查询待办列表，只能返回指派给自己的 PROCESSING 工单
     */
    @Test
    void testEmployeeCanOnlySeeAssignedTodoApprovals() {
        // 创建并提交 2 个工单：一个指派给张三，一个指派给经理
        createAndSubmitApproval("员工待办测试1", USER_ID_LISI, USER_ID_ZHANGSAN);
        createAndSubmitApproval("员工待办测试2", USER_ID_LISI, USER_ID_MANAGER);

        ApprovalQuery query = new ApprovalQuery();
        query.setCurrent(1);
        query.setSize(10);

        PageResult<ApprovalDetailResponse> result = approvalService.getTodoList(USER_ID_ZHANGSAN, query);

        assertNotNull(result);
        assertEquals(1, result.getTotal(), "张三应该只看到 1 条指派给自己的待办");
        assertEquals("员工待办测试1", result.getRecords().get(0).getTitle());
    }

    /**
     * admin 查询待办列表，验证能看到真实数据库中已有的待办工单
     */
    @Test
    void testAdminCanSeeExistingTodoApprovals() {
        ApprovalQuery query = new ApprovalQuery();
        query.setCurrent(1);
        query.setSize(10);

        PageResult<ApprovalDetailResponse> result = approvalService.getTodoList(USER_ID_ADMIN, query);

        assertNotNull(result);
        // 真实数据库中已有 2 条 PROCESSING 工单，admin 应该能看到
        assertTrue(result.getTotal() >= 2, "admin 应该至少看到真实数据库中的 2 条待办");
    }

    // ==================== 工作台统计测试 ====================

    /**
     * admin 工作台 pendingCount 包含全系统 PROCESSING 工单（含非指派给自己的）
     */
    @Test
    void testAdminDashboardPendingCount() {
        // 先获取增强前的基准值（含真实数据库中的待办）
        DashboardStatisticsResponse baseline = approvalService.getDashboardStatistics(USER_ID_ADMIN);
        long baselineCount = baseline.getPendingCount();

        // 创建并提交 2 个工单：一个指派给经理，一个指派给张三
        createAndSubmitApproval("admin统计测试1", USER_ID_LISI, USER_ID_MANAGER);
        createAndSubmitApproval("admin统计测试2", USER_ID_ZHANGSAN, USER_ID_ZHANGSAN);

        DashboardStatisticsResponse statistics = approvalService.getDashboardStatistics(USER_ID_ADMIN);

        assertNotNull(statistics);
        // 验证 pendingCount 在基准值基础上增加了 2
        assertEquals(baselineCount + 2, statistics.getPendingCount(),
                "admin 的 pendingCount 应包含新增的全系统待办");
    }

    /**
     * 经理工作台 pendingCount 仅包含指派给自己的 PROCESSING 工单
     */
    @Test
    void testManagerDashboardPendingCount() {
        // 先获取基准值
        DashboardStatisticsResponse baseline = approvalService.getDashboardStatistics(USER_ID_MANAGER);
        long baselineCount = baseline.getPendingCount();

        // 创建并提交 2 个工单：一个指派给经理，一个指派给 admin
        createAndSubmitApproval("经理统计测试1", USER_ID_LISI, USER_ID_MANAGER);
        createAndSubmitApproval("经理统计测试2", USER_ID_ZHANGSAN, USER_ID_ADMIN);

        DashboardStatisticsResponse statistics = approvalService.getDashboardStatistics(USER_ID_MANAGER);

        assertNotNull(statistics);
        // 经理的 pendingCount 只应增加指派给自己的 1 条
        assertEquals(baselineCount + 1, statistics.getPendingCount(),
                "经理的 pendingCount 只应增加指派给自己的待办");
    }

    /**
     * 员工工作台 pendingCount 仅包含指派给自己的 PROCESSING 工单
     */
    @Test
    void testEmployeeDashboardPendingCount() {
        // 先获取基准值
        DashboardStatisticsResponse baseline = approvalService.getDashboardStatistics(USER_ID_ZHANGSAN);
        long baselineCount = baseline.getPendingCount();

        // 创建并提交 2 个工单：一个指派给张三，一个指派给经理
        createAndSubmitApproval("员工统计测试1", USER_ID_LISI, USER_ID_ZHANGSAN);
        createAndSubmitApproval("员工统计测试2", USER_ID_LISI, USER_ID_MANAGER);

        DashboardStatisticsResponse statistics = approvalService.getDashboardStatistics(USER_ID_ZHANGSAN);

        assertNotNull(statistics);
        // 张三的 pendingCount 只应增加指派给自己的 1 条
        assertEquals(baselineCount + 1, statistics.getPendingCount(),
                "张三的 pendingCount 只应增加指派给自己的待办");
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
}
