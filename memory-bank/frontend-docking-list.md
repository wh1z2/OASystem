# 前端接口对接清单

> 本文档用于跟踪 OA Approval System 前端项目的接口对接进度
> 最后更新：2026-04-13

---

## 说明

- ✅ 表示已对接后端接口
- ⏳ 表示待对接后端接口（仍使用Mock数据）

---

## 一、认证模块 (Auth Module)

| 序号 | 功能描述 | 接口路径 | 请求方法 | 数据来源 | 备注 |
|------|----------|----------|----------|----------|------|
| 1 | 用户登录 | `/api/auth/login` | POST | ✅ 已对接 | 返回JWT Token和用户信息 |
| 2 | 获取当前用户信息 | `/api/auth/info` | GET | ✅ 已对接 | Token自动续期校验 |
| 3 | 用户登出 | 本地清除 | - | ✅ 已对接 | 清除localStorage中的token |

**涉及文件**：
- `src/stores/auth.js` - 认证状态管理
- `src/views/Login.vue` - 登录页面

---

## 二、审批工单模块 (Approval Module)

| 序号 | 功能描述 | 接口路径 | 请求方法 | 数据来源 | 备注 |
|------|----------|----------|----------|----------|------|
| 1 | 获取审批列表 | `/api/approvals` | GET | ✅ 已对接 | 支持分页、筛选、搜索 |
| 2 | 获取审批详情 | `/api/approvals/{id}` | GET | ✅ 已对接 | 包含完整的审批信息 |
| 3 | 创建审批工单 | `/api/approvals` | POST | ✅ 已对接 | 提交新审批申请 |
| 4 | 更新审批工单 | `/api/approvals/{id}` | PUT | ✅ 已对接 | 修改草稿状态工单 |
| 5 | 删除审批工单 | `/api/approvals/{id}` | DELETE | ✅ 已对接 | 删除草稿/已撤销工单 |
| 6 | 提交审批 | `/api/approvals/{id}/submit` | POST | ✅ 已对接 | 从草稿提交到审批流 |
| 7 | 审批通过 | `/api/approvals/{id}/approve` | POST | ✅ 已对接 | 审批人通过审批 |
| 8 | 审批拒绝 | `/api/approvals/{id}/reject` | POST | ✅ 已对接 | 审批人打回申请 |
| 9 | 重新编辑 | `/api/approvals/{id}/reedit` | POST | ✅ 已对接 | 打回后重新编辑 |
| 10 | 撤销申请 | `/api/approvals/{id}/revoke` | POST | ✅ 已对接 | 申请人主动撤销 |
| 11 | 获取待办列表 | `/api/approvals/todo` | GET | ✅ 已对接 | 当前用户的待审批列表 |
| 12 | 获取已办列表 | `/api/approvals/done` | GET | ✅ 已对接 | 当前用户已处理的审批 |
| 13 | 获取我的申请 | `/api/approvals/my` | GET | ✅ 已对接 | 当前用户提交的申请 |
| 14 | 获取审批历史 | `/api/approvals/{id}/history` | GET | ✅ 已对接 | 审批流程的历史记录 |

**涉及文件**：
- `src/stores/approval.js` - 审批状态管理（已全部对接）
- `src/views/ApprovalManage.vue` - 审批管理列表
- `src/views/ApprovalDetail.vue` - 审批详情页
- `src/views/ApprovalCreate.vue` - 创建审批页（需修复，见下方问题）
- `src/views/TodoList.vue` - 待办列表
- `src/views/DoneList.vue` - 已办列表
- `src/views/Dashboard.vue` - 仪表盘统计

**状态映射说明**：
```javascript
// 后端状态值 -> 前端状态
0: 'draft'       // 草稿
1: 'processing'  // 审批中
2: 'approved'    // 已通过
3: 'returned'    // 已打回
4: 'revoked'     // 已撤销
```

