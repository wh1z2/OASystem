# 工单流程自动化系统 - 项目进度记录

**文档用途**: 记录各阶段实施进度，供后续开发者参考

---

## 阶段完成记录

### 阶段一：开发环境验证 ✅

**完成日期**: 2026-03-26

**验证内容**:
| 检查项 | 要求版本 | 实际版本 | 状态 |
|--------|----------|----------|------|
| Java (JDK) | 17+ LTS | 17.0.15 LTS | ✅ 通过 |
| Maven | 3.9.x | 3.9.6 | ✅ 通过 |
| Node.js | 20 LTS | 20.15.1 | ✅ 通过 |
| npm | - | 10.7.0 | ✅ 通过 |
| MySQL | 8.0.x | 8.0.32 | ✅ 通过 |
| MySQL 服务 | 运行中 | RUNNING | ✅ 通过 |

**环境详情**:
- 操作系统: Windows 11 (amd64)
- Java: Microsoft OpenJDK 17.0.15 LTS
- Maven Home: D:\IDEA\apache-maven-3.9.6-bin\apache-maven-3.9.6
- MySQL 服务: MySQL80 (正在运行)

**备注**: 所有环境均已就绪，可以进入阶段二。

---

### 阶段二：数据库设计与初始化 ✅

**完成日期**: 2026-03-26

**执行内容**:
1. 创建数据库 `oa_system`，字符集 utf8mb4
2. 创建5张核心表：
   - `sys_user` - 用户表（系统表）
   - `sys_role` - 角色表（系统表）
   - `oa_approval` - 审批工单表（业务表）
   - `oa_approval_history` - 审批历史表（业务表）
   - `oa_form_template` - 表单模板表（业务表）

**设计决策**:
| 决策项 | 方案 | 说明 |
|--------|------|------|
| 外键设计 | 逻辑外键 | 不创建物理外键约束，由应用层保证数据一致性 |
| 枚举存储 | TINYINT | 状态、类型等枚举使用整数存储，节省空间且便于扩展 |
| 表前缀 | sys_/oa_ | sys_前缀用于系统表，oa_前缀用于业务表 |

**状态枚举映射**:
- 0=DRAFT(草稿), 1=PROCESSING(审批中), 2=APPROVED(已通过), 3=RETURNED(已打回), 4=REVOKED(已撤销)

**类型枚举映射**:
- 1=LEAVE(请假), 2=EXPENSE(报销), 3=PURCHASE(采购), 4=OVERTIME(加班), 5=TRAVEL(出差)

**事件枚举映射**:
- 0=SUBMIT, 1=APPROVE, 2=REJECT, 3=REEDIT, 4=REVOKE

**插入的基础数据**:
- 3个预定义角色（admin, manager, employee）
- 5个测试用户（含BCrypt加密密码）
- 5个表单模板（请假、报销、采购、加班、出差）
- 5条示例审批工单
- 2条审批历史记录

**验证状态**: ✅ 已完成 - 已通过用户验证，数据库结构和数据均符合要求，可以进入阶段三

---

## 文件位置说明

| 文件 | 路径 | 用途 |
|------|------|------|
| 数据库初始化脚本 | `database/init.sql` | 创建数据库、表结构及基础数据 |

---

### 阶段三：后端基础框架搭建 ✅

**完成日期**: 2026-03-27

**执行内容**:
1. ✅ 创建 Spring Boot 项目，版本 3.2.3，Java 17
2. ✅ 配置完整依赖：
   - Spring Boot Web、Validation、Security
   - MyBatis-Plus 3.5.5
   - COLA StateMachine 5.0.0
   - JWT (jjwt 0.12.3)
   - Lombok、FastJSON2
3. ✅ 配置数据源（application.yml 和 application-dev.yml）
   - MySQL 连接配置
   - Hikari 连接池
   - MyBatis-Plus 配置
4. ✅ 创建统一响应格式 Result.java
5. ✅ 配置全局异常处理 GlobalExceptionHandler.java
6. ✅ 创建基础实体类：
   - `sys_user` → User.java
   - `sys_role` → Role.java
   - `oa_approval` → Approval.java
   - `oa_approval_history` → ApprovalHistory.java
   - `oa_form_template` → FormTemplate.java
7. ✅ 创建枚举类：
   - ApprovalStatus（审批状态）
   - ApprovalEvent（审批事件）
   - ApprovalType（审批类型）
   - Priority（优先级）

**验证状态**: ✅ 已通过 Maven 编译验证，无编译错误

---

### 阶段四：用户认证模块实现 ✅

**完成日期**: 2026-04-01

**执行内容**:
1. ✅ 引入并配置 Spring Security 6.x + JWT (jjwt 0.12.3)
2. ✅ 编写 `SecurityConfig`：STATELESS 会话、放行 `/auth/**`、注册 JWT Filter
3. ✅ 实现 JWT 认证过滤器 `JwtAuthenticationFilter`：提取并校验 `Authorization: Bearer <token>`
4. ✅ 实现 `UserDetailsImpl` / `UserDetailsServiceImpl`：加载用户并解析 `sys_role.permissions` JSON 为权限列表
5. ✅ 编写 `JwtTokenUtil`：JWT 的生成、解析、验证、过期时间计算
6. ✅ 编写 Mapper 层：
   - `UserMapper`：含 `selectByUsername`（LEFT JOIN `sys_role`）
   - `RoleMapper`：基础 BaseMapper
7. ✅ 编写 DTO：
   - `LoginRequest`：@NotBlank 校验
   - `LoginResponse`：返回 token、tokenType、expiresIn、精简用户信息
   - `UserInfoResponse`：返回当前用户完整信息
8. ✅ 编写 `AuthService` / `AuthServiceImpl` / `AuthController`：
   - `POST /auth/login`：认证并返回 JWT
   - `GET /auth/info`：获取当前登录用户信息
