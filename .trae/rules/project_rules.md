1. 接口调用路径 Invoke-RestMethod -Uri http://localhost:8088/api/api/backtest/ta4j/delete-strategy/AI_SMA_009 -Method DELETE 
2. 程序已经启动，并且使用rebel自动编译，不需要频繁重启
3. 程序启动后不要在原终端上执行命令，会kill掉程序
4. powershell接口调用方式 powershell -Command Invoke-RestMethod -Uri 'http://localhost:8088/api/api/backtest/ta4j/generate-strategy' -Method POST -Body '"基于双均线RSI组合的交易策略，使用9日和26日移动平均线交叉信号，结合RSI指标过滤信号"' -ContentType 'application/json; charset=utf-8'
5. 正确的powershell调用方式 powershell -Command "Invoke-RestMethod -Uri 'http://localhost:8088/api/api/backtest/ta4j/generate-strategy' -Method POST -Body '\"基于RSI超买超卖策略，当RSI低于30时买入，高于70时卖出\"' -ContentType 'application/json; charset=utf-8'"
6. 如果是修改接口，每次修改完接口后，尝试调用接口验证结果是否准确，不准确继续修改