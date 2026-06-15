# Database Schema & Redis Rules

## Scope

适用于 `backend/` 下所有涉及 MySQL 数据库和 Redis 缓存的代码。

---

## MySQL 规范

### 1. 建表规范

**Rule**: 使用 InnoDB + utf8mb4，每列有 COMMENT，主键 BIGINT UNSIGNED AUTO_INCREMENT。

### Good
```sql
CREATE TABLE `task_order`
(
    `id`              BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `task_id`         BIGINT UNSIGNED NOT NULL COMMENT '关联的任务ID',
    `runner_id`       BIGINT UNSIGNED NOT NULL COMMENT '跑腿员用户ID',
    `status`          TINYINT        DEFAULT 0 COMMENT '订单状态：0-待确认 1-进行中 2-已完成 3-已取消',
    `accept_time`     DATETIME       DEFAULT NULL COMMENT '接单时间',
    `created_at`      DATETIME       DEFAULT CURRENT_TIMESTAMP,
    `updated_at`      DATETIME       DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (`id`),
    KEY `idx_task_id` (`task_id`),
    KEY `idx_runner_id` (`runner_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='任务订单表';
```

### Bad
```sql
CREATE TABLE orders (            # ❌ 缺少 COMMENT
    id INT,                      # ❌ 未使用 UNSIGNED BIGINT，无 AUTO_INCREMENT
    task_id INT                  # ❌ 无索引
);
```

### 2. Entity 同步 SQL

**Rule**: Entity 字段变更时必须同步 `runningerrands.sql`。

### Good
```java
// User.java
@TableField("new_field")
private String newField;
```
```sql
-- runningerrands.sql
ALTER TABLE `user` ADD COLUMN `new_field` VARCHAR(32) DEFAULT '' COMMENT '新字段';
```

### Bad
```java
// Entity 加了字段但 SQL 没更新 → 测试/生产环境建表缺失
private String newField;
```

### 3. 字段类型约定

| Java 类型 | MySQL 类型 | 说明 |
|-----------|-----------|------|
| `Long` / `BigInteger` (ID) | `BIGINT UNSIGNED` | 主键/外键 |
| `String` (短文本) | `VARCHAR(N)` | N 取合理值 |
| `String` (URL) | `VARCHAR(512)` | 图片/头像URL |
| `String` (手机号) | `CHAR(11)` | 固定长度 |
| `BigDecimal` (金额) | `DECIMAL(10,2)` | 精确小数 |
| `Integer` / `TINYINT` | `TINYINT` | 状态/标志/类型 |
| `LocalDateTime` | `DATETIME` | 时间字段 |
| `Boolean` | `TINYINT(1)` | 布尔标记 |

### 4. MyBatis-Plus 实体规范

**Rule**: Entity 必须有 `@TableName` + `@TableId(type = IdType.AUTO)`。

### Good
```java
@Data
@TableName("task_order")
public class TaskOrder {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long runnerId;
    private Integer status;
    private LocalDateTime acceptTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

### Bad
```java
public class TaskOrder {         // ❌ 缺少 @TableName
    private Long id;             // ❌ 缺少 @TableId
}
```

### 5. SQL 查询规范

**Rule**: 使用 LambdaWrapper 参数化查询，禁止字符串拼接。

### Good
```java
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(User::getPhone, phone)
       .eq(User::getStatus, 1)
       .orderByDesc(User::getCreatedAt);
userMapper.selectList(wrapper);
```

### Bad
```java
String sql = "SELECT * FROM user WHERE phone = '" + phone + "'";  // ❌ SQL 注入风险
```

---

## Redis 规范

### 1. 缓存命名约定

| 前缀 | 用途 | TTL | 示例 Key |
|------|------|-----|---------|
| `admin:dashboard` | 仪表盘缓存 | 3 min | `admin:dashboard:summary` |
| `task:hall` | 任务大厅分页缓存 | 2 min | `task:hall:1:10` |
| `task:detail` | 任务详情缓存 | 10 min | `task:detail:123` |
| `runner:leaderboard` | 排行榜缓存 | 5 min | `runner:leaderboard:orderCount:10` |
| `lock:order` | 订单分布式锁 | 10s hold | `lock:order:accept:123` |
| `login:fail` | 登录失败计数 | 300s | `login:fail:admin` |

### 2. Spring Cache 注解

**Rule**: 使用 `@Cacheable` / `@CacheEvict` 管理缓存，缓存在 `RedisConfiguration` 中统一注册。

### Good
```java
@Cacheable(value = RedisConstant.CACHE_DASHBOARD, key = "'summary'")
public DashboardVO getDashboardSummary() { ... }

@CacheEvict(value = RedisConstant.CACHE_DASHBOARD, allEntries = true)
public void updateOrderStatus(Long orderId, Integer status) { ... }
```

### Bad
```java
// 手动操作 RedisTemplate 管理缓存生命周期 — 应用层缓存用注解更清晰
public DashboardVO getDashboardSummary() {
    String key = "dashboard:summary";
    String cached = redisTemplate.opsForValue().get(key);
    if (cached != null) return JSON.parseObject(cached, DashboardVO.class);
    // ...
}
```

### 3. 缓存防护（@RedisDefend）

**Rule**: 高并发读接口使用 `@RedisDefend` + `RedisDefendUtil.getOrLoad()` 组合防护。

```java
@RedisDefend  // 自动缓存穿透 + 击穿防护
public PageResult<TaskListVO> getTaskHall(int page, int size) {
    return redisDefendUtil.getOrLoad(
        RedisConstant.CACHE_TASK_HALL,
        key,                    // 缓存 key
        () -> queryFromDB(page, size),  // 缓存未命中时执行
        PageResult.class
    );
}
```

### 4. 分布式锁

**Rule**: 使用 Redisson `RLock`，锁前缀定义在 `RedisConstant`。

### Good
```java
String lockKey = RedisConstant.LOCK_ORDER_ACCEPT + orderId;
RLock lock = redissonClient.getLock(lockKey);
try {
    if (lock.tryLock(3, 10, TimeUnit.SECONDS)) {  // 等待 3s，持有 10s
        // 业务逻辑
    }
} finally {
    if (lock.isHeldByCurrentThread()) lock.unlock();
}
```

### Bad
```java
// 使用 SETNX 手动实现锁 → 已统一用 Redisson
Boolean locked = redisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
```

### 5. 缓存清除策略

| 操作 | 策略 |
|------|------|
| 仪表盘写操作 | `@CacheEvict(allEntries = true)` 全量清除 |
| 任务状态变更 | 任务相关缓存逐出 + 仪表盘全量清除 |
| 订单状态变更 | 同任务 |
| 排行榜 | TTL 自然过期，不主动清除 |
