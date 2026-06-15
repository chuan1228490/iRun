# Mobile Frontend Exploration Guide

## Scope

适用于 `mobile/` 下 uni-app (Vue 3) 微信小程序端探索。

---

## 探索步骤

### Step 1: 目录结构

```
mobile/
├── pages.json                        # 33 页面注册 + TabBar 配置
├── App.vue                           # CSS 自定义属性 + 全局样式
├── main.js
├── api/
│   └── index.js                      # 统一命名空间导出 10 API 模块
├── pages/                            # 33 个页面
│   ├── index/                        # 首页
│   ├── task-hall/                    # 任务大厅
│   ├── task-detail/                  # 任务详情
│   ├── task-publish/                 # 发布任务
│   ├── print-order/                  # 打印订单
│   ├── coffee-order/                 # 咖啡下单
│   ├── paper-express/                # 论文快递
│   ├── general-publish/              # 通用发布
│   ├── service-publish/              # 服务发布
│   ├── order-list/                   # 订单列表
│   ├── order-detail/                 # 订单详情
│   ├── chat/                         # 聊天
│   ├── profile/                      # 个人中心
│   ├── address-list/                 # 地址列表
│   ├── address-edit/                 # 地址编辑
│   ├── ...                           # 更多页面
├── store/
│   ├── index.js                      # 主 Store：登录态、用户信息、支付密码
│   └── chat.js                       # 聊天 Store：STOMP 连接、消息缓存
├── utils/
│   ├── request.js                    # HTTP 封装：get/post/put/del + auth 参数
│   ├── stomp.js                      # 自制 STOMP 1.2 WebSocket 客户端
│   ├── config.js                     # 动态环境检测（develop/trial/release）
│   ├── constants.js                  # 状态码枚举 + 徽章样式
│   ├── toast.js                      # 智能 toast
│   ├── submit-guard.js               # 防重复提交 composable
│   ├── task-normalizer.js            # API → 统一任务卡片格式
│   ├── campus-data.js                # 双校区硬编码数据
│   ├── custom-icons.js               # iconfont 内联 SVG
│   └── draft-save.js                 # 草稿保存 composable
└── components/
    ├── custom-navbar.vue             # 自定义导航栏
    ├── custom-tabbar.vue             # 毛玻璃 TabBar
    ├── iconpark-icon.vue
    ├── pay-password-dialog.vue       # 支付密码弹窗
    └── upload-grid.vue               # 图片上传网格
```

### Step 2: 导航结构

```
TabBar (5 标签，毛玻璃效果)：
├── 首页      /pages/index/index
├── 任务大厅  /pages/task-hall/task-hall
├── 发布      /pages/task-publish/task-publish
├── 消息      /pages/chat/chat
└── 我的      /pages/profile/profile

非 Tab 页面（navigateTo）：
├── 任务详情   /pages/task-detail/task-detail?id=xxx
├── 订单列表   /pages/order-list/order-list
├── 订单详情   /pages/order-detail/order-detail?id=xxx
├── 地址管理   /pages/address-list/address-list
├── 地址编辑   /pages/address-edit/address-edit
├── 认证中心   /pages/certification/certification
├── 跑腿员中心 /pages/runner-center/runner-center
├── ... (更多)
```

### Step 3: 认证流程

```
微信登录：
  → wx.login() 获取 code
  → POST /user/wechat-login (WeChatLoginDTO)
  → 后端通过 code 换取 openid
  → 返回 { userId, token, refreshToken }
  → uni.setStorageSync('d2d_user_token', token)
  → 主 Store login() 更新用户信息
  → 自动连接 STOMP WebSocket

手机号登录：
  → POST /user/login (UserLoginDTO)
  → 返回 { userId, token, refreshToken }
  → 同上流程

401 自动刷新：
  → request.js 检测 code === 401
  → isRefreshing + 请求队列
  → POST /user/refresh-token
  → 更新 storage + Store
  → 重放队列请求
```

### Step 4: API 调用链路

```
页面 (pages/task-hall/task-hall.vue)
  → api/index.js: taskAPI.getTaskHall({ page, size, campus })
  → utils/request.js: get('/user/task-hall', params, 'user')
  → uni.request({ header: { authentication: storage.token } })
  → 后端 /api/user/task-hall
  → 响应: request.js 解包 code===1 返回 data
  → task-normalizer.js: 统一卡片格式
  → 页面渲染
```

### Step 5: WebSocket 生命周期

```
登录成功
  → main Store login()
  → StompClient.connect('ws://host:8080/api/ws')
  → 订阅 /user/queue/notifications (通知)
  → 订阅 /user/queue/chat (聊天)
  → 心跳 10s 发送 \n
  → 断线 3 次重试 → 尝试 refresh token → 失败则退出

退出登录
  → main Store logout()
  → StompClient.disconnect()
  → 清除 storage token
```
