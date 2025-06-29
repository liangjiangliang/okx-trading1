# 新增风险指标测试说明

## 概述
本文档说明如何测试新增的高级风险指标和综合评分系统。

## 新增功能列表

### 1. 新增风险指标 (15个)
- **峰度 (kurtosis)** - 衡量收益率分布的尾部风险
- **条件风险价值 (cvar)** - 极端损失的期望值
- **风险价值 (var95, var99)** - 95%和99%置信度下的风险价值
- **信息比率 (informationRatio)** - 超额收益相对于跟踪误差的比率
- **跟踪误差 (trackingError)** - 策略与基准收益率的标准差
- **Sterling比率 (sterlingRatio)** - 年化收益与平均最大回撤的比率
- **Burke比率 (burkeRatio)** - 年化收益与平方根回撤的比率
- **修正夏普比率 (modifiedSharpeRatio)** - 考虑偏度和峰度的夏普比率
- **下行偏差 (downsideDeviation)** - 只考虑负收益的标准差
- **上涨捕获率 (uptrendCapture)** - 基准上涨时策略的表现
- **下跌捕获率 (downtrendCapture)** - 基准下跌时策略的表现
- **最大回撤持续期 (maxDrawdownDuration)** - 从峰值到恢复的最长时间
- **痛苦指数 (painIndex)** - 回撤深度与持续时间的综合指标
- **风险调整收益 (riskAdjustedReturn)** - 综合多种风险因素的收益评估

### 2. 综合评分系统 (1个)
- **综合评分 (comprehensiveScore)** - 0-10分的科学评分体系

## 测试方法

### 1. 单个策略回测测试
```bash
curl --location 'http://localhost:8088/api/backtest/ta4j/run?endTime=2025-01-01%2000%3A00%3A00&initialAmount=10000&interval=1D&saveResult=true&startTime=2024-01-01%2000%3A00%3A00&strategyType=SMA&symbol=BTC-USDT'
```

### 2. 批量策略回测测试
```bash
curl --location 'http://localhost:8088/api/backtest/ta4j/run-all?startTime=2024-01-01%2000%3A00%3A00&endTime=2024-12-01%2000%3A00%3A00&initialAmount=10000&symbol=BTC-USDT&interval=1D&saveResult=true&feeRatio=0.001'
```

### 3. 数据库迁移测试
执行以下脚本添加新字段到现有数据库：
```bash
mysql -u root -p okx_trading < src/main/resources/migration_add_risk_indicators.sql
```

## 预期结果

### API 响应应包含所有新字段
```json
{
  "success": true,
  "data": {
    "success": true,
    "backtestId": "uuid",
    "totalReturn": 0.1234,
    "sharpeRatio": 1.5,
    
    // 新增的风险指标
    "kurtosis": 2.5,
    "cvar": -0.05,
    "var95": -0.03,
    "var99": -0.07,
    "informationRatio": 0.8,
    "trackingError": 0.02,
    "sterlingRatio": 1.2,
    "burkeRatio": 1.1,
    "modifiedSharpeRatio": 1.3,
    "downsideDeviation": 0.015,
    "uptrendCapture": 0.85,
    "downtrendCapture": 0.75,
    "maxDrawdownDuration": 30.5,
    "painIndex": 0.25,
    "riskAdjustedReturn": 0.08,
    
    // 综合评分
    "comprehensiveScore": 7.5
  }
}
```

### 数据库存储验证
1. 检查 `backtest_summary` 表是否包含新字段
2. 执行回测后，确认新字段被正确保存
3. 查询回测历史，确认新指标能正确显示

### 控制台输出验证
回测完成后，控制台应显示新增指标：
```
峰度: 2.5000
VaR95%: -0.0300
CVaR: -0.0500
信息比率: 0.8000
修正夏普比率: 1.3000
痛苦指数: 0.2500
------------------------------------------------------
综合评分: 7.50/10
```

## 评分标准说明

### 评分维度 (总分10分)
1. **收益表现 (4.0分)** - 40%权重
   - 年化收益率 (1.5分)
   - 总收益率 (1.5分)  
   - 盈利因子 (1.0分)

2. **风险控制 (3.0分)** - 30%权重
   - 夏普比率 (1.0分)
   - 最大回撤 (1.0分)
   - 波动率 (0.5分)
   - VaR指标 (0.5分)

3. **交易质量 (2.0分)** - 20%权重
   - 胜率 (1.0分)
   - 平均盈利 (0.5分)
   - 交易次数适度性 (0.5分)

4. **稳定性 (1.0分)** - 10%权重
   - 偏度 (0.3分)
   - 峰度 (0.3分)
   - 痛苦指数 (0.4分)

### 评分等级
- **9.0-10.0分**: 优秀策略
- **7.0-8.9分**: 良好策略
- **5.0-6.9分**: 一般策略
- **3.0-4.9分**: 较差策略
- **0.0-2.9分**: 不推荐策略

## 故障排除

### 常见问题
1. **新字段为null**: 检查BacktestMetricsCalculator计算逻辑
2. **数据库保存失败**: 确认schema.sql已更新，或执行迁移脚本
3. **评分异常**: 检查综合评分算法的边界条件处理

### 日志检查
- 查看应用日志中的回测计算过程
- 确认所有新指标都有相应的日志输出
- 检查数据库操作是否成功

## 注意事项
1. 新增字段在无交易情况下默认为0
2. 部分指标需要基准数据(BTC-USDT)才能计算
3. 综合评分算法考虑了不同市场环境的适应性
4. 建议在生产环境使用前进行充分测试 