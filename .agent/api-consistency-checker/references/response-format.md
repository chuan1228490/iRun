# Response Format Rules

## Scope

适用于所有接口返回值。

---

## 1. 统一响应结构

**Rule**: 所有接口通过 `Result<T>` 包装。

```json
// 成功 — 单对象
{ "code": 1, "msg": "操作成功", "data": { "id": 1, "name": "张三" } }

// 成功 — 分页
{ "code": 1, "msg": "操作成功", "data": { "total": 100, "records": [...] } }

// 成功 — 无数据
{ "code": 1, "msg": "操作成功", "data": null }

// 失败
{ "code": 0, "msg": "用户名已存在" }
```

## 2. 返回值类型映射

| 场景 | Controller 返回类型 | 调用方式 |
|------|-------------------|---------|
| 查询单对象 | `Result<XxxVO>` | `Result.success(xxxVO)` |
| 查询列表（分页） | `Result<PageResult<XxxVO>>` | `Result.success(pageResult)` |
| 创建/更新/删除 | `Result<Void>` | `Result.success()` 或 `Result.success(MessageConstant.SUCCESS)` |

### Good
```java
@GetMapping("/admin/orders/{id}")
public Result<OrderDetailVO> getDetail(@PathVariable Long id) {
    return Result.success(adminOrderService.getOrderDetail(id));
}

@PutMapping("/admin/orders/{id}/status")
public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
    adminOrderService.updateOrderStatus(id, status);
    return Result.success();
}
```

### Bad
```java
@GetMapping("/admin/orders")
public PageResult<OrderManageVO> list() {   // ❌ 缺少 Result 包装
    return adminOrderService.listAllOrders(null, 1, 10);
}

@PutMapping("/admin/orders/{id}/status")
public String updateStatus() {              // ❌ 返回类型错误
    return "success";
}
```

## 3. 错误处理

**Rule**: Controller 不手动构建错误响应，由全局异常处理器统一处理。

### Good
```java
// Service 层抛业务异常
if (order == null) {
    throw new NotFoundException(MessageConstant.ORDER_NOT_EXIST);
}
// Controller 不做 try-catch
```

### Bad
```java
// Controller 中手动构建错误
@GetMapping("/admin/orders/{id}")
public Result<?> getDetail(@PathVariable Long id) {
    try {
        return Result.success(service.getDetail(id));
    } catch (Exception e) {
        return Result.error(500, e.getMessage());  // ❌
    }
}
```
