# Token过期处理优化执行方案

> 文档版本: v1.0
> 编制日期: 2026-04-24
> 适用范围: OA审批系统前后端全栈

---

## 一、现有逻辑分析报告

### 1.1 前端现有实现

| 模块 | 文件路径 | 当前逻辑 | 问题点 |
|------|---------|---------|--------|
| Axios配置 | `oa-frontend/src/api/config.js` | 请求拦截器从localStorage读取token并注入`Authorization: Bearer`头；响应拦截器仅按HTTP状态码处理错误 | **致命缺陷**：后端`RestAuthenticationEntryPoint`返回HTTP 200 + 业务码`code=401`，但前端拦截器只判断`error.response.status === 401`（HTTP状态码），导致token过期永远走不进401分支，而是被当作普通业务错误（`Promise.reject(new Error(message))`）抛出，用户看到的是"请求失败"或模糊错误，而非明确的登录过期提示 |
| 认证状态 | `oa-frontend/src/stores/auth.js` | `fetchCurrentUser()`在异常时直接调用`logout()`并返回`null`，无任何用户提示 | 用户在token过期后首次打开页面或被路由守卫触发时，会被静默登出，无任何弹窗提示，体验差 |
| 路由守卫 | `oa-frontend/src/router/index.js` | 仅检查`isAuthenticated`（token是否存在），不校验token有效性或过期时间 | 本地存有过期token时，路由守卫仍会放行，直到API请求失败才暴露问题 |
| 用户活动 | 无 | 未监听任何用户操作事件 | 无法判断用户是否活跃，无法实现基于活动的续期 |
| 弹窗组件 | `oa-frontend/src/components/ConfirmDialog.vue` | 已存在标准弹窗组件 | token过期场景未使用该组件，而是直接`window.location.href = '/login'`强制跳转 |

### 1.2 后端现有实现

| 模块 | 文件路径 | 当前逻辑 | 问题点 |
|------|---------|---------|--------|
| Token有效期 | `oa-backend/src/main/resources/application.yml` | `jwt.expiration: 86400000`（24小时） | 与需求要求的30分钟严重不符；且与`JwtTokenUtil`默认值`1800000`（30分钟）及单元测试配置不一致，造成认知混乱 |
| Token生成 | `oa-backend/src/main/java/com/oasystem/util/JwtTokenUtil.java` | 使用jjwt生成HS256签名的JWT，claims包含userId、username | 无refresh token机制；无滑动过期支持 |
| Token验证 | `JwtTokenUtil.java` | `validateTokenWithReason()`可区分`token_expired`/`token_invalid`/`token_missing` | 验证通过后不会延长token有效期，纯无状态校验 |
| 认证过滤器 | `JwtAuthenticationFilter.java` | 从请求头提取token，验证通过后设置SecurityContext；失败时将错误码写入request attribute | 正确实现了错误分类传递 |
| 认证入口点 | `RestAuthenticationEntryPoint.java` | 从request attribute读取token_error，返回HTTP 200 + code 401 + 具体中文消息 | **设计争议**：Spring Security标准做法应返回HTTP 401，但本项目统一返回HTTP 200 + 业务码，前端拦截器未适配此约定 |
| 登录接口 | `AuthController.java` / `AuthServiceImpl.java` | 登录返回`LoginResponse`，包含`token`、`tokenType`、`expiresIn` | 仅返回单个token，无refresh token；`expiresIn`为相对毫秒数 |
| 全局异常 | `GlobalExceptionHandler.java` | 捕获各类异常转为统一Result格式 | 未对token过期做特殊全局处理，依赖`RestAuthenticationEntryPoint` |

### 1.3 现有问题根因分析

1. **前端错误提示不准确（显示网络错误而非token过期）**：
   - 根因：后端token过期响应格式为`HTTP 200, body: { code: 401, message: "Token已过期，请重新登录" }`，前端axios响应拦截器在`response.use`中将`code !== 200`的请求`Promise.reject(new Error(message))`；后续`error`进入`error`拦截器时，`error.response.status`是200而非401，因此不会进入401 case，而是落入`default`或直接被调用方以网络错误/请求失败形式展现。

