# 工单编辑功能测试方案

**编制日期**: 2026-04-20
**对应需求**: 工单编辑功能评估与重构方案（方案4）
**测试范围**: 后端 `update` / `reedit` 接口、前端编辑入口、COLA 状态机兼容性

---

## 一、测试目标

1. 验证 `update` 接口对 DRAFT 状态工单的编辑能力符合预期（仅申请人可编辑、仅 DRAFT 可编辑）。
2. 验证 `reedit` 接口增强后，可选携带内容参数时，能正确完成"状态回退 + 内容更新"的原子操作。
3. 验证 `reedit` 不带内容参数时，行为与增强前完全一致（100% 向后兼容）。
4. 验证前端草稿状态编辑入口（`ApprovalDetail.vue`、`ApprovalManage.vue`、`ApprovalCreate.vue` 编辑模式）功能可用。
5. 验证 COLA 状态机未受任何影响，现有 6 条流转规则全部有效。

---

## 二、测试策略

| 测试层级 | 范围 | 工具/框架 | 负责人 |
|----------|------|-----------|--------|
| 单元测试 | Service 层核心方法（`update`、`reedit`） | JUnit 5 + Mockito + Spring Boot Test | 开发 |
| 集成测试 | Controller 层接口、状态机流转 | Spring Boot Test + MockMvc | 开发 |
| 前端单元测试 | Vue 组件交互逻辑 | Vitest + Vue Test Utils | 前端开发 |
| 端到端测试 | 完整编辑流程（草稿编辑、已打回重新编辑） | Playwright / 手工 | 测试/QA |
| 兼容性测试 | `reedit` 无请求体时的旧行为 | 自动化回归 | 开发 |

---

## 三、测试用例

### 3.1 后端接口测试

#### TC-BE-001: `update` 编辑草稿工单成功
- **前置**: 创建一条 DRAFT 状态工单，申请人 ID = 100
- **操作**: 调用 `POST /approvals/{id}/update`，body 包含 title="新标题", content="新内容"
- **预期**:
  - HTTP 200，返回 `Result.success(true)`
  - 数据库中该工单 title="新标题"，content="新内容"
  - status 仍为 DRAFT
  - `update_time` 字段已更新

#### TC-BE-002: `update` 编辑非草稿工单失败
- **前置**: 创建一条 PROCESSING 状态工单
- **操作**: 调用 `POST /approvals/{id}/update`
- **预期**: HTTP 200，返回业务错误码，`message` 包含"只有草稿状态的工单可以编辑"

#### TC-BE-003: `update` 非申请人编辑失败
- **前置**: 创建一条 DRAFT 状态工单，申请人 ID = 100
- **操作**: 以 ID = 999（非申请人）调用 `POST /approvals/{id}/update`
- **预期**: 返回业务错误，提示无权编辑

#### TC-BE-004: `reedit` 不带请求体——已通过状态回退
- **前置**: 创建并审批通过一条工单（APPROVED）
- **操作**: 申请人调用 `POST /approvals/{id}/reedit`，body 为 `null` 或 `{}`
- **预期**:
  - HTTP 200，返回 `Result.success(true)`
  - 数据库 status 变为 DRAFT
  - 工单内容（title/content/priority/formData）与 reedit 前完全一致
  - `oa_approval_history` 新增一条 REEDIT 记录

#### TC-BE-005: `reedit` 携带请求体——已打回状态回退并同步更新内容
- **前置**: 创建、提交、审批拒绝一条工单（RETURNED），原 title="旧标题"
- **操作**: 申请人调用 `POST /approvals/{id}/reedit`，body 包含 title="新标题", priority=2, content="已修正内容", formData={updated: true}
- **预期**:
  - HTTP 200，返回 `Result.success(true)`
  - 数据库 status 变为 DRAFT
  - title="新标题"，priority=2，content="已修正内容"，formData 已更新
  - 审批历史新增 REEDIT 记录

