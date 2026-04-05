# 工单流程自动化系统 - 架构洞察

**文档用途**: 记录系统架构设计决策和文件作用说明

---

## 1. 项目文档结构

### 1.1 核心文档说明

| 文件 | 作用 | 关键内容 |
|------|------|----------|
| `product-design-document.md` | 产品设计文档 | 需求定义、业务流程、界面原型、数据字典 |
| `tech-stack.md` | 技术架构文档 | 技术选型、架构设计、接口规范、安全架构 |
| `implementation-plan.md` | 实施计划 | 11个阶段的详细实施步骤和验证标准 |
| `progress.md` | 进度记录 | 各阶段完成情况、环境信息、测试结果 |
| `architecture.md` | 架构洞察 | 本文件，记录架构决策和文件作用 |

### 1.2 文档引用关系
```
product-design-document.md (需求基准)
            ↓
tech-stack.md (技术实现方案)
            ↓
implementation-plan.md (分阶段实施)
            ↓
progress.md (进度跟踪)
architecture.md (架构解释)
```

---

## 2. 关键技术决策

### 2.1 状态机设计 (COLA StateMachine)

**决策**: 使用阿里巴巴 COLA 状态机作为工作流引擎

**理由**:
- 轻量级，适合毕业设计规模
- 学习曲线平缓，概念清晰（State/Event/Transition/Action）
- 代码即配置，版本控制友好

**状态定义** (来源: product-design-document.md v1.1):
```
状态编码 (TINYINT):
  0 = DRAFT(草稿)
  1 = PROCESSING(审批中)
  2 = APPROVED(已通过)
  3 = RETURNED(已打回)
  4 = REVOKED(已撤销)

状态流转:
  DRAFT(0) → SUBMIT(0) → PROCESSING(1)
  PROCESSING(1) → APPROVE(1) → APPROVED(2)
  PROCESSING(1) → REJECT(2) → RETURNED(3)
  PROCESSING(1) → REVOKE(4) → DRAFT(0)
  APPROVED(2)/RETURNED(3) → REEDIT(3) → DRAFT(0)

事件编码 (TINYINT):
  0 = SUBMIT(提交)
  1 = APPROVE(审批同意)
  2 = REJECT(审批不同意)
  3 = REEDIT(重新编辑)
  4 = REVOKE(撤销)
```

### 2.2 数据库表前缀规范

**决策**: 统一前缀区分表类型

| 前缀 | 含义 | 示例 |
|------|------|------|
| `sys_` | 系统表（用户、角色、权限） | sys_user, sys_role |
| `oa_` | 业务表（审批、历史、表单） | oa_approval, oa_approval_history |

### 2.3 外键设计决策

**决策**: 使用逻辑外键，不创建物理外键约束

**理由**:
- 灵活性更高，便于后续数据维护和迁移
- 避免物理外键带来的级联删除风险
- 符合微服务拆分趋势（虽然本项目是单体应用）
- 由应用程序层保证数据一致性

**实现方式**:
- 表中保留外键字段（如 `role_id`, `applicant_id`）
- 创建普通索引加速关联查询
- 在字段注释中标注关联关系
- Service层通过事务保证数据完整性

### 2.4 枚举存储方案

**决策**: 使用 TINYINT 存储枚举值，而非 VARCHAR 字符串

**理由**:
- 存储空间更小（1字节 vs 多字节字符串）
- 查询性能更好（整数比较 vs 字符串比较）
- 便于扩展，新增枚举值无需修改表结构
- 数据库层面保持紧凑，业务层面通过枚举类映射

**枚举映射表**:

| 枚举类型 | 存储类型 | 值映射 |
|---------|---------|--------|
| ApprovalStatus | TINYINT | 0=DRAFT, 1=PROCESSING, 2=APPROVED, 3=RETURNED, 4=REVOKED |
| ApprovalEvent | TINYINT | 0=SUBMIT, 1=APPROVE, 2=REJECT, 3=REEDIT, 4=REVOKE |
| ApprovalType | TINYINT | 1=LEAVE, 2=EXPENSE, 3=PURCHASE, 4=OVERTIME, 5=TRAVEL |
| Priority | TINYINT | 0=LOW, 1=NORMAL, 2=HIGH |

### 2.5 审批人逻辑

**决策**: 单人审批即流转

**说明**: 一个工单可配置多个审批人，但只要其中一人审批，工单状态即发生流转。暂不支持会签（多人同时审批通过才流转）。

