<p align="center">
  <img src="admin/public/logo.svg" alt="小i跑腿" width="120" />
</p>

<h1 align="center">小i跑腿 · runningerrands</h1>

<p align="center">
  <img src="https://img.shields.io/badge/release-v1.0.0-blue?style=plastic" alt="release" />
  <img src="https://img.shields.io/badge/springboot-3.2.0-brightgreen?style=plastic&logo=springboot" alt="springboot" />
  <img src="https://img.shields.io/badge/Vue3-grey?style=plastic&logo=vue.js" alt="vue" />
</p>

---

校园跑腿服务平台。用户发布代取快递、代拿餐食、校内代办、代购物品等任务，跑腿员接单配送。含微信小程序端（uni-app）、管理后台（Vue 3）、Spring Boot 后端。

---

## 项目结构

```
runningerrands/
├── backend/
│   ├── pom.xml                          # Maven 父 POM（多模块）
│   ├── runningerrands-common/           # 共享：常量、异常、工具类、JWT、properties
│   ├── runningerrands-model/            # 实体（Entity）、DTO、VO
│   ├── runningerrands-server/           # Spring Boot 应用
│   │   └── src/main/java/com/ikeu/server/
│   │       ├── controller/              # Common + admin/（管理端） + user/（用户端）
│   │       ├── service/ + impl/         # 业务接口与实现
│   │       ├── mapper/                  # MyBatis-Plus Mapper + XML
│   │       ├── config/                  # 配置类 + AdminInitializer（超管初始化）
│   │       ├── interceptor/             # JWT 双拦截器（admin + user）
│   │       ├── aspect/                  # 切面：通知、操作日志、角色检查
│   │       ├── annotation/              # @SendNotification, @OperationLog, @RequireRole
│   │       ├── task/                    # 定时任务
│   │       └── websocket/               # STOMP WebSocket
│   └── runningerrands.sql              # 建库建表脚本
├── admin/                               # Vue 3 + TypeScript + Element Plus 管理端
│   └── src/
│       ├── api/                         # 10 模块按领域拆分
│       ├── views/                       # 15 视图（用户/跑腿员/任务/订单/流水/通知/日志/员工）
│       ├── router/                      # history /api/，beforeEach 守卫 + 角色过滤
│       ├── stores/                      # Pinia：auth（token/adminInfo） + app（侧边栏）
│       ├── composables/                 # useCountUp, usePageEnter（GSAP 动画）
│       ├── utils/                       # request.ts（Axios + 双 token 刷新队列）
│       └── styles/theme.css             # CSS 设计令牌
├── mobile/                              # uni-app (Vue 3) 微信小程序
│   ├── pages.json                       # 32 页面注册 + 5 标签毛玻璃 TabBar
│   ├── api/index.js                     # 10 API 模块统一导出
│   ├── store/                           # Pinia：主 Store + 聊天 Store + STOMP 管理
│   ├── utils/
│   │   ├── request.js                   # HTTP 封装（auth 参数控制认证类型）
│   │   ├── stomp.js                     # 自制 STOMP 1.2 WebSocket 客户端
│   │   ├── config.js                    # 动态环境检测（develop/trial/release）
│   │   ├── draft-save.js                # 草稿保存 composable
│   │   └── ...                          # 更多工具模块
│   └── components/                      # custom-navbar, custom-tabbar, pay-password-dialog 等
├── docs/                                # 开发文档 & 工作记录
└── .agent/                              # AI 辅助代理定义（init, code-reviewer, security-auditor 等）
```

---

## 环境要求

| 工具 | 版本 | 说明 |
|------|------|------|
| JDK | 21+ | 后端编译运行 |
| MySQL | 8.0+ | 主数据库 |
| Redis | 6.0+ | 缓存 + 分布式锁 + 登录保护 |
| Maven | 3.8+ | 或使用 `mvnw` wrapper |
| Node.js | 18+ / 22+ | 管理端前端 |
| HBuilderX | 最新版 | 移动端开发与编译 |
| 微信开发者工具 | 最新版 | 小程序调试 |

---

## 快速开始

### 1. 初始化数据库

```bash
mysql -u root -p < backend/runningerrands.sql
```

脚本创建 `runningerrands` 数据库及全部表结构，不含种子数据。首次启动后 `AdminInitializer` 自动创建超管账号。

### 2. 后端配置

