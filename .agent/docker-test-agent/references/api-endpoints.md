# API 端点速查

## 认证头
- 用户端: `authentication: <jwt>` (JwtTokenUserInterceptor)
- 管理端: `token: <jwt>` (JwtTokenAdminInterceptor)
- 公开: 无需认证头

## 端点清单

### 用户端 — 认证 (UserController)
| 方法 | 路径 | 认证 | 说明 |
|------|------|:--:|------|
| POST | `/user/login` | none | 密码/验证码登录 |
| POST | `/user/register` | none | 注册 |
| POST | `/user/wechat-login` | none | 微信登录 |
| GET | `/user/info` | user | 获取用户信息 |
| PUT | `/user/profile` | user | 修改资料 |
| PUT | `/user/password` | user | 修改登录密码（需旧密码） |
| PUT | `/user/password/reset` | user | 重置登录密码（SMS） |
| PUT | `/user/pay-password` | user | 首次设置支付密码 |
| PUT | `/user/pay-password/change` | user | 修改支付密码（需旧密码） |
| PUT | `/user/pay-password/reset` | user | 重置支付密码（SMS） |
| GET | `/user/pay-password/status` | user | 查询支付密码状态 |
| POST | `/user/send` | none | 发送 SMS 验证码 |
| PUT | `/user/phone` | user | 修改手机号 |
| POST | `/user/certify` | user | 提交实名认证 |
| GET | `/user/certify/status` | user | 认证状态 |
| DELETE | `/user/account` | user | 注销账户 |
| POST | `/user/refresh` | none | 刷新 Token |

### 用户端 — 任务 (TaskController)
| 方法 | 路径 | 认证 | 说明 |
|------|------|:--:|------|
| POST | `/user/task` | user | 发布任务 |
| GET | `/user/task/hall` | user | 任务大厅 |
| GET | `/user/task/detail/{id}` | user | 任务详情 |
| POST | `/user/task/{id}/cancel` | user | 取消任务 |
| GET | `/user/task/my/published` | user | 我发布的任务 |

### 用户端 — 订单 (TaskOrderController)
| 方法 | 路径 | 认证 | 说明 |
|------|------|:--:|------|
| POST | `/user/order/accept` | user | 接单 |
| POST | `/user/order/cancel` | user | 取消订单 |
| PUT | `/user/order/pickup` | user | 确认取货 |
| PUT | `/user/order/deliver` | user | 确认送达 |
| PUT | `/user/order/confirm` | user | 确认完成 |
| GET | `/user/order/detail/{id}` | user | 订单详情 |
| GET | `/user/order/my/accepted` | user | 我接的订单 |

### 用户端 — 其他
| 方法 | 路径 | 认证 | 说明 |
|------|------|:--:|------|
| POST | `/user/address` | user | 添加地址 |
| GET | `/user/address/list` | user | 地址列表 |
| POST | `/user/review` | user | 创建评价 |
| POST | `/user/transaction/recharge` | user | 充值 |
| GET | `/user/runner/profile` | user | 跑腿员档案 |
| POST | `/user/runner/apply` | user | 申请跑腿员 |
| PUT | `/user/runner/online` | user | 上线 |
| PUT | `/user/runner/offline` | user | 离线 |
| GET | `/user/runner/leaderboard` | user | 排行榜 |
| GET | `/user/notification/list` | user | 通知列表 |

### 管理端 (Admin)
| 方法 | 路径 | 认证 | 说明 |
|------|------|:--:|------|
| POST | `/admin/login` | none | 管理员登录 |
| GET | `/admin/dashboard` | admin | 仪表盘 |
| GET | `/admin/users` | admin | 用户列表 |
| GET | `/admin/users/{id}` | admin | 用户详情 |
| GET | `/admin/runners` | admin | 跑腿员列表 |
| GET | `/admin/tasks` | admin | 任务列表 |
| GET | `/admin/tasks/{id}` | admin | 任务详情 |
| GET | `/admin/orders` | admin | 订单列表 |
| GET | `/admin/orders/{id}` | admin | 订单详情 |
| GET | `/admin/transactions` | admin | 流水列表 |
| POST | `/admin/notifications/broadcast` | admin | 群发通知 |
| GET | `/admin/logs` | admin | 操作日志 |
| POST | `/admin/employees` | admin | 创建管理员 (超管) |
| POST | `/admin/refresh` | none | 刷新 Token |

### 通用 (CommonController)
| 方法 | 路径 | 认证 | 说明 |
|------|------|:--:|------|
| POST | `/common/upload` | * | 文件上传 |

---

## 响应格式

### 成功
```json
{"code":1, "msg":null, "data":{...}}
```

### 分页
```json
{"code":1, "msg":null, "data":{"total":100, "records":[...]}}
```

### 业务错误
```json
{"code":0, "msg":"错误描述", "data":null}
```

### 常见错误码
| HTTP | code | 含义 |
|------|:--:|------|
| 200 | 1 | 成功 |
| 200 | 0 | 业务错误 |
| 401 | — | 未认证/Token 过期 |
| 403 | — | 权限不足 |
