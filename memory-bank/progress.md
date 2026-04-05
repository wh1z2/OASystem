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

---

## 待完成阶段

- [x] 阶段一：开发环境验证 ✅
- [x] 阶段二：数据库设计与初始化 ✅
- [x] 阶段三：后端基础框架搭建 ✅
- [x] 阶段四：用户认证模块实现 ✅
- [x] 阶段五：COLA状态机集成 ✅
- [x] 阶段六：审批流程核心功能 ✅
- [ ] 阶段七：前端接口对接
- [ ] 阶段八：表单设计器实现
- [ ] 阶段九：用户与角色管理
- [ ] 阶段十：系统测试与优化
- [ ] 阶段十一：部署上线

---

*最后更新: 2026-04-05 (阶段六已完成：审批流程核心功能实现完成，包含工单CRUD、状态流转、4级权限层级、代审批、审批人权限校验、OpenAPI测试文档，冒烟测试通过)*