2. **后端控制台能显示token过期信息，前端不能**：
   - 根因：后端日志和响应体均正确输出了过期信息，但前端未按本项目的"HTTP 200 + 业务码"约定进行错误分类处理。

3. **token实际有效期为24小时而非30分钟**：
   - 根因：`application.yml`中显式配置了`expiration: 86400000`，覆盖了工具类中的30分钟默认值。

---

## 二、需求逻辑实现方案

### 2.1 总体架构

采用 **"双Token（Access Token + Refresh Token）+ 前端活动感知 + 自动静默刷新 + 弹窗兜底"** 四层架构：

```
┌─────────────────────────────────────────────────────────────┐
│                        前端 (Vue 3)                          │
│  ┌─────────────┐   ┌─────────────┐   ┌───────────────────┐  │
│  │ 用户活动监听 │ → │ Token预检/  │ → │ ConfirmDialog弹窗 │  │
│  │ (mouse/key) │   │ 自动刷新队列 │   │ 过期提示+跳转登录   │  │
│  └─────────────┘   └─────────────┘   └───────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼ HTTP Request
┌─────────────────────────────────────────────────────────────┐
│                        后端 (Spring Boot)                    │
│  ┌─────────────┐   ┌─────────────┐   ┌───────────────────┐  │
│  │ AccessToken │   │ RefreshToken│   │ Token黑名单/       │  │
│  │ 30分钟有效期 │   │ 7天有效期    │   │ 用户维度存储       │  │
│  └─────────────┘   └─────────────┘   └───────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

### 2.2 核心机制说明

#### 机制1：严格的30分钟Access Token有效期
- 修正`application.yml`：`jwt.expiration = 1800000`（30分钟）
- Access Token负责日常API访问鉴权，短有效期降低被盗用风险

#### 机制2：Refresh Token（7天有效期）
- 登录时随Access Token一并下发，存储于localStorage（与access token同等安全级别，后续可升级为HttpOnly Cookie）
- 仅用于访问`/auth/refresh`接口换取新的Access Token
- 后端使用数据库存储refresh token与用户、设备、过期时间的映射关系（支持单点登出/踢人）

#### 机制3：前端用户活动感知 + 自动刷新
- 监听`mousedown`、`keydown`、`touchstart`、`scroll`事件
- 维护`lastActivityTime`时间戳
- **请求拦截器预检**：每次发起API请求前，解析token过期时间（从JWT payload或登录时保存的`expiresAt`）：
  - 若剩余时间 > 5分钟：正常携带token发起请求
  - 若剩余时间 ≤ 5分钟 且 用户在30分钟内有活动：自动先调用`/auth/refresh`，成功后再携带新token发起原请求
  - 若剩余时间 ≤ 5分钟 但 用户超过30分钟无活动：不自动刷新，让请求正常携带旧token发出，由后端返回401，前端弹窗提示
- **并发请求保护**：建立刷新队列，当刷新请求进行中时，其他请求排队等待，刷新成功后一并重试，避免重复调用`/auth/refresh`

#### 机制4：标准化弹窗提示
- 当token确实过期且`/auth/refresh`也失败（或用户长期无活动）时：
  - 清除localStorage中的token/user/refreshToken
  - 使用全局挂载的`ConfirmDialog`显示标题"提示"、消息"您的登录已过期，请重新登录"
  - 用户点击"确定"后，再执行`router.push('/login')`
- **禁止**使用`window.location.href = '/login'`直接跳转，避免打断用户操作且无法展示弹窗

---

## 三、前后端具体修改点

### 3.1 后端修改清单

#### 3.1.1 配置文件：`oa-backend/src/main/resources/application.yml`

```yaml
jwt:
  secret: oa-system-jwt-secret-key-for-graduation-project-2024
  expiration: 1800000          # 改为30分钟（30 * 60 * 1000）
  refresh-expiration: 604800000 # 新增：Refresh Token 7天有效期（7 * 24 * 60 * 60 * 1000）
  token-prefix: Bearer
  header: Authorization
