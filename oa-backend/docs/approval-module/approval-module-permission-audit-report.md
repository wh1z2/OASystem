# OA审批系统 - 待办/已办/审批流程模块权限与数据一致性审查报告

> **审查日期**: 2026-04-18
> **审查范围**: 前端 (`oa-frontend/src/views/TodoList.vue`, `DoneList.vue`, `ApprovalManage.vue`, `stores/approval.js`) + 后端 (`ApprovalController`, `ApprovalServiceImpl`, `UserDetailsServiceImpl`, `SecurityConfig`) + 数据库 (`database/init.sql`)
> **审查账号**: 张经理 (manager / 部门经理角色)
> **报告状态**: 已确认多项高危缺陷

---

## 执行摘要

本次审查发现 **3个核心模块（待办事项、已办事项、审批流程）存在严重的数据状态污染与权限控制不一致问题**。主要表现为：

1. **前端状态管理设计缺陷**：三个模块共享同一个 `approvals` 状态数组，任一模块的接口成功响应会污染其他模块的显示数据。
2. **后端接口权限控制不一致**：`/approvals`（审批流程）仅需登录即可访问，而 `/approvals/todo` 和 `/approvals/done` 额外要求 `'approval'` 权限，导致张经理在能查看审批列表的情况下，待办/已办接口返回 403。
3. **错误处理与数据展示脱节**：前端在接口返回 403 时未清空本地状态，导致页面在"无权访问"的情况下仍能渲染数据，形成严重的权限与数据不一致。

---

## 问题现象回顾

### 场景一：初始登录后进入待办/已办

- **操作**: 以"张经理"登录后，首次点击左侧菜单"待办事项"。
- **现象**: 页面显示 1 条数据（工单"111"）。
- **网络**: `/api/approvals/todo?current=1&size=10` 返回 `{"code":403,"message":"无权访问该资源"}`。
- **已办事项同理**: `/api/approvals/done` 也返回 403，但页面显示 4 条数据（"111"、"1112"、"222"、"456"）。

### 场景二：点击审批流程后再返回

- **操作**: 点击左侧"审批流程"模块（调用 `/api/approvals` 并成功返回 8 条记录），随后返回"待办事项"。
- **现象**: 待办事项从 1 条变为 3 条，新增了 "FE-DET-003-employee"、"李四-年假申请"。
- **网络**: `/api/approvals/todo` **仍然返回 403**。
- **已办事项同理**: 已办事项从 4 条变为 8 条，新增了 "FE-DET-003-employee"、"FE-DET-003-admin"、"李四-病假申请"、"李四-年假申请"。

---

## 前端代码分析

### 共享状态污染（核心根因）

文件: [`oa-frontend/src/stores/approval.js`](oa-frontend/src/stores/approval.js)

```javascript
// 第32行：全局唯一的审批列表状态
const approvals = ref([])

// 第51-53行：待办列表 = 从 approvals 中过滤 status === 'processing'
const pendingApprovals = computed(() =>
  approvals.value.filter(a => a.status === 'processing')
)
```

三个 API 方法**全部写入同一个 `approvals.value`**：

| 方法 | 调用接口 | 写入状态 |
|------|---------|---------|
| `fetchApprovals()` | `GET /approvals` | `approvals.value = records` |
| `fetchTodoList()` | `GET /approvals/todo` | `approvals.value = records` |
| `fetchDoneList()` | `GET /approvals/done` | `approvals.value = records` |

**致命问题**：这三个模块本应维护独立的数据集（全部审批 / 我的待办 / 我的已办），但 Store 层面使用了同一个数组。当 `fetchApprovals()` 成功写入 8 条全部审批数据后，即使 `fetchTodoList()` 失败，`pendingApprovals` 计算属性仍然会从这 8 条数据中过滤出 `processing` 状态的记录并渲染到页面上。

### 错误处理未清空状态

