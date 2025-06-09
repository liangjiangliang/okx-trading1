# 测试中文编码的PowerShell脚本
$headers = @{
    'Content-Type' = 'application/json; charset=utf-8'
}

$body = '{"strategyCode":"TEST_CHINESE_006","strategyName":"测试中文策略6","description":"这是一个包含中文字符的策略描述，用于验证编码修复效果","category":"测试分类","paramsDesc":"参数说明：短期周期,长期周期","defaultParams":"5,20"}'

Write-Host "发送请求..."
Invoke-RestMethod -Uri "http://localhost:8088/api/api/backtest/ta4j/generate-strategy" -Method POST -Headers $headers -Body $body