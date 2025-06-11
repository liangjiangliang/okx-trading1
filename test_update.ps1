# 测试更新策略接口
Write-Host "获取现有策略信息..."

$strategiesResponse = Invoke-RestMethod -Uri 'http://localhost:8088/api/api/backtest/ta4j/strategies' -Method GET
$firstStrategy = $strategiesResponse.data.PSObject.Properties | Select-Object -First 1
$strategyCode = $firstStrategy.Name
$strategyInfo = $firstStrategy.Value

Write-Host "找到策略: $strategyCode"
Write-Host "策略ID: $($strategyInfo.id)"

# 构造更新请求JSON
$updateData = @{
    id = [int]$strategyInfo.id
    strategyCode = $strategyCode
    description = "更新策略：基于RSI的改进版本，增加MACD确认信号"
    strategyName = "改进的RSI策略"
    category = "技术指标策略"
}

$json = $updateData | ConvertTo-Json -Depth 10
Write-Host "发送更新请求..."
Write-Host "请求体: $json"

try {
    $response = Invoke-RestMethod -Uri 'http://localhost:8088/api/api/backtest/ta4j/update-strategy' -Method PUT -Body $json -ContentType 'application/json; charset=utf-8'
    Write-Host "更新成功:"
    $response | ConvertTo-Json -Depth 10
} catch {
    Write-Host "更新失败:"
    Write-Host $_.Exception.Message
    if ($_.Exception.Response) {
        $stream = $_.Exception.Response.GetResponseStream()
        $reader = New-Object System.IO.StreamReader($stream)
        $responseBody = $reader.ReadToEnd()
        $reader.Close()
        $stream.Close()
        Write-Host "响应内容: $responseBody"
    }
}