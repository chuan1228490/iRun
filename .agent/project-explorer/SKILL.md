---
name: "project-explorer"
description: "项目初始化探索代理，三端并行探索（后端/管理端/移动端），梳理架构、链路、数据流、关键代码和配置。触发于新成员入职、项目交接、架构文档生成。"
model: sonnet
memory: user
---

# Project Explorer

## When to use this skill

**触发场景：**
- 新成员加入项目，需要快速了解项目全貌
- 接手陌生模块，需要梳理请求链路和数据流
- 生成或更新项目架构文档
- PR 审查前需要理解修改影响范围
- 排查问题时需要追踪跨层调用链
- 定期项目健康检查（依赖、配置、代码质量）

**DO NOT use when:**
- 只需看单个文件的代码逻辑 → 直接 Read
- 只需搜索某个关键字 → 直接用 Grep
- 修改代码 → 使用 `backend-code-reviewer` 或 `frontend-code-reviewer`
- 只需运行测试 → 直接 Bash

## How to use this skill

```
/agent:project-explorer --scope <full|backend|admin|mobile> --depth <quick|standard|deep>
```

### 探索深度
- **quick**: 目录结构 + 入口文件 + 关键配置（5min）
- **standard**: 加上核心链路追踪 + 数据模型（15min，默认）
- **deep**: 全量代码分析 + 依赖图谱 + 架构评估（30min+）

### 三端并行探索

每次探索默认三端并行，各自启动 Explore 子代理：

```
主代理：分配任务 → 等待三端结果 → 汇总输出

├── 后端探索子代理：Spring Boot 架构、Controller→Service→Mapper、缓存策略、配置
├── 管理端子代理：Vue 3 路由、Store、API 模块、组件树
└── 移动端子代理：uni-app 页面、API 层、WebSocket、Store
```

## General Rules

### 后端探索清单

- **目录结构**：Maven 多模块（common/model/server），各模块职责
- **入口**：`RunningerrandsServerApplication`，启动 profile，上下文路径 `/api`
- **配置**：`application.yml` + `application-template.yml`，环境变量占位，个人 dev 配置在 `.gitignore`
- **Controller 层**：按 `admin/` 和 `user/` 分包，双 token 拦截器
- **Service 层**：接口 + 实现分离，`@Service` + `@RequiredArgsConstructor`
- **Mapper 层**：MyBatis-Plus `BaseMapper`，LambdaWrapper 查询，XML 复杂 SQL
- **缓存**：`@Cacheable`/`@CacheEvict` 仪表盘，`@RedisDefend` 任务大厅，Redisson 分布式锁
- **认证**：JWT 双 token（access + refresh），BCrypt 密码加密
- **数据库**：`runningerrands.sql`，Entity 变更需同步
- **定时任务**：`task/` 包下的 `@Scheduled` 任务

### 管理端探索清单

- **入口**：`main.ts`，Element Plus 全局注册，路由 `history base /api/`
- **路由**：15 视图按模块分目录，`meta.role` 权限控制
- **Store**：Pinia (`auth.ts` + `app.ts`)，token 管理
- **API**：`api/` 10 模块，`utils/request.ts` Axios 封装，双 token 刷新队列
- **组件**：Element Plus 原生组件，详情页 v2 CSS 序列动画
- **样式**：`styles/theme.css` CSS 自定义属性

### 移动端探索清单

- **页面**：33 页面，`pages.json` 注册，自定义导航栏 + 毛玻璃 TabBar
- **API**：`api/index.js` 10 模块，`utils/request.js` 认证类型参数
- **Store**：Pinia (`index.js` 主 Store + `chat.js` 聊天 Store)，STOMP 连接管理
- **WebSocket**：自制 `StompClient`，心跳 10s，3 次重试
- **支付密码**：promise 式弹窗 `promptPayPassword()`
- **工具**：`config.js` 动态环境、`toast.js` 智能提示、`submit-guard.js` 防重复

## CheckList

### 探索输出格式

```markdown
## 项目概览
- 项目名称、定位、技术栈
- 模块划分（后端/管理端/移动端）

## 后端架构
- 目录结构 + 模块职责
- 请求链路（URL → Controller → Service → Mapper → DB）
- 缓存策略（Spring Cache + Redisson）
- 认证流程（双 token 拦截器）

## 管理端架构
- 路由树 + 页面清单
- 状态管理（Pinia Store）
- API 调用链路（组件 → API 模块 → Axios → 后端）

## 移动端架构
- 页面清单 + 导航结构
- API 调用链路（页面 → api 模块 → request.js → 后端）
- WebSocket 连接生命周期

## 关键代码路径
| 功能 | 后端入口 | Service | Mapper | 前端组件 |
|------|---------|---------|--------|---------|

## 配置清单
- 环境变量列表
- 外部依赖（MySQL, Redis, OSS, 微信）
```

### 完成标准
- [ ] 三端目录结构已梳理
- [ ] 核心请求链路已追踪（至少 3 条完整链路）
- [ ] 关键配置项已提取
- [ ] 外部依赖已列出
- [ ] 输出完整可读的架构摘要
