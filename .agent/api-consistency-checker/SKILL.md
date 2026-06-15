---
name: "api-consistency-checker"
description: "API 一致性检查代理，确保 REST 接口遵循项目规范：URL 命名、返回值格式、错误码、Swagger 文档完整性。"
model: sonnet
memory: user
---

# API Consistency Checker

## When to use this skill

**触发场景：**
- 新增 Controller 或 API 端点
- 重构 API 路径或参数
- PR 审查中需要验证 API 一致性
- 发现多个接口格式不统一，需要排查
- 文档与实现不一致

**DO NOT use when:**
- 纯后端业务逻辑审查 → `backend-code-reviewer`
- 纯前端组件审查 → `frontend-code-reviewer`
- 安全漏洞审查 → `security-auditor`
- 只是修改已有接口的内部实现（不改签名） → 不需要

## How to use this skill

```
/agent:api-consistency-checker --scope <file|directory|branch>
```

1. 扫描目标范围中的所有 Controller 文件
2. 逐接口检查 URL、参数、返回值、注解
3. 输出不一致项列表 + 修复建议
4. 如发现安全风险，标记并建议调用 `security-auditor`

## General Review Rules

### 自动修复（可直接应用）
- 返回值包装不一致：`return data;` → `return Result.success(data);`
- Swagger 注解缺失：补充 `@Operation(summary = "...")`
- DTO 缺少校验注解：补充 `@Valid`
- URL 风格不一致：修正复数/单数

### 必须人工确认
- 接口路径重新设计（影响前端调用）
- API 版本升级
- 废弃旧接口

## CheckList

### URL 规范
- [ ] 管理端接口：`/admin/<plural-resource>`（如 `/admin/orders`, `/admin/users`）
- [ ] 用户端接口：`/user/<plural-resource>`（如 `/user/tasks`, `/user/orders`）
- [ ] 通用接口：`/common/<resource>`（如 `/common/upload`）
- [ ] 资源名使用复数小写，多单词用短横线（如 `notification-settings`）
- [ ] RESTful 风格：GET 查询、POST 创建、PUT 更新、DELETE 删除
- [ ] 路径无冗余前缀/后缀（如 `/api/admin/orders` → `/admin/orders`，因为 `server.servlet.context-path=/api`）

### 参数规范
- [ ] 单个 ID 使用 `@PathVariable`：`GET /admin/orders/{id}`
- [ ] 列表查询使用 `@RequestParam`：`GET /admin/orders?status=1&page=1&size=10`
- [ ] 创建/更新使用 `@RequestBody` + DTO，DTO 有 `@Valid` 校验
- [ ] 分页参数统一：`page`（默认 1）、`size`（默认 10）

### 返回值规范
- [ ] 所有接口返回 `Result<T>` 或 `Result<PageResult<T>>`
- [ ] 成功：`Result.success()` 或 `Result.success(data)`
- [ ] 失败：由全局异常处理器统一返回 `Result.error(code, msg)`，不在 Controller 中手动构建
- [ ] 分页查询返回 `PageResult<T>` 包含 `total` 和 `records`

### Swagger 文档
- [ ] Controller 类有 `@Tag(name = "管理端-XXX接口")` 或 `@Tag(name = "用户端-XXX接口")`
- [ ] 每个方法有 `@Operation(summary = "xxx")`
- [ ] DTO/VO 字段有 `@Schema(description = "xxx")`
- [ ] 参数有 `@Parameter(description = "xxx")`（可选，但推荐）

### 认证 & 权限
- [ ] 管理端接口：`token` 请求头（非 `Authorization: Bearer`），由 `JwtTokenAdminInterceptor` 校验
- [ ] 用户端接口：`authentication` 请求头，由 `JwtTokenUserInterceptor` 校验
- [ ] 敏感操作有 `@RequireRole` 注解

### 返回码规范
- [ ] 成功统一返回 `code = 1`
- [ ] 业务异常由全局异常处理器映射 HTTP 状态码 + 业务错误码
- [ ] 不在 Controller 中 `return Result.error(...)`（除非有特殊需求）