### 2.6 权限系统设计 (4级权限层级)

**决策**: 实现4级审批权限层级，支持代审批场景

**权限层级定义**:
```
Level 1: 直接审批 (DIRECT)
  └─ 当前用户是被指定的审批人
  └─ 需要权限: approval:execute

Level 2: 管理员代审批 (PROXY_ADMIN)
  └─ 当前用户是系统管理员(role=admin)
  └─ 需要权限: approval:execute:all
  └─ 可审批全系统所有工单

Level 3: 部门经理代审批 (PROXY_MANAGER)
  └─ 当前用户是部门经理(role=manager)
  └─ 需要权限: approval:execute:dept
  └─ 只能审批本部门(dept_id相同)的工单

Level 4: 无权限 (DENIED)
  └─ 不满足以上任一条件
  └─ 操作被拒绝，返回权限不足提示
```

**设计理由**:
- 灵活性：支持正常审批流程和紧急情况下的代审批
- 可追溯：代审批操作在历史记录中明确标识，包含原审批人信息
- 安全性：部门经理只能代审批本部门工单，管理员可全系统代审批
- 审计合规：代审批场景记录审批类型和原因，满足审计要求

**相关枚举**:
| 审批类型 | 代码 | 说明 | 是否代审批 |
|---------|------|------|-----------|
| DIRECT | 1 | 直接审批 | 否 |
| PROXY_ADMIN | 2 | 管理员代审批 | 是 |
| PROXY_MANAGER | 3 | 部门经理代审批 | 是 |

### 2.7 审批人指定权限校验

**决策**: 在指定审批人时校验其权限，而非在执行审批时校验

**校验点**:
1. **创建工单时** (`ApprovalServiceImpl.create`): 校验 `currentApproverId` 是否有 `approval:execute` 权限
2. **更新工单时** (`ApprovalServiceImpl.update`): 如果更换审批人，校验新审批人权限
3. **状态机提交时** (`ApprovalStateMachineHelper.doSubmit`): 校验 `nextApproverId` 权限

**设计理由**:
- 前置校验：在指定环节就阻止无权限用户被指定，避免后续流程卡死
- 简化执行逻辑：执行审批时无需再次校验被指定人的权限
- 权限一致性：确保所有被指定为审批人的用户都有审批权限
- 业务合理性：普通员工(employee)不应被指定为审批人

**异常提示**: "指定的审批人无审批权限，请选择管理员或部门经理作为审批人"

### 2.8 数据语言规范

**决策**: 业务数据使用中文，技术术语使用英文

| 数据类型 | 语言 | 示例 |
|---------|------|------|
| 角色名称 | 中文 | 系统管理员、部门经理、普通员工 |
| 用户姓名 | 中文 | 张三、李四、王五 |
| 表单名称 | 中文 | 请假申请单、费用报销单 |
| 工单标题 | 中文 | 李四-病假申请、张三-差旅费报销 |
| 审批意见 | 中文 | 同意采购，请尽快下单 |
| 字段标签 | 中文 | 请假事由、开始日期、费用项目 |
| 代码标识 | 英文 | admin, manager, employee, LEAVE_FORM |
| 枚举值 | 数字 | 0=DRAFT, 1=PROCESSING |

---

## 3. 项目目录结构

### 3.1 整体结构
```
OASystem/
├── oa-frontend/          # Vue 3 前端应用
├── oa-backend/           # Spring Boot 后端应用
├── memory-bank/          # 项目文档
└── CLAUDE.md             # Claude Code 工作指导
```

### 3.2 后端目录规划 (阶段三创建)

