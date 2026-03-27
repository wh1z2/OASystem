# 工单流程自动化系统 - 技术架构文档

**文档版本**: v1.0
**编写日期**: 2026-03-24
**文档状态**: 正式发布

---

## 目录

1. [架构概述](#1-架构概述)
2. [技术栈选型](#2-技术栈选型)
3. [系统架构设计](#3-系统架构设计)
4. [COLA状态机集成方案](#4-cola状态机集成方案)
5. [数据存储设计](#5-数据存储设计)
6. [接口设计规范](#6-接口设计规范)
7. [安全架构](#7-安全架构)
8. [部署架构](#8-部署架构)
9. [技术难点与解决方案](#9-技术难点与解决方案)

---

## 1. 架构概述

### 1.1 架构设计原则

本系统遵循**"最简单但最健壮"**的核心原则，在满足毕业设计项目需求的前提下，优先选择成熟稳定、学习曲线平缓、社区活跃的技术方案。

| 原则 | 说明 |
|-----|------|
| **简单优先** | 避免过度设计，每个技术选型都有明确的必要性 |
| **成熟稳定** | 优先选择LTS版本，社区活跃度高的技术 |
| **渐进增强** | 核心功能先实现，扩展功能后续迭代 |
| **可维护性** | 代码结构清晰，文档完善，便于后续维护 |

### 1.2 架构风格

采用经典的分层架构（Layered Architecture）结合前后端分离模式：

```
┌─────────────────────────────────────────────────────────────────┐
│                        客户端层                                 │
│              Vue 3 SPA (单页应用)                               │
├─────────────────────────────────────────────────────────────────┤
│                        接入层                                   │
│              Nginx (反向代理、静态资源、负载均衡)                │
├─────────────────────────────────────────────────────────────────┤
│                        应用层                                   │
│              Spring Boot + COLA状态机                           │
├─────────────────────────────────────────────────────────────────┤
│                        数据层                                   │
│              MySQL (关系型数据)                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. 技术栈选型

### 2.1 技术栈总览

| 层次 | 技术组件 | 版本 | 选型理由 |
|-----|---------|------|---------|
| **前端框架** | Vue 3 | 3.4.x | 响应式系统、组合式API、性能优秀 |
| **前端构建** | Vite | 5.x | 快速冷启动、原生ESM、配置简单 |
| **UI组件库** | Tailwind CSS | 3.4.x | 原子化CSS、高度定制、无需引入组件库 |
| **状态管理** | Pinia | 2.1.x | Vue官方推荐、TypeScript友好 |
| **后端框架** | Spring Boot | 3.2.x | 生态完善、自动配置、生产级稳定 |
| **流程引擎** | COLA StateMachine | 5.x | 阿里开源、轻量级、适合毕业设计 |
| **数据库** | MySQL | 8.0.x | 开源免费、社区庞大、文档丰富 |
| **ORM框架** | MyBatis-Plus | 3.5.x | 简化CRUD、代码生成、性能优秀 |
| **安全框架** | Spring Security | 6.x | 与Spring Boot深度集成 |
| **构建工具** | Maven | 3.9.x | 标准构建工具、依赖管理成熟 |

### 2.2 技术选型详细说明

#### 2.2.1 前端技术栈

##### Vue 3 (v3.4.x)

**选型理由**:
- Vue 3采用Proxy-based响应式系统，性能相比Vue 2提升显著
- 组合式API（Composition API）使代码逻辑更易组织和复用
- 更好的TypeScript支持，便于类型检查
- 较小的学习曲线，适合毕业设计快速开发

**核心特性应用**:
```javascript
// 组合式API示例
<script setup>
import { ref, computed } from 'vue'

const approvals = ref([])
const pendingCount = computed(() =>
  approvals.value.filter(a => a.status === 'pending').length
)
</script>
```

##### Vite (v5.x)

**选型理由**:
- 冷启动速度极快（秒级），大幅提升开发效率
- 原生ESM支持，无需打包即可运行
- 热更新(HMR)速度快，保存即刷新
- 配置简单，开箱即用

**配置要点**:
```javascript
// vite.config.js
export default {
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
}
```

##### Tailwind CSS (v3.4.x)

**选型理由**:
- 原子化CSS方案，无需维护大量自定义CSS文件
- 高度可定制，通过配置文件统一设计规范
- 开发效率高，直接在HTML中编写样式
- 生产环境自动清理未使用样式，体积小

**设计系统配置**:
```javascript
// tailwind.config.js
module.exports = {
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#eff6ff',
          500: '#3b82f6',
          600: '#2563eb',
        }
      }
    }
  }
}
```

##### Pinia (v2.1.x)

**选型理由**:
- Vue官方推荐的状态管理方案
- API设计更简洁直观，比Vuex更易用
- 完整的TypeScript支持
- 支持Vue DevTools调试

#### 2.2.2 后端技术栈

##### Spring Boot (v3.2.x)

**选型理由**:
- Java生态最主流的企业级框架
- 自动配置大幅减少样板代码
- 内嵌Tomcat，部署简单
- 丰富的Starter依赖，集成方便
- 长期支持版本，稳定性有保障

**版本选择说明**:
- Spring Boot 3.x基于Spring Framework 6
- 要求JDK 17+，符合现代化Java开发趋势
- 原生支持虚拟线程（Project Loom）

##### COLA StateMachine (v5.x) ⭐ 核心组件

**选型理由（重点）**:

1. **阿里开源背书**: 阿里巴巴开源的COLA架构体系组件，经过阿里内部大规模生产验证
2. **轻量级设计**: 相比Activiti、Camunda等重量级流程引擎，COLA状态机更轻量，更适合毕业设计场景
3. **学习曲线平缓**: 概念简单（State/Event/Transition/Action），易于理解和使用
4. **代码即配置**: 通过Java代码定义状态机，无需XML配置，版本控制友好
5. **性能优秀**: 基于状态模式实现，状态转换高效

**版本选择**: v5.x（最新稳定版）

**Maven依赖**:
```xml
<dependency>
    <groupId>com.alibaba.cola</groupId>
    <artifactId>cola-component-statemachine</artifactId>
    <version>5.0.0</version>
</dependency>
```

**核心概念**:
```
StateMachine = States + Events + Transitions + Actions

States（状态）: DRAFT, PROCESSING, APPROVED, RETURNED, REVOKED
Events（事件）: SUBMIT, APPROVE, REJECT, REEDIT, REVOKE
Transitions（转换）: DRAFT --SUBMIT--> PENDING
Actions（动作）: 状态转换时执行的的业务逻辑
```

##### MySQL (v8.0.x)

**选型理由**:
- 开源关系型数据库，免费使用
- 社区庞大，问题解决方案丰富
- 性能优秀，支持高并发访问
- 支持JSON数据类型，可存储半结构化数据
- 与Spring Boot集成成熟

**字符集配置**:
```sql
-- 使用utf8mb4支持完整Unicode字符集
CREATE DATABASE oa_system
CHARACTER SET utf8mb4
COLLATE utf8mb4_unicode_ci;
```

##### MyBatis-Plus (v3.5.x)

**选型理由**:
- 在MyBatis基础上增强，提供通用CRUD接口
- 内置分页插件、性能分析插件
- 代码生成器可快速生成基础代码
- 支持ActiveRecord模式，开发效率高

#### 2.2.3 开发工具链

| 工具 | 用途 | 版本 |
|-----|------|------|
| JDK | Java运行环境 | 17 LTS |
| Maven | 项目构建 | 3.9.x |
| Node.js | 前端运行环境 | 20 LTS |
| VS Code / IDEA | IDE | 最新版 |
| Git | 版本控制 | 2.x |
| Postman | API测试 | 最新版 |

### 2.3 明确不采用的技术

根据"最简单但最健壮"原则，以下技术在本项目中**明确不使用**：

| 技术 | 不采用理由 |
|-----|-----------|
| **消息队列** (RabbitMQ/Kafka/RocketMQ) | 业务场景简单，无高并发削峰需求，引入MQ增加系统复杂度 |
| **缓存中间件** (Redis) | 数据量小，读多写少场景不明显，MySQL查询性能已足够 |
| **搜索引擎** (Elasticsearch) | 全文搜索需求简单，MySQL LIKE查询可满足 |
| **NoSQL数据库** (MongoDB) | 业务数据关系明确，关系型数据库更合适 |
| **微服务架构** | 系统规模小，单体应用更易开发和部署 |
| **容器编排** (K8s) | 毕业设计无需生产级容器编排，Docker足够 |

---

## 3. 系统架构设计

### 3.1 整体架构图

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                               客户端层                                       │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                          Vue 3 SPA                                    │  │
│  │  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐        │  │
│  │  │ Dashboard│ │ Approval│ │  Form   │ │  User   │ │  Role   │        │  │
│  │  │  仪表盘  │ │  审批   │ │ Designer│ │ 用户管理│ │ 角色管理│        │  │
│  │  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘        │  │
│  │                           ↑ Pinia Store                             │  │
│  │                           ↓ Axios HTTP                              │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      │ HTTP/JSON
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                               接入层                                         │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                           Nginx                                       │  │
│  │  • 静态资源服务 (dist目录)                                            │  │
│  │  • 反向代理 (/api -> Spring Boot)                                     │  │
│  │  • 负载均衡 (可选)                                                    │  │
│  │  • Gzip压缩                                                           │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                               应用层                                         │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                      Spring Boot Application                          │  │
│  │                                                                       │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  │  │
│  │  │ Controller  │  │   Service   │  │    Mapper   │  │    Entity   │  │  │
│  │  │   控制器层   │→ │   业务逻辑   │→ │   数据访问   │→ │   实体类    │  │  │
│  │  └─────────────┘  └──────┬──────┘  └─────────────┘  └─────────────┘  │  │
│  │                          │                                          │  │
│  │                          ↓                                          │  │
│  │  ┌───────────────────────────────────────────────────────────────┐  │  │
│  │  │              COLA StateMachine (流程引擎核心)                   │  │
│  │  │                                                               │  │
│  │  │   ┌──────────┐    Event     ┌──────────┐                     │  │
│  │  │   │  DRAFT   │ ───────────→ │ PENDING  │                     │  │
│  │  │   └──────────┘              └────┬─────┘                     │  │
│  │  │                                  │                           │  │
│  │  │                    ┌─────────────┴─────────────┐              │  │
│  │  │                    ↓                           ↓              │  │
│  │  │              ┌──────────┐               ┌──────────┐          │  │
│  │  │              │ APPROVED │               │ REJECTED │          │  │
│  │  │              └──────────┘               └──────────┘          │  │
│  │  └───────────────────────────────────────────────────────────────┘  │  │
│  │                                                                       │  │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐                  │  │
│  │  │ Spring Sec  │  │    JWT      │  │   AOP/Log   │                  │  │
│  │  │   权限控制   │  │   Token     │  │  日志/事务   │                  │  │
│  │  └─────────────┘  └─────────────┘  └─────────────┘                  │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
                                      │
                                      │ JDBC
                                      ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                               数据层                                         │
│  ┌───────────────────────────────────────────────────────────────────────┐  │
│  │                           MySQL 8.0                                   │  │
│  │                                                                       │  │
│  │   ┌────────────┐ ┌────────────┐ ┌────────────┐ ┌────────────┐        │  │
│  │   │  sys_user  │ │  sys_role  │ │ oa_approval│ │  oa_history│        │  │
│  │   │   用户表    │ │   角色表    │ │  审批工单表 │ │  审批历史表 │        │  │
│  │   └────────────┘ └────────────┘ └────────────┘ └────────────┘        │  │
│  │                                                                       │  │
│  └───────────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────┘
```

### 3.2 分层架构详细说明

#### 3.2.1 控制器层 (Controller)

职责：接收HTTP请求，参数校验，调用Service，返回统一响应

```java
@RestController
@RequestMapping("/api/approval")
@RequiredArgsConstructor
public class ApprovalController {

    private final ApprovalService approvalService;

    @PostMapping
    public Result<Long> create(@RequestBody @Valid ApprovalCreateCmd cmd) {
        return Result.success(approvalService.create(cmd));
    }

    @PostMapping("/{id}/approve")
    public Result<Void> approve(@PathVariable Long id,
                                @RequestBody @Valid ApprovalActionCmd cmd) {
        approvalService.approve(id, cmd);
        return Result.success();
    }
}
```

#### 3.2.2 业务逻辑层 (Service)

职责：业务逻辑处理，事务管理，调用状态机

```java
@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl implements ApprovalService {

    private final StateMachine<ApprovalStatus, ApprovalEvent> stateMachine;
    private final ApprovalRepository approvalRepository;

    @Override
    @Transactional
    public void approve(Long approvalId, ApprovalActionCmd cmd) {
        Approval approval = approvalRepository.findById(approvalId);

        // 使用COLA状态机驱动状态流转
        stateMachine.fireEvent(
            approval.getStatus(),
            ApprovalEvent.APPROVE,
            new ApprovalContext(approval, cmd)
        );

        approvalRepository.save(approval);
    }
}
```

#### 3.2.3 数据访问层 (Mapper/Repository)

职责：数据库CRUD操作，使用MyBatis-Plus实现

```java
@Mapper
public interface ApprovalMapper extends BaseMapper<ApprovalDO> {

    @Select("SELECT * FROM oa_approval WHERE status = #{status}")
    List<ApprovalDO> selectByStatus(@Param("status") String status);
}
```

### 3.3 组件依赖关系

```
┌─────────────────────────────────────────────────────────────┐
│                      组件依赖关系图                          │
│                                                             │
│                    ┌───────────────┐                        │
│                    │   Controller  │                        │
│                    └───────┬───────┘                        │
│                            │ 依赖                           │
│                    ┌───────▼───────┐                        │
│                    │    Service    │                        │
│                    └───────┬───────┘                        │
│                            │ 依赖                           │
│           ┌────────────────┼────────────────┐               │
│           │                │                │               │
│    ┌──────▼──────┐  ┌──────▼──────┐  ┌──────▼──────┐       │
│    │   StateMachine    │  │ Repository  │  │   Mapper    │       │
│    │   (COLA)    │  │   (接口)    │  │(MyBatis-Plus)       │
│    └─────────────┘  └─────────────┘  └─────────────┘       │
│                                        │                    │
│                                        ▼                    │
│                                ┌─────────────┐              │
│                                │   MySQL     │              │
│                                └─────────────┘              │
└─────────────────────────────────────────────────────────────┘
```

---

## 4. COLA状态机集成方案

### 4.1 状态机设计

#### 4.1.1 状态定义

```java
public enum ApprovalStatus {
    DRAFT("草稿"),
    PROCESSING("审批中"),
    APPROVED("已通过"),
    RETURNED("已打回"),
    REVOKED("已撤销");

    private final String desc;

    ApprovalStatus(String desc) {
        this.desc = desc;
    }
}
```

#### 4.1.2 事件定义

```java
public enum ApprovalEvent {
    SUBMIT("提交申请"),
    APPROVE("审批通过"),
    REJECT("审批拒绝"),
    REEDIT("重新编辑"),
    REVOKE("撤销申请");

    private final String desc;

    ApprovalEvent(String desc) {
        this.desc = desc;
    }
}
```

#### 4.1.3 状态机配置

```java
@Configuration
public class StateMachineConfig {

    @Bean
    public StateMachine<ApprovalStatus, ApprovalEvent> approvalStateMachine() {
        StateMachineBuilder<ApprovalStatus, ApprovalEvent> builder =
            StateMachineBuilderFactory.create();

        // DRAFT -> PROCESSING (提交)
        builder.externalTransition()
            .from(ApprovalStatus.DRAFT)
            .to(ApprovalStatus.PROCESSING)
            .on(ApprovalEvent.SUBMIT)
            .when(checkCondition())
            .perform(doAction());

        // PROCESSING -> APPROVED (通过)
        builder.externalTransition()
            .from(ApprovalStatus.PROCESSING)
            .to(ApprovalStatus.APPROVED)
            .on(ApprovalEvent.APPROVE)
            .when(checkPermission())
            .perform(doApprove());

        // PROCESSING -> RETURNED (拒绝)
        builder.externalTransition()
            .from(ApprovalStatus.PROCESSING)
            .to(ApprovalStatus.RETURNED)
            .on(ApprovalEvent.REJECT)
            .when(checkPermission())
            .perform(doReject());

        // PROCESSING -> DRAFT (撤销)
        builder.externalTransition()
            .from(ApprovalStatus.PROCESSING)
            .to(ApprovalStatus.DRAFT)
            .on(ApprovalEvent.REVOKE)
            .when(checkOwner())
            .perform(doRevoke());

        // APPROVED -> DRAFT (重新编辑)
        builder.externalTransition()
            .from(ApprovalStatus.APPROVED)
            .to(ApprovalStatus.DRAFT)
            .on(ApprovalEvent.REEDIT)
            .when(checkOwner())
            .perform(doReedit());

        // RETURNED -> DRAFT (重新编辑)
        builder.externalTransition()
            .from(ApprovalStatus.RETURNED)
            .to(ApprovalStatus.DRAFT)
            .on(ApprovalEvent.REEDIT)
            .when(checkOwner())
            .perform(doReedit());

        return builder.build("ApprovalStateMachine");
    }
}
```

### 4.2 状态机上下文

```java
@Data
@AllArgsConstructor
public class ApprovalContext {
    private Approval approval;
    private ApprovalActionCmd cmd;
    private Long operatorId;
}
```

### 4.3 条件与动作实现

```java
@Component
public class ApprovalStateMachineHelper {

    /**
     * 检查条件
     */
    public Condition<ApprovalContext> checkCondition() {
        return ctx -> {
            // 检查表单数据完整性
            return ctx.getApproval().getFormData() != null;
        };
    }

    /**
     * 检查权限
     */
    public Condition<ApprovalContext> checkPermission() {
        return ctx -> {
            // 检查当前用户是否有审批权限
            Long currentUserId = ctx.getOperatorId();
            Long approverId = ctx.getApproval().getCurrentApproverId();
            return currentUserId.equals(approverId);
        };
    }

    /**
     * 检查是否为申请人
     */
    public Condition<ApprovalContext> checkOwner() {
        return ctx -> {
            Long currentUserId = ctx.getOperatorId();
            Long applicantId = ctx.getApproval().getApplicantId();
            return currentUserId.equals(applicantId);
        };
    }

    /**
     * 审批通过动作
     */
    public Action<ApprovalStatus, ApprovalEvent, ApprovalContext> doApprove() {
        return (from, to, event, ctx) -> {
            Approval approval = ctx.getApproval();
            approval.setStatus(to);
            approval.setCurrentApproverId(null);

            // 记录审批历史
            ApprovalHistory history = new ApprovalHistory();
            history.setApprovalId(approval.getId());
            history.setApproverId(ctx.getOperatorId());
            history.setAction("APPROVE");
            history.setComment(ctx.getCmd().getComment());
            // save history...
        };
    }

    /**
     * 审批拒绝动作
     */
    public Action<ApprovalStatus, ApprovalEvent, ApprovalContext> doReject() {
        return (from, to, event, ctx) -> {
            Approval approval = ctx.getApproval();
            approval.setStatus(to);
            approval.setCurrentApproverId(null);

            // 记录审批历史
            // ...
        };
    }
}
```

### 4.4 状态机使用示例

```java
@Service
@RequiredArgsConstructor
public class ApprovalServiceImpl implements ApprovalService {

    private final StateMachine<ApprovalStatus, ApprovalEvent> stateMachine;

    @Override
    public void processApproval(Long approvalId, ApprovalEvent event,
                                ApprovalActionCmd cmd, Long operatorId) {
        Approval approval = approvalRepository.findById(approvalId)
            .orElseThrow(() -> new BusinessException("审批单不存在"));

        ApprovalContext context = new ApprovalContext(approval, cmd, operatorId);

        // 执行状态转换
        ApprovalStatus targetStatus = stateMachine.fireEvent(
            approval.getStatus(),
            event,
            context
        );

        if (targetStatus == null) {
            throw new BusinessException("当前状态不允许执行该操作");
        }

        // 保存更新后的审批单
        approvalRepository.save(approval);
    }
}
```

---

## 5. 数据存储设计

### 5.1 数据库设计原则

1. **第三范式为主**: 减少数据冗余，保证数据一致性
2. **适当反范化**: 高频查询场景适当增加冗余字段
3. **JSON字段应用**: 存储半结构化的表单数据
4. **索引优化**: 高频查询字段建立索引

### 5.2 表结构设计

```sql
-- 用户表
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '加密密码',
    name VARCHAR(50) NOT NULL COMMENT '真实姓名',
    email VARCHAR(100) NOT NULL COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    avatar VARCHAR(200) COMMENT '头像URL',
    role_id BIGINT NOT NULL COMMENT '角色ID',
    dept_id BIGINT COMMENT '部门ID',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0禁用,1启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_username (username),
    KEY idx_role_id (role_id),
    KEY idx_dept_id (dept_id),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 角色表
CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(50) NOT NULL COMMENT '角色标识',
    label VARCHAR(50) NOT NULL COMMENT '角色显示名',
    description VARCHAR(200) COMMENT '角色描述',
    permissions JSON COMMENT '权限列表',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- 审批工单表
CREATE TABLE oa_approval (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    title VARCHAR(200) NOT NULL COMMENT '审批标题',
    type TINYINT NOT NULL COMMENT '审批类型: 1=LEAVE, 2=EXPENSE, 3=PURCHASE, 4=OVERTIME, 5=TRAVEL',
    applicant_id BIGINT NOT NULL COMMENT '申请人ID（逻辑外键）',
    current_approver_id BIGINT COMMENT '当前审批人ID（逻辑外键）',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0=DRAFT, 1=PROCESSING, 2=APPROVED, 3=RETURNED, 4=REVOKED',
    priority TINYINT NOT NULL DEFAULT 1 COMMENT '优先级: 0=LOW, 1=NORMAL, 2=HIGH',
    content TEXT COMMENT '申请内容',
    form_data JSON COMMENT '表单数据',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    KEY idx_applicant_id (applicant_id),
    KEY idx_current_approver_id (current_approver_id),
    KEY idx_status (status),
    KEY idx_type (type),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批工单表';

-- 审批历史表
CREATE TABLE oa_approval_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    approval_id BIGINT NOT NULL COMMENT '工单ID（逻辑外键）',
    approver_id BIGINT NOT NULL COMMENT '审批人ID（逻辑外键）',
    action TINYINT NOT NULL COMMENT '操作类型: 0=SUBMIT, 1=APPROVE, 2=REJECT, 3=REEDIT, 4=REVOKE',
    comment VARCHAR(500) COMMENT '审批意见',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    KEY idx_approval_id (approval_id),
    KEY idx_approver_id (approver_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审批历史表';

-- 表单模板表
CREATE TABLE oa_form_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '表单名称',
    code VARCHAR(50) NOT NULL COMMENT '表单编码',
    description VARCHAR(500) COMMENT '表单描述',
    fields_config JSON NOT NULL COMMENT '字段配置',
    flow_config VARCHAR(50) COMMENT '流程配置',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态:0禁用,1启用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_code (code),
    KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表单模板表';
```

### 5.3 索引设计说明

| 表名 | 索引名 | 字段 | 类型 | 说明 |
|-----|-------|------|------|------|
| sys_user | uk_username | username | 唯一索引 | 用户名唯一 |
| sys_user | idx_role_id | role_id | 普通索引 | 按角色查询 |
| oa_approval | idx_applicant_id | applicant_id | 普通索引 | 查询我的申请 |
| oa_approval | idx_current_approver_id | current_approver_id | 普通索引 | 查询待办 |
| oa_approval | idx_status | status | 普通索引 | 按状态筛选 |
| oa_approval | idx_create_time | create_time | 普通索引 | 按时间排序 |

---

## 6. 接口设计规范

### 6.1 RESTful API规范

| 方法 | 用途 | 示例 |
|-----|------|------|
| GET | 查询资源 | GET /api/approvals |
| POST | 创建资源 | POST /api/approvals |
| PUT | 更新资源 | PUT /api/approvals/1 |
| DELETE | 删除资源 | DELETE /api/approvals/1 |
| POST | 执行操作 | POST /api/approvals/1/approve |

### 6.2 统一响应格式

```java
@Data
public class Result<T> {
    private Integer code;      // 状态码: 200成功, 其他失败
    private String message;    // 提示信息
    private T data;            // 响应数据
    private Long timestamp;    // 时间戳

    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }

    public static <T> Result<T> error(String message) {
        Result<T> result = new Result<>();
        result.setCode(500);
        result.setMessage(message);
        result.setTimestamp(System.currentTimeMillis());
        return result;
    }
}
```

### 6.3 核心接口清单

#### 认证接口

```
POST /api/auth/login          # 登录
POST /api/auth/logout         # 登出
POST /api/auth/refresh        # 刷新Token
GET  /api/auth/info           # 获取当前用户信息
```

#### 审批接口

```
GET    /api/approvals              # 获取审批列表
POST   /api/approvals              # 创建审批
GET    /api/approvals/{id}         # 获取审批详情
PUT    /api/approvals/{id}         # 更新审批
DELETE /api/approvals/{id}         # 删除审批
POST   /api/approvals/{id}/submit  # 提交审批
POST   /api/approvals/{id}/approve # 审批通过
POST   /api/approvals/{id}/reject  # 审批拒绝
GET    /api/approvals/todo         # 获取待办列表
GET    /api/approvals/done         # 获取已办列表
```

#### 用户接口

```
GET    /api/users           # 获取用户列表
POST   /api/users           # 创建用户
GET    /api/users/{id}      # 获取用户详情
PUT    /api/users/{id}      # 更新用户
DELETE /api/users/{id}      # 删除用户
```

#### 角色接口

```
GET    /api/roles           # 获取角色列表
POST   /api/roles           # 创建角色
PUT    /api/roles/{id}      # 更新角色
DELETE /api/roles/{id}      # 删除角色
```

---

## 7. 安全架构

### 7.1 认证方案

采用JWT（JSON Web Token）方案：

```
┌─────────────┐         ┌─────────────┐         ┌─────────────┐
│    客户端    │         │   服务端     │         │   数据库    │
└──────┬──────┘         └──────┬──────┘         └─────────────┘
       │                       │
       │ 1. 登录请求            │
       │ POST /login           │
       │ {username, password}  │
       │──────────────────────>│
       │                       │ 2. 验证凭证
       │                       │──────┐
       │                       │      │
       │                       │<─────┘
       │                       │ 3. 生成JWT
       │ 4. 返回Token          │
       │ {token, expires}      │
       │<──────────────────────│
       │                       │
       │ 5. 后续请求携带Token   │
       │ Authorization: Bearer │
       │──────────────────────>│ 6. 验证Token
       │                       │──────┐
       │ 7. 返回数据            │<─────┘
       │<──────────────────────│
```

### 7.2 JWT配置

```java
@Configuration
public class JwtConfig {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration:86400}")
    private Long expiration; // 默认24小时

    @Bean
    public JwtTokenUtil jwtTokenUtil() {
        return new JwtTokenUtil(secret, expiration);
    }
}
```

### 7.3 权限控制

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/approvals/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter(),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

---

## 8. 部署架构

### 8.1 开发环境

```
┌─────────────────────────────────────────────────────────────┐
│                      开发环境架构                            │
│                                                             │
│   ┌─────────────┐     ┌─────────────┐     ┌─────────────┐  │
│   │   VS Code   │     │  Terminal   │     │   Chrome    │  │
│   │   (IDEA)    │◄───►│   (Bash)    │◄───►│  (Browser)  │  │
│   └──────┬──────┘     └──────┬──────┘     └──────┬──────┘  │
│          │                   │                   │         │
│          └───────────────────┼───────────────────┘         │
│                              ▼                              │
│   ┌─────────────────────────────────────────────────────┐  │
│   │              Docker Desktop (可选)                   │  │
│   │   ┌─────────┐   ┌─────────┐   ┌─────────┐          │  │
│   │   │  MySQL  │   │  Java   │   │  Node   │          │  │
│   │   │  8.0    │   │   17    │   │   20    │          │  │
│   │   └─────────┘   └─────────┘   └─────────┘          │  │
│   └─────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 8.2 生产部署架构（简化版）

```
┌─────────────────────────────────────────────────────────────┐
│                      生产环境架构                            │
│                                                             │
│   ┌─────────────────────────────────────────────────────┐  │
│   │                   Nginx (80/443)                     │  │
│   │  • 静态资源服务 (Vue构建产物)                         │  │
│   │  • 反向代理 /api/* -> Spring Boot                   │  │
│   │  • SSL终止                                          │  │
│   └─────────────────────────┬───────────────────────────┘  │
│                             │                               │
│              ┌──────────────┴──────────────┐                │
│              ▼                              ▼                │
│   ┌─────────────────────┐      ┌─────────────────────┐      │
│   │   Spring Boot App   │      │       MySQL         │      │
│   │     (8080端口)      │◄────►│       8.0           │      │
│   │  • 内嵌Tomcat       │      │  • 数据持久化        │      │
│   │  • JVM参数优化      │      │  • 定时备份          │      │
│   └─────────────────────┘      └─────────────────────┘      │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 8.3 部署脚本示例

```bash
#!/bin/bash
# deploy.sh - 一键部署脚本

# 1. 构建前端
cd oa-frontend
npm install
npm run build
cd ..

# 2. 构建后端
cd oa-backend
mvn clean package -DskipTests
cd ..

# 3. 部署到服务器
scp -r oa-frontend/dist user@server:/var/www/oa/
scp oa-backend/target/oa-system.jar user@server:/opt/oa/

# 4. 重启服务
ssh user@server "sudo systemctl restart oa-service"
```

---

## 9. 技术难点与解决方案

### 9.1 难点一：动态表单数据存储

**问题描述**: 表单设计器允许用户自定义字段，如何存储结构不固定的表单数据？

**解决方案**:

1. **JSON字段存储**: 使用MySQL 8.0的JSON类型存储表单数据
2. **Schema校验**: 后端使用JSON Schema验证表单数据格式
3. **索引优化**: 对频繁查询的JSON字段建立虚拟列索引

```sql
-- JSON字段示例
CREATE TABLE oa_approval (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    form_data JSON,
    -- 虚拟列用于索引
    applicant_name VARCHAR(50) GENERATED ALWAYS AS (
        JSON_UNQUOTE(JSON_EXTRACT(form_data, '$.applicantName'))
    ) STORED,
    KEY idx_applicant_name (applicant_name)
);
```

### 9.2 难点二：状态机与业务逻辑解耦

**问题描述**: 状态转换时需要执行业务逻辑（如发送通知、记录日志），如何保持状态机纯粹？

**解决方案**:

1. **Action接口实现**: COLA状态机支持Action接口，将业务逻辑封装在Action中
2. **Spring事件机制**: 状态转换后发布Spring事件，由监听器异步处理
3. **事务控制**: 确保状态更新和业务逻辑在同一个事务中

```java
@Component
public class ApprovalEventListener {

    @EventListener
    @Async
    public void onApprovalApproved(ApprovalApprovedEvent event) {
        // 发送通知
        notificationService.notifyApplicant(event.getApprovalId(), "您的申请已通过");
        // 记录审计日志
        auditLogService.log(event);
    }
}
```

### 9.3 难点三：权限控制的细粒度管理

**问题描述**: 不同角色对同一资源有不同的操作权限，如何优雅实现？

**解决方案**:

1. **RBAC模型**: 基于角色的访问控制
2. **注解权限**: 使用自定义注解标记接口权限
3. **AOP拦截**: 通过AOP统一处理权限校验

```java
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {
    String value();
}

@RequirePermission("approval:approve")
@PostMapping("/{id}/approve")
public Result<Void> approve(@PathVariable Long id) {
    // 方法执行前自动校验权限
}
```

### 9.4 难点四：前后端数据一致性

**问题描述**: 前端展示的状态与后端实际状态可能出现不一致。

**解决方案**:

1. **乐观锁**: 使用version字段防止并发修改
2. **状态校验**: 状态转换前再次校验当前状态
3. **前端刷新**: 操作成功后重新拉取最新数据

```java
@Update("UPDATE oa_approval SET status = #{newStatus}, version = version + 1 " +
        "WHERE id = #{id} AND status = #{currentStatus} AND version = #{version}")
int updateStatus(@Param("id") Long id,
                 @Param("currentStatus") String currentStatus,
                 @Param("newStatus") String newStatus,
                 @Param("version") Integer version);
```

### 9.5 难点五：毕业设计的时间限制

**问题描述**: 毕业设计时间有限，如何快速完成开发？

**解决方案**:

1. **代码生成器**: 使用MyBatis-Plus生成基础CRUD代码
2. **组件复用**: 前端使用Tailwind CSS原子类，减少自定义CSS
3. **功能裁剪**: 优先实现核心流程，次要功能后续迭代
4. **Mock数据**: 开发阶段使用Mock数据，前后端并行开发

---

## 附录

### A. 技术栈版本锁定

```xml
<!-- pom.xml 关键依赖版本 -->
<properties>
    <java.version>17</java.version>
    <spring-boot.version>3.2.0</spring-boot.version>
    <cola.version>5.0.0</cola.version>
    <mybatis-plus.version>3.5.5</mybatis-plus.version>
    <mysql.version>8.0.33</mysql.version>
</properties>
```

```json
// package.json 关键依赖版本
{
  "dependencies": {
    "vue": "^3.4.0",
    "vue-router": "^4.2.0",
    "pinia": "^2.1.0",
    "axios": "^1.6.0"
  },
  "devDependencies": {
    "vite": "^5.0.0",
    "tailwindcss": "^3.4.0"
  }
}
```

### B. 参考资源

- COLA状态机: https://github.com/alibaba/COLA
- Spring Boot: https://spring.io/projects/spring-boot
- Vue 3: https://vuejs.org/
- Tailwind CSS: https://tailwindcss.com/

### C. 文档修订历史

| 版本 | 日期 | 修订内容 |
|-----|------|---------|
| v1.0 | 2026-03-24 | 初始版本发布 |

---

*文档结束*