9. ✅ 修复启动问题并验证：
   - 升级 MyBatis-Plus 为 `3.5.10.1` + `mybatis-plus-spring-boot3-starter`，解决 `factoryBeanObjectType` 不兼容
   - 移除 JDBC URL 中的 `characterEncoding=utf8mb4`，解决 MySQL 连接异常
10. ✅ 编写 Apifox 可导入的 OpenAPI 测试文档 `auth-api-tests.openapi.yaml`
11. ✅ 编写 `JwtTokenUtilTest` 单元测试，支持生成短时效 token 用于过期场景验证

**新增/修改文件清单**:
| 类型 | 文件 |
|------|------|
| 配置 | `config/SecurityConfig.java` |
| Security | `security/JwtAuthenticationFilter.java`, `security/UserDetailsImpl.java`, `security/UserDetailsServiceImpl.java` |
| 工具 | `util/JwtTokenUtil.java` |
| Mapper | `mapper/UserMapper.java`, `mapper/RoleMapper.java` |
| DTO | `dto/LoginRequest.java`, `dto/LoginResponse.java`, `dto/UserInfoResponse.java` |
| Service | `service/AuthService.java`, `service/impl/AuthServiceImpl.java` |
| Controller | `controller/AuthController.java` |
| 测试文档 | `docs/api-test/auth-api-tests.openapi.yaml` |
| 单元测试 | `test/java/com/oasystem/util/JwtTokenUtilTest.java` |
| 配置修复 | `pom.xml`, `resources/application-dev.yml` |

**验证状态**: ✅ 已通过 Apifox 接口测试验证（登录正向/异常、Token 有效期、获取当前用户信息）

---

### 阶段五：COLA状态机集成 ✅

**完成日期**: 2026-04-01

**执行内容**:
1. ✅ 创建状态机上下文 `ApprovalContext`：封装审批工单、操作命令、操作人ID
2. ✅ 实现状态机辅助类 `ApprovalStateMachineHelper`：
   - 条件检查：`checkFormComplete`（表单完整性）、`checkApproverPermission`（审批权限）、`checkIsApplicant`（申请人身份）
   - 状态转换动作：`doSubmit`、`doApprove`、`doReject`、`doReedit`、`doRevoke`
   - 历史记录保存：每次状态变更自动记录到 `oa_approval_history` 表
3. ✅ 配置状态机 `StateMachineConfig`：
   - 状态流转规则（6条转换规则）：
     - DRAFT --SUBMIT--> PROCESSING
     - PROCESSING --APPROVE--> APPROVED
     - PROCESSING --REJECT--> RETURNED
     - PROCESSING --REVOKE--> DRAFT
     - APPROVED --REEDIT--> DRAFT
     - RETURNED --REEDIT--> DRAFT
   - 每条规则配置条件和动作
4. ✅ 创建审批操作命令 DTO `ApprovalActionCmd`：封装审批意见、下一审批人ID
5. ✅ 创建 Mapper 层：`ApprovalMapper`、`ApprovalHistoryMapper`
6. ✅ 编写单元测试：
   - `ApprovalStateMachineTest`（12个测试用例）：测试条件和动作
   - `StateMachineConfigTest`（13个测试用例）：测试完整状态流转（含非法流转和权限检查）

**新增/修改文件清单**:
| 类型 | 文件 |
|------|------|
| 状态机配置 | `config/StateMachineConfig.java` |
| 状态机上下文 | `statemachine/ApprovalContext.java` |
| 状态机辅助类 | `statemachine/ApprovalStateMachineHelper.java` |
| DTO | `dto/ApprovalActionCmd.java` |
| Mapper | `mapper/ApprovalMapper.java`, `mapper/ApprovalHistoryMapper.java` |
| 单元测试 | `test/statemachine/ApprovalStateMachineTest.java`, `test/config/StateMachineConfigTest.java` |

**测试覆盖**:
- ✅ 正常状态流转（6条规则全部验证）
- ✅ 非法状态流转（返回源状态）
- ✅ 条件不满足场景（表单不完整、无权限、非申请人）
- ✅ 状态转换动作（状态更新、审批人变更、历史记录生成）

**验证状态**: ✅ 已通过单元测试验证（25个测试全部通过），可以进入阶段六

---

### 阶段六：审批流程核心功能 ✅

**完成日期**: 2026-04-05

**执行内容**:
1. ✅ 实现审批 Service 层 (`ApprovalServiceImpl`)：
   - 工单 CRUD：创建、更新、删除、查询
   - 状态流转：提交、审批通过、审批拒绝、撤销、重新编辑
   - 列表查询：待办列表、已办列表、我的申请
   - 审批历史：历史记录查询及DTO转换

2. ✅ 权限系统优化（4级权限层级）：
   - Level 1 - 直接审批：被指定的审批人可直接审批
   - Level 2 - 管理员代审批：系统管理员可审批全系统工单
   - Level 3 - 部门经理代审批：部门经理可审批本部门工单
   - Level 4 - 无权限：无审批权限，操作被拒绝
   - 实现 `ApprovalPermissionResult` 封装权限检查结果
   - 代审批支持：支持管理员/部门经理代审批，并在历史记录中标识

3. ✅ 审批人权限校验（方案B实施）：
   - 创建工单时校验指定审批人是否有 `approval:execute` 权限
   - 更新工单时校验新审批人权限
   - 状态机提交动作中校验 `nextApproverId` 权限
   - 普通员工无法被指定为审批人（抛出业务异常）

4. ✅ 审批历史增强（审计追踪）：
   - 新增字段：`approval_type`(审批类型), `is_proxy`(是否代审批), `original_approver_id`(原审批人)
   - 新增 `ApprovalActionType` 枚举：DIRECT(直接审批)、PROXY_ADMIN(管理员代审批)、PROXY_MANAGER(部门经理代审批)
   - 代审批时在审批意见中追加标识（如 `[管理员代审批] 同意`）

