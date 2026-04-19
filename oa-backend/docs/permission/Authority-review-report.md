# OA审批系统权限控制机制审查报告

**审查日期**: 2026-04-14
**审查范围**: 前端权限控制实现 + 后端权限验证逻辑

---

## 一、现有权限控制体系概览

### 1. 用户角色定义与权限分配规则

**角色体系**：
系统采用三级角色架构：
- **系统管理员(admin)**：拥有 `all` 权限
- **部门经理(manager)**：拥有 `approval`, `user_view`, `report`, `apply`, `personal`
- **普通员工(employee)**：拥有 `apply`, `personal`

**数据库设计**：
- `sys_role` 表使用 JSON 字段 `permissions` 存储权限编码数组
- `sys_user` 表通过 `role_id` 外键单关联角色，**不支持多角色**

**后端解析**：
- `UserDetailsServiceImpl` 将 `sys_role.permissions` 解析为 `SimpleGrantedAuthority` 列表
- `UserDetailsImpl` 封装了用户完整信息（含角色名、部门ID、权限列表）

### 2. 权限粒度设计

| 粒度层级 | 实现状态 | 说明 |
|---------|---------|------|
| **功能级权限** | 前端缺失 / 后端部分缺失 | 前端菜单无权限过滤；后端仅审批模块有自定义权限检查，其他接口无 `@PreAuthorize` 控制 |
| **操作级权限** | 前端缺失 / 后端部分实现 | 前端所有用户可见全部操作按钮；后端审批操作（通过/拒绝）有4级权限检查 |
| **数据级权限** | 后端部分实现 | 审批模块的待办/已办/我的申请按用户ID隔离；但 `getById` 和 `list` 接口**无数据权限过滤** |

### 3. 权限验证流程

**登录认证**：
- 采用 **JWT + Spring Security 6** 无状态认证架构
- 流程：`POST /auth/login` → `AuthenticationManager` 校验 → 颁发 JWT → 后续请求携带 `Authorization: Bearer <token>`
- `JwtAuthenticationFilter` 提取并校验 Token，写入 `SecurityContextHolder`

**会话管理**：
- 前端 `auth.js` 将 token 和用户数据存储在 `localStorage`
- 后端 `SecurityConfig` 配置 `SessionCreationPolicy.STATELESS`
- **风险**：前端 `initAuth()` 仅从 localStorage 恢复状态，刷新页面时若 token 已过期但本地仍存在，会有短暂的安全窗口

**接口访问控制**：
- `SecurityConfig.java` 配置极其宽松：
  ```java
  .requestMatchers("/auth/**").permitAll()
  .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
  .anyRequest().authenticated()
  ```
  **所有认证用户均可访问所有非公开端点**，没有 URL 级别或方法级别的权限/角色控制。

---

## 二、前端权限控制审查

### 1. 路由权限守卫
`router/index.js` 仅实现了**是否登录**的判断：
```javascript
router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next('/login')
  } else {
    next()
  }
})
```
**问题**：任何已登录用户（包括普通员工）都可以通过直接输入 URL 访问 `/users`、`/roles`、`/form-designer` 等管理页面。

### 2. 菜单展示控制
`MainLayout.vue` 的侧边栏菜单**完全静态**，没有任何基于角色的 `v-if` 条件渲染：
- 普通员工登录后仍能看到"用户管理"、"角色权限"、"表单设计器"菜单
- 菜单徽章计数逻辑与权限无关

### 3. 操作按钮控制
- `ApprovalManage.vue`：列表中的"审批"按钮仅按 `status === 'processing'` 显示，**未校验当前用户是否为审批人**
- `ApprovalDetail.vue`：详情页的操作区同样只按状态显示，普通员工也能看到"通过审批"/"拒绝审批"按钮
- `UserManage.vue` / `RoleManage.vue`：添加/编辑/删除按钮对所有用户可见

### 4. 权限配置管理界面
`RoleManage.vue` 和 `UserManage.vue` 目前仍依赖 `stores/user.js` 中的**本地模拟数据**，没有对接后端真实接口。这导致：
- 角色权限修改仅在前端生效，刷新后可能丢失
- 没有权限变更的持久化和同步机制

