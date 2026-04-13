# CLAUDE.md

本文件为 Claude Code (claude.ai/code) 提供在本仓库中工作的指导。

# ⚠️ 重要提示（必须遵守）

在编写任何代码之前，AI 必须**完整阅读**以下文件：
- `memory-bank/architecture.md`
- `memory-bank/product-design-document.md`

每完成一个重大功能或里程碑后，必须**更新** `memory-bank/architecture.md` 以反映最新设计。

## 项目概述

这是**工单流程自动化系统 (OA Approval System)** - 一个企业审批流程的工作流自动化系统。它是一个毕业设计项目，实现了支持自定义表单和状态机驱动的审批流的工单/审批工作流系统。

该项目目前包含一个完整的 **Vue 3 前端原型**，具有完整的 UI/UX。后端实现按照技术设计文档进行规划。

## 代码组织规范

### 前端
- 每个 Vue 组件（`.vue`）应遵循 **单一职责原则**，单个组件文件行数建议不超过 300 行。
- 复杂页面（如审批管理）必须拆分为子组件，存放在该页面目录下的 `components/` 文件夹中。
- 业务逻辑应抽取到 **Pinia stores** 或 **composables**（`src/composables/`），避免在组件内编写大量复杂逻辑。
- 禁止将多个不同视图的代码放在同一个 `.vue` 文件中。

#### 前后端分离原则
在进行前端代码改动时，必须严格遵循前后端分离的架构原则：
- 前端仅负责用户界面（UI）的呈现、用户交互逻辑的处理、客户端状态管理以及数据展示等前端专属职责
- **严禁**在前端代码中包含任何本应属于后端的业务逻辑处理、数据持久化操作、核心算法实现或敏感信息处理等功能
- 所有数据交互必须通过调用后端提供的标准API接口来完成
- 保持前后端之间清晰的职责边界和通信规范

### 后端（待实现）
- 按功能模块分包（如 `approval`、`user`、`form`），每个模块包含 `controller`、`service`、`mapper`、`entity` 等。
- Service 层方法应保持简洁，复杂业务流程应拆分到多个私有方法或独立组件中。
- 禁止在 Controller 中编写业务逻辑，禁止在 Service 中编写 SQL 拼接。

### 通用
- 每个文件应只包含一个类/主要组件（测试文件除外）。
- 定期检查大文件（>500 行），主动进行拆分重构。

## 架构

### 项目结构

```
OASystem/
├── oa-frontend/              # Vue 3 前端应用
│   ├── src/
│   │   ├── views/            # 页面组件 (Vue SFCs)
│   │   ├── layouts/          # 布局组件
│   │   ├── router/           # Vue Router 配置
│   │   ├── stores/           # Pinia 状态管理
│   │   └── assets/           # 静态资源
│   ├── package.json
│   ├── vite.config.js
│   └── tailwind.config.js
├── product-design-document.md   # 产品设计 (v1.1, 中文)
└── tech-stack.md               # 技术架构 (中文)
```

### 技术栈

**前端 (已实现)**:
- Vue 3.4 with Composition API (`<script setup>`)
- Vue Router 4 for SPA routing
- Pinia 2 for state management
- Tailwind CSS 3.4 for styling
- Vite 5 for build tooling
- Axios for HTTP requests (configured but using mock data)