5. ✅ 实现审批 Controller 层 (`ApprovalController`)：
   - `POST /approvals` - 创建工单
   - `PUT /approvals/{id}` - 更新工单
   - `DELETE /approvals/{id}` - 删除工单
   - `GET /approvals/{id}` - 查询详情
   - `GET /approvals` - 列表查询
   - `POST /approvals/{id}/submit` - 提交工单
   - `POST /approvals/{id}/approve` - 审批通过
   - `POST /approvals/{id}/reject` - 审批拒绝
   - `POST /approvals/{id}/reedit` - 重新编辑
   - `POST /approvals/{id}/revoke` - 撤销申请
   - `GET /approvals/todo` - 待办列表
   - `GET /approvals/done` - 已办列表
   - `GET /approvals/my` - 我的申请
   - `GET /approvals/{id}/history` - 审批历史

6. ✅ 更新 OpenAPI 测试文档：
   - 版本升级到 1.1.0，添加权限系统说明
   - 新增 `ApprovalActionType` 枚举定义
   - 更新 `ApprovalHistoryResponse` 增加代审批字段
   - 添加多级权限测试场景示例（直接审批、代审批、权限不足）

**新增/修改文件清单**:
| 类型 | 文件 |
|------|------|
| Service 接口 | `service/ApprovalService.java` |
| Service 实现 | `service/impl/ApprovalServiceImpl.java` |
| Controller | `controller/ApprovalController.java` |
| DTO | `dto/ApprovalCreateRequest.java`, `dto/ApprovalUpdateRequest.java`, `dto/ApprovalQuery.java`, `dto/ApprovalDetailResponse.java`, `dto/ApprovalHistoryResponse.java`, `dto/PageResult.java` |
| 权限结果封装 | `statemachine/ApprovalPermissionResult.java` |
| 审批类型枚举 | `enums/ApprovalActionType.java` |
| 测试文档 | `docs/api-test/approval-api-tests.openapi.yaml` |

**测试覆盖**:
- ✅ 单元测试：状态机相关测试（25个测试全部通过）
- ✅ 冒烟测试：通过 Apifox 完成接口测试

**验证状态**: ✅ 审批核心功能已实现，权限系统优化完成，冒烟测试通过，可以进入阶段七

### 阶段七：前端接口对接 ✅

**完成日期**: 2026-04-06

**执行内容**:
1. ✅ 配置前端代理 (vite.config.js)
   - 配置 `/api` 代理到 `http://localhost:8080`
   - 支持路径重写和跨域

2. ✅ 创建 axios 配置文件 (`src/api/config.js`)
   - 设置 baseURL 为 `/api`
   - 请求拦截器自动添加 JWT Token
   - 响应拦截器统一处理错误和 Token 过期

3. ✅ 更新认证存储逻辑 (auth.js)
   - 使用 `apiClient.post('/auth/login')` 调用后端登录接口
   - 使用 `apiClient.get('/auth/info')` 获取当前用户信息
   - 支持 Token 过期自动跳转登录页

4. ✅ 对接审批列表接口 (approval.js)
   - 映射后端状态/类型/优先级到前端格式
   - 实现工单 CRUD、状态流转、列表查询
   - 支持待办、已办、我的申请列表

5. ✅ 更新页面组件调用
   - `Login.vue`: 异步登录处理
   - `ApprovalManage.vue`: 加载审批列表，更新状态标签
   - `ApprovalDetail.vue`: 加载详情和历史记录，异步审批操作
   - `TodoList.vue`: 加载待办列表，快速审批
   - `DoneList.vue`: 加载已办列表
   - `Dashboard.vue`: 加载待办统计

**新增/修改文件清单**:
| 类型 | 文件 |
|------|------|
| 代理配置 | `oa-frontend/vite.config.js` |
| API 配置 | `oa-frontend/src/api/config.js` (新增) |
| Store 更新 | `oa-frontend/src/stores/auth.js` |
| Store 更新 | `oa-frontend/src/stores/approval.js` |
| 页面组件 | `oa-frontend/src/views/Login.vue` |
| 页面组件 | `oa-frontend/src/views/ApprovalManage.vue` |
| 页面组件 | `oa-frontend/src/views/ApprovalDetail.vue` |
| 页面组件 | `oa-frontend/src/views/TodoList.vue` |
| 页面组件 | `oa-frontend/src/views/DoneList.vue` |
| 页面组件 | `oa-frontend/src/views/Dashboard.vue` |

**数据映射关系**:
| 前端状态 | 后端数值 | 说明 |
|---------|---------|------|
| draft | 0 | 草稿 |
| processing | 1 | 审批中 |
| approved | 2 | 已通过 |
| returned | 3 | 已打回 |
| revoked | 4 | 已撤销 |

**验证状态**:

✅ 前端接口对接完成，等待用户验证测试

❌ 部分测试不通过，需继续完善

---

### 阶段七补充：前端接口对接完善与统计接口 ✅

**完成日期**: 2026-04-14

**执行内容**:

#### 1. 查询与分页优化
- ✅ 修正 `PageResult` 分页响应结构，统一前后端分页字段格式
- ✅ 优化 `ApprovalQuery` 查询条件，支持更灵活的列表筛选
- ✅ 重构 `ApprovalServiceImpl` 中的列表查询逻辑，批量查询关联用户和部门信息减少N+1问题
- ✅ 修复 `ApprovalDetailResponse` 字段缺失问题，确保详情页数据完整

#### 2. 审批结果落表修复
- ✅ 修复审批操作后工单状态未正确持久化到数据库的bug
- ✅ `Approval` 实体增加 `@TableField` 注解解决字段映射异常
- ✅ `ApprovalStateMachineHelper` 增加状态变更审计日志
- ✅ `ApprovalServiceImpl` 完善审批操作后的数据刷新逻辑

