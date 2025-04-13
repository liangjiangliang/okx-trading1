# Ta4j回测系统使用说明

Ta4j是一个强大的Java技术分析和回测库，我们已将其集成到交易系统中，实现更加灵活和全面的策略回测功能。

## 一、系统架构

Ta4j回测系统由以下几个主要组件组成：

1. **数据转换器**（`CandlestickBarSeriesConverter`）：
   - 将现有的蜡烛图数据转换为Ta4j可以使用的格式
   - 支持从Ta4j的BarSeries转换回CandlestickEntity

2. **回测控制器**（`Ta4jBacktestController`）：
   - 提供REST API接口，用于执行不同策略的回测
   - 处理请求参数和回测结果的格式化

3. **策略实现**：
   - 简单移动平均线（SMA）交叉策略
   - 布林带策略（反转和突破两种模式）
   - 可自定义其他更多策略

## 二、可用的回测策略

### 1. 简单移动平均线策略（SMA交叉）

当短期移动平均线上穿长期移动平均线时买入，下穿时卖出。这是一种经典的趋势跟踪策略。

**参数说明**：
- `shortPeriod`：短期移动平均线周期，默认值：5
- `longPeriod`：长期移动平均线周期，默认值：20

### 2. 布林带策略

布林带策略有两种模式：

**反转模式（REVERSAL）**：
- 当价格触及下轨时买入（认为超卖）
- 当价格触及上轨时卖出（认为超买）
- 适合震荡市场

**突破模式（BREAKOUT）**：
- 当价格突破上轨时买入（认为上涨趋势确立）
- 当价格跌破下轨时卖出（认为下跌趋势确立）
- 适合趋势市场

**参数说明**：
- `period`：布林带周期，默认值：20
- `deviation`：标准差倍数，默认值：2.0
- `tradingMode`：交易模式，可选值：REVERSAL（反转）、BREAKOUT（突破）

## 三、API端点

### 1. SMA策略回测

```
GET /api/ta4j/backtest/sma
```

**请求参数**：
- `symbol`：交易对，如BTC-USDT
- `interval`：K线周期，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
- `startTime`：开始时间（yyyy-MM-dd HH:mm:ss）
- `endTime`：结束时间（yyyy-MM-dd HH:mm:ss）
- `shortPeriod`：短期均线周期，默认值：5
- `longPeriod`：长期均线周期，默认值：20
- `initialBalance`：初始资金，默认值：10000

### 2. 布林带策略回测

```
GET /api/ta4j/backtest/bollinger
```

**请求参数**：
- `symbol`：交易对，如BTC-USDT
- `interval`：K线周期，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M
- `startTime`：开始时间（yyyy-MM-dd HH:mm:ss）
- `endTime`：结束时间（yyyy-MM-dd HH:mm:ss）
- `period`：布林带周期，默认值：20
- `deviation`：标准差倍数，默认值：2.0
- `initialBalance`：初始资金，默认值：10000
- `tradingMode`：交易模式，可选值：REVERSAL（反转）、BREAKOUT（突破），默认值：REVERSAL

## 四、回测结果指标说明

返回的回测结果包含以下关键指标：

1. **基本信息**：
   - `symbol`：交易对
   - `interval`：K线周期
   - `dataPoints`：数据点数量
   - `startTime`：开始时间
   - `endTime`：结束时间
   - `strategyType`：策略类型
   - `strategyParams`：策略参数

2. **绩效指标**：
   - `initialBalance`：初始资金
   - `finalBalance`：最终资金
   - `totalReturn`：总收益率
   - `absoluteProfit`：绝对收益
   - `tradeCount`：交易次数
   - `positionCount`：持仓次数
   - `winCount`：盈利交易次数
   - `lossCount`：亏损交易次数
   - `winRate`：胜率
   - `totalProfit`：总盈利
   - `totalLoss`：总亏损
   - `profitFactor`：盈亏比

## 五、自定义策略

如需添加新的交易策略，请按以下步骤操作：

1. 在 `Ta4jBacktestController` 中添加新的策略创建方法，参考已有的SMA和布林带策略
2. 创建新的API端点，处理请求参数和回测逻辑
3. 构建相应的指标和规则

## 六、与传统回测框架的对比

Ta4j回测系统相比原有回测框架有以下优势：

1. **更加专业**：使用专门为技术分析设计的库
2. **更多内置指标**：Ta4j提供丰富的技术指标实现
3. **更灵活的策略定义**：基于规则（Rule）和指标（Indicator）组合构建策略
4. **更完善的性能评估**：提供标准化的策略评估指标
5. **可视化支持**：更好地支持结果可视化（需要前端配合）

## 七、使用示例

### 示例1：进行SMA策略回测

```
http://localhost:8080/api/ta4j/backtest/sma?symbol=BTC-USDT&interval=1D&startTime=2022-01-01%2000:00:00&endTime=2023-01-01%2000:00:00&shortPeriod=5&longPeriod=20&initialBalance=10000
```

### 示例2：进行布林带反转策略回测

```
http://localhost:8080/api/ta4j/backtest/bollinger?symbol=BTC-USDT&interval=1D&startTime=2022-01-01%2000:00:00&endTime=2023-01-01%2000:00:00&period=20&deviation=2.0&tradingMode=REVERSAL&initialBalance=10000
```

### 示例3：进行布林带突破策略回测

```
http://localhost:8080/api/ta4j/backtest/bollinger?symbol=BTC-USDT&interval=1D&startTime=2022-01-01%2000:00:00&endTime=2023-01-01%2000:00:00&period=20&deviation=2.0&tradingMode=BREAKOUT&initialBalance=10000
``` 