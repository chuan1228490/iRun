# Nginx 部署配置

小i跑腿管理端 (admin) 生产环境 nginx 反向代理配置。

## 架构

```
浏览器
  │
  ▼
nginx (:80/:443)
  ├── /api/assets/**  → 静态文件 (nginx 直接服务)
  ├── /api/admin/**   → proxy_pass → Spring Boot :8080
  ├── /api/user/**    → proxy_pass → Spring Boot :8080
  ├── /api/common/**  → proxy_pass → Spring Boot :8080
  ├── /api/ws/**      → proxy_pass → Spring Boot :8080 (WebSocket)
  └── /api/**         → SPA 兜底 → index.html
```

## 配置模板

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 静态资源（长期缓存）
    location /api/assets/ {
        alias /opt/runningerrands/admin/dist/assets/;
        expires 1y;
        add_header Cache-Control "public, immutable";
    }

    location /api/logo.svg {
        alias /opt/runningerrands/admin/dist/logo.svg;
    }

    # 后端管理 API
    location /api/admin/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 后端用户端 API
    location /api/user/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # 后端通用 API（上传等）
    location /api/common/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # WebSocket (STOMP)
    location /api/ws/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # SPA 兜底 — 前端路由由 Vue Router 处理
    location /api/ {
        alias /opt/runningerrands/admin/dist/;
        try_files $uri /api/index.html;
    }
}
```

## HTTPS (可选)

```nginx
server {
    listen 443 ssl http2;
    server_name your-domain.com;

    ssl_certificate     /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;

    # ... 同上 location 规则 ...
}

server {
    listen 80;
    server_name your-domain.com;
    return 301 https://$host$request_uri;
}
```

## 部署流程

```bash
# 1. 构建前端
cd admin && npx vite build

# 2. 部署到服务器
rsync -avz dist/ user@server:/opt/runningerrands/admin/dist/

# 3. 重载 nginx
ssh user@server "sudo nginx -t && sudo nginx -s reload"
```

## 注意事项

- `location` 顺序敏感：精确匹配 `/api/assets/` 需在 SPA 兜底 `/api/` 之前
- WebSocket 需要 `Upgrade` 和 `Connection` 头
- 如果后端 context-path 不是 `/api`，需同步修改 `vite.config.ts` 的 `base` 和所有 `location` 前缀