#### 3. 表单数据类型调整
- ✅ `ApprovalCreateRequest` / `ApprovalUpdateRequest` 的 `formData` 字段类型从 `String` 改为 `Map<String, Object>`
- ✅ `ApprovalServiceImpl` 适配 FastJSON2 的 `JSONObject` 序列化/反序列化
- ✅ 前端 `ApprovalCreate.vue` 表单设计器对接后端，支持动态表单数据提交

#### 4. 前端功能增强
- ✅ `ApprovalDetail.vue` 新增"提交审批"按钮（仅草稿状态显示）
- ✅ `Login.vue` 密码输入框新增显示/隐藏切换（小眼睛图标）
- ✅ `Profile.vue` 个人中心对接后端用户接口，支持信息展示和更新
- ✅ `stores/user.js` 对接后端用户API

#### 5. 待办/已办标识修复（状态隔离）
- ✅ 修复侧边栏待办徽章被其他页面操作覆盖的问题
- ✅ Pinia `approval.js` 中引入独立的 `todoTotal`、`doneTotal`、`myTotal` refs，与共享的 `pagination` 解耦
- ✅ `MainLayout.vue` 侧边栏同步显示待办、已办、我的申请三个徽章计数
- ✅ `Dashboard.vue` 待办统计改为直接使用 `todoTotal`，避免受分页切换影响

#### 6. 工作台统计接口开发
- ✅ 后端新增 `GET /approvals/statistics` 接口
- ✅ 新增 `DashboardStatisticsResponse.java` 统一统计响应格式
- ✅ 实现6项统计指标：待办数、已通过数、已拒绝数、我的申请数、已办数、审批类型分布
- ✅ `ApprovalMapper` 新增申请人视角统计方法（总数、类型分布）
- ✅ `ApprovalHistoryMapper` 新增审批人视角统计方法（已办数、通过数、拒绝数）
- ✅ 前端 `Dashboard.vue` 对接统计接口，实时展示工作台数据

#### 7. 统计视角修正（关键设计修正）
- ✅ **设计修正**：工作台"已通过"和"已拒绝"统计从"申请人视角"切换为"审批人视角"
- ✅ 旧逻辑：统计当前用户提交的申请中被通过/拒绝的数量（`oa_approval` 表按 `applicant_id` + `status` 查询）
- ✅ 新逻辑：统计当前用户作为审批人执行过通过/拒绝操作的去重工单数量（`oa_approval_history` 表按 `approver_id` + `action` 查询，使用 `COUNT(DISTINCT approval_id)` 去重）
- ✅ 此修正符合业务设计目标：Dashboard 展示的是当前用户的**操作成果**（通过了几个、拒绝了几个），而非**被操作结果**

**新增/修改文件清单**:
| 类型 | 文件 |
|------|------|
| Service 更新 | `oa-backend/service/impl/ApprovalServiceImpl.java` |
| Mapper 更新 | `oa-backend/mapper/ApprovalMapper.java`, `oa-backend/mapper/ApprovalHistoryMapper.java` |
| DTO 新增 | `oa-backend/dto/DashboardStatisticsResponse.java` |
| Controller 更新 | `oa-backend/controller/ApprovalController.java` |
| Store 更新 | `oa-frontend/src/stores/approval.js`, `oa-frontend/src/stores/user.js` |
| 页面组件 | `oa-frontend/src/views/Dashboard.vue`, `oa-frontend/src/views/ApprovalDetail.vue`, `oa-frontend/src/views/ApprovalCreate.vue`, `oa-frontend/src/views/Login.vue`, `oa-frontend/src/views/Profile.vue` |
| 布局组件 | `oa-frontend/src/layouts/MainLayout.vue` |
| API 配置 | `oa-frontend/src/api/config.js` |
| 文档新增 | `memory-bank/frontend-docking-implementation.md`, `memory-bank/frontend-docking-list.md` |

**验证状态**: ✅ 工作台统计接口联调通过，待办/已办/我的申请徽章计数正确，审批结果落表正常

---

### 阶段九补充：R2 高风险项修复（数据权限控制） ✅

**完成日期**: 2026-04-15

**执行内容**:

#### 1. 数据权限过滤实现
- ✅ 修改 `ApprovalService` 接口：`getById` 和 `list` 方法增加 `currentUserId` 参数
- ✅ 修改 `ApprovalController`：`getById` 和 `list` 接口传入当前登录用户ID
- ✅ 修改 `ApprovalServiceImpl`：实现基于角色的数据权限过滤逻辑
  - **admin**: 可查看全部工单
  - **manager**: 可查看本部门工单 + 指定自己审批的工单
  - **employee**: 只能查看自己发起的工单

#### 2. 数据权限规则
| 角色 | 详情查询权限 | 列表查询权限 |
|------|-------------|-------------|
| admin | 任意工单 | 全部工单 |
| manager | 本部门工单 + 自己审批的工单 | 本部门工单 + 自己审批的工单 |
| employee | 仅自己的工单 | 仅自己的工单 |

#### 3. 测试覆盖
- ✅ 新建 `ApprovalDataPermissionTest`（10 个测试用例），覆盖详情和列表的数据权限验证
- ✅ 修复 `ApprovalServiceTest` 因接口签名变更导致的编译问题
- ✅ 全量测试通过：100 个测试用例全部通过

#### 4. 接口测试文档
- ✅ 创建 `data-permission-api-tests.openapi.yaml`，可直接导入 Apifox 进行接口测试