```javascript
// approval.js 第212-228行
async function fetchTodoList(params = {}) {
  try {
    const { records, total, current, size } = await apiClient.get('/approvals/todo', ...)
    approvals.value = records.map(transformApproval)  // 成功时覆盖
    // ...
  } catch (error) {
    return { success: false, message: error.message }
    // ❌ 失败时 approvals.value 保持原样，不清空、不重置
  }
}
```

同样的缺陷存在于 `fetchDoneList()` 和 `fetchApprovals()` 中。这导致：
- 首次进入待办：`fetchTodoList()` 403 → `approvals.value` 可能保留 dashboard 或其他页面遗留的数据（如 1 条"111"）。
- 访问审批流程后再回待办：`fetchApprovals()` 成功写入 8 条 → 回待办时 `fetchTodoList()` 403 → `approvals.value` 仍为 8 条 → `pendingApprovals` 过滤出 3 条 `processing` 状态记录。

### 已办事项页面数据绑定错误

文件: [`oa-frontend/src/views/DoneList.vue`](oa-frontend/src/views/DoneList.vue)

```javascript
// 第90-94行
const filteredApprovals = computed(() => {
  // 已办列表从后端获取
  if (!filterStatus.value) return approvalStore.approvals
  return approvalStore.approvals.filter(item => item.status === filterStatus.value)
})
```

**问题**：当用户未选择状态筛选时，`DoneList` 直接展示 `approvalStore.approvals` 的全部内容。该页面假设 `fetchDoneList()` 只会把"已办"数据写入 `approvals`，但由于 3.1 所述的共享状态污染，审批流程模块的数据会泄露到已办事项页面。而且当 `fetchDoneList()` 403 失败后，页面仍展示旧数据（可能是全部审批，而非真正的已办）。

### 路由权限与后端权限不匹配

文件: [`oa-frontend/src/router/index.js`](oa-frontend/src/router/index.js)

```javascript
// 第25-26行
{
  path: 'todo',
  meta: { permissionCheck: (perms) => hasApprovalPermission(perms) },
}
```

前端路由允许"张经理"进入待办/已办页面，因为前端权限函数 `hasApprovalPermission` 检查 `['approval', 'approval:execute', 'approval:execute:all', ...]`，而登录时后端返回的用户信息包含 `permissions: ["approval:execute", "approval:execute:dept", ...]`（从 `AuthServiceImpl.login` 中获取 `userDetails.getPermissions()`）。由于 `hasApprovalPermission` 将细粒度权限 `approval:execute` 也判定为"拥有审批权限"，所以前端渲染了菜单和页面。

**矛盾点**：前端认为用户有审批相关权限（因此展示菜单和页面），但调用 `/approvals/todo` 时后端 Spring Security 的 `@PreAuthorize("hasAnyAuthority('approval', 'all')")` 只认粗粒度的 `'approval'` 权限，不认 `'approval:execute'`。这种**前后端权限粒度判定不一致**，导致了"页面能进、接口报错、数据还能显示"的诡异现象。

---

## 后端代码分析

### 接口权限控制不一致

文件: [`oa-backend/src/main/java/com/oasystem/controller/ApprovalController.java`](oa-backend/src/main/java/com/oasystem/controller/ApprovalController.java)

```java
// 第71-77行：审批流程列表（ApprovalManage）
@GetMapping
@PreAuthorize("isAuthenticated()")
public Result<PageResult<ApprovalDetailResponse>> list(ApprovalQuery query) {
    // 仅需登录，内部再做数据权限过滤（R2修复）
}

// 第137-143行：待办列表
@GetMapping("/todo")
@PreAuthorize("hasAnyAuthority('approval', 'all')")
public Result<PageResult<ApprovalDetailResponse>> getTodoList(ApprovalQuery query) {
    // 需要 'approval' 或 'all' 权限
}

// 第148-154行：已办列表
@GetMapping("/done")
@PreAuthorize("hasAnyAuthority('approval', 'all')")
public Result<PageResult<ApprovalDetailResponse>> getDoneList(ApprovalQuery query) {
    // 需要 'approval' 或 'all' 权限
}
```

**问题分析**：

