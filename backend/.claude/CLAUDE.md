# 小i跑腿 — runningerrands CLAUDE.md

校园跑腿服务平台，后端 Spring Boot 3.2 + MyBatis-Plus，管理端 Vue 3 + TypeScript，移动端 uni-app。

## 项目结构

```
F:/ikeu_runningerrands/
├── backend/
│   ├── pom.xml                          # 父 POM（Maven 多模块）
│   ├── runningerrands-common/           # 共享：常量、异常、工具类、JWT、properties
│   ├── runningerrands-model/            # 实体、DTO、VO
│   ├── runningerrands-server/           # Spring Boot 应用
│   │   └── src/main/java/com/ikeu/server/
│   │       ├── controller/              # CommonController（根）
│   │       │   ├── admin/               # 管理端接口（/admin/**）
│   │       │   └── user/                # 用户端接口
│   │       ├── service/ + impl/
│   │       ├── mapper/                  # MyBatis-Plus Mapper
│   │       ├── config/                  # 配置类 + AdminInitializer
│   │       ├── interceptor/             # JWT 拦截器（admin + user 分离）
│   │       ├── aspect/                  # 切面：通知、操作日志、角色检查
│   │       ├── annotation/              # @SendNotification, @OperationLog, @RequireRole
│   │       └── task/                    # 定时任务
│   └── runningerrands.sql              # 建库建表 + ALTER 语句
├── frontend/
│   ├── admin/                           # Vue 3 + TS + Element Plus 管理端
│   └── mobile/                          # uni-app 微信小程序
└── .claude/
    └── settings.local.json              # 本地命令自动批准
```

## 角色与权限

| 角色 | 值 | Admin 表 | 权限 |
|------|----|---------|------|
| 超级管理员 | 1 | admin.role | 全部功能，包括员工管理、操作日志、禁止跑腿员接单 |
| 普通管理员 | 2 | admin.role | 仪表盘、用户/跑腿员/任务/订单/流水/消息管理、审核 |

- 仅超管可对普通管理员 CRUD；超管不能停用/删除自己
- 创建员工强制 role=2，超管仅一人
- `@RequireRole` 注解 + `RoleCheckAspect` 切面实现 RBAC

## 关键注解

- `@RequireRole({1, 2})` — 角色权限（默认仅超管）
- `@OperationLog(module, action, description)` — 自动记录操作日志
- `@SendNotification` — 方法成功后自动推送站内通知

## 常用命令

```bash
# 后端编译
cd F:/ikeu_runningerrands/backend && ./runningerrands-server/mvnw compile -q -DskipTests

# 后端启动
cd F:/ikeu_runningerrands/backend && ./runningerrands-server/mvnw spring-boot:run

# 管理端前端
cd F:/ikeu_runningerrands/frontend/admin
npm run dev          # 开发服务器（端口 3001，代理 /api → localhost:8080）
npx vue-tsc --noEmit # TypeScript 类型检查
npx vite build        # 生产构建

# 移动端（HBuilderX 项目，在 IDE 中编译运行）
```

## 今日工作总结（2026-06-01）

### Controller 分包
- 拆分 `controller/admin/`（6 个）+ `controller/user/`（9 个），`CommonController` 根级

### 操作日志系统
- `operation_log` 表 + 实体 + AOP（`@OperationLog`）+ 查询接口

### 员工管理 + RBAC
- `AdminEmployeeController` CRUD + `@RequireRole` 注解/切面
- 角色 1-超管 / 2-普通管理员（移除原角色 3-财务）

### 仪表盘增强
- `DashboardVO` 扩展：在线跑腿员、7 天用户趋势、收入趋势、任务分类占比

### 消息管理
- 管理员定向发送 + 全站广播通知

### 跑腿员管理增强
- 详情接口（含累计收入）、`is_banned` 禁止接单、接单时拦截检查

### 数据清理
- 移除 `user.role` 字段（冗余，跑腿员身份由 `runner_profile` 确定）

### 管理端前端（Vue 3 + TS + Element Plus + ECharts）
- 登录、仪表盘（3 图表）、用户管理、跑腿员管理、任务管理、订单管理、资金流水、消息管理、员工管理、操作日志、审核管理
- Axios 双令牌封装、侧边栏角色权限过滤

### 移动端修复
- `request.js` HTTP 非 200 时优先使用后端 `body.msg`
- 微信注册用户设置支付密码时提示初始密码 123456

## 可用技能 / Skills

| 技能 | 用途 |
|------|------|
| `/code-review` | 代码审查 |
| `/code-simplifier` | 代码精简化 |
| `/security-guidance` | 安全审查 |
| `/frontend-design` | 前端 UI 设计 |
| `/skill-creator` | 创建新技能 |
| `/commit-commands` | Git 提交辅助 |
| `/feature-dev` | 功能开发流程 |
| `/doc-coauthoring` | 文档协作 |
| `/frontend-slides` | 前端演示文稿 |

## 可用子代理 / Agents

| 代理 | 用途 |
|------|------|
| `code-reviewer` | 深度代码审查（正确性/可读性/架构/安全/性能） |
| `security-auditor` | 专项安全审计 |
| `test-engineer` | 测试策略与用例生成 |
| `code-simplifier` | 代码简化与重构 |
| `deep-code-reviewer` | 综合静态分析 + 自动修复 |
| `doc-sync-monitor` | 代码变更后同步文档 |
| `java-comment-enforcer` | Java 注释规范化 |
| `sre-debugger` | 错误日志/堆栈跟踪根因分析 |
| `understand-anything:*` | 代码库架构/领域知识分析 |