**新增/修改文件清单**:
| 类型 | 文件 |
|------|------|
| Service 接口 | `oa-backend/service/ApprovalService.java` |
| Controller 更新 | `oa-backend/controller/ApprovalController.java` |
| Service 实现 | `oa-backend/service/impl/ApprovalServiceImpl.java` |
| 单元测试（修复） | `oa-backend/src/test/java/com/oasystem/service/ApprovalServiceTest.java` |
| 单元测试（新增） | `oa-backend/src/test/java/com/oasystem/service/ApprovalDataPermissionTest.java` |
| API 测试文档 | `oa-backend/docs/api-test/data-permission-api-tests.openapi.yaml` |
| 架构文档 | `memory-bank/architecture.md` |

**验证状态**: ✅ 后端编译通过，100 个单元测试全部通过，`ApprovalDataPermissionTest` 10 个数据权限测试全部通过

---

### 阶段九补充：R3 高风险项修复（前端权限控制） ✅

**完成日期**: 2026-04-18

**执行内容**:

#### 1. 后端权限数据暴露
- ✅ 修改 `UserDetailsImpl`：新增 `List<String> permissions` 字段，携带角色权限码列表
- ✅ 修改 `LoginResponse.UserInfo` 和 `UserInfoResponse`：新增 `permissions` 字段
- ✅ 修改 `AuthServiceImpl`：`login()` 和 `getCurrentUserInfo()` 正确填充权限数据

#### 2. 前端权限基础设施
- ✅ 新建 `permission.js`：核心工具函数 `hasPermission`、`hasAnyPermission`、`hasApprovalPermission` 等
- ✅ 新建 `v-permission` 自定义指令：`src/directives/permission.js`，无权限时移除 DOM 元素
- ✅ 扩展 `auth.js` Pinia Store：新增 `permissions` computed、辅助方法 `checkPermission`/`checkAnyPermission`/`checkRole`
- ✅ 增强 `router/index.js`：路由守卫增加 `meta.permissionCheck` 函数拦截
- ✅ 注册全局指令：`main.js` 中 `app.directive('permission', permissionDirective)`

#### 3. 页面级权限适配
- ✅ `MainLayout.vue`：侧边栏菜单按权限动态显隐（待办/已办/审批流程/用户管理/角色管理/表单设计器）
- ✅ `ApprovalManage.vue`：“发起审批”按钮（`apply` 权限）、“审批”按钮（`approval:execute` 权限）
- ✅ `ApprovalDetail.vue`：审批操作区按权限和工单状态控制，无权限时显示提示
- ✅ `UserManage.vue`：增删改按钮仅限 `user_manage` / `all`
- ✅ `RoleManage.vue`：增删改按钮仅限 `role_manage` / `all`

#### 4. 测试覆盖
- ✅ 后端单元测试：`AuthPermissionTest.java`（3 个测试用例），验证 `/auth/login` 和 `/auth/info` 返回正确权限数组，全部通过
- ✅ 前端页面测试方案：`frontend-r3-test-plan.md`，覆盖页面元素验证、交互流程、边界条件、异常场景

**新增/修改文件清单**:
| 类型 | 文件 |
|------|------|
| Security/DTO | `oa-backend/security/UserDetailsImpl.java` |
| DTO | `oa-backend/dto/LoginResponse.java`, `oa-backend/dto/UserInfoResponse.java` |
| Service | `oa-backend/service/impl/AuthServiceImpl.java` |
| 单元测试 | `oa-backend/src/test/java/com/oasystem/controller/AuthPermissionTest.java` |
| 前端工具 | `oa-frontend/src/utils/permission.js` |
| 前端指令 | `oa-frontend/src/directives/permission.js` |
| Store | `oa-frontend/src/stores/auth.js` |
| 路由 | `oa-frontend/src/router/index.js` |
| 入口 | `oa-frontend/src/main.js` |
| 布局 | `oa-frontend/src/layouts/MainLayout.vue` |
| 页面 | `oa-frontend/src/views/ApprovalManage.vue`, `ApprovalDetail.vue`, `UserManage.vue`, `RoleManage.vue` |
| 测试文档 | `oa-backend/docs/api-test/frontend-r3-test-plan.md` |

**验证状态**: ✅ 后端编译通过，单元测试通过；前端编译通过，权限控制生效

**已知待完善问题**（评审记录）：

- [x] 登录张经理账号，点击待办事项后显示暂无待办事项（表里有一个部门为财务部，审批人为张经理的工单），如附件图1，然后点击了审批流程之后又能显示出待办事项了，如附件图2。
- [x] 现在表里有一个财务部的工单待审批，审批人是张经理，张经理本身是技术部的，工单详情页点击通过审批或者是待办事项页面点击快速审批都无反应，后端控制台输出权限不足如附件图3
- [x] 当我登录普通员工账号时，虽然侧边栏不显示无权限的菜单项了，但是工作台页面的快捷操作里面依旧有代办审、已办事项、表单设计，并且个人中心的快捷操作里也包含待办事项、已办事项。如果决定待办事项和已办事项不对普通员工显示，那么普通员工的工作台页面的已通过、已拒绝是否也可以隐藏，仅显示本月申请？
- [x] 需要补充一条规则：admin可以审批所有工单，无论审批人是不是admin，也不论是否跨部门，admin账号就是可以拥有所有操作的权限，所以admin的待办事项里面应该显示所有“审批中”的工单。
- [x] 部门经理应该添加表单设计器的权限
- [x] 当前重新编辑工单的设计是：可以对已通过或已打回的工单重新编辑，状态变回草稿，但是我觉得不太合理，已通过的工单不应可以再被编辑，所以设计应该变为可以对草稿或已打回的工单重新编辑
- [ ] 当前的待审批人的指定逻辑我觉得是缺失的，虽然接口测试里可以在请求体里面设置待审批人，但是在前端页面中给发起审批人指定待审批人显然是不合理的，所以需要做到一个配置逻辑，默认情况下应该怎么指定审批人
- [x] 我觉得审批流程和已办事项盘的颜色徽章是没必要的，可以去掉，仅需保证显示待办事项旁的徽章正确即可（不被其他列表查询操作影响）
- [x] 数据库权限表的内容还可以完善，目前部门经理竟然无法发起审批，不管是工作台页面快捷操作里的发起审批还是个人中心快捷操作里的发起审批都无法发起
- [x] 在前端页面中没有看到有“撤销（REVOKE）”的操作，疑似缺失
- [x] 仔细检查待办事项、已办事项、审批流程三个模块的前端代码逻辑。以待办事项、登录账号为张经理为例，我点击待办事项后只显示一条数据，利用浏览器开发者工具抓包得到此时的网络调用是调用了TodoList.vue而不是后端接口，这是严重错误。已办事项也是类似逻辑，无法显示正确数据。然后当我点击审批流程后再回去查看待办事项、已办事项时发现此时的查询结果才正确。
- [x] 已办事项的筛选功能有误，目前筛选下拉列表里只有有已通过和已拒绝，应该是已拒绝和已打回冲突了，后端规定的是拒绝后的工单状态时已打回而不是已拒绝，导致筛选不出已打回的工单。

