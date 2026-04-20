# 工单编辑功能评估与重构方案

**编制日期**: 2026-04-20
**评估范围**: 后端 `update`、`reedit` 接口；前端编辑入口；COLA 状态机设计
**文档状态**: 待审批

---

## 一、现状诊断

### 1.1 当前后端接口能力

| 接口 | 路径 | 当前行为 | 允许的状态 |
|------|------|----------|-----------|
| 更新工单 | `POST /approvals/{id}/update` | 纯内容更新（标题、优先级、内容、表单数据、审批人） | **仅 DRAFT** |
| 重新编辑 | `POST /approvals/{id}/reedit` | 状态流转（APPROVED/RETURNED → DRAFT）+ 记录审批历史 | **APPROVED、RETURNED** |
| 提交 | `POST /approvals/{id}/submit` | 状态流转（DRAFT → PROCESSING） | **仅 DRAFT** |

### 1.2 关键发现

- `update` 接口**已经具备**草稿状态工单的编辑能力，权限校验完整（仅申请人可编辑自己草稿）。
- `reedit` 接口**已经同时支持** APPROVED 和 RETURNED 两种状态的重新编辑（见 `StateMachineConfig.java:87-100`），通过 COLA 状态机驱动状态回退至 DRAFT 并自动记录历史。
- 前端 `ApprovalDetail.vue` 中，草稿状态仅展示**"提交审批"**按钮，**未提供"编辑"入口**，这是导致"申请人无法对草稿状态的工单进行编辑"这一感知的直接原因。

---

## 二、方案评估

### 方案1：新增独立编辑接口（草稿专用）

**设计**: 保留现有 `update` 语义，将其路径规范化为 `PUT /approvals/{id}`，专门负责 DRAFT 状态的内容编辑。`reedit` 保持现状不变。

| 维度 | 评估 |
|------|------|
| 语义清晰度 | 高。`update` = 内容更新，`reedit` = 状态回退，职责边界清晰。 |
| 状态机影响 | 无。COLA 状态机无需任何改动，流转规则保持纯净。 |
| 前端复杂度 | 中。前端需维护两个入口：草稿状态调 `update`，已通过/已打回调 `reedit`。 |
| 业务灵活性 | 高。APPROVED 工单仍可重新编辑，不损失现有能力。 |
| 代码改动量 | 小。后端仅需调整 Controller 路径注解（可选）；前端需补充编辑按钮和表单。 |

**结论**: 技术风险低，架构合理，但并未解决用户希望"统一编辑入口"的诉求。

---

### 方案2：复用现有 `reedit` 接口，扩展支持 DRAFT 状态

**设计**: 修改 `reedit` 接口，使其同时处理 DRAFT、APPROVED、RETURNED 三种状态。DRAFT 状态时只更新内容不改变状态；APPROVED/RETURNED 时保持现有回退逻辑。

| 维度 | 评估 |
|------|------|
| 语义清晰度 | **低**。对草稿状态调用 `reedit`（Re-edit，重新编辑）严重违背命名语义——草稿本就处于可编辑状态，不存在"重新"。 |
| 状态机影响 | **高**。需在 COLA 状态机中为 DRAFT 状态增加 `REEDIT` 事件的处理，只能设计成 DRAFT → DRAFT 的自循环，这与 COLA 状态机"外部流转"的设计哲学冲突。 |
| 前端复杂度 | 低。前端只需要一个"编辑"按钮，统一调 `reedit`。 |
| 业务灵活性 | 高。APPROVED 仍可重新编辑。 |
| 代码改动量 | 中。需修改状态机配置、Service 层逻辑、增加分支判断。 |

**结论**: 为降低前端复杂度而牺牲后端语义和状态机纯净度，得不偿失。`reedit` 是一个**状态流转事件**，不应被滥用为通用编辑接口。

---

### 方案3（用户倾向）：重构 `reedit`，支持 DRAFT 和 RETURNED，排除 APPROVED

**设计**: 去掉 APPROVED → DRAFT 的重新编辑能力，仅保留 RETURNED → DRAFT；同时让 `reedit` 支持 DRAFT 状态的直接内容编辑。

