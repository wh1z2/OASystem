# 权限系统优化实现文档

**文档版本**: v1.0
**编写日期**: 2026-04-05
**文档状态**: 设计中

---

# 目录

1. [文档概述](#1-文档概述)
2. [权限角色的重新定义与分类](#2-权限角色的重新定义与分类)
3. [审批权限的层级划分方案](#3-审批权限的层级划分方案)
4. [权限检查逻辑的实现](#4-权限检查逻辑的实现)
5. [与现有审批流程的兼容性保障](#5-与现有审批流程的兼容性保障)
6. [审批流程的完整性与可追溯性](#6-审批流程的完整性与可追溯性)
7. [数据库变更清单](#7-数据库变更清单)
8. [未来可扩展功能](#8-未来可扩展功能)
9. [总结](#9-总结)

---

## 文档概述

### 背景

系统自测过程中发现，拥有审批权限的部门经理和系统管理员均无法审批当前审批人非本人的工单，系统提示"无权执行审批操作，您不是当前审批人"。这表明现有权限系统存在功能缺陷，需要进行优化完善。

### 目标

- 建立完整的双层权限模型（功能权限 + 数据权限 + 审批代理权限）
- 实现管理员全范围代审批（方案A）与部门经理本部门代审批（方案B）的协同工作
- 确保代审批操作的完整记录和可追溯性
- 保持与现有审批流程的完全兼容

### 核心设计原则

| 原则 | 说明 |
|------|------|
| 最小化改动 | 在现有基础上增强，避免大规模重构 |
| 向后兼容 | API接口和数据库变更保持兼容 |
| 可追溯性 | 所有代审批操作必须记录完整信息 |
| 层级清晰 | 权限检查优先级明确，避免冲突 |

---

## 权限角色的重新定义与分类

采用**功能权限 + 数据权限 + 审批代理权限**三层模型。

### 功能权限（操作能力）

| 权限编码 | 说明 | 适用角色 | 备注 |
|---------|------|---------|------|
| `approval:execute` | 执行审批操作（通过/拒绝） | 管理员、部门经理 | 基础审批权限 |
| `approval:execute:all` | 执行任意工单审批（全范围） | 管理员 | 覆盖所有部门 |
| `approval:execute:dept` | 执行本部门工单审批 | 部门经理 | 限于本部门范围 |
| `approval:view:all` | 查看所有审批工单 | 管理员、部门经理 | 数据查看权限 |
| `approval:transfer` | 转交审批权限 | 管理员 | 未来扩展 |

### 数据权限（数据范围）

| 权限类型 | 数据范围 | 适用角色 | 说明 |
|---------|---------|---------|------|
| `SCOPE_ALL` | 全部数据 | 管理员 | 可访问所有部门工单 |
| `SCOPE_DEPT` | 本部门及下属部门 | 部门经理 | 基于部门层级关系 |
| `SCOPE_OWN` | 仅自己的数据 | 普通员工 | 个人相关工单 |

### 审批代理权限（新增权限类型）

| 权限编码 | 触发条件 | 行为标识 | 记录要求 |
|---------|---------|---------|---------|
| `PROXY_APPROVAL_ADMIN` | 管理员审批非本人工单 | 管理员代审批 | 记录代审批信息 |
| `PROXY_APPROVAL_MANAGER` | 部门经理审批本部门非本人工单 | 经理代审批 | 记录代审批信息 |
| `DIRECT_APPROVAL` | 指定审批人审批本人工单 | 直接审批 | 标准记录 |

---

## 审批权限的层级划分方案

### 四层权限模型

```
┌─────────────────────────────────────────────────────────────────────┐
│                    审批权限层级结构（四层模型）                       │
├─────────────────────────────────────────────────────────────────────┤
│  Level 1: 指定审批人直接审批权限（最高优先级）                        │
│  ├─ 工单的 current_approver_id 明确指向该用户                        │
│  ├─ 用户拥有 approval:execute 权限                                  │
│  ├─ 标识为 DIRECT_APPROVAL                                          │
│  └─ 无需额外检查，直接拥有完全审批权                                 │
├─────────────────────────────────────────────────────────────────────┤
│  Level 2: 系统管理员全范围代审批权限（方案A）                         │
│  ├─ 用户角色为系统管理员（ADMIN）                                    │
│  ├─ 用户拥有 approval:execute:all 权限                              │
│  ├─ 可审批任意部门、任意状态的待审批工单                             │
│  ├─ 标识为 PROXY_APPROVAL_ADMIN                                     │
│  ├─ 强制记录代审批信息（代审批人、时间、原审批人）                   │
│  └─ 历史记录显示"管理员代审批"标识                                   │
├─────────────────────────────────────────────────────────────────────┤
│  Level 3: 部门经理本部门代审批权限（方案B）                           │
│  ├─ 用户角色为部门经理（MANAGER）                                    │
│  ├─ 用户拥有 approval:execute:dept 权限                              │
│  ├─ 工单申请人所属部门与经理所在部门一致（dept_id匹配）              │
│  ├─ 可审批本部门内非本人指派的待审批工单                             │
│  ├─ 标识为 PROXY_APPROVAL_MANAGER                                   │
│  ├─ 强制记录代审批信息（代审批人、时间、原审批人）                   │
│  └─ 历史记录显示"部门经理代审批"标识                                 │
├─────────────────────────────────────────────────────────────────────┤
│  Level 4: 无审批权限                                                │
│  ├─ 不满足以上任一条件                                              │
│  └─ 返回权限不足错误提示                                             │
└─────────────────────────────────────────────────────────────────────┘
```

### 层级冲突解决机制

当多个条件同时满足时，按优先级选择：

| 优先级 | 条件组合 | 实际采用层级 | 说明 |
|--------|---------|-------------|------|
| 1 | 是指定审批人 + 是管理员 | Level 1（直接审批） | 优先视为正常审批 |
| 2 | 是指定审批人 + 是部门经理 | Level 1（直接审批） | 优先视为正常审批 |
| 3 | 是管理员 + 非指定审批人 | Level 2（管理员代审批） | 触发代审批逻辑 |
| 4 | 是部门经理 + 同部门 + 非指定审批人 | Level 3（经理代审批） | 触发代审批逻辑 |

---

## 权限检查逻辑的实现

### 核心检查方法

```java
/**
 * 综合权限检查（整合方案A和方案B）
 *
 * @param context 状态机上下文
 * @param currentUser 当前操作用户（包含角色、部门信息）
 * @return ApprovalPermissionResult 权限检查结果
 */
public ApprovalPermissionResult checkApproverPermission(
        ApprovalContext context,
        User currentUser) {

    Approval approval = context.getApproval();
    Long currentUserId = currentUser.getId();
    Long approverId = approval.getCurrentApproverId();
    String roleName = currentUser.getRole().getName();

    // ========== Level 1: 指定审批人直接审批 ==========
    if (currentUserId != null && currentUserId.equals(approverId)) {
        // 检查基础审批权限
        if (!hasPermission(currentUser, "approval:execute")) {
            return ApprovalPermissionResult.denied("您没有审批权限，请联系管理员");
        }
        return ApprovalPermissionResult.granted(
            ApprovalType.DIRECT,
            "直接审批"
        );
    }

    // ========== Level 2: 管理员全范围代审批（方案A） ==========
    if ("ADMIN".equals(roleName) && hasPermission(currentUser, "approval:execute:all")) {
        log.info("管理员 {} 代审批工单 {}，原审批人应为 {}",
                currentUserId, approval.getId(), approverId);
        return ApprovalPermissionResult.granted(
            ApprovalType.PROXY_ADMIN,
            "管理员代审批",
            approverId  // 记录原审批人
        );
    }

    // ========== Level 3: 部门经理本部门代审批（方案B） ==========
    if ("MANAGER".equals(roleName) && hasPermission(currentUser, "approval:execute:dept")) {
        // 检查是否本部门工单
        if (isSameDepartment(approval.getApplicantId(), currentUserId)) {
            log.info("部门经理 {} 代审批本部门工单 {}，原审批人应为 {}",
                    currentUserId, approval.getId(), approverId);
            return ApprovalPermissionResult.granted(
                ApprovalType.PROXY_MANAGER,
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
```

### 权限检查结果类

```java
@Data
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApprovalPermissionResult {

    private final boolean granted;           // 是否允许
    private final String message;            // 提示信息
    private final ApprovalType approvalType; // 审批类型（直接/代理）
    private final Long originalApproverId;   // 原指定审批人ID（代审批时）

    /**
     * 允许直接审批
     */
    public static ApprovalPermissionResult granted(ApprovalType type, String message) {
        return new ApprovalPermissionResult(true, message, type, null);
    }

    /**
     * 允许代审批
     */
    public static ApprovalPermissionResult granted(ApprovalType type, String message,
                                                    Long originalApproverId) {
        return new ApprovalPermissionResult(true, message, type, originalApproverId);
    }

    /**
     * 拒绝审批
     */
    public static ApprovalPermissionResult denied(String message) {
        return new ApprovalPermissionResult(false, message, null, null);
    }

    /**
     * 判断是否为代审批
     */
    public boolean isProxyApproval() {
        return approvalType == ApprovalType.PROXY_ADMIN
            || approvalType == ApprovalType.PROXY_MANAGER;
    }
}
```

### 审批类型枚举

```java
public enum ApprovalType {
    DIRECT("DIRECT", "直接审批", false),
    PROXY_ADMIN("PROXY_ADMIN", "管理员代审批", true),
    PROXY_MANAGER("PROXY_MANAGER", "部门经理代审批", true);

    private final String code;
    private final String label;
    private final boolean proxy;  // 是否为代审批

    ApprovalType(String code, String label, boolean proxy) {
        this.code = code;
        this.label = label;
        this.proxy = proxy;
    }

    public String getCode() { return code; }
    public String getLabel() { return label; }
    public boolean isProxy() { return proxy; }
}
```

### 部门检查辅助方法

```java
/**
 * 检查两个用户是否属于同一部门
 */
private boolean isSameDepartment(Long applicantId, Long currentUserId) {
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
```

---

## 与现有审批流程的兼容性保障

### Service层调用调整

```java
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

    ApprovalStatus currentStatus = ApprovalStatus.fromCode(approval.getStatus());
    ApprovalContext context = new ApprovalContext(approval, cmd, operatorId);

    // 综合权限检查（整合方案A和方案B）
    ApprovalPermissionResult permissionResult =
        stateMachineHelper.checkApproverPermission(context, currentUser);

    if (!permissionResult.isGranted()) {
        throw new BusinessException(permissionResult.getMessage());
    }

    ApprovalStatus newStatus = stateMachine.fireEvent(
        currentStatus, ApprovalEvent.APPROVE, context);

    if (newStatus == currentStatus) {
        throw new BusinessException("状态转换失败");
    }

    // 更新工单
    approvalMapper.updateById(approval);

    // 记录代审批信息到历史（如为代审批）
    if (permissionResult.isProxyApproval()) {
        log.info("代审批完成：工单ID={}, 代审批人={}, 原审批人={}, 类型={}",
            id, operatorId, permissionResult.getOriginalApproverId(),
            permissionResult.getApprovalType().getLabel());
    }

    log.info("审批通过成功：id={}, operatorId={}, approvalType={}",
        id, operatorId, permissionResult.getApprovalType());
    return true;
}
```

### 状态机动作执行增强

```java
/**
 * 执行动作：审批通过（支持代审批标识）
 */
public void doApprove(ApprovalStatus from, ApprovalStatus to,
                      ApprovalEvent event, ApprovalContext context) {

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
            permissionResult.getApprovalType().getLabel(),
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
 * 保存审批历史记录（增强版，支持代审批信息）
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
            history.setApprovalType(permissionResult.getApprovalType().getCode());
            history.setIsProxy(1);
            history.setOriginalApproverId(permissionResult.getOriginalApproverId());
            history.setProxyReason(permissionResult.getApprovalType().getLabel());
        } else {
            history.setApprovalType(ApprovalType.DIRECT.getCode());
            history.setIsProxy(0);
        }

        approvalHistoryMapper.insert(history);

        // 记录审计日志
        logAudit(history, permissionResult);

    } catch (Exception e) {
        log.error("保存审批历史记录失败：approvalId={}, action={}",
                  approvalId, action, e);
    }
}
```

### API 兼容性说明

| 接口 | 变更 | 兼容性 |
|------|------|--------|
| `POST /approvals/{id}/approve` | 内部逻辑增强，请求/响应格式不变 | ✅ 完全兼容 |
| `POST /approvals/{id}/reject` | 同上 | ✅ 完全兼容 |
| `GET /approvals/{id}/history` | 响应增加代审批相关字段 | ⚠️ 新增字段，前端可选展示 |

### 前端适配建议

1. **审批按钮显示**：代审批时按钮文字显示为"代审批通过"/"代审批拒绝"
2. **审批历史展示**：代审批记录显示特殊标识（如标签"代审批"）
3. **审批详情页**：如为代审批，显示"原审批人：XXX"

---

## 审批流程的完整性与可追溯性

### 审计日志记录策略

```java
/**
 * 审计日志记录
 */
private void logAudit(ApprovalHistory history, ApprovalPermissionResult permissionResult) {
    if (permissionResult != null && permissionResult.isProxyApproval()) {
        log.info("[代审批审计] 工单ID={}, 代审批人ID={}, 原审批人ID={}, " +
                 "审批类型={}, 操作时间={}, 审批意见={}",
            history.getApprovalId(),
            history.getApproverId(),
            history.getOriginalApproverId(),
            permissionResult.getApprovalType().getLabel(),
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
```

### 关键信息记录清单

| 记录点 | 记录内容 | 存储位置 |
|--------|---------|---------|
| 审批操作 | 操作人、操作类型、时间、意见 | oa_approval_history |
| 代审批标识 | 是否代审批、代审批类型 | oa_approval_history.is_proxy |
| 原审批人信息 | 原指定审批人ID | oa_approval_history.original_approver_id |
| 审计日志 | 完整操作信息（含代审批原因） | 应用日志文件 |

---

## 数据库变更清单

### 审批历史表增强

```sql
-- 在 oa_approval_history 表中增加代审批相关字段
ALTER TABLE oa_approval_history
ADD COLUMN approval_type VARCHAR(20) DEFAULT 'DIRECT'
    COMMENT '审批类型：DIRECT直接审批/PROXY_ADMIN管理员代审批/PROXY_MANAGER经理代审批',
ADD COLUMN is_proxy TINYINT DEFAULT 0
    COMMENT '是否为代审批：0否 1是',
ADD COLUMN original_approver_id BIGINT
    COMMENT '原指定审批人ID（代审批时记录）',
ADD COLUMN proxy_reason VARCHAR(200)
    COMMENT '代审批原因/备注';

-- 创建索引优化查询
CREATE INDEX idx_approval_type ON oa_approval_history(approval_type);
CREATE INDEX idx_is_proxy ON oa_approval_history(is_proxy);
```

### 角色权限配置更新

```sql
-- 更新管理员权限（增加全范围审批权限）
UPDATE sys_role
SET permissions = '["all", "approval:execute", "approval:execute:all", "approval:view:all"]'
WHERE name = 'ADMIN';

-- 更新部门经理权限（增加本部门审批权限）
UPDATE sys_role
SET permissions = '["approval:execute", "approval:execute:dept", "approval:view:all", "user_view", "report"]'
WHERE name = 'MANAGER';
```

### 变更影响评估

| 变更项 | 影响范围 | 回滚策略 |
|--------|---------|---------|
| 新增字段 | 仅 oa_approval_history 表 | 删除新增字段即可 |
| 权限配置更新 | sys_role 表数据 | 恢复原有权限JSON |
| 索引创建 | 查询性能优化 | 删除索引即可 |

---

## 未来可扩展功能

以下为非当前必需的未来扩展方向：

### 审批委托机制
- 用户可主动将某类审批权限委托给他人
- 支持设置委托有效期
- 委托期间被委托人拥有完全审批权

### 代审批审批流程
- 部门经理代审批需经过管理员二次确认（敏感场景）
- 大额工单代审批触发额外审批流程

### 代审批统计报表
- 单独统计代审批次数、原因分布
- 代审批频率预警
- 合规审计报告生成

### 审批权限时效控制
- 管理员/经理的代审批权限可设置有效期
- 临时授权机制

### 审批代理链
- 支持多级代理（A委托B，B委托C时的处理）
- 代理链长度限制

### 审批预警机制
- 代审批频繁触发时向系统管理员发送预警
- 异常审批模式检测

---

## 总结

### 核心改进点

| 评估项 | 结论 |
|--------|------|
| **方案整合** | ✅ 方案A（管理员全范围）和方案B（部门经理本部门）协同工作 |
| **权限层级** | 四层模型：直接审批 > 管理员代审批 > 经理代审批 > 无权限 |
| **冲突解决** | 明确优先级规则，是指定审批人时优先视为直接审批 |
| **代审批记录** | 完整记录代审批人、原审批人、时间、类型 |
| **可追溯性** | 所有操作（含代审批）均有详细日志和历史记录 |
| **兼容性** | 数据库最小化变更，API保持兼容 |

### 实施建议

1. **阶段一**：数据库变更 + 核心权限检查逻辑实现
2. **阶段二**：审批历史记录增强 + 审计日志完善
3. **阶段三**：前端适配 + 集成测试
4. **阶段四**：生产部署 + 监控验证

### 风险评估

| 风险点 | 缓解措施 |
|--------|---------|
| 权限检查逻辑复杂化 | 充分单元测试，覆盖所有层级场景 |
| 代审批滥用 | 增加审计日志，定期review代审批记录 |
| 部门关系判断错误 | 明确部门ID为空时的边界处理逻辑 |

---

*文档结束*
