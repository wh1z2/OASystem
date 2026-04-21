package com.oasystem.service.impl;

import com.alibaba.cola.statemachine.StateMachine;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.oasystem.dto.*;
import com.oasystem.entity.Approval;
import com.oasystem.entity.ApprovalHistory;
import com.oasystem.entity.Department;
import com.oasystem.entity.User;
import com.oasystem.enums.ApprovalEvent;
import com.oasystem.enums.ApprovalStatus;
import com.oasystem.enums.ApprovalType;
import com.oasystem.enums.Priority;
import com.oasystem.exception.BusinessException;
import com.oasystem.mapper.ApprovalHistoryMapper;
import com.oasystem.mapper.ApprovalMapper;
import com.oasystem.mapper.DepartmentMapper;
import com.oasystem.mapper.UserMapper;
import com.oasystem.resolver.DefaultApproverResolver;
import com.oasystem.service.ApprovalService;
import com.oasystem.statemachine.ApprovalContext;
import com.oasystem.statemachine.ApprovalPermissionResult;
import com.oasystem.statemachine.ApprovalStateMachineHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 审批工单服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl implements ApprovalService {

    private final ApprovalMapper approvalMapper;
    private final ApprovalHistoryMapper approvalHistoryMapper;
    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;
    private final StateMachine<ApprovalStatus, ApprovalEvent, ApprovalContext> stateMachine;
    private final ApprovalStateMachineHelper stateMachineHelper;
    private final DefaultApproverResolver defaultApproverResolver;

    /**
     * 审批执行权限
     */
    private static final String PERMISSION_APPROVAL_EXECUTE = "approval:execute";

    /**
     * 检查用户是否拥有审批权限
     *
     * @param user 用户实体
     * @return true表示有权限
     */
    private boolean hasApprovalPermission(User user) {
        if (user == null || !StringUtils.hasText(user.getPermissions())) {
            return false;
        }
        try {
            List<String> permissions = JSON.parseObject(
                    user.getPermissions(),
                    new TypeReference<>() {}
            );
            return permissions != null &&
                    (permissions.contains("all") || permissions.contains(PERMISSION_APPROVAL_EXECUTE));
        } catch (Exception e) {
            log.error("解析用户权限失败：userId={}, permissions={}", user.getId(), user.getPermissions(), e);
            return false;
        }
    }

    /**
     * 校验指定的审批人是否有审批权限
     *
     * @param approverId 审批人ID
     * @throws BusinessException 审批人不存在或无权限时抛出
     */
    private void validateApproverPermission(Long approverId) {
        if (approverId == null) {
            return;
        }
        User approver = userMapper.selectByIdWithRole(approverId);
        if (approver == null) {
            throw new BusinessException("指定的审批人不存在");
        }
        if (!hasApprovalPermission(approver)) {
            throw new BusinessException("指定的审批人无审批权限，请选择管理员或部门经理作为审批人");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(ApprovalCreateRequest request, Long applicantId) {
        Approval approval = new Approval();
        approval.setTitle(request.getTitle());
        approval.setType(request.getType());
        approval.setApplicantId(applicantId);
        approval.setStatus(ApprovalStatus.DRAFT.getCode());
        approval.setPriority(request.getPriority());
        approval.setContent(request.getContent());
        // 将Map转为JSON字符串存储
        if (request.getFormData() != null) {
            approval.setFormData(JSON.toJSONString(request.getFormData()));
        }

        // 自动解析默认审批人：手动指定优先，未指定则尝试自动解析（创建草稿时允许失败，提交时再强制解析）
        if (request.getCurrentApproverId() != null) {
            validateApproverPermission(request.getCurrentApproverId());
            approval.setCurrentApproverId(request.getCurrentApproverId());
        } else {
            com.oasystem.dto.ResolverResult result = defaultApproverResolver.resolve(applicantId, request.getType());
            if (result.isSuccess()) {
                approval.setCurrentApproverId(result.getApproverId());
                log.info("自动分配审批人：工单标题={}, 规则={}, 审批人ID={}",
                        approval.getTitle(), result.getRuleName(), result.getApproverId());
            } else {
                log.warn("创建工单时自动解析审批人失败，草稿将无审批人：title={}, message={}",
                        approval.getTitle(), result.getMessage());
            }
        }

        approvalMapper.insert(approval);
        log.info("创建审批工单成功：id={}, title={}", approval.getId(), approval.getTitle());
        return approval.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean update(Long id, ApprovalUpdateRequest request) {
        Approval approval = approvalMapper.selectById(id);
        if (approval == null) {
            throw new BusinessException("审批工单不存在");
        }

        // 只有草稿状态的工单可以编辑
        if (!ApprovalStatus.DRAFT.getCode().equals(approval.getStatus())) {
            throw new BusinessException("只有草稿状态的工单可以编辑");
        }

        // 如果审批人为空（如撤销后），自动解析默认审批人，保证草稿详情页也能正常展示
        if (approval.getCurrentApproverId() == null) {
            com.oasystem.dto.ResolverResult result = defaultApproverResolver.resolve(
                    approval.getApplicantId(), approval.getType());
            if (result.isSuccess()) {
                approval.setCurrentApproverId(result.getApproverId());
                log.info("编辑保存时自动分配审批人：工单id={}, 审批人ID={}",
                        approval.getId(), result.getApproverId());
            }
        }

        // 如果前端指定了审批人，校验权限并覆盖
        if (request.getCurrentApproverId() != null) {
            validateApproverPermission(request.getCurrentApproverId());
            approval.setCurrentApproverId(request.getCurrentApproverId());
        }

        approval.setTitle(request.getTitle());
        approval.setPriority(request.getPriority());
        approval.setContent(request.getContent());
        // 将Map转为JSON字符串存储
        if (request.getFormData() != null) {
            approval.setFormData(JSON.toJSONString(request.getFormData()));
        }

        int rows = approvalMapper.updateById(approval);
        log.info("更新审批工单成功：id={}", id);
        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean delete(Long id, Long operatorId) {
        Approval approval = approvalMapper.selectById(id);
        if (approval == null) {
            throw new BusinessException("审批工单不存在");
        }

        // 只有草稿状态的工单可以删除
        if (!ApprovalStatus.DRAFT.getCode().equals(approval.getStatus())) {
            throw new BusinessException("只有草稿状态的工单可以删除");
        }

        // 只有申请人自己可以删除
        if (!approval.getApplicantId().equals(operatorId)) {
            throw new BusinessException("无权删除该工单");
        }

        int rows = approvalMapper.deleteById(id);
        log.info("删除审批工单成功：id={}", id);
        return rows > 0;
    }

    @Override
    public ApprovalDetailResponse getById(Long id, Long currentUserId) {
        Approval approval = approvalMapper.selectById(id);
        if (approval == null) {
            throw new BusinessException("审批工单不存在");
        }

        // R2修复：数据权限校验
        checkViewPermission(approval, currentUserId);

        return convertToDetailResponse(approval);
    }

    /**
     * 校验用户是否有权限查看指定工单详情
     *
     * @param approval      工单实体
     * @param currentUserId 当前用户ID
     * @throws BusinessException 无权限时抛出
     */
    private void checkViewPermission(Approval approval, Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException("用户未登录");
        }

        User currentUser = userMapper.selectByIdWithRole(currentUserId);
        if (currentUser == null) {
            throw new BusinessException("当前用户不存在");
        }

        // 管理员可以查看全部
        if ("admin".equals(currentUser.getRoleName())) {
            return;
        }

        // 部门经理可以查看本部门工单 + 需要自己审批的工单
        if ("manager".equals(currentUser.getRoleName())) {
            // 查询申请人部门信息
            User applicant = userMapper.selectById(approval.getApplicantId());
            if (applicant != null && applicant.getDeptId() != null
                    && applicant.getDeptId().equals(currentUser.getDeptId())) {
                return;
            }
            // 或当前审批人是自己
            if (approval.getCurrentApproverId() != null
                    && approval.getCurrentApproverId().equals(currentUserId)) {
                return;
            }
            throw new BusinessException("无权查看该工单");
        }

        // 普通员工只能查看自己的工单
        if (!approval.getApplicantId().equals(currentUserId)) {
            throw new BusinessException("无权查看该工单");
        }
    }

    @Override
    public PageResult<ApprovalDetailResponse> list(ApprovalQuery query, Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException("用户未登录");
        }

        User currentUser = userMapper.selectByIdWithRole(currentUserId);
        if (currentUser == null) {
            throw new BusinessException("当前用户不存在");
        }

        LambdaQueryWrapper<Approval> wrapper = Wrappers.lambdaQuery();

        if (StringUtils.hasText(query.getTitle())) {
            wrapper.like(Approval::getTitle, query.getTitle());
        }
        if (query.getType() != null) {
            wrapper.eq(Approval::getType, query.getType());
        }
        if (query.getStatus() != null) {
            wrapper.eq(Approval::getStatus, query.getStatus());
        }
        if (query.getApplicantId() != null) {
            wrapper.eq(Approval::getApplicantId, query.getApplicantId());
        }

        // R2修复：数据权限过滤
        if (!"admin".equals(currentUser.getRoleName())) {
            if ("manager".equals(currentUser.getRoleName())) {
                wrapper.and(w -> {
                    w.apply("applicant_id IN (SELECT id FROM sys_user WHERE dept_id = {0})", currentUser.getDeptId())
                     .or()
                     .eq(Approval::getCurrentApproverId, currentUserId);
                });
            } else {
                // 普通员工只能查看自己的工单
                wrapper.eq(Approval::getApplicantId, currentUserId);
            }
        }

        wrapper.orderByDesc(Approval::getCreateTime);

        List<Approval> approvals = approvalMapper.selectList(wrapper);
        long total = approvalMapper.selectCount(wrapper);

        List<ApprovalDetailResponse> responses = approvals.stream()
                .map(this::convertToDetailResponse)
                .collect(Collectors.toList());

        return PageResult.of(responses, total, query.getPageNum(), query.getPageSize());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean submit(Long id, Long operatorId) {
        Approval approval = approvalMapper.selectById(id);
        if (approval == null) {
            throw new BusinessException("审批工单不存在");
        }

        // 验证操作人是申请人
        if (!approval.getApplicantId().equals(operatorId)) {
            throw new BusinessException("只有申请人可以提交工单");
        }

        // 提交时若仍无审批人，再次自动解析
        if (approval.getCurrentApproverId() == null) {
            com.oasystem.dto.ResolverResult result = defaultApproverResolver.resolve(
                    approval.getApplicantId(), approval.getType());
            if (!result.isSuccess()) {
                throw new BusinessException("提交失败：" + result.getMessage());
            }
            approval.setCurrentApproverId(result.getApproverId());
        }

        ApprovalStatus currentStatus = ApprovalStatus.fromCode(approval.getStatus());
        ApprovalContext context = new ApprovalContext(approval, null, operatorId);

        ApprovalStatus newStatus = stateMachine.fireEvent(currentStatus, ApprovalEvent.SUBMIT, context);

        if (newStatus == currentStatus) {
            throw new BusinessException("当前状态不允许提交，请检查表单数据是否完整");
        }

        // 更新工单
        approvalMapper.updateById(approval);
        log.info("提交审批工单成功：id={}, status={} -> {}", id, currentStatus, newStatus);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean approve(Long id, ApprovalActionCmd cmd, Long operatorId) {
        Approval approval = approvalMapper.selectById(id);
        if (approval == null) {
            throw new BusinessException("审批工单不存在");
        }

        // 验证当前状态
        if (!ApprovalStatus.PROCESSING.getCode().equals(approval.getStatus())) {
            throw new BusinessException("只有审批中的工单可以执行审批操作");
        }

        // 获取当前用户完整信息（包含角色、部门）
        User currentUser = userMapper.selectByIdWithRole(operatorId);
        if (currentUser == null) {
            throw new BusinessException("当前用户不存在");
        }

        ApprovalStatus currentStatus = ApprovalStatus.fromCode(approval.getStatus());
        ApprovalContext context = new ApprovalContext(approval, cmd, operatorId);

        // 综合权限检查（整合方案A和方案B）
        ApprovalPermissionResult permissionResult =
                stateMachineHelper.checkApproverPermissionDetail(context);

        if (!permissionResult.isGranted()) {
            throw new BusinessException(permissionResult.getMessage());
        }

        // 将权限检查结果放入上下文，供后续状态机动作使用
        context.setPermissionResult(permissionResult);

        log.info("即将触发状态机：approvalId={}, currentStatus={}, event=APPROVE", id);
        ApprovalStatus newStatus = stateMachine.fireEvent(currentStatus, ApprovalEvent.APPROVE, context);
        log.info("状态机执行完成：approvalId={}, newStatus={}", id, newStatus);

        if (newStatus == currentStatus) {
            log.error("状态转换失败：approvalId={}, status unchanged", id);
            throw new BusinessException("状态转换失败");
        }

        // 更新工单
        int rows = approvalMapper.updateById(approval);
        log.info("工单更新完成：approvalId={}, rows={}", id, rows);

        // 记录代审批信息到日志（如为代审批）
        if (permissionResult.isProxyApproval()) {
            log.info("代审批完成：工单ID={}, 代审批人={}, 原审批人={}, 类型={}",
                    id, operatorId, permissionResult.getOriginalApproverId(),
                    permissionResult.getApprovalTypeLabel());
        }

        log.info("审批通过成功：id={}, operatorId={}, approvalType={}",
                id, operatorId, permissionResult.getApprovalTypeCode());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean reject(Long id, ApprovalActionCmd cmd, Long operatorId) {
        Approval approval = approvalMapper.selectById(id);
        if (approval == null) {
            throw new BusinessException("审批工单不存在");
        }

        // 验证当前状态
        if (!ApprovalStatus.PROCESSING.getCode().equals(approval.getStatus())) {
            throw new BusinessException("只有审批中的工单可以执行审批操作");
        }

        // 获取当前用户完整信息（包含角色、部门）
        User currentUser = userMapper.selectByIdWithRole(operatorId);
        if (currentUser == null) {
            throw new BusinessException("当前用户不存在");
        }

        ApprovalStatus currentStatus = ApprovalStatus.fromCode(approval.getStatus());
        ApprovalContext context = new ApprovalContext(approval, cmd, operatorId);

        // 综合权限检查（整合方案A和方案B）
        ApprovalPermissionResult permissionResult =
                stateMachineHelper.checkApproverPermissionDetail(context);

        if (!permissionResult.isGranted()) {
            throw new BusinessException(permissionResult.getMessage());
        }

        // 将权限检查结果放入上下文，供后续状态机动作使用
        context.setPermissionResult(permissionResult);

        log.info("即将触发状态机：approvalId={}, currentStatus={}, event=REJECT", id);
        ApprovalStatus newStatus = stateMachine.fireEvent(currentStatus, ApprovalEvent.REJECT, context);
        log.info("状态机执行完成：approvalId={}, newStatus={}", id, newStatus);

        if (newStatus == currentStatus) {
            log.error("状态转换失败：approvalId={}, status unchanged", id);
            throw new BusinessException("状态转换失败");
        }

        // 更新工单
        int rows = approvalMapper.updateById(approval);
        log.info("工单更新完成：approvalId={}, rows={}", id, rows);

        // 记录代审批信息到日志（如为代审批）
        if (permissionResult.isProxyApproval()) {
            log.info("代审批拒绝完成：工单ID={}, 代审批人={}, 原审批人={}, 类型={}",
                    id, operatorId, permissionResult.getOriginalApproverId(),
                    permissionResult.getApprovalTypeLabel());
        }

        log.info("审批拒绝成功：id={}, operatorId={}, approvalType={}",
                id, operatorId, permissionResult.getApprovalTypeCode());
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean reedit(Long id, Long operatorId, ApprovalUpdateRequest request) {
        Approval approval = approvalMapper.selectById(id);
        if (approval == null) {
            throw new BusinessException("审批工单不存在");
        }

        ApprovalStatus currentStatus = ApprovalStatus.fromCode(approval.getStatus());
        ApprovalContext context = new ApprovalContext(approval, null, operatorId);

        ApprovalStatus newStatus = stateMachine.fireEvent(currentStatus, ApprovalEvent.REEDIT, context);

        if (newStatus == currentStatus) {
            throw new BusinessException("当前状态不允许重新编辑");
        }

        // 如果携带了更新参数，同步更新内容（避免先 reedit 再 update 的两次操作）
        if (request != null) {
            if (request.getCurrentApproverId() != null) {
                validateApproverPermission(request.getCurrentApproverId());
                approval.setCurrentApproverId(request.getCurrentApproverId());
            }
            if (request.getTitle() != null) {
                approval.setTitle(request.getTitle());
            }
            if (request.getPriority() != null) {
                approval.setPriority(request.getPriority());
            }
            if (request.getContent() != null) {
                approval.setContent(request.getContent());
            }
            if (request.getFormData() != null) {
                approval.setFormData(JSON.toJSONString(request.getFormData()));
            }
        }

        // 更新工单
        approvalMapper.updateById(approval);
        log.info("重新编辑成功：id={}, operatorId={}, contentUpdated={}", id, operatorId, request != null);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean revoke(Long id, Long operatorId) {
        Approval approval = approvalMapper.selectById(id);
        if (approval == null) {
            throw new BusinessException("审批工单不存在");
        }

        // 验证当前状态
        if (!ApprovalStatus.PROCESSING.getCode().equals(approval.getStatus())) {
            throw new BusinessException("只有审批中的工单可以撤销");
        }

        ApprovalStatus currentStatus = ApprovalStatus.fromCode(approval.getStatus());
        ApprovalContext context = new ApprovalContext(approval, null, operatorId);

        ApprovalStatus newStatus = stateMachine.fireEvent(currentStatus, ApprovalEvent.REVOKE, context);

        if (newStatus == currentStatus) {
            throw new BusinessException("无权撤销该工单，您不是申请人");
        }

        // 更新工单
        approvalMapper.updateById(approval);
        log.info("撤销申请成功：id={}, operatorId={}", id, operatorId);
        return true;
    }

    @Override
    public PageResult<ApprovalDetailResponse> getTodoList(Long approverId, ApprovalQuery query) {
        // 参数校验：当前用户ID不能为空
        if (approverId == null) {
            log.warn("获取待办列表失败：当前用户ID为空");
            return PageResult.of(Collections.emptyList(), 0L, query.getPageNum(), query.getPageSize());
        }

        // 获取当前用户信息（含角色），用于判断是否为 admin
        User currentUser = userMapper.selectByIdWithRole(approverId);

        // 查询待办工单（审批中）
        LambdaQueryWrapper<Approval> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Approval::getStatus, ApprovalStatus.PROCESSING.getCode());

        // 非 admin 用户，只查询指派给自己的待办；admin 查询全系统待办
        if (currentUser == null || !"admin".equals(currentUser.getRoleName())) {
            wrapper.eq(Approval::getCurrentApproverId, approverId);
        }

        wrapper.orderByDesc(Approval::getCreateTime);

        List<Approval> approvals = approvalMapper.selectList(wrapper);
        long total = approvalMapper.selectCount(wrapper);

        // 批量查询用户信息和部门信息
        Map<Long, User> userMap;
        Map<Long, String> deptNameMap;
        {
            @SuppressWarnings("unchecked")
            Map<Long, ?>[] result = batchQueryUsersAndDepts(approvals);
            userMap = (Map<Long, User>) result[0];
            deptNameMap = (Map<Long, String>) result[1];
        }

        List<ApprovalDetailResponse> responses = new ArrayList<>();
        for (Approval approval : approvals) {
            responses.add(convertToDetailResponse(approval, userMap, deptNameMap));
        }

        return PageResult.of(responses, total, query.getPageNum(), query.getPageSize());
    }

    @Override
    public PageResult<ApprovalDetailResponse> getDoneList(Long approverId, ApprovalQuery query) {
        // 从审批历史表中查询该用户处理过的工单ID
        List<ApprovalHistory> histories = approvalHistoryMapper.selectByApproverId(approverId);

        if (histories.isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0L, query.getPageNum(), query.getPageSize());
        }

        Set<Long> approvalIds = histories.stream()
                .map(ApprovalHistory::getApprovalId)
                .collect(Collectors.toSet());

        // 查询这些工单的详情
        LambdaQueryWrapper<Approval> wrapper = Wrappers.lambdaQuery();
        wrapper.in(Approval::getId, approvalIds);
        wrapper.orderByDesc(Approval::getCreateTime);

        List<Approval> approvals = approvalMapper.selectList(wrapper);
        long total = approvalMapper.selectCount(wrapper);

        // 批量查询用户信息和部门信息
        Map<Long, User> userMap;
        Map<Long, String> deptNameMap;
        {
            @SuppressWarnings("unchecked")
            Map<Long, ?>[] result = batchQueryUsersAndDepts(approvals);
            userMap = (Map<Long, User>) result[0];
            deptNameMap = (Map<Long, String>) result[1];
        }

        List<ApprovalDetailResponse> responses = new ArrayList<>();
        for (Approval approval : approvals) {
            responses.add(convertToDetailResponse(approval, userMap, deptNameMap));
        }

        return PageResult.of(responses, total, query.getPageNum(), query.getPageSize());
    }

    @Override
    public List<ApprovalHistoryResponse> getHistory(Long approvalId) {
        List<ApprovalHistory> histories = approvalHistoryMapper.selectByApprovalId(approvalId);

        // 获取所有相关用户ID（包括审批人和原审批人）
        Set<Long> userIds = histories.stream()
                .map(ApprovalHistory::getApproverId)
                .collect(Collectors.toSet());

        // 收集原审批人ID（代审批情况）
        Set<Long> originalApproverIds = histories.stream()
                .filter(h -> h.getOriginalApproverId() != null)
                .map(ApprovalHistory::getOriginalApproverId)
                .collect(Collectors.toSet());
        userIds.addAll(originalApproverIds);

        // 批量查询用户信息
        Map<Long, String> userNameMap = Collections.emptyMap();
        if (!userIds.isEmpty()) {
            LambdaQueryWrapper<User> userWrapper = Wrappers.lambdaQuery();
            userWrapper.in(User::getId, userIds);
            List<User> users = userMapper.selectList(userWrapper);
            userNameMap = users.stream()
                    .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));
        }

        Map<Long, String> finalUserNameMap = userNameMap;
        return histories.stream()
                .map(h -> convertToHistoryResponse(h, finalUserNameMap))
                .collect(Collectors.toList());
    }

    @Override
    public PageResult<ApprovalDetailResponse> getMyApprovals(Long applicantId, ApprovalQuery query) {
        LambdaQueryWrapper<Approval> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Approval::getApplicantId, applicantId);

        if (query.getStatus() != null) {
            wrapper.eq(Approval::getStatus, query.getStatus());
        }
        if (query.getType() != null) {
            wrapper.eq(Approval::getType, query.getType());
        }

        wrapper.orderByDesc(Approval::getCreateTime);

        List<Approval> approvals = approvalMapper.selectList(wrapper);
        long total = approvalMapper.selectCount(wrapper);

        List<ApprovalDetailResponse> responses = approvals.stream()
                .map(this::convertToDetailResponse)
                .collect(Collectors.toList());

        return PageResult.of(responses, total, query.getPageNum(), query.getPageSize());
    }

    @Override
    public DashboardStatisticsResponse getDashboardStatistics(Long userId) {
        if (userId == null) {
            log.warn("获取工作台统计数据失败：当前用户ID为空");
            return buildEmptyStatisticsResponse();
        }

        DashboardStatisticsResponse response = new DashboardStatisticsResponse();

        // 获取当前用户信息（含角色），用于判断是否为 admin
        User currentUser = userMapper.selectByIdWithRole(userId);

        // 待办数量：admin 查看全系统待办，其他用户查看指派给自己的待办
        Long pendingCount;
        if (currentUser != null && "admin".equals(currentUser.getRoleName())) {
            pendingCount = approvalMapper.countAllTodos();
        } else {
            pendingCount = approvalMapper.countTodoByApproverId(userId);
        }
        response.setPendingCount(pendingCount != null ? pendingCount : 0L);

        // 已通过数量（当前用户作为审批人，审批通过的去重工单数）
        Long approvedCount = approvalHistoryMapper.countApprovedByApproverId(userId);
        response.setApprovedCount(approvedCount != null ? approvedCount : 0L);

        // 已拒绝数量（当前用户作为审批人，审批拒绝的去重工单数）
        Long rejectedCount = approvalHistoryMapper.countRejectedByApproverId(userId);
        response.setRejectedCount(rejectedCount != null ? rejectedCount : 0L);

        // 我的申请总数
        Long myApprovalCount = approvalMapper.countMyApprovalsByApplicantId(userId);
        response.setMyApprovalCount(myApprovalCount != null ? myApprovalCount : 0L);

        // 已办数量（当前用户在审批历史中处理过的去重工单数）
        Long doneCount = approvalHistoryMapper.countDoneByApproverId(userId);
        response.setDoneCount(doneCount != null ? doneCount : 0L);

        // 审批类型分布（按当前用户发起的申请统计）
        List<java.util.Map<String, Object>> typeDistribution = approvalMapper.countTypeDistributionByApplicantId(userId);
        List<DashboardStatisticsResponse.TypeDistributionItem> distributionItems = new ArrayList<>();
        if (typeDistribution != null) {
            for (java.util.Map<String, Object> item : typeDistribution) {
                DashboardStatisticsResponse.TypeDistributionItem distributionItem = new DashboardStatisticsResponse.TypeDistributionItem();
                Integer typeCode = item.get("type") instanceof Number ? ((Number) item.get("type")).intValue() : null;
                Long count = item.get("count") instanceof Number ? ((Number) item.get("count")).longValue() : 0L;
                distributionItem.setType(typeCode);
                distributionItem.setTypeName(typeCode != null ? ApprovalType.fromCode(typeCode).getLabel() : "");
                distributionItem.setCount(count);
                distributionItems.add(distributionItem);
            }
        }
        response.setApprovalTypeDistribution(distributionItems);

        log.info("获取工作台统计数据成功：userId={}, pending={}, approved={}, rejected={}, my={}, done={}",
                userId, response.getPendingCount(), response.getApprovedCount(),
                response.getRejectedCount(), response.getMyApprovalCount(), response.getDoneCount());
        return response;
    }

    /**
     * 构建空的统计数据响应
     */
    private DashboardStatisticsResponse buildEmptyStatisticsResponse() {
        DashboardStatisticsResponse response = new DashboardStatisticsResponse();
        response.setPendingCount(0L);
        response.setApprovedCount(0L);
        response.setRejectedCount(0L);
        response.setMyApprovalCount(0L);
        response.setDoneCount(0L);
        response.setApprovalTypeDistribution(Collections.emptyList());
        return response;
    }

    /**
     * 批量查询工单相关的用户信息和部门信息
     * 返回一个包含两个Map的数组：第一个是用户Map，第二个是部门Map
     */
    @SuppressWarnings("unchecked")
    private Map<Long, User>[] batchQueryUsersAndDepts(List<Approval> approvals) {
        if (approvals.isEmpty()) {
            return new Map[]{Collections.emptyMap(), Collections.emptyMap()};
        }

        // 收集所有用户ID（申请人 + 当前审批人）
        Set<Long> userIds = approvals.stream()
                .map(Approval::getApplicantId)
                .collect(Collectors.toSet());
        approvals.stream()
                .map(Approval::getCurrentApproverId)
                .filter(id -> id != null)
                .forEach(userIds::add);

        // 批量查询用户
        if (userIds.isEmpty()) {
            return new Map[]{Collections.emptyMap(), Collections.emptyMap()};
        }

        LambdaQueryWrapper<User> userWrapper = Wrappers.lambdaQuery();
        userWrapper.in(User::getId, userIds);
        List<User> users = userMapper.selectList(userWrapper);

        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        // 收集所有部门ID
        Set<Long> deptIds = users.stream()
                .map(User::getDeptId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        // 批量查询部门
        Map<Long, String> deptNameMap = Collections.emptyMap();
        if (!deptIds.isEmpty()) {
            LambdaQueryWrapper<Department> deptWrapper = Wrappers.lambdaQuery();
            deptWrapper.in(Department::getId, deptIds);
            List<Department> depts = departmentMapper.selectList(deptWrapper);
            deptNameMap = depts.stream()
                    .collect(Collectors.toMap(Department::getId, Department::getName, (a, b) -> a));
        }

        return new Map[]{userMap, deptNameMap};
    }

    /**
     * 转换为详情响应DTO（带用户信息和部门信息缓存）
     */
    private ApprovalDetailResponse convertToDetailResponse(Approval approval, Map<Long, User> userMap, Map<Long, String> deptNameMap) {
        ApprovalDetailResponse response = new ApprovalDetailResponse();
        response.setId(approval.getId());
        response.setTitle(approval.getTitle());
        response.setType(approval.getType());
        response.setApplicantId(approval.getApplicantId());
        response.setCurrentApproverId(approval.getCurrentApproverId());
        response.setStatus(approval.getStatus());
        response.setPriority(approval.getPriority());
        response.setContent(approval.getContent());
        response.setFormData(approval.getFormData());
        response.setCreateTime(approval.getCreateTime());
        response.setUpdateTime(approval.getUpdateTime());

        // 设置枚举名称
        ApprovalType type = ApprovalType.fromCode(approval.getType());
        response.setTypeName(type != null ? type.getLabel() : "");

        ApprovalStatus status = ApprovalStatus.fromCode(approval.getStatus());
        response.setStatusName(status != null ? status.getLabel() : "");

        Priority priority = Priority.fromCode(approval.getPriority());
        response.setPriorityName(priority != null ? priority.getLabel() : "");

        // 设置用户名称和部门名称
        User applicant = userMap.get(approval.getApplicantId());
        if (applicant != null) {
            response.setApplicantName(applicant.getName());
            response.setDeptName(deptNameMap.getOrDefault(applicant.getDeptId(), ""));
        }

        User currentApprover = userMap.get(approval.getCurrentApproverId());
        if (currentApprover != null) {
            response.setCurrentApproverName(currentApprover.getName());
        }

        return response;
    }

    /**
     * 转换为详情响应DTO（单个查询，用于getById等场景）
     */
    @SuppressWarnings("unchecked")
    private ApprovalDetailResponse convertToDetailResponse(Approval approval) {
        Map<Long, ?>[] result = batchQueryUsersAndDepts(Collections.singletonList(approval));
        Map<Long, User> userMap = (Map<Long, User>) result[0];
        Map<Long, String> deptNameMap = (Map<Long, String>) result[1];
        return convertToDetailResponse(approval, userMap, deptNameMap);
    }

    /**
     * 转换为历史记录响应DTO
     */
    private ApprovalHistoryResponse convertToHistoryResponse(ApprovalHistory history, Map<Long, String> userNameMap) {
        ApprovalHistoryResponse response = new ApprovalHistoryResponse();
        response.setId(history.getId());
        response.setApprovalId(history.getApprovalId());
        response.setApproverId(history.getApproverId());
        response.setApproverName(userNameMap.getOrDefault(history.getApproverId(), ""));
        response.setAction(history.getAction());
        response.setComment(history.getComment());
        response.setCreateTime(history.getCreateTime());

        // 设置操作类型名称
        ApprovalEvent event = ApprovalEvent.fromCode(history.getAction());
        response.setActionName(event != null ? event.getLabel() : "");

        // 设置代审批相关信息（权限系统优化新增）
        response.setIsProxy(history.getIsProxy());
        response.setApprovalType(history.getApprovalType());
        response.setOriginalApproverId(history.getOriginalApproverId());
        response.setOriginalApproverName(userNameMap.getOrDefault(history.getOriginalApproverId(), ""));
        response.setProxyReason(history.getProxyReason());

        return response;
    }
}
