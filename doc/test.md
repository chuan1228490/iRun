# 测试环境部署指南

> 通用模板，替换 `<xxx>` 占位符后即可用于新环境。

## 前置要求

- 一台虚拟机或服务器，或Docker容器，本文档以Ubuntu为例。
- Java 21、MySQL 8、Redis
- 内网穿透工具（ngrok / frp 等）用于公网访问

## 1. 环境准备

```bash
# Java 21
sudo apt update && sudo apt install -y openjdk-21-jdk

# MySQL 8
sudo apt install -y mysql-server
sudo mysql_secure_installation
mysql -u <db-user> -p -e "CREATE DATABASE IF NOT EXISTS runningerrands DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;"

# Redis
sudo apt install -y redis-server
sudo systemctl enable --now redis-server
```

导入表结构：
```bash
mysql -u <db-user> -p runningerrands < runningerrands.sql
```

## 2. 打包

在开发机：

```bash
cd backend
./runningerrands-server/mvnw clean package -DskipTests
```

产物：`backend/runningerrands-server/target/runningerrands-server-1.0-SNAPSHOT.jar`

管理端静态资源已嵌入 JAR，无需单独部署前端。

## 3. 上传

```bash
scp backend/runningerrands-server/target/runningerrands-server-1.0-SNAPSHOT.jar <user>@<server-ip>:~/project/
scp backend/runningerrands.sql <user>@<server-ip>:~/project/
```

## 4. 环境变量

在服务器 `~/project/env.sh`：

```bash
#!/bin/bash
# 数据库
export RUNNING_ERRANDS_MYSQL_URL=jdbc:mysql://localhost:3306/runningerrands?useUnicode=true&characterEncoding=UTF-8&serverTimezone=GMT%2B8
export RUNNING_ERRANDS_MYSQL_USERNAME=<db-username>
export RUNNING_ERRANDS_MYSQL_PASSWORD=<db-password>

# Redis
export RUNNING_ERRANDS_REDIS_HOST=localhost
export RUNNING_ERRANDS_REDIS_PORT=6379
export RUNNING_ERRANDS_REDIS_DATABASE=1

# 超管初始化（首次启动自动创建）
export RUNNING_ERRANDS_ADMIN_INIT_USERNAME=<admin-username>
export RUNNING_ERRANDS_ADMIN_INIT_PASSWORD=<admin-password>
export RUNNING_ERRANDS_ADMIN_INIT_NAME=<admin-display-name>

# JWT 密钥
export RUNNING_ERRANDS_JWT_ADMIN_SECRET=<random-string>
export RUNNING_ERRANDS_JWT_USER_SECRET=<random-string>

# 阿里云 OSS
export ALIYUN_ACCESS_KEY_ID=<aliyun-key>
export ALIYUN_ACCESS_KEY_SECRET=<aliyun-secret>

# 微信小程序
export WECHAT_APP_ID=<wechat-app-id>
export WECHAT_APP_SECRET=<wechat-app-secret>

# 腾讯地图
export TENCENT_MAP_API_KEY=<map-api-key>
```

## 5. 启动

```bash
ssh <user>@<server-ip>
cd ~/project && source env.sh
nohup java -Dspring.profiles.active=test -jar runningerrands-server-1.0-SNAPSHOT.jar > app.log 2>&1 &

# 启动内网穿透
nohup ngrok http 8080 > /dev/null 2>&1 &
```

## 6. 验证

```bash
# 后端启动确认
tail -f ~/project/app.log
# 看到 "Started RunningerrandsServerApplication" 即成功

# 本地检查
curl http://localhost:8080/api/v3/api-docs

# 公网检查
curl https://<ngrok-domain>.ngrok-free.dev/api/v3/api-docs
```

管理后台：`https://<ngrok-domain>.ngrok-free.dev/api/`
Swagger 文档：`https://<ngrok-domain>.ngrok-free.dev/api/swagger-ui.html`

## 7. 停止

```bash
kill $(pgrep -f runningerrands-server)
kill $(pgrep ngrok)
```

## 8. 更新部署

```bash
# 开发机：打包 & 上传
cd backend
./runningerrands-server/mvnw clean package -DskipTests
scp runningerrands-server/target/runningerrands-server-1.0-SNAPSHOT.jar <user>@<server-ip>:~/project/

# 服务器：重启
ssh <user>@<server-ip>
kill $(pgrep -f runningerrands-server)
cd ~/project && source env.sh
nohup java -Dspring.profiles.active=test -jar runningerrands-server-1.0-SNAPSHOT.jar > app.log 2>&1 &
```
