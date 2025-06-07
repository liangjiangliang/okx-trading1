-- 初始化策略信息表数据

-- 移动平均线策�?INSERT INTO `strategy_info` (`strategy_code`, `strategy_name`, `description`, `params_desc`, `default_params`, `category`, `create_time`, `update_time`) VALUES
('SMA', '简单移动平均线策略', '基于简单移动平均线的交叉信号生成买卖决�?, '{"shortPeriod":"短期均线周期","longPeriod":"长期均线周期"}', '{"shortPeriod":9,"longPeriod":26}', '移动平均线策�?, NOW(), NOW()),
('EMA', '指数移动平均线策�?, '基于指数移动平均线的交叉信号生成买卖决策', '{"shortPeriod":"短期均线周期","longPeriod":"长期均线周期"}', '{"shortPeriod":9,"longPeriod":26}', '移动平均线策�?, NOW(), NOW()),
('WMA', '加权移动平均线策�?, '基于加权移动平均线的交叉信号生成买卖决策', '{"shortPeriod":"短期均线周期","longPeriod":"长期均线周期"}', '{"shortPeriod":9,"longPeriod":26}', '移动平均线策�?, NOW(), NOW()),
('HMA', '赫尔移动平均线策�?, '基于赫尔移动平均线的交叉信号生成买卖决策', '{"shortPeriod":"短期均线周期","longPeriod":"长期均线周期"}', '{"shortPeriod":9,"longPeriod":26}', '移动平均线策�?, NOW(), NOW()),
('KAMA', '考夫曼自适应移动平均线策�?, '基于考夫曼自适应移动平均线的交叉信号生成买卖决策', '{"shortPeriod":"短期均线周期","longPeriod":"长期均线周期","effRatioShortPeriod":"效率比率短期","effRatioLongPeriod":"效率比率长期"}', '{"shortPeriod":9,"longPeriod":26,"effRatioShortPeriod":2,"effRatioLongPeriod":30}', '移动平均线策�?, NOW(), NOW()),
('ZLEMA', '零滞后指数移动平均线策略', '基于零滞后指数移动平均线的交叉信号生成买卖决�?, '{"shortPeriod":"短期均线周期","longPeriod":"长期均线周期"}', '{"shortPeriod":9,"longPeriod":26}', '移动平均线策�?, NOW(), NOW()),
('DEMA', '双重指数移动平均线策�?, '基于双重指数移动平均线的交叉信号生成买卖决策', '{"shortPeriod":"短期均线周期","longPeriod":"长期均线周期"}', '{"shortPeriod":9,"longPeriod":26}', '移动平均线策�?, NOW(), NOW()),
('TEMA', '三重指数移动平均线策�?, '基于三重指数移动平均线的交叉信号生成买卖决策', '{"shortPeriod":"短期均线周期","longPeriod":"长期均线周期"}', '{"shortPeriod":9,"longPeriod":26}', '移动平均线策�?, NOW(), NOW()),
('VWAP', '成交量加权平均价格策�?, '基于成交量加权平均价格的交叉信号生成买卖决策', '{"period":"计算周期"}', '{"period":14}', '移动平均线策�?, NOW(), NOW()),

