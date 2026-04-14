package com.oasystem.service;

import com.oasystem.dto.*;

import java.util.List;

/**
 * 审批工单服务接口
 */
public interface ApprovalService {

    /**
     * 创建审批工单
     * @param request 创建请求
     * @param applicantId 申请人ID
     * @return 工单ID
     */
    Long create(ApprovalCreateRequest request, Long applicantId);

    /**
     * 更新审批工单
     * @param id 工单ID
     * @param request 更新请求
     * @return 是否成功
     */
    Boolean update(Long id, ApprovalUpdateRequest request);

    /**
     * 删除审批工单（仅限草稿状态）
     * @param id 工单ID
     * @param operatorId 操作人ID
     * @return 是否成功
     */
    Boolean delete(Long id, Long operatorId);

    /**
     * 根据ID查询工单详情
     * @param id 工单ID
     * @return 详情响应
     */
    ApprovalDetailResponse getById(Long id);

    /**
     * 分页查询工单列表
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<ApprovalDetailResponse> list(ApprovalQuery query);

    /**
     * 提交申请
     * @param id 工单ID
     * @param operatorId 操作人ID
     * @return 是否成功
     */
    Boolean submit(Long id, Long operatorId);

    /**
     * 审批通过
     * @param id 工单ID
     * @param cmd 审批操作命令
     * @param operatorId 审批人ID
     * @return 是否成功
     */
    Boolean approve(Long id, ApprovalActionCmd cmd, Long operatorId);

    /**
     * 审批拒绝
     * @param id 工单ID
     * @param cmd 审批操作命令
     * @param operatorId 审批人ID
     * @return 是否成功
     */
    Boolean reject(Long id, ApprovalActionCmd cmd, Long operatorId);

    /**
     * 重新编辑（已通过或已打回状态的工单）
     * @param id 工单ID
     * @param operatorId 操作人ID
     * @return 是否成功
     */
    Boolean reedit(Long id, Long operatorId);

    /**
     * 撤销申请（审批中状态的工单）
     * @param id 工单ID
     * @param operatorId 操作人ID
     * @return 是否成功
     */
    Boolean revoke(Long id, Long operatorId);

    /**
     * 获取待办列表
     * @param approverId 审批人ID
     * @param query 分页参数
     * @return 分页结果
     */
    PageResult<ApprovalDetailResponse> getTodoList(Long approverId, ApprovalQuery query);

    /**
     * 获取已办列表
     * @param approverId 审批人ID
     * @param query 分页参数
     * @return 分页结果
     */
    PageResult<ApprovalDetailResponse> getDoneList(Long approverId, ApprovalQuery query);

    /**
     * 获取审批历史记录
     * @param approvalId 工单ID
     * @return 历史记录列表
     */
    List<ApprovalHistoryResponse> getHistory(Long approvalId);

    /**
     * 获取我发起的工单列表
     * @param applicantId 申请人ID
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<ApprovalDetailResponse> getMyApprovals(Long applicantId, ApprovalQuery query);

    /**
     * 获取工作台统计数据
     * @param userId 当前用户ID
     * @return 统计数据
     */
    DashboardStatisticsResponse getDashboardStatistics(Long userId);
}
