# 测试更新策略接口和对话记录功能

try {
    # 测试更新策略接口
    $updateUrl = "http://localhost:8088/api/api/backtest/ta4j/update-strategy?description=优化MACD策略，增加RSI过滤条件&id=1"
    Write-Host "正在调用更新策略接口..."
    $response = Invoke-RestMethod -Uri $updateUrl -Method PUT
    
    Write-Host "更新策略接口调用成功:"
    $response | ConvertTo-Json -Depth 3
    
} catch {
    Write-Host "更新策略接口调用失败: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "错误详情: $responseBody"
    }
}

Write-Host "测试完成"