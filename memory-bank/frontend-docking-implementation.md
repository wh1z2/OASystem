# 阶段七：前端接口对接实施计划

> 本文档基于 `implementation-plan.md` 框架，专门为阶段七（前端接口对接）制定详细实施计划
>
> **文档版本**: v1.0
> **编写日期**: 2026-04-13
> **关联文档**: [frontend-docking-list.md](./frontend-docking-list.md)

---

## 目录

1. [阶段概述](#1-阶段概述)
2. [对接目标](#2-对接目标)
3. [前置依赖](#3-前置依赖)
4. [实施步骤](#4-实施步骤)
5. [接口规范](#5-接口规范)
6. [数据映射规范](#6-数据映射规范)
7. [测试策略](#7-测试策略)
8. [风险与应对](#8-风险与应对)
9. [验收标准](#9-验收标准)

---

## 阶段概述

### 阶段定位

阶段七"前端接口对接"是核心功能开发的最后一个阶段，位于后端核心接口开发完成之后。本阶段的主要任务是将前端 Vue 应用从 Mock 数据切换到真实后端接口，确保前后端数据流通顺畅、功能完整可用。

### 当前状态

根据 `frontend-docking-list.md` 统计：

| 模块 | 总接口数 | 已对接 | 待修复/对接 | 对接率 |
|------|----------|--------|-------------|--------|
| 认证模块 | 3 | 3 | 0 | 100% |
| 审批工单模块 | 14 | 14 | 0 | 100% |
| 个人中心模块 | 4 | 1 | 3 | 25% |
| **阶段七核心范围** | **21** | **18** | **3** | **85.7%** |

### 阶段目标

- 修复已知前端 Bug（ApprovalCreate.vue 方法调用错误）
- 完成个人中心模块接口对接（Profile.vue）
- 验证所有已对接接口的实际连通性
- 确保前后端数据格式一致性
- 完善错误处理和用户反馈机制

---

## 对接目标

### 功能修复目标

| 优先级 | 目标描述 | 涉及文件 | 预计工作量 |
|--------|----------|----------|------------|
| P0 | 修复 ApprovalCreate.vue 提交方法调用错误 | `src/views/ApprovalCreate.vue` | 2h |
| P0 | 对接更新个人信息接口 | `src/views/Profile.vue` | 2h |
| P0 | 对接修改密码接口 | `src/views/Profile.vue` | 2h |
| P1 | 完善前端统一错误处理 | `src/api/config.js` | 4h |
| P1 | 优化加载状态和空状态展示 | 多个视图文件 | 4h |

### 接口对接清单（阶段七范围内）

| 序号 | 接口路径 | 方法 | 功能 | 状态 | 优先级 |
|------|----------|------|------|------|--------|
| 1 | `/api/approvals` | POST | 创建审批工单 | 已对接待验证 | P0 |
| 2 | `/api/users/profile` | PUT | 更新个人信息 | 待对接 | P0 |
| 3 | `/api/users/password` | PUT | 修改密码 | 待对接 | P0 |

---

## 3. 前置依赖

### 后端依赖

在开始本阶段前，必须确保以下后端接口已完成开发并通过测试：

- [x] `POST /api/approvals` - 创建审批工单
- [x] `PUT /api/users/profile` - 更新个人信息
- [x] `PUT /api/users/password` - 修改密码

### 3.2 环境依赖

| 依赖项 | 版本要求 | 验证命令 |
|--------|----------|----------|
| Node.js | >= 20.0.0 | `node -v` |
| npm | >= 10.0.0 | `npm -v` |
| 后端服务 | 正常运行 | `curl http://localhost:8080/actuator/health` |

### 前端配置检查

确保 `oa-frontend/.env.development` 配置正确：

```bash
# API 基础路径
VITE_API_BASE_URL=/api

# 开发服务器代理目标
VITE_PROXY_TARGET=http://localhost:8080
```

---

## 实施步骤

### 步骤一：环境验证与配置检查

**执行动作**:
1. 确认后端服务已启动并监听在预期端口（默认 8080）
2. 检查前端开发服务器代理配置
3. 验证前端可以访问后端健康检查接口

**预期结果**:
- 前端开发服务器正常启动（默认端口 3000）
- 代理配置正确，API 请求可转发到后端

**验证方法**:
```bash
# 1. 启动前端开发服务器
cd oa-frontend
npm run dev

# 2. 测试代理是否工作
# 在浏览器开发者工具 Network 面板中查看 API 请求
# 预期：请求发送到 http://localhost:3000/api/xxx
# 实际：转发到 http://localhost:8080/api/xxx
```

**验证记录**:
```
验证项目: 步骤一 - 环境验证与配置检查
验证日期:
验证人:
验证结果:
问题记录:
```

---

### 步骤二：修复 ApprovalCreate.vue Bug

**执行动作**:
1. 打开 `src/views/ApprovalCreate.vue` 文件
2. 定位到 `handleSubmit` 函数（约第 188 行）
3. 将 `approvalStore.addApproval()` 修改为 `approvalStore.createApproval()`
4. 调整表单字段与后端 API 匹配

**代码修复示例**:
```javascript
// 修复前（错误）
function handleSubmit() {
  const approval = approvalStore.addApproval({  // ❌ 方法不存在
    ...form.value,
    applicant: authStore.currentUser?.name || '未知用户',
    applicantId: authStore.currentUser?.id,
    department: authStore.currentUser?.department || '未知部门',
    currentApprover: '张经理'
  })
  router.push(`/approval/detail/${approval.id}`)
}

// 修复后（正确）
async function handleSubmit() {
  try {
    // 构建符合后端 API 格式的请求数据
    const approvalData = {
      title: form.value.title,
      type: typeMap[form.value.type],  // 前端字符串转后端数值
      priority: priorityMap[form.value.priority],
      content: form.value.content,
      formData: form.value.formData || {}
    }

    const result = await approvalStore.createApproval(approvalData)

    if (result.success) {
      // 使用返回的数据中的 ID
      router.push(`/approval/detail/${result.data.id}`)
    } else {
      // 显示错误提示
      alert('创建失败：' + result.message)
    }
  } catch (error) {
    console.error('创建审批失败:', error)
    alert('创建失败，请稍后重试')
  }
}
```

**类型和优先级映射**:
```javascript
// 前端类型值 -> 后端类型值
const typeMap = {
  'leave': 1,     // 请假
  'expense': 2,   // 报销
  'purchase': 3,  // 采购
  'overtime': 4,  // 加班
  'travel': 5     // 出差
}

// 前端优先级 -> 后端优先级
const priorityMap = {
  'low': 0,       // 低
  'normal': 1,    // 普通
  'high': 2       // 紧急
}
```

**预期结果**:
- 创建审批页面可正常提交数据到后端
- 创建成功后跳转到审批详情页
- 创建失败时显示友好错误提示

**验证方法**:
1. 打开创建审批页面
2. 填写表单数据
3. 点击提交按钮
4. 验证：
   - Network 面板显示 POST 请求发送到 `/api/approvals`
   - 请求体格式正确
   - 后端返回 200 状态码和数据
   - 页面成功跳转到详情页

**验证记录**:
```
验证项目: 步骤二 - 修复 ApprovalCreate.vue Bug
验证日期:
验证人:
验证结果:
问题记录:
```

---

### 步骤三：对接更新个人信息接口

**执行动作**:
1. 在 `src/stores/user.js` 中添加 `updateProfile` 方法
2. 修改 `src/views/Profile.vue` 中的保存逻辑
3. 将 alert 提示替换为实际 API 调用

**后端接口信息**:
```
接口路径: /api/users/profile
请求方法: PUT
Content-Type: application/json

请求体:
{
  "name": "string",      // 姓名
  "phone": "string",     // 电话
  "email": "string",     // 邮箱
  "department": "string" // 部门
}

响应格式:
{
  "code": 200,
  "message": "success",
  "data": null,
  "timestamp": 1234567890
}
```

**前端实现**:
```javascript
// src/stores/user.js
async function updateProfile(profileData) {
  try {
    await apiClient.put('/users/profile', profileData)
    // 更新本地存储的用户信息
    const currentUser = JSON.parse(localStorage.getItem('user') || '{}')
    localStorage.setItem('user', JSON.stringify({
      ...currentUser,
      ...profileData
    }))
    return { success: true }
  } catch (error) {
    return { success: false, message: error.message }
  }
}

// src/views/Profile.vue
async function saveProfile() {
  const result = await userStore.updateProfile(form.value)
  if (result.success) {
    alert('保存成功！')
    // 刷新用户信息
    await authStore.fetchCurrentUser()
  } else {
    alert('保存失败：' + result.message)
  }
}
```

**预期结果**:
- 个人中心页面可正常更新用户信息
- 更新成功后本地存储同步更新
- 更新失败时显示错误提示

**验证方法**:
1. 打开个人中心页面
2. 修改个人信息（如姓名、电话）
3. 点击保存按钮
4. 验证：
   - Network 面板显示 PUT 请求发送到 `/api/users/profile`
   - 请求体包含修改后的数据
   - 后端返回 200 状态码
   - 页面显示保存成功提示
   - 刷新页面后修改仍然生效

**验证记录**:
```
验证项目: 步骤三 - 对接更新个人信息接口
验证日期:
验证人:
验证结果:
问题记录:
```

---

### 步骤四：对接修改密码接口

**执行动作**:
1. 在 `src/stores/user.js` 中添加 `changePassword` 方法
2. 修改 `src/views/Profile.vue` 中的修改密码逻辑
3. 实现密码修改表单验证和 API 调用

**后端接口信息**:
```
接口路径: /api/users/password
请求方法: PUT
Content-Type: application/json

请求体:
{
  "oldPassword": "string",  // 原密码
  "newPassword": "string"   // 新密码
}

响应格式（成功）:
{
  "code": 200,
  "message": "success",
  "data": null,
  "timestamp": 1234567890
}

响应格式（失败-原密码错误）:
{
  "code": 400,
  "message": "原密码不正确",
  "data": null,
  "timestamp": 1234567890
}
```

**前端实现**:
```javascript
// src/stores/user.js
async function changePassword(passwordData) {
  try {
    await apiClient.put('/users/password', passwordData)
    return { success: true }
  } catch (error) {
    return {
      success: false,
      message: error.response?.data?.message || '修改密码失败'
    }
  }
}

// src/views/Profile.vue
const passwordForm = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

async function handleChangePassword() {
  // 前端验证
  if (!passwordForm.value.oldPassword) {
    alert('请输入原密码')
    return
  }
  if (!passwordForm.value.newPassword) {
    alert('请输入新密码')
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    alert('两次输入的新密码不一致')
    return
  }
  if (passwordForm.value.newPassword.length < 6) {
    alert('新密码长度不能少于6位')
    return
  }

  // 调用 API
  const result = await userStore.changePassword({
    oldPassword: passwordForm.value.oldPassword,
    newPassword: passwordForm.value.newPassword
  })

  if (result.success) {
    alert('密码修改成功！')
    // 清空表单
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  } else {
    alert('密码修改失败：' + result.message)
  }
}
```

**预期结果**:
- 用户可正常修改密码
- 原密码验证失败时给出明确提示
- 新密码符合安全要求（长度、复杂度）
- 修改成功后清空表单

**验证方法**:
1. 打开个人中心页面
2. 切换到修改密码标签
3. 输入错误的原密码，验证提示
4. 输入正确的原密码，但两次新密码不一致，验证提示
5. 输入正确的数据，验证修改成功
6. 使用新密码重新登录，验证生效

**验证记录**:
```
验证项目: 步骤四 - 对接修改密码接口
验证日期:
验证人:
验证结果:
问题记录:
```

---

### 步骤五：完善错误处理机制

**执行动作**:
1. 增强 `src/api/config.js` 中的响应拦截器
2. 统一处理常见 HTTP 错误状态码
3. 添加网络错误和超时处理
4. 实现全局错误提示组件（可选）

**响应拦截器增强**:
```javascript
// src/api/config.js
apiClient.interceptors.response.use(
  (response) => {
    // 统一处理响应格式
    const { code, message, data } = response.data
    if (code !== 200) {
      // 业务逻辑错误
      return Promise.reject(new Error(message || '请求失败'))
    }
    return data
  },
  (error) => {
    // 网络或服务器错误
    if (error.response) {
      // 服务器返回了错误响应
      const { status, data } = error.response
      switch (status) {
        case 400:
          error.message = data.message || '请求参数错误'
          break
        case 401:
          error.message = '登录已过期，请重新登录'
          // 清除本地认证信息
          localStorage.removeItem('token')
          localStorage.removeItem('user')
          // 跳转到登录页
          window.location.href = '/login'
          break
        case 403:
          error.message = '没有权限执行此操作'
          break
        case 404:
          error.message = '请求的资源不存在'
          break
        case 500:
          error.message = '服务器内部错误，请稍后重试'
          break
        default:
          error.message = data.message || `请求失败(${status})`
      }
    } else if (error.request) {
      // 请求发送但没有收到响应
      error.message = '网络连接失败，请检查网络设置'
    } else {
      // 请求配置错误
      error.message = '请求配置错误'
    }
    return Promise.reject(error)
  }
)
```

**预期结果**:
- 所有 API 错误都有统一的错误提示
- 401 未授权错误自动跳转到登录页
- 网络错误给出友好提示

**验证方法**:
1. 模拟各种错误场景（断网、服务器错误、401等）
2. 验证错误提示是否正确显示

**验证记录**:
```
验证项目: 步骤五 - 完善错误处理机制
验证日期:
验证人:
验证结果:
问题记录:
```

---

### 步骤六：全面功能验证

**执行动作**:
1. 按照测试用例列表逐项验证
2. 检查所有已对接接口的实际连通性
3. 验证数据流转的完整性

**验证清单**:

| 验证项 | 操作步骤 | 预期结果 | 实际结果 |
|--------|----------|----------|----------|
| 登录流程 | 1. 输入账号密码<br>2. 点击登录 | 登录成功，跳转到首页 | 符合预期 |
| 审批列表 | 1. 打开审批管理页面 | 显示后端真实数据 | 符合预期 |
| 审批筛选 | 1. 选择筛选条件<br>2. 点击搜索 | 筛选结果正确 | 符合预期 |
| 创建审批 | 1. 填写表单<br>2. 提交 | 创建成功，跳转到详情 | 符合预期 |
| 审批通过 | 1. 打开审批详情<br>2. 点击通过 | 状态变为已通过 | 符合预期 |
| 审批拒绝 | 1. 打开审批详情<br>2. 点击拒绝 | 状态变为已打回 | 符合预期 |
| 待办列表 | 1. 打开待办页面 | 显示当前用户待办 | 部分符合预期但有异常 |
| 已办列表 | 1. 打开已办页面 | 显示已处理工单 | 符合预期 |
| 更新资料 | 1. 修改个人信息<br>2. 保存 | 更新成功 | 后端逻辑未完成 |
| 修改密码 | 1. 输入原密码和新密码<br>2. 提交 | 密码修改成功 | 后端逻辑未完成 |

**验证记录**:
```
验证项目: 步骤六 - 全面功能验证
验证日期:
验证人:
验证结果:
问题记录:
```

---

## 5. 接口规范

### 请求规范

**基础 URL**: `/api`

**请求头**:
```
Content-Type: application/json
Authorization: Bearer {token}
```

**标准请求体格式**:
```json
{
  "field1": "value1",
  "field2": "value2"
}
```

### 响应规范

**成功响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": { },
  "timestamp": 1234567890
}
```

**失败响应**:
```json
{
  "code": 400,
  "message": "错误描述信息",
  "data": null,
  "timestamp": 1234567890
}
```

**分页响应**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [ ],
    "total": 100,
    "current": 1,
    "size": 10
  },
  "timestamp": 1234567890
}
```

### 阶段七涉及接口详情

#### 接口1: 创建审批工单

```
POST /api/approvals
```

**请求体**:
```json
{
  "title": "请假申请-张三",
  "type": 1,
  "priority": 1,
  "content": "因身体不适需要请假一天",
  "formData": {
    "leaveType": "sick",
    "startDate": "2026-04-15",
    "endDate": "2026-04-15",
    "days": 1
  }
}
```

**响应体**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "id": 123,
    "title": "请假申请-张三",
    "type": 1,
    "status": 0,
    "priority": 1,
    "applicantId": 1,
    "applicantName": "张三",
    "deptName": "技术部",
    "createTime": "2026-04-13 10:00:00"
  },
  "timestamp": 1234567890
}
```

#### 接口2: 更新个人信息

```
PUT /api/users/profile
```

**请求体**:
```json
{
  "name": "张三",
  "phone": "13800138000",
  "email": "zhangsan@example.com",
  "department": "技术部"
}
```

**响应体**:
```json
{
  "code": 200,
  "message": "success",
  "data": null,
  "timestamp": 1234567890
}
```

#### 接口3: 修改密码

```
PUT /api/users/password
```

**请求体**:
```json
{
  "oldPassword": "oldPass123",
  "newPassword": "newPass456"
}
```

**响应体（成功）**:
```json
{
  "code": 200,
  "message": "success",
  "data": null,
  "timestamp": 1234567890
}
```

**响应体（失败）**:
```json
{
  "code": 400,
  "message": "原密码不正确",
  "data": null,
  "timestamp": 1234567890
}
```

---

## 6. 数据映射规范

### 审批状态映射

| 后端数值 | 前端字符串 | 中文含义 |
|----------|------------|----------|
| 0 | `draft` | 草稿 |
| 1 | `processing` | 审批中 |
| 2 | `approved` | 已通过 |
| 3 | `returned` | 已打回 |
| 4 | `revoked` | 已撤销 |

**转换函数**:
```javascript
const statusMap = {
  0: 'draft',
  1: 'processing',
  2: 'approved',
  3: 'returned',
  4: 'revoked'
}

const statusMapReverse = {
  draft: 0,
  processing: 1,
  approved: 2,
  returned: 3,
  revoked: 4
}
```

### 审批类型映射

| 后端数值 | 前端字符串 | 中文含义 |
|----------|------------|----------|
| 1 | `leave` | 请假 |
| 2 | `expense` | 报销 |
| 3 | `purchase` | 采购 |
| 4 | `overtime` | 加班 |
| 5 | `travel` | 出差 |

**转换函数**:
```javascript
const typeMap = {
  1: 'leave',
  2: 'expense',
  3: 'purchase',
  4: 'overtime',
  5: 'travel'
}

const typeMapReverse = {
  leave: 1,
  expense: 2,
  purchase: 3,
  overtime: 4,
  travel: 5
}
```

### 优先级映射

| 后端数值 | 前端字符串 | 中文含义 |
|----------|------------|----------|
| 0 | `low` | 低 |
| 1 | `normal` | 普通 |
| 2 | `high` | 紧急 |

**转换函数**:
```javascript
const priorityMap = {
  0: 'low',
  1: 'normal',
  2: 'high'
}

const priorityMapReverse = {
  low: 0,
  normal: 1,
  high: 2
}
```

---

## 测试策略

### 测试范围

| 测试类型 | 覆盖内容 | 优先级 |
|----------|----------|--------|
| 单元测试 | Store 方法、工具函数 | P1 |
| 集成测试 | API 调用链、数据流转 | P0 |
| 功能测试 | 完整业务流程 | P0 |
| 兼容性测试 | 不同浏览器 | P2 |
| 性能测试 | 接口响应时间 | P1 |

### 测试用例

#### 用例1: 创建审批工单

| 项目 | 内容 |
|------|------|
| 用例编号 | TC-APPROVAL-001 |
| 用例名称 | 创建审批工单成功 |
| 前置条件 | 用户已登录，具有创建权限 |
| 测试步骤 | 1. 打开创建审批页面<br>2. 填写表单数据<br>3. 点击提交按钮 |
| 预期结果 | 1. 表单验证通过<br>2. POST 请求发送成功<br>3. 后端返回 200<br>4. 跳转到详情页 |
| 测试数据 | 标题：测试审批，类型：请假，优先级：普通 |

#### 用例2: 更新个人信息

| 项目 | 内容 |
|------|------|
| 用例编号 | TC-PROFILE-001 |
| 用例名称 | 更新个人信息成功 |
| 前置条件 | 用户已登录 |
| 测试步骤 | 1. 打开个人中心页面<br>2. 修改姓名为"李四"<br>3. 点击保存按钮 |
| 预期结果 | 1. PUT 请求发送成功<br>2. 后端返回 200<br>3. 显示保存成功提示<br>4. 刷新后信息保持 |
| 测试数据 | 姓名：李四 |

#### 用例3: 修改密码-原密码错误

| 项目 | 内容 |
|------|------|
| 用例编号 | TC-PASSWORD-001 |
| 用例名称 | 修改密码-原密码错误 |
| 前置条件 | 用户已登录 |
| 测试步骤 | 1. 打开个人中心页面<br>2. 输入错误的原密码<br>3. 输入新密码<br>4. 点击提交按钮 |
| 预期结果 | 1. PUT 请求发送成功<br>2. 后端返回 400<br>3. 显示"原密码不正确"提示 |
| 测试数据 | 原密码：wrongpass，新密码：newpass123 |

#### 用例4: 修改密码-成功

| 项目 | 内容 |
|------|------|
| 用例编号 | TC-PASSWORD-002 |
| 用例名称 | 修改密码成功 |
| 前置条件 | 用户已登录 |
| 测试步骤 | 1. 打开个人中心页面<br>2. 输入正确的原密码<br>3. 输入新密码<br>4. 点击提交按钮<br>5. 使用新密码登录 |
| 预期结果 | 1. PUT 请求发送成功<br>2. 后端返回 200<br>3. 显示修改成功提示<br>4. 使用新密码可正常登录 |
| 测试数据 | 原密码：正确密码，新密码：newpass123 |

### 测试工具

| 工具 | 用途 | 版本 |
|------|------|------|
| Chrome DevTools | 调试网络请求、查看响应 | 最新版 |
| Postman / Apifox | 单独测试后端接口 | - |
| Vitest | 前端单元测试 | ^1.0.0 |

---

## 风险与应对

| 风险编号 | 风险描述 | 影响程度 | 应对措施 |
|----------|----------|----------|----------|
| R001 | 后端接口返回格式与预期不符 | 高 | 1. 事先与后端确认接口规范<br>2. 在响应拦截器中增加格式校验<br>3. 做好默认值处理 |
| R002 | 跨域问题导致请求失败 | 中 | 1. 检查前端代理配置<br>2. 确认后端 CORS 配置正确<br>3. 使用浏览器插件临时绕过 |
| R003 | 后端接口性能不佳影响体验 | 中 | 1. 添加加载状态提示<br>2. 实现请求防抖/节流<br>3. 反馈给后端优化 |
| R004 | Token 过期处理不完善 | 高 | 1. 实现 Token 刷新机制<br>2. 401 错误自动跳转登录<br>3. 保存未提交的表单数据 |
| R005 | 数据类型转换错误 | 中 | 1. 使用 TypeScript 或 JSDoc<br>2. 增加类型检查单元测试<br>3. 前后端统一枚举定义 |

---

## 验收标准

### 功能验收

| 验收项 | 验收标准 | 验收方法 |
|--------|----------|----------|
| 创建审批 | 表单提交成功，数据保存到数据库 | 创建后刷新列表查看 |
| 更新资料 | 个人信息修改成功，刷新后保持 | 修改后刷新页面验证 |
| 修改密码 | 密码修改成功，新密码可登录 | 修改后重新登录验证 |
| 错误处理 | 各类错误有友好提示 | 模拟错误场景验证 |

### 性能验收

| 验收项 | 验收标准 | 验收方法 |
|--------|----------|----------|
| 接口响应 | 页面首屏加载 < 2s | Chrome DevTools Network |
| 操作响应 | 按钮点击到响应 < 500ms | 手动测试 |
| 并发处理 | 同时发起5个请求不报错 | 自动化测试脚本 |

### 代码验收

| 验收项 | 验收标准 |
|--------|----------|
| 代码规范 | 符合 ESLint 配置，无警告 |
| 类型安全 | 关键数据结构有类型定义 |
| 错误处理 | 所有 API 调用有 try-catch |
| 注释完整 | 复杂逻辑有注释说明 |

### 验收签字

| 角色 | 签字 | 日期 |
|------|------|------|
| 前端开发 | | |
| 后端开发 | | |
| 测试人员 | | |
| 项目负责人 | | |

---

## 附录

### 相关文档索引

- [产品设计文档](./product-design-document.md)
- [技术架构文档](./tech-stack.md)
- [接口对接清单](./frontend-docking-list.md)
- [实施计划总览](./implementation-plan.md)

### 快速命令参考

```bash
# 启动前端开发服务器
cd oa-frontend
npm run dev

# 构建生产包
npm run build

# 运行单元测试
npm run test:unit

# 代码检查
npm run lint
```

### 问题反馈模板

```
问题描述:
复现步骤:
1.
2.
3.

预期结果:
实际结果:
环境信息:
- 浏览器:
- 前端版本:
- 后端版本:

截图/日志:
```

---

*文档维护：前端开发团队*
*更新周期：每次接口对接完成后更新*