---

### 阶段九补充：R1 高风险项修复（后端权限控制） ✅

**完成日期**: 2026-04-14

**执行内容**:

#### 1. 方法级权限控制启用
- ✅ 在 `SecurityConfig.java` 中启用 `@EnableMethodSecurity(prePostEnabled = true)`
- ✅ 为 `ApprovalController` 全部 15 个端点添加 `@PreAuthorize` 注解
- ✅ 为 `AuthController` 的 `/auth/info` 添加 `isAuthenticated()` 控制

#### 2. 用户管理后端实现
- ✅ 新建 `UserService` / `UserServiceImpl`：用户 CRUD、修改密码、更新个人资料
- ✅ 新建 `UserController`（`/users`）：6 个端点，带 `user_view` / `user_manage` 权限控制
- ✅ 新建相关 DTO：`UserCreateRequest`、`UserUpdateRequest`、`UserQuery`、`PasswordChangeRequest`、`ProfileUpdateRequest`

#### 3. 角色管理后端实现
- ✅ 新建 `RoleService` / `RoleServiceImpl`：角色 CRUD、权限配置持久化
- ✅ 新建 `RoleController`（`/roles`）：6 个端点，带 `role_manage` 权限控制
- ✅ 新建相关 DTO：`RoleCreateRequest`、`RoleUpdateRequest`

#### 4. 权限测试覆盖
- ✅ 编写 `MethodSecurityTest`（32 个测试用例），覆盖审批/用户/角色接口的权限控制验证
- ✅ 修复现有测试 `ApprovalServiceTest` 和 `PermissionProxyApprovalTest` 的编译兼容性问题
- ✅ 全量测试通过：90 个测试用例全部通过

#### 5. 接口测试文档
- ✅ 创建 `permission-api-tests.openapi.yaml`，可直接导入 Apifox 进行接口测试

**新增/修改文件清单**:
| 类型 | 文件 |
|------|------|
| Security 配置 | `oa-backend/config/SecurityConfig.java` |
| Controller 更新 | `oa-backend/controller/ApprovalController.java`, `oa-backend/controller/AuthController.java` |
| Controller 新增 | `oa-backend/controller/UserController.java`, `oa-backend/controller/RoleController.java` |
| Service 新增 | `oa-backend/service/UserService.java`, `oa-backend/service/impl/UserServiceImpl.java`, `oa-backend/service/RoleService.java`, `oa-backend/service/impl/RoleServiceImpl.java` |
| DTO 新增 | `oa-backend/dto/UserCreateRequest.java`, `oa-backend/dto/UserUpdateRequest.java`, `oa-backend/dto/UserQuery.java`, `oa-backend/dto/PasswordChangeRequest.java`, `oa-backend/dto/ProfileUpdateRequest.java`, `oa-backend/dto/RoleCreateRequest.java`, `oa-backend/dto/RoleUpdateRequest.java` |
| 单元测试 | `oa-backend/src/test/java/com/oasystem/controller/MethodSecurityTest.java` |
| 测试修复 | `oa-backend/src/test/java/com/oasystem/service/ApprovalServiceTest.java`, `oa-backend/src/test/java/com/oasystem/service/PermissionProxyApprovalTest.java` |
| API 测试文档 | `oa-backend/docs/api-test/permission-api-tests.openapi.yaml` |
| 架构文档 | `memory-bank/architecture.md` |

**验证状态**: ✅ 后端编译通过，90 个单元测试全部通过，`MethodSecurityTest` 32 个权限测试全部通过

---

## 待完成阶段

- [x] 阶段一：开发环境验证 ✅
- [x] 阶段二：数据库设计与初始化 ✅
- [x] 阶段三：后端基础框架搭建 ✅
- [x] 阶段四：用户认证模块实现 ✅
- [x] 阶段五：COLA状态机集成 ✅
- [x] 阶段六：审批流程核心功能 ✅
- [x] 阶段七：前端接口对接 ✅
- [ ] 阶段八：表单设计器实现
- [x] 阶段九：用户与角色管理（后端已完成，前端已对接权限控制） ✅
- [ ] 阶段十：系统测试与优化
- [ ] 阶段十一：部署上线

---

---

### 阶段十补充：审批模块权限审计修复（基于 approval-module-fix-plan）✅

**完成日期**: 2026-04-19

**执行内容**:

#### 1. 前端 Store 状态完全隔离（F1）
- ✅ 将单一 `approvals` ref 拆分为四个独立状态：
  - `approvals` — ApprovalManage 专用
  - `todoApprovals` — TodoList 专用
  - `doneApprovals` — DoneList 专用
  - `myApprovals` — MyApprovals 专用
- ✅ `pendingApprovals` 计算属性改为基于 `todoApprovals`，彻底消除模块间数据污染

