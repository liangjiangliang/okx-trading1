# Test generate and update strategy APIs

try {
    Write-Host "=== Testing Generate Strategy API ==="
    $generateUrl = "http://localhost:8088/api/api/backtest/ta4j/generate-strategy"
    $description = "MACD trading strategy"
    
    Write-Host "Generating strategy..."
    Write-Host "Description: $description"
    
    $jsonBody = ConvertTo-Json $description
    $response = Invoke-RestMethod -Uri $generateUrl -Method POST -Body $jsonBody -ContentType "application/json; charset=utf-8"
    
    Write-Host "Strategy generated successfully!"
    Write-Host "Response:"
    $response | ConvertTo-Json -Depth 5
    
    if ($response.data -and $response.data.id) {
        $strategyId = $response.data.id
        Write-Host ""
        Write-Host "=== Generated Strategy Info ==="
        Write-Host "Strategy ID: $strategyId"
        Write-Host "Strategy Code: $($response.data.strategyCode)"
        Write-Host "Strategy Name: $($response.data.strategyName)"
        
        # Test update strategy
        Write-Host ""
        Write-Host "=== Testing Update Strategy API ==="
        $updateDescription = "Optimize MACD strategy with RSI filter"
        $updateUrl = "http://localhost:8088/api/api/backtest/ta4j/update-strategy?description=$updateDescription&id=$strategyId"
        
        Write-Host "Updating strategy..."
        Write-Host "Update description: $updateDescription"
        
        $updateResponse = Invoke-RestMethod -Uri $updateUrl -Method POST
        
        Write-Host "Strategy updated successfully!"
        Write-Host "Update response:"
        $updateResponse | ConvertTo-Json -Depth 5
    }
    
} catch {
    Write-Host "Operation failed: $($_.Exception.Message)"
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Error details: $responseBody"
    }
}

Write-Host ""
Write-Host "Test completed"