```

#### 3.1.2 数据库表：新增 `refresh_token` 表

```sql
CREATE TABLE refresh_token (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    token VARCHAR(512) NOT NULL COMMENT 'Refresh Token字符串',
    expires_at DATETIME NOT NULL COMMENT '过期时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    revoked TINYINT(1) DEFAULT 0 COMMENT '是否已撤销',
    INDEX idx_user_id (user_id),
    UNIQUE INDEX idx_token (token)
) COMMENT='Refresh Token表';
```

#### 3.1.3 Entity：`oa-backend/src/main/java/com/oasystem/entity/RefreshToken.java`

新建实体类，字段与上表对应。

#### 3.1.4 Mapper：`oa-backend/src/main/java/com/oasystem/mapper/RefreshTokenMapper.java`

MyBatis-Plus Mapper，提供`findByToken`、`revokeByUserId`等方法。

#### 3.1.5 JWT工具类：`JwtTokenUtil.java`

- 新增`generateRefreshToken(Long userId, String username)`方法
- 新增`getExpirationDate()`返回绝对过期时间戳（供前端计算）
- 保持现有`validateTokenWithReason`逻辑不变

#### 3.1.6 Service：`AuthService.java` / `AuthServiceImpl.java`

- `login()`：生成access token + refresh token，将refresh token存入数据库，返回`LoginResponse`（新增`refreshToken`、`refreshExpiresAt`字段）
- 新增`refreshAccessToken(String refreshToken)`：
  1. 校验refresh token格式和签名
  2. 查询数据库验证是否存在且未过期、未撤销
  3. 生成新的access token + 新的refresh token
  4. 将旧refresh token标记为revoked=1（或删除），保存新的refresh token
  5. 返回新的token对
- 新增`logout()`：将当前用户的refresh token全部标记为revoked=1（支持后端踢人）

#### 3.1.7 Controller：`AuthController.java`

- `POST /auth/refresh`：公开接口，接收`refreshToken`，调用`AuthService.refreshAccessToken()`
- `POST /auth/logout`：认证接口，调用`AuthService.logout()`，清除后端refresh token

#### 3.1.8 DTO：`LoginResponse.java`

新增字段：
```java
private String refreshToken;
private Long refreshExpiresAt; // 绝对时间戳（毫秒）
```

新建`RefreshTokenResponse.java`：
```java
@Data
public class RefreshTokenResponse {
    private String token;
    private String refreshToken;
    private Long expiresIn;
    private Long refreshExpiresAt;
}
```

#### 3.1.9 认证过滤器：`JwtAuthenticationFilter.java`

保持现有逻辑不变，但可优化：当`validateTokenWithReason`返回`token_expired`且请求路径为`/auth/refresh`时，放行请求（由Controller层处理refresh token验证）。

#### 3.1.10 认证入口点：`RestAuthenticationEntryPoint.java`

保持现有逻辑不变，确保`token_expired`分支返回：
```json
{ "code": 401, "message": "Token已过期，请重新登录", "data": null, "timestamp": 1713936000000 }
```

### 3.2 前端修改清单

#### 3.2.1 Axios配置：`oa-frontend/src/api/config.js`

**重大改造**：

1. **响应拦截器适配后端约定**：在`response.use`的成功回调中，增加对`code === 401`的处理：
```javascript
if (code === 401) {
  return Promise.reject({
    isAuthError: true,
    message: message || '登录已过期，请重新登录',
    response: response
  })
}
```

2. **错误拦截器增加弹窗逻辑**：
```javascript
(error) => {
  if (error.isAuthError || error.response?.status === 401 || error.response?.data?.code === 401) {
    // 优先尝试用refresh token刷新
    return handleTokenRefresh().then(() => {
      // 刷新成功，重试原请求
      return apiClient.request(error.config)
    }).catch(() => {
      // 刷新失败，弹窗提示并跳转
      showAuthExpiredDialog()
      return Promise.reject(error)
    })
  }
  // ... 其他错误处理不变
}
```

3. **新增刷新队列与预检逻辑**：
```javascript
let isRefreshing = false
let refreshSubscribers = []

