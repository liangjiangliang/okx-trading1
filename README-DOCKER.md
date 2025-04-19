# OKX Trading Docker部署指南

本文档提供了使用Docker部署OKX Trading应用的详细说明。该配置使用本地的MySQL和Redis服务。

## 目录结构

```
.
├── Dockerfile               # Docker镜像构建文件
├── docker-compose.yml       # Docker Compose配置文件
├── .dockerignore            # Docker构建忽略文件
├── deploy.sh                # Linux/MacOS部署脚本
├── deploy.ps1               # Windows部署脚本
└── README-DOCKER.md         # 部署文档
```

## 前提条件

- 安装 [Docker](https://www.docker.com/get-started)
- 安装 [Docker Compose](https://docs.docker.com/compose/install/)
- 安装 Java 8 JDK (用于本地构建)
- 安装 Maven (或使用项目中的mvnw)
- 本地运行 MySQL 服务 (端口3306)
- 本地运行 Redis 服务 (端口6379)

## 本地服务配置要求

### MySQL
- 地址: localhost:3306
- 用户名: root
- 密码: Password123?
- 数据库: okx_trading

### Redis
- 地址: localhost:6379
- 无密码

## 配置说明

### Dockerfile

- 基于OpenJDK 8 Alpine镜像构建
- 配置了代理设置，可通过构建参数修改
- 内存配置通过环境变量`JAVA_OPTS`设置
- 时区设置为Asia/Shanghai

### docker-compose.yml

- 仅包含应用服务，使用本地的MySQL和Redis
- 端口映射：应用：8080 -> 8080
- 使用`host.docker.internal`连接宿主机上的MySQL和Redis服务
- 数据卷：logs：应用日志持久化

### 环境变量

应用服务中可配置以下环境变量：

- `SPRING_PROFILES_ACTIVE`：Spring配置文件
- `SPRING_DATASOURCE_URL`：数据库连接URL (连接到宿主机MySQL)
- `SPRING_DATASOURCE_USERNAME`：数据库用户名
- `SPRING_DATASOURCE_PASSWORD`：数据库密码
- `SPRING_REDIS_HOST`：Redis主机 (连接到宿主机Redis)
- `SPRING_REDIS_PORT`：Redis端口
- `JAVA_OPTS`：JVM参数

## 部署步骤

### Windows系统

1. 确保本地MySQL和Redis服务正在运行
2. 打开PowerShell
3. 进入项目根目录
4. 执行部署脚本：

```powershell
.\deploy.ps1
```

### Linux/MacOS系统

1. 确保本地MySQL和Redis服务正在运行
2. 打开终端
3. 进入项目根目录
4. 给部署脚本添加执行权限：

```bash
chmod +x deploy.sh
```

5. 执行部署脚本：

```bash
./deploy.sh
```

## 手动部署步骤

如果需要手动部署，可以按照以下步骤操作：

1. 确保本地MySQL和Redis服务正在运行
2. 构建应用：

```bash
./mvnw clean package -DskipTests
```

3. 构建Docker镜像并启动容器：

```bash
docker-compose up -d
```

4. 查看容器状态：

```bash
docker-compose ps
```

5. 查看应用日志：

```bash
docker-compose logs -f app
```

## 停止和清理

停止容器：

```bash
docker-compose down
```

## 常见问题排查

1. **应用无法连接数据库**
   - 确保本地MySQL服务正在运行
   - 检查本地MySQL的用户名和密码是否与docker-compose.yml中配置一致
   - 确认本地MySQL有okx_trading数据库
   - 检查本地MySQL的防火墙或访问控制设置

2. **应用无法连接Redis**
   - 确保本地Redis服务正在运行
   - 检查Redis监听设置，确保允许远程连接

3. **代理设置问题**
   - 如需修改代理设置，编辑`docker-compose.yml`中的`args`部分，然后重新构建：`docker-compose up -d --build`

4. **host.docker.internal不生效**
   - 如果遇到容器无法连接宿主机服务的问题，检查docker-compose.yml中的extra_hosts配置是否正确
   - 不同Docker版本和操作系统可能需要不同的配置来连接宿主机 