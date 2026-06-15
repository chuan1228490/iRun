# Chain Testing Guide

## Scope

适用于对修改功能进行端到端的链路性测试排查。

---

## 链路追踪

**Rule**: 每次修改后，沿着数据流方向追踪受影响范围。

### 追踪路径

```
HTTP Request → Controller → Service → Mapper → DB
                   ↓            ↓
              Interceptor    Cache (@Cacheable/@RedisDefend)
                   ↓            ↓
              Aspect        Redisson (RLock)
                   ↓            ↓
              Response      Notification/WebSocket
```

### 分析步骤

1. **向上追踪**：谁调用了被修改的方法？（Controller → API 端点）
2. **向下追踪**：被修改的方法调用了谁？（Service → Mapper → SQL）
3. **横向追踪**：哪些切面/拦截器会影响这个方法？（@Transactional, @Cacheable, @RequireRole, @OperationLog）
4. **异步追踪**：是否有异步/定时任务依赖这个方法？（@Async, @Scheduled）

---

## 测试生成优先级

| 优先级 | 测试类型 | 覆盖内容 |
|--------|---------|---------|
| P0 | Happy Path | 正常输入 → 期望输出，验证功能可用 |
| P1 | 边界条件 | null、空集合、零值、极值、过期/未过期 |
| P1 | 异常路径 | 业务异常、数据库异常、网络异常 |
| P2 | 并发 | 重复提交、分布式锁竞争 |
| P2 | 回归 | 模块内已有测试必须通过 |

---

## 测试模板

### Service 单元测试
```java
@ExtendWith(MockitoExtension.class)
class XxxServiceImplTest {

    @Mock private XxxMapper xxxMapper;
    @Mock private NotificationService notificationService;
    @InjectMocks private XxxServiceImpl xxxService;

    @Test
    void methodName_HappyPath_Success() {
        // Given
        when(xxxMapper.selectById(anyLong())).thenReturn(mockEntity);

        // When
        var result = xxxService.methodName(input);

        // Then
        assertThat(result).isNotNull();
        verify(xxxMapper).selectById(anyLong());
    }

    @Test
    void methodName_NullInput_ThrowsException() {
        assertThrows(BusinessException.class, () -> xxxService.methodName(null));
    }
}
```

### Controller 集成测试
```java
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminNotificationControllerTest {

    @Autowired private MockMvc mockMvc;

    @Test
    void send_ValidDTO_ReturnsSuccess() throws Exception {
        mockMvc.perform(post("/admin/notifications/send")
                .header("token", "valid-test-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"userIds":[1,2,3],"type":1,"title":"测试","content":"内容"}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1));
    }
}
```

---

## 失败处理

| 失败原因 | 处理方式 |
|---------|---------|
| 测试代码错误（assert 写错） | 修复测试代码，重试 |
| Mock 配置不足（when 未覆盖分支） | 补充 mock，重试 |
| 被测代码存在真实 bug | 标记 CRITICAL，不修改源码，报告中注明 |
| 数据库连接失败 | 检查 dev 配置，重试 |
| 已有测试回归失败 | 优先排查是否修改了公共接口 |