- `/approvals` 的权限门槛过低（仅需 `isAuthenticated()`），其内部依靠 `ApprovalServiceImpl.list()` 中的 R2 数据权限修复逻辑（按角色过滤部门数据）进行控制。
- `/approvals/todo` 和 `/approvals/done` 的权限门槛为 `hasAnyAuthority('approval', 'all')`，要求用户拥有 `'approval'` 或 `'all'` 权限。
- **经直接查询数据库 `sys_role` 表，manager 角色的实际权限为**：
  ```json
  ["approval:execute", "approval:execute:dept", "approval:view:all", "user_view", "report"]
  ```
  其中**不包含 `'approval'`**，因此张经理访问 `/todo` 和 `/done` 返回 403 是**权限校验逻辑的预期结果**。
- **根因不是数据库配置错误或权限加载失败，而是前后端权限粒度设计不一致**：后端 Controller 层使用粗粒度权限 `'approval'`，而数据库中配置的是细粒度权限（`'approval:execute'`、`'approval:execute:dept'` 等）。同时，前端路由的 `hasApprovalPermission()` 函数将细粒度权限也视为可进入待办/已办页面的凭证，导致前端允许进入页面、后端却拒绝接口。

### 权限加载链路潜在缺陷

文件: [`oa-backend/src/main/java/com/oasystem/security/UserDetailsServiceImpl.java`](oa-backend/src/main/java/com/oasystem/security/UserDetailsServiceImpl.java)

```java
Role role = roleMapper.selectById(user.getRoleId());
// ...
List<String> permissions = Collections.emptyList();
if (role != null && role.getPermissions() != null && !role.getPermissions().isEmpty()) {
    try {
        permissions = JSON.parseArray(role.getPermissions(), String.class);
    } catch (Exception e) {
        log.warn("解析角色权限失败: {}", e.getMessage());
    }
}
```

**风险点**：

1. `roleMapper.selectById()` 通过 MyBatis-Plus 查询 `sys_role` 表。`permissions` 字段在数据库中是 `JSON` 类型（`init.sql` 第56行），实体类中定义为 `String`。如果 MyBatis-Plus / JDBC 驱动在反序列化 JSON 列时出现兼容性问题（如返回 `null`），`permissions` 将为空列表，导致 `@PreAuthorize("hasAnyAuthority('approval', 'all')")` 永远失败。
2. `UserDetailsServiceImpl` 未对 `role == null` 的情况做告警日志，仅静默返回空权限。
3. 虽然 `UserMapper.selectByUsername()` 的 JOIN 查询已经带回了 `r.permissions`，但 `UserDetailsServiceImpl` 选择再次查询 `RoleMapper`，增加了额外的失败点。

### 异常处理配置冲突

文件: [`oa-backend/src/main/java/com/oasystem/security/RestAccessDeniedHandler.java`](oa-backend/src/main/java/com/oasystem/security/RestAccessDeniedHandler.java) 与 [`GlobalExceptionHandler.java`](oa-backend/src/main/java/com/oasystem/exception/GlobalExceptionHandler.java)

`SecurityConfig` 中配置了 `RestAccessDeniedHandler` 来处理 403，但 `GlobalExceptionHandler` 也定义了 `@ExceptionHandler(AccessDeniedException.class)`。

- `RestAccessDeniedHandler` 返回消息：`"无权访问该资源，权限不足"`
- `GlobalExceptionHandler` 返回消息：`"无权访问该资源"`

截图中的响应消息为 `"无权访问该资源"`，与 `GlobalExceptionHandler` 匹配。这说明 `AccessDeniedException` 最终到达了 Spring MVC 的全局异常处理器，而非由 `RestAccessDeniedHandler` 处理。**这暗示 Spring Security 的 FilterChain 与 `@EnableMethodSecurity` 的异常处理存在配置冲突或执行顺序问题**，值得进一步排查。

---

## 问题根因深度分析

### 问题一：为什么待办/已办内容在点击审批流程后发生变化？

**根本原因：前端 Pinia Store 的共享状态污染。**

