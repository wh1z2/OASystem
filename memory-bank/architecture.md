# 工单流程自动化系统 - 架构洞察

**文档用途**: 记录系统架构设计决策和文件作用说明

---

## 项目文档结构

### 核心文档说明

| 文件 | 作用 | 关键内容 |
|------|------|----------|
| `product-design-document.md` | 产品设计文档 | 需求定义、业务流程、界面原型、数据字典 |
| `tech-stack.md` | 技术架构文档 | 技术选型、架构设计、接口规范、安全架构 |
| `implementation-plan.md` | 实施计划 | 11个阶段的详细实施步骤和验证标准 |
| `progress.md` | 进度记录 | 各阶段完成情况、环境信息、测试结果 |
| `architecture.md` | 架构洞察 | 本文件，记录架构决策和文件作用 |

### 文档引用关系
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

## 关键技术决策

### 工单编辑接口职责分离（方案4）

**决策**: 保持 `update` 与 `reedit` 接口分离，`reedit` 可选携带内容参数

**接口职责**:
| 接口 | 路径 | 职责 | 允许状态 |
|------|------|------|----------|
| `update` | `POST /approvals/{id}/update` | 纯内容编辑（标题、优先级、内容、表单数据、审批人） | **仅 DRAFT** |
| `reedit` | `POST /approvals/{id}/reedit` | 状态回退 + 可选内容更新 | **APPROVED、RETURNED** |

**设计理由**:
- `update` = 编辑草稿内容，状态不变，语义清晰
- `reedit` = 重新打开已结案工单（状态回退），可选地同时修正内容，避免先 reedit 再 update 的两次操作
- COLA 状态机无需任何改动，保持现有 6 条流转规则，未引入自循环或非法流转
- 向后兼容：`reedit` 的 `request` 参数为 `null` 时，行为与增强前完全一致

**前端编辑模式复用**:
- 新增 `/approval/edit/:id` 路由，复用 `ApprovalCreate.vue` 组件
- 通过 `route.params.id` 判断编辑模式，动态回填数据
- 编辑模式下审批类型字段只读（静态文本展示），其余字段可修改
- 草稿状态编辑调用 `update`；已通过/已打回状态调用 `reedit`

**相关文件**:
| 文件 | 作用 |
|------|------|
| `oa-backend/controller/ApprovalController.java` | `reedit` 方法增加可选 body 参数 |
| `oa-backend/service/impl/ApprovalServiceImpl.java` | 流转成功后回写请求体字段 |
| `oa-frontend/src/views/ApprovalCreate.vue` | 编辑模式：数据回填、类型只读、调用 update |
| `oa-frontend/src/views/ApprovalDetail.vue` | 草稿/已通过/已打回状态展示对应编辑按钮 |
| `oa-frontend/src/views/ApprovalManage.vue` | 列表操作列按状态展示编辑/重新编辑按钮 |

---

### 状态机设计 (COLA StateMachine)

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

### 数据库表前缀规范

**决策**: 统一前缀区分表类型

| 前缀 | 含义 | 示例 |
|------|------|------|
| `sys_` | 系统表（用户、角色、权限） | sys_user, sys_role |
| `oa_` | 业务表（审批、历史、表单） | oa_approval, oa_approval_history |

### 外键设计决策

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

### 枚举存储方案

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
| ApproverStrategyType | TINYINT | 1=DEPT_ROLE(按部门角色), 3=FIXED_USER(固定人员) |
| ApproverType | TINYINT | 1=SPECIFIC_USER(指定用户), 2=SPECIFIC_ROLE(指定角色) |

### 审批人逻辑

**决策**: 单人审批即流转

**说明**: 一个工单可配置多个审批人，但只要其中一人审批，工单状态即发生流转。暂不支持会签（多人同时审批通过才流转）。

### 权限系统设计 (4级权限层级)

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

### 审批人指定权限校验

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

### 默认审批人自动解析 (DefaultApproverResolver)

**决策**: 当创建/提交工单时未手动指定审批人，系统自动根据预设规则解析默认审批人

**解析优先级**:
```
1. 手动指定优先: 传入 currentApproverId → 校验权限 → 直接使用
2. 规则匹配: 未传入 → 查询 oa_approver_rule → 按 priority 排序匹配
3. 兜底策略: 无匹配规则 → 查找申请人所在部门的 manager 角色用户
4. 失败阻断: 兜底也失败 → 抛出 BusinessException，流程阻断
```

**v1.0 实现策略**:
| 策略 | 编码 | 说明 |
|------|------|------|
| DEPT_ROLE | 1 | 按部门+角色匹配，如技术部请假找部门经理 |
| FIXED_USER | 3 | 固定人员，如所有报销找财务张经理 |

**设计理由**:
- 零配置提交：普通员工无需关心具体审批人是谁
- 手动优先：保留管理员/特殊场景手动指定能力
- 实时生效：规则变更即时生效，无需重启
- 安全兜底：解析失败明确提示，禁止静默跳过
- 防止自审：解析出的审批人与申请人相同时自动跳过

**集成点**:
- `ApprovalServiceImpl.create()`: 若 currentApproverId 为空则调用解析引擎
- `ApprovalServiceImpl.submit()`: 若提交时仍无审批人则再次解析

**相关文件**:
| 文件 | 作用 |
|------|------|
| `oa-backend/resolver/DefaultApproverResolver.java` | 核心解析引擎 |
| `oa-backend/entity/ApproverRule.java` | 审批规则实体 |
| `oa-backend/service/ApproverRuleService.java` | 规则管理接口 |
| `oa-backend/controller/ApproverRuleController.java` | 规则管理 REST API |
| `oa-frontend/src/views/ApproverRuleManage.vue` | 规则配置页面 |
| `database/approver-rule-migration.sql` | 数据库迁移脚本 |

### 数据语言规范

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

## 项目目录结构

### 3.1 整体结构
```
OASystem/
├── oa-frontend/          # Vue 3 前端应用
├── oa-backend/           # Spring Boot 后端应用
├── memory-bank/          # 项目文档
└── CLAUDE.md             # Claude Code 工作指导
```

### 后端目录规划 (阶段三创建)

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

### 目录职责说明

