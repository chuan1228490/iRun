# Admin Frontend Exploration Guide

## Scope

适用于 `admin/` 下 Vue 3 + TypeScript + Element Plus 管理端探索。

---

## 探索步骤

### Step 1: 目录结构

```
admin/
├── index.html
├── vite.config.ts
├── tsconfig.json
├── package.json
└── src/
    ├── main.ts                       # 入口：注册 Element Plus + 图标 + 路由 + Pinia
    ├── App.vue
    ├── api/                          # 10 模块按领域拆分
    │   ├── auth.ts
    │   ├── dashboard.ts
    │   ├── employees.ts
    │   ├── logs.ts
    │   ├── notifications.ts
    │   ├── orders.ts
    │   ├── runners.ts
    │   ├── tasks.ts
    │   ├── transactions.ts
    │   └── users.ts
    ├── views/                        # 15 视图按模块分目录
    │   ├── login/
    │   ├── dashboard/
    │   ├── users/
    │   ├── runners/
    │   ├── tasks/
    │   ├── orders/
    │   ├── notifications/
    │   ├── transactions/
    │   ├── logs/
    │   ├── employees/
    │   ├── settings/
    │   └── error/
    ├── router/
    │   └── index.ts                  # history /api/, beforeEach 守卫
    ├── stores/
    │   ├── app.ts                    # 侧边栏折叠
    │   └── auth.ts                   # token/adminInfo/login/logout
    ├── composables/
    │   ├── useCountUp.ts             # GSAP 数字递增
    │   └── usePageEnter.ts           # GSAP 页面入场
    ├── utils/
    │   ├── request.ts                # Axios 实例 + 拦截器
    │   ├── constants.ts              # 状态枚举映射
    │   └── task-specs-parser.ts      # 任务规格解析
    ├── styles/
    │   └── theme.css                 # CSS 自定义属性
    └── components/                   # 空目录，全部用 Element Plus
```

### Step 2: 路由结构

```
/ (AdminLayout)
├── /login                            # 登录页（独立布局）
├── /dashboard                        # 仪表盘（默认首页）
├── /users                            # 用户管理
├── /users/:id                        # 用户详情
├── /runners                          # 跑腿员管理
├── /runners/:id                      # 跑腿员详情
├── /tasks                            # 任务管理
├── /tasks/:id                        # 任务详情
├── /orders                           # 订单管理
├── /orders/:id                       # 订单详情
├── /notifications                    # 消息管理
├── /transactions                     # 交易流水
├── /logs                             # 操作日志（仅超管）
├── /employees                        # 员工管理（仅超管）
├── /settings                         # 系统设置（仅超管）
└── /404                              # 404 页面
```

### Step 3: 认证流程

```
用户输入账号密码
  → POST /admin/login (auth.ts login())
  → 后端返回 { adminId, token, refreshToken }
  → localStorage.setItem('admin_token', token)
  → localStorage.setItem('admin_refresh_token', refreshToken)
  → auth store 更新 adminInfo
  → router.push('/dashboard')

401 自动刷新：
  → request.ts 响应拦截器检测 code === 401
  → isRefreshing 互斥锁 + refreshQueue 请求队列
  → POST /admin/refresh-token
  → 更新 localStorage + auth store
  → 重放队列中的请求
```

### Step 4: API 调用链路

```
组件 (views/users/index.vue)
  → api/users.ts: getUsers({ page, size, keyword })
  → utils/request.ts: request.get('/admin/users', { params })
  → Axios 请求拦截器: headers.token = localStorage.getItem('admin_token')
  → 后端 /api/admin/users
  → 响应拦截器: code === 1 ? return data : toast error
  → 组件拿到 { total, records }
  → 绑定 el-table :data + el-pagination
```
