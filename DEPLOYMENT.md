# Insta-Graph 快速部署指南

## 前置要求

- Docker 20.10+
- Docker Compose 2.0+

## 快速开始（推荐）

### 1. 配置 API Key

```bash
# 复制环境变量模板
cp .env.example .env

# 编辑 .env 文件，填入你的 RapidAPI key
nano .env  # 或使用任何编辑器
```

`.env` 文件内容：
```
TIKHUB_API_KEY=你的_rapidapi_key
```

### 2. 一键启动

```bash
# 构建并启动所有服务（首次部署）
docker-compose up -d --build

# 查看启动日志
docker-compose logs -f
```

### 3. 验证部署

等待约 60-90 秒让服务完全启动，然后访问：

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **Neo4j Browser**: http://localhost:7474 (用户名: neo4j, 密码: password)

检查健康状态：
```bash
# 检查所有服务状态
docker-compose ps

# 应用健康检查
curl http://localhost:8080/actuator/health
```

## 常用操作

### 查看日志
```bash
# 查看所有服务日志
docker-compose logs -f

# 仅查看应用日志
docker-compose logs -f insta-graph-app

# 仅查看 Neo4j 日志
docker-compose logs -f neo4j
```

### 停止服务
```bash
# 停止所有服务
docker-compose down

# 停止并删除数据卷（清空数据库）
docker-compose down -v
```

### 重启服务
```bash
# 重启所有服务
docker-compose restart

# 仅重启应用（不重启数据库）
docker-compose restart insta-graph-app
```

### 更新代码后重新部署
```bash
# 重新构建并启动
docker-compose up -d --build
```

## 配置说明

### 内存配置

默认配置：
- Neo4j: 512MB-2GB heap
- Spring Boot: 256MB-1GB heap

如需调整，编辑 `docker-compose.yml`：

```yaml
# Neo4j 内存
- NEO4J_dbms_memory_heap_initial__size=512m
- NEO4J_dbms_memory_heap_max__size=2G

# Spring Boot JVM 选项
- JAVA_OPTS=-Xms256m -Xmx1g -XX:+UseG1GC
```

### 端口配置

默认端口映射：
- `8080`: Spring Boot API
- `7474`: Neo4j HTTP (浏览器界面)
- `7687`: Neo4j Bolt (应用连接)

如需更改，编辑 `docker-compose.yml` 的 ports 配置。

### 数据持久化

数据存储在 Docker volumes 中：
- `neo4j_data`: 数据库数据
- `neo4j_logs`: 数据库日志
- `./logs`: 应用日志（宿主机目录）

查看 volumes：
```bash
docker volume ls | grep insta-graph
```

备份数据：
```bash
docker run --rm -v insta-graph_neo4j_data:/data -v $(pwd):/backup ubuntu tar czf /backup/neo4j_backup.tar.gz /data
```

## 故障排查

### 应用无法连接到 Neo4j

**症状**: 日志显示连接错误
```
Unable to connect to localhost:7687
```

**解决方案**:
1. 确认 Neo4j 已完全启动：
   ```bash
   docker-compose logs neo4j | grep "Started"
   ```
2. 检查健康状态：
   ```bash
   docker-compose ps
   ```
3. Neo4j 应显示 `healthy` 状态

### 构建失败

**症状**: `docker-compose up --build` 失败

**解决方案**:
1. 清理旧镜像和容器：
   ```bash
   docker-compose down
   docker system prune -f
   ```
2. 重新构建：
   ```bash
   docker-compose build --no-cache
   docker-compose up -d
   ```

### API Key 未生效

**症状**: Tikhub API 调用返回 401 错误

**解决方案**:
1. 确认 `.env` 文件存在且格式正确
2. 重启服务以加载新的环境变量：
   ```bash
   docker-compose down
   docker-compose up -d
   ```
3. 检查环境变量是否正确注入：
   ```bash
   docker-compose exec insta-graph-app env | grep TIKHUB
   ```

### 内存不足

**症状**: 容器频繁重启或 OOM 错误

**解决方案**:
1. 增加 Docker Desktop 内存限制（推荐至少 4GB）
2. 减少 docker-compose.yml 中的内存配置
3. 监控资源使用：
   ```bash
   docker stats
   ```

## 生产环境建议

### 安全配置

1. **修改默认密码**:
   编辑 `docker-compose.yml`，修改 Neo4j 密码：
   ```yaml
   - NEO4J_AUTH=neo4j/你的强密码
   ```

2. **使用 secrets 管理敏感信息**:
   参考 Docker Compose secrets 文档

3. **限制端口暴露**:
   仅暴露必要端口，可以移除 Neo4j 7474 端口映射

### 性能优化

1. **启用生产配置**:
   ```yaml
   environment:
     - SPRING_PROFILES_ACTIVE=prod
     - JAVA_OPTS=-Xms1g -Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200
   ```

2. **调整 Neo4j 性能**:
   根据服务器规格调整内存和并发配置

3. **添加日志轮转**:
   配置日志大小限制和轮转策略

### 监控

添加监控服务（可选）:
- Prometheus + Grafana
- Spring Boot Admin
- Neo4j monitoring

## 开发模式 vs 生产模式

| 配置项 | 开发模式 | 生产模式 |
|--------|---------|---------|
| 数据持久化 | Volumes | 外部存储 |
| 密码 | 默认密码 | 强密码 |
| 端口暴露 | 全部暴露 | 最小化 |
| 日志级别 | DEBUG | INFO/WARN |
| 资源限制 | 宽松 | 严格控制 |
| 重启策略 | unless-stopped | always |

## 更多信息

- **项目文档**: 查看 `操作说明.md`
- **API 文档**: http://localhost:8080/swagger-ui.html
- **Neo4j 文档**: https://neo4j.com/docs/
- **Docker Compose 参考**: https://docs.docker.com/compose/