| 目录 | 职责 | 设计原则 |
|------|------|----------|
| `controller/` | 接收HTTP请求，参数校验，返回响应 | 不包含业务逻辑 |
| `service/` | 业务逻辑处理，事务管理，状态机调用 | 复杂业务拆分私有方法 |
| `mapper/` | 数据库CRUD操作 | 使用MyBatis-Plus简化 |
| `entity/` | 与数据库表结构对应的POJO | 字段与表字段一一对应 |
| `dto/` | 接口请求/响应的数据对象 | 与entity分离，按需定义 |
| `enums/` | 状态、事件等枚举定义 | 包含中文描述字段 |
| `config/` | 框架配置、第三方组件配置 | 按组件分文件 |
### 数据库文件 (阶段二已创建)

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

### 阶段四新增文件 (用户认证模块)

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

### 阶段五新增文件 (COLA状态机集成)

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

### 阶段七新增文件 (前端接口对接)

| 文件 | 路径 | 作用 |
|------|------|------|
| **API 配置** |||
| `config.js` | `oa-frontend/src/api/config.js` | Axios 配置文件。创建 axios 实例，设置 baseURL 为 `/api`；请求拦截器自动从 localStorage 获取 token 并添加到 Authorization 头；响应拦截器统一处理后端响应格式，处理 401 Token 过期自动跳转登录页 |
| **Store 更新** |||
| `auth.js` (更新) | `oa-frontend/src/stores/auth.js` | 更新为异步方法。`login` 调用 `POST /auth/login` 获取 JWT；`fetchCurrentUser` 调用 `GET /auth/info` 获取用户信息；`initAuth` 初始化时验证 token 有效性 |
| `approval.js` (更新) | `oa-frontend/src/stores/approval.js` | 完整对接后端 14 个审批接口。包含状态/类型/优先级映射转换；实现工单 CRUD、状态流转、待办/已办/我的申请列表查询、审批历史查询 |
| **前端页面更新** |||
| `Login.vue` (更新) | `oa-frontend/src/views/Login.vue` | 更新登录处理为异步，调用 authStore.login 并等待结果 |
| `ApprovalManage.vue` (更新) | `oa-frontend/src/views/ApprovalManage.vue` | 添加 onMounted 加载审批列表；更新状态标签映射；异步审批操作并刷新列表 |
| `ApprovalDetail.vue` (更新) | `oa-frontend/src/views/ApprovalDetail.vue` | 添加 onMounted 加载详情和历史；使用 approvalHistory 计算属性；异步审批操作 |
| `TodoList.vue` (更新) | `oa-frontend/src/views/TodoList.vue` | 添加 onMounted 加载待办列表；异步快速审批/拒绝操作 |
| `DoneList.vue` (更新) | `oa-frontend/src/views/DoneList.vue` | 添加 onMounted 加载已办列表；更新状态标签 |
| `Dashboard.vue` (更新) | `oa-frontend/src/views/Dashboard.vue` | 添加 onMounted 加载待办列表 |
| **构建配置更新** |||
| `vite.config.js` (更新) | `oa-frontend/vite.config.js` | 添加代理配置，将 `/api` 请求转发到 `http://localhost:8080`，支持路径重写 |

### 阶段六新增文件 (审批流程核心功能)

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

### 后端基础文件 (阶段三创建)

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

### 实体类详细说明

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

## 关键技术点

### 统一响应格式

所有API返回统一格式的JSON:
```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1711456789000
}
```

### 认证方案

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

### 状态机设计 (COLA StateMachine 5.0)

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

### 状态机上下文

```java
@Data
@AllArgsConstructor
public class ApprovalContext {
    private Approval approval;        // 审批工单实体
    private ApprovalActionCmd cmd;    // 操作命令
    private Long operatorId;          // 当前操作人ID
}
```

### 权限系统设计 (阶段六实现)

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

## 开发规范

### 代码组织原则
- 单一职责：每个类只做一件事
- 文件大小：单个文件不超过500行，超过需拆分
- 命名规范：使用英文，避免拼音，遵循Java/Spring惯例

### 数据库规范
- 所有表使用 InnoDB 引擎
- 字符集统一使用 utf8mb4
- 时间字段使用 DATETIME，带默认 CURRENT_TIMESTAMP
- 逻辑删除使用 status 字段，不使用物理删除

---

*最后更新: 2026-04-14 (R1高风险项修复完成：方法级权限控制、用户/角色管理后端实现、权限测试覆盖)*

---

## 阶段七补充：架构洞察

### Pinia Store 状态隔离模式

**问题背景**：
`approval.js` 最初使用单一的 `approvals` ref 和 `pagination` ref 来存储所有列表数据。当用户在待办列表、已办列表、我的申请列表之间切换时，后一次请求会覆盖前一次的 `approvals` 和 `pagination`，导致侧边栏的徽章计数（基于 `approvals.length` 或 `pagination.total`）出现错误。

**解决方案**：
引入独立的计数 refs，与共享的列表状态解耦：

```javascript
const todoTotal = ref(0)   // 待办总数，独立存储
const doneTotal = ref(0)   // 已办总数，独立存储
const myTotal = ref(0)     // 我的申请总数，独立存储
```

**设计原则**：
- **列表数据共享**：`approvals` 和 `pagination` 仍然共享，因为用户通常只在一个列表页面操作
- **计数数据隔离**：侧边栏徽章需要持久准确的计数，因此使用独立的 refs，由各自的 fetch 方法单独维护
- **计算属性暴露**：通过 `pendingCount`、`doneCount`、`myApprovalCount` 计算属性暴露给 UI，保证响应式更新

**相关文件**：
| 文件 | 作用 |
|------|------|
| `oa-frontend/src/stores/approval.js` | Store 核心，维护独立的 `todoTotal`/`doneTotal`/`myTotal` refs |
| `oa-frontend/src/layouts/MainLayout.vue` | 侧边栏导航，显示三个徽章计数 |
| `oa-frontend/src/views/Dashboard.vue` | 工作台首页，使用 `pendingCount` 显示待办统计 |

### Dashboard Statistics API 设计

**接口定义**：
- **端点**：`GET /approvals/statistics`
- **响应**：`Result<DashboardStatisticsResponse>`

