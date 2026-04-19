# Admin 权限增强实施计划

**文档版本**: v1.1
**编写日期**: 2026-04-19
**文档状态**: 待审批（已更新为真实数据库数据）

---

## 目录

1. [需求概述](#1-需求概述)
2. [现状分析](#2-现状分析)
3. [改动范围](#3-改动范围)
4. [详细设计方案](#4-详细设计方案)
5. [测试计划](#5-测试计划)
6. [回归测试清单](#6-回归测试清单)
7. [风险与兼容性评估](#7-风险与兼容性评估)
8. [实施步骤](#8-实施步骤)

---

## 1. 需求概述

为系统管理员（admin）补充以下两项权限：

| 需求 | 描述 | 涉及层面 |
|------|------|---------|
| **审批所有待审批工单** | admin 可以审批系统中任意状态为"审批中(PROCESSING)"的工单，无论该工单是否指派给 admin | 后端操作权限 |
| **查看所有待审批工单** | admin 在待办列表、工作台统计中能看到全系统所有待审批工单 | 后端数据查询 |

---

## 2. 现状分析

### 2.1 审批操作层面 —— 已实现

后端 `ApprovalStateMachineHelper.checkApproverPermissionDetail()` 已实现 **4级权限层级**：

```
Level 1: 直接审批 (DIRECT)     → 当前用户 = current_approver_id
Level 2: 管理员代审批 (PROXY_ADMIN) → role=admin 且有 approval:execute:all
Level 3: 经理代审批 (PROXY_MANAGER) → role=manager 且同部门
Level 4: 无权限 (DENIED)
```

**真实数据库角色权限配置（截至 2026-04-19）**：

| 角色 | permissions JSON |
|------|-----------------|
| admin | `["all", "approval:execute", "approval:execute:all", "approval:view:all"]` |
| manager | `["approval:execute", "approval:execute:dept", "approval:view:all", "user_view", "report"]` |
| employee | `["apply", "personal"]` |

admin 已显式拥有 `approval:execute:all` 和 `approval:view:all`。`hasPermission()` 方法逻辑为 `permissions.contains("all") || permissions.contains(permission)`，因此 admin 已满足 Level 2 权限校验。

**结论：admin 在审批操作层面已经具备全范围代审批能力。** 现有测试 `PermissionProxyApprovalTest` 已覆盖此场景。

### 2.2 数据查询层面 —— 未实现

当前待办列表查询逻辑（`ApprovalServiceImpl.getTodoList`）固定按 `current_approver_id = ? AND status = 1` 过滤：

```java
wrapper.eq(Approval::getCurrentApproverId, approverId);
wrapper.eq(Approval::getStatus, ApprovalStatus.PROCESSING.getCode());
```

这导致 admin 调用待办接口时，**只能看到指派给自己的待审批工单**，无法看到全系统待办。

同理，工作台统计（`getDashboardStatistics`）中的 `pendingCount` 使用 `approvalMapper.countTodoByApproverId(userId)`，admin 的待办数量统计也仅包含指派给自己的工单。

**结论：数据查询层面需要增强，使 admin 的待办视角为全系统范围。**

---

## 3. 改动范围

### 3.1 后端代码改动

| 文件 | 路径 | 改动内容 | 影响 |
|------|------|---------|------|
| `ApprovalServiceImpl.java` | `service/impl/` | `getTodoList()` 增加角色判断：admin 查询全部 PROCESSING 工单 | 待办列表数据范围 |
| `ApprovalServiceImpl.java` | `service/impl/` | `getDashboardStatistics()` 增加角色判断：admin 统计全部 PROCESSING 工单数 | 工作台统计数字 |
| `ApprovalMapper.java` | `mapper/` | 新增 `countAllTodos()` 查询全系统待办总数 | 统计接口支持 |
| `ApprovalService.java` | `service/` | 接口文件无需变更（签名不变） | - |
| `ApprovalController.java` | `controller/` | 无需变更 | - |

### 3.2 数据库改动

**无需改动。** 真实数据库 `sys_role` 表中 admin 角色的 permissions 已包含 `"approval:execute:all"` 和 `"approval:view:all"`（见 2.1 节表格）。`init.sql` 初始化文件与实际数据存在差异，以数据库真实数据为准。

### 3.3 前端改动

**无需任何前端改动。** 前端 `TodoList.vue`、`Dashboard.vue` 均通过标准 API 获取数据，数据范围由后端控制。

---

## 4. 详细设计方案

### 4.1 待办列表查询增强

**当前逻辑（`ApprovalServiceImpl.getTodoList`）**：
```java
LambdaQueryWrapper<Approval> wrapper = Wrappers.lambdaQuery();
wrapper.eq(Approval::getCurrentApproverId, approverId);
wrapper.eq(Approval::getStatus, ApprovalStatus.PROCESSING.getCode());
```

**目标逻辑**：
```java
User currentUser = userMapper.selectByIdWithRole(approverId);

LambdaQueryWrapper<Approval> wrapper = Wrappers.lambdaQuery();
wrapper.eq(Approval::getStatus, ApprovalStatus.PROCESSING.getCode());

// 非 admin 用户，仍然只查询指派给自己的待办
if (currentUser == null || !"admin".equals(currentUser.getRoleName())) {
    wrapper.eq(Approval::getCurrentApproverId, approverId);
}
// admin 用户不增加 current_approver_id 过滤条件，即查看全系统待办

wrapper.orderByDesc(Approval::getCreateTime);
```

### 4.2 工作台统计增强

**当前逻辑（`ApprovalServiceImpl.getDashboardStatistics`）**：
```java
Long pendingCount = approvalMapper.countTodoByApproverId(userId);
```

**目标逻辑**：
```java
User currentUser = userMapper.selectByIdWithRole(userId);
Long pendingCount;
if (currentUser != null && "admin".equals(currentUser.getRoleName())) {
    pendingCount = approvalMapper.countAllTodos(); // 全系统待办数
} else {
    pendingCount = approvalMapper.countTodoByApproverId(userId); // 个人待办数
}
```

### 4.3 Mapper 新增方法

在 `ApprovalMapper.java` 中新增：
```java
/**
 * 查询全系统待办工单数量（admin 统计用）
 */
@Select("SELECT COUNT(*) FROM oa_approval WHERE status = 1")
Long countAllTodos();
```

---

## 5. 测试计划

### 5.1 新增单元测试

新建测试类 `AdminPermissionEnhancementTest.java`：

| 测试方法 | 场景 | 断言 |
|---------|------|------|
| `testAdminCanSeeAllTodoApprovals` | admin 查询待办列表 | 返回全系统所有 PROCESSING 工单，包含非指派给自己的 |
| `testManagerCanOnlySeeAssignedTodoApprovals` | 经理查询待办列表 | 仅返回指派给自己的 PROCESSING 工单（回归） |
| `testEmployeeCanOnlySeeAssignedTodoApprovals` | 员工查询待办列表 | 仅返回指派给自己的 PROCESSING 工单（回归） |
| `testAdminDashboardPendingCount` | admin 工作台统计 | pendingCount 等于全系统 PROCESSING 工单总数 |
| `testManagerDashboardPendingCount` | 经理工作台统计 | pendingCount 等于指派给自己的 PROCESSING 工单数（回归） |

### 5.2 测试数据准备（基于真实数据库数据）

当前 `sys_user` 真实数据：

| id | username | role | dept_id | 部门 |
|----|----------|------|---------|------|
| 1 | admin | admin | 1 | 技术部 |
| 2 | manager | manager | 1 | 技术部 |
| 3 | user | employee | 2 | 财务部 |
| 4 | zhangsan | employee | 2 | 财务部 |
| 5 | wangwu | employee | 3 | 人事部 |

**当前真实数据**：李四（id=3）`dept_id=2`（财务部），与经理（`dept_id=1`，技术部）不同部门。

**对待办列表测试的影响**：
- 当前全系统 `status=1`（PROCESSING）的工单共 **2 条**：
  - id=4：applicant_id=3（李四），current_approver_id=2（经理）
  - id=401：applicant_id=3（李四），current_approver_id=1（admin）
- 增强后 admin 查询待办，应返回 **2 条**（id=4 和 id=401）。
- 经理查询待办，目前仅返回指派给自己的 **1 条**（id=4）。

测试中使用 `SpringBootTest + @Transactional` 确保测试数据隔离，不受真实数据变化影响。

---

## 6. 回归测试清单

实施完成后，必须运行以下现有测试，确保无功能退化：

| 测试类 | 路径 | 测试范围 | 用例数 |
|--------|------|---------|--------|
| `ApprovalServiceTest` | `service/` | 工单 CRUD、提交、审批、拒绝、重新编辑、撤销 | ~18 |
| `PermissionProxyApprovalTest` | `service/` | 4级权限层级、代审批场景 | ~8 |
| `ApprovalDataPermissionTest` | `service/` | 数据权限过滤（详情/列表） | ~10 |
| `MethodSecurityTest` | `controller/` | 方法级权限控制 | ~32 |
| `StateMachineConfigTest` | `config/` | COLA 状态机流转规则 | ~13 |
| `ApprovalStateMachineTest` | `statemachine/` | 状态机条件和动作 | ~12 |
| `JwtTokenUtilTest` | `util/` | JWT 工具类 | ~4 |
| `AuthPermissionTest` | `controller/` | 认证接口权限 | ~若干 |

**回归通过标准**：上述所有测试用例 100% 通过，无新增失败。

---

## 7. 风险与兼容性评估

| 风险点 | 影响 | 缓解措施 |
|--------|------|---------|
| admin 待办列表数据量过大 | 性能 | 待办列表本身已有分页参数（ApprovalQuery），admin 查询全量时仍受分页保护 |
| 经理/员工待办列表行为变化 | 功能退化 | 代码逻辑明确区分 `admin` 与其他角色，非 admin 走原分支，不影响 |
| 工作台统计数字突变 | 用户体验 | admin 的 pendingCount 将变为全系统待办数，符合"看到所有待审批"的需求预期 |
| 前端徽章计数异常 | 功能退化 | 前端 `todoTotal` 独立存储，仅由 `/approvals/todo` 接口维护，无需改动 |

**API 兼容性**：所有接口的请求/响应格式完全不变，仅 admin 用户看到的数据范围扩大。

---

## 8. 实施步骤

```
Step 1: 后端 Mapper 层
  └─ ApprovalMapper.java 新增 countAllTodos() 方法

Step 2: 后端 Service 层
  └─ ApprovalServiceImpl.java 修改 getTodoList() 方法
  └─ ApprovalServiceImpl.java 修改 getDashboardStatistics() 方法

Step 3: 新增单元测试
  └─ 创建 AdminPermissionEnhancementTest.java
  └─ 覆盖 admin 待办列表、admin 统计、非 admin 回归场景

Step 4: 回归测试
  └─ 运行全部现有测试类，确认 100% 通过

Step 5: 更新架构文档
  └─ 更新 memory-bank/architecture.md 中相关章节
```

---

## 附录：审批权限层级回顾（已落地）

```
Level 1: 直接审批 (DIRECT)
  └─ 当前用户 = current_approver_id

Level 2: 管理员代审批 (PROXY_ADMIN)  ← admin 已具备
  └─ role = admin

Level 3: 部门经理代审批 (PROXY_MANAGER)
  └─ role = manager 且同部门

Level 4: 无权限 (DENIED)
```

本次增强的本质是：**在数据查询维度（待办列表/统计）补齐 admin 的全局视野**，与已有操作维度的代审批权限对齐。

---

*文档结束*