```
oa-backend/
├── src/main/java/com/oasystem/
│   ├── OaSystemApplication.java      # 应用入口
│   ├── config/                        # 配置类
│   │   ├── StateMachineConfig.java   # COLA状态机配置
│   │   └── SecurityConfig.java       # Spring Security配置
│   ├── controller/                    # 控制器层 (API接口)
│   │   └── AuthController.java       # 认证控制器 (登录 / 用户信息)
│   ├── service/                       # 业务逻辑层
│   │   ├── AuthService.java          # 认证服务接口
│   │   └── impl/                      # 服务实现
│   │       └── AuthServiceImpl.java  # 认证服务实现
│   ├── mapper/                        # 数据访问层 (MyBatis-Plus)
│   │   ├── UserMapper.java           # 用户查询 (含 JOIN 角色)
│   │   └── RoleMapper.java           # 角色基础 CRUD
│   ├── entity/                        # 实体类 (数据库映射)
│   ├── dto/                           # 数据传输对象
│   │   ├── Result.java               # 统一响应封装
│   │   ├── LoginRequest.java         # 登录请求 DTO
│   │   ├── LoginResponse.java        # 登录响应 DTO
│   │   └── UserInfoResponse.java     # 当前用户详情 DTO
│   ├── enums/                         # 枚举类
│   │   ├── ApprovalStatus.java       # 审批状态枚举
│   │   └── ApprovalEvent.java        # 审批事件枚举
│   ├── security/                      # 安全认证相关
│   │   ├── JwtAuthenticationFilter.java  # JWT 请求过滤器
│   │   ├── UserDetailsImpl.java      # Spring Security 用户封装
│   │   └── UserDetailsServiceImpl.java   # 用户加载服务
│   ├── statemachine/                  # 状态机相关
│   │   ├── ApprovalStateMachineHelper.java  # 条件与动作
│   │   └── ApprovalContext.java      # 状态机上下文
│   └── util/                          # 工具类
│       └── JwtTokenUtil.java         # JWT 生成 / 解析 / 校验工具
├── src/main/resources/
│   ├── application.yml               # 主配置
│   ├── application-dev.yml           # 开发环境配置
│   └── mapper/                        # XML映射文件
├── src/test/java/                     # 测试代码
│   └── com/oasystem/util/
│       └── JwtTokenUtilTest.java     # JWT 工具单元测试
├── docs/api-test/                     # API 测试文档
│   └── auth-api-tests.openapi.yaml   # 认证模块 Apifox 测试用例
└── pom.xml                            # Maven配置
```

### 3.3 目录职责说明

| 目录 | 职责 | 设计原则 |
|------|------|----------|
| `controller/` | 接收HTTP请求，参数校验，返回响应 | 不包含业务逻辑 |
| `service/` | 业务逻辑处理，事务管理，状态机调用 | 复杂业务拆分私有方法 |
| `mapper/` | 数据库CRUD操作 | 使用MyBatis-Plus简化 |
| `entity/` | 与数据库表结构对应的POJO | 字段与表字段一一对应 |
| `dto/` | 接口请求/响应的数据对象 | 与entity分离，按需定义 |
| `enums/` | 状态、事件等枚举定义 | 包含中文描述字段 |
| `config/` | 框架配置、第三方组件配置 | 按组件分文件 |
### 3.6 数据库文件 (阶段二已创建)

| 文件 | 路径 | 作用 |
|------|------|------|
| `init.sql` | `database/init.sql` | MySQL数据库初始化脚本，包含：<br>- 数据库创建（utf8mb4字符集）<br>- 5张核心表的DDL<br>- 基础数据插入（角色、用户、表单模板、示例工单） |

**数据库表清单**:

| 表名 | 类型 | 记录数 | 说明 |
|------|------|--------|------|
| `sys_role` | 系统表 | 3 | 预定义角色：系统管理员、部门经理、普通员工 |
| `sys_user` | 系统表 | 5 | 测试用户：admin、张经理、李四、张三、王五 |
| `oa_approval` | 业务表 | 5 | 示例审批工单，状态使用TINYINT枚举存储 |
| `oa_approval_history` | 业务表 | 2 | 审批历史记录 |
| `oa_form_template` | 业务表 | 5 | 表单模板：请假、报销、采购、加班、出差 |

**执行方式**:
```bash
mysql -u root -p < database/init.sql
```

---

### 3.4 阶段四新增文件 (用户认证模块)