**计划中的后端**:
- Spring Boot 3.2
- COLA StateMachine 5.x (Alibaba's state machine engine for workflow)
- MySQL 8.0
- MyBatis-Plus

### 状态管理架构

Pinia stores 按领域组织:
- `stores/auth.js` - 认证状态 (JWT token, current user)
- `stores/approval.js` - 审批工作流状态和模拟数据
- `stores/user.js` - 用户和角色管理

### 审批工作流状态 (v1.1)

系统使用状态机，具有以下状态和转换:

```
DRAFT →(SUBMIT)→ PROCESSING →(APPROVE)→ APPROVED →(REEDIT)→ DRAFT
                    |
                    └→(REJECT)→ RETURNED →(REEDIT)→ DRAFT
```

状态: `DRAFT`, `PROCESSING`, `APPROVED`, `RETURNED`, `REVOKED`
事件: `SUBMIT`, `APPROVE`, `REJECT`, `REEDIT`, `REVOKE`

## 开发命令

所有命令应在 `oa-frontend/` 目录下运行:

```bash
# 安装依赖
npm install

# 启动开发服务器 (运行在 3000 端口)
npm run dev

# 生产构建
npm run build

# 预览生产构建
npm run preview
```

### 开发服务器配置

- 端口: 3000 (在 `vite.config.js` 中配置)
- 启动时自动打开浏览器
- 路径别名 `@/` 映射到 `src/`

## 关键文件参考

### 配置
- `oa-frontend/vite.config.js` - Vite 构建配置，包含 `@/` 别名
- `oa-frontend/tailwind.config.js` - 自定义调色板 (primary, success, warning, danger)
- `oa-frontend/postcss.config.js` - PostCSS with Tailwind and autoprefixer

### 路由
- `oa-frontend/src/router/index.js` - 路由定义，包含认证守卫

### 状态管理
- `oa-frontend/src/stores/auth.js` - 模拟认证 (admin/manager/user 测试账号)
- `oa-frontend/src/stores/approval.js` - 模拟审批数据，支持状态过滤
- `oa-frontend/src/stores/user.js` - 用户和角色管理

### 视图 (页面)
- `Dashboard.vue` - 统计仪表盘，带待办列表
- `Login.vue` - 认证页面，带测试账号快捷入口
- `ApprovalManage.vue` - 审批列表，带筛选和操作
- `ApprovalDetail.vue` - 单个审批详情，带操作按钮
- `ApprovalCreate.vue` - 新建审批表单
- `FormDesigner.vue` - 可视化表单设计器 (拖拽风格)
- `UserManage.vue` / `RoleManage.vue` - RBAC 管理
- `TodoList.vue` / `DoneList.vue` - 任务视图

## 设计系统

Tailwind CSS 配置了自定义调色板:
- **primary**: 蓝色调色板 (500: #3b82f6)
- **success**: 绿色调色板 (500: #22c55e)
- **warning**: 琥珀色调色板 (500: #f59e0b)
- **danger**: 红色调色板 (500: #ef4444)

跨组件使用的通用 CSS 类:
- `card` - 卡片容器样式
- `btn`, `btn-primary`, `btn-secondary`, `btn-danger` - 按钮变体
- `input` - 表单输入样式
- `badge`, `badge-primary`, `badge-success`, `badge-warning`, `badge-danger` - 状态徽章

## 模拟数据与测试

前端目前使用模拟数据:
- **测试账号**: admin/admin123, manager/manager123, user/user123
- **模拟审批**: 5 条各种状态的示例审批工单
- **模拟用户**: 5 个不同角色的用户

认证使用 localStorage 持久化模拟。

## 文档

- `product-design-document.md` - 完整产品需求，中文 (v1.1)
- `tech-stack.md` - 技术架构和 COLA StateMachine 集成方案

两份文档均为中文，包含以下详细规范:
- 用户角色 (Admin, Manager, Employee)
- RBAC 权限
- 数据库 schema
- API 规范
- 状态机设计

## 开发注意事项

1. **目前无后端** - 前端使用模拟数据存储。后端实现应遵循 `tech-stack.md` 中的规范。

2. **状态机集成** - 审批工作流应使用阿里巴巴的 COLA StateMachine (在 `tech-stack.md` 第 4 节中有文档说明)。

3. **表单设计器** - 目前是 UI 原型。审批中的 `form_data` 字段应根据表单模板配置存储 JSON。

4. **响应式设计** - UI 使用 Tailwind 响应式类 (sm:, md:, lg: 前缀)。