| 维度 | 评估 |
|------|------|
| 语义清晰度 | **低**。与方案2相同，对草稿调用 `reedit` 语义不通。 |
| 状态机影响 | **高**。与方案2相同，需破坏状态机规则增加 DRAFT 自循环；同时**删除** APPROVED → DRAFT 的合法流转。 |
| 前端复杂度 | 低。统一入口。 |
| 业务灵活性 | **严重受损**。审批通过的工单一旦发现问题，**只能重新创建**，无法在原工单上修正后重新提交。这在实际 OA 场景中属于明显的功能倒退。 |
| 代码改动量 | 中。需删除状态机规则、新增 DRAFT 处理分支、调整 Service 逻辑。 |
| 接口冗余 | 现有 `update` 接口被架空，系统同时存在两个"编辑"接口但只有一个被使用，造成维护负担。 |

**关键风险**: 排除 APPROVED 的重新编辑，意味着已通过工单的 `form_data` 和 `content` 一旦需要修正，用户必须：
1. 重新填写一份全新工单；
2. 丢失原工单的历史审批记录关联性；
3. 产生数据冗余。

这与产品文档 v1.1 中定义的状态流转矩阵直接冲突（矩阵明确包含 APPROVED → REEDIT → DRAFT）。

**结论**: 该方案在业务合理性和架构语义上均存在明显缺陷，**不推荐作为首选方案**。

---

### 推荐方案（方案4）：后端保持接口分离 + 前端补充编辑入口 + `reedit` 可选携带内容参数

**设计**:
1. **后端架构保持现状**：`update` 负责 DRAFT 编辑，`reedit` 负责 APPROVED/RETURNED → DRAFT 流转。这是当前最合理的职责划分。
2. **后端微小增强**：允许 `reedit` 接口**可选地**接收 `ApprovalUpdateRequest` 请求体。当工单从 APPROVED/RETURNED 回退到 DRAFT 时，可**同时**更新内容，避免用户先 reedit 再 update 的两次操作。
3. **前端补充**：在 `ApprovalDetail.vue` 和 `ApprovalManage.vue` 中为草稿状态工单增加"编辑"按钮/操作，调用 `update` 接口。

| 维度 | 评估 |
|------|------|
| 语义清晰度 | 高。`update` = 编辑内容，`reedit` = 重新打开已结案工单（含可选的内容修正）。 |
| 状态机影响 | 无。COLA 状态机无需改动，保持现有 6 条流转规则。 |
| 前端复杂度 | 中。需区分两个入口，但语义明确，易于理解。 |
| 业务灵活性 | 高。保留 APPROVED 重新编辑能力，同时减少用户操作步骤。 |
| 代码改动量 | 小。后端 `reedit` 增加可选参数 + 前端补充编辑入口。 |

---

## 三、实施计划（推荐方案4）

### Phase 1：后端增强 `reedit` 接口（支持可选内容参数）

**目标**: 让重新编辑时可以一步到位更新内容，无需二次请求。

**涉及文件**:
- `oa-backend/src/main/java/com/oasystem/controller/ApprovalController.java`
- `oa-backend/src/main/java/com/oasystem/service/ApprovalService.java`
- `oa-backend/src/main/java/com/oasystem/service/impl/ApprovalServiceImpl.java`
- `oa-backend/src/main/java/com/oasystem/statemachine/ApprovalStateMachineHelper.java`

**具体改动**:
1. `ApprovalController.reedit` 方法增加 `@RequestBody(required = false) ApprovalUpdateRequest request` 参数。
2. `ApprovalService.reedit` 接口签名同步增加 `ApprovalUpdateRequest request` 参数。
3. `ApprovalServiceImpl.reedit` 中，状态机流转成功后，如果 `request != null`，将请求体中的字段（title、priority、content、formData、currentApproverId）回写到 `approval` 实体，再执行 `updateById`。
4. `ApprovalStateMachineHelper.doReedit` 中保持现有逻辑（只变状态、记录历史），**不**处理内容更新——内容更新由 Service 层在状态流转后统一处理，保持状态机动作的原子性。

