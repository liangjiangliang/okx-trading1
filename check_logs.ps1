# 查看最新的错误日志

$errorLogDir = "c:\Users\ralph\IdeaProject\okx-trading\logs\error"
$allLogDir = "c:\Users\ralph\IdeaProject\okx-trading\logs\all"

Write-Host "=== 最新错误日志 ==="
if (Test-Path $errorLogDir) {
    $latestErrorLog = Get-ChildItem $errorLogDir -Filter "*.log" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($latestErrorLog) {
        Write-Host "错误日志文件: $($latestErrorLog.Name)"
        Get-Content $latestErrorLog.FullName | Select-Object -Last 30
    } else {
        Write-Host "没有找到错误日志文件"
    }
} else {
    Write-Host "错误日志目录不存在"
}

Write-Host ""
Write-Host "=== 最新应用日志 ==="
if (Test-Path $allLogDir) {
    $latestAllLog = Get-ChildItem $allLogDir -Filter "*.log" | Sort-Object LastWriteTime -Descending | Select-Object -First 1
    if ($latestAllLog) {
        Write-Host "应用日志文件: $($latestAllLog.Name)"
        Get-Content $latestAllLog.FullName | Select-Object -Last 30
    } else {
        Write-Host "没有找到应用日志文件"
    }
} else {
    Write-Host "应用日志目录不存在"
}