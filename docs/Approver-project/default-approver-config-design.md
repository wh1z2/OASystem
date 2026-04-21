# 默认审批人配置机制技术设计文档

**文档版本**: v1.0  
**编写日期**: 2026-04-20  
**文档状态**: 待审批  
**编写人**: AI Assistant  

---

## 目录

1. [背景与问题分析](#1-背景与问题分析)
2. [设计目标与原则](#2-设计目标与原则)
3. [总体架构设计](#3-总体架构设计)
4. [数据库设计](#4-数据库设计)
5. [后端设计](#5-后端设计)
6. [前端设计](#6-前端设计)
7. [审批人解析引擎](#7-审批人解析引擎)
8. [优先级与冲突处理](#8-优先级与冲突处理)
9. [API 接口规范](#9-api-接口规范)
10. [实施计划](#10-实施计划)
11. [附录](#11-附录)

---

## 1. 背景与问题分析

### 1.1 当前问题

当前系统的审批人指定逻辑存在以下问题：

| 问题编号 | 问题描述 | 影响 |
|---------|---------|------|
| P1 | 创建/提交工单时 `currentApproverId` 依赖前端传入或手动指定 | 普通员工发起申请时需手动选择审批人，操作繁琐 |
| P2 | 缺乏统一的审批规则管理 | 无法按部门、业务类型自动匹配审批人 |
| P3 | 审批人变更需逐单修改 | 人员调动时历史规则无法批量更新 |
| P4 | 无上级汇报链支持 | 无法实现按组织架构层级自动向上审批 |

### 1.2 需求来源

- 产品设计文档 v1.1 第 4.1.2 节流程条件判断中提出：按部门、金额、类型匹配审批人
- 实际业务场景：员工提交申请时不应关心具体审批人是谁，系统应自动按规则分配

---

## 2. 设计目标与原则

### 2.1 设计目标

1. **零配置提交**：普通员工发起审批时无需手动指定审批人，系统自动根据预设规则分配
2. **规则可配置**：管理员可通过可视化界面配置、启用、禁用审批规则
3. **实时生效**：规则变更后即时生效，无需重启服务
4. **可扩展**：支持未来新增规则策略（如按金额区间、按项目等）

### 2.2 设计原则

| 原则 | 说明 |
|-----|------|
| 前后端分离 | 后端提供 RESTful API，前端负责 UI 呈现 |
| 职责分离 | 规则配置、规则解析、规则执行三者独立 |
| 向后兼容 | 保留手动指定审批人的能力，作为默认规则的补充 |
| 安全兜底 | 规则匹配失败时必须给出明确提示，禁止静默跳过 |

---

## 3. 总体架构设计

### 3.1 架构分层

```
┌─────────────────────────────────────────────────────────────────┐
│                        前端展示层                                │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────────┐   │
│  │ 规则列表页  │ │ 规则编辑页  │ │ 发起申请页（无审批人选择）│   │
│  └─────────────┘ └─────────────┘ └─────────────────────────┘   │
├─────────────────────────────────────────────────────────────────┤
│                        后端 API 层                               │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  ApproverRuleController  (RESTful API)                  │   │
│  │  ├─ GET    /approver-rules              查询规则列表     │   │
│  │  ├─ GET    /approver-rules/{id}         查询规则详情     │   │
│  │  ├─ POST   /approver-rules              创建规则         │   │
│  │  ├─ PUT    /approver-rules/{id}         更新规则         │   │
│  │  ├─ DELETE /approver-rules/{id}         删除/禁用规则    │   │
│  │  └─ POST   /approver-rules/preview      规则效果预览     │   │
│  └─────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────┤
│                        业务逻辑层                                │
│  ┌─────────────────────┐  ┌─────────────────────────────────┐  │
│  │ ApproverRuleService │  │ ApprovalService (改造点)         │  │
│  │ ├─ 规则 CRUD        │  │ ├─ create()  → 调用解析引擎      │  │
│  │ ├─ 规则校验         │  │ ├─ submit()  → 调用解析引擎      │  │
│  │ └─ 规则排序         │  │ └─ update()  → 校验手动指定      │  │
│  └─────────────────────┘  └─────────────────────────────────┘  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │              DefaultApproverResolver (核心解析引擎)       │   │
│  │  ├─ resolve()  入口方法                                  │   │
│  │  ├─ 策略1: 按部门+角色查找                                │   │
│  │  ~~├─ 策略2: 按审批类型匹配~~（v1.0 不实现）              │   │
│  │  ├─ 策略3: 按固定人员指定                                │   │
│  │  ~~├─ 策略4: 按上级汇报链查找~~（v1.0 不实现）            │   │
│  │  ~~└─ 策略5: 按条件表达式过滤~~（v1.0 不实现）            │   │
│  └─────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────┤
│                        数据访问层                                │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐             │
│  │ ApproverRuleMapper         │ │ UserMapper    │             │
│  │ DeptMapper（扩展parent_id）│ │ ApprovalMapper│             │
│  └─────────────┘ └─────────────┘ └─────────────┘             │
└─────────────────────────────────────────────────────────────────┘
```

### 3.2 与现有系统集成点

| 集成点 | 现有文件 | 改造内容 |
|-------|---------|---------|
| 创建工单 | `ApprovalServiceImpl.create()` | 若 `currentApproverId` 为空，调用解析引擎自动填充 |
| 提交工单 | `ApprovalServiceImpl.submit()` | 若提交时无审批人，调用解析引擎自动分配 |
| 更新工单 | `ApprovalServiceImpl.update()` | 保留手动指定能力，校验权限逻辑不变 |
| 前端创建页 | `ApprovalCreate.vue` | 移除审批人选择 UI，改为展示"将由系统自动分配" |
| 数据库 | `sys_dept` | 增加 `parent_id` 字段支持汇报链 |

---

## 4. 数据库设计

### 4.1 审批规则表 (oa_approver_rule)

```sql
CREATE TABLE IF NOT EXISTS oa_approver_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '规则ID',
    name VARCHAR(100) NOT NULL COMMENT '规则名称（如：技术部请假审批规则）',
    strategy_type TINYINT NOT NULL DEFAULT 1 COMMENT '策略类型：1=按部门角色, ~~2=按审批类型~~(v1.0不实现), 3=固定人员, ~~4=上级汇报~~(v1.0不实现), ~~5=条件表达式~~(v1.0不实现)',
    
    -- 匹配条件（JSON格式，支持多维度组合）
    match_conditions JSON COMMENT '匹配条件：{"deptIds":[1], "types":[1,2], "roleIds":[2]}',
    
    -- 审批人指定方式
    approver_type TINYINT NOT NULL DEFAULT 1 COMMENT '审批人类型：1=指定用户, 2=指定角色, ~~3=上级~~(v1.0不实现), ~~4=部门负责人~~(v1.0不实现)',
    approver_value VARCHAR(500) COMMENT '审批人值：用户ID/角色ID/层级数等，JSON格式',
    
    -- 规则元数据
    priority INT NOT NULL DEFAULT 100 COMMENT '优先级（数字越小优先级越高，默认100）',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0=禁用, 1=启用',
    description VARCHAR(500) COMMENT '规则描述',
    
    -- 作用范围
    scope_type TINYINT NOT NULL DEFAULT 1 COMMENT '作用范围：1=全局, 2=指定部门, 3=指定角色',
    
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    KEY idx_strategy_type (strategy_type),
    KEY idx_status (status),
    KEY idx_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='默认审批人规则表';
```

### 4.2 部门表扩展 (sys_dept)

```sql
-- 增加上级部门字段，支持汇报链
ALTER TABLE sys_dept ADD COLUMN parent_id BIGINT NULL COMMENT '上级部门ID（逻辑外键）' AFTER id;
ALTER TABLE sys_dept ADD KEY idx_parent_id (parent_id);
```

### ~~4.3 审批工单表扩展（可选，用于多级审批）~~（v1.0 不实现）

~~```sql~~
~~-- 若未来支持多级审批，增加审批步骤字段~~
~~ALTER TABLE oa_approval ADD COLUMN approval_step INT NOT NULL DEFAULT 1 COMMENT '当前审批步骤（第几级）' AFTER current_approver_id;~~
~~```~~

### 4.4 规则示例数据

```sql
-- 规则1：技术部请假/加班申请 → 部门经理审批
INSERT INTO oa_approver_rule (name, strategy_type, match_conditions, approver_type, approver_value, priority, status, description, scope_type) VALUES
('技术部日常审批规则', 1, '{"deptIds":[1], "types":[1,4]}', 2, '[2]', 10, 1, '技术部员工请假或加班申请，由部门经理审批', 2);

-- ~~规则2：报销/采购申请 → 按审批类型匹配财务人员~~（策略2 v1.0 不实现，可用策略3替代）
-- ~~INSERT INTO oa_approver_rule (...) VALUES ('财务类审批规则', 2, ...);~~

-- ~~规则3：按上级汇报链（需 dept.parent_id 支持）~~（策略4 v1.0 不实现）
-- ~~INSERT INTO oa_approver_rule (...) VALUES ('上级汇报规则', 4, ...);~~
```

---

## 5. 后端设计

### 5.1 实体类设计

#### 5.1.1 ApproverRule 实体

```java
@Data
@TableName("oa_approver_rule")
public class ApproverRule {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Integer strategyType;      // 策略类型枚举
    private String matchConditions;    // JSON 字符串
    private Integer approverType;      // 审批人类型枚举
    private String approverValue;      // JSON 字符串
    private Integer priority;
    private Integer status;
    private String description;
    private Integer scopeType;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    
    // 非持久化字段
    @TableField(exist = false)
    private List<Long> matchDeptIds;   // 解析后的部门ID列表
    @TableField(exist = false)
    private List<Integer> matchTypes;  // 解析后的审批类型列表
}
```

#### 5.1.2 策略类型枚举 (ApproverStrategyType)

```java
public enum ApproverStrategyType {
    DEPT_ROLE(1, "按部门角色"),
    ~~APPROVAL_TYPE(2, "按审批类型"),~~ // v1.0 不实现
    FIXED_USER(3, "固定人员"),
    ~~REPORTING_LINE(4, "上级汇报链"),~~ // v1.0 不实现
    ~~CONDITION_EXPR(5, "条件表达式");~~ // v1.0 不实现
    // v1.0 仅实现 DEPT_ROLE(1) 和 FIXED_USER(3)
    
    private final int code;
    private final String label;
}
```

#### 5.1.3 审批人类型枚举 (ApproverType)

```java
public enum ApproverType {
    SPECIFIC_USER(1, "指定用户"),
    SPECIFIC_ROLE(2, "指定角色"),
    ~~SUPERVISOR(3, "直接上级"),~~ // v1.0 不实现（依赖策略4）
    ~~DEPT_HEAD(4, "部门负责人");~~ // v1.0 不实现（依赖策略4）
    // v1.0 仅使用 SPECIFIC_USER(1) 和 SPECIFIC_ROLE(2)
    
    private final int code;
    private final String label;
}
```

### 5.2 核心解析引擎 (DefaultApproverResolver)

```java
@Component
@RequiredArgsConstructor
public class DefaultApproverResolver {
    
    private final ApproverRuleMapper ruleMapper;
    private final UserMapper userMapper;
    private final DepartmentMapper departmentMapper;
    
    /**
     * 解析默认审批人
     * @param applicantId 申请人ID
     * @param approvalType 审批类型
     * @return 解析结果，包含审批人ID和匹配到的规则信息
     */
    public ResolverResult resolve(Long applicantId, Integer approvalType) {
        // 1. 获取申请人信息
        User applicant = userMapper.selectByIdWithRole(applicantId);
        if (applicant == null) {
            throw new BusinessException("申请人不存在");
        }
        
        // 2. 查询所有启用的规则，按优先级排序
        List<ApproverRule> rules = ruleMapper.selectEnabledRulesOrderedByPriority();
        
        // 3. 依次匹配规则
        for (ApproverRule rule : rules) {
            if (isMatch(rule, applicant, approvalType)) {
                Long approverId = resolveApproverId(rule, applicant);
                if (approverId != null) {
                    return ResolverResult.success(approverId, rule.getId(), rule.getName());
                }
            }
        }
        
        // 4. 无匹配规则时的兜底策略
        return fallbackResolve(applicant);
    }
    
    /**
     * 判断规则是否匹配当前申请
     */
    private boolean isMatch(ApproverRule rule, User applicant, Integer approvalType) {
        // 解析匹配条件
        MatchCondition condition = JSON.parseObject(rule.getMatchConditions(), MatchCondition.class);
        
        // 校验部门匹配
        if (condition.getDeptIds() != null && !condition.getDeptIds().isEmpty()) {
            if (!condition.getDeptIds().contains(applicant.getDeptId())) {
                return false;
            }
        }
        
        // 校验审批类型匹配
        if (condition.getTypes() != null && !condition.getTypes().isEmpty()) {
            if (!condition.getTypes().contains(approvalType)) {
                return false;
            }
        }
        
        // 校验角色匹配
        if (condition.getRoleIds() != null && !condition.getRoleIds().isEmpty()) {
            if (!condition.getRoleIds().contains(applicant.getRoleId())) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 根据规则解析出具体的审批人ID
     */
    private Long resolveApproverId(ApproverRule rule, User applicant) {
        List<Long> approverIds = JSON.parseArray(rule.getApproverValue(), Long.class);
        
        switch (ApproverType.fromCode(rule.getApproverType())) {
            case SPECIFIC_USER:
                // 直接返回第一个有效用户
                return approverIds.stream()
                    .map(userMapper::selectById)
                    .filter(u -> u != null && u.getStatus() == 1)
                    .findFirst()
                    .map(User::getId)
                    .orElse(null);
                    
            case SPECIFIC_ROLE:
                // 查找该部门下拥有指定角色的用户
                Long roleId = approverIds.get(0);
                return findUserByRoleAndDept(roleId, applicant.getDeptId());

            // ~~case SUPERVISOR:~~ // v1.0 不实现（策略4）
            // ~~case DEPT_HEAD:~~ // v1.0 不实现（策略4）

            default:
                return null;
        }
    }
    
    /**
     * 兜底策略：查找申请人所在部门的经理
     */
    private ResolverResult fallbackResolve(User applicant) {
        Long deptHeadId = findDeptHead(applicant.getDeptId());
        if (deptHeadId != null) {
            return ResolverResult.success(deptHeadId, null, "默认部门负责人兜底策略");
        }
        return ResolverResult.failed("未找到匹配的审批规则，且无法定位部门负责人，请联系管理员配置审批规则");
    }
}
```

### 5.3 Service 层改造点

#### 5.3.1 ApprovalServiceImpl.create() 改造

```java
@Override
@Transactional(rollbackFor = Exception.class)
public Long create(ApprovalCreateRequest request, Long applicantId) {
    Approval approval = new Approval();
    approval.setTitle(request.getTitle());
    approval.setType(request.getType());
    approval.setApplicantId(applicantId);
    approval.setStatus(ApprovalStatus.DRAFT.getCode());
    approval.setPriority(request.getPriority());
    approval.setContent(request.getContent());
    if (request.getFormData() != null) {
        approval.setFormData(JSON.toJSONString(request.getFormData()));
    }
    
    // ========== 改造点：自动解析默认审批人 ==========
    if (request.getCurrentApproverId() != null) {
        // 手动指定优先：校验权限
        validateApproverPermission(request.getCurrentApproverId());
        approval.setCurrentApproverId(request.getCurrentApproverId());
    } else {
        // 自动解析默认审批人
        ResolverResult result = defaultApproverResolver.resolve(applicantId, request.getType());
        if (!result.isSuccess()) {
            throw new BusinessException(result.getMessage());
        }
        approval.setCurrentApproverId(result.getApproverId());
        log.info("自动分配审批人：工单标题={}, 规则={}, 审批人ID={}", 
            approval.getTitle(), result.getRuleName(), result.getApproverId());
    }
    // ================================================
    
    approvalMapper.insert(approval);
    return approval.getId();
}
```

#### 5.3.2 ApprovalServiceImpl.submit() 改造

```java
@Override
@Transactional(rollbackFor = Exception.class)
public Boolean submit(Long id, Long operatorId) {
    Approval approval = approvalMapper.selectById(id);
    // ... 校验逻辑 ...
    
    // ========== 改造点：提交时若仍无审批人，再次解析 ==========
    if (approval.getCurrentApproverId() == null) {
        ResolverResult result = defaultApproverResolver.resolve(
            approval.getApplicantId(), approval.getType());
        if (!result.isSuccess()) {
            throw new BusinessException("提交失败：" + result.getMessage());
        }
        approval.setCurrentApproverId(result.getApproverId());
    }
    // ======================================================
    
    ApprovalContext context = new ApprovalContext(approval, null, operatorId);
    ApprovalStatus newStatus = stateMachine.fireEvent(
        ApprovalStatus.fromCode(approval.getStatus()), ApprovalEvent.SUBMIT, context);
    // ... 后续逻辑 ...
}
```

---

## 6. 前端设计

> **UI/UX 规范说明**：新增前端页面（如默认审批人配置页）的风格应与现有系统页面保持高度一致，包括配色（Tailwind 自定义调色板：`primary`、`success`、`warning`、`danger`）、组件样式（`card`、`btn`、`input`、`badge` 等通用类名）、布局规范（响应式断点、间距体系）以及交互模式（Toast 反馈、确认弹窗、加载状态）。在编写前端代码时，可调用项目内置的 `ui-ux-pro-max` Skill 进行设计辅助与一致性检查，该 Skill 位于 `/OASystem/.claude/skills/ui-ux-pro-max`。

### 6.1 新增页面：默认审批人配置页

**路由**: `/system/approver-rules`  
**权限**: 仅 `admin` 角色可见（`all` 或 `role_manage` 权限）

#### 6.1.1 页面布局

```
┌─────────────────────────────────────────────────────────────┐
│  默认审批人配置                                               │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ [新建规则]  搜索框...                                 │   │
│  └─────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ 规则名称 │ 策略类型 │ 匹配条件 │ 审批人 │ 优先级 │ 状态 │ 操作 │
│  ├─────────────────────────────────────────────────────┤   │
│  │ 技术部请假规则 │ 按部门角色 │ 技术部+请假 │ 部门经理 │ 10 │ 启用 │ 编辑/禁用 │
│  │ 财务审批规则   │ 按审批类型 │ 报销/采购  │ 张经理   │ 20 │ 启用 │ 编辑/禁用 │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

#### 6.1.2 规则编辑弹窗

| 字段 | 组件 | 说明 |
|-----|------|------|
| 规则名称 | 文本输入 | 必填，如"技术部请假审批规则" |
| 策略类型 | 下拉选择 | 单选：按部门角色/固定人员（~~按审批类型/上级汇报~~ v1.0不实现） |
| 匹配条件 | 动态表单 | 根据策略类型动态渲染条件输入区 |
| 审批人指定 | 动态表单 | 根据策略类型动态渲染审批人选择 |
| 优先级 | 数字输入 | 越小越优先，默认100 |
| 状态 | 开关 | 启用/禁用 |
| 规则描述 | 文本域 | 可选 |

### 6.2 改造页面：发起申请页 (ApprovalCreate.vue)

**改造内容**：

1. 移除"选择审批人"相关 UI 元素（当前版本本无此 UI，但后续若有需移除）
2. 在审批流程预览卡片中增加说明：
   > "审批人将由系统根据预设规则自动分配，您无需手动选择。"
3. 提交后详情页展示实际分配的审批人姓名

### 6.3 新增 API 封装 (approverRule.js)

```javascript
import axios from './config'

export const approverRuleApi = {
  getList: (params) => axios.get('/approver-rules', { params }),
  getById: (id) => axios.get(`/approver-rules/${id}`),
  create: (data) => axios.post('/approver-rules', data),
  update: (id, data) => axios.put(`/approver-rules/${id}`, data),
  delete: (id) => axios.delete(`/approver-rules/${id}`),
  preview: (data) => axios.post('/approver-rules/preview', data)
}
```

---

## 7. 审批人解析引擎

### 7.1 引擎执行流程

```
发起申请 / 提交工单
    │
    ▼
┌──────────────────┐
│ currentApproverId │──── 有值? ────→ 校验权限 ──→ 使用手动指定
│ 是否已指定？      │    │
└──────────────────┘    │ 无值
    │                   ▼
    │           ┌─────────────────────┐
    │           │ DefaultApproverResolver
    │           │ 1. 获取申请人信息    │
    │           │ 2. 加载启用规则列表  │
    │           │ 3. 按优先级排序      │
    │           │ 4. 依次匹配条件      │
    │           │ 5. 解析审批人ID      │
    │           └─────────────────────┘
    │                   │
    │           ┌───────┴───────┐
    │           ▼               ▼
    │      解析成功          解析失败
    │           │               │
    │           ▼               ▼
    │    填充currentApproverId  抛出BusinessException
    │    记录分配日志            提示用户联系管理员
    │           │
    └───────────┘
                ▼
        继续后续业务流程
```

### 7.2 各策略详细说明

#### 策略1：按部门角色 (DEPT_ROLE)

**适用场景**：某部门内特定类型的申请由特定角色审批

**匹配条件示例**：
```json
{
  "deptIds": [1, 2],
  "types": [1, 4],
  "roleIds": [2]
}
```

**审批人解析**：查找 `sys_user` 中 `dept_id IN (1,2) AND role_id = 2 AND status = 1` 的用户

#### ~~策略2：按审批类型 (APPROVAL_TYPE)~~（v1.0 不实现）

~~**适用场景**：某类审批（如报销）固定由财务角色审批，与申请部门无关~~

~~**匹配条件示例**：~~
~~```json~~
~~{~~
~~  "types": [2, 3]~~
~~}~~
~~```~~

~~**审批人解析**：查找拥有财务角色的用户，或查找规则中指定的固定审批人~~

> v1.0 中可用**策略3（固定人员）**实现相同效果，如所有报销固定指派给财务经理。

#### 策略3：固定人员 (FIXED_USER)

**适用场景**：高管审批、特定业务专属审批人

**审批人值示例**：
```json
[2]
```

#### ~~策略4：上级汇报链 (REPORTING_LINE)~~（v1.0 不实现）

~~**适用场景**：按组织架构层级向上审批~~

~~**前提条件**：`sys_dept` 表已配置 `parent_id` 形成树形结构~~

~~**审批人值示例**：~~
~~```json~~
~~"1"  // 向上找1级（直接上级部门负责人）~~
~~```~~

~~**解析逻辑**：~~
~~1. 获取申请人所在部门~~
~~2. 通过 `parent_id` 向上追溯 N 级~~
~~3. 取目标部门的 `role_id = manager` 的用户~~

#### ~~策略5：条件表达式 (CONDITION_EXPR) - 预留扩展~~（v1.0 不实现）

~~**适用场景**：按金额、天数等表单字段动态决定审批人~~

~~**匹配条件示例**：~~
~~```json~~
~~{~~
~~  "types": [2],~~
~~  "expression": "formData.amount > 10000"~~
~~}~~
~~```~~

~~**实现说明**：v1.0 版本预留接口，不实现完整表达式引擎。可使用简单规则引擎（如 AviatorScript 或 Spring EL）在 v1.1 中扩展。~~

---

## 8. 优先级与冲突处理

### 8.1 规则匹配优先级

| 优先级规则 | 说明 |
|-----------|------|
| 1. 手动指定优先 | 若创建/提交时显式传入 `currentApproverId`，跳过自动解析，直接校验权限并使用 |
| 2. 规则优先级字段 | `priority` 值越小越优先。无匹配时执行兜底策略 |
| 3. 精确匹配优先 | 同时匹配到多条规则时，取 `priority` 最小的规则；若 `priority` 相同，取 `id` 较小的 |

### 8.2 冲突处理机制

| 冲突场景 | 处理策略 |
|---------|---------|
| 多条规则同时匹配 | 按 `priority` 排序取第一条，其余忽略 |
| 规则匹配但审批人已禁用 | 记录告警日志，继续匹配下一条规则 |
| 规则匹配但找不到具体用户 | 返回失败，提示"规则XX匹配成功但无法解析到有效审批人" |
| 无规则匹配且无部门负责人 | 返回失败，提示"请联系管理员配置默认审批规则" |
| 申请人自己就是解析出的审批人 | 校验并拒绝，防止自审（抛出 BusinessException） |

### 8.3 手动指定 vs 自动解析优先级

```
创建工单 / 提交工单
    │
    ├──→ 传入 currentApproverId ?
    │       ├──→ 是 → 校验该用户是否有 approval:execute 权限
    │       │           ├──→ 有 → 使用该审批人（手动指定生效）
    │       │           └──→ 无 → 抛出异常，拒绝创建
    │       └──→ 否 → 调用 DefaultApproverResolver 自动解析
    │                   ├──→ 成功 → 使用解析结果
    │                   └──→ 失败 → 抛出异常，流程阻断
    │
    注：reedit 时若传入新的 currentApproverId，同样遵循手动优先原则
```

---

## 9. API 接口规范

### 9.1 规则管理接口

#### 9.1.1 获取规则列表

```
GET /api/approver-rules
Authorization: Bearer <token>
```

**查询参数**：
| 参数 | 类型 | 说明 |
|-----|------|------|
| keyword | string | 按规则名称模糊搜索 |
| strategyType | int | 按策略类型筛选 |
| status | int | 按状态筛选 |
| pageNum | int | 页码，默认1 |
| pageSize | int | 每页大小，默认10 |

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "list": [
      {
        "id": 1,
        "name": "技术部请假审批规则",
        "strategyType": 1,
        "strategyTypeName": "按部门角色",
        "matchConditions": {"deptIds":[1], "types":[1,4]},
        "approverType": 2,
        "approverTypeName": "指定角色",
        "approverValue": "[2]",
        "priority": 10,
        "status": 1,
        "description": "技术部员工请假或加班由部门经理审批"
      }
    ],
    "total": 3,
    "pageNum": 1,
    "pageSize": 10
  },
  "timestamp": 1713612345678
}
```

#### 9.1.2 创建规则

```
POST /api/approver-rules
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体**：
```json
{
  "name": "人事部出差审批规则",
  "strategyType": 1,
  "matchConditions": {"deptIds":[3], "types":[5]},
  "approverType": 2,
  "approverValue": "[2]",
  "priority": 15,
  "status": 1,
  "description": "人事部出差申请由部门经理审批"
}
```

**权限要求**：`all` 或 `role_manage`

#### 9.1.3 更新规则

```
PUT /api/approver-rules/{id}
Authorization: Bearer <token>
```

#### 9.1.4 删除/禁用规则

```
DELETE /api/approver-rules/{id}
Authorization: Bearer <token>
```

**说明**：物理删除。若需保留历史，改为更新 `status=0` 禁用。

#### 9.1.5 规则效果预览

```
POST /api/approver-rules/preview
Authorization: Bearer <token>
```

**请求体**：
```json
{
  "applicantId": 3,
  "type": 1
}
```

**响应示例**：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "success": true,
    "approverId": 2,
    "approverName": "张经理",
    "ruleId": 1,
    "ruleName": "技术部请假审批规则",
    "message": "匹配成功"
  }
}
```

### 9.2 改造后的审批接口

#### 9.2.1 创建工单（兼容改造）

```
POST /api/approvals
```

**请求体变化**：`currentApproverId` 从必填变为可选
```json
{
  "title": "请假申请",
  "type": 1,
  "priority": 1,
  "content": "申请病假2天",
  "formData": {"reason": "感冒发烧"},
  "currentApproverId": null  // 不传则自动解析
}
```

---

## 10. 实施计划

### 10.1 实施阶段

| 阶段 | 任务 | 预计工时 | 产出物 |
|-----|------|---------|--------|
| 1 | 数据库变更：新建 `oa_approver_rule` 表，扩展 `sys_dept` | 0.5h | SQL 脚本 |
| 2 | 后端枚举与实体：StrategyType、ApproverType、ApproverRule | 1h | Java 类 |
| 3 | 后端 Mapper 与 Service：ApproverRuleMapper、ApproverRuleService | 2h | Java 类 |
| 4 | 后端核心引擎：DefaultApproverResolver | 2h | Java 类 + 单元测试（仅实现策略1、3） |
| 5 | 后端 Controller：ApproverRuleController | 1.5h | Java 类 |
| 6 | 改造 ApprovalService：集成解析引擎到 create/submit | 1.5h | 修改现有类 |
| 7 | 前端页面：ApproverRuleManage.vue（规则列表/编辑） | 3h | Vue 组件（策略下拉仅2项） |
| 8 | 前端改造：ApprovalCreate.vue 移除审批人选择逻辑 | 0.5h | 修改现有组件 |
| 9 | 前后端联调与集成测试 | 3h | 测试报告 |
| 10 | 文档更新：更新 architecture.md、product-design-document.md | 1h | Markdown |

**总计预计工时**：约 14 小时（删减后）

### 10.2 文件变更清单

#### 新增文件

| 文件路径 | 说明 |
|---------|------|
| `oa-backend/entity/ApproverRule.java` | 审批规则实体 |
| `oa-backend/enums/ApproverStrategyType.java` | 策略类型枚举 |
| `oa-backend/enums/ApproverType.java` | 审批人类型枚举 |
| `oa-backend/mapper/ApproverRuleMapper.java` | 规则 Mapper |
| `oa-backend/service/ApproverRuleService.java` | 规则服务接口 |
| `oa-backend/service/impl/ApproverRuleServiceImpl.java` | 规则服务实现 |
| `oa-backend/controller/ApproverRuleController.java` | 规则控制器 |
| `oa-backend/resolver/DefaultApproverResolver.java` | 核心解析引擎 |
| `oa-backend/dto/ApproverRuleCreateRequest.java` | 创建规则 DTO |
| `oa-backend/dto/ApproverRuleUpdateRequest.java` | 更新规则 DTO |
| `oa-backend/dto/ResolverResult.java` | 解析结果 DTO |
| `oa-backend/dto/ApproverRuleQuery.java` | 规则查询 DTO |
| `oa-frontend/src/views/ApproverRuleManage.vue` | 规则管理页面 |
| `oa-frontend/src/api/approverRule.js` | 规则 API 封装 |
| `database/approver-rule-migration.sql` | 数据库迁移脚本 |

#### 修改文件

| 文件路径 | 修改内容 |
|---------|---------|
| `oa-backend/service/impl/ApprovalServiceImpl.java` | create/submit 集成自动解析 |
| `oa-backend/dto/ApprovalCreateRequest.java` | `currentApproverId` 可选校验 |
| `oa-backend/entity/Department.java` | 增加 `parentId` 字段 |
| `database/init.sql` | 增加 `sys_dept.parent_id` 和示例规则数据 |
| `oa-frontend/src/views/ApprovalCreate.vue` | 移除审批人选择，增加自动分配提示 |
| `oa-frontend/src/router/index.js` | 新增 `/system/approver-rules` 路由 |
| `oa-frontend/src/layouts/MainLayout.vue` | 侧边栏增加「审批规则配置」菜单项（admin 可见） |

---

## 11. 附录

### 11.1 与现有审批权限体系的关系

本机制仅解决**"审批人是谁"**的问题，不改变现有**"谁能审批"**的权限校验逻辑：

1. `DefaultApproverResolver` 负责找出默认审批人
2. `validateApproverPermission()` 仍负责校验该审批人是否有 `approval:execute` 权限
3. `ApprovalStateMachineHelper.checkApproverPermissionDetail()` 仍负责执行时的 4 级权限检查

### ~~11.2 多级审批预留设计（v1.1 扩展）~~（v1.0 不实现）

~~当前 v1.0 版本仅支持**单级审批**（一个 `current_approver_id`）。若未来扩展为多级审批：~~

~~1. 新增 `oa_approval_step` 表存储流程定义~~
~~2. `DefaultApproverResolver` 扩展为返回 `List<Long>` 多级审批人链~~
~~3. 状态机流转时按步骤推进，逐级变更 `current_approver_id`~~

### 11.3 风险与应对

| 风险 | 影响 | 应对措施 |
|-----|------|---------|
| 规则配置错误导致找不到审批人 | 工单无法提交 | 提交前强制校验，解析失败明确提示 |
| 审批人被禁用后规则仍指向该用户 | 工单分配给无效用户 | 解析时校验用户状态，无效则继续匹配下一条规则 |
| ~~循环汇报链~~ | ~~解析死循环~~ | ~~上级汇报策略设置最大追溯层级（如3级），超限则失败~~（v1.0不实现策略4） |
| 性能问题（规则过多） | 每次提交都查询全表 | 启用规则数量通常 < 50，可接受；未来可加入 Redis 缓存 |

### 11.4 数据库迁移脚本（完整版）

```sql
-- ============================================
-- 默认审批人配置机制数据库迁移脚本
-- ============================================

-- 1. 扩展部门表，增加上级部门字段
ALTER TABLE sys_dept ADD COLUMN parent_id BIGINT NULL COMMENT '上级部门ID' AFTER id;
ALTER TABLE sys_dept ADD KEY idx_parent_id (parent_id);

-- 更新示例部门数据，建立汇报关系
UPDATE sys_dept SET parent_id = NULL WHERE id = 4;  -- 系统管理部为顶级
UPDATE sys_dept SET parent_id = 4 WHERE id = 1;     -- 技术部上级为系统管理部
UPDATE sys_dept SET parent_id = 4 WHERE id = 2;     -- 财务部上级为系统管理部
UPDATE sys_dept SET parent_id = 4 WHERE id = 3;     -- 人事部上级为系统管理部

-- 2. 创建审批规则表
CREATE TABLE IF NOT EXISTS oa_approver_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '规则ID',
    name VARCHAR(100) NOT NULL COMMENT '规则名称',
    strategy_type TINYINT NOT NULL DEFAULT 1 COMMENT '策略类型：1=按部门角色, ~~2=按审批类型~~(v1.0不实现), 3=固定人员, ~~4=上级汇报~~(v1.0不实现)',
    match_conditions JSON COMMENT '匹配条件JSON',
    approver_type TINYINT NOT NULL DEFAULT 1 COMMENT '审批人类型：1=指定用户, 2=指定角色, ~~3=上级~~(v1.0不实现), ~~4=部门负责人~~(v1.0不实现)',
    approver_value VARCHAR(500) COMMENT '审批人值JSON',
    priority INT NOT NULL DEFAULT 100 COMMENT '优先级（越小越优先）',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0=禁用, 1=启用',
    description VARCHAR(500) COMMENT '规则描述',
    scope_type TINYINT NOT NULL DEFAULT 1 COMMENT '作用范围：1=全局, 2=指定部门, 3=指定角色',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_strategy_type (strategy_type),
    KEY idx_status (status),
    KEY idx_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='默认审批人规则表';

-- 3. 插入默认规则（v1.0 仅策略1和策略3）
INSERT INTO oa_approver_rule (name, strategy_type, match_conditions, approver_type, approver_value, priority, status, description, scope_type) VALUES
('技术部日常审批规则', 1, '{"deptIds":[1], "types":[1,4]}', 2, '[2]', 10, 1, '技术部请假或加班由部门经理审批', 2),
('财务类审批规则', 3, '{"types":[2,3]}', 1, '[2]', 20, 1, '报销和采购由张经理审批（固定人员）', 1);
-- ~~('通用上级汇报规则', 4, ...)~~ // v1.0 不实现（策略4）

SELECT 'Migration completed!' AS result;
```

---

*文档结束*