**类型映射说明**：
```javascript
// 后端类型值 -> 前端类型
1: 'leave'     // 请假
2: 'expense'   // 报销
3: 'purchase'  // 采购
4: 'overtime'  // 加班
5: 'travel'    // 出差
```

---

## 三、用户管理模块 (User Module)

| 序号 | 功能描述 | 接口路径 | 请求方法 | 数据来源 | 备注 |
|------|----------|----------|----------|----------|------|
| 1 | 获取用户列表 | `/api/users` | GET | ⏳ Mock数据 | src/stores/user.js 硬编码 |
| 2 | 获取用户详情 | `/api/users/{id}` | GET | ⏳ Mock数据 | 通过本地方法查找 |
| 3 | 创建用户 | `/api/users` | POST | ⏳ Mock数据 | 本地数组操作 |
| 4 | 更新用户 | `/api/users/{id}` | PUT | ⏳ Mock数据 | 本地数组操作 |
| 5 | 删除用户 | `/api/users/{id}` | DELETE | ⏳ Mock数据 | 本地数组操作 |
| 6 | 获取角色列表 | `/api/roles` | GET | ⏳ Mock数据 | src/stores/user.js 硬编码 |
| 7 | 创建角色 | `/api/roles` | POST | ⏳ Mock数据 | 本地数组操作 |
| 8 | 更新角色 | `/api/roles/{id}` | PUT | ⏳ Mock数据 | 本地数组操作 |
| 9 | 删除角色 | `/api/roles/{id}` | DELETE | ⏳ Mock数据 | 本地数组操作 |

**涉及文件**：
- `src/stores/user.js` - 用户/角色状态管理（**完全使用Mock数据**）
- `src/views/UserManage.vue` - 用户管理页面
- `src/views/RoleManage.vue` - 角色管理页面

**Mock数据位置**：
```javascript
// src/stores/user.js
const users = ref([...])  // 5条硬编码用户数据
const roles = ref([...])  // 3条硬编码角色数据
```

---

## 四、表单设计器模块 (Form Designer)

| 序号 | 功能描述 | 接口路径 | 请求方法 | 数据来源 | 备注 |
|------|----------|----------|----------|----------|------|
| 1 | 保存表单模板 | `/api/form-templates` | POST | ⏳ 前端模拟 | 仅使用alert('表单保存成功！') |
| 2 | 获取表单模板列表 | `/api/form-templates` | GET | ⏳ 未实现 | 当前为纯前端原型 |
| 3 | 获取表单模板详情 | `/api/form-templates/{id}` | GET | ⏳ 未实现 | 当前为纯前端原型 |
| 4 | 更新表单模板 | `/api/form-templates/{id}` | PUT | ⏳ 未实现 | 当前为纯前端原型 |
| 5 | 删除表单模板 | `/api/form-templates/{id}` | DELETE | ⏳ 未实现 | 当前为纯前端原型 |

**涉及文件**：
- `src/views/FormDesigner.vue` - 表单设计器页面（**纯前端原型**）

**当前状态**：表单设计器仅实现了UI原型，所有数据保存在前端内存中，刷新后丢失。

---

## 五、个人中心模块 (Profile)

| 序号 | 功能描述 | 接口路径 | 请求方法 | 数据来源 | 备注 |
|------|----------|----------|----------|----------|------|
| 1 | 获取个人信息 | 复用 `/api/auth/info` | GET | ✅ 已对接 | 从authStore获取 |
| 2 | 更新个人信息 | `/api/users/profile` | PUT | ⏳ 前端模拟 | 仅使用alert('保存成功！') |
| 3 | 修改密码 | `/api/users/password` | PUT | ⏳ 前端模拟 | 仅使用alert('密码修改成功！') |
| 4 | 上传头像 | `/api/users/avatar` | POST | ⏳ 未实现 | 按钮存在但无功能 |

**涉及文件**：
- `src/views/Profile.vue` - 个人中心页面

---

