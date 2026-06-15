# Backend Exploration Guide

## Scope

适用于 `backend/` 下完整的 Spring Boot 后端探索。

---

## 探索步骤

### Step 1: 模块结构

```
backend/
├── pom.xml                              # 父 POM，模块声明
├── runningerrands-common/               # 共享：constant, enums, exception, result, utils
│   └── src/main/java/com/ikeu/common/
├── runningerrands-model/                # Entity, DTO, VO
│   └── src/main/java/com/ikeu/model/
├── runningerrands-server/               # Spring Boot 应用主模块
│   └── src/main/java/com/ikeu/server/
│       ├── controller/                  # CommonController + admin/ + user/
│       ├── service/ + impl/             # 接口 + 实现
│       ├── mapper/                      # MyBatis-Plus Mapper + XML
│       ├── config/                      # 配置类 + AdminInitializer
│       ├── interceptor/                 # JWT 拦截器
│       ├── aspect/                      # 切面（通知/日志/角色）
│       ├── annotation/                  # @SendNotification, @OperationLog, @RequireRole
│       ├── task/                        # 定时任务
│       └── websocket/                   # STOMP WebSocket
└── runningerrands.sql                   # 建库建表
```

### Step 2: 请求链路追踪

以「管理员查看订单列表」为例：

```
GET /api/admin/orders?status=1&page=1&size=10
  │
  ├─ [Filter] JwtTokenAdminInterceptor
  │   提取 token 头 → 解析 JWT → 设置 AdminContext
  │
  ├─ [Aspect] RoleCheckAspect
  │   检查 @RequireRole({1, 2}) → role ∈ {1, 2}?
  │
  ├─ [Controller] AdminOrderController.listAllOrders()
  │   参数绑定 → 调用 service
  │
  ├─ [Service] AdminOrderServiceImpl.listAllOrders()
  │   LambdaQueryWrapper 构建 → taskOrderMapper.selectPage()
  │   → 批量查询 Task + User → 组装 OrderManageVO
  │   → 返回 PageResult<OrderManageVO>
  │
  ├─ [Aspect] OperationLogAspect
  │   记录 @OperationLog(module, action, description)
  │
  └─ [Response] Result<PageResult<OrderManageVO>>
     { "code": 1, "data": { "total": 100, "records": [...] } }
```

### Step 3: 缓存策略追踪

| 场景 | 注解/机制 | Key 格式 | TTL |
|------|---------|---------|-----|
| 仪表盘摘要 | `@Cacheable("admin:dashboard")` | `admin:dashboard:summary` | 3 min |
| 仪表盘趋势 | `@Cacheable("admin:dashboard")` | `admin:dashboard:userTrend` | 3 min |
| 任务大厅 | `@RedisDefend` + `RedisDefendUtil` | `task:hall:1:10` | 2 min |
| 任务详情 | `@RedisDefend` | `task:detail:123` | 10 min |
| 排行榜 | `@Cacheable("runner:leaderboard")` | `runner:leaderboard:orderCount:10` | 5 min |
| 写操作 | `@CacheEvict(allEntries = true)` | 全量清除仪表盘缓存 | — |
| 接单/支付 | Redisson `RLock` | `lock:order:accept:123` | 3s wait, 10s hold |

### Step 4: 外部依赖

| 依赖 | 用途 | 配置位置 |
|------|------|---------|
| MySQL 8 | 主数据库 | `spring.datasource` |
| Redis | 缓存 + 分布式锁 + 登录保护 | `spring.data.redis` |
| Redisson | 分布式锁 | `RedissonConfiguration` |
| 阿里云 OSS | 图片/文件存储 | `runningerrands.alioss` |
| 阿里云 SMS | 短信验证码 | `runningerrands.sms` |
| 腾讯地图 API | 地理编码/距离计算 | `runningerrands.map` |
| 微信小程序 | 登录/用户信息 | `runningerrands.wechat` |