-- 震荡指标策略
('RSI', '相对强弱指数策略', '基于相对强弱指数的超买超卖信号生成买卖决�?, '{"period":"RSI计算周期","overSoldThreshold":"超卖阈�?,"overBoughtThreshold":"超买阈�?}', '{"period":14,"overSoldThreshold":30,"overBoughtThreshold":70}', '震荡指标策略', NOW(), NOW()),
('STOCHASTIC', '随机指标策略', '基于随机指标的超买超卖信号生成买卖决�?, '{"kPeriod":"K值周�?,"dPeriod":"D值周�?,"overSoldThreshold":"超卖阈�?,"overBoughtThreshold":"超买阈�?}', '{"kPeriod":14,"dPeriod":3,"overSoldThreshold":20,"overBoughtThreshold":80}', '震荡指标策略', NOW(), NOW()),
('STOCHASTIC_RSI', '随机相对强弱指数策略', '结合随机指标和RSI的优势，提供更敏感的超买超卖信号', '{"rsiPeriod":"RSI计算周期","stochasticPeriod":"随机指标周期","kPeriod":"K值周�?,"dPeriod":"D值周�?,"overSoldThreshold":"超卖阈�?,"overBoughtThreshold":"超买阈�?}', '{"rsiPeriod":14,"stochasticPeriod":14,"kPeriod":3,"dPeriod":3,"overSoldThreshold":20,"overBoughtThreshold":80}', '震荡指标策略', NOW(), NOW()),
('CCI', '商品通道指数策略', '基于商品通道指数的超买超卖信号生成买卖决�?, '{"period":"CCI计算周期","overSoldThreshold":"超卖阈�?,"overBoughtThreshold":"超买阈�?}', '{"period":20,"overSoldThreshold":-100,"overBoughtThreshold":100}', '震荡指标策略', NOW(), NOW()),
('WILLIAMS_R', '威廉指标策略', '基于威廉指标的超买超卖信号生成买卖决�?, '{"period":"计算周期","overSoldThreshold":"超卖阈�?,"overBoughtThreshold":"超买阈�?}', '{"period":14,"overSoldThreshold":-80,"overBoughtThreshold":-20}', '震荡指标策略', NOW(), NOW()),
('CMO', '钱德动量摆动指标策略', '基于钱德动量摆动指标的超买超卖信号生成买卖决�?, '{"period":"计算周期","overSoldThreshold":"超卖阈�?,"overBoughtThreshold":"超买阈�?}', '{"period":14,"overSoldThreshold":-50,"overBoughtThreshold":50}', '震荡指标策略', NOW(), NOW()),
('ROC', '变动率指标策�?, '基于价格变动率的超买超卖信号生成买卖决策', '{"period":"计算周期","overSoldThreshold":"超卖阈�?,"overBoughtThreshold":"超买阈�?}', '{"period":12,"overSoldThreshold":-5,"overBoughtThreshold":5}', '震荡指标策略', NOW(), NOW()),
('PPO', '百分比价格震荡指标策�?, '基于百分比价格震荡指标的超买超卖信号生成买卖决策', '{"shortPeriod":"短期周期","longPeriod":"长期周期","signalPeriod":"信号周期"}', '{"shortPeriod":12,"longPeriod":26,"signalPeriod":9}', '震荡指标策略', NOW(), NOW()),
('DPO', '区间震荡指标策略', '基于区间震荡指标的超买超卖信号生成买卖决�?, '{"period":"计算周期"}', '{"period":20}', '震荡指标策略', NOW(), NOW()),
('TRIX', '三重指数平滑平均指标策略', '基于三重指数平滑平均指标的超买超卖信号生成买卖决�?, '{"period":"计算周期","signalPeriod":"信号周期"}', '{"period":15,"signalPeriod":9}', '震荡指标策略', NOW(), NOW()),

-- 趋势指标策略
('MACD', '移动平均收敛发散策略', '基于MACD指标的趋势信号生成买卖决�?, '{"shortPeriod":"短期EMA周期","longPeriod":"长期EMA周期","signalPeriod":"信号线周�?}', '{"shortPeriod":12,"longPeriod":26,"signalPeriod":9}', '趋势指标策略', NOW(), NOW()),
('ADX', '平均趋向指数策略', '基于ADX指标的趋势强度信号生成买卖决�?, '{"period":"计算周期","adxThreshold":"ADX阈�?}', '{"period":14,"adxThreshold":25}', '趋势指标策略', NOW(), NOW()),
('AWESOME_OSCILLATOR', '神奇震荡指标策略', '基于神奇震荡指标的趋势信号生成买卖决�?, '{"shortPeriod":"短期周期","longPeriod":"长期周期"}', '{"shortPeriod":5,"longPeriod":34}', '趋势指标策略', NOW(), NOW()),
('AROO', '阿隆指标策略', '基于阿隆指标的趋势信号生成买卖决�?, '{"period":"计算周期"}', '{"period":25}', '趋势指标策略', NOW(), NOW()),
('ICHIMOKU_CLOUD_BREAKOUT', '一目均衡表云突破策�?, '基于一目均衡表云突破的趋势信号生成买卖决策', '{"conversionPeriod":"转换线周�?,"basePeriod":"基准线周�?,"spanPeriod":"先行跨度周期","displacement":"延迟周期"}', '{"conversionPeriod":9,"basePeriod":26,"spanPeriod":52,"displacement":26}', '趋势指标策略', NOW(), NOW()),
('DMA', '差分移动平均线策�?, '基于差分移动平均线的趋势信号生成买卖决策', '{"shortPeriod":"短期周期","longPeriod":"长期周期","signalPeriod":"信号周期"}', '{"shortPeriod":10,"longPeriod":50,"signalPeriod":10}', '趋势指标策略', NOW(), NOW()),
('DMI', '方向运动指标策略', '基于方向运动指标的趋势信号生成买卖决�?, '{"period":"计算周期"}', '{"period":14}', '趋势指标策略', NOW(), NOW()),
('SUPERTREND', '超级趋势指标策略', '基于超级趋势指标的趋势信号生成买卖决�?, '{"period":"计算周期","multiplier":"乘数"}', '{"period":10,"multiplier":3}', '趋势指标策略', NOW(), NOW()),

-- 波动指标策略
('BOLLINGER', '布林带策�?, '基于布林带的价格通道信号生成买卖决策', '{"period":"计算周期","stdDev":"标准差倍数"}', '{"period":20,"stdDev":2}', '波动指标策略', NOW(), NOW()),
('KELTNER_CHANNEL', '肯特纳通道策略', '基于肯特纳通道的价格通道信号生成买卖决策', '{"emaPeriod":"EMA周期","atrPeriod":"ATR周期","multiplier":"乘数"}', '{"emaPeriod":20,"atrPeriod":10,"multiplier":2}', '波动指标策略', NOW(), NOW()),
('ULCER_INDEX', '溃疡指数策略', '基于溃疡指数的市场风险信号生成买卖决�?, '{"period":"计算周期","threshold":"阈�?}', '{"period":14,"threshold":5}', '波动指标策略', NOW(), NOW()),
('ATR', '平均真实波幅策略', '基于平均真实波幅的波动性信号生成买卖决�?, '{"period":"计算周期"}', '{"period":14}', '波动指标策略', NOW(), NOW()),
('PARABOLIC_SAR', '抛物线SAR策略', '基于抛物线SAR的趋势反转信号生成买卖决�?, '{"accelerationFactor":"加速因�?,"maxAcceleration":"最大加速�?}', '{"accelerationFactor":0.02,"maxAcceleration":0.2}', '波动指标策略', NOW(), NOW()),

-- 成交量指标策�?('KDJ', 'KDJ指标策略', '基于KDJ指标的超买超卖信号生成买卖决�?, '{"kPeriod":"K值周�?,"dPeriod":"D值周�?,"jPeriod":"J值周�?,"overSoldThreshold":"超卖阈�?,"overBoughtThreshold":"超买阈�?}', '{"kPeriod":9,"dPeriod":3,"jPeriod":3,"overSoldThreshold":20,"overBoughtThreshold":80}', '成交量指标策�?, NOW(), NOW()),
('OBV', '能量潮指标策�?, '基于能量潮指标的成交量信号生成买卖决�?, '{"period":"计算周期"}', '{"period":20}', '成交量指标策�?, NOW(), NOW()),
('MASS_INDEX', '质量指数策略', '基于质量指数的成交量信号生成买卖决策', '{"emaPeriod":"EMA周期","sumPeriod":"求和周期","threshold":"阈�?}', '{"emaPeriod":9,"sumPeriod":25,"threshold":27}', '成交量指标策�?, NOW(), NOW()),

-- 蜡烛图形态策�?('DOJI', '十字星策�?, '基于十字星形态的反转信号生成买卖决策', '{"bodyFactor":"实体因子"}', '{"bodyFactor":0.1}', '蜡烛图形态策�?, NOW(), NOW()),
('BULLISH_ENGULFING', '看涨吞没策略', '基于看涨吞没形态的反转信号生成买卖决策', '{"bodyFactor":"实体因子"}', '{"bodyFactor":0.1}', '蜡烛图形态策�?, NOW(), NOW()),
('BEARISH_ENGULFING', '看跌吞没策略', '基于看跌吞没形态的反转信号生成买卖决策', '{"bodyFactor":"实体因子"}', '{"bodyFactor":0.1}', '蜡烛图形态策�?, NOW(), NOW()),
('BULLISH_HARAMI', '看涨孕线策略', '基于看涨孕线形态的反转信号生成买卖决策', '{"bodyFactor":"实体因子"}', '{"bodyFactor":0.1}', '蜡烛图形态策�?, NOW(), NOW()),
('BEARISH_HARAMI', '看跌孕线策略', '基于看跌孕线形态的反转信号生成买卖决策', '{"bodyFactor":"实体因子"}', '{"bodyFactor":0.1}', '蜡烛图形态策�?, NOW(), NOW()),
('THREE_WHITE_SOLDIERS', '三白兵策�?, '基于三白兵形态的反转信号生成买卖决策', '{"bodyFactor":"实体因子"}', '{"bodyFactor":0.1}', '蜡烛图形态策�?, NOW(), NOW()),
('THREE_BLACK_CROWS', '三黑乌鸦策略', '基于三黑乌鸦形态的反转信号生成买卖决策', '{"bodyFactor":"实体因子"}', '{"bodyFactor":0.1}', '蜡烛图形态策�?, NOW(), NOW()),
('HANGING_MA', '吊人线策�?, '基于吊人线形态的反转信号生成买卖决策', '{"bodyFactor":"实体因子","shadowFactor":"影线因子"}', '{"bodyFactor":0.1,"shadowFactor":2}', '蜡烛图形态策�?, NOW(), NOW()),

-- 组合策略
('DUAL_THRUST', '双推策略', '基于价格突破区间的双推策略生成买卖决�?, '{"period":"计算周期","buyCoef":"买入系数","sellCoef":"卖出系数"}', '{"period":14,"buyCoef":0.5,"sellCoef":0.5}', '组合策略', NOW(), NOW()),
('TURTLE_TRADING', '海龟交易策略', '基于唐奇安通道的海龟交易策略生成买卖决�?, '{"entryPeriod":"入场周期","exitPeriod":"出场周期","stopLossPeriod":"止损周期"}', '{"entryPeriod":20,"exitPeriod":10,"stopLossPeriod":2}', '组合策略', NOW(), NOW()),
('MEAN_REVERSIO', '均值回归策�?, '基于价格回归均值的策略生成买卖决策', '{"period":"计算周期","stdDev":"标准差倍数"}', '{"period":20,"stdDev":2}', '组合策略', NOW(), NOW()),
('TREND_FOLLOWING', '趋势跟踪策略', '基于价格趋势的跟踪策略生成买卖决�?, '{"shortPeriod":"短期周期","longPeriod":"长期周期"}', '{"shortPeriod":10,"longPeriod":50}', '组合策略', NOW(), NOW()),
('BREAKOUT', '突破策略', '基于价格突破支撑阻力位的策略生成买卖决策', '{"period":"计算周期"}', '{"period":20}', '组合策略', NOW(), NOW()),
('GOLDEN_CROSS', '金叉策略', '基于短期均线上穿长期均线的策略生成买卖决�?, '{"shortPeriod":"短期均线周期","longPeriod":"长期均线周期","signalPeriod":"信号周期"}', '{"shortPeriod":9,"longPeriod":26,"signalPeriod":9}', '组合策略', NOW(), NOW()),
('DEATH_CROSS', '死叉策略', '基于短期均线下穿长期均线的策略生成买卖决�?, '{"shortPeriod":"短期均线周期","longPeriod":"长期均线周期","signalPeriod":"信号周期"}', '{"shortPeriod":9,"longPeriod":26,"signalPeriod":9}', '组合策略', NOW(), NOW()),
('DUAL_MA_WITH_RSI', '双均线RSI组合策略', '结合双均线和RSI指标的组合策略生成买卖决�?, '{"shortPeriod":"短期均线周期","longPeriod":"长期均线周期","rsiPeriod":"RSI周期","overSoldThreshold":"超卖阈�?,"overBoughtThreshold":"超买阈�?}', '{"shortPeriod":9,"longPeriod":26,"rsiPeriod":14,"overSoldThreshold":30,"overBoughtThreshold":70}', '组合策略', NOW(), NOW()),
('MACD_WITH_BOLLINGER', 'MACD与布林带组合策略', '结合MACD和布林带的组合策略生成买卖决�?, '{"shortPeriod":"短期EMA周期","longPeriod":"长期EMA周期","signalPeriod":"信号线周�?,"bollingerPeriod":"布林带周�?,"stdDev":"标准差倍数"}', '{"shortPeriod":12,"longPeriod":26,"signalPeriod":9,"bollingerPeriod":20,"stdDev":2}', '组合策略', NOW(), NOW()),
('ICHIMOKU', '一目均衡表策略', '基于一目均衡表的综合信号生成买卖决�?, '{"conversionPeriod":"转换线周�?,"basePeriod":"基准线周�?,"spanPeriod":"先行跨度周期","displacement":"延迟周期"}', '{"conversionPeriod":9,"basePeriod":26,"spanPeriod":52,"displacement":26}', '组合策略', NOW(), NOW());
