<p align="center">
  <img src="admin/public/logo.svg" alt="runningerrands" width="120" />
</p>

<h1 align="center">小i跑腿 · runningerrands</h1>

<p align="center">
  <a href="README.md">中文</a>
  ·
  <a href="README_EN.md">English</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/release-v1.0.0-blue?style=plastic" alt="release" />
  <img src="https://img.shields.io/badge/springboot-3.2.0-brightgreen?style=plastic&logo=springboot" alt="springboot" />
  <img src="https://img.shields.io/badge/Vue3-grey?style=plastic&logo=vue.js" alt="vue" />
</p>

---

A campus errand-running service platform. Students publish tasks (package pickup, food delivery, campus errands, shopping proxy), and runners accept and fulfill orders. Includes a WeChat Mini Program (uni-app), admin dashboard (Vue 3), and Spring Boot backend.

---

## Project Structure

```
runningerrands/
├── backend/
│   ├── pom.xml                          # Maven parent POM (multi-module)
│   ├── runningerrands-common/           # Shared: constants, exceptions, utils, JWT, properties
│   ├── runningerrands-model/            # Entities, DTOs, VOs
│   ├── runningerrands-server/           # Spring Boot application
│   │   └── src/main/java/com/ikeu/server/
│   │       ├── controller/              # Common + admin/ (dashboard) + user/ (mobile client)
│   │       ├── service/ + impl/         # Business interfaces & implementations
│   │       ├── mapper/                  # MyBatis-Plus mappers + XML
│   │       ├── config/                  # Config classes + AdminInitializer (superadmin bootstrap)
│   │       ├── interceptor/             # Dual JWT interceptors (admin + user)
│   │       ├── aspect/                  # Aspects: notification, operation log, role check
│   │       ├── annotation/              # @SendNotification, @OperationLog, @RequireRole
│   │       ├── task/                    # Scheduled tasks
│   │       └── websocket/               # STOMP WebSocket
│   └── runningerrands.sql              # Database DDL script
├── admin/                               # Vue 3 + TypeScript + Element Plus admin dashboard
│   └── src/
│       ├── api/                         # 10 domain-split API modules
│       ├── views/                       # 15 views (users/runners/tasks/orders/transactions/notifications/logs/employees)
│       ├── router/                      # history /api/, beforeEach guard + role filtering
│       ├── stores/                      # Pinia: auth (token/adminInfo) + app (sidebar)
│       ├── composables/                 # useCountUp, usePageEnter (GSAP animations)
│       ├── utils/                       # request.ts (Axios + dual-token refresh queue)
│       └── styles/theme.css             # CSS design tokens
├── mobile/                              # uni-app (Vue 3) WeChat Mini Program
│   ├── pages.json                       # 33 pages + 5-tab glassmorphism TabBar
│   ├── api/index.js                     # 10 API modules barrel export
│   ├── store/                           # Pinia: main store + chat store + STOMP lifecycle
│   ├── utils/
│   │   ├── request.js                   # HTTP wrapper (auth type parameter)
│   │   ├── stomp.js                     # Custom STOMP 1.2 WebSocket client
│   │   ├── config.js                    # Dynamic env detection (develop/trial/release)
│   │   ├── draft-save.js                # Form draft auto-save composable
│   │   └── ...                          # Additional utility modules
│   └── components/                      # custom-navbar, custom-tabbar, pay-password-dialog, etc.
├── docs/                                # Development docs & work logs
└── .agent/                              # AI agent definitions (init, code-reviewer, security-auditor, etc.)
```

---

## Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| JDK | 21+ | Backend compilation & runtime |
| MySQL | 8.0+ | Primary database |
| Redis | 6.0+ | Cache + distributed lock + login protection |
| Maven | 3.8+ | Or use the `mvnw` wrapper |
| Node.js | 18+ / 22+ | Admin frontend |
| HBuilderX | Latest | Mobile development & build |
| WeChat DevTools | Latest | Mini Program debugging |

---

## Quick Start

### 1. Database Setup

```bash
mysql -u root -p < backend/runningerrands.sql
```

The script creates the `runningerrands` database and all tables. No seed data is included. `AdminInitializer` auto-creates the superadmin account on first boot.

### 2. Backend

```bash
cd backend

# Copy the template to create your local dev config
cp runningerrands-server/src/main/resources/application-template.yml \
   runningerrands-server/src/main/resources/application-dev.yml

# Edit application-dev.yml with your local DB, Redis, and service credentials
# application-dev.yml is gitignored — never committed
```

**Required environment variables** (or set in `application-dev.yml`):

| Variable | Description | Example |
|------|------|------|
| `MYSQL_PASSWORD` | MySQL password | `your_password` |
| `ALIYUN_ACCESS_KEY_ID` | Alibaba Cloud AccessKey | `LTAI5t...` |
| `ALIYUN_ACCESS_KEY_SECRET` | Alibaba Cloud AccessSecret | |
| `WECHAT_APP_ID` | WeChat Mini Program AppID | `wx74e8...` |
| `WECHAT_APP_SECRET` | WeChat Mini Program AppSecret | |
| `TENCENT_MAP_API_KEY` | Tencent Maps WebService API Key | |
| `RUNNING_ERRANDS_JWT_ADMIN_SECRET` | Admin JWT signing key | Random 32+ chars |
| `RUNNING_ERRANDS_JWT_USER_SECRET` | User JWT signing key | Random 32+ chars |

```bash
# Build & run
./runningerrands-server/mvnw compile -q -DskipTests
./runningerrands-server/mvnw spring-boot:run
# Server: http://localhost:8080/api
# API docs: http://localhost:8080/api/doc.html
# Default superadmin: admin / admin
```

