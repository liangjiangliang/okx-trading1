# OKX交易回测工具使用指南

## 简介

OKX交易回测工具是一个基于Ta4j库的量化策略回测系统，允许交易者在不使用真实资金的情况下测试和评估各种交易策略的表现。系统支持多种技术指标策略，如简单移动平均线（SMA）和布林带（Bollinger Bands）等。

## 特点

- 支持多种交易策略（SMA、布林带等）
- 基于历史K线数据进行模拟交易
- 计算多种性能指标（收益率、胜率、夏普比率、最大回撤等）
- 详细的交易记录和汇总信息
- 结果可视化和CSV导出功能
- 命令行工具支持

## 支持的策略

目前支持以下交易策略：

1. **简单移动平均线交叉策略(SMA)**
   - 当短期均线上穿长期均线时买入
   - 当短期均线下穿长期均线时卖出
   - 参数格式：`短期均线周期,长期均线周期`（例如：`5,20`）

2. **布林带策略(Bollinger Bands)**
   - 当价格触及下轨时买入
   - 当价格触及上轨时卖出
   - 参数格式：`周期,标准差倍数`（例如：`20,2.0`）

## 使用方法

### API接口使用

可以通过以下API接口进行回测：

#### 1. 执行回测

```
GET /api/backtest/ta4j/run
```

**参数：**
- `symbol`: 交易对（例如：BTC-USDT）
- `interval`: 时间间隔（例如：1h, 4h, 1d）
- `startTime`: 开始时间（格式：yyyy-MM-dd HH:mm:ss）
- `endTime`: 结束时间（格式：yyyy-MM-dd HH:mm:ss）
- `strategyType`: 策略类型（SMA 或 BOLLINGER）
- `strategyParams`: 策略参数（格式取决于策略类型）
- `initialAmount`: 初始资金
- `saveResult`: 是否保存结果（true/false）

**示例：**
```
GET /api/backtest/ta4j/run?symbol=BTC-USDT&interval=1h&startTime=2023-01-01 00:00:00&endTime=2023-03-01 00:00:00&strategyType=BOLLINGER&strategyParams=20,2.0&initialAmount=10000&saveResult=true
```

#### 2. 获取回测历史

```
GET /api/backtest/ta4j/history
```

#### 3. 获取回测详情

```
GET /api/backtest/ta4j/detail/{backtestId}
```

#### 4. 获取回测汇总信息

```
GET /api/backtest/ta4j/summary/{backtestId}
```

### 命令行工具使用

还可以使用命令行工具来查询和导出回测结果：

#### 1. 列出最近的回测结果

```
java -jar okx-trading.jar --backtest.command=list
```

#### 2. 查看特定回测的详细信息

```
java -jar okx-trading.jar --backtest.command=print --backtest.id=<回测ID>
```

#### 3. 导出回测结果到CSV文件

```
java -jar okx-trading.jar --backtest.command=export --backtest.id=<回测ID> --backtest.export.path=/path/to/output.csv
```

#### 4. 查找最佳表现的回测

```
java -jar okx-trading.jar --backtest.command=best --backtest.strategy=SMA --backtest.symbol=BTC-USDT
```

## 回测结果解释

回测结果包含以下主要指标：

- **总收益率**: 策略在整个回测期间的收益率
- **胜率**: 盈利交易占总交易次数的比例
- **最大回撤**: 策略在任何时点经历的最大账户价值下跌百分比
- **夏普比率**: 风险调整后的收益指标，值越高表示策略风险回报比越好
- **交易次数**: 回测期间执行的总交易次数
- **盈利交易数**: 获利的交易次数
- **亏损交易数**: 亏损的交易次数

## 最佳实践

1. **选择合适的回测时间范围**: 建议回测不同的市场周期（牛市、熊市、震荡市）
2. **优化策略参数**: 尝试不同的参数组合，找到最优配置
3. **注意过度拟合**: 避免过度调整参数以适应特定历史数据
4. **考虑交易成本**: 实际交易中会产生手续费，可能影响策略表现
5. **结合多个指标**: 单一指标可能产生错误信号，综合考虑多个指标可提高准确性

## 贡献与反馈

如果您有任何问题、建议或者想要贡献代码，请提交issue或pull request。

## 免责声明

本工具仅供学习和研究使用，不构成投资建议。任何基于本工具的交易决策风险自负。历史表现不代表未来收益，实际交易结果可能与回测结果有显著差异。 