| 文件 | 路径 | 作用 |
|------|------|------|
| **Security 配置与过滤器** |||
| `SecurityConfig.java` | `oa-backend/config/SecurityConfig.java` | Spring Security 核心配置类。禁用 CSRF、设置 STATELESS 会话策略、放行 `/auth/**` 公开端点、其余请求需认证，并将 `JwtAuthenticationFilter` 注册到过滤器链中 |
| `JwtAuthenticationFilter.java` | `oa-backend/security/JwtAuthenticationFilter.java` | 继承 `OncePerRequestFilter`。从请求头 `Authorization: Bearer <token>` 中提取 JWT，调用 `JwtTokenUtil` 校验有效性，校验成功后构造 `UsernamePasswordAuthenticationToken` 并写入 `SecurityContextHolder`，使后续业务层可通过 `SecurityContext` 获取当前用户 |
| `UserDetailsImpl.java` | `oa-backend/security/UserDetailsImpl.java` | 实现 Spring Security 的 `UserDetails` 接口。封装 `User` 实体及从 `sys_role.permissions` 解析出的权限列表 (`GrantedAuthority`)，供框架进行认证和鉴权判断 |
| `UserDetailsServiceImpl.java` | `oa-backend/security/UserDetailsServiceImpl.java` | 实现 `UserDetailsService` 接口。通过 `UserMapper.selectByUsername` 加载用户（含角色 JOIN），解析角色表中的 `permissions` JSON 数组字段为权限字符串列表，最终包装为 `UserDetailsImpl` 返回 |
| **JWT 工具类** |||
| `JwtTokenUtil.java` | `oa-backend/util/JwtTokenUtil.java` | 基于 `io.jsonwebtoken` 的 JWT 工具类。提供 `generateToken(userId, username)` 生成令牌、`validateToken(token)` 校验有效性、`getUserIdFromToken` / `getUsernameFromToken` 提取声明、`getExpirationDate` 计算剩余毫秒数。密钥从 `jwt.secret` 读取，默认有效期 30 分钟 |
| **数据访问层 (Mapper)** |||
| `UserMapper.java` | `oa-backend/mapper/UserMapper.java` | 扩展 `BaseMapper<User>`。定义 `@Select` 注解方法 `selectByUsername`，通过 LEFT JOIN `sys_role` 一次性查询用户及其角色信息，用于登录时加载用户 |
| `RoleMapper.java` | `oa-backend/mapper/RoleMapper.java` | 扩展 `BaseMapper<Role>`，为角色数据提供基础 CRUD |
| **数据传输对象 (DTO)** |||
| `LoginRequest.java` | `oa-backend/dto/LoginRequest.java` | 登录请求 DTO。字段 `username`、`password` 均带 `@NotBlank` 校验 |
| `LoginResponse.java` | `oa-backend/dto/LoginResponse.java` | 登录成功响应 DTO。包含 `token`、`tokenType`(Bearer)、`expiresIn`(毫秒) 及嵌套的精简用户信息 (`LoginUserInfo`) |
| `UserInfoResponse.java` | `oa-backend/dto/UserInfoResponse.java` | 当前用户详情响应 DTO。返回 `id`、`username`、`name`、`email`、`phone`、`avatar`、`roleId`、`roleName`、`roleLabel`、`deptId`、`status`、`createTime` 等完整字段 |
| **业务逻辑层 (Service)** |||
| `AuthService.java` | `oa-backend/service/AuthService.java` | 认证服务接口。定义 `login(LoginRequest)` 和 `getCurrentUserInfo()` |
| `AuthServiceImpl.java` | `oa-backend/service/impl/AuthServiceImpl.java` | 认证服务实现。`login` 方法调用 `AuthenticationManager` 执行用户名密码认证，成功后生成 JWT 并组装 `LoginResponse`；`getCurrentUserInfo` 从 `SecurityContextHolder` 获取 `UserDetailsImpl` 并转换为 `UserInfoResponse` |
| **控制器层 (Controller)** |||
| `AuthController.java` | `oa-backend/controller/AuthController.java` | 认证控制器。提供 `POST /auth/login`（无需认证）和 `GET /auth/info`（需认证）两个端点，统一返回 `Result<T>` 格式 |
| **测试与文档** |||
| `auth-api-tests.openapi.yaml` | `oa-backend/docs/api-test/auth-api-tests.openapi.yaml` | OpenAPI 3.0.3 接口测试文档，含多个测试场景的 example。可直接导入 Apifox 生成测试用例 |
| `JwtTokenUtilTest.java` | `oa-backend/test/java/com/oasystem/util/JwtTokenUtilTest.java` | JWT 工具单元测试。覆盖正常 token 生成解析、短时效 token 过期验证、非法 token 拒绝、剩余时间计算等场景 |

### 3.5 阶段五新增文件 (COLA状态机集成)

