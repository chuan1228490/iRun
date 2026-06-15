# Data Protection Rules

## Scope

适用于本项目中所有涉及用户数据的存储、传输和展示。

---

## 1. 个人敏感信息

| 数据类型 | 存储方式 | 日志 | 传输 | 展示 |
|---------|---------|------|------|------|
| 登录密码 | BCrypt 哈希 | 禁止 | HTTPS + body | 禁止 |
| 支付密码 | 独立加密 | 禁止 | HTTPS + body | 禁止 |
| 手机号 | 明文 | 禁止 | HTTPS | 脱敏 138****8000 |
| 身份证号 | 明文 | 禁止 | HTTPS | 脱敏 |
| 学号 | 明文 | 允许（非敏感） | HTTPS | 管理端可见 |
| 真实姓名 | 明文 | 允许 | HTTPS | 管理端/本人可见 |

## 2. 凭证照片（学生证）

- 存储于阿里云 OSS，URL 不对外公开
- 仅审核管理员和用户本人可查看
- 审核完成后可考虑定期清理

## 3. 配置文件安全

### Good
```yaml
runningerrands:
  jwt:
    admin-secret-key: ${RUNNING_ERRANDS_JWT_ADMIN_SECRET:}
  alioss:
    access-key-id: ${ALIYUN_ACCESS_KEY_ID:}
    access-key-secret: ${ALIYUN_ACCESS_KEY_SECRET:}
  wechat:
    app-secret: ${WECHAT_APP_SECRET:}
```

### Bad
```yaml
runningerrands:
  jwt:
    admin-secret-key: my-real-secret-key-abc123   # ❌ 硬编码
  alioss:
    access-key-id: LTAI5tAtqCb8eCy5               # ❌ 真实密钥
```

## 4. 前端数据安全

- Token 存储在 localStorage/uni.storage，不暴露在 URL 参数中
- 支付密码弹窗使用安全输入（`pay-password-dialog` 组件）
- 管理端退出时清除 `admin_token` + `admin_refresh_token`
- 移动端登录/退出时自动连接/断开 STOMP WebSocket
