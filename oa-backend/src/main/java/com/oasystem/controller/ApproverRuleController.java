package com.oasystem.controller;

import com.oasystem.dto.*;
import com.oasystem.security.UserDetailsImpl;
import com.oasystem.service.ApproverRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * 审批规则管理控制器
 */
@RestController
@RequestMapping("/approver-rules")
@RequiredArgsConstructor
public class ApproverRuleController {

    private final ApproverRuleService approverRuleService;

    /**
     * 创建规则
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('role_manage', 'all')")
    public Result<Long> create(@RequestBody @Valid ApproverRuleCreateRequest request) {
        Long id = approverRuleService.create(request);
        return Result.success(id);
    }

    /**
     * 更新规则
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('role_manage', 'all')")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody @Valid ApproverRuleUpdateRequest request) {
        Boolean success = approverRuleService.update(id, request);
        return Result.success(success);
    }

    /**
     * 删除规则
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('role_manage', 'all')")
    public Result<Boolean> delete(@PathVariable Long id) {
        Boolean success = approverRuleService.delete(id);
        return Result.success(success);
    }

    /**
     * 获取规则详情
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('role_manage', 'all')")
    public Result<ApproverRuleResponse> getById(@PathVariable Long id) {
        ApproverRuleResponse response = approverRuleService.getById(id);
        return Result.success(response);
    }

    /**
     * 分页查询规则列表
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('role_manage', 'all')")
    public Result<PageResult<ApproverRuleResponse>> list(ApproverRuleQuery query) {
        PageResult<ApproverRuleResponse> result = approverRuleService.list(query);
        return Result.success(result);
    }

    /**
     * 规则效果预览
     */
    @PostMapping("/preview")
    @PreAuthorize("isAuthenticated()")
    public Result<ResolverResult> preview(@RequestBody ResolverPreviewRequest request) {
        ResolverResult result = approverRuleService.preview(request.getApplicantId(), request.getType());
        return Result.success(result);
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
