# AI策略生成功能说明

## 功能概述

本项目新增了基于DeepSeek API的AI策略生成功能，可以通过自然语言描述自动生成Ta4j交易策略，并动态加载到系统中进行回测。

## 主要特性

1. **AI策略生成**: 通过DeepSeek API根据策略描述生成lambda函数代码
2. **动态编译加载**: 使用Janino编译器动态编译策略代码并加载到StrategyFactory
3. **数据库存储**: 策略代码存储在strategy_info表的source_code字段中
4. **热更新**: 无需重启服务即可加载新策略
5. **策略更新**: 支持修改策略描述并重新生成代码

## 环境配置

### 1. 数据库配置

执行以下SQL为strategy_info表添加source_code字段：

```sql
ALTER TABLE strategy_info ADD COLUMN source_code TEXT COMMENT '策略源代码，存储lambda函数的序列化字符串';
```

### 2. DeepSeek API配置

在环境变量中设置DeepSeek API密钥：

```bash
export DEEPSEEK_API_KEY=your_deepseek_api_key
```

或在application.yml中配置：

```yaml
deepseek:
  api:
    key: your_deepseek_api_key
    url: https://api.deepseek.com/v1/chat/completions
```

## API接口说明

### 1. 生成AI策略

**接口**: `POST /api/backtest/ta4j/generate-strategy`

**请求参数**:
```json
{
  "strategyCode": "AI_GENERATED_001",
  "strategyName": "AI生成的双均线策略",
  "description": "基于5日和20日移动平均线的交叉策略，当短期均线上穿长期均线时买入，下穿时卖出",
  "category": "AI生成策略",
  "paramsDesc": "无需参数",
  "defaultParams": ""
}
```

**响应示例**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "strategyCode": "AI_GENERATED_001",
    "strategyName": "AI生成的双均线策略",
    "description": "基于5日和20日移动平均线的交叉策略",
    "sourceCode": "(series, params) -> { ... }",
    "category": "AI生成策略",
    "createTime": "2024-01-01T10:00:00",
    "updateTime": "2024-01-01T10:00:00"
  }
}
```

### 2. 更新策略

**接口**: `PUT /api/backtest/ta4j/update-strategy`

**请求参数**:
```json
{
  "id": 1,
  "strategyCode": "AI_GENERATED_001",
  "strategyName": "更新后的AI策略",
  "description": "更新后的策略描述，基于RSI指标的超买超卖策略",
  "category": "AI生成策略"
}
```

### 3. 重新加载动态策略

**接口**: `POST /api/backtest/ta4j/reload-dynamic-strategies`

用于手动重新加载数据库中的所有动态策略。

### 4. 使用生成的策略进行回测

生成策略后，可以直接使用现有的回测接口：

**接口**: `GET /api/backtest/ta4j/run`

**参数示例**:
```
symbol=BTC-USDT
interval=1h
startTime=2023-01-01 00:00:00
endTime=2023-12-31 23:59:59
strategyType=AI_GENERATED_001
initialAmount=100000
saveResult=true
```

## 技术实现

### 1. 核心组件

- **DeepSeekApiService**: 调用DeepSeek API生成策略代码
- **DynamicStrategyService**: 编译和动态加载策略
- **StrategyInfoEntity**: 扩展了source_code字段存储策略代码
- **DynamicStrategyConfig**: 应用启动时自动加载动态策略

### 2. 策略代码格式

生成的策略代码为lambda函数，格式如下：

```java
(BarSeries series, Map<String, Object> params) -> {
    // 创建指标
    SMAIndicator shortSma = new SMAIndicator(series.getBarData(BarData.CLOSE), 5);
    SMAIndicator longSma = new SMAIndicator(series.getBarData(BarData.CLOSE), 20);
    
    // 买入规则
    Rule buyRule = new CrossedUpIndicatorRule(shortSma, longSma);
    
    // 卖出规则
    Rule sellRule = new CrossedDownIndicatorRule(shortSma, longSma);
    
    return new BaseStrategy(buyRule, sellRule);
}
```

### 3. 动态加载机制

1. 使用Janino编译器编译lambda函数代码
2. 通过反射获取StrategyFactory的strategyCreators字段
3. 将编译后的函数添加到策略映射中
4. 支持运行时热更新，无需重启服务

## 使用示例

### 1. 生成简单移动平均线策略

```bash
curl -X POST "http://localhost:8088/api/backtest/ta4j/generate-strategy" \
  -H "Content-Type: application/json" \
  -d '{
    "strategyCode": "AI_SMA_CROSS",
    "strategyName": "AI双均线交叉策略",
    "description": "当10日均线上穿30日均线时买入，下穿时卖出",
    "category": "AI生成策略"
  }'
```

### 2. 生成RSI策略

```bash
curl -X POST "http://localhost:8088/api/backtest/ta4j/generate-strategy" \
  -H "Content-Type: application/json" \
  -d '{
    "strategyCode": "AI_RSI_STRATEGY",
    "strategyName": "AI RSI超买超卖策略",
    "description": "当RSI低于30时买入，高于70时卖出",
    "category": "AI生成策略"
  }'
```

### 3. 执行回测

```bash
curl "http://localhost:8088/api/backtest/ta4j/run?symbol=BTC-USDT&interval=1h&startTime=2023-01-01%2000:00:00&endTime=2023-12-31%2023:59:59&strategyType=AI_SMA_CROSS&initialAmount=100000&saveResult=true"
```

## 注意事项

1. **API密钥安全**: 请妥善保管DeepSeek API密钥，不要提交到代码仓库
2. **策略验证**: AI生成的策略代码需要经过测试验证后再用于实际交易
3. **性能考虑**: 动态编译会消耗一定的CPU资源，建议在低峰期进行策略更新
4. **错误处理**: 如果策略代码编译失败，请检查生成的代码是否符合Ta4j语法规范
5. **数据库备份**: 在执行数据库迁移前请做好备份

## 故障排除

### 1. API调用失败
- 检查DeepSeek API密钥是否正确配置
- 确认网络连接正常
- 查看应用日志获取详细错误信息

### 2. 策略编译失败
- 检查生成的代码语法是否正确
- 确认所有必要的import语句已包含
- 查看DynamicStrategyService的日志

### 3. 策略加载失败
- 确认数据库中的source_code字段不为空
- 检查StrategyFactory的反射访问权限
- 重启应用重新加载所有策略

## 扩展功能

未来可以考虑添加以下功能：

1. **策略模板**: 预定义常用策略模板
2. **策略评分**: 基于历史回测结果对策略进行评分
3. **策略组合**: 支持多个策略的组合使用
4. **可视化编辑**: 提供图形化的策略编辑界面
5. **策略分享**: 支持策略的导入导出和分享功能