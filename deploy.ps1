# PowerShell部署脚本，适用于Windows环境

# 检查MySQL和Redis服务是否运行
Write-Host "检查本地MySQL服务是否运行..." -ForegroundColor Cyan
$mysqlRunning = Test-NetConnection -ComputerName localhost -Port 3306 -InformationLevel Quiet
if ($mysqlRunning) {
    Write-Host "MySQL服务正在运行" -ForegroundColor Green
} else {
    Write-Host "警告: 本地MySQL服务未运行。请先启动MySQL服务后再部署应用" -ForegroundColor Red
    Write-Host "可以通过以下步骤启动MySQL服务:" -ForegroundColor Yellow
    Write-Host "1. 打开服务管理器 (services.msc)" -ForegroundColor Yellow
    Write-Host "2. 找到MySQL服务并启动" -ForegroundColor Yellow
    exit 1
}

Write-Host "检查本地Redis服务是否运行..." -ForegroundColor Cyan
$redisRunning = Test-NetConnection -ComputerName localhost -Port 6379 -InformationLevel Quiet
if ($redisRunning) {
    Write-Host "Redis服务正在运行" -ForegroundColor Green
} else {
    Write-Host "警告: 本地Redis服务未运行。请先启动Redis服务后再部署应用" -ForegroundColor Red
    Write-Host "可以通过以下步骤启动Redis服务:" -ForegroundColor Yellow
    Write-Host "1. 打开服务管理器 (services.msc)" -ForegroundColor Yellow
    Write-Host "2. 找到Redis服务并启动" -ForegroundColor Yellow
    exit 1
}

# 构建应用
Write-Host "正在构建应用..." -ForegroundColor Cyan
if (Test-Path -Path ".\mvnw.cmd") {
    .\mvnw.cmd clean package -DskipTests
} else {
    mvn clean package -DskipTests
}

# 停止并删除旧容器
Write-Host "正在停止和移除旧容器..." -ForegroundColor Cyan
docker-compose down

# 启动新容器
Write-Host "正在启动新容器..." -ForegroundColor Cyan
docker-compose up -d

# 查看容器状态
Write-Host "查看容器状态..." -ForegroundColor Cyan
docker-compose ps

# 查看日志
Write-Host "查看应用程序日志..." -ForegroundColor Cyan
docker-compose logs -f app 