| 文件 | 路径 | 作用 |
|------|------|------|
| **状态机配置** |||
| `StateMachineConfig.java` | `oa-backend/config/StateMachineConfig.java` | COLA 状态机核心配置类。定义6条状态流转规则（草稿→审批中→已通过/已打回→草稿），每条规则配置条件检查和动作执行。状态机ID为 `approvalStateMachine` |
| **状态机上下文与辅助类** |||
| `ApprovalContext.java` | `oa-backend/statemachine/ApprovalContext.java` | 状态机上下文类，封装状态转换所需数据：审批工单实体、操作命令、当前操作人ID |
| `ApprovalStateMachineHelper.java` | `oa-backend/statemachine/ApprovalStateMachineHelper.java` | 状态机条件和动作实现类。包含3个条件检查方法（表单完整性、审批权限、申请人身份）和5个动作方法（提交、通过、拒绝、重新编辑、撤销），每个动作自动记录审批历史 |
| **数据传输对象 (DTO)** |||
| `ApprovalActionCmd.java` | `oa-backend/dto/ApprovalActionCmd.java` | 审批操作命令 DTO。封装审批意见、下一审批人ID，用于审批操作接口请求参数 |
| **数据访问层 (Mapper)** |||
| `ApprovalMapper.java` | `oa-backend/mapper/ApprovalMapper.java` | 审批工单 Mapper。提供按状态、申请人、当前审批人查询，支持待办列表查询 |
| `ApprovalHistoryMapper.java` | `oa-backend/mapper/ApprovalHistoryMapper.java` | 审批历史 Mapper。提供按工单ID、审批人查询，支持已办列表查询 |
| **单元测试** |||
| `StateMachineConfigTest.java` | `oa-backend/test/config/StateMachineConfigTest.java` | 状态机集成测试（13个测试用例）。验证6条正常流转规则和6条非法/权限不足场景 |
| `ApprovalStateMachineTest.java` | `oa-backend/test/statemachine/ApprovalStateMachineTest.java` | 状态机辅助类单元测试（12个测试用例）。验证3个条件检查和5个动作执行 |

### 3.6 阶段六新增文件 (审批流程核心功能)

| 文件 | 路径 | 作用 |
|------|------|------|
| **业务逻辑层 (Service)** |||
| `ApprovalService.java` | `oa-backend/service/ApprovalService.java` | 审批服务接口。定义工单CRUD（创建、更新、删除、查询）、状态流转（提交、审批通过/拒绝、撤销、重新编辑）、列表查询（待办、已办、我的申请）、审批历史查询 |
| `ApprovalServiceImpl.java` | `oa-backend/service/impl/ApprovalServiceImpl.java` | 审批服务实现。集成COLA状态机执行状态流转，实现审批人权限校验（创建/更新时校验指定审批人），封装审批历史DTO转换，支持分页查询 |
| **控制器层 (Controller)** |||
| `ApprovalController.java` | `oa-backend/controller/ApprovalController.java` | 审批控制器。提供14个REST端点：工单CRUD、状态流转操作（提交、审批、拒绝、撤销、重新编辑）、列表查询（待办、已办、我的申请）、审批历史查询。所有接口统一返回 `Result<T>` 格式 |
| **数据传输对象 (DTO)** |||
| `ApprovalCreateRequest.java` | `oa-backend/dto/ApprovalCreateRequest.java` | 创建工单请求DTO。包含标题、类型、优先级、内容、表单数据、指定审批人ID |
| `ApprovalUpdateRequest.java` | `oa-backend/dto/ApprovalUpdateRequest.java` | 更新工单请求DTO。仅允许更新草稿状态工单，包含可修改字段 |
| `ApprovalQuery.java` | `oa-backend/dto/ApprovalQuery.java` | 工单查询条件DTO。继承分页参数，支持按标题、类型、状态、申请人筛选 |
| `ApprovalDetailResponse.java` | `oa-backend/dto/ApprovalDetailResponse.java` | 工单详情响应DTO。包含完整工单信息及枚举字段的中文名称映射（typeName、statusName、priorityName） |
| `ApprovalHistoryResponse.java` | `oa-backend/dto/ApprovalHistoryResponse.java` | 审批历史响应DTO。包含操作人信息、操作类型、审批意见、时间，以及代审批相关信息（isProxy、approvalType、originalApproverId、proxyReason） |
| `PageResult.java` | `oa-backend/dto/PageResult.java` | 统一分页响应封装。包含当前页数据列表、总记录数、当前页码、每页大小 |
| **权限系统优化** |||
| `ApprovalPermissionResult.java` | `oa-backend/statemachine/ApprovalPermissionResult.java` | 权限检查结果封装类。记录权限是否通过、审批类型（DIRECT/PROXY_ADMIN/PROXY_MANAGER）、原审批人ID（代审批场景）、提示消息。支持代审批场景的数据传递 |
| `ApprovalActionType.java` | `oa-backend/enums/ApprovalActionType.java` | 审批类型枚举。定义三种审批方式：DIRECT(直接审批)、PROXY_ADMIN(管理员代审批)、PROXY_MANAGER(部门经理代审批)，用于审计追踪 |
| **测试文档** |||
| `approval-api-tests.openapi.yaml` | `oa-backend/docs/api-test/approval-api-tests.openapi.yaml` | 审批模块OpenAPI测试文档（v1.1.0）。包含14个接口的详细定义、4级权限层级说明、代审批场景示例、多级权限测试用例，可直接导入Apifox |

