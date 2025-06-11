# Simple test for conversation recording

try {
    Write-Host "Testing generate-strategy API..."
    
    $url = "http://localhost:8088/api/api/backtest/ta4j/generate-strategy"
    $body = '"Simple MACD strategy for testing"'
    $headers = @{
        'Content-Type' = 'application/json; charset=utf-8'
    }
    
    $response = Invoke-RestMethod -Uri $url -Method POST -Body $body -Headers $headers
    
    Write-Host "Generate API Response:"
    $response | ConvertTo-Json -Depth 3
    
    if ($response.data -and $response.data.id) {
        $strategyId = $response.data.id
        Write-Host "Strategy ID: $strategyId"
        
        # Test update API
        Write-Host "Testing update-strategy API..."
        $updateUrl = "http://localhost:8088/api/api/backtest/ta4j/update-strategy?description=Updated+MACD+strategy&id=$strategyId"
        
        $updateResponse = Invoke-RestMethod -Uri $updateUrl -Method POST
        
        Write-Host "Update API Response:"
        $updateResponse | ConvertTo-Json -Depth 3
        
        Write-Host "Both APIs executed successfully!"
        Write-Host "Conversation records should be saved with:"
        Write-Host "- Generate: conversationType='generate'"
        Write-Host "- Update: conversationType='update'"
    }
    
} catch {
    Write-Host "Error: $($_.Exception.Message)"
    Write-Host "Response: $($_.Exception.Response)"
}

Write-Host "Test completed"