## 六、数据统计模块 (Dashboard)

| 序号 | 功能描述 | 接口路径 | 请求方法 | 数据来源 | 备注 |
|------|----------|----------|----------|----------|------|
| 1 | 获取统计数据 | 复用 `/api/approvals` | GET | ✅ 已对接 | 前端计算统计数据 |
| 2 | 获取待办列表 | 复用 `/api/approvals/todo` | GET | ✅ 已对接 | 取前5条展示 |
| 3 | 获取类型分布 | 复用 `/api/approvals` | GET | ✅ 已对接 | 前端统计各类型数量 |

**涉及文件**：
- `src/views/Dashboard.vue` - 仪表盘页面

---

## 七、已知问题与待修复项

### 1. ApprovalCreate.vue 功能异常

**问题描述**：
```javascript
// src/views/ApprovalCreate.vue:188
function handleSubmit() {
  const approval = approvalStore.addApproval({  // ❌ addApproval 方法不存在！
    ...form.value,
    applicant: authStore.currentUser?.name || '未知用户',
    applicantId: authStore.currentUser?.id,
    department: authStore.currentUser?.department || '未知部门',
    currentApprover: '张经理'
  })
  router.push(`/approval/detail/${approval.id}`)
}
```

**修复建议**：
- 应该使用 `approvalStore.createApproval()` 方法并对接后端
- 需要调整表单字段与后端API匹配

### 2. Profile.vue 保存功能未对接

**问题描述**：
- 保存个人信息仅使用 `alert('保存成功！')`，未调用API
- 修改密码仅进行前端校验，未调用API

### 3. FormDesigner.vue 完全未对接

**问题描述**：
- 表单设计器是纯粹的UI原型
- 保存功能仅使用 `alert('表单保存成功！')`
- 表单模板的数据结构未定义

---

## 八、待开发后端接口汇总

### 优先级：高
1. `POST /api/approvals` - 创建审批（ApprovalCreate.vue 需要）
2. `PUT /api/users/profile` - 更新个人信息
3. `PUT /api/users/password` - 修改密码

### 优先级：中
4. `GET /api/users` - 获取用户列表
5. `POST /api/users` - 创建用户
6. `PUT /api/users/{id}` - 更新用户
7. `DELETE /api/users/{id}` - 删除用户
8. `GET /api/roles` - 获取角色列表
9. `POST /api/roles` - 创建角色
10. `PUT /api/roles/{id}` - 更新角色
11. `DELETE /api/roles/{id}` - 删除角色

### 优先级：低
12. `POST /api/form-templates` - 保存表单模板
13. `GET /api/form-templates` - 获取表单模板列表
14. `GET /api/form-templates/{id}` - 获取表单模板详情
15. `PUT /api/form-templates/{id}` - 更新表单模板
16. `DELETE /api/form-templates/{id}` - 删除表单模板
17. `POST /api/users/avatar` - 上传头像

---

## 九、接口对接进度统计

| 模块 | 总接口数 | 已对接 | 未对接 | 对接率 |
|------|----------|--------|--------|--------|
| 认证模块 | 3 | 3 | 0 | 100% |
| 审批工单模块 | 14 | 14 | 0 | 100% |
| 用户管理模块 | 9 | 0 | 9 | 0% |
| 表单设计器模块 | 5 | 0 | 5 | 0% |
| 个人中心模块 | 4 | 1 | 3 | 25% |
| **总计** | **35** | **18** | **17** | **51.4%** |

---

## 十、快速参考

### API客户端配置
```javascript
// src/api/config.js
const apiClient = axios.create({
  baseURL: '/api',
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json'
  }
})
```

### 后端响应格式
```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "timestamp": 1234567890
}
```

### 分页响应格式
```json
{
  "records": [],
  "total": 100,
  "current": 1,
  "size": 10
}
```

---

*文档维护：前端开发团队*
*更新周期：每次接口对接完成后更新*
