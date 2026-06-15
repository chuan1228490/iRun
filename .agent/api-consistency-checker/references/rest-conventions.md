# REST API Conventions

## Scope

适用于 `backend/runningerrands-server/src/main/java/com/ikeu/server/controller/` 下所有 Controller。

---

## 1. URL 设计

**Rule**: `{context-path}/<admin|user>/<plural-resource>[/{id}][/action]`

### Good
```
GET    /admin/orders              # 查询订单列表
GET    /admin/orders/{id}         # 查询订单详情
PUT    /admin/orders/{id}/status  # 修改订单状态
POST   /user/tasks                # 发布任务
DELETE /user/tasks/{id}           # 删除任务
POST   /admin/notifications/send  # 发送通知（动作后缀）
POST   /admin/notifications/broadcast # 广播通知
```

### Bad
```
GET /admin/getOrderList           # ❌ 动词 + 非 RESTful
GET /admin/order/{id}             # ❌ 单数
POST /admin/order/updateStatus    # ❌ 动词路径 + 非 RESTful
```

---

## 2. HTTP 方法语义

| 方法 | 语义 | 幂等 | 示例 |
|------|------|------|------|
| GET | 查询 | 是 | `GET /admin/users?page=1&size=10` |
| POST | 创建/操作 | 否 | `POST /user/tasks/publish` |
| PUT | 全量更新 | 是 | `PUT /admin/orders/{id}/status` |
| DELETE | 删除 | 是 | `DELETE /user/addresses/{id}` |

---

## 3. 分页参数

**Rule**: 参数名统一为 `page` 和 `size`。

### Good
```java
@GetMapping("/admin/orders")
public Result<PageResult<OrderManageVO>> list(
    @RequestParam(defaultValue = "1") int page,
    @RequestParam(defaultValue = "10") int size) { ... }
```

### Bad
```java
@GetMapping("/admin/orders")
public Result<PageResult<OrderManageVO>> list(
    @RequestParam(defaultValue = "1") int pageNum,    // ❌ pageNum
    @RequestParam(defaultValue = "10") int pageSize) { // ❌ pageSize
```

---

## 4. Controller 包结构

| 路径前缀 | 包位置 | 拦截器 |
|---------|--------|--------|
| `/admin/**` | `controller/admin/` | `JwtTokenAdminInterceptor` |
| `/user/**` | `controller/user/` | `JwtTokenUserInterceptor` |
| `/common/**` | `controller/` | 无需认证或按需 |
