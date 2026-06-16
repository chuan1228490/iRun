# 测试场景清单

## 1. 认证模块 (Auth)

### 1.1 密码登录
```bash
# 正常登录
curl -s -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"loginType":1,"username":"testuser","password":"123456"}'
# 期望: code=1, 返回 token+refreshToken

# 错误密码
curl -s -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"loginType":1,"username":"testuser","password":"wrong"}'
# 期望: code=0, msg="用户名或密码错误"

# 不存在用户
curl -s -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"loginType":1,"username":"nouser","password":"123456"}'
# 期望: code=0
```

### 1.2 验证码登录
```bash
# 注入验证码
docker exec rr-redis redis-cli -n 1 SET "user:code:login:13800000001" "888888" EX 300

# 验证码登录
curl -s -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"loginType":2,"phone":"13800000001","code":"888888"}'
# 期望: code=1

# 错误验证码
curl -s -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"loginType":2,"phone":"13800000001","code":"000000"}'
# 期望: code=0, msg="验证码错误或已过期"
```

### 1.3 暴力破解防护
```bash
# 连续 5 次错误密码
for i in 1 2 3 4 5; do
  curl -s -X POST http://localhost:8080/api/user/login \
    -H "Content-Type: application/json" \
    -d '{"loginType":1,"username":"testuser","password":"wrong"}'
done
# 第 6 次: msg 包含 "5分钟后再试"
```

### 1.4 管理员登录
```bash
curl -s -X POST http://localhost:8080/api/admin/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
# 期望: code=1
```

---

## 2. 用户模块 (User)

### 2.1 获取用户信息
```bash
curl -s http://localhost:8080/api/user/info \
  -H "authentication: $TOKEN"
# 期望: code=1, 含 nickname/phone/balance
```

### 2.2 修改密码
```bash
curl -s -X PUT http://localhost:8080/api/user/password \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN" \
  -d '{"oldPassword":"123456","newPassword":"654321"}'
# 期望: code=1, msg="密码修改成功"

# 还原密码
curl -s -X PUT http://localhost:8080/api/user/password \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN" \
  -d '{"oldPassword":"654321","newPassword":"123456"}'
```

### 2.3 支付密码 — 首次设置（testuser2 无支付密码）
```bash
TOKEN2=$(curl -s -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"loginType":1,"username":"testuser2","password":"123456"}' \
  | sed 's/.*"token":"\([^"]*\)".*/\1/')

# 注意: testuser2 已有支付密码(DockerTestDataInitializer 预设)，此场景用于无支付密码用户
curl -s -X PUT http://localhost:8080/api/user/pay-password \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN2" \
  -d '{"payPassword":"111111"}'
# 期望: code=0 (已设置过)
```

### 2.4 支付密码 — 修改
```bash
curl -s -X PUT http://localhost:8080/api/user/pay-password/change \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN" \
  -d '{"oldPayPassword":"123456","newPayPassword":"111111"}'
# 期望: code=1

# 还原
curl -s -X PUT http://localhost:8080/api/user/pay-password/change \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN" \
  -d '{"oldPayPassword":"111111","newPayPassword":"123456"}'
```

---

## 3. SMS 验证码安全 (S1 隔离验证)

### 3.1 跨操作拒绝 — register 码不能用于 login
```bash
docker exec rr-redis redis-cli -n 1 SET "user:code:register:13800000001" "999999" EX 300

curl -s -X POST http://localhost:8080/api/user/login \
  -H "Content-Type: application/json" \
  -d '{"loginType":2,"phone":"13800000001","code":"999999"}'
# 期望: code=0, msg="验证码错误或已过期"
```

### 3.2 跨操作拒绝 — login 码不能用于 reset_password
```bash
docker exec rr-redis redis-cli -n 1 SET "user:code:login:13800000001" "777777" EX 300

curl -s -X PUT http://localhost:8080/api/user/password/reset \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN" \
  -d '{"phone":"13800000001","code":"777777","newPassword":"123456"}'
# 期望: code=0
```

### 3.3 正常重置密码流程
```bash
docker exec rr-redis redis-cli -n 1 SET "user:code:reset_password:13800000001" "666666" EX 300

curl -s -X PUT http://localhost:8080/api/user/password/reset \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN" \
  -d '{"phone":"13800000001","code":"666666","newPassword":"123456"}'
# 期望: code=1, msg="登录密码重置成功"
```

### 3.4 手机号不匹配
```bash
curl -s -X PUT http://localhost:8080/api/user/password/reset \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN" \
  -d '{"phone":"13900000000","code":"666666","newPassword":"123456"}'
# 期望: code=0, msg="手机号与当前账户不匹配"
```

---

## 4. 密码重置暴力破解 (S2 锁定验证)

### 4.1 5 次失败后锁定
```bash
for i in 1 2 3 4 5; do
  curl -s -X PUT http://localhost:8080/api/user/password/reset \
    -H "Content-Type: application/json" \
    -H "authentication: $TOKEN" \
    -d '{"phone":"13800000001","code":"000000","newPassword":"123456"}'
done

# 第 6 次
curl -s -X PUT http://localhost:8080/api/user/password/reset \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN" \
  -d '{"phone":"13800000001","code":"000000","newPassword":"123456"}'
# 期望: msg="密码重置尝试次数过多，请5分钟后再试"
```

### 4.2 支付密码重置同样锁定
```bash
for i in 1 2 3 4 5; do
  curl -s -X PUT http://localhost:8080/api/user/pay-password/reset \
    -H "Content-Type: application/json" \
    -H "authentication: $TOKEN" \
    -d '{"phone":"13800000001","code":"000000","newPassword":"111111"}'
done

# 第 6 次期望锁定
```

---

## 5. 鉴权与越权 (Auth Bypass)

### 5.1 无 Token 访问受保护接口
```bash
curl -s http://localhost:8080/api/user/info
# 期望: code=0 (401 系列)
```

### 5.2 用户 Token 访问管理端接口
```bash
curl -s http://localhost:8080/api/admin/dashboard \
  -H "token: $TOKEN"
# 期望: code=0 (401/403)
```

### 5.3 管理端 Token 访问用户接口
```bash
curl -s http://localhost:8080/api/user/info \
  -H "authentication: $ADMIN_TOKEN"
# 期望: code=0
```

---

## 6. 任务模块 (Task)

### 6.1 发布任务
```bash
curl -s -X POST http://localhost:8080/api/user/task \
  -H "Content-Type: application/json" \
  -H "authentication: $TOKEN" \
  -d '{
    "type":"EXPRESS","subType":"PICKUP",
    "publicDesc":"测试快递任务",
    "taskSpecs":"{\"包裹列表\":[{\"规格\":\"小件\",\"数量\":1}]}",
    "tip":2,"deliveryFee":3,"productCost":0,
    "pickupAddress":"菜鸟驿站","deliveryAddressId":1,
    "payPassword":"123456"
  }'
# 期望: code=1
```

### 6.2 任务大厅列表
```bash
curl -s http://localhost:8080/api/user/task/hall?page=1\&size=10 \
  -H "authentication: $TOKEN"
# 期望: code=1, 含 records
```

---

## 7. 健康检查
```bash
curl -s http://localhost:8080/api/actuator/health
# 期望: {"status":"UP"}
```
