package com.oasystem.service.impl;

import com.alibaba.cola.statemachine.StateMachine;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.oasystem.dto.*;
import com.oasystem.entity.Approval;
import com.oasystem.entity.ApprovalHistory;
import com.oasystem.entity.User;
import com.oasystem.enums.ApprovalEvent;
import com.oasystem.enums.ApprovalStatus;
import com.oasystem.enums.ApprovalType;
import com.oasystem.enums.Priority;
import com.oasystem.exception.BusinessException;
import com.oasystem.mapper.ApprovalHistoryMapper;
import com.oasystem.mapper.ApprovalMapper;
import com.oasystem.mapper.UserMapper;
import com.oasystem.service.ApprovalService;
import com.oasystem.statemachine.ApprovalContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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
    private final StateMachine<ApprovalStatus, ApprovalEvent, ApprovalContext> stateMachine;

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
        approval.setFormData(request.getFormData());
        approval.setCurrentApproverId(request.getCurrentApproverId());

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

        approval.setTitle(request.getTitle());
        approval.setPriority(request.getPriority());
        approval.setContent(request.getContent());
        approval.setFormData(request.getFormData());
        if (request.getCurrentApproverId() != null) {
            approval.setCurrentApproverId(request.getCurrentApproverId());
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
    public ApprovalDetailResponse getById(Long id) {
        Approval approval = approvalMapper.selectById(id);
        if (approval == null) {
            throw new BusinessException("审批工单不存在");
        }
        return convertToDetailResponse(approval);
    }

    @Override
    public PageResult<ApprovalDetailResponse> list(ApprovalQuery query) {
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

        ApprovalStatus currentStatus = ApprovalStatus.fromCode(approval.getStatus());
        ApprovalContext context = new ApprovalContext(approval, cmd, operatorId);

        ApprovalStatus newStatus = stateMachine.fireEvent(currentStatus, ApprovalEvent.APPROVE, context);

        if (newStatus == currentStatus) {
            throw new BusinessException("无权执行审批操作，您不是当前审批人");
        }

        // 更新工单
        approvalMapper.updateById(approval);
        log.info("审批通过成功：id={}, operatorId={}", id, operatorId);
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

        ApprovalStatus currentStatus = ApprovalStatus.fromCode(approval.getStatus());
        ApprovalContext context = new ApprovalContext(approval, cmd, operatorId);

        ApprovalStatus newStatus = stateMachine.fireEvent(currentStatus, ApprovalEvent.REJECT, context);

        if (newStatus == currentStatus) {
            throw new BusinessException("无权执行审批操作，您不是当前审批人");
        }

        // 更新工单
        approvalMapper.updateById(approval);
        log.info("审批拒绝成功：id={}, operatorId={}", id, operatorId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean reedit(Long id, Long operatorId) {
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

        // 更新工单
        approvalMapper.updateById(approval);
        log.info("重新编辑成功：id={}, operatorId={}", id, operatorId);
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
        // 查询待办工单（审批中且当前审批人是该用户）
        LambdaQueryWrapper<Approval> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(Approval::getCurrentApproverId, approverId);
        wrapper.eq(Approval::getStatus, ApprovalStatus.PROCESSING.getCode());
        wrapper.orderByDesc(Approval::getCreateTime);

        List<Approval> approvals = approvalMapper.selectList(wrapper);
        long total = approvalMapper.selectCount(wrapper);

        List<ApprovalDetailResponse> responses = approvals.stream()
                .map(this::convertToDetailResponse)
                .collect(Collectors.toList());

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

        List<ApprovalDetailResponse> responses = approvals.stream()
                .map(this::convertToDetailResponse)
                .collect(Collectors.toList());

        return PageResult.of(responses, total, query.getPageNum(), query.getPageSize());
    }

    @Override
    public List<ApprovalHistoryResponse> getHistory(Long approvalId) {
        List<ApprovalHistory> histories = approvalHistoryMapper.selectByApprovalId(approvalId);

        // 获取所有相关用户ID
        Set<Long> userIds = histories.stream()
                .map(ApprovalHistory::getApproverId)
                .collect(Collectors.toSet());

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

    /**
     * 转换为详情响应DTO
     */
    private ApprovalDetailResponse convertToDetailResponse(Approval approval) {
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

        return response;
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

        return response;
    }
}
