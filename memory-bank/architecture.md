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

### 2.6 数据语言规范

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
│   │   ├── SecurityConfig.java       # Spring Security配置
│   │   └── JwtConfig.java            # JWT配置
│   ├── controller/                    # 控制器层 (API接口)
│   ├── service/                       # 业务逻辑层
│   │   └── impl/                      # 服务实现
│   ├── mapper/                        # 数据访问层 (MyBatis-Plus)
│   ├── entity/                        # 实体类 (数据库映射)
│   ├── dto/                           # 数据传输对象
│   ├── enums/                         # 枚举类
│   │   ├── ApprovalStatus.java       # 审批状态枚举
│   │   └── ApprovalEvent.java        # 审批事件枚举
│   ├── statemachine/                  # 状态机相关
│   │   ├── ApprovalStateMachineHelper.java  # 条件与动作
│   │   └── ApprovalContext.java      # 状态机上下文
│   └── util/                          # 工具类
│       └── JwtTokenUtil.java         # JWT工具
├── src/main/resources/
│   ├── application.yml               # 主配置
│   ├── application-dev.yml           # 开发环境配置
│   └── mapper/                        # XML映射文件
├── src/test/java/                     # 测试代码
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
### 3.4 数据库文件 (阶段二已创建)

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

### 3.4 后端基础文件 (阶段三创建)

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

### 3.5 实体类详细说明

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

- 使用 JWT Token 进行身份认证
- Token 有效期默认 24 小时
- 请求头格式: `Authorization: Bearer <token>`

### 4.3 状态机上下文

```java
@Data
@AllArgsConstructor
public class ApprovalContext {
    private Approval approval;        // 审批工单实体
    private ApprovalActionCmd cmd;    // 操作命令
    private Long operatorId;          // 当前操作人ID
}
```

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

*最后更新: 2026-03-27 (阶段三完成：后端基础框架搭建，包含5个实体类、4个枚举类、统一响应、全局异常处理、完整配置)*
