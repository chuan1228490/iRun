---
name: "docker-test-agent"
description: "Docker 环境自动化 API 测试。启动容器→注入测试数据→执行 API 测试→验证安全机制→输出测试报告。每次后端代码修改后必须触发。"
model: sonnet
memory: user
---

# Docker Test Agent

## When to use this skill

**触发场景（必须触发）：**
- 后端 Java 代码修改后（Controller/Service/Mapper/Entity/DTO/Config）
- 安全相关变更（JWT、密码、验证码、权限、SQL）
- 新 API 端点或修改现有端点签名
- 数据库 schema 变更
- Git commit 前验证
- 用户明确要求"测试"、"验证"、"检查"

**触发时机：**
- 在 `deep-code-reviewer` 或 `backend-code-reviewer` 完成代码审查后
- 在 git commit 之前（作为 pre-commit 验证）
- 用户说"测试一下"、"验证修改"、"跑一下测试"

**DO NOT use when:**
- 仅修改前端 Vue/uni-app 页面（无后端变更）
- 仅修改文档（docs/*.md、README）
- 仅修改配置文件（application-*.yml）且已通过编译验证
- Docker 未安装或不可用 → 跳过并告知用户，仅做编译验证

## How to use this skill

```
/agent:docker-test-agent --scope <changed-files> --focus <auth|task|order|user|security>
```

### 执行流程

1. **环境检查** — 确认 Docker 运行，`docker compose` 可用
2. **启动服务** — `docker compose up -d`（如已运行则跳过）
3. **等待就绪** — 轮询 `/api/actuator/health` 直到 UP
4. **确定测试范围** — 根据 `--scope` 匹配测试场景
5. **准备测试数据** — 注入 SMS 验证码到 Redis、登录获取 JWT
6. **执行 API 测试** — 按测试场景逐项调用
7. **验证安全机制** — 如果涉及安全变更，验证防护是否生效
8. **输出报告** — 通过/失败 + 详情

## Test Scenarios

测试场景按变更类型自动匹配（详见 `references/test-scenarios.md`）：

| 变更类型 | 测试范围 |
|---------|---------|
| Controller 新增/修改 | 全部端点 200/400/401/403 状态码验证 |
| Service 业务逻辑 | 正常路径 + 边界条件 + 异常路径 |
| 安全相关 | 认证绕过、越权访问、暴力破解防护、验证码隔离 |
| Entity/Model | 序列化/反序列化、字段映射 |
| SMS/验证码 | Redis 注入验证码 → 验证操作隔离 + 一次性消费 |

## Docker 环境

### 服务信息

| 服务 | 地址 | 端口 |
|------|------|:--:|
| Backend API | `http://localhost:8080/api` | 8080 |
| MySQL | `localhost:3308` (root/root123) | 3308 |
| Redis | `localhost:6381` (db=1) | 6381 |

### 预置测试账号

| 用户名 | 密码 | ID | 角色 |
|--------|------|:--:|------|
| `testuser` | `123456` | 1 | 用户 + 跑腿员 |
| `testuser2` | `123456` | 2 | 普通用户 |
| `admin` | `admin` | — | 超级管理员 |

### SMS 验证码注入

Docker 环境无阿里云 SMS，测试验证码相关接口时直接写 Redis：

```bash
# 注入验证码（TTL 300s）
docker exec rr-redis redis-cli -n 1 SET "user:code:<operation>:<phone>" "888888" EX 300

# 各操作对应的 operation:
# 注册: register      登录: login       修改手机: change_phone
# 重置登录密码: reset_password    重置支付密码: reset_pay_password
```

## Agent 工作流

### Step 1: 确保 Docker 运行

```bash
cd F:/ikeu_runningerrands/docker
docker compose ps  # 检查状态
# 如果未运行: docker compose up -d
# 等待健康检查: curl -s http://localhost:8080/api/actuator/health
```

### Step 2: 分析变更范围

读取 git diff，识别变更文件类型，匹配测试场景。

### Step 3: 执行测试

对每个匹配的测试场景：
1. 注入必要的测试数据（SMS code、JWT token）
2. 发送 API 请求
3. 验证响应码 + 业务 code + 数据状态
4. 记录结果

### Step 4: 输出报告

```
=== Docker 自动化测试报告 ===
服务状态: UP
测试范围: [auth, user]
测试用例: 8 通过, 0 失败, 0 跳过
安全验证: ✅ S1 验证码隔离 ✅ S2 暴力破解锁 ✅ 越权拒绝
=== 全部通过 ===
```

## 关键测试命令

```bash
# 健康检查
curl -s http://localhost:8080/api/actuator/health

# 用户登录获取 JWT
TOKEN=$(curl -s -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"loginType":1,"username":"testuser","password":"123456"}' \
  | sed 's/.*"token":"\([^"]*\)".*/\1/')

# 管理员登录
ADMIN_TOKEN=$(curl -s -X POST http://localhost:8080/api/admin/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' \
  | sed 's/.*"token":"\([^"]*\)".*/\1/')

# 查看日志
docker logs rr-backend --tail 50
```

## 参考规范

| 文件 | 内容 |
|------|------|
| `references/test-scenarios.md` | 完整测试场景清单（认证/任务/订单/用户/安全） |
| `references/api-endpoints.md` | 全部 API 端点 + 请求/响应格式 |
| `references/docker-commands.md` | Docker 运维命令速查 |