**兼容性**: 完全向后兼容。`request` 为 `null` 时行为与现在完全一致。

### Phase 2：前端补充草稿状态编辑入口

**目标**: 解决用户"草稿无法编辑"的实际痛点。

**涉及文件**:
- `oa-frontend/src/views/ApprovalDetail.vue`
- `oa-frontend/src/views/ApprovalManage.vue`
- `oa-frontend/src/stores/approval.js`（可能需要补充弹窗/编辑表单逻辑）

**具体改动**:
1. `ApprovalDetail.vue`：在 `approval.status === 'draft' && canSubmit` 的分支中，增加**"编辑内容"**按钮，点击后弹出编辑表单（可复用 `ApprovalCreate.vue` 的表单组件或跳转至编辑页）。
2. `ApprovalManage.vue`：在列表操作列中，为 `status === 'draft'` 的行增加**"编辑"**操作，允许快捷进入编辑状态。
3. 编辑完成后调用 `updateApproval` Store 方法，成功后刷新详情/列表。

### Phase 3：测试覆盖

**涉及文件**:
- `oa-backend/src/test/java/com/oasystem/service/ApprovalServiceTest.java`
- `oa-backend/src/test/java/com/oasystem/config/StateMachineConfigTest.java`

**新增测试用例**:
1. `reedit` 不带请求体：APPROVED → DRAFT，内容不变。
2. `reedit` 携带请求体：RETURNED → DRAFT，内容同步更新。
3. `update` 编辑草稿：内容正确更新，状态保持 DRAFT。
4. `update` 编辑非草稿：抛出 BusinessException。
5. 状态机验证：确认 APPROVED/RETURNED → DRAFT 流转仍然有效（防止误删）。

---

## 四、备选实施计划（若坚持采用方案3）

如果经审批后决定采用方案3（重构 `reedit` 仅支持 DRAFT 和 RETURNED），实施计划如下：

### Step 1：状态机规则调整
- 删除 `StateMachineConfig.java` 中 `APPROVED --REEDIT--> DRAFT` 的流转规则（第86-93行）。
- 新增 `DRAFT --REEDIT--> DRAFT` 的内部/自循环规则（COLA 状态机需使用 `internalTransition` 或外部流转至自身）。
- 更新状态机配置注释和日志输出。

### Step 2：Service 层逻辑重构
- `ApprovalServiceImpl.reedit`：
  - 如果当前状态为 DRAFT：跳过状态机，直接调用 `updateById` 更新内容。
  - 如果当前状态为 RETURNED：保持现有状态机流转逻辑，流转成功后可选更新内容。
  - 如果当前状态为 APPROVED：返回 `BusinessException("当前状态不允许重新编辑")`。

### Step 3：前端适配
- `ApprovalDetail.vue`：已通过状态工单不再显示"重新编辑"按钮。
- `ApprovalManage.vue`：已通过状态行移除"重新编辑"操作。

### Step 4：文档与测试同步
- 更新 `product-design-document.md` 状态流转矩阵，移除 APPROVED → DRAFT 路径。
- 更新 `architecture.md` 状态机说明。
- 更新 `ApprovalStateMachineTest` 和 `StateMachineConfigTest`，移除 APPROVED + REEDIT 的合法用例，新增 DRAFT + REEDIT 用例。

**风险提醒**: 执行此方案后，已通过工单的历史数据如果需要修正，系统将不提供任何回退机制，用户只能通过创建新工单解决。

---

## 五、影响范围与兼容性

| 影响项 | 推荐方案4 | 方案3 |
|--------|-----------|-------|
| 数据库 Schema | 无影响 | 无影响 |
| 现有 API 兼容性 | `reedit` 增加可选参数，100% 兼容 | `reedit` 行为变更，不兼容（APPROVED 无法再 reedit） |
| 前端接口调用 | `reedit` 现有调用无需修改 | 需审查所有前端 `reedit` 调用点，移除已通过工单的入口 |
| 产品文档一致性 | 与 v1.1 文档完全一致 | 需修改 v1.1 状态流转图和矩阵 |
| 历史数据 | 无影响 | 无影响，但已批准的工单失去修正能力 |
| COLA 状态机 | 无影响 | 需删除/新增规则，破坏既有配置 |