```
用户进入 TodoList
  └─> fetchTodoList() 403 失败
      └─> approvals.value 保持旧值（如 Dashboard 预加载的 1 条数据）
          └─> pendingApprovals 过滤出 1 条 processing 记录

用户进入 ApprovalManage
  └─> fetchApprovals() 200 成功
      └─> approvals.value 被覆盖为 8 条全部审批记录

用户返回 TodoList
  └─> fetchTodoList() 403 失败
      └─> approvals.value 仍为 8 条（来自 ApprovalManage）
          └─> pendingApprovals 过滤出 3 条 processing 记录（数据变了！）
```

**结论**：三个模块本应是独立的领域状态，但前端使用了一个统一的 `approvals.value` 数组，且没有在接口失败时进行回滚/清空，导致跨页面数据互相污染。

### 问题二：为什么特定工单在初始状态未显示，点击审批流程后能显示？

**根本原因：前端共享状态污染 + 前后端对待办数据的定义不一致。**

首先明确后端 `/todo` 接口的真实数据定义：`getTodoList` 查询的是 `current_approver_id = 登录用户ID AND status = PROCESSING`。基于实际数据库数据（`oa_approval` 表），张经理（`id=2`）当前需要审批的工单有且仅有 **2 条**：

| id | 标题 | applicant_id | current_approver_id | status |
|----|------|--------------|---------------------|--------|
| 4 | 李四-年假申请 | 3（李四） | 2（张经理） | 1（processing） |
| 399 | FE-DET-003-employee | 3（李四） | 2（张经理） | 1（processing） |

而工单 `"111"`（`id=10`，`applicant_id=2`，`current_approver_id=NULL`，`status=processing`）**不应出现在任何人的待办中**（没有指定当前审批人）。

**现象拆解**：

- **初始状态**：`TodoList` 调用 `fetchTodoList()` 返回 403，页面数据来自 `approvals.value` 的残留数据。此时只显示满足前端 `status === 'processing'` 的残留记录（如 `"111"`）。由于前端的 `pendingApprovals` 仅按状态过滤、**不校验 `currentApproverId`**，所以 `current_approver_id=NULL` 的 `"111"` 被错误地渲染出来，而本应显示的两条工单（id=4、id=399）因接口失败未被加载。

- **点击审批流程后**：`ApprovalManage` 调用 `fetchApprovals()`，该接口仅需 `isAuthenticated()`。后端 `list()` 方法对张经理返回了其部门下（`dept_id=1`）的全部工单，共 **8 条**（`id=1, 4, 10, 11, 12, 13, 398, 399`）。这些数据被写入共享的 `approvals.value`。

- **返回待办后**：`TodoList` 的 `pendingApprovals` 计算属性从已被污染的 `approvals.value` 中重新过滤 `status === 'processing'`。此时共享状态中有 3 条 `processing` 记录：`id=10("111")`、`id=4(李四-年假申请)`、`id=399(FE-DET-003-employee)`，于是页面显示为 3 条。

**修正后的结论**：

1. **`"李四-年假申请"` 和 `"FE-DET-003-employee"` 确实属于张经理的合法待办工单**（`current_approver_id=2` 且 `status=processing`）。它们之所以在初始状态未显示，是因为 `/todo` 接口返回 403，前端无法从后端获取真实的待办数据。
2. **`"111"` 本不该出现在待办中**。它的 `current_approver_id=NULL`，但由于前端 `pendingApprovals` 只按 `status` 过滤、不按 `currentApproverId` 过滤，导致这条"无审批人"的异常工单被错误地展示为待办。
3. **核心缺陷是前端用全部审批列表做客户端过滤来模拟待办**，而非调用专属的 `/todo` 接口。这不仅导致数据泄露（张经理能看到全部部门工单），还导致数据缺失（真正的待办因 403 加载失败）。当 `fetchApprovals()` 成功污染共享状态后，前端"凑巧"过滤出了两条本应属于待办的工单，但这只是状态污染的副作用，而非正确行为。

### 问题三：为什么接口返回 403，页面仍能显示数据？

**多层面缺陷叠加导致**：

#### 前端层面：违规的数据残留与渲染逻辑

