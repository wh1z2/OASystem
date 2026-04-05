package com.oasystem.statemachine;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.oasystem.entity.Approval;
import com.oasystem.entity.ApprovalHistory;
import com.oasystem.entity.User;
import com.oasystem.enums.ApprovalActionType;
import com.oasystem.enums.ApprovalEvent;
import com.oasystem.enums.ApprovalStatus;
import com.oasystem.mapper.ApprovalHistoryMapper;
import com.oasystem.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批状态机辅助类
 * 定义状态转换的条件（Condition）和动作（Action）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalStateMachineHelper {

    private final ApprovalHistoryMapper approvalHistoryMapper;
    private final UserMapper userMapper;

    // ==================== 权限检查相关常量 ====================

    /**
     * 系统管理员角色标识（小写，与数据库保持一致）
     */
    private static final String ROLE_ADMIN = "admin";

    /**
     * 部门经理角色标识（小写，与数据库保持一致）
     */
    private static final String ROLE_MANAGER = "manager";

    /**
     * 审批执行权限
     */
    private static final String PERMISSION_APPROVAL_EXECUTE = "approval:execute";

    /**
     * 全范围审批权限
     */
    private static final String PERMISSION_APPROVAL_EXECUTE_ALL = "approval:execute:all";

    /**
     * 本部门审批权限
     */
    private static final String PERMISSION_APPROVAL_EXECUTE_DEPT = "approval:execute:dept";

    // ==================== 条件检查方法 ====================

    /**
     * 检查条件：检查表单数据完整性
     * 提交申请时调用
     */
    public boolean checkFormComplete(ApprovalContext context) {
        Approval approval = context.getApproval();
        // 检查表单数据是否为空
        boolean valid = approval.getFormData() != null && !approval.getFormData().isEmpty();
        if (!valid) {
            log.warn("工单 {} 表单数据不完整，无法提交", approval.getId());
        }
        return valid;
    }

    /**
     * 检查条件：检查当前用户是否有审批权限
     * 审批通过/拒绝时调用
     * 注意：此方法用于状态机的条件判断，如需获取详细权限信息请使用 checkApproverPermissionDetail
     */
    public boolean checkApproverPermission(ApprovalContext context) {
        ApprovalPermissionResult result = checkApproverPermissionDetail(context);
        return result.isGranted();
    }

    /**
     * 综合权限检查（整合方案A和方案B）
     *
     * @param context 状态机上下文
     * @return ApprovalPermissionResult 权限检查结果
     */
    public ApprovalPermissionResult checkApproverPermissionDetail(ApprovalContext context) {
        Approval approval = context.getApproval();
        Long currentUserId = context.getOperatorId();
        Long approverId = approval.getCurrentApproverId();

        // 获取当前用户完整信息（包含角色、部门）
        User currentUser = userMapper.selectByIdWithRole(currentUserId);
        if (currentUser == null) {
            log.warn("用户 {} 不存在", currentUserId);
            return ApprovalPermissionResult.denied("用户不存在");
        }

        String roleName = currentUser.getRoleName();

        // ========== Level 1: 指定审批人直接审批 ==========
        if (currentUserId != null && currentUserId.equals(approverId)) {
            // 检查基础审批权限
            if (!hasPermission(currentUser, PERMISSION_APPROVAL_EXECUTE)) {
                return ApprovalPermissionResult.denied("您没有审批权限，请联系管理员");
            }
            return ApprovalPermissionResult.granted(
                    ApprovalActionType.DIRECT,
                    "直接审批"
            );
        }

        // ========== Level 2: 管理员全范围代审批（方案A） ==========
        if (ROLE_ADMIN.equals(roleName) && hasPermission(currentUser, PERMISSION_APPROVAL_EXECUTE_ALL)) {
            log.info("管理员 {} 代审批工单 {}，原审批人应为 {}",
                    currentUserId, approval.getId(), approverId);
            return ApprovalPermissionResult.granted(
                    ApprovalActionType.PROXY_ADMIN,
                    "管理员代审批",
                    approverId  // 记录原审批人
            );
        }

        // ========== Level 3: 部门经理本部门代审批（方案B） ==========
        if (ROLE_MANAGER.equals(roleName) && hasPermission(currentUser, PERMISSION_APPROVAL_EXECUTE_DEPT)) {
            // 检查是否本部门工单
            if (isSameDepartment(approval.getApplicantId(), currentUserId)) {
                log.info("部门经理 {} 代审批本部门工单 {}，原审批人应为 {}",
                        currentUserId, approval.getId(), approverId);
                return ApprovalPermissionResult.granted(
                        ApprovalActionType.PROXY_MANAGER,
                        "部门经理代审批",
                        approverId  // 记录原审批人
                );
            } else {
                log.warn("部门经理 {} 尝试审批非本部门工单 {}，权限被拒绝",
                        currentUserId, approval.getId());
                return ApprovalPermissionResult.denied(
                        "您只能审批本部门的工单"
                );
            }
        }

        // ========== Level 4: 无权限 ==========
        log.warn("用户 {} 无权审批工单 {}，当前审批人应为 {}",
                currentUserId, approval.getId(), approverId);
        return ApprovalPermissionResult.denied(
                "无权执行审批操作，您不是当前审批人"
        );
    }

    /**
     * 检查条件：检查当前用户是否为工单申请人
     * 撤销申请和重新编辑时调用
     */
    public boolean checkIsApplicant(ApprovalContext context) {
        Approval approval = context.getApproval();
        Long currentUserId = context.getOperatorId();
        Long applicantId = approval.getApplicantId();

        boolean valid = currentUserId != null && currentUserId.equals(applicantId);
        if (!valid) {
            log.warn("用户 {} 不是工单 {} 的申请人，无权执行此操作",
                    currentUserId, approval.getId());
        }
        return valid;
    }

    // ==================== 动作执行方法 ====================

    /**
     * 执行动作：提交申请
     * 状态：草稿 -> 审批中
     */
    public void doSubmit(ApprovalStatus from, ApprovalStatus to, ApprovalEvent event, ApprovalContext context) {
        log.info("执行提交动作：工单 {} 从 {} 变为 {}",
                context.getApproval().getId(), from, to);

        Approval approval = context.getApproval();
        approval.setStatus(to.getCode());

        // 如果指定了下一审批人，校验权限并设置
        if (context.getCmd() != null && context.getCmd().getNextApproverId() != null) {
            Long nextApproverId = context.getCmd().getNextApproverId();
            User nextApprover = userMapper.selectByIdWithRole(nextApproverId);
            if (nextApprover == null) {
                throw new IllegalStateException("指定的下一审批人不存在");
            }
            if (!hasPermission(nextApprover, PERMISSION_APPROVAL_EXECUTE)) {
                throw new IllegalStateException("指定的下一审批人无审批权限");
            }
            approval.setCurrentApproverId(nextApproverId);
        }

        // 记录审批历史
        saveHistory(approval.getId(), context.getOperatorId(),
                ApprovalEvent.SUBMIT.getCode(), "提交申请", null);
    }

    /**
     * 执行动作：审批通过
     * 状态：审批中 -> 已通过
     */
    public void doApprove(ApprovalStatus from, ApprovalStatus to, ApprovalEvent event, ApprovalContext context) {
        log.info("执行审批通过动作：工单 {} 从 {} 变为 {}",
                context.getApproval().getId(), from, to);

        Approval approval = context.getApproval();
        approval.setStatus(to.getCode());
        approval.setCurrentApproverId(null);

        // 从上下文中获取权限检查结果
        ApprovalPermissionResult permissionResult = context.getPermissionResult();

        // 记录审批历史（增强版）
        String comment = context.getCmd() != null ? context.getCmd().getComment() : "同意";

        // 代审批时追加标识信息
        if (permissionResult != null && permissionResult.isProxyApproval()) {
            comment = String.format("[%s] %s",
                    permissionResult.getApprovalTypeLabel(),
                    comment);
        }

        saveHistory(
                approval.getId(),
                context.getOperatorId(),
                ApprovalEvent.APPROVE.getCode(),
                comment,
                permissionResult
        );
    }

    /**
     * 执行动作：审批拒绝
     * 状态：审批中 -> 已打回
     */
    public void doReject(ApprovalStatus from, ApprovalStatus to, ApprovalEvent event, ApprovalContext context) {
        log.info("执行审批拒绝动作：工单 {} 从 {} 变为 {}",
                context.getApproval().getId(), from, to);

        Approval approval = context.getApproval();
        approval.setStatus(to.getCode());
        approval.setCurrentApproverId(null);

        // 从上下文中获取权限检查结果
        ApprovalPermissionResult permissionResult = context.getPermissionResult();

        // 记录审批历史（增强版）
        String comment = context.getCmd() != null ? context.getCmd().getComment() : "拒绝";

        // 代审批时追加标识信息
        if (permissionResult != null && permissionResult.isProxyApproval()) {
            comment = String.format("[%s] %s",
                    permissionResult.getApprovalTypeLabel(),
                    comment);
        }

        saveHistory(
                approval.getId(),
                context.getOperatorId(),
                ApprovalEvent.REJECT.getCode(),
                comment,
                permissionResult
        );
    }

    /**
     * 执行动作：重新编辑
     * 状态：已通过/已打回 -> 草稿
     */
    public void doReedit(ApprovalStatus from, ApprovalStatus to, ApprovalEvent event, ApprovalContext context) {
        log.info("执行重新编辑动作：工单 {} 从 {} 变为 {}",
                context.getApproval().getId(), from, to);

        Approval approval = context.getApproval();
        approval.setStatus(to.getCode());

        // 记录审批历史
        saveHistory(approval.getId(), context.getOperatorId(),
                ApprovalEvent.REEDIT.getCode(), "重新编辑", null);
    }

    /**
     * 执行动作：撤销申请
     * 状态：审批中 -> 草稿
     */
    public void doRevoke(ApprovalStatus from, ApprovalStatus to, ApprovalEvent event, ApprovalContext context) {
        log.info("执行撤销动作：工单 {} 从 {} 变为 {}",
                context.getApproval().getId(), from, to);

        Approval approval = context.getApproval();
        approval.setStatus(to.getCode());
        approval.setCurrentApproverId(null);

        // 记录审批历史
        String comment = context.getCmd() != null ? context.getCmd().getComment() : "撤销申请";
        saveHistory(approval.getId(), context.getOperatorId(),
                ApprovalEvent.REVOKE.getCode(), comment, null);
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 保存审批历史记录（增强版，支持代审批信息）
     *
     * @param approvalId        工单ID
     * @param approverId        审批人ID
     * @param action            操作类型
     * @param comment           审批意见
     * @param permissionResult  权限检查结果（代审批信息）
     */
    private void saveHistory(Long approvalId, Long approverId,
                             Integer action, String comment,
                             ApprovalPermissionResult permissionResult) {
        try {
            ApprovalHistory history = new ApprovalHistory();
            history.setApprovalId(approvalId);
            history.setApproverId(approverId);
            history.setAction(action);
            history.setComment(comment);
            history.setCreateTime(LocalDateTime.now());

            // 代审批信息记录
            if (permissionResult != null && permissionResult.isProxyApproval()) {
                history.setApprovalType(permissionResult.getApprovalTypeCode());
                history.setIsProxy(1);
                history.setOriginalApproverId(permissionResult.getOriginalApproverId());
                history.setProxyReason(permissionResult.getApprovalTypeLabel());
            } else {
                history.setApprovalType(ApprovalActionType.DIRECT.getCode());
                history.setIsProxy(0);
            }

            approvalHistoryMapper.insert(history);

            // 记录审计日志
            logAudit(history, permissionResult);

        } catch (Exception e) {
            log.error("保存审批历史记录失败：approvalId={}, action={}", approvalId, action, e);
        }
    }

    /**
     * 审计日志记录
     *
     * @param history          审批历史记录
     * @param permissionResult 权限检查结果
     */
    private void logAudit(ApprovalHistory history, ApprovalPermissionResult permissionResult) {
        if (permissionResult != null && permissionResult.isProxyApproval()) {
            log.info("[代审批审计] 工单ID={}, 代审批人ID={}, 原审批人ID={}, " +
                            "审批类型={}, 操作时间={}, 审批意见={}",
                    history.getApprovalId(),
                    history.getApproverId(),
                    history.getOriginalApproverId(),
                    permissionResult.getApprovalTypeLabel(),
                    history.getCreateTime(),
                    history.getComment()
            );
        } else {
            log.info("[正常审批] 工单ID={}, 审批人ID={}, 操作时间={}, 审批意见={}",
                    history.getApprovalId(),
                    history.getApproverId(),
                    history.getCreateTime(),
                    history.getComment()
            );
        }
    }

    /**
     * 检查用户是否拥有指定权限
     *
     * @param user       用户实体（需包含permissions字段）
     * @param permission 权限编码
     * @return true表示拥有该权限
     */
    private boolean hasPermission(User user, String permission) {
        if (user == null || !StringUtils.hasText(user.getPermissions())) {
            return false;
        }

        try {
            List<String> permissions = JSON.parseObject(
                    user.getPermissions(),
                    new TypeReference<>() {
                    }
            );
            // 检查是否拥有指定权限或 "all" 权限
            return permissions != null &&
                    (permissions.contains("all") || permissions.contains(permission));
        } catch (Exception e) {
            log.error("解析用户权限失败：userId={}, permissions={}",
                    user.getId(), user.getPermissions(), e);
            return false;
        }
    }

    /**
     * 检查两个用户是否属于同一部门
     *
     * @param applicantId    申请人ID
     * @param currentUserId  当前用户ID
     * @return true表示属于同一部门
     */
    private boolean isSameDepartment(Long applicantId, Long currentUserId) {
        if (applicantId == null || currentUserId == null) {
            return false;
        }

        User applicant = userMapper.selectById(applicantId);
        User currentUser = userMapper.selectById(currentUserId);

        if (applicant == null || currentUser == null) {
            return false;
        }

        Long applicantDeptId = applicant.getDeptId();
        Long currentDeptId = currentUser.getDeptId();

        // 都为空视为同部门（边界情况处理）
        if (applicantDeptId == null && currentDeptId == null) {
            return true;
        }

        // 任一为空则不同部门
        if (applicantDeptId == null || currentDeptId == null) {
            return false;
        }

        return applicantDeptId.equals(currentDeptId);
    }
}