### 3.6 后端基础文件 (阶段三创建)

| 文件 | 路径 | 作用 |
|------|------|------|
| **实体类 (Entity)** |||
| `User.java` | `oa-backend/entity/User.java` | 用户实体，映射 sys_user 表，包含用户登录信息、角色关联、状态等 |
| `Role.java` | `oa-backend/entity/Role.java` | 角色实体，映射 sys_role 表，定义系统角色和权限配置(JSON) |
| `Approval.java` | `oa-backend/entity/Approval.java` | 审批工单实体，映射 oa_approval 表，核心业务流程数据 |
| `ApprovalHistory.java` | `oa-backend/entity/ApprovalHistory.java` | 审批历史实体，映射 oa_approval_history 表，记录每次状态变更 |
| `FormTemplate.java` | `oa-backend/entity/FormTemplate.java` | 表单模板实体，映射 oa_form_template 表，存储表单设计器配置 |
| **枚举类 (Enum)** |||
| `ApprovalStatus.java` | `oa-backend/enums/ApprovalStatus.java` | 审批状态枚举：0草稿/1审批中/2已通过/3已打回/4已撤销 |
| `ApprovalEvent.java` | `oa-backend/enums/ApprovalEvent.java` | 审批事件枚举：0提交/1同意/2拒绝/3重新编辑/4撤销，用于状态机驱动 |
| `ApprovalType.java` | `oa-backend/enums/ApprovalType.java` | 审批类型枚举：1请假/2报销/3采购/4加班/5出差 |
| `Priority.java` | `oa-backend/enums/Priority.java` | 优先级枚举：0低/1普通/2高 |
| **数据传输对象 (DTO)** |||
| `Result.java` | `oa-backend/dto/Result.java` | 统一响应封装类，所有API返回此格式：{code, message, data, timestamp} |
| **异常处理** |||
| `BusinessException.java` | `oa-backend/exception/BusinessException.java` | 业务异常基类，用于区分业务错误和系统错误，支持自定义错误码 |
| `GlobalExceptionHandler.java` | `oa-backend/exception/GlobalExceptionHandler.java` | 全局异常处理器(@RestControllerAdvice)，统一捕获并处理各类异常，返回标准Result |
| **配置文件** |||
| `application.yml` | `oa-backend/resources/application.yml` | 主配置文件，包含服务器端口、Jackson、日志、JWT配置 |
| `application-dev.yml` | `oa-backend/resources/application-dev.yml` | 开发环境配置，包含MySQL连接、Hikari连接池、MyBatis-Plus配置 |
| `pom.xml` | `oa-backend/pom.xml` | Maven构建配置，定义依赖版本和构建插件 |

### 3.7 实体类详细说明

#### User 实体类
```
sys_user 表的Java映射
- 核心字段: id, username, password(BCrypt加密), name, email, phone
- 关联字段: role_id, dept_id
- 状态字段: status(0禁用/1启用)
- 审计字段: create_time, update_time
```

#### Role 实体类
```
sys_role 表的Java映射
- 核心字段: id, name(角色标识), label(显示名), description
- 权限字段: permissions(JSON数组格式存储权限编码)
- 审计字段: create_time
```

#### Approval 实体类
```
oa_approval 表的Java映射 - 核心业务实体
- 核心字段: id, title(审批标题), type(审批类型枚举), content(申请内容)
- 流程字段: applicant_id(申请人), current_approver_id(当前审批人), status(状态枚举)
- 扩展字段: priority(优先级), form_data(JSON格式存储动态表单数据)
- 审计字段: create_time, update_time
```

#### ApprovalHistory 实体类
```
oa_approval_history 表的Java映射 - 审计追踪
- 核心字段: id, approval_id(关联工单)
- 操作字段: approver_id(操作人), action(操作类型枚举), comment(审批意见)
- 审计字段: create_time(操作时间)
- 用途: 记录工单的完整生命周期，用于审计和流程追溯
```