- `fetchTodoList()` 在 catch 块中未将 `approvals.value` 置空或回滚。
- `TodoList.vue` 和 `DoneList.vue` 都直接绑定到共享状态，不感知接口是否成功。
- 这违反了"前后端分离"原则中的基本契约：前端应当信任并仅展示后端接口返回的数据，接口失败时应展示空状态或错误提示，而非继续渲染陈旧数据。

#### 后端层面：权限校验不一致与潜在的权限加载缺陷

- `/todo` 和 `/done` 需要 `'approval'` 权限，而 `/approvals` 不需要。张经理能访问 `/approvals` 说明他是认证用户，但 `/todo` 返回 403 说明其 `Authentication` 对象中的 `GrantedAuthority` 集合不包含 `'approval'`。
- **经直接查询数据库，`manager` 角色的实际权限为 `["approval:execute", "approval:execute:dept", "approval:view:all", "user_view", "report"]`，确实不含 `'approval'`**。这不是权限加载失败或数据库被意外修改，而是**数据库配置与 Controller 权限注解的粒度不匹配**：数据库存的是细粒度权限（`approval:execute` 等），而 `@PreAuthorize` 要求的是粗粒度权限（`approval`）。
- 前端 `hasApprovalPermission()` 函数将细粒度权限也视为可进入页面的凭证，进一步加剧了"页面能进、接口被拒"的不一致现象。

#### 数据库层面：权限粒度设计与实际配置不匹配

- **已确认的实际数据**：直接查询 `sys_role` 表，`manager` 角色的 `permissions` 为 `["approval:execute", "approval:execute:dept", "approval:view:all", "user_view", "report"]`，**不包含 `'approval'`**。
- **数据状态**：`oa_approval` 表共有 11 条记录。其中 `current_approver_id=2`（张经理）且 `status=1`（processing）的工单仅 2 条（`id=4`、`id=399`），其余工单 `current_approver_id` 均为 `NULL`。
- **异常数据**：`id=10`（标题"111"）为张经理自己创建的工单，`status=processing` 但 `current_approver_id=NULL`，说明该工单在提交后未正确分配审批人，处于异常状态。这条工单本不应出现在任何人的待办列表中。

---

## 问题重现步骤

### 前置条件
- 使用"张经理"账号（manager / manager123）登录系统。
- 确保后端 `/approvals/todo` 接口对该用户返回 403（如当前环境所示）。

### 步骤 1：验证初始状态污染
1. 清除浏览器缓存并重新登录。
2. 打开浏览器开发者工具，切换到 Network 面板。
3. 直接点击左侧菜单"待办事项"。
4. 观察：
   - `/api/approvals/todo` 请求返回 403。
   - 页面仍显示 N 条数据（来自 `approvals.value` 残留）。

### 步骤 2：验证跨模块状态污染
1. 在待办事项页面记录当前显示的数据条数。
2. 点击左侧"审批流程"。
3. 确认 `/api/approvals` 返回 200 并携带多条记录。
4. 再次点击"待办事项"。
5. 观察：
   - `/api/approvals/todo` 仍返回 403。
   - 页面显示的数据条数增加，且出现了不属于待办的工单。

### 步骤 3：验证已办事项同样受影响
1. 重复步骤 2，但观察"已办事项"页面。
2. 确认 `/api/approvals/done` 返回 403。
3. 确认页面显示的条数从 4 条变为 8 条（与 `/approvals` 返回的总数一致）。

---

## 解决方案

### 前端修复（高优先级）

#### 修复 1：拆分 Store 状态，模块间数据隔离

文件: `oa-frontend/src/stores/approval.js`

将单一的 `approvals.value` 拆分为三个独立的状态：

```javascript
const allApprovals = ref([])      // 审批流程模块使用
const todoApprovals = ref([])     // 待办事项模块使用
const doneApprovals = ref([])     // 已办事项模块使用

const pendingApprovals = computed(() => todoApprovals.value)
```

`fetchApprovals()` 只写入 `allApprovals.value`，`fetchTodoList()` 只写入 `todoApprovals.value`，`fetchDoneList()` 只写入 `doneApprovals.value`。