```bash
cd backend

# 从模板复制开发环境配置
cp runningerrands-server/src/main/resources/application-template.yml \
   runningerrands-server/src/main/resources/application-dev.yml

# 编辑 application-dev.yml，填写本地数据库、Redis 等信息
# application-dev.yml 已在 .gitignore 中，不会被提交
```

**必须配置的环境变量**（或写在 `application-dev.yml` 中）：

| 变量 | 说明 | 示例 |
|------|------|------|
| `MYSQL_PASSWORD` | MySQL 密码 | `your_password` |
| `ALIYUN_ACCESS_KEY_ID` | 阿里云 AccessKey | `LTAI5t...` |
| `ALIYUN_ACCESS_KEY_SECRET` | 阿里云 AccessSecret | |
| `WECHAT_APP_ID` | 微信小程序 AppID | `wx74e8...` |
| `WECHAT_APP_SECRET` | 微信小程序 AppSecret | |
| `TENCENT_MAP_API_KEY` | 腾讯地图 WebService API Key | |
| `RUNNING_ERRANDS_JWT_ADMIN_SECRET` | 管理端 JWT 签名密钥 | 随机 32+ 位字符串 |
| `RUNNING_ERRANDS_JWT_USER_SECRET` | 用户端 JWT 签名密钥 | 随机 32+ 位字符串 |

```bash
# 编译 & 启动
./runningerrands-server/mvnw compile -q -DskipTests
./runningerrands-server/mvnw spring-boot:run
# 服务运行在 http://localhost:8080/api
# Swagger 文档：http://localhost:8080/api/doc.html
# 超管默认账号：admin / admin
```

### 3. 管理端前端

```bash
cd admin
npm install
npm run dev          # http://localhost:3001
```

Vite 自动将 `/api` 开头的请求代理到 `http://localhost:8080`，本地开发无需额外配置。

### 4. 移动端

1. HBuilderX 打开 `mobile/` 目录
2. 修改 `mobile/manifest.json` 中的微信小程序 AppID（`mp-weixin.appid`）
3. 修改 `mobile/utils/config.js` 中的后端地址（本地开发默认 `localhost:8080`）
4. 运行 → 微信小程序

---

## 开发环境配置指南

### 环境架构

```
┌──────────────┐     ┌─────────────────┐     ┌──────────────┐
│  移动端 uni-app │────→│  后端 Spring Boot  │←────│  管理端 Vue 3  │
│  (微信开发者工具) │     │  :8080/api       │     │  :3001 → proxy │
│  config.js     │     │  application.yml │     │  vite.config.ts│
└──────────────┘     └─────────────────┘     └──────────────┘
```

### 后端环境切换

后端通过 `spring.profiles.active` 切换环境，配置在 `application.yml` 中：

```
application.yml              # 公共配置（所有环境共享）
application-template.yml     # 开发环境模板（提交到 git，新成员复制使用）
application-dev.yml          # 本地开发配置（.gitignore，每个开发者自己维护）
application-test.yml         # 测试环境配置（.gitignore）
application-prod.yml         # 生产环境配置（.gitignore，通过环境变量注入）
```

**切换方式**：

```bash
# 开发环境（默认）
./mvnw spring-boot:run

# 测试环境
./mvnw spring-boot:run -Dspring-boot.run.profiles=test

# 生产环境
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

### 前端 API 地址配置

**不推荐使用 ngrok 作为统一方案**。请按以下方式配置：

#### 管理端（Vite 代理）

编辑 `admin/vite.config.ts`：

```typescript
// 本地开发：代理到本地后端（默认，无需修改）
server: {
  port: 3001,
  proxy: {
    '^/api/(admin|user/|common|ws)': {
      target: 'http://localhost:8080',   // ← 改这里指向你的后端地址
      changeOrigin: true,
    }
  }
}
```

如需连接远程后端（测试服务器），修改 `target` 为服务器地址即可，无需 ngrok。

#### 移动端（环境自动切换）

编辑 `mobile/utils/config.js`，根据微信小程序运行版本自动选择后端地址：

```javascript
const ENV_CONFIG = {
  // 开发版（开发者工具 / 开发版）
  develop: {
    SERVER_ORIGIN: 'http://localhost:8080',        // ← 本地后端
    // SERVER_ORIGIN: 'https://your-test-server.com', // ← 或测试服务器
  },
  // 体验版
  trial: {
    SERVER_ORIGIN: 'https://test-api.your-domain.com',  // ← 测试服务器
  },
  // 正式版
  release: {
    SERVER_ORIGIN: 'https://api.your-domain.com',       // ← 生产服务器
  }
}