#### FormTemplate 实体类
```
oa_form_template 表的Java映射 - 表单设计器存储
- 核心字段: id, name(表单名称), code(唯一编码), description
- 配置字段: fields_config(JSON存储字段定义数组), flow_config(流程配置)
- 状态字段: status(0禁用/1启用)
- 审计字段: create_time
- 用途: 支持可视化表单设计器的保存和加载
```

---

## 4. 关键技术点

### 4.1 统一响应格式

所有API返回统一格式的JSON:
```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1711456789000
}
```

### 4.2 认证方案

**阶段四已落地实现**：基于 Spring Security 6.x + JWT (jjwt 0.12.3) 的无状态认证架构。

| 组件 | 实现类 | 作用 |
|------|--------|------|
| 安全框架 | Spring Security 6.x | 负责请求拦截、认证鉴权、会话管理 |
| Token 机制 | JWT (jjwt 0.12.3) | 用户登录成功后颁发 JWT，后续请求携带该令牌进行身份校验 |
| 过滤器 | `JwtAuthenticationFilter` | 在每个请求到达 Controller 前，从 `Authorization: Bearer <token>` 中提取并校验 JWT，将认证信息写入 `SecurityContextHolder` |
| 用户加载 | `UserDetailsServiceImpl` | 通过 `UserMapper.selectByUsername` 查询用户（JOIN 角色表），将 `sys_role.permissions` JSON 数组解析为权限列表，封装为 `UserDetailsImpl` |
| 安全配置 | `SecurityConfig` | 禁用 CSRF、设置 STATELESS 会话策略、放行 `/auth/**`（登录接口），其余端点需认证 |

**Token 规范**:
- 算法: HS256
- 默认有效期: 30 分钟（由 `jwt.expiration` 配置，单位毫秒）
- 请求头格式: `Authorization: Bearer <token>`
- 载荷声明: `userId`, `username`, `sub`(userId), `iat`, `exp`

**认证流程**:
1. 客户端 `POST /auth/login` 提交用户名密码
2. `AuthService` 调用 `AuthenticationManager` 进行用户名密码校验
3. 校验成功后，`JwtTokenUtil` 生成 JWT
4. 客户端在后续请求头中携带 `Authorization: Bearer <token>`
5. `JwtAuthenticationFilter` 校验 token 有效性，将用户及权限信息注入 `SecurityContext`
6. 控制器/业务层通过 `SecurityContextHolder` 获取当前登录用户

### 4.3 状态机设计 (COLA StateMachine 5.0)

**阶段五已落地实现**：基于阿里巴巴 COLA 状态机 5.0 的审批流程引擎。

| 组件 | 实现类 | 作用 |
|------|--------|------|
| 状态机配置 | `StateMachineConfig` | 定义状态流转规则，构建状态机 Bean |
| 状态定义 | `ApprovalStatus` 枚举 | 5种状态：DRAFT/PROCESSING/APPROVED/RETURNED/REVOKED |
| 事件定义 | `ApprovalEvent` 枚举 | 5种事件：SUBMIT/APPROVE/REJECT/REEDIT/REVOKE |
| 上下文 | `ApprovalContext` | 传递业务数据：工单实体、操作命令、操作人ID |
| 条件检查 | `ApprovalStateMachineHelper` | 检查表单完整性、审批权限、申请人身份 |
| 动作执行 | `ApprovalStateMachineHelper` | 执行状态变更、更新审批人、记录历史 |

**状态流转图**:
```
DRAFT --SUBMIT--> PROCESSING --APPROVE--> APPROVED
                          --REJECT--> RETURNED
                          --REVOKE--> DRAFT

APPROVED --REEDIT--> DRAFT
RETURNED --REEDIT--> DRAFT
```

**状态机特点**:
- 条件不满足时返回源状态（COLA 5.0 行为）
- 每个合法转换自动记录审批历史
- 审批通过/拒绝/撤销时清空当前审批人

**使用示例**:
```java
@Autowired
private StateMachine<ApprovalStatus, ApprovalEvent, ApprovalContext> stateMachine;

// 执行状态转换
ApprovalStatus newStatus = stateMachine.fireEvent(
    currentStatus,           // 当前状态
    ApprovalEvent.APPROVE,   // 触发事件
    new ApprovalContext(approval, cmd, operatorId)  // 上下文
);

if (newStatus == currentStatus) {
    // 转换失败（条件不满足或非法流转）
    throw new BusinessException("当前状态不允许执行该操作");
}
```