### 3. Admin Dashboard

```bash
cd admin
npm install
npm run dev          # http://localhost:3001
```

Vite proxies `/api` requests to `http://localhost:8080` automatically. No additional config needed for local dev.

### 4. Mobile

1. Open `mobile/` in HBuilderX
2. Set your WeChat Mini Program AppID in `mobile/manifest.json` (`mp-weixin.appid`)
3. Update backend address in `mobile/utils/config.js` (defaults to `localhost:8080` for local dev)
4. Run → WeChat Mini Program

---

## Dev Environment Guide

### Environment Architecture

```
┌──────────────┐     ┌─────────────────┐     ┌──────────────┐
│  Mobile uni-app │────→│  Backend Spring   │←────│  Admin Vue 3   │
│  (WeChat DevTools)│   │  :8080/api        │     │  :3001 → proxy  │
│  config.js       │     │  application.yml  │     │  vite.config.ts │
└──────────────┘     └─────────────────┘     └──────────────┘
```

### Backend Profile Switching

```
application.yml              # Shared config (all environments)
application-template.yml     # Dev template (committed; new devs copy this)
application-dev.yml          # Local dev config (.gitignore, per-developer)
application-test.yml         # Test environment (.gitignore)
application-prod.yml         # Production (.gitignore, env-vars injected)
```

```bash
# Dev (default)
./mvnw spring-boot:run

# Test
./mvnw spring-boot:run -Dspring-boot.run.profiles=test

# Production
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
```

### Frontend API Configuration

Two approaches are supported:

#### Option A: Local Direct Connect (default)

- **Admin**: Vite proxy target `http://localhost:8080` in `admin/vite.config.ts`
- **Mobile**: `develop` env in `mobile/utils/config.js` points to `http://localhost:8080`
- In WeChat DevTools: enable "Skip domain validation" in project settings

#### Option B: Tunnel (for real device testing)

When testing on a physical device or external access is needed, use ngrok / frp / localtunnel:

```bash
# Start ngrok tunnel to backend
ngrok http 8080
# Output: https://xxxx.ngrok-free.dev → http://localhost:8080
```

Then update:
- **Admin** `vite.config.ts`: proxy target to `https://xxxx.ngrok-free.dev` (add `ngrok-skip-browser-warning` header)
- **Mobile** `config.js`: `develop.SERVER_ORIGIN` to `https://xxxx.ngrok-free.dev`
- WS_URL auto-converts `https` → `wss`

> Free ngrok URLs change on restart. For a stable domain, use ngrok paid plan or self-hosted frp.

### WeChat Mini Program AppID

Each developer must use their **own AppID**:

1. Get AppID from [WeChat Official Platform](https://mp.weixin.qq.com) → Development → Settings
2. Set in `mobile/manifest.json`: `mp-weixin.appid`
3. Backend `WECHAT_APP_ID` + `WECHAT_APP_SECRET` must match

---

## New Developer Onboarding Checklist

| # | File | Setting | Notes |
|---|------|---------|-------|
| 1 | `application-dev.yml` | `spring.datasource.*` | MySQL connection |
| 2 | `application-dev.yml` | `spring.data.redis.*` | Redis connection |
| 3 | `application-dev.yml` | `runningerrands.alioss.bucket-name` | Replace with your OSS bucket |
| 4 | `application-dev.yml` | `runningerrands.sms.sign-name` | Replace with your SMS signature |
| 5 | `application-dev.yml` | `runningerrands.sms.template-code` | Replace with your SMS template |
| 6 | `application.yml` | `jwt.admin-secret-key` | Set via env var: random 32+ chars |
| 7 | `application.yml` | `jwt.user-secret-key` | Set via env var: random 32+ chars |
| 8 | `application.yml` | `wechat.app-id` | WeChat Mini Program AppID |
| 9 | `application.yml` | `wechat.app-secret` | WeChat Mini Program AppSecret |
| 10 | `application.yml` | `map.api-key` | Tencent Maps API Key |
| 11 | `admin/vite.config.ts` | `target` | Backend URL (default `localhost:8080`) |
| 12 | `mobile/manifest.json` | `mp-weixin.appid` | Your WeChat AppID |
| 13 | `mobile/manifest.json` | `appid` | Your DCloud app ID |
| 14 | `mobile/utils/config.js` | `develop.trial.release` | Backend URL per env |

---

## Tech Stack

| Layer | Technology |
|------|------------|
| **Backend** | Spring Boot 3.2.0 · Java 21 · MyBatis-Plus 3.5.5 · MySQL 8 · Redis · Redisson |
| **Admin** | Vue 3.5 · TypeScript 6.0 · Vite 8 · Element Plus 2.14 · ECharts 6 · Pinia 3 · GSAP 3 |
| **Mobile** | uni-app (Vue 3) · WeChat Mini Program · Pinia · Custom STOMP 1.2 WebSocket client |
| **Auth** | JWT dual-token (access + refresh), separate keys & interceptors for admin and user |
| **API Docs** | Knife4j (Swagger) · springdoc-openapi |

---

## Auth & Permissions

```
Admin: token header → JwtTokenAdminInterceptor → /admin/**
User:  authentication header → JwtTokenUserInterceptor → /user/**
WebSocket: JwtHandshakeInterceptor → AuthChannelInterceptor
```

| Role | Value | Access |
|------|-------|--------|
| Superadmin | 1 | Full access, including employee management, operation logs |
| Admin | 2 | Dashboard, user/runner/task/order/transaction/notification management, verification |

Annotation-based RBAC: `@RequireRole({1, 2})` + `RoleCheckAspect`.

---

## License

[MIT](LICENSE) © 2025 ikeu
