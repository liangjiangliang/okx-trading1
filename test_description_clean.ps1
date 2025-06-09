# Test description cleaning functionality

# Test 1: Normal description
Write-Host "=== Test 1: Normal Description ==="
$description1 = "基于双均线策略，使用短期和长期移动平均线交叉信号"
$response1 = Invoke-RestMethod -Uri 'http://localhost:8088/api/api/backtest/ta4j/generate-strategy' -Method POST -Body (ConvertTo-Json $description1) -ContentType 'application/json; charset=utf-8'
Write-Host "Original: $description1"
Write-Host "Cleaned: $($response1.data.description)"
Write-Host ""

# Test 2: Description with quotes and backslashes
Write-Host "=== Test 2: Description with Special Characters ==="
$description2 = '" 基于RSI超买超卖策略，包含\"特殊符号\"和多余    空格"'
$response2 = Invoke-RestMethod -Uri 'http://localhost:8088/api/api/backtest/ta4j/generate-strategy' -Method POST -Body $description2 -ContentType 'application/json; charset=utf-8'
Write-Host "Original: $description2"
Write-Host "Cleaned: $($response2.data.description)"
Write-Host ""

# Test 3: Description with English and special symbols
Write-Host "=== Test 3: Mixed Content ==="
$description3 = '"基于MACD策略@#$%^&*()包含English文字和特殊符号"'
$response3 = Invoke-RestMethod -Uri 'http://localhost:8088/api/api/backtest/ta4j/generate-strategy' -Method POST -Body $description3 -ContentType 'application/json; charset=utf-8'
Write-Host "Original: $description3"
Write-Host "Cleaned: $($response3.data.description)"