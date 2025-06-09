# Test Chinese strategy generation
$description = "基于布林带均值回归策略，使用20日移动平均线和2倍标准差构建上下轨，当价格触及下轨时买入，触及上轨时卖出"

# Send request
$response = Invoke-RestMethod -Uri 'http://localhost:8088/api/api/backtest/ta4j/generate-strategy' -Method POST -Body (ConvertTo-Json $description) -ContentType 'application/json; charset=utf-8'

# Display results
Write-Host "=== 中文策略生成结果 ==="
Write-Host "状态码: $($response.code)"
Write-Host "消息: $($response.message)"
Write-Host ""
Write-Host "=== 策略信息 ==="
Write-Host "策略代码: $($response.data.strategyCode)"
Write-Host "策略名称: $($response.data.strategyName)"
Write-Host "策略分类: $($response.data.category)"
Write-Host "策略描述: $($response.data.description)"
Write-Host "默认参数: $($response.data.defaultParams)"
Write-Host "参数描述: $($response.data.paramsDesc)"
Write-Host "创建时间: $($response.data.createTime)"
Write-Host "策略ID: $($response.data.id)"