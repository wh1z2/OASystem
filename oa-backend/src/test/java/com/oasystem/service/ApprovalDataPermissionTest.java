package com.oasystem.service;

import com.oasystem.dto.ApprovalCreateRequest;
import com.oasystem.dto.ApprovalDetailResponse;
import com.oasystem.dto.ApprovalQuery;
import com.oasystem.dto.PageResult;
import com.oasystem.entity.Approval;
import com.oasystem.enums.ApprovalType;
import com.oasystem.exception.BusinessException;
import com.oasystem.mapper.ApprovalMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * R2修复：审批数据权限测试类
 * 测试 GET /approvals/{id} 和 GET /approvals 的数据权限过滤
 */
@SpringBootTest
@Transactional
class ApprovalDataPermissionTest {

    @Autowired
    private ApprovalService approvalService;

    @Autowired
    private ApprovalMapper approvalMapper;

    // 测试用户ID（来自init.sql中的测试数据）
    private static final Long USER_ID_ADMIN = 1L;      // 系统管理员，技术部(dept_id=1)
    private static final Long USER_ID_MANAGER = 2L;    // 张经理，技术部(dept_id=1)
    private static final Long USER_ID_LISI = 3L;       // 李四，财务部(dept_id=2)
    private static final Long USER_ID_ZHANGSAN = 4L;   // 张三，财务部(dept_id=2)
    private static final Long USER_ID_WANGWU = 5L;     // 王五，人事部(dept_id=3)

    // ==================== 工单详情数据权限测试 ====================

    /**
     * 管理员可以查看任意工单详情
     */
    @Test
    void testAdminCanViewAnyApproval() {
        Long id = createTestApproval("管理员查看测试", USER_ID_LISI);

        ApprovalDetailResponse response = approvalService.getById(id, USER_ID_ADMIN);

        assertNotNull(response);
        assertEquals(id, response.getId());
        assertEquals("管理员查看测试", response.getTitle());
    }

    /**
     * 部门经理可以查看本部门员工的工单详情
     */
    @Test
    void testManagerCanViewDeptApproval() {
        // admin(技术部) 创建的工单，经理(技术部) 应该可以查看
        Long id = createTestApproval("本部门工单", USER_ID_ADMIN);

        ApprovalDetailResponse response = approvalService.getById(id, USER_ID_MANAGER);

        assertNotNull(response);
        assertEquals(id, response.getId());
    }

    /**
     * 部门经理可以查看指定自己为审批人的非本部门工单
     */
    @Test
    void testManagerCanViewApprovalWhereTheyAreApprover() {
        // 李四(财务部) 创建的工单，指定经理为审批人
        Long id = createTestApproval("跨部门审批工单", USER_ID_LISI);
        Approval approval = approvalMapper.selectById(id);
        approval.setCurrentApproverId(USER_ID_MANAGER);
        approvalMapper.updateById(approval);

        ApprovalDetailResponse response = approvalService.getById(id, USER_ID_MANAGER);

        assertNotNull(response);
        assertEquals(id, response.getId());
    }

