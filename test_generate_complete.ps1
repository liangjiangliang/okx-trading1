# 测试生成策略接口的完整功能

try {
    Write-Host "=== 测试生成策略接口 ==="
    $generateUrl = "http://localhost:8088/api/api/backtest/ta4j/generate-strategy"
    $description = "基于MACD指标的交易策略，当MACD线上穿信号线时买入，下穿时卖出"
    
    Write-Host "正在生成策略..."
    Write-Host "描述: $description"
    
    $jsonBody = ConvertTo-Json $description
    $response = Invoke-RestMethod -Uri $generateUrl -Method POST -Body $jsonBody -ContentType "application/json; charset=utf-8"
    
    Write-Host "策略生成成功!"
    Write-Host "返回结果:"
    $response | ConvertTo-Json -Depth 5
    
    if ($response.data -and $response.data.id) {
        $strategyId = $response.data.id
        Write-Host ""
        Write-Host "=== 生成的策略信息 ==="
        Write-Host "策略ID: $strategyId"
        Write-Host "策略代码: $($response.data.strategyCode)"
        Write-Host "策略名称: $($response.data.strategyName)"
        Write-Host "策略描述: $($response.data.description)"
        Write-Host "策略分类: $($response.data.category)"
        
        # 测试更新策略
        Write-Host ""
        Write-Host "=== 测试更新策略接口 ==="
        $updateDescription = "优化MACD策略，增加RSI过滤条件"
        $updateUrl = "http://localhost:8088/api/api/backtest/ta4j/update-strategy?description=$updateDescription&id=$strategyId"
        
        Write-Host "正在更新策略..."
        Write-Host "更新描述: $updateDescription"
        
        $updateResponse = Invoke-RestMethod -Uri $updateUrl -Method PUT
        
        Write-Host "策略更新成功!"
        Write-Host "更新结果:"
        $updateResponse | ConvertTo-Json -Depth 5
    }
    
} catch {
    Write-Host "操作失败: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "错误详情: $responseBody"
    }
}

Write-Host ""
Write-Host "测试完成"