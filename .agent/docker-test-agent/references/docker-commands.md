# Docker 运维命令速查

## 生命周期

```bash
# 启动（docker/ 目录）
cd F:/ikeu_runningerrands/docker
docker compose up -d

# 重新构建并启动（代码修改后）
docker compose up -d --build backend

# 停止
docker compose down

# 停止并清除数据卷
docker compose down -v

# 查看状态
docker compose ps
```

## 日志

```bash
# 后端日志（实时）
docker logs -f rr-backend

# 最近 100 行
docker logs rr-backend --tail 100

# MySQL 日志
docker logs rr-mysql --tail 50

# Redis 日志
docker logs rr-redis --tail 20
```

## 数据库

```bash
# 进入 MySQL
docker exec -it rr-mysql mysql -uroot -proot123 runningerrands

# 执行 SQL 查询
docker exec rr-mysql mysql -uroot -proot123 runningerrands -e "SELECT id,username,phone FROM user"

# 进入 Redis
docker exec -it rr-redis redis-cli -n 1

# Redis 常用操作
docker exec rr-redis redis-cli -n 1 KEYS "user:*"        # 查看所有 user key
docker exec rr-redis redis-cli -n 1 GET "user:code:login:13800000001"  # 查看验证码
docker exec rr-redis redis-cli -n 1 TTL "user:code:login:13800000001"  # 查看剩余时间
docker exec rr-redis redis-cli -n 1 FLUSHDB              # 清空当前 db
```

## 后端应用

```bash
# 重启后端
docker compose restart backend

# 进入后端容器
docker exec -it rr-backend sh

# 查看 JVM 内存
docker stats rr-backend
```

## 网络

```bash
# 查看容器 IP
docker inspect -f '{{.Name}} {{.NetworkSettings.Networks.rr_net.IPAddress}}' rr-mysql rr-redis rr-backend

# 从后端容器内测试 MySQL 连通性
docker exec rr-backend sh -c "wget -qO- http://mysql:3306 2>&1 || echo 'MySQL reachable'"
```