#### 修复 2：接口失败时清空对应状态

```javascript
async function fetchTodoList(params = {}) {
  try {
    const { records, total, current, size } = await apiClient.get('/approvals/todo', ...)
    todoApprovals.value = records.map(transformApproval)
    todoTotal.value = total
    return { success: true, data: todoApprovals.value }
  } catch (error) {
    todoApprovals.value = []   // ❗失败时清空，避免渲染旧数据
    todoTotal.value = 0
    return { success: false, message: error.message }
  }
}
```

对 `fetchDoneList()` 和 `fetchApprovals()` 做同样处理。

#### 修复 3：已办事项页面绑定独立状态

文件: `oa-frontend/src/views/DoneList.vue`

```javascript
const filteredApprovals = computed(() => {
  const baseList = approvalStore.doneApprovals  // 绑定已办专属状态
  if (!filterStatus.value) return baseList
  return baseList.filter(item => item.status === filterStatus.value)
})
```

#### 修复 4：错误状态 UI 反馈

当 `fetchTodoList()` / `fetchDoneList()` 返回 `success: false` 时，页面应在列表区域展示权限错误提示（如"暂无权限查看待办事项，请联系管理员"），而非渲染列表或空状态文案。

### 后端修复（高优先级）

#### 修复 1：统一待办/已办/全部列表的权限门槛

文件: `oa-backend/src/main/java/com/oasystem/controller/ApprovalController.java`

建议将 `/todo` 和 `/done` 的权限注解与 `/approvals` 保持一致，改为 `@PreAuthorize("isAuthenticated()")`，把权限判断下沉到 Service 层的数据权限逻辑中（已在 `ApprovalServiceImpl.getTodoList` 中按 `currentApproverId` 过滤，`getDoneList` 按审批历史过滤，本身已有数据隔离）：

```java
@GetMapping("/todo")
@PreAuthorize("isAuthenticated()")  // 改为仅需登录
public Result<PageResult<ApprovalDetailResponse>> getTodoList(ApprovalQuery query) {
    Long approverId = getCurrentUserId();
    PageResult<ApprovalDetailResponse> result = approvalService.getTodoList(approverId, query);
    return Result.success(result);
}
```

**理由**：
- 待办的定义已经是"当前审批人是登录用户"，天然带有数据隔离。
- 已办的定义已经是"审批历史中存在该用户的处理记录"，也天然带有数据隔离。
- 在已经做了数据权限隔离的情况下，再要求 `'approval'` 权限是多余的，且造成了前后端不一致。

#### 修复 2：加固权限加载链路，增加诊断日志

文件: `oa-backend/src/main/java/com/oasystem/security/UserDetailsServiceImpl.java`

```java
@Override
public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    User user = userMapper.selectByUsername(username);
    if (user == null) {
        throw new UsernameNotFoundException("用户不存在: " + username);
    }

    Role role = roleMapper.selectById(user.getRoleId());
    List<String> permissions = Collections.emptyList();

    if (role == null) {
        log.error("用户 {} 的角色 {} 不存在", username, user.getRoleId());
    } else {
        if (role.getPermissions() == null || role.getPermissions().isEmpty()) {
            log.error("角色 {} 的 permissions 字段为空", role.getId());
        } else {
            try {
                permissions = JSON.parseArray(role.getPermissions(), String.class);
                log.debug("用户 {} 加载权限: {}", username, permissions);
            } catch (Exception e) {
                log.error("解析角色权限失败: roleId={}, permissions={}", role.getId(), role.getPermissions(), e);
            }
        }
    }

    // ...
}
```

#### 修复 3：排查并统一 403 异常处理路径

确认 `RestAccessDeniedHandler` 和 `GlobalExceptionHandler` 的优先级。建议：
- 如果希望所有 403 都走 `RestAccessDeniedHandler`（Filter 层），则移除 `GlobalExceptionHandler` 中对 `AccessDeniedException` 的处理器。
- 或者，在 `RestAccessDeniedHandler` 中记录更详细的日志（包括当前用户的 username 和 authorities），便于排查为什么 `manager` 用户会被拒绝。

