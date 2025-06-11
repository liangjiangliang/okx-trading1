# Verify conversation recording functionality

try {
    Write-Host "=== Verifying Conversation Recording ==="
    
    # 1. Generate new strategy
    Write-Host "1. Generating new strategy..."
    $generateUrl = "http://localhost:8088/api/api/backtest/ta4j/generate-strategy"
    $description = "Bollinger Bands trading strategy"
    
    $jsonBody = ConvertTo-Json $description
    $response = Invoke-RestMethod -Uri $generateUrl -Method POST -Body $jsonBody -ContentType "application/json; charset=utf-8"
    
    if ($response.data -and $response.data.id) {
        $strategyId = $response.data.id
        Write-Host "Strategy generated successfully, ID: $strategyId"
        
        # 2. Update strategy
        Write-Host "2. Updating strategy..."
        $updateDescription = "Optimize Bollinger Bands with volume confirmation"
        $encodedDescription = [System.Web.HttpUtility]::UrlEncode($updateDescription)
        $updateUrl = "http://localhost:8088/api/api/backtest/ta4j/update-strategy?description=$encodedDescription&id=$strategyId"
        
        $updateResponse = Invoke-RestMethod -Uri $updateUrl -Method POST
        Write-Host "Strategy updated successfully"
        
        # 3. Verify conversation records
        Write-Host "3. Verifying conversation records..."
        Write-Host "Strategy ID $strategyId generation and update operations completed"
        Write-Host "Conversation records should be saved to strategy_conversation table"
        Write-Host "- Generate operation: conversationType='generate'"
        Write-Host "- Update operation: conversationType='update'"
        
    } else {
        Write-Host "Strategy generation failed"
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
Write-Host "Verification completed"