---

## 三、后端权限验证逻辑审查

### 1. Spring Security 配置缺陷
`SecurityConfig.java` 存在重大权限控制缺口：
- **没有启用方法级安全注解**（缺少 `@EnableMethodSecurity`）
- **没有使用 `@PreAuthorize` / `@Secured`** 对 Controller 方法进行权限保护
- 所有通过 JWT 认证的用户都能调用 `/approvals/**` 的全部 14 个端点
- 没有用户管理/角色管理相关的后端 Controller（目前仅实现了 `AuthController` 和 `ApprovalController`）

### 2. 审批模块的权限控制（亮点）
审批模块实现了相对完善的 **4级权限层级**：

`ApprovalStateMachineHelper.checkApproverPermissionDetail()`：
```
Level 1: 直接审批 (DIRECT)
  └─ 当前用户 = current_approver_id 且拥有 approval:execute 权限

Level 2: 管理员代审批 (PROXY_ADMIN)
  └─ role = admin 且拥有 approval:execute:all 权限

Level 3: 部门经理代审批 (PROXY_MANAGER)
  └─ role = manager 且拥有 approval:execute:dept 权限
  └─ 且申请人与经理 dept_id 相同

Level 4: 无权限 (DENIED)
```

**前置权限校验**：
- `ApprovalServiceImpl.create()` 创建工单时校验指定审批人是否有 `approval:execute` 权限
- `ApprovalServiceImpl.update()` 更新审批人时同样校验

**数据隔离**：
- `getTodoList`：按 `current_approver_id + PROCESSING` 查询
- `getDoneList`：按审批历史中的 `approver_id` 查询
- `getMyApprovals`：按 `applicant_id` 查询
- `getDashboardStatistics`：按当前用户ID多维度统计

### 3. 数据权限漏洞
**严重问题**：
- `ApprovalController.getById()` 和 `ApprovalController.list()` **没有任何数据权限校验**
- 任何认证用户都能通过 `GET /approvals/{id}` 查看任意工单的详情（包括其他部门、其他员工的敏感申请内容）
- `GET /approvals` 列表查询同样返回全量数据

### 4. 审计日志记录
**现状**：
- 审批历史记录在 `oa_approval_history` 表中，包含 `is_proxy`、`approval_type`、`original_approver_id` 等代审批审计字段
- `ApprovalStateMachineHelper.saveHistory()` 自动记录每次状态变更
- 代审批场景有专门的 Slf4j 审计日志输出（`[代审批审计]` 前缀）

**缺失**：
- **没有独立的系统审计日志表**（如 `sys_audit_log`）
- 没有记录用户登录/登出、权限变更、系统配置修改等安全事件
- 审计日志仅依赖应用日志文件，未持久化到数据库，不便于查询和合规审计

---

## 四、安全性分析

### 高风险项

| 风险编号 | 风险描述 | 影响 |
|---------|---------|------|
| **R1** | 后端接口缺少方法级权限控制，所有认证用户可调用任意管理接口 | 越权操作 |
| **R2** | `GET /approvals/{id}` 和 `GET /approvals` 无数据权限过滤 | 敏感数据泄露 |
| **R3** | 前端无任何角色权限控制，所有菜单和按钮对所有用户可见 | 用户体验差、易被绕过 |
| **R4** | 用户/角色管理页面使用本地模拟数据，未对接后端真实权限系统 | 权限配置不生效 |
| **R5** | 没有登录失败次数限制、验证码、密码复杂度强制校验 | 暴力破解风险 |

### 中低风险项

| 风险编号 | 风险描述 | 影响 |
|---------|---------|------|
| **R6** | 用户表仅支持单角色（`role_id`），无多角色和角色继承机制 | 扩展性受限 |
| **R7** | 没有独立的审计日志表和查询接口 | 合规性不足 |
| **R8** | `SecurityConfig` 禁用了 CSRF（单体应用+Cookie场景下有风险） | CSRF 攻击（当前 JWT 头方式风险较低） |

