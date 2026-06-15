# Backend Architecture Rules

## Scope

适用于 `backend/` 下所有 Java 代码，涵盖 Controller → Service → Mapper → Entity 四层架构。

---

## 1. 分层职责

**Rule**: 每层只做自己的事，不跨层调用。

### Good
```java
// Controller: 只做参数接收和结果返回
@RestController
@RequiredArgsConstructor
public class AdminOrderController {
    private final AdminOrderService adminOrderService;

    @GetMapping("/admin/orders")
    public Result<PageResult<OrderManageVO>> list(@RequestParam int page, @RequestParam int size) {
        return Result.success(adminOrderService.listAllOrders(null, page, size));
    }
}

// Service: 业务逻辑 + 组装
@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {
    private final TaskOrderMapper taskOrderMapper;
    // 不直接注入其他 Controller 的 Mapper
}
```

### Bad
```java
// Controller 直接操作 Mapper — 违反分层
@RestController
public class AdminNotificationController {
    private final UserMapper userMapper;         // ❌ Controller 不应注入 Mapper
    private final NotificationService notificationService;

    @PostMapping("/notifications/send")
    public Result<Void> send(@RequestBody NotificationSendDTO dto) {
        for (Long userId : dto.getUserIds()) {
            User user = userMapper.selectById(userId); // ❌ 业务逻辑在 Controller
            // ...
        }
    }
}
```

---

## 2. 依赖注入

**Rule**: 构造器注入，禁止字段注入。

### Good
```java
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
}
```

### Bad
```java
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;       // ❌ 字段注入
    @Autowired
    private PasswordEncoder passwordEncoder;
}
```

---

## 3. 返回值规范

**Rule**: 统一使用 `Result<T>` (单对象) 和 `PageResult<T>` (分页)。

### Good
```java
public Result<UserInfoVO> getUserInfo(Long userId) { ... }
public Result<PageResult<TaskListVO>> listTasks(int page, int size) { ... }
public Result<Void> deleteUser(Long id) { ... }
```

### Bad
```java
public UserInfoVO getUserInfo(Long userId) { ... }           // ❌ 未包装
public Map<String, Object> listTasks(int page, int size) { ... } // ❌ 使用 Map
```

---

## 4. 异常处理

**Rule**: 使用项目自定义业务异常，不抛裸 RuntimeException。

### Good
```java
if (user == null) {
    throw new NotFoundException(MessageConstant.USER_NOT_EXIST);
}
if (order.getStatus().equals(StatusConstant.ORDER_CANCELLED)) {
    throw new BusinessException(MessageConstant.ORDER_ALREADY_CANCELLED);
}
```

### Bad
```java
if (user == null) {
    throw new RuntimeException("用户不存在");   // ❌ 裸异常
}
```

---

## 5. 事务管理

**Rule**: 写操作加 `@Transactional`，只读操作加 `@Transactional(readOnly = true)`（可选）。

### Good
```java
@Override
@Transactional
public void createOrder(CreateOrderDTO dto) {
    orderMapper.insert(order);
    taskMapper.updateById(task);
}
```

### Bad
```java
@Override
public void createOrder(CreateOrderDTO dto) {     // ❌ 缺少事务
    orderMapper.insert(order);
    taskMapper.updateById(task);                  // 如果这里失败，order 不会回滚
}
```

---

## 6. 日志规范

**Rule**: 使用 `@Slf4j`，不打印敏感信息。

### Good
```java
log.info("用户 {} 下单成功，订单ID: {}", userId, orderId);
log.error("向用户 {} 发送通知失败", userId, e);
```

### Bad
```java
log.info("token: {}, 密码: {}", token, password);  // ❌ 打印敏感信息
System.out.println("用户登录了");                  // ❌ 使用 System.out
```