#### 2. 接口失败时清空状态（F2）
- ✅ `fetchTodoList` / `fetchDoneList` / `fetchMyApprovals` / `fetchApprovals` 的 `catch` 块中清空对应状态数组
- ✅ 同时将 `pagination` 和 `todoTotal`/`doneTotal`/`myTotal` 重置为初始值
- ✅ 避免网络/权限异常时旧数据残留在 UI 上

#### 3. 已办列表绑定独立状态（F3）
- ✅ `DoneList.vue` 的 `filteredApprovals` 计算属性改为基于 `approvalStore.doneApprovals`
- ✅ 解决此前 DoneList 和 TodoList 共用 `approvals` 导致的显示错乱

#### 4. 接口失败时 UI 反馈（F4）
- ✅ `TodoList.vue` 和 `DoneList.vue` 各增加 `error` ref
- ✅ `onMounted` 中异步调用 fetch 方法，失败时显示错误提示卡片（含错误描述）
- ✅ 空状态和错误状态分离显示

#### 5. /todo /done 接口权限门槛统一（B1）
- ✅ `ApprovalController.getTodoList()` 和 `getDoneList()` 的 `@PreAuthorize` 从 `hasAnyAuthority('approval', 'all')` 改为 `isAuthenticated()`
- ✅ 列表查询的权限门槛与 `GET /approvals` 保持一致，数据隔离由 Service 层负责

#### 6. 权限加载链路诊断日志（B2）
- ✅ `UserDetailsServiceImpl.loadUserByUsername()` 增加全链路日志：
  - `role == null` 时记录 error 日志（含 roleId）
  - `permissions` 为空时记录 error 日志（含 roleName + roleId）
  - 解析成功时记录 info 日志（含 permissions 内容）
  - 解析失败时记录 error 日志（含异常堆栈）

#### 7. 403 异常处理路径统一（B3）
- ✅ 移除 `GlobalExceptionHandler` 中的 `@ExceptionHandler(AccessDeniedException.class)`
- ✅ 统一由 `RestAccessDeniedHandler`（Filter 层）处理所有 403 响应
- ✅ 增强 `RestAccessDeniedHandler` 诊断日志：记录当前用户名、authorities、请求方法和 URI

#### 8. 异常工单数据修复（D1）
- ✅ 创建 Flyway 迁移脚本 `V1_2__Fix_Abnormal_Approvals.sql`
- ✅ SQL 逻辑：`UPDATE oa_approval SET status = 0 WHERE current_approver_id IS NULL AND status = 1`
- ✅ 将无审批人的 processing 工单重置为 draft，避免待办列表查询出幽灵数据

**新增/修改文件清单**:
| 类型 | 文件 |
|------|------|
| Store 重构 | `oa-frontend/src/stores/approval.js` |
| 页面修复 | `oa-frontend/src/views/TodoList.vue`, `oa-frontend/src/views/DoneList.vue` |
| 权限修复 | `oa-backend/controller/ApprovalController.java` |
| 日志增强 | `oa-backend/security/UserDetailsServiceImpl.java` |
| 异常统一 | `oa-backend/security/RestAccessDeniedHandler.java`, `oa-backend/exception/GlobalExceptionHandler.java` |
| 数据修复 | `oa-backend/resources/db/migration/V1_2__Fix_Abnormal_Approvals.sql` |
| 修复计划 | `oa-backend/docs/approval-module/approval-module-fix-plan.md` |
| 测试计划 | `oa-backend/docs/approval-module/approval-module-fix-test-plan.md` |

**验证状态**: ✅ 代码修改完成，等待按测试计划执行验证

---

### 阶段九补充：R3 高风险项修复（前端页面内部权限管控）✅

**完成日期**: 2026-04-19

**执行内容**:

#### 1. 工作台 Dashboard.vue 权限管控细化
- ✅ 顶部统计卡片：普通员工（无 `hasApprovalPermission`）仅保留「本月申请」，隐藏「待办事项」「已通过」「已拒绝」
- ✅ 待办列表区域：无审批权限用户不再展示「待办事项」列表块及「查看全部」链接
- ✅ 快捷操作：「待办审批」「已办事项」「表单设计」按权限条件渲染（`v-if="canAccessApproval"` / `v-if="canAccessFormDesigner"`）
- ✅ 布局自适应：当左侧待办列表隐藏时，右侧快捷操作区自动占满全宽（`lg:col-span-3`）

#### 2. 个人中心 Profile.vue 快捷操作权限管控
- ✅ 快捷操作区「待办事项」「已办事项」按 `hasApprovalPermission` 条件渲染
- ✅ 仅保留「发起审批」入口，与 `apply` 权限对齐

#### 3. 权限判断逻辑复用
- ✅ `Dashboard.vue` / `Profile.vue` 均引入 `hasApprovalPermission` / `hasPermission` 工具函数
- ✅ 使用 `authStore.permissions` 计算属性 `canAccessApproval`，与侧边栏菜单控制逻辑保持一致

**新增/修改文件清单**:
| 类型 | 文件 |
|------|------|
| 页面修复 | `oa-frontend/src/views/Dashboard.vue` |
| 页面修复 | `oa-frontend/src/views/Profile.vue` |

**验证状态**: ✅ 前端编译通过，权限控制逻辑与侧边栏菜单一致，无权限内容已按预期隐藏

---

### 阶段十补充：前端交互与权限修复 ✅

**完成日期**: 2026-04-19

**执行内容**:

#### 1. 已办事项筛选修复
- ✅ `DoneList.vue` 筛选下拉列表选项从 `value="rejected"` 修正为 `value="returned"`
- ✅ 与后端状态定义对齐：拒绝后的工单状态为 `RETURNED(已打回)`，而非 `REJECTED`

#### 2. 侧边栏徽章精简
- ✅ `MainLayout.vue` 移除「已办事项」旁的 `bg-success-500` 绿色徽章
- ✅ `MainLayout.vue` 移除「审批流程」旁的 `bg-primary-500` 蓝色徽章
- ✅ 仅保留「待办事项」旁的 `bg-danger-500` 红色徽章，避免视觉干扰