---

## 五、合理性、可维护性与用户体验分析

### 合理性
- **审批状态机的4级权限设计较为合理**，支持直接审批和代审批场景，满足实际业务需求
- **RBAC 模型选择恰当**，符合中小型企业应用规模
- **不合理之处**：权限编码设计过于扁平（`all`, `approval`, `apply` 等），缺少模块:操作 的细分命名规范（除审批模块外）

### 可维护性
- 权限校验逻辑分散：
  - Spring Security 配置层：几乎空白
  - Controller 层：无权限注解
  - Service 层：审批模块有大量自定义权限代码
  - 状态机辅助类：审批权限核心逻辑
  - **问题**：权限校验没有统一抽象，新增模块时需要重复实现
- 前端没有封装统一的权限判断指令/组件（如 `v-permission`）

### 用户体验
- **正面**：登录流程简洁，JWT 自动续期/过期跳转处理在 `api/config.js` 中有实现
- **负面**：普通员工登录后仍能看到大量无权限的管理菜单和按钮，点击后可能因后端拒绝而报错，体验不佳

---

## 六、优化建议

### 后端接口权限控制（高优先级）
- 在 `SecurityConfig` 中启用 `@EnableMethodSecurity`
- 为 Controller 方法添加 `@PreAuthorize` 注解：
  ```java
  @PreAuthorize("hasAuthority('user_manage') or hasAuthority('all')")
  @GetMapping("/users")
  public Result<...> listUsers() { ... }
  ```
- 为审批列表/详情接口补充数据权限过滤：
  - 普通员工：只能查看 `applicant_id = 自己` 的工单
  - 部门经理：可查看本部门工单 + 需要自己审批的工单
  - 管理员：可查看全部

### 前端权限控制（高优先级）
- 扩展 `auth.js` 存储当前用户的 `permissions` 数组
- 封装 `v-permission` 自定义指令和 `hasPermission()` 工具函数
- 在 `MainLayout.vue` 中根据角色动态过滤菜单项
- 在 `ApprovalManage.vue` / `ApprovalDetail.vue` 中根据用户权限和工单状态控制按钮显示
- 在路由守卫中添加角色/权限校验

### 完善用户与角色管理后端（高优先级）
- 实现 `UserController` 和 `RoleController`
- 为角色管理接口添加 `role_manage` 权限校验
- 为用户管理接口添加 `user_manage` 权限校验

### 建立统一审计日志机制（中优先级）
- 新增 `sys_audit_log` 表，记录：操作人、操作类型、目标对象、变更前后值、IP、时间戳
- 使用 AOP 拦截 `@AuditLog` 注解，自动记录关键操作
- 增加登录日志（成功/失败）、权限变更日志

### 安全加固（中优先级）
- 增加登录失败次数限制（如 5 次/15 分钟）
- 增加密码复杂度校验（后端 DTO + 前端表单）
- Token 过期后主动调用后端 `/auth/refresh` 或清除本地状态
- 考虑为敏感操作（如删除用户、重置密码）增加二次确认或操作密码

### 权限模型扩展（低优先级）
- 评估是否需要从单角色升级为 **用户-角色-权限** 多对多关系
- 增加数据权限规则配置表（如 `sys_data_permission`），将数据权限从硬编码改为可配置

---

## 七、总结

该OA系统的权限控制呈现 **"后端审批模块相对完善，整体架构严重缺失"** 的特点：

- **优点**：审批状态机的4级权限检查（直接审批/管理员代审批/部门经理代审批/拒绝）设计合理，数据隔离在待办/已办/我的申请维度实现较好，审计追踪在审批历史层面有完整记录。
- **缺点**：前端几乎没有任何权限控制；后端除审批业务外，接口级和方法级权限控制完全空白；缺少用户/角色管理后端；没有独立的系统审计日志表；数据权限在详情/列表查询中存在漏洞。

**建议优先修复 R1~R4 的高风险项**，以确保系统具备基本的安全可用性。

---

*报告生成时间: 2026-04-14*
