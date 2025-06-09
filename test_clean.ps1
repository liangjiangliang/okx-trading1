# Test description cleaning
$testDesc = '"基于RSI策略包含@#$特殊符号和多余   空格"'
Write-Host "Original description: $testDesc"

try {
    $response = Invoke-RestMethod -Uri 'http://localhost:8088/api/api/backtest/ta4j/generate-strategy' -Method POST -Body $testDesc -ContentType 'application/json; charset=utf-8'
    Write-Host "Success! Strategy generated:"
    Write-Host "  ID: $($response.data.id)"
    Write-Host "  Description: $($response.data.description)"
    Write-Host "  Category: $($response.data.category)"
} catch {
    Write-Host "Error: $($_.Exception.Message)"
}