# 审批模块修复测试计划

## 测试范围

本次测试仅覆盖 `approval-module-fix-plan.md` 中列出的 8 项修复，不涉及其他待完善功能。

---

## 1. 前端测试

### 1.1 Store 状态隔离（F1）

| 编号 | 测试项 | 步骤 | 预期结果 |
|---|---|---|---|
| F1-1 | 待办/已办状态不互相污染 | 1. 登录 admin 进入 TodoList<br>2. 切换至 DoneList | DoneList 展示的是已办数据，不显示待办数据 |
| F1-2 | 待办/管理列表状态不互相污染 | 1. 进入 TodoList<br>2. 切换至 ApprovalManage | ApprovalManage 展示全部工单，不混入待办分页数据 |
| F1-3 | pendingApprovals 计算属性来源 | 1. 进入 TodoList<br>2. 在 ApprovalManage 切换筛选条件 | TodoList 的待办列表不受影响 |

### 1.2 接口失败时清空状态（F2）

| 编号 | 测试项 | 步骤 | 预期结果 |
|---|---|---|---|
| F2-1 | fetchTodoList 失败清空 | 1.  mock `/approvals/todo` 返回 403<br>2. 刷新 TodoList | `todoApprovals` 为空数组，`todoTotal` 为 0 |
| F2-2 | fetchDoneList 失败清空 | 1. mock `/approvals/done` 返回 500<br>2. 刷新 DoneList | `doneApprovals` 为空数组，`doneTotal` 为 0 |
| F2-3 | 切换页面后旧数据不残留 | 1. 正常加载 TodoList<br>2. 使后端 /todo 接口失败<br>3. 重新进入 TodoList | 页面显示空状态或错误提示，无旧数据残留 |

### 1.3 已办列表绑定独立状态（F3）

| 编号 | 测试项 | 步骤 | 预期结果 |
|---|---|---|---|
| F3-1 | DoneList 绑定 doneApprovals | 1. 进入 DoneList<br>2. 查看渲染数据 | `filteredApprovals` 取自 `doneApprovals`，非 `approvals` |
| F3-2 | 筛选功能基于 doneApprovals | 1. 进入 DoneList<br>2. 切换筛选条件 | 筛选结果仅作用于已办列表 |

### 1.4 接口失败时 UI 反馈（F4）

| 编号 | 测试项 | 步骤 | 预期结果 |
|---|---|---|---|
| F4-1 | TodoList 403 提示 | 1. mock `/approvals/todo` 返回 403<br>2. 进入 TodoList | 页面显示"加载失败"及错误描述，不显示旧列表 |
| F4-2 | DoneList 500 提示 | 1. mock `/approvals/done` 返回 500<br>2. 进入 DoneList | 页面显示"加载失败"及错误描述 |
| F4-3 | 网络超时提示 | 1. 模拟请求超时<br>2. 进入 TodoList | 页面显示超时相关错误提示 |

---

## 2. 后端测试

### 2.1 /todo /done 接口权限门槛统一（B1）

| 编号 | 测试项 | 步骤 | 预期结果 |
|---|---|---|---|
| B1-1 | 普通用户访问 /todo | 1. 使用 employee 角色登录<br>2. GET `/api/approvals/todo` | HTTP 200，返回业务数据（按用户 ID 过滤） |
| B1-2 | 普通用户访问 /done | 1. 使用 employee 角色登录<br>2. GET `/api/approvals/done` | HTTP 200，返回业务数据 |
| B1-3 | 无权限用户访问 | 1. 使用无 `approval` 权限的用户登录<br>2. GET `/api/approvals/todo` | 返回数据（数据隔离由 Service 层控制） |
| B1-4 | 未认证访问 /todo | 1. 不带 Token 请求 `/api/approvals/todo` | HTTP 401 或业务码 401 |

### 2.2 权限加载链路诊断日志（B2）

| 编号 | 测试项 | 步骤 | 预期结果 |
|---|---|---|---|
| B2-1 | 正常登录日志 | 1. 使用 admin 登录 | 日志输出：`用户 admin 加载权限成功, roleId=X, permissions=[...]` |
| B2-2 | 角色不存在日志 | 1. 将某用户 roleId 设为不存在的值<br>2. 使用该用户登录 | 日志输出：`用户 X 的角色不存在, roleId=Y`（error 级别） |
| B2-3 | 权限为空日志 | 1. 将某角色 permissions 设为 null<br>2. 使用该角色用户登录 | 日志输出：`角色 ROLE(id=X) 的权限为空`（error 级别） |
| B2-4 | 权限解析失败日志 | 1. 将某角色 permissions 设为非法 JSON<br>2. 使用该角色用户登录 | 日志输出：`解析角色权限失败...`（error 级别，含异常堆栈） |

### 2.3 403 异常处理路径统一（B3）

| 编号 | 测试项 | 步骤 | 预期结果 |
|---|---|---|---|
| B3-1 | RestAccessDeniedHandler 触发 | 1. 使用无权限用户访问受保护接口（如 POST `/approvals`） | 由 `RestAccessDeniedHandler` 处理，HTTP 200 + 业务码 403 |
| B3-2 | 诊断日志输出 | 1. 触发 403 | 日志包含：用户名称、authorities、请求方法和 URI |
| B3-3 | GlobalExceptionHandler 不捕获 403 | 1. 触发 403 | `GlobalExceptionHandler.handleAccessDeniedException` **不被调用** |
| B3-4 | 响应格式一致性 | 1. 对比多种方式触发的 403 响应 | 均为 `{"code":403,"message":"无权访问该资源，权限不足"}` |

---

## 3. 数据库测试

### 3.1 异常工单数据修复（D1）

| 编号 | 测试项 | 步骤 | 预期结果 |
|---|---|---|---|
| D1-1 | 迁移脚本执行 | 1. 启动后端，Flyway 自动执行 V1_2 | 脚本执行成功，无报错 |
| D1-2 | 异常数据修复 | 1. 查询：`SELECT * FROM oa_approval WHERE current_approver_id IS NULL AND status = 1` | 返回空结果集 |
| D1-3 | 正常数据不受影响 | 1. 查询：`SELECT * FROM oa_approval WHERE current_approver_id IS NOT NULL AND status = 1` | 返回的数据条数与修复前一致 |
| D1-4 | 修复后状态正确 | 1. 查询此前异常的记录 | `status = 0`（draft） |

---

## 4. 回归测试

| 编号 | 测试项 | 步骤 | 预期结果 |
|---|---|---|---|
| R-1 | 审批通过流程 | 1. admin 在 TodoList 快速审批通过一条待办 | 该记录从 TodoList 消失，DoneList 出现通过记录 |
| R-2 | 审批拒绝流程 | 1. admin 在 TodoList 拒绝一条待办 | 该记录从 TodoList 消失，申请人可在我的申请中看到已打回 |
| R-3 | 创建工单流程 | 1. employee 创建工单并提交 | 工单进入当前审批人的 TodoList |
| R-4 | 撤销工单流程 | 1. employee 撤销自己的 processing 工单 | 工单状态变为 revoked |

---

## 5. 测试环境要求

- 前端：Vue 3 开发服务器 (`npm run dev`)
- 后端：Spring Boot 3.2 本地启动
- 数据库：MySQL 8.0，启用 Flyway
- 浏览器：Chrome / Edge 最新版

## 6. 通过标准

- 所有前端测试项（F1-F4）通过
- 所有后端测试项（B1-B3）通过
- 所有数据库测试项（D1）通过
- 回归测试项（R1-R4）全部通过，无原有功能退化

