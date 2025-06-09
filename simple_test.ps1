$description = '"基于RSI策略包含@#$特殊符号和多余   空格"'
$response = Invoke-RestMethod -Uri 'http://localhost:8088/api/api/backtest/ta4j/generate-strategy' -Method POST -Body $description -ContentType 'application/json; charset=utf-8'
Write-Host "Original: $description"
Write-Host "Cleaned: $($response.data.description)"
Write-Host "Strategy ID: $($response.data.id)"
Write-Host "Category: $($response.data.category)"