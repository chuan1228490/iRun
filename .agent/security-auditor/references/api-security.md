# API Security Rules

## Scope

适用于 `backend/` 下所有 REST API 端点。

---

## 1. 认证隔离

**Rule**: 管理端和用户端使用独立的 JWT 密钥和拦截器，不可混用。

### Good
```java
// 管理端 → JwtTokenAdminInterceptor，从 token 头提取
@GetMapping("/admin/orders")
public Result<...> listOrders() { ... }

// 用户端 → JwtTokenUserInterceptor，从 authentication 头提取
@GetMapping("/user/tasks")
public Result<...> listTasks() { ... }
```

### Bad
```java
// 管理端接口用了用户端 token → 权限边界模糊
@GetMapping("/admin/orders")
public Result<...> listOrders(@RequestHeader("authentication") String token) { ... }
```

---

## 2. 越权防护

**Rule**: 涉及用户私有数据的操作必须校验归属关系。

### Good
```java
// 查询订单前校验归属
TaskOrder order = taskOrderMapper.selectById(orderId);
if (order == null || !order.getRunnerId().equals(currentUserId)) {
    throw new ForbiddenException(MessageConstant.NO_PERMISSION);
}
```

### Bad
```java
// 直接操作，不校验归属
public void cancelOrder(Long orderId) {
    TaskOrder order = taskOrderMapper.selectById(orderId);
    order.setStatus(StatusConstant.ORDER_CANCELLED);
    taskOrderMapper.updateById(order);
}
```

---

## 3. 敏感信息不泄露

**Rule**: 日志、响应、异常信息中不包含密码、token、身份证号、手机号明文。

### Good
```java
log.info("用户 {} 登录成功", userId);           // 使用 ID
log.error("发送通知失败，userId={}", userId, e);  // 不打印 token
```

### Bad
```java
log.info("token: {}, 密码: {}", token, password);  // ❌ 明文
log.info("用户 {} 手机号 {} 修改了密码", phone, newPwd); // ❌
```

---

## 4. SQL 注入防护

**Rule**: 使用 MyBatis-Plus LambdaWrapper，禁止字符串拼接 SQL。

### Good
```java
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(User::getPhone, phone);   // 参数化查询
userMapper.selectOne(wrapper);
```

### Bad
```java
String sql = "SELECT * FROM user WHERE phone = '" + phone + "'"; // ❌ 拼接
```

---

## 5. 频率限制

**Rule**: 验证码发送、登录尝试、支付操作必须有频率限制。

本项目实现：
- 登录：Redis 计数，5 次错误锁定 300s（`LoginAttemptService`）
- 验证码：Redis TTL 限制发送间隔
- 支付：分布式锁防并发（Redisson `RLock`）

---

## 6. 密码安全

**Rule**: 密码使用 BCrypt 加密，支付密码独立加密，不允许明文存储或对比。

### Good
```java
String encoded = passwordEncoder.encode(rawPassword);
boolean match = passwordEncoder.matches(rawPassword, encodedPassword);
```

### Bad
```java
if (inputPassword.equals(user.getPassword())) { ... }  // ❌ 明文对比
```