**统计项说明**：

| 字段 | 数据来源 | 视角 | 说明 |
|------|----------|------|------|
| `pendingCount` | `oa_approval` | 审批人 | 当前用户作为 `current_approver_id` 且 `status=1`(审批中) 的工单数；**admin 角色统计全系统待办数** |
| `approvedCount` | `oa_approval_history` | 审批人 | 当前用户在历史记录中 `action=1`(通过) 的去重工单数 |
| `rejectedCount` | `oa_approval_history` | 审批人 | 当前用户在历史记录中 `action=2`(拒绝) 的去重工单数 |
| `myApprovalCount` | `oa_approval` | 申请人 | 当前用户作为 `applicant_id` 发起的工单总数 |
| `doneCount` | `oa_approval_history` | 审批人 | 当前用户在历史记录中有任意操作的去重工单数 |
| `approvalTypeDistribution` | `oa_approval` | 申请人 | 当前用户发起的申请按 `type` 分组统计 |

**关键设计决策**：

1. **已通过/已拒绝使用审批人视角**：
   - 工作台首页的"已通过"和"已拒绝"卡片展示的是当前用户作为审批人的**操作成果**，而非其申请被他人处理的结果。
   - 数据源从 `oa_approval`（工单当前状态）切换为 `oa_approval_history`（审批历史记录）。
   - 使用 `COUNT(DISTINCT approval_id)` 避免同一工单多次操作导致计数膨胀。

2. **我的申请/类型分布使用申请人视角**：
   - "本月申请"和"审批类型分布"自然属于申请人视角，数据源仍为 `oa_approval` 表。

3. **待办/已办分离**：
   - 待办（pending）是"需要我审批的"，已办（done）是"我已经审批过的"，两者从不同维度统计，不可混用同一数据源。

**相关文件**：
| 文件 | 作用 |
|------|------|
| `oa-backend/src/main/java/com/oasystem/dto/DashboardStatisticsResponse.java` | 工作台统计数据响应 DTO，包含6项统计指标和类型分布嵌套类 |
| `oa-backend/src/main/java/com/oasystem/controller/ApprovalController.java` | 提供 `GET /approvals/statistics` 端点，从 `SecurityContext` 获取当前用户ID |
| `oa-backend/src/main/java/com/oasystem/service/impl/ApprovalServiceImpl.java` | 实现 `getDashboardStatistics()`，组合多个 Mapper 查询结果并组装响应 |
| `oa-backend/src/main/java/com/oasystem/mapper/ApprovalMapper.java` | 提供申请人视角的统计查询（待办数、我的申请数、类型分布） |
| `oa-backend/src/main/java/com/oasystem/mapper/ApprovalHistoryMapper.java` | 提供审批人视角的统计查询（已办数、通过数、拒绝数），使用 `COUNT(DISTINCT)` 去重 |
| `oa-frontend/src/views/Dashboard.vue` | 对接统计接口，将后端数据映射到前端4个统计卡片和类型分布图 |

### R1 修复：后端方法级权限控制与用户/角色管理后端

**问题背景**：
根据 `Authority-review-report.md` 的审查结果，R1 高风险项指出后端接口缺少方法级权限控制，所有认证用户均可调用任意管理接口，存在严重越权风险。具体表现为：
- `SecurityConfig` 未启用 `@EnableMethodSecurity`
- Controller 层无任何 `@PreAuthorize` / `@Secured` 注解
- 缺失 `UserController` 和 `RoleController`，用户/角色管理仅依赖前端模拟数据

**修复方案**：
1. **启用方法级安全**：在 `SecurityConfig` 中添加 `@EnableMethodSecurity(prePostEnabled = true)`
2. **为现有 Controller 添加权限注解**：
   - `ApprovalController`：创建/更新/删除/提交/重新编辑/撤销 需 `apply` 权限；审批通过/拒绝/待办/已办 需 `approval` 权限；列表查询需认证
   - `AuthController`：`/auth/info` 需 `isAuthenticated()`
3. **补齐用户/角色管理后端**：
   - 新建 `UserService` / `UserServiceImpl` / `UserController`，支持用户 CRUD、修改密码、更新个人资料
   - 新建 `RoleService` / `RoleServiceImpl` / `RoleController`，支持角色 CRUD
   - 为管理接口添加 `user_view` / `user_manage` / `role_manage` 权限控制
4. **测试覆盖**：编写 `MethodSecurityTest`（32 个测试用例），全面验证权限控制生效

