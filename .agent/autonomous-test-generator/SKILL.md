---
name: "autonomous-test-generator"
description: "自动化测试排查代理，对修改的功能进行链路性测试：编译 → 单元测试 → 集成测试 → 回归测试，自动生成缺失测试并迭代修复至通过。"
model: sonnet
memory: user
---

# Autonomous Test Generator

## When to use this skill

**触发场景：**
- 后端 Java 代码修改（Service/Mapper/Controller）后需要验证
- 新增功能点，需要生成对应的单元测试和集成测试
- PR 提交前需要完整的测试覆盖
- 怀疑修改可能影响上下游功能

**DO NOT use when:**
- 纯配置文件修改（`application.yml`）→ 编译确认即可
- 纯文档修改（`README`, `docs/`）→ 不需要测试
- 纯前端代码修改（Vue/JS/CSS）→ 应由前端手动测试
- 测试依赖外部服务（短信/微信支付）且无法 mock → 手动测试

## How to use this skill

```
/agent:autonomous-test-generator --target <file|module> --mode <unit|integration|chain>
```

### 模式
- **unit**: 对指定文件的方法生成单元测试
- **integration**: 对指定模块生成集成测试
- **chain**: 对修改的功能进行全链路测试排查（默认）

### 工作流程

```
编译验证 → 识别变更范围 → 分析影响链路 → 生成/补充测试 → 执行 → 失败修复循环 → 报告
```

## General Rules

### 测试框架
- JUnit 5 + Mockito（单元测试）
- Spring Boot Test + 真实数据库（集成测试，按项目规范不 mock 数据库）
- `@SpringBootTest` + `@Transactional` 保证测试数据回滚

### 测试文件位置
```
backend/runningerrands-server/src/test/java/com/ikeu/server/
├── service/
│   └── impl/
│       └── AdminNotificationServiceImplTest.java
└── controller/
    └── admin/
        └── AdminNotificationControllerTest.java
```

### 自动迭代规则
1. 生成测试 → 执行 → 失败 → 分析失败原因 → 修复测试 → 再执行
2. 最多迭代 5 次，5 次仍未通过则报告失败原因并标记人工介入
3. 不修改被测源代码（测试的职责是发现 bug，不是为通过而改测试）
4. 测试失败如果是源码 bug，在报告中标注但不修改源码

### 测试命名规范
```
方法名_场景_预期结果
例：sendToUsers_ValidDTO_Success
例：sendToUsers_UserDisabled_SkipAndContinue
例：sendToUsers_NullUserId_ThrowsException
```

## CheckList

### 编译阶段
- [ ] `mvnw compile -q -DskipTests` 编译通过
- [ ] 无新增编译警告

### 变更分析
- [ ] 识别所有变更的 public 方法（新增/修改/删除）
- [ ] 追踪调用链路：Controller → Service → Mapper
- [ ] 标记受影响的上游调用方
- [ ] 标记受影响的数据库操作

### 测试生成
- [ ] 每个新增/修改的 public 方法有对应测试
- [ ] 覆盖正常路径（Happy Path）
- [ ] 覆盖边界条件（null、空集合、极值）
- [ ] 覆盖异常路径（业务异常、数据库异常）
- [ ] 集成测试验证完整链路（Controller → DB → Response）

### 执行验证
- [ ] `mvnw test` 全部通过
- [ ] 测试覆盖率不低于修改前的水平
- [ ] 无因测试导致的数据库污染

### 测试内容覆盖

#### Service 层测试重点
```java
@Test
void sendToUsers_ValidDTO_Success() {
    // 准备：构造 DTO + mock service
    // 执行：调用目标方法
    // 验证：verify notificationService.sendNotification() 被调用 N 次
}

@Test
void sendToUsers_UserDisabled_SkipAndContinue() {
    // 验证：禁用的用户被跳过，不抛异常
}

@Test
void sendToUsers_DatabaseException_LogErrorAndContinue() {
    // 验证：单用户失败不影响其他用户
}
```

#### Mapper 层测试重点
```java
@Test
void selectById_ValidId_ReturnsEntity() { ... }

@Test
void selectById_DeletedRecord_ReturnsNull() { ... }
```

#### Controller 层测试重点
```java
@Test
void send_ValidRequest_Returns200() {
    // MockMvc 模拟 HTTP 请求
    // 验证：响应 code = 1，HTTP status = 200
}
```

### 回归测试
- [ ] 运行模块内所有已有测试，确认无回归
- [ ] 如已有测试失败，分析是否由本修改引起
- [ ] 如由本修改引起，检查是否因为修改了公共接口签名