const envVersion = uni.getAccountInfoSync().miniProgram.envVersion
const BASE_URL = ENV_CONFIG[envVersion].SERVER_ORIGIN + '/api'
const WS_URL = ENV_CONFIG[envVersion].SERVER_ORIGIN.replace('https', 'wss')
                  .replace('http', 'ws') + '/api/ws'
```

**不需要 ngrok**：微信开发者工具支持直接连接局域网/localhost 后端，只需在工具中关闭"不校验合法域名"选项即可调试。

### 移动端微信小程序 AppID 配置

每个开发者必须使用**自己的微信小程序 AppID**：

1. 登录 [微信公众平台](https://mp.weixin.qq.com) → 开发管理 → 开发设置 → 获取 AppID
2. 修改 `mobile/manifest.json`：
   ```json
   {
     "mp-weixin": {
       "appid": "wx你的小程序AppID"
     }
   }
   ```
3. 后端 `WECHAT_APP_ID` 和 `WECHAT_APP_SECRET` 需与此 AppID 一致

---

## 完整配置清单

新开发者接入项目时，需要修改以下个人相关配置：

### 后端配置项

| # | 文件 | 配置项 | 说明 |
|---|------|--------|------|
| 1 | `application-dev.yml` | `spring.datasource.*` | MySQL 连接信息 |
| 2 | `application-dev.yml` | `spring.data.redis.*` | Redis 连接信息 |
| 3 | `application-dev.yml` | `runningerrands.alioss.bucket-name` | 替换为自己的 OSS 桶名 |
| 4 | `application-dev.yml` | `runningerrands.sms.sign-name` | 替换为自己的短信签名 |
| 5 | `application-dev.yml` | `runningerrands.sms.template-code` | 替换为自己的短信模板 |
| 6 | `application.yml` | `runningerrands.jwt.admin-secret-key` | 设置随机密钥（通过环境变量） |
| 7 | `application.yml` | `runningerrands.jwt.user-secret-key` | 设置随机密钥（通过环境变量） |
| 8 | `application.yml` | `runningerrands.wechat.app-id` | 微信小程序 AppID |
| 9 | `application.yml` | `runningerrands.wechat.app-secret` | 微信小程序 AppSecret |
| 10 | `application.yml` | `runningerrands.map.api-key` | 腾讯地图 API Key |

### 前端配置项

| # | 文件 | 配置项 | 说明 |
|---|------|--------|------|
| 11 | `admin/vite.config.ts` | `target` | 代理目标后端地址（本地开发默认 localhost:8080） |
| 12 | `mobile/manifest.json` | `mp-weixin.appid` | 替换为你的微信小程序 AppID |
| 13 | `mobile/manifest.json` | `appid` | 替换为你的 DCloud appid |
| 14 | `mobile/utils/config.js` | `develop.trial.release` | 三环境后端地址 |

---

## 技术栈

| 层 | 技术 |
|------|------|
| **后端** | Spring Boot 3.2.0 · Java 21 · MyBatis-Plus 3.5.5 · MySQL 8 · Redis · Redisson |
| **管理端** | Vue 3.5 · TypeScript 6.0 · Vite 8 · Element Plus 2.14 · ECharts 6 · Pinia 3 · GSAP 3 |
| **移动端** | uni-app (Vue 3) · 微信小程序 · Pinia · 自制 STOMP 1.2 WebSocket 客户端 |
| **认证** | JWT 双令牌（access + refresh），管理端与用户端密钥/拦截器隔离 |
| **API 文档** | Knife4j (Swagger) · springdoc-openapi |

---

## 认证与权限

```
管理端：token 头 → JwtTokenAdminInterceptor → /admin/**
用户端：authentication 头 → JwtTokenUserInterceptor → /user/**
WebSocket：JwtHandshakeInterceptor → AuthChannelInterceptor
```

| 角色 | 值 | 权限范围 |
|------|-----|---------|
| 超级管理员 | 1 | 全部功能，包括员工管理、操作日志 |
| 普通管理员 | 2 | 仪表盘、用户/跑腿员/任务/订单/流水/消息管理、审核 |

注解权限控制：`@RequireRole({1, 2})` + `RoleCheckAspect` 切面。

---

## CI / CD

CI/CD 配置待补充。

---

## License

[MIT](LICENSE) © 2025 ikeu