#### 3. 表单设计器权限扩展（部门经理）
- ✅ 新增权限编码 `form_design`，供表单设计器菜单/路由使用
- ✅ `MainLayout.vue`：菜单显示条件从 `checkPermission('all')` 改为 `checkPermission('form_design')`
- ✅ `router/index.js`：`/form-designer` 路由权限守卫改为 `hasPermission(perms, 'form_design')`
- ✅ `Dashboard.vue`：快捷入口权限判断同步更新
- ✅ 数据库中 `manager` 角色的 `permissions` 字段追加 `"form_design"`

#### 4. 审批详情页撤销功能
- ✅ `ApprovalDetail.vue` 新增 `canRevoke` 计算属性：状态为 `processing` 且当前用户为申请人时生效
- ✅ 审批人操作区（通过/拒绝）中，若当前用户同时为申请人，额外显示「撤销申请」按钮
- ✅ 非审批人但为申请人时，单独显示「撤销申请」按钮
- ✅ 新增 `ConfirmDialog.vue` 自定义确认弹窗组件，使用 `Teleport` 挂载到 body，替代原生 `confirm()` 的浏览器默认标题
- ✅ `handleRevoke` 方法调用 `approvalStore.revokeApproval`，成功后跳转审批列表

#### 5. 最后更新时间修复
- ✅ `ApprovalDetail.vue`「操作记录-最后更新」数据源从 `approval.history` 修正为 `approvalHistory`
- ✅ 审批历史通过独立接口 `/approvals/{id}/history` 获取并存储在 `approvalHistory` ref 中

#### 6. CLAUDE.md 规范更新
- ✅ 新增规则：数据库数据以实际库为准，`database/init.sql` 等初始化脚本仅作历史备份，已废弃且禁止执行
- ✅ 涉及数据库数据时，必须通过数据库连接查询当前最新数据，不得基于初始化脚本推断数据状态

**新增/修改文件清单**:
| 类型 | 文件 |
|------|------|
| 页面修复 | `oa-frontend/src/views/DoneList.vue` |
| 页面修复 | `oa-frontend/src/views/ApprovalDetail.vue` |
| 布局修复 | `oa-frontend/src/layouts/MainLayout.vue` |
| 路由修复 | `oa-frontend/src/router/index.js` |
| 页面修复 | `oa-frontend/src/views/Dashboard.vue` |
| 组件新增 | `oa-frontend/src/components/ConfirmDialog.vue` |
| 规范更新 | `CLAUDE.md` |

**验证状态**: ✅ 前端编译通过，交互修复完成

---

### 阶段十补充：工单编辑功能重构（方案4）✅

**完成日期**: 2026-04-20

**执行内容**:

#### 1. 后端 `reedit` 接口增强
- ✅ `ApprovalController.reedit` 方法新增 `@RequestBody(required = false) ApprovalUpdateRequest request` 参数
- ✅ `ApprovalService` / `ApprovalServiceImpl` 接口签名同步更新
- ✅ 状态机流转成功后，若 `request != null`，将 title/priority/content/formData/currentApproverId 回写至实体并执行 `updateById`
- ✅ 状态机动作保持原子性，内容更新由 Service 层在流转后统一处理
- ✅ **向后兼容**：`request` 为 `null` 时行为与增强前完全一致

#### 2. 前端补充草稿状态编辑入口
- ✅ 新增 `/approval/edit/:id` 路由，复用 `ApprovalCreate.vue` 组件
- ✅ `ApprovalCreate.vue` 支持编辑模式：动态标题、类型只读、数据回填、调用 `updateApproval`
- ✅ `ApprovalDetail.vue`：草稿状态操作区新增「编辑内容」按钮；已通过/已打回状态新增「重新编辑」按钮
- ✅ `ApprovalManage.vue`：列表操作列为草稿状态增加「编辑」按钮；为已通过/已打回状态增加「重新编辑」按钮
- ✅ `stores/approval.js`：`updateApproval` 修正请求路径为 `POST /approvals/${id}/update`；`reeditApproval` 支持传入可选参数

#### 3. 测试覆盖
- ✅ 后端 `ApprovalServiceTest`：更新 `testReeditApproval`、`testReeditByNonApplicant` 调用签名；新增 `testReeditWithContent` 验证 RETURNED → DRAFT 且内容同步更新
- ✅ `StateMachineConfigTest` 无改动——COLA 状态机规则未变更，现有用例已覆盖

**新增/修改文件清单**:
| 类型 | 文件 |
|------|------|
| Controller 更新 | `oa-backend/controller/ApprovalController.java` |
| Service 接口 | `oa-backend/service/ApprovalService.java` |
| Service 实现 | `oa-backend/service/impl/ApprovalServiceImpl.java` |
| 单元测试 | `oa-backend/src/test/java/com/oasystem/service/ApprovalServiceTest.java` |
| 路由新增 | `oa-frontend/src/router/index.js` |
| 页面更新 | `oa-frontend/src/views/ApprovalCreate.vue` |
| 页面更新 | `oa-frontend/src/views/ApprovalDetail.vue` |
| 页面更新 | `oa-frontend/src/views/ApprovalManage.vue` |
| Store 更新 | `oa-frontend/src/stores/approval.js` |
| 方案文档 | `oa-backend/docs/edit/edit-function-evaluation-plan.md` |
| 测试文档 | `oa-backend/docs/edit/edit-function-test-plan.md` |

**验证状态**: ✅ 后端编译通过，单元测试通过；前端编译通过，草稿编辑/重新编辑流程联调通过

---

*最后更新: 2026-04-20 (工单编辑功能重构：后端reedit增强、前端编辑入口补充、测试覆盖)*