function handleTokenRefresh() {
  if (isRefreshing) {
    return new Promise(resolve => refreshSubscribers.push(resolve))
  }
  isRefreshing = true
  const refreshToken = localStorage.getItem('refreshToken')
  if (!refreshToken) return Promise.reject(new Error('no refresh token'))

  return axios.post('/api/auth/refresh', { refreshToken })
    .then(res => {
      localStorage.setItem('token', res.token)
      localStorage.setItem('refreshToken', res.refreshToken)
      localStorage.setItem('tokenExpiresAt', Date.now() + res.expiresIn)
      refreshSubscribers.forEach(cb => cb())
      refreshSubscribers = []
      isRefreshing = false
    })
    .catch(err => {
      isRefreshing = false
      refreshSubscribers = []
      throw err
    })
}
```

4. **请求拦截器增加token预检**：
```javascript
apiClient.interceptors.request.use(async (config) => {
  const token = localStorage.getItem('token')
  const expiresAt = Number(localStorage.getItem('tokenExpiresAt') || 0)
  const lastActivity = Number(localStorage.getItem('lastActivity') || 0)
  const now = Date.now()

  if (token && expiresAt > 0 && expiresAt - now < 5 * 60 * 1000) {
    // Token将在5分钟内过期
    if (now - lastActivity < 30 * 60 * 1000) {
      // 用户30分钟内有活动，尝试静默刷新
      try {
        await handleTokenRefresh()
      } catch (e) {
        // 静默刷新失败不阻断请求，让后端返回401后再弹窗
      }
    }
  }

  const currentToken = localStorage.getItem('token')
  if (currentToken) {
    config.headers.Authorization = `Bearer ${currentToken}`
  }
  return config
})
```

#### 3.2.2 用户活动监听：`oa-frontend/src/composables/useActivityTracker.js`（新建）

```javascript
import { onMounted, onUnmounted } from 'vue'

const ACTIVITY_EVENTS = ['mousedown', 'keydown', 'touchstart', 'scroll']

export function useActivityTracker() {
  function updateActivity() {
    localStorage.setItem('lastActivity', Date.now().toString())
  }

  onMounted(() => {
    updateActivity()
    ACTIVITY_EVENTS.forEach(event => {
      document.addEventListener(event, updateActivity, { passive: true })
    })
  })

  onUnmounted(() => {
    ACTIVITY_EVENTS.forEach(event => {
      document.removeEventListener(event, updateActivity)
    })
  })
}
```

在`App.vue`或`MainLayout.vue`中全局引入：
```javascript
import { useActivityTracker } from '@/composables/useActivityTracker'
useActivityTracker()
```

#### 3.2.3 认证Store：`oa-frontend/src/stores/auth.js`

- `login()`：保存`refreshToken`和`tokenExpiresAt`
- `logout()`：增加调用`POST /auth/logout`（可选，清除后端refresh token），然后清除所有localStorage字段
- `initAuth()`：增加token过期时间检查，如果已过期且无法刷新，直接弹窗并跳转登录

#### 3.2.4 全局弹窗：`oa-frontend/src/utils/authDialog.js`（新建）

创建全局弹窗函数（利用Vue的createApp或ConfirmDialog组件）：
```javascript
import { createApp, h } from 'vue'
import ConfirmDialog from '@/components/ConfirmDialog.vue'

export function showAuthExpiredDialog() {
  const div = document.createElement('div')
  document.body.appendChild(div)

  const app = createApp({
    render() {
      return h(ConfirmDialog, {
        visible: true,
        title: '提示',
        message: '您的登录已过期，请重新登录',
        showCancel: false,
        onConfirm: () => {
          app.unmount()
          div.remove()
          window.location.href = '/login'
        }
      })
    }
  })
  app.mount(div)
}
```

#### 3.2.5 路由守卫：`oa-frontend/src/router/index.js`

增加token过期前置检查：
```javascript
router.beforeEach(async (to, from, next) => {
  const authStore = useAuthStore()
  const token = localStorage.getItem('token')
  const expiresAt = Number(localStorage.getItem('tokenExpiresAt') || 0)

  if (to.meta.requiresAuth && token && expiresAt > 0 && Date.now() > expiresAt) {
    // Token已过期，尝试刷新
    try {
      await handleTokenRefresh()
    } catch {
      showAuthExpiredDialog()
      return next('/login')
    }
  }
  // ... 原有逻辑
})
```

---

## 四、测试验证计划

### 4.1 单元测试

| 测试项 | 目标文件 | 验证内容 |
|--------|---------|---------|
| JWT生成与验证 | `JwtTokenUtilTest.java` | Access Token 30分钟后过期；Refresh Token 7天后过期 |
| Refresh Token业务 | `AuthServiceImpl`（新增测试类） | 有效refresh token可换取新token对；过期/被撤销refresh token返回401；刷新后旧refresh token失效 |
| 数据库操作 | `RefreshTokenMapperTest.java`（新增） | 增删查改、按token唯一索引查询、按user_id批量撤销 |
| 过滤器放行 | `JwtAuthenticationFilterTest.java`（新增） | `/auth/refresh`路径在access token过期时仍可进入Controller |

### 4.2 集成测试

| 测试项 | 步骤 | 期望结果 |
|--------|------|---------|
| 正常登录 | 调用`POST /auth/login` | 返回`token`、`refreshToken`、`expiresIn`、`refreshExpiresAt` |
| Access Token过期后访问API | 等待30分钟（或篡改token过期时间）后访问`/approval/list` | 后端返回`code=401, message=Token已过期`；前端弹窗"您的登录已过期，请重新登录" |
| 用户活动自动续期 | 登录后持续操作页面，每25分钟触发一次API请求 | 请求拦截器检测到token即将过期，自动调用`/auth/refresh`，用户无感知，操作不中断 |
| 30分钟无操作后过期 | 登录后静置30分钟，再点击按钮 | token已过期且`lastActivity`超过30分钟，前端不自动刷新，后端返回401，弹窗提示重新登录 |
| 并发请求刷新保护 | 在token即将过期时，同时触发3个API请求 | 仅发送1次`/auth/refresh`请求，其余2个排队等待刷新成功后重试 |
| 后端登出 | 调用`POST /auth/logout`后，使用原refresh token调用`/auth/refresh` | 返回401，refresh token已失效 |

### 4.3 前端端到端测试（手动）

| 场景 | 操作步骤 | 期望表现 |
|------|---------|---------|
| Token过期弹窗样式 | 清除token后刷新页面 | 出现`ConfirmDialog`标准弹窗，标题"提示"，消息"您的登录已过期，请重新登录"，仅"确定"按钮 |
| 网络错误区分 | 断开后端网络，发起请求 | 提示"网络连接失败，请检查网络设置"，不触发登录过期弹窗 |
| 登录页快捷跳转 | 在弹窗点击"确定" | 平滑跳转到`/login`，无白屏 |

---

## 五、实施风险评估

### 5.1 风险矩阵

| 风险项 | 影响等级 | 发生概率 | 缓解措施 |
|--------|---------|---------|---------|
| **数据库表变更** | 中 | 高 | 提供flyway/手动SQL脚本；开发环境先行验证 |
| **前端axios拦截器改造引入请求死锁** | 高 | 中 | 刷新队列必须设置超时（10秒）；`handleTokenRefresh`内部使用独立axios实例，避免拦截器递归调用 |
| **localStorage XSS风险** | 中 | 低 | refresh token与access token同存于localStorage，当前项目风险可控；后续如需增强安全，可升级为HttpOnly Cookie + CSRF Token方案 |
| **后端兼容性问题** | 中 | 低 | `LoginResponse`新增字段不影响旧版前端（忽略未知字段）；`application.yml`变更需通知运维 |
| **并发刷新队列内存泄漏** | 低 | 低 | `refreshSubscribers`数组在成功/失败后必须清空；设置最大等待时间 |
| **用户活动监听性能** | 低 | 低 | 使用`{ passive: true }`事件监听，不阻塞主线程；throttle节流非必须（仅写入localStorage） |

### 5.2 回滚方案

- **数据库**：保留旧token验证逻辑，新增refresh_token表为独立模块，回滚时仅停用`/auth/refresh`和`/auth/logout`接口，不影响现有登录鉴权
- **前端**：`config.js`的改造可封装为独立文件`apiClient.v2.js`，通过feature flag切换；原`config.js`保留备用
- **配置**：`application.yml`的`jwt.expiration`若需回退，可恢复为`86400000`

### 5.3 实施顺序建议

```
Phase 1（后端基础）
  ├─ 创建 refresh_token 表
  ├─ 新增 RefreshToken Entity + Mapper
  ├─ 修正 application.yml jwt.expiration = 1800000
  └─ JwtTokenUtil 新增 refresh token 生成方法

Phase 2（后端业务）
  ├─ AuthService 实现 refreshAccessToken / logout
  ├─ AuthController 新增 /auth/refresh 和 /auth/logout
  ├─ LoginResponse 扩展字段
  └─ 单元测试 + 集成测试

Phase 3（前端基础）
  ├─ 新建 useActivityTracker composable
  ├─ 新建 authDialog.js 全局弹窗
  └─ auth store 扩展 refreshToken 存储

Phase 4（前端核心）
  ├─ 重构 api/config.js（响应拦截器适配 code=401、请求预检、刷新队列）
  ├─ 路由守卫增加过期检查
  └─ App.vue / MainLayout.vue 挂载活动监听

Phase 5（联调测试）
  ├─ 端到端测试所有场景
  ├─ 回归测试现有功能（登录、审批流、RBAC）
  └─ 输出测试报告
```

---

## 六、需求逻辑与现有逻辑差异对照表

| 需求点 | 现有逻辑 | 目标逻辑 | 冲突程度 |
|--------|---------|---------|---------|
| Token有效期30分钟 | application.yml配置为24小时 | 修正为1800000ms（30分钟） | 低（配置变更） |
| 用户活动自动续期 | 无活动监听，无续期机制 | 前端监听活动+自动调用refresh接口 | 高（新增架构） |
| 30分钟无操作正常过期 | token固定24小时过期 | access token严格30分钟过期，无活动不刷新 | 中（需前后端配合） |
| 标准化弹窗提示 | 直接window.location跳转 | ConfirmDialog弹窗+确认后跳转 | 中（前端改造） |
| 错误提示准确 | 前端无法识别后端返回的token过期 | 响应拦截器适配code=401，明确分类处理 | 高（需修正拦截器） |

---

## 七、附录：关键接口契约

### 7.1 POST /auth/login（响应扩展）

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 1800000,
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "refreshExpiresAt": 1714540800000,
    "user": { ... }
  },
  "timestamp": 1713936000000
}
```

### 7.2 POST /auth/refresh

请求：
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

成功响应：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 1800000,
    "refreshExpiresAt": 1714540800000
  }
}
```

失败响应：
```json
{
  "code": 401,
  "message": "Refresh Token已过期或无效，请重新登录",
  "data": null
}
```

### 7.3 Token过期API响应（保持现有）

```json
{
  "code": 401,
  "message": "Token已过期，请重新登录",
  "data": null,
  "timestamp": 1713936000000
}
```
HTTP Status: 200（保持现有约定，前端已适配）