---

## 六、审批意见

| 审批项 | 选项 |
|--------|------|
| 采纳方案 | ☑ 推荐方案4（保持后端分离 + 前端补充入口 + reedit 可选带参数）<br>□ 方案3（重构 reedit 仅支持 DRAFT/RETURNED，排除 APPROVED）<br>□ 其他：__________ |
| 是否立即执行 | ☑ 是，按选定方案执行代码修改<br>□ 否，需进一步讨论 |
| 补充意见 | 已于 2026-04-20 完成全部三阶段实施，详见第七、八节。 |

---

---

## 七、实施记录（2026-04-20）

### Phase 1：后端增强 `reedit` 接口

**已完成**。`reedit` 接口现支持可选携带 `ApprovalUpdateRequest` 请求体，实现状态回退与内容更新的一步到位。

| 文件 | 改动摘要 |
|------|----------|
| `ApprovalController.java:115-121` | `reedit` 方法新增 `@RequestBody(required = false) ApprovalUpdateRequest request` 参数 |
| `ApprovalService.java:78-84` | `reedit` 接口签名增加 `ApprovalUpdateRequest request` 参数 |
| `ApprovalServiceImpl.java:433-454` | 状态机流转成功后，若 `request != null`，将 title/priority/content/formData/currentApproverId 回写至实体并更新 |

**兼容性**：`request` 为 `null` 时行为与修改前完全一致，100% 向后兼容。

### Phase 2：前端补充草稿状态编辑入口

**已完成**。草稿状态工单现在拥有明确的编辑入口。

| 文件 | 改动摘要 |
|------|----------|
| `router/index.js:51-55` | 新增 `/approval/edit/:id` 路由，复用 `ApprovalCreate.vue` |
| `ApprovalCreate.vue` | 支持编辑模式：动态标题、类型只读、数据回填、调用 `updateApproval` |
| `ApprovalDetail.vue:123-133` | 草稿状态操作区新增**"编辑内容"**按钮，跳转至编辑页 |
| `ApprovalManage.vue:90-96` | 列表操作列为草稿状态增加**"编辑"**按钮（仅申请人或管理员可见） |
| `stores/approval.js:141-149` | 修正 `updateApproval` 请求路径为 `POST /approvals/${id}/update`（匹配后端实际接口）；`reeditApproval` 支持传入可选参数 |

### Phase 3：测试覆盖

**已完成**。新增/更新测试用例，全部通过（`mvn test -Dtest=ApprovalServiceTest,StateMachineConfigTest`，exit code 0）。

| 文件 | 测试变动 |
|------|----------|
| `ApprovalServiceTest.java` | 更新 `testReeditApproval`、`testReeditByNonApplicant` 调用签名（增加 `null` 参数）；新增 `testReeditWithContent`（验证 RETURNED → DRAFT 且内容同步更新）；新增辅助方法 `createSubmitAndReject` |
| `StateMachineConfigTest.java` | 无改动——COLA 状态机规则未变更，现有用例已覆盖 APPROVED/RETURNED → DRAFT 流转 |

---

## 八、实施后架构速览

```
DRAFT ──update────► DRAFT      (纯内容编辑，状态不变)
DRAFT ──submit────► PROCESSING
PROCESSING ──approve──► APPROVED
PROCESSING ──reject───► RETURNED
PROCESSING ──revoke───► DRAFT
APPROVED ──reedit(+opt body)──► DRAFT
RETURNED ──reedit(+opt body)──► DRAFT
```

- `update` = 编辑草稿内容（专用）
- `reedit` = 重新打开已结案工单（状态回退），**可选地**同时修正内容
- COLA 状态机保持现有 6 条流转规则，未引入任何自循环或非法流转

---

*本文档由 Claude Code 基于当前代码库实际状态评估生成，评估基准：后端 `oa-backend/src/main/java/com/oasystem` 全量源码、前端 `oa-frontend/src/views/ApprovalDetail.vue` 及 `stores/approval.js`、`memory-bank/product-design-document.md` v1.1。*