**权限编码设计**：
| 权限编码 | 适用接口 | 默认角色 |
|---------|---------|---------|
| `all` | 所有接口 | admin |
| `user_view` | GET /users, GET /users/{id} | admin, manager |
| `user_manage` | POST/PUT/DELETE /users/* | admin |
| `role_manage` | GET/POST/PUT/DELETE /roles/* | admin |
| `approval` | 审批操作、待办/已办列表 | admin, manager |
| `apply` | 创建/编辑/提交/撤销工单 | admin, manager, employee |
| `personal` | 修改密码、更新个人资料 | admin, manager, employee |

**相关文件**：
| 文件 | 作用 |
|------|------|
| `oa-backend/src/main/java/com/oasystem/config/SecurityConfig.java` | 启用 `@EnableMethodSecurity` |
| `oa-backend/src/main/java/com/oasystem/controller/ApprovalController.java` | 添加 `@PreAuthorize` 方法级权限控制 |
| `oa-backend/src/main/java/com/oasystem/controller/AuthController.java` | 添加 `isAuthenticated()` 控制 |
| `oa-backend/src/main/java/com/oasystem/controller/UserController.java` | 用户管理控制器（新建） |
| `oa-backend/src/main/java/com/oasystem/controller/RoleController.java` | 角色管理控制器（新建） |
| `oa-backend/src/main/java/com/oasystem/service/UserService.java` / `impl/UserServiceImpl.java` | 用户服务层（新建） |
| `oa-backend/src/main/java/com/oasystem/service/RoleService.java` / `impl/RoleServiceImpl.java` | 角色服务层（新建） |
| `oa-backend/src/test/java/com/oasystem/controller/MethodSecurityTest.java` | 权限控制集成测试（32个用例） |
| `oa-backend/docs/api-test/permission-api-tests.openapi.yaml` | Apifox 接口测试用例文档 |

### R2 修复：审批数据权限控制

**问题背景**：
根据 `Authority-review-report.md` 的审查结果，R2 高风险项指出 `GET /approvals/{id}` 和 `GET /approvals` 接口没有任何数据权限过滤，任何认证用户都能查看任意工单的详情和全部列表，存在敏感数据泄露风险。

**修复方案**：
1. **接口签名调整**：为 `ApprovalService.getById()` 和 `ApprovalService.list()` 增加 `currentUserId` 参数，使 Service 层能够感知当前操作人
2. **Controller 层透传**：`ApprovalController` 从 `SecurityContext` 获取当前用户ID，传入 Service 方法
3. **Service 层权限校验**：
   - `getById`：查询到工单后，调用 `checkViewPermission()` 进行数据权限校验
   - `list`：在 MyBatis-Plus 查询条件中动态注入数据权限过滤条件

**数据权限规则**：

| 角色 | 详情查询 | 列表查询 |
|------|---------|---------|
| admin | 全部 | 全部 |
| manager | 本部门工单 + 指定自己审批的工单 | 本部门工单 + 指定自己审批的工单 |
| employee | 仅自己的工单 | 仅自己的工单 |

**实现细节**：
- 经理的列表查询使用子查询实现：`applicant_id IN (SELECT id FROM sys_user WHERE dept_id = ?) OR current_approver_id = ?`
- MyBatis-Plus `apply("... {0} ...", deptId)` 确保参数化安全
- 无权限时抛出 `BusinessException("无权查看该工单")`，由全局异常处理器统一返回 `code=500` 的标准响应

**相关文件**：
| 文件 | 作用 |
|------|------|
| `oa-backend/src/main/java/com/oasystem/service/ApprovalService.java` | 接口签名增加 `currentUserId` 参数 |
| `oa-backend/src/main/java/com/oasystem/controller/ApprovalController.java` | 从 SecurityContext 获取用户ID并透传 |
| `oa-backend/src/main/java/com/oasystem/service/impl/ApprovalServiceImpl.java` | 实现 `checkViewPermission()` 和列表权限过滤 |
| `oa-backend/src/test/java/com/oasystem/service/ApprovalDataPermissionTest.java` | 数据权限单元测试（10个用例） |

### 审批结果落表修复的架构启示

**问题**：COLA 状态机执行动作后更新了 `Approval` 实体的内存状态，但某些字段因缺少 MyBatis-Plus 的 `@TableField` 注解，导致 `updateById()` 时未正确映射到数据库列。

**修复方案**：
- 在 `Approval` 实体中为关键流程字段补充 `@TableField` 注解，确保 `status`、`current_approver_id` 等字段能被 MyBatis-Plus 正确识别并持久化。
- 在 `ApprovalStateMachineHelper` 的动作方法中增加关键日志，便于追踪状态转换的执行路径。

**架构启示**：
- ORM 实体类必须与数据库表结构保持严格一致，新增/修改字段时同步更新实体注解。
- 状态机只负责内存中的状态流转，持久化逻辑需要在 Service 层显式调用并验证结果。

**相关文件**：
| 文件 | 作用 |
|------|------|
| `oa-backend/src/main/java/com/oasystem/entity/Approval.java` | 审批工单实体，补充 `@TableField` 注解解决字段映射问题 |
| `oa-backend/src/main/java/com/oasystem/statemachine/ApprovalStateMachineHelper.java` | 状态机动作实现，增加状态变更审计日志 |
| `oa-backend/src/main/java/com/oasystem/service/impl/ApprovalServiceImpl.java` | 审批服务实现，修复审批操作后的持久化逻辑 |

### 动态表单数据类型演进

**演进过程**：
- **初始设计**：`formData` 在前端作为 JSON 字符串提交，后端以 `String` 接收并存储。
- **问题**：前后端需要手动进行 JSON 字符串和对象的转换，容易出错且不符合 RESTful 设计。
- **最终设计**：`formData` 改为 `Map<String, Object>`，前端直接提交 JSON 对象，后端使用 FastJSON2 自动序列化为 `JSON` 类型存储到 MySQL。

**相关文件**：
| 文件 | 作用 |
|------|------|
| `oa-backend/src/main/java/com/oasystem/dto/ApprovalCreateRequest.java` | 创建工单请求 DTO，`formData` 字段类型改为 `Map<String, Object>` |
| `oa-backend/src/main/java/com/oasystem/dto/ApprovalUpdateRequest.java` | 更新工单请求 DTO，`formData` 字段类型改为 `Map<String, Object>` |
| `oa-backend/src/main/java/com/oasystem/service/impl/ApprovalServiceImpl.java` | 适配 `Map` 类型的表单数据，使用 `JSON.toJSONString()` 持久化 |
| `oa-frontend/src/views/ApprovalCreate.vue` | 表单设计器渲染的动态表单，直接输出 JSON 对象作为 `formData` |

### R3 修复：前端权限控制体系

**问题背景**：
根据 `Authority-review-report.md` 的审查结果，R3 高风险项指出前端无任何角色权限控制，所有菜单和按钮对所有用户可见。前端虽然使用 Pinia 管理认证状态，但路由、侧边栏、操作按钮均未根据用户角色或权限进行显隐控制，存在越权操作风险。

**修复方案**：
1. **后端权限数据暴露**：扩展 `UserDetailsImpl`、`LoginResponse`、`UserInfoResponse`，将 `sys_role.permissions` JSON 数组以字符串列表形式下发到前端
2. **前端权限基础设施**：
   - 工具层 `permission.js`：提供 `hasPermission`、`hasAnyPermission`、`hasApprovalPermission`、`hasRole` 等纯函数
   - 指令层 `v-permission`：自定义 Vue 指令，在挂载前检查权限，无权限则移除 DOM 元素
   - Store 层 `auth.js`：扩展 `permissions` computed property 和 `checkPermission`/`checkAnyPermission`/`checkRole` 方法
   - 路由层 `router/index.js`：增强 `beforeEach` 守卫，支持 `meta.permissionCheck` 函数拦截未授权导航
3. **页面级权限适配**：
   - `MainLayout.vue` 侧边栏：按权限动态计算 `showTodoMenu`、`showApprovalMenu`、`showUserMenuItem` 等
   - `ApprovalManage.vue`：发起审批（`apply`）、审批操作（`approval:execute`）按钮权限控制
   - `ApprovalDetail.vue`：通过/拒绝/提交按钮按状态+权限双条件控制
   - `UserManage.vue` / `RoleManage.vue`：管理按钮按 `user_manage` / `role_manage` / `all` 控制

**前端权限架构层次**：
```
┌─────────────────────────────────────────────────────────────┐
│                      前端权限控制架构                          │
├─────────────────────────────────────────────────────────────┤
│  数据层                                                       │
│    └─ 后端下发 permissions: ["all", "approval", "user_manage"] │
│         ↑ 存储于 Pinia authStore.userInfo.permissions         │
├─────────────────────────────────────────────────────────────┤
│  工具层  (src/utils/permission.js)                            │
│    ├─ hasPermission(perms, perm) → boolean                   │
│    ├─ hasAnyPermission(perms, requiredPerms) → boolean       │
│    ├─ hasApprovalPermission(perms) → boolean                 │
│    └─ hasRole(currentRole, roles) → boolean                  │
├─────────────────────────────────────────────────────────────┤
│  指令层  (src/directives/permission.js)                       │
│    └─ v-permission="'user_manage'"                           │
│         无权限时 el.parentNode?.removeChild(el)               │
├─────────────────────────────────────────────────────────────┤
│  Store层  (src/stores/auth.js)                                │
│    ├─ permissions (computed) → 当前用户权限列表               │
│    ├─ checkPermission(perm) → boolean                        │
│    ├─ checkAnyPermission(perms) → boolean                    │
│    └─ checkRole(roles) → boolean                             │
├─────────────────────────────────────────────────────────────┤
│  路由层  (src/router/index.js)                                │
│    ├─ meta.permissionCheck: (perms) => boolean               │
│    └─ beforeEach 中调用 checkRoutePermission 拦截             │
├─────────────────────────────────────────────────────────────┤
│  视图层                                                       │
│    ├─ v-if="canShowMenu" / v-permission="'apply'"            │
│    └─ 按钮级、菜单级、页面级权限控制                           │
└─────────────────────────────────────────────────────────────┘
```

**权限编码与前端映射**：
| 权限编码 | 前端控制点 | 默认角色 |
|---------|-----------|---------|
| `all` | 全部菜单/按钮 | admin |
| `user_view` | 用户列表可见 | admin, manager |
| `user_manage` | 用户增删改 | admin |
| `role_manage` | 角色增删改 | admin |
| `approval` | 审批列表、审批操作 | admin, manager |
| `approval:execute` | 直接审批按钮 | admin, manager |
| `approval:execute:dept` | 部门经理代审批 | manager |
| `approval:execute:all` | 管理员代审批 | admin |
| `apply` | 发起审批、提交、重新编辑、撤销 | admin, manager, employee |
| `personal` | 个人资料、修改密码 | admin, manager, employee |

**相关文件**：
| 文件 | 作用 |
|------|------|
| `oa-backend/security/UserDetailsImpl.java` | 新增 `permissions` 字段，携带角色权限码 |
| `oa-backend/dto/LoginResponse.java` / `UserInfoResponse.java` | 新增 `permissions` 字段 |
| `oa-backend/service/impl/AuthServiceImpl.java` | 填充权限数据到响应 |
| `oa-frontend/src/utils/permission.js` | 权限校验工具函数 |
| `oa-frontend/src/directives/permission.js` | `v-permission` 自定义指令 |
| `oa-frontend/src/stores/auth.js` | Store 扩展权限相关 computed 和方法 |
| `oa-frontend/src/router/index.js` | 路由守卫 `permissionCheck` |
| `oa-frontend/src/main.js` | 全局注册 `v-permission` 指令 |
| `oa-frontend/src/layouts/MainLayout.vue` | 侧边栏菜单权限控制 |
| `oa-frontend/src/views/ApprovalManage.vue` | 审批列表按钮权限控制 |
| `oa-frontend/src/views/ApprovalDetail.vue` | 详情页操作按钮权限控制 |
| `oa-frontend/src/views/UserManage.vue` / `RoleManage.vue` | 管理页面按钮权限控制 |

### 审批模块 Store 状态完全隔离（架构升级）

**问题背景**：
阶段七补充中虽然引入了独立的 `todoTotal`/`doneTotal`/`myTotal` refs，但列表数据仍然共享同一个 `approvals` ref。当用户在 TodoList、DoneList、ApprovalManage 之间切换时，`fetchTodoList` 和 `fetchDoneList` 都会覆盖 `approvals.value`，导致：
1. 切换到 DoneList 后返回 TodoList，若未重新请求则显示已办数据
2. `pendingApprovals` 计算属性基于 `approvals.value` 过滤，在 DoneList 加载后会被污染
3. 接口 403/500 失败时，旧数据残留在页面上，用户无法感知异常

**解决方案**：
将共享的 `approvals` 拆分为完全独立的四个状态：

```javascript
const approvals = ref([])       // ApprovalManage 专用
const todoApprovals = ref([])   // TodoList 专用
const doneApprovals = ref([])   // DoneList 专用
const myApprovals = ref([])     // MyApprovals 专用
```

**fetch 方法职责分离**：
| 方法 | 写入目标 | 失败时清空 |
|------|---------|-----------|
| `fetchApprovals` | `approvals` | `approvals = []` |
| `fetchTodoList` | `todoApprovals` | `todoApprovals = []`, `todoTotal = 0` |
| `fetchDoneList` | `doneApprovals` | `doneApprovals = []`, `doneTotal = 0` |
| `fetchMyApprovals` | `myApprovals` | `myApprovals = []`, `myTotal = 0` |

**计算属性绑定**：
- `pendingApprovals` → `todoApprovals.value.filter(a => a.status === 'processing')`
- `DoneList.filteredApprovals` → `approvalStore.doneApprovals`

**架构原则**：
- **一视图一状态**：每个列表视图拥有独立的响应式状态，互不干扰
- **失败即清空**：任何接口异常都会将对应状态重置为初始值，避免脏数据残留
- **错误可视化**：TodoList / DoneList 增加 `error` ref，失败时展示错误提示而非旧列表

**相关文件**：
| 文件 | 作用 |
|------|------|
| `oa-frontend/src/stores/approval.js` | Store 核心，维护四个独立的列表状态 |
| `oa-frontend/src/views/TodoList.vue` | 绑定 `todoApprovals`，增加错误状态展示 |
| `oa-frontend/src/views/DoneList.vue` | 绑定 `doneApprovals`，增加错误状态展示 |

### R3 修复补充：前端页面内部权限管控（视图层细化）

**问题背景**：
R3 修复已完成侧边栏菜单级权限控制，但页面内部的快捷操作、统计卡片、内容区域未做权限隔离，导致普通员工仍可在工作台看到「待办审批」「已办事项」「表单设计」入口，个人中心也显示无权限快捷操作。

**修复方案**：
1. **统计卡片权限隔离**：工作台顶部统计卡片按权限条件渲染，普通员工（仅有 `apply` 权限）仅展示「本月申请」，隐藏「待办事项」「已通过」「已拒绝」
2. **内容区域权限隔离**：待办事项列表块整体按 `canAccessApproval` 控制，无权限时不渲染左侧列表区
3. **快捷操作权限隔离**：工作台和个人中心的快捷操作按钮均使用 `v-if` 按权限显隐
4. **布局自适应**：当左侧列表区隐藏时，右侧快捷操作区自动占满全宽，避免页面空洞

**权限判断复用**：
```javascript
// Dashboard.vue / Profile.vue 统一使用与侧边栏一致的判断逻辑
const canAccessApproval = computed(() => hasApprovalPermission(authStore.permissions))
const canAccessFormDesigner = computed(() => hasPermission(authStore.permissions, 'all'))
```

**相关文件**：
| 文件 | 作用 |
|------|------|
| `oa-frontend/src/views/Dashboard.vue` | 统计卡片、待办列表、快捷操作按权限渲染 |
| `oa-frontend/src/views/Profile.vue` | 快捷操作按权限渲染 |

### 403 异常处理路径统一

**问题背景**：
`GlobalExceptionHandler` 和 `RestAccessDeniedHandler` 同时处理了 `AccessDeniedException`，导致行为不一致：
- Filter 层抛出的 403 → 由 `RestAccessDeniedHandler` 处理 → HTTP 200 + 业务码 403
- Controller 层 `@PreAuthorize` 抛出的 403 → 被 `@ExceptionHandler` 捕获 → 可能返回不同格式

**解决方案**：
1. **移除 `@ExceptionHandler(AccessDeniedException.class)`**：`GlobalExceptionHandler` 不再捕获权限异常
2. **统一由 `RestAccessDeniedHandler` 处理**：所有 `AccessDeniedException`（无论来源）最终都会经过 Spring Security 的 Filter 链，由 `AccessDeniedHandler` 处理
3. **增强诊断日志**：在 `RestAccessDeniedHandler` 中记录当前认证用户名称、authorities、请求方法和 URI，便于排查权限问题

**处理流程**：
```
请求 → Spring Security Filter Chain
  → 认证通过，到达 @PreAuthorize
    → 权限不足 → 抛出 AccessDeniedException
      → ExceptionTranslationFilter 捕获
        → 调用 RestAccessDeniedHandler.handle()
          → 记录诊断日志（用户、权限、URI）
          → 返回 HTTP 200 + Result.forbidden()
```

**相关文件**：
| 文件 | 作用 |
|------|------|
| `oa-backend/security/RestAccessDeniedHandler.java` | 统一 403 响应处理器，含诊断日志 |
| `oa-backend/exception/GlobalExceptionHandler.java` | 移除 `@ExceptionHandler(AccessDeniedException.class)` |

### Admin 权限增强：数据查询维度全局视野

**问题背景**：
后端审批操作层面已实现 4 级权限层级（直接审批 / 管理员代审批 / 经理代审批 / 拒绝），admin 可审批任意工单。但数据查询层面（待办列表、工作台统计）仍按 `current_approver_id = 自己` 过滤，admin 只能看到指派给自己的待办，无法看到全系统待审批工单。

**修复方案**：
1. **`getTodoList()`**：查询前获取当前用户角色，admin 角色不加 `current_approver_id` 过滤条件，返回全系统 `status=1` 工单。
2. **`getDashboardStatistics()`**：admin 的 `pendingCount` 使用 `countAllTodos()` 统计全系统待办数，其他角色仍使用 `countTodoByApproverId()` 统计个人待办。

**权限判断逻辑**：
```java
User currentUser = userMapper.selectByIdWithRole(userId);
if (currentUser != null && "admin".equals(currentUser.getRoleName())) {
    // admin：全系统视角
} else {
    // 其他角色：个人视角
}
```

**相关文件**：
| 文件 | 作用 |
|------|------|
| `oa-backend/mapper/ApprovalMapper.java` | 新增 `countAllTodos()` 方法 |
| `oa-backend/service/impl/ApprovalServiceImpl.java` | `getTodoList()` / `getDashboardStatistics()` 增加角色判断 |
| `oa-backend/src/test/java/com/oasystem/service/AdminPermissionEnhancementTest.java` | 新增 7 个测试用例覆盖 admin/经理/员工的待办和统计 |

### 前端撤销操作与确认弹窗设计

**问题背景**：
审批详情页在 `processing` 状态下仅提供「通过/拒绝」按钮，申请人无法撤销已提交的工单。此外，原生 `confirm()` 弹窗会显示浏览器默认标题（如 `localhost:3000 显示`），体验不佳。

**修复方案**：
1. **撤销权限判断**：新增 `canRevoke` 计算属性，条件为 `status === 'processing' && applicantId === currentUser.id`
2. **撤销按钮布局**：
   - 审批人操作区（通过/拒绝）中，若当前用户同时为申请人，额外显示「撤销申请」按钮
   - 非审批人但为申请人时，单独显示「撤销申请」按钮
3. **自定义确认弹窗**：新建 `ConfirmDialog.vue`，使用 Vue `Teleport` 挂载到 body，带遮罩层和过渡动画，完全自定义标题和内容

**相关文件**：
| 文件 | 作用 |
|------|------|
| `oa-frontend/src/views/ApprovalDetail.vue` | 新增撤销按钮、确认弹窗调用 |
| `oa-frontend/src/components/ConfirmDialog.vue` | 自定义确认弹窗组件 |

### 侧边栏徽章精简设计

**问题背景**：
侧边栏「已办事项」和「审批流程」旁的计数徽章信息量低且视觉冗余，用户反馈无需持续关注。

**设计决策**：
- 仅保留「待办事项」旁的红色徽章（`bg-danger-500`），因其具有强提醒属性
- 移除「已办事项」绿色徽章和「审批流程」蓝色徽章
- 计数数据仍由 `doneTotal` / `myApprovalCount` 维护，仅不再在 UI 上展示

**相关文件**：
| 文件 | 作用 |
|------|------|
| `oa-frontend/src/layouts/MainLayout.vue` | 精简徽章展示逻辑 |

### 数据库数据查询规范

**决策**：涉及数据库数据时，必须以实际数据库中的最新数据为准，禁止参考或执行数据库初始化脚本。

**理由**：
- `database/init.sql` 仅作历史备份，其中的角色权限、用户数据等可能已与实际运行环境不一致
- 基于初始化脚本推断当前数据状态会导致权限判断错误（如 manager 角色缺少 `form_design` 或 `apply` 权限）
- 生产环境和开发环境的数据由用户手动维护，AI 不应擅自假设数据内容

**规范**：
- 查询数据库权限、角色、用户等动态数据时，必须通过数据库连接执行实时查询
- 禁止将 `init.sql` 中的数据作为决策依据
- 禁止运行 `init.sql` 或任何数据库初始化脚本

**相关文件**：
| 文件 | 作用 |
|------|------|
| `CLAUDE.md` | 新增数据库数据查询规则 |

### 动态表单数据展示修复

**问题背景**：
审批工单支持多种类型（请假、报销、出差等），每种类型有特定的扩展字段（请假日期、报销金额、出差地点）。创建页将扩展字段存储在 `formData` JSON 字段中，但详情页仅渲染固定的 `content` 文本，导致审批人无法查看这些关键信息。

**根因分析**：
- 前端创建页 (`ApprovalCreate.vue`) 将动态字段收集到 `formData` 对象中提交给后端
- 后端将 `formData` 以 JSON 格式持久化到 `oa_approval.form_data` 字段
- 前端详情页 (`ApprovalDetail.vue`) 未读取 `formData`，仅展示 `content` 字段

**修复方案**：
1. **数据解析兼容**：新增 `parsedFormData` 计算属性，兼容后端返回的 JSON 字符串和对象两种格式（FastJSON2 在某些场景下可能返回字符串）
2. **条件渲染控制**：新增 `hasFormData` 计算属性，检测是否存在任一动态字段，避免空区域渲染
3. **字段映射展示**：在"申请内容"下方新增"申请详情"区域，按类型展示动态字段
   - 请假：`startDate` / `endDate`
   - 报销：`amount`（带货币符号）
   - 出差：`destination`

**前后端数据流**：
```
ApprovalCreate.vue → formData: { startDate, endDate, amount, destination }
                   → POST /approvals (Map<String, Object>)
Backend → JSON.toJSONString() → oa_approval.form_data (MySQL JSON type)
                   → GET /approvals/{id}
ApprovalDetail.vue → parsedFormData (兼容 String/Object)
                   → 条件渲染动态字段
```

**相关文件**：
| 文件 | 作用 |
|------|------|
| `oa-frontend/src/views/ApprovalCreate.vue` | 收集并提交动态表单数据 |
| `oa-frontend/src/views/ApprovalDetail.vue` | 解析并展示动态表单数据 |
| `oa-backend/dto/ApprovalCreateRequest.java` | `formData` 字段类型 `Map<String, Object>` |
| `oa-backend/entity/Approval.java` | `formData` 字段映射 `oa_approval.form_data` |

### 工作台统计块点击交互与列表页路由设计

**问题背景**：
工作台首页的四个统计卡片（待办事项、已通过、已拒绝、我的申请）原本仅为静态展示，用户无法快速查看对应分类的详细列表。需增强交互性，使统计数字可直接跳转至对应列表。

**设计决策**：
1. **统计卡片可点击**：四个统计卡片增加 `@click` 事件，点击后通过 Vue Router 跳转至对应页面
2. **新增三个专用列表页**：
   - `ApprovedList.vue`（`/approved`）：仅展示 `status = 2 (APPROVED)` 的工单，绿色主题
   - `RejectedList.vue`（`/rejected`）：仅展示 `status = 3 (RETURNED)` 的工单，红色主题
   - `MyApplicationList.vue`（`/my-applications`）：展示当前用户作为 applicant 的全部工单
3. **权限隔离**：`/approved` 和 `/rejected` 需 `hasApprovalPermission`（即 `approval:execute` 或 `all`），`/my-applications` 对全部认证用户开放
4. **列表页复用现有能力**：三个页面均使用 `approvalStore.fetchApprovals({ status: X })` 获取数据，复用 `ApprovalService.list()` 的分页、排序和数据权限过滤能力

**路由映射**：
| 统计卡片 | 路由 | 页面 | 权限要求 |
|---------|------|------|---------|
| 待办事项 | `/todo` | `TodoList.vue` | `approval:execute` / `all` |
| 已通过 | `/approved` | `ApprovedList.vue` | `approval:execute` / `all` |
| 已拒绝 | `/rejected` | `RejectedList.vue` | `approval:execute` / `all` |
| 我的申请 | `/my-applications` | `MyApplicationList.vue` | 已认证即可 |

**相关文件**：
| 文件 | 作用 |
|------|------|
| `oa-frontend/src/views/Dashboard.vue` | 统计卡片增加点击跳转 |
| `oa-frontend/src/views/ApprovedList.vue` | 已通过工单列表 |
| `oa-frontend/src/views/RejectedList.vue` | 已拒绝（已打回）工单列表 |
| `oa-frontend/src/views/MyApplicationList.vue` | 我的申请列表 |
| `oa-frontend/src/router/index.js` | 新增三条路由，配置权限守卫 |

### 统计口径与数据权限一致性设计

**问题背景**：
工作台统计接口 `GET /approvals/statistics` 返回的 `approvedCount`/`rejectedCount` 与前端列表页 `GET /approvals?status=2` / `GET /approvals?status=3` 返回的实际数据量不一致。具体表现为 manager 角色工作台显示 5 条已通过，列表页只显示 3 条。

**根因分析**：
| 维度 | 工作台统计（旧逻辑） | 列表页查询 |
|------|-------------------|-----------|
| 数据源 | `oa_approval_history`（审批历史） | `oa_approval`（工单表） |
| 统计口径 | "我审批过的"去重工单数 | "当前状态 = APPROVED/RETURNED" 的工单数 |
| 权限过滤 | 无（仅按 approver_id 过滤） | 有（R2 数据权限：部门+指派） |
| 时效性 | 包含历史上审批过但当前状态可能已变更的工单 | 仅包含当前状态仍为满足条件的工单 |

**修复方案**：
1. **提取公共数据权限方法**：将 `list()` 中的权限过滤逻辑抽取为 `applyDataPermission(LambdaQueryWrapper, User, Long)` 私有方法
2. **统一统计口径**：工作台统计改为从 `oa_approval` 表按状态查询，并调用 `applyDataPermission()` 应用与列表页**完全相同**的数据权限规则
3. **消除数据源差异**：统计和列表均基于 `oa_approval` 表的当前状态，不再查询 `oa_approval_history`

**数据权限规则（复用 R2）**：
```java
private void applyDataPermission(LambdaQueryWrapper<Approval> wrapper, User currentUser, Long currentUserId) {
    if ("admin".equals(currentUser.getRoleName())) return; // admin 不受限制
    if ("manager".equals(currentUser.getRoleName())) {
        wrapper.and(w -> {
            w.apply("applicant_id IN (SELECT id FROM sys_user WHERE dept_id = {0})", currentUser.getDeptId())
             .or()
             .eq(Approval::getCurrentApproverId, currentUserId);
        });
    } else {
        wrapper.eq(Approval::getApplicantId, currentUserId); // 普通员工仅看自己
    }
}
```

**关键代码位置**：
| 文件 | 方法 | 说明 |
|------|------|------|
| `oa-backend/service/impl/ApprovalServiceImpl.java` | `list()` | 列表查询，应用数据权限 |
| `oa-backend/service/impl/ApprovalServiceImpl.java` | `getDashboardStatistics()` | 统计接口，approvedCount/rejectedCount 复用 `applyDataPermission()` |
| `oa-backend/service/impl/ApprovalServiceImpl.java` | `applyDataPermission()` | 公共数据权限过滤方法 |

**设计原则**：
- **同源同权**：同一角色在同一维度下的统计和列表查询必须使用相同的数据源和权限规则
- **单一真实来源**：`oa_approval` 表的当前状态是工单状态的唯一真实来源，统计不应基于历史记录
- **公共方法复用**：数据权限逻辑抽取为独立方法，避免统计和列表两处维护导致不一致

---

### 表单模板管理架构

**问题背景**：
系统需要支持自定义审批表单，不同审批类型（请假、报销、采购等）拥有不同的字段集合。初始方案在 `ApprovalCreate.vue` 中硬编码了各类型对应的扩展字段（请假日期、报销金额、出差地点等），新增字段类型需要修改前端代码，扩展性差。

**设计决策**：
1. **表单模板化**：将表单字段配置抽取到 `oa_form_template` 表，通过 JSON 存储字段定义数组
2. **编码映射**：审批类型与表单模板通过编码一对一映射（如 `leave` → `LEAVE_FORM`），创建审批时自动加载对应模板
3. **统一渲染**：使用 `DynamicForm.vue` 通用组件，根据模板字段配置动态渲染输入控件，同时支持编辑模式和只读模式

**字段配置格式**：
```json
[
  {
    "id": "field_001",
    "type": "text",
    "label": "请假事由",
    "name": "reason",
    "placeholder": "请输入请假原因",
    "required": true
  },
  {
    "id": "field_002",
    "type": "select",
    "label": "请假类型",
    "name": "type",
    "options": [
      {"value": "sick", "label": "病假"},
      {"value": "personal", "label": "事假"}
    ],
    "required": true
  }
]
```

**数据流**：
```
FormDesigner.vue → fieldsConfig (数组) → POST /form-templates
Backend → JSON.toJSONString() → oa_form_template.fields_config (MySQL JSON)

ApprovalCreate.vue → type selection → GET /form-templates/code/{code}
  → fieldsConfig → DynamicForm (编辑模式)
  → formData (对象) → POST /approvals (Map<String, Object>)

ApprovalDetail.vue → GET /form-templates/code/{code}
  → fieldsConfig + formData → DynamicForm (readonly 模式)
```

**权限设计**：
- 表单模板查询：`isAuthenticated()`（所有登录用户可见，用于创建/查看审批时加载模板）
- 表单模板管理：`form_design` 或 `all`（仅管理员和部门经理可创建/编辑/删除）

**相关文件**：
| 文件 | 作用 |
|------|------|
| `oa-backend/controller/FormTemplateController.java` | 表单模板 REST API |
| `oa-backend/service/impl/FormTemplateServiceImpl.java` | 业务逻辑，fieldsConfig JSON 转换 |
| `oa-backend/mapper/FormTemplateMapper.java` | 数据访问，含按 code 查询 |
| `oa-frontend/src/components/DynamicForm.vue` | 通用动态表单渲染组件 |
| `oa-frontend/src/views/FormDesigner.vue` | 表单设计器（列表+设计双模式） |
| `oa-frontend/src/views/ApprovalCreate.vue` | 按类型自动加载模板并渲染 |
| `oa-frontend/src/views/ApprovalDetail.vue` | 按模板只读展示申请详情 |

---

*最后更新: 2026-04-22 (阶段八：表单设计器实现完成)*