    /**
     * 部门经理不能查看非本部门且非自己审批的工单详情
     */
    @Test
    void testManagerCannotViewOtherDeptApproval() {
        // 王五(人事部) 创建的工单，不指定经理为审批人
        Long id = createTestApprovalWithApprover("其他部门工单", USER_ID_WANGWU, null);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            approvalService.getById(id, USER_ID_MANAGER);
        });

        assertEquals("无权查看该工单", exception.getMessage());
    }

    /**
     * 普通员工可以查看自己的工单详情
     */
    @Test
    void testEmployeeCanViewOwnApproval() {
        Long id = createTestApproval("自己的工单", USER_ID_LISI);

        ApprovalDetailResponse response = approvalService.getById(id, USER_ID_LISI);

        assertNotNull(response);
        assertEquals(id, response.getId());
    }

    /**
     * 普通员工不能查看他人的工单详情
     */
    @Test
    void testEmployeeCannotViewOthersApproval() {
        Long id = createTestApproval("他人的工单", USER_ID_ZHANGSAN);

        BusinessException exception = assertThrows(BusinessException.class, () -> {
            approvalService.getById(id, USER_ID_LISI);
        });

        assertEquals("无权查看该工单", exception.getMessage());
    }

    // ==================== 工单列表数据权限测试 ====================

    /**
     * 管理员查询列表可以返回全部工单
     */
    @Test
    void testAdminCanListAllApprovals() {
        createTestApproval("admin列表测试1", USER_ID_LISI);
        createTestApproval("admin列表测试2", USER_ID_ZHANGSAN);
        createTestApproval("admin列表测试3", USER_ID_WANGWU);

        ApprovalQuery query = new ApprovalQuery();
        query.setCurrent(1);
        query.setSize(100);

        PageResult<ApprovalDetailResponse> result = approvalService.list(query, USER_ID_ADMIN);

        assertNotNull(result);
        assertTrue(result.getTotal() >= 3, "管理员应该能看到全部工单");
    }

    /**
     * 部门经理查询列表返回本部门工单 + 指定自己审批的工单
     */
    @Test
    void testManagerCanListDeptAndAssignedApprovals() {
        // 本部门工单：admin(技术部) 创建
        createTestApproval("经理本部门工单", USER_ID_ADMIN);
        // 跨部门但指定经理审批：李四(财务部) 创建，指定经理审批
        Long crossDeptId = createTestApprovalWithApprover("经理跨部门工单", USER_ID_LISI, USER_ID_MANAGER);
        // 其他部门非经理审批：王五(人事部) 创建，不指定经理
        createTestApprovalWithApprover("其他部门工单", USER_ID_WANGWU, null);

        ApprovalQuery query = new ApprovalQuery();
        query.setCurrent(1);
        query.setSize(100);

        PageResult<ApprovalDetailResponse> result = approvalService.list(query, USER_ID_MANAGER);

        assertNotNull(result);
        // 验证结果中不包含"其他部门工单"（王五创建，无经理审批）
        boolean hasOtherDept = result.getRecords().stream()
                .anyMatch(r -> "其他部门工单".equals(r.getTitle()));
        assertFalse(hasOtherDept, "经理不应看到非本部门且非自己审批的工单");

        // 验证结果中包含经理有权限看到的工单
        boolean hasDeptApproval = result.getRecords().stream()
                .anyMatch(r -> "经理本部门工单".equals(r.getTitle()));
        boolean hasAssignedApproval = result.getRecords().stream()
                .anyMatch(r -> "经理跨部门工单".equals(r.getTitle()));
        assertTrue(hasDeptApproval, "经理应该看到本部门工单");
        assertTrue(hasAssignedApproval, "经理应该看到指定自己审批的工单");
    }

    /**
     * 普通员工查询列表只能返回自己的工单
     */
    @Test
    void testEmployeeCanListOnlyOwnApprovals() {
        createTestApproval("李四的工单1", USER_ID_LISI);
        createTestApproval("李四的工单2", USER_ID_LISI);
        createTestApproval("张三的工单", USER_ID_ZHANGSAN);

        ApprovalQuery query = new ApprovalQuery();
        query.setCurrent(1);
        query.setSize(100);

        PageResult<ApprovalDetailResponse> result = approvalService.list(query, USER_ID_LISI);

        assertNotNull(result);
        assertTrue(result.getTotal() >= 2, "李四至少能看到自己的2个新创建工单");
        assertTrue(result.getRecords().stream()
                        .allMatch(r -> r.getApplicantId().equals(USER_ID_LISI)),
                "列表中所有工单申请人必须是李四");

        // 验证结果中不包含张三的工单
        boolean hasZhangsanApproval = result.getRecords().stream()
                .anyMatch(r -> "张三的工单".equals(r.getTitle()));
        assertFalse(hasZhangsanApproval, "李四不应看到张三的工单");
    }

    /**
     * 普通员工查询列表时，即使有按申请人筛选条件也不会看到他人工单
     */
    @Test
    void testEmployeeListWithApplicantFilter() {
        createTestApproval("李四的筛选工单", USER_ID_LISI);

        ApprovalQuery query = new ApprovalQuery();
        query.setApplicantId(USER_ID_ZHANGSAN); // 尝试筛选张三的工单
        query.setCurrent(1);
        query.setSize(10);

        PageResult<ApprovalDetailResponse> result = approvalService.list(query, USER_ID_LISI);

        assertNotNull(result);
        assertEquals(0, result.getTotal(), "员工无法通过筛选条件查看他人工单");
    }

    // ==================== 辅助方法 ====================

    /**
     * 创建测试工单（默认指定经理为审批人）
     */
    private Long createTestApproval(String title, Long applicantId) {
        return createTestApprovalWithApprover(title, applicantId, USER_ID_MANAGER);
    }

    /**
     * 创建测试工单（可自定义审批人）
     */
    private Long createTestApprovalWithApprover(String title, Long applicantId, Long approverId) {
        ApprovalCreateRequest request = new ApprovalCreateRequest();
        request.setTitle(title);
        request.setType(ApprovalType.LEAVE.getCode());
        request.setContent("测试内容");
        request.setFormData(Map.of("test", true));
        request.setCurrentApproverId(approverId);
        return approvalService.create(request, applicantId);
    }
}