#### TC-BE-006: `reedit` 携带部分请求体——仅更新非空字段
- **前置**: 创建、提交、审批通过一条工单（APPROVED），原 title="原标题", content="原内容"
- **操作**: 申请人调用 `POST /approvals/{id}/reedit`，body 仅包含 title="仅改标题"
- **预期**:
  - title 变为"仅改标题"
  - content 保持"原内容"不变（非空字段覆盖策略）
  - status 变为 DRAFT

#### TC-BE-007: `reedit` 审批人字段校验
- **前置**: 创建、提交、审批通过一条工单（APPROVED）
- **操作**: 申请人调用 `POST /approvals/{id}/reedit`，body 包含 `currentApproverId=99999`（不存在的用户）
- **预期**: 返回业务错误，提示"指定的审批人不存在"

#### TC-BE-008: `reedit` 非申请人调用失败
- **前置**: 创建、提交、审批通过一条工单（APPROVED），申请人 ID = 100
- **操作**: 以 ID = 999（非申请人）调用 `POST /approvals/{id}/reedit`
- **预期**: 返回业务错误，提示"当前状态不允许重新编辑"

#### TC-BE-009: `reedit` 草稿状态调用失败
- **前置**: 创建一条 DRAFT 状态工单
- **操作**: 申请人调用 `POST /approvals/{id}/reedit`
- **预期**: 返回业务错误，提示"当前状态不允许重新编辑"（状态机无 DRAFT→DRAFT 自循环）

#### TC-BE-010: 向后兼容性——旧版前端调用 `reedit` 无 body
- **前置**: 已上线系统的前端调用 `POST /approvals/{id}/reedit`，不带 Content-Type body
- **操作**: 直接调用接口
- **预期**: 行为与增强前完全一致，状态回退成功，内容不变，HTTP 200

---

### 3.2 状态机集成测试

#### TC-SM-001: APPROVED → DRAFT（REEDIT）
- **验证点**: 确认方案4未破坏原有流转
- **预期**: `stateMachine.fireEvent(APPROVED, REEDIT, context)` 返回 DRAFT

#### TC-SM-002: RETURNED → DRAFT（REEDIT）
- **验证点**: 确认 RETURNED 回退仍然有效
- **预期**: `stateMachine.fireEvent(RETURNED, REEDIT, context)` 返回 DRAFT

#### TC-SM-003: DRAFT → REEDIT（应失败）
- **验证点**: 确认不存在 DRAFT 自循环
- **预期**: `stateMachine.fireEvent(DRAFT, REEDIT, context)` 返回 DRAFT（源状态，即失败）

#### TC-SM-004: 历史数据保护——已通过工单仍可 reedit
- **验证点**: 数据库中状态为 APPROVED 的历史工单
- **操作**: 申请人调用 `reedit`
- **预期**: 可正常回退至 DRAFT，无功能倒退

---

### 3.3 前端交互测试

#### TC-FE-001: 详情页草稿状态显示"编辑内容"按钮
- **页面**: `ApprovalDetail.vue`
- **前置**: 打开一条 DRAFT 状态工单的详情页，当前用户为申请人
- **预期**: 审批操作卡片中显示"提交审批"和"编辑内容"两个按钮

#### TC-FE-002: 点击"编辑内容"跳转编辑页
- **页面**: `ApprovalDetail.vue`
- **操作**: 点击"编辑内容"
- **预期**: 路由跳转至 `/approval/edit/{id}`，页面标题为"编辑审批"

#### TC-FE-003: 编辑页数据回填
- **页面**: `ApprovalCreate.vue`（编辑模式）
- **前置**: 打开编辑页，工单原数据 title="测试标题", type=leave, priority=normal
- **预期**: 表单中 title 显示"测试标题"，类型显示"请假申请"（只读），优先级选中"普通"

#### TC-FE-004: 编辑页类型字段只读
- **页面**: `ApprovalCreate.vue`（编辑模式）
- **操作**: 观察审批类型字段
- **预期**: 类型为静态文本展示，不可选择/修改

