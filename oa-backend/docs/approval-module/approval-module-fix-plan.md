# 审批模块权限与数据一致性修复计划

> **对应报告**: [approval-module-permission-audit-report.md](./approval-module-permission-audit-report.md)
> **范围**: 仅针对审计报告中已确认的问题，不包含其他待完善项

---

## 1. 前端修复（oa-frontend）

### 1.1 拆分 Store 状态，隔离模块数据

**文件**: `src/stores/approval.js`

将单一的 `approvals.value` 拆分为三个独立状态：
- `allApprovals` —— 审批流程模块（`ApprovalManage`）专用
- `todoApprovals` —— 待办事项模块（`TodoList`）专用
- `doneApprovals` —— 已办事项模块（`DoneList`）专用

`fetchApprovals()` / `fetchTodoList()` / `fetchDoneList()` 各写入对应状态，禁止跨模块写入。

### 1.2 接口失败时清空对应状态

**文件**: `src/stores/approval.js`

在 `fetchTodoList()`、`fetchDoneList()`、`fetchApprovals()` 的 `catch` 块中，将对应状态数组清空（`[]`）并将分页总数置 `0`，避免接口 403/失败时页面仍渲染旧数据。

### 1.3 已办事项页面绑定独立状态

**文件**: `src/views/DoneList.vue`

`filteredApprovals` 计算属性改为基于 `approvalStore.doneApprovals`，而非 `approvalStore.approvals`。

### 1.4 接口失败时 UI 反馈

**文件**: `src/views/TodoList.vue`、`src/views/DoneList.vue`

当 `fetchTodoList()` / `fetchDoneList()` 返回 `success: false` 时，列表区域展示权限/错误提示文案，而非继续渲染列表或空状态。

---

## 2. 后端修复（oa-backend）

### 2.1 统一 `/todo` `/done` 接口权限门槛

**文件**: `src/main/java/com/oasystem/controller/ApprovalController.java`

将 `@GetMapping("/todo")` 与 `@GetMapping("/done")` 的 `@PreAuthorize` 注解统一为 `@PreAuthorize("isAuthenticated()")`，与 `/approvals` 保持一致。数据隔离已在 `ApprovalServiceImpl` 中通过 `currentApproverId` / 审批历史实现，无需额外的粗粒度权限注解。

### 2.2 加固权限加载链路并增加诊断日志

**文件**: `src/main/java/com/oasystem/security/UserDetailsServiceImpl.java`

- 对 `role == null` 的情况记录 `error` 级别日志。
- 对 `role.getPermissions()` 为空的情况记录 `error` 级别日志。
- 权限解析成功/失败均记录详细日志（含 `roleId`、权限内容或异常堆栈），便于排查权限加载问题。
- 评估复用 `UserMapper.selectByUsername()` 已带回的 `permissions` 字段，避免二次查询 `RoleMapper` 带来的额外失败点。

### 2.3 统一 403 异常处理路径

**涉及文件**:
- `src/main/java/com/oasystem/security/RestAccessDeniedHandler.java`
- `src/main/java/com/oasystem/exception/GlobalExceptionHandler.java`

排查并确认 `AccessDeniedException` 的最终处理路径：
- 若希望 403 统一由 `RestAccessDeniedHandler`（Filter 层）处理，则移除 `GlobalExceptionHandler` 中的 `@ExceptionHandler(AccessDeniedException.class)`。
- 或者在 `RestAccessDeniedHandler` 中增加诊断日志（当前用户、authorities），确保权限被拒绝时可追溯。

---

## 3. 数据库修复

### 3.1 修复异常工单数据

**数据**: `oa_approval` 表中 `current_approver_id IS NULL AND status = 1` 的工单（如 `id=10`，标题"111"）

执行 SQL 将无审批人的 `processing` 工单重置为 `draft`，或补充正确的 `current_approver_id`：

```sql
UPDATE oa_approval SET status = 0 WHERE current_approver_id IS NULL AND status = 1;
```

> 注：不修改 `sys_role` 中 `manager` 角色的权限配置，以保持细粒度权限体系。权限对齐通过 2.1 的注解修改完成。

---

## 修复优先级

| 顺序 | 修复项 | 优先级 | 说明 |
|------|--------|--------|------|
| 1 | 1.1 拆分 Store 状态 | 高 | 消除跨模块数据污染 |
| 2 | 1.2 失败时清空状态 | 高 | 消除接口失败后的旧数据残留 |
| 3 | 2.1 统一权限门槛 | 高 | 消除前后端权限粒度不一致导致的 403 |
| 4 | 1.3 已办绑定独立状态 | 中 | 消除已办页面数据泄露 |
| 5 | 1.4 失败 UI 反馈 | 中 | 提升权限拒绝时的用户体验 |
| 6 | 2.2 加固权限加载日志 | 中 | 增强权限问题排查能力 |
| 7 | 3.1 修复异常工单 | 中 | 清理脏数据 |
| 8 | 2.3 统一 403 处理路径 | 低 | 消除异常处理配置冲突 |