### 4.4 状态机上下文

```java
@Data
@AllArgsConstructor
public class ApprovalContext {
    private Approval approval;        // 审批工单实体
    private ApprovalActionCmd cmd;    // 操作命令
    private Long operatorId;          // 当前操作人ID
}
```

### 4.5 权限系统设计 (阶段六实现)

**4级权限层级架构**:

```
┌─────────────────────────────────────────────────────────────┐
│                      审批权限检查流程                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Step 1: 是否是被指定的审批人?                                │
│     ├─ 是 → 有 approval:execute 权限?                        │
│     │         ├─ 是 → ✅ Level 1: 直接审批 (DIRECT)           │
│     │         └─ 否 → ❌ 拒绝："您没有审批权限"                │
│     └─ 否 → 继续 Step 2                                      │
│                                                             │
│  Step 2: 是否是管理员(role=admin)?                            │
│     ├─ 是 → 有 approval:execute:all 权限?                    │
│     │         ├─ 是 → ✅ Level 2: 管理员代审批 (PROXY_ADMIN)   │
│     │         └─ 否 → 继续 Step 3                            │
│     └─ 否 → 继续 Step 3                                      │
│                                                             │
│  Step 3: 是否是部门经理(role=manager)?                        │
│     ├─ 是 → 有 approval:execute:dept 权限?                   │
│     │         ├─ 是 → 申请人和经理同部门?                     │
│     │         │         ├─ 是 → ✅ Level 3: 经理代审批         │
│     │         │         │              (PROXY_MANAGER)        │
│     │         │         └─ 否 → ❌ 拒绝："只能审批本部门工单"   │
│     │         └─ 否 → 继续 Step 4                            │
│     └─ 否 → 继续 Step 4                                      │
│                                                             │
│  Step 4: ❌ Level 4: 无权限 (DENIED)                          │
│          "无权执行审批操作，您不是当前审批人"                   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

**核心实现类**:

| 组件 | 实现类 | 作用 |
|------|--------|------|
| 权限检查 | `ApprovalStateMachineHelper.checkApproverPermissionDetail()` | 执行4级权限检查，返回权限结果 |
| 结果封装 | `ApprovalPermissionResult` | 封装权限检查结果，包含审批类型、原审批人ID、提示消息 |
| 审批类型 | `ApprovalActionType` 枚举 | 定义 DIRECT(1)、PROXY_ADMIN(2)、PROXY_MANAGER(3) |
| 历史记录 | `ApprovalHistory` 实体扩展 | 新增 is_proxy、approval_type、original_approver_id 字段 |

**代审批审计追踪**:

当执行代审批时，系统会：
1. 在审批历史中记录 `is_proxy=1` 标识
2. 记录 `approval_type`（2=管理员代审批，3=部门经理代审批）
3. 记录 `original_approver_id`（原指定的审批人）
4. 在审批意见前追加标识，如 `"[管理员代审批] 同意采购"`
5. 输出审计日志：`[代审批审计] 工单ID=X, 代审批人ID=Y, 原审批人ID=Z...`

**审批人权限校验**:

在以下场景会校验被指定审批人的权限：
- 创建工单时 (`POST /approvals`): 校验 `currentApproverId` 是否有 `approval:execute` 权限
- 更新工单时 (`PUT /approvals/{id}`): 如果更换审批人，校验新审批人权限
- 提交工单时 (`POST /approvals/{id}/submit`): 如果指定 `nextApproverId`，校验其权限

校验失败抛出 `BusinessException`: "指定的审批人无审批权限，请选择管理员或部门经理作为审批人"

---

## 5. 开发规范

### 5.1 代码组织原则
- 单一职责：每个类只做一件事
- 文件大小：单个文件不超过500行，超过需拆分
- 命名规范：使用英文，避免拼音，遵循Java/Spring惯例

### 5.2 数据库规范
- 所有表使用 InnoDB 引擎
- 字符集统一使用 utf8mb4
- 时间字段使用 DATETIME，带默认 CURRENT_TIMESTAMP
- 逻辑删除使用 status 字段，不使用物理删除

---

*最后更新: 2026-04-05 (阶段六完成：审批流程核心功能已落地，包含工单CRUD、状态流转、4级权限层级、代审批支持、审批人权限校验、14个REST端点、OpenAPI测试文档)*