#### TC-FE-005: 编辑页保存修改成功
- **页面**: `ApprovalCreate.vue`（编辑模式）
- **操作**: 修改 title 为"新标题"，点击"保存修改"
- **预期**: 调用 `updateApproval`，成功后跳转回详情页，详情页显示新标题

#### TC-FE-006: 列表页草稿行显示"编辑"操作
- **页面**: `ApprovalManage.vue`
- **前置**: 列表中存在 DRAFT 状态工单
- **预期**: 对应行的操作列显示"查看"、"编辑"按钮

#### TC-FE-007: 列表页非草稿行不显示"编辑"
- **页面**: `ApprovalManage.vue`
- **前置**: 列表中存在 APPROVED / PROCESSING 状态工单
- **预期**: 对应行操作列不显示"编辑"按钮

#### TC-FE-008: 列表页非申请人看不到"编辑"
- **页面**: `ApprovalManage.vue`
- **前置**: 列表中存在其他用户创建的 DRAFT 工单
- **预期**: 当前用户非申请人且非管理员时，该 DRAFT 行不显示"编辑"

---

### 3.4 端到端流程测试

#### TC-E2E-001: 完整草稿编辑流程
```
登录（申请人）→ 创建工单（保存为草稿）→ 进入我的申请 → 点击编辑
→ 修改内容 → 保存 → 返回详情页 → 验证内容已更新 → 状态仍为草稿
```

#### TC-E2E-002: 完整已打回重新编辑流程（一步到位）
```
登录（申请人）→ 提交工单 → 审批人拒绝 → 申请人进入我的申请
→ 打开已打回工单 → 点击重新编辑（带内容修正）→ 输入修正内容
→ 确认 → 验证工单变为草稿且内容已更新
```

#### TC-E2E-003: 已通过后重新编辑（兼容旧流程）
```
登录（申请人）→ 提交工单 → 审批人通过 → 申请人进入我的申请
→ 打开已通过工单 → 点击重新编辑（不带内容修正）→ 验证变为草稿
→ 再点击编辑内容 → 修正内容 → 保存 → 重新提交
```

---

## 四、测试环境

| 环境 | 用途 | 配置 |
|------|------|------|
| 本地开发环境 | 单元测试、前端单测 | H2 内存数据库 / 实际 MySQL |
| CI 环境 | 自动化集成测试 | Docker 启动 MySQL 8.0 |
| 测试服务器 | 端到端测试 | 完整前后端部署 |

---

## 五、测试通过标准

1. **后端**: TC-BE-001 ~ TC-BE-010 全部通过，单元测试覆盖率 ≥ 80%。
2. **状态机**: TC-SM-001 ~ TC-SM-004 全部通过，`StateMachineConfigTest` 无回归失败。
3. **前端**: TC-FE-001 ~ TC-FE-008 手工验证通过，无控制台报错。
4. **端到端**: TC-E2E-001 ~ TC-E2E-003 主流程跑通。
5. **兼容性**: 旧版前端调用 `reedit` 无 body 时行为一致，无 4xx/5xx 错误。

---

## 六、风险与应对措施

| 风险 | 影响 | 应对措施 |
|------|------|----------|
| `reedit` 接收空 JSON `{}` 时与 `null` 行为不一致 | 中 | 已在 Controller 使用 `required = false`，前端不传 body 时 Spring 注入 `null`；前端传入 `{}` 时 Service 层会遍历字段（均为 null），不会误覆盖数据 |
| 前端缓存导致编辑页显示旧数据 | 低 | 编辑保存成功后刷新 `currentApproval`，路由跳转触发重新获取 |
| 并发编辑导致数据覆盖 | 低 | 当前系统无乐观锁，属于已知限制；如需解决应独立引入 `@Version` 字段 |

---

*本文档为工单编辑功能重构（方案4）的配套测试方案，与 `edit-function-evaluation-plan.md` 配套使用。* 