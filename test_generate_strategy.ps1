# Test strategy generation API
$description = "Based on MACD trend following strategy, using 12-day and 26-day EMA to calculate MACD indicator, buy when MACD line crosses above signal line, sell when crosses below"

# Send request
$response = Invoke-RestMethod -Uri 'http://localhost:8088/api/api/backtest/ta4j/generate-strategy' -Method POST -Body (ConvertTo-Json $description) -ContentType 'application/json; charset=utf-8'

# Display results
Write-Host "=== Strategy Generation Result ==="
Write-Host "Status Code: $($response.code)"
Write-Host "Message: $($response.message)"
Write-Host ""
Write-Host "=== Strategy Information ==="
Write-Host "Strategy Code: $($response.data.strategyCode)"
Write-Host "Strategy Name: $($response.data.strategyName)"
Write-Host "Category: $($response.data.category)"
Write-Host "Description: $($response.data.description)"
Write-Host "Default Params: $($response.data.defaultParams)"
Write-Host "Params Description: $($response.data.paramsDesc)"
Write-Host "Create Time: $($response.data.createTime)"
Write-Host "Strategy ID: $($response.data.id)"