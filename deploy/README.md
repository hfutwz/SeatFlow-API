# SeatFlow Docker 部署指南

## 目录结构

```
deploy/
├── backend/           # 后端 Dockerfile
├── frontend/          # 前端 Dockerfile + nginx.conf
├── mysql/             # MySQL 初始化脚本
│   └── init.sql
├── docker-compose.yml
├── .env.example
└── README.md
```

## 快速部署

### 1. 准备环境

确保已安装：
- Docker >= 20.10
- Docker Compose >= 2.0

### 2. 配置环境变量（可选）

```bash
cp .env.example .env
# 编辑 .env 自定义端口和密码
```

### 3. 启动服务

```bash
cd deploy
docker-compose up -d
```

等待约 1-2 分钟（MySQL 首次启动需初始化数据库）。

### 4. 验证服务

- 前端：http://localhost:${FRONTEND_PORT:-80}
- 后端 API：http://localhost:${BACKEND_PORT:-8081}/api
- 健康检查：http://localhost:${BACKEND_PORT:-8081}/api/auth/me

### 5. 停止服务

```bash
cd deploy
docker-compose down
```

### 6. 清理数据（重置数据库）

```bash
docker-compose down -v
```

## 端口说明

| 服务 | 主机端口 | 容器端口 | 说明 |
|---|---|---|---|
| MySQL | 3307 | 3306 | 可通过 MySQL 客户端连接 |
| 后端 | 8081 | 8080 | Spring Boot API |
| 前端 | 80 | 80 | Nginx 反向代理 |

## 数据持久化

MySQL 数据存储在 Docker volume 中，删除容器不会丢失数据。

## 常见问题

**Q: MySQL 启动失败？**
A: 检查端口是否被占用：`lsof -i :3307`

**Q: 后端连接数据库失败？**
A: 等待 MySQL 完全启动（约 30 秒），确保 `service_healthy` 状态后再访问。

**Q: 前端静态资源加载失败？**
A: 确认 `frontend/nginx.conf` 中的 `proxy_pass` 地址正确指向后端容器名 `backend:8080`。

## 生产环境注意事项

- 修改 `.env` 中的 `DB_ROOT_PASSWORD` 为强密码
- 考虑使用外部数据库而非 Docker Volume
- 前端 Nginx 配置可根据需要添加 SSL/TLS
- 后端可配置日志收集和监控
