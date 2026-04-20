package com.oasystem.controller;

import com.oasystem.dto.*;
import com.oasystem.security.UserDetailsImpl;
import com.oasystem.service.ApprovalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 审批工单控制器
 */
@RestController
@RequestMapping("/approvals")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    /**
     * 创建审批工单
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('apply', 'all')")
    public Result<Long> create(@RequestBody @Valid ApprovalCreateRequest request) {
        Long applicantId = getCurrentUserId();
        Long id = approvalService.create(request, applicantId);
        return Result.success(id);
    }

    /**
     * 更新审批工单
     */
    @PostMapping("/{id}/update")
    @PreAuthorize("hasAnyAuthority('apply', 'all')")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody @Valid ApprovalUpdateRequest request) {
        Boolean success = approvalService.update(id, request);
        return Result.success(success);
    }

    /**
     * 删除审批工单
     */
    @PostMapping("/{id}/delete")
    @PreAuthorize("hasAnyAuthority('apply', 'all')")
    public Result<Boolean> delete(@PathVariable Long id) {
        Long operatorId = getCurrentUserId();
        Boolean success = approvalService.delete(id, operatorId);
        return Result.success(success);
    }

    /**
     * 获取工单详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public Result<ApprovalDetailResponse> getById(@PathVariable Long id) {
        Long currentUserId = getCurrentUserId();
        ApprovalDetailResponse response = approvalService.getById(id, currentUserId);
        return Result.success(response);
    }

    /**
     * 分页查询工单列表
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<ApprovalDetailResponse>> list(ApprovalQuery query) {
        Long currentUserId = getCurrentUserId();
        PageResult<ApprovalDetailResponse> result = approvalService.list(query, currentUserId);
        return Result.success(result);
    }

    /**
     * 提交申请
     */
    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAnyAuthority('apply', 'all')")
    public Result<Boolean> submit(@PathVariable Long id) {
        Long operatorId = getCurrentUserId();
        Boolean success = approvalService.submit(id, operatorId);
        return Result.success(success);
    }

    /**
     * 审批通过
     */
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyAuthority('approval', 'all')")
    public Result<Boolean> approve(@PathVariable Long id, @RequestBody ApprovalActionCmd cmd) {
        Long operatorId = getCurrentUserId();
        Boolean success = approvalService.approve(id, cmd, operatorId);
        return Result.success(success);
    }

    /**
     * 审批拒绝
     */
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyAuthority('approval', 'all')")
    public Result<Boolean> reject(@PathVariable Long id, @RequestBody ApprovalActionCmd cmd) {
        Long operatorId = getCurrentUserId();
        Boolean success = approvalService.reject(id, cmd, operatorId);
        return Result.success(success);
    }

    /**
     * 重新编辑（可选携带内容参数，实现一步到位更新）
     */
    @PostMapping("/{id}/reedit")
    @PreAuthorize("hasAnyAuthority('apply', 'all')")
    public Result<Boolean> reedit(@PathVariable Long id, @RequestBody(required = false) ApprovalUpdateRequest request) {
        Long operatorId = getCurrentUserId();
        Boolean success = approvalService.reedit(id, operatorId, request);
        return Result.success(success);
    }

    /**
     * 撤销申请
     */
    @PostMapping("/{id}/revoke")
    @PreAuthorize("hasAnyAuthority('apply', 'all')")
    public Result<Boolean> revoke(@PathVariable Long id) {
        Long operatorId = getCurrentUserId();
        Boolean success = approvalService.revoke(id, operatorId);
        return Result.success(success);
    }

    /**
     * 获取待办列表
     */
    @GetMapping("/todo")
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<ApprovalDetailResponse>> getTodoList(ApprovalQuery query) {
        Long approverId = getCurrentUserId();
        PageResult<ApprovalDetailResponse> result = approvalService.getTodoList(approverId, query);
        return Result.success(result);
    }

    /**
     * 获取已办列表
     */
    @GetMapping("/done")
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<ApprovalDetailResponse>> getDoneList(ApprovalQuery query) {
        Long approverId = getCurrentUserId();
        PageResult<ApprovalDetailResponse> result = approvalService.getDoneList(approverId, query);
        return Result.success(result);
    }

    /**
     * 获取我的申请列表
     */
    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public Result<PageResult<ApprovalDetailResponse>> getMyApprovals(ApprovalQuery query) {
        Long applicantId = getCurrentUserId();
        PageResult<ApprovalDetailResponse> result = approvalService.getMyApprovals(applicantId, query);
        return Result.success(result);
    }

    /**
     * 获取审批历史
     */
    @GetMapping("/{id}/history")
    @PreAuthorize("isAuthenticated()")
    public Result<List<ApprovalHistoryResponse>> getHistory(@PathVariable Long id) {
        List<ApprovalHistoryResponse> result = approvalService.getHistory(id);
        return Result.success(result);
    }

    /**
     * 获取工作台统计数据
     */
    @GetMapping("/statistics")
    @PreAuthorize("isAuthenticated()")
    public Result<DashboardStatisticsResponse> getDashboardStatistics() {
        Long userId = getCurrentUserId();
        if (userId == null) {
            return Result.unauthorized("用户未登录");
        }
        DashboardStatisticsResponse statistics = approvalService.getDashboardStatistics(userId);
        return Result.success(statistics);
    }

    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetailsImpl) {
            return ((UserDetailsImpl) principal).getId();
        }
        return null;
    }
}