### 数据库检查与修复（中优先级）

**已执行的实际查询及结果**：

```sql
-- 1. 检查 manager 角色的权限配置
SELECT id, name, label, permissions FROM sys_role WHERE name = 'manager';
-- 结果: permissions = ["approval:execute", "approval:execute:dept", "approval:view:all", "user_view", "report"]

-- 2. 检查张经理的实际角色绑定
SELECT u.id, u.username, u.name, u.role_id, r.name as role_name, r.permissions
FROM sys_user u
LEFT JOIN sys_role r ON u.role_id = r.id
WHERE u.username = 'manager';
-- 结果: role_id=2, role_name=manager

-- 3. 检查所有审批工单
SELECT id, title, applicant_id, current_approver_id, status FROM oa_approval;
-- 结果: 共11条。current_approver_id=2 且 status=1 的仅 id=4、id=399
```

**基于实际数据的修复建议**：

1. **权限粒度对齐（推荐）**：
   不要简单地在数据库中给 `manager` 角色添加粗粒度 `"approval"` 权限，因为这会破坏现有的细粒度权限体系（`approval:execute`、`approval:execute:dept` 等）。正确的做法是将后端 `@PreAuthorize("hasAnyAuthority('approval', 'all')")` 修改为支持细粒度权限：
   ```java
   @PreAuthorize("hasAnyAuthority('approval', 'approval:execute', 'approval:execute:all', 'all')")
   ```
   或者统一降为 `@PreAuthorize("isAuthenticated()")`，将权限控制完全下沉到 Service 层的数据权限逻辑中。

2. **修复异常工单数据**：
   `id=10`（"111"）的 `current_approver_id=NULL` 但 `status=processing`，属于异常状态。应检查状态机或提交逻辑，确保工单提交时正确设置 `current_approver_id`。对于已存在的脏数据，可通过 SQL 修正：
   ```sql
   -- 将无审批人的 processing 工单重置为 draft，或补充正确的 current_approver_id
   UPDATE oa_approval SET status = 0 WHERE current_approver_id IS NULL AND status = 1;
   ```

---

## 8. 总结

| 问题 | 根因 | 责任方 | 严重程度 |
|------|------|--------|----------|
| 模块间数据互相污染 | Store 使用共享 `approvals.value` | 前端 | **高危** |
| 接口失败后仍显示旧数据 | Catch 块未清空状态 | 前端 | **高危** |
| 已办页面显示全部审批 | DoneList 直接绑定 `approvalStore.approvals` | 前端 | **中危** |
| 待办/已办接口 403 | 后端 `@PreAuthorize` 要求粗粒度 `'approval'` 权限，但 DB 中配置的是细粒度权限（`approval:execute` 等），导致前后端权限粒度不匹配 | 后端 | **高危** |
| 前后端权限判定不一致 | 前端路由允许进入，后端接口拒绝 | 前后端 | **中危** |
| 403 异常处理路径混乱 | `RestAccessDeniedHandler` 与 `GlobalExceptionHandler` 并存 | 后端 | **低危** |

**建议修复顺序**：

1. **立即**：前端拆分 Store 状态 + 失败时清空数据（消除数据污染和安全风险）。
2. **立即**：后端统一 `/todo` `/done` 的权限注解为 `isAuthenticated()`，或排查并修复数据库中 `manager` 角色的实际权限缺失问题。
3. **随后**：后端增加权限加载的诊断日志，统一 403 异常处理路径。

---

> **报告生成说明**：本报告基于对 `oa-frontend/src/stores/approval.js`、`oa-frontend/src/views/TodoList.vue`、`DoneList.vue`、`ApprovalManage.vue`、`oa-backend/src/main/java/com/oasystem/controller/ApprovalController.java`、`ApprovalServiceImpl.java`、`UserDetailsServiceImpl.java`、`SecurityConfig.java` 及 `database/init.sql` 的完整代码审查，并结合用户提供的浏览器开发者工具抓包截图进行分析得出。
