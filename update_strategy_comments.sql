-- 更新策略信息表的comments字段
-- 移动平均线策略
UPDATE `strategy_info` SET `comments` = '适用于趋势市场，经典的均线策略，信号明确但可能滞后，适合中长线交易' WHERE `strategy_code` = 'SMA';
UPDATE `strategy_info` SET `comments` = '对价格变化反应更敏感，适用于趋势市场，信号及时但可能产生更多假信号' WHERE `strategy_code` = 'EMA';
UPDATE `strategy_info` SET `comments` = '更重视近期价格，反应速度介于SMA和EMA之间，适合趋势跟踪' WHERE `strategy_code` = 'WMA';
UPDATE `strategy_info` SET `comments` = '减少滞后性同时保持平滑性，适用于快速变化的市场环境' WHERE `strategy_code` = 'HMA';
UPDATE `strategy_info` SET `comments` = '根据市场波动性自动调整，在趋势市场反应快，震荡市场保持稳定' WHERE `strategy_code` = 'KAMA';
UPDATE `strategy_info` SET `comments` = '显著减少滞后性，适合快速交易，但可能增加假信号' WHERE `strategy_code` = 'ZLEMA';
UPDATE `strategy_info` SET `comments` = '比EMA更敏感，减少滞后的同时保持相对稳定性' WHERE `strategy_code` = 'DEMA';
UPDATE `strategy_info` SET `comments` = '最小化滞后性，适合短线交易，但信号可能较为敏感' WHERE `strategy_code` = 'TEMA';
UPDATE `strategy_info` SET `comments` = '结合成交量信息，适合日内交易，反映真实的市场成本' WHERE `strategy_code` = 'VWAP';
UPDATE `strategy_info` SET `comments` = '双重平滑处理，减少噪音，适合震荡市场，信号稳定但反应较慢' WHERE `strategy_code` = 'TRIMA';
UPDATE `strategy_info` SET `comments` = '结合平滑性和响应性，适合中线趋势跟踪，参数可调整敏感度' WHERE `strategy_code` = 'T3';
UPDATE `strategy_info` SET `comments` = '自适应算法，能根据市场周期自动调整，适合复杂多变的市场环境' WHERE `strategy_code` = 'MAMA';
UPDATE `strategy_info` SET `comments` = '根据波动性调整权重，高波动时更敏感，低波动时更平滑' WHERE `strategy_code` = 'VIDYA';
UPDATE `strategy_info` SET `comments` = '威尔德专用EMA，常用于RSI等技术指标计算，信号平滑稳定' WHERE `strategy_code` = 'WILDERS';

-- 震荡指标策略
UPDATE `strategy_info` SET `comments` = '经典震荡指标，适合震荡市场和反转交易，信号清晰可靠' WHERE `strategy_code` = 'RSI';
UPDATE `strategy_info` SET `comments` = '敏感度较高的震荡指标，适合捕捉短期反转机会' WHERE `strategy_code` = 'STOCHASTIC';
UPDATE `strategy_info` SET `comments` = '组合指标，提供更早的反转信号，但可能产生更多假信号' WHERE `strategy_code` = 'STOCHASTIC_RSI';
UPDATE `strategy_info` SET `comments` = '衡量价格偏离均值程度，适合识别极端价格水平' WHERE `strategy_code` = 'CCI';
UPDATE `strategy_info` SET `comments` = '快速震荡指标，反应敏感，适合短线交易和快速反转识别' WHERE `strategy_code` = 'WILLIAMS_R';
UPDATE `strategy_info` SET `comments` = '衡量价格动量，避免价格长期停滞的影响，适合趋势确认' WHERE `strategy_code` = 'CMO';
UPDATE `strategy_info` SET `comments` = '简单有效的动量指标，适合识别价格变化速度和反转点' WHERE `strategy_code` = 'ROC';
UPDATE `strategy_info` SET `comments` = '百分比形式的MACD，便于比较不同价格水平的股票' WHERE `strategy_code` = 'PPO';
UPDATE `strategy_info` SET `comments` = '消除趋势影响，专注于周期性波动，适合周期性市场分析' WHERE `strategy_code` = 'DPO';
UPDATE `strategy_info` SET `comments` = '过滤价格噪音，提供平滑的动量信号，适合中长线趋势判断' WHERE `strategy_code` = 'TRIX';
UPDATE `strategy_info` SET `comments` = '将价格数据转换为正态分布，提供更清晰的超买超卖信号' WHERE `strategy_code` = 'FISHER';
UPDATE `strategy_info` SET `comments` = '基于线性回归的预测，适合识别价格偏离趋势的程度' WHERE `strategy_code` = 'FOSC';
UPDATE `strategy_info` SET `comments` = '结合价格和成交量，衡量价格移动的难易程度' WHERE `strategy_code` = 'EOM';
UPDATE `strategy_info` SET `comments` = '识别市场是处于趋势还是震荡状态，辅助策略选择' WHERE `strategy_code` = 'CHOP';
UPDATE `strategy_info` SET `comments` = '结合价格和成交量的高级震荡器，适合确认趋势强度' WHERE `strategy_code` = 'KVO';
UPDATE `strategy_info` SET `comments` = '衡量收盘价相对于开盘价的位置，反映买卖力量对比' WHERE `strategy_code` = 'RVGI';
UPDATE `strategy_info` SET `comments` = '结合MACD和随机指标优势，提供更准确的趋势信号' WHERE `strategy_code` = 'STC';

-- 趋势指标策略
UPDATE `strategy_info` SET `comments` = '经典趋势指标，适合趋势确认和动量分析，信号可靠' WHERE `strategy_code` = 'MACD';
UPDATE `strategy_info` SET `comments` = '衡量趋势强度而非方向，适合确认趋势是否值得跟随' WHERE `strategy_code` = 'ADX';
UPDATE `strategy_info` SET `comments` = '简单有效的动量指标，适合识别动量变化' WHERE `strategy_code` = 'AWESOME_OSCILLATOR';
UPDATE `strategy_info` SET `comments` = '识别趋势的开始和结束，适合趋势转换点分析' WHERE `strategy_code` = 'AROON';
UPDATE `strategy_info` SET `comments` = '日本传统技术分析，提供全面的趋势信息' WHERE `strategy_code` = 'ICHIMOKU_CLOUD_BREAKOUT';
UPDATE `strategy_info` SET `comments` = '双均线差值指标，适合趋势强度判断' WHERE `strategy_code` = 'DMA';
UPDATE `strategy_info` SET `comments` = '衡量趋势方向强度，适合趋势交易确认' WHERE `strategy_code` = 'DMI';
UPDATE `strategy_info` SET `comments` = '基于ATR的趋势跟踪指标，信号明确易于执行' WHERE `strategy_code` = 'SUPERTREND';
UPDATE `strategy_info` SET `comments` = '衡量价格的旋转运动，识别趋势变化的早期信号' WHERE `strategy_code` = 'VORTEX';
UPDATE `strategy_info` SET `comments` = '衡量买卖压力差异，适合短期趋势判断' WHERE `strategy_code` = 'QSTICK';
UPDATE `strategy_info` SET `comments` = '三线系统识别趋势状态，适合趋势跟踪' WHERE `strategy_code` = 'WILLIAMS_ALLIGATOR';
UPDATE `strategy_info` SET `comments` = '高级数学变换，提供平滑的趋势线' WHERE `strategy_code` = 'HT_TRENDLINE';

-- 波动指标策略
UPDATE `strategy_info` SET `comments` = '经典波动性指标，适合震荡交易和突破策略' WHERE `strategy_code` = 'BOLLINGER';
UPDATE `strategy_info` SET `comments` = '基于ATR的通道指标，较布林带更稳定' WHERE `strategy_code` = 'KELTNER_CHANNEL';
UPDATE `strategy_info` SET `comments` = '衡量下跌风险，适合风险控制和资金管理' WHERE `strategy_code` = 'ULCER_INDEX';
UPDATE `strategy_info` SET `comments` = '衡量价格波动性，常用于止损和仓位管理' WHERE `strategy_code` = 'ATR';
UPDATE `strategy_info` SET `comments` = '趋势跟踪指标，提供动态止损位' WHERE `strategy_code` = 'PARABOLIC_SAR';
UPDATE `strategy_info` SET `comments` = 'ATR的归一化版本，便于不同价格水平的比较' WHERE `strategy_code` = 'NATR';
UPDATE `strategy_info` SET `comments` = '通过价格区间识别反转信号，适合波动性分析' WHERE `strategy_code` = 'MASS';
UPDATE `strategy_info` SET `comments` = '统计学指标，衡量价格偏离程度' WHERE `strategy_code` = 'STDDEV';
UPDATE `strategy_info` SET `comments` = '识别低波动后的突破机会，适合突破交易' WHERE `strategy_code` = 'SQUEEZE';
UPDATE `strategy_info` SET `comments` = '衡量布林带宽度变化，预测波动性变化' WHERE `strategy_code` = 'BBW';
UPDATE `strategy_info` SET `comments` = '年化波动率计算，用于风险评估' WHERE `strategy_code` = 'VOLATILITY';
UPDATE `strategy_info` SET `comments` = '基于最高最低价的通道，经典突破系统' WHERE `strategy_code` = 'DONCHIAN_CHANNELS';

-- 成交量指标策略
UPDATE `strategy_info` SET `comments` = '中国投资者熟悉的技术指标，敏感度高' WHERE `strategy_code` = 'KDJ';
UPDATE `strategy_info` SET `comments` = '经典成交量指标，确认价格趋势的可靠性' WHERE `strategy_code` = 'OBV';
UPDATE `strategy_info` SET `comments` = '通过成交量识别反转，适合成交量分析' WHERE `strategy_code` = 'MASS_INDEX';
UPDATE `strategy_info` SET `comments` = '累积分配线，跟踪资金流向，确认趋势' WHERE `strategy_code` = 'AD';
UPDATE `strategy_info` SET `comments` = 'AD线的震荡器版本，提供买卖信号' WHERE `strategy_code` = 'ADOSC';
UPDATE `strategy_info` SET `comments` = '关注成交量下降时的价格行为，适合机构行为分析' WHERE `strategy_code` = 'NVI';
UPDATE `strategy_info` SET `comments` = '关注成交量上升时的价格行为，适合散户行为分析' WHERE `strategy_code` = 'PVI';
UPDATE `strategy_info` SET `comments` = '成交量加权均线，反映真实的平均成本' WHERE `strategy_code` = 'VWMA';
UPDATE `strategy_info` SET `comments` = '成交量震荡器，识别成交量变化趋势' WHERE `strategy_code` = 'VOSC';
UPDATE `strategy_info` SET `comments` = '市场便利指数，衡量价格移动的容易程度' WHERE `strategy_code` = 'MARKETFI';

-- 蜡烛图形态策略
UPDATE `strategy_info` SET `comments` = '经典反转形态，市场犹豫不决的信号' WHERE `strategy_code` = 'DOJI';
UPDATE `strategy_info` SET `comments` = '强烈的看涨反转信号，适合底部抄底' WHERE `strategy_code` = 'BULLISH_ENGULFING';
UPDATE `strategy_info` SET `comments` = '强烈的看跌反转信号，适合顶部做空' WHERE `strategy_code` = 'BEARISH_ENGULFING';
UPDATE `strategy_info` SET `comments` = '温和的看涨信号，需要其他指标确认' WHERE `strategy_code` = 'BULLISH_HARAMI';
UPDATE `strategy_info` SET `comments` = '温和的看跌信号，需要其他指标确认' WHERE `strategy_code` = 'BEARISH_HARAMI';
UPDATE `strategy_info` SET `comments` = '强烈的看涨信号，连续三根阳线' WHERE `strategy_code` = 'THREE_WHITE_SOLDIERS';
UPDATE `strategy_info` SET `comments` = '强烈的看跌信号，连续三根阴线' WHERE `strategy_code` = 'THREE_BLACK_CROWS';
UPDATE `strategy_info` SET `comments` = '顶部反转信号，需要成交量确认' WHERE `strategy_code` = 'HANGING_MAN';
UPDATE `strategy_info` SET `comments` = '底部反转信号，下影线长表示支撑强劲' WHERE `strategy_code` = 'HAMMER';
UPDATE `strategy_info` SET `comments` = '底部反转信号，上影线长但仍显示买盘' WHERE `strategy_code` = 'INVERTED_HAMMER';
UPDATE `strategy_info` SET `comments` = '顶部反转信号，上影线长表示抛压重' WHERE `strategy_code` = 'SHOOTING_STAR';
UPDATE `strategy_info` SET `comments` = '三K线看涨反转组合，信号可靠' WHERE `strategy_code` = 'MORNING_STAR';
UPDATE `strategy_info` SET `comments` = '三K线看跌反转组合，信号可靠' WHERE `strategy_code` = 'EVENING_STAR';
UPDATE `strategy_info` SET `comments` = '看涨反转信号，第二根K线深入第一根' WHERE `strategy_code` = 'PIERCING';
UPDATE `strategy_info` SET `comments` = '看跌反转信号，乌云盖顶形态' WHERE `strategy_code` = 'DARK_CLOUD_COVER';
UPDATE `strategy_info` SET `comments` = '强势信号，没有影线表示单边行情' WHERE `strategy_code` = 'MARUBOZU';

-- 统计函数策略
UPDATE `strategy_info` SET `comments` = '衡量与大盘相关性，用于风险评估' WHERE `strategy_code` = 'BETA';
UPDATE `strategy_info` SET `comments` = '相关系数分析，适合配对交易' WHERE `strategy_code` = 'CORREL';
UPDATE `strategy_info` SET `comments` = '线性回归分析，识别价格趋势' WHERE `strategy_code` = 'LINEARREG';
UPDATE `strategy_info` SET `comments` = '回归线角度，衡量趋势强度' WHERE `strategy_code` = 'LINEARREG_ANGLE';
UPDATE `strategy_info` SET `comments` = '回归截距，预测价格水平' WHERE `strategy_code` = 'LINEARREG_INTERCEPT';
UPDATE `strategy_info` SET `comments` = '回归斜率，衡量价格变化速度' WHERE `strategy_code` = 'LINEARREG_SLOPE';
UPDATE `strategy_info` SET `comments` = '时间序列预测，短期价格预测' WHERE `strategy_code` = 'TSF';
UPDATE `strategy_info` SET `comments` = '方差分析，衡量价格离散程度' WHERE `strategy_code` = 'VAR';

-- 希尔伯特变换策略
UPDATE `strategy_info` SET `comments` = '识别主导市场周期，适合周期性交易' WHERE `strategy_code` = 'HT_DCPERIOD';
UPDATE `strategy_info` SET `comments` = '相位分析，识别周期位置' WHERE `strategy_code` = 'HT_DCPHASE';
UPDATE `strategy_info` SET `comments` = '相量分析，高级信号处理技术' WHERE `strategy_code` = 'HT_PHASOR';
UPDATE `strategy_info` SET `comments` = '正弦波分析，适合周期性市场' WHERE `strategy_code` = 'HT_SINE';
UPDATE `strategy_info` SET `comments` = '区分趋势和周期模式，指导策略选择' WHERE `strategy_code` = 'HT_TRENDMODE';
UPDATE `strategy_info` SET `comments` = 'MESA正弦波，高级周期分析工具' WHERE `strategy_code` = 'MSW';

-- 组合策略
UPDATE `strategy_info` SET `comments` = '经典量化策略，适合日内突破交易' WHERE `strategy_code` = 'DUAL_THRUST';
UPDATE `strategy_info` SET `comments` = '著名的趋势跟踪策略，适合长线交易' WHERE `strategy_code` = 'TURTLE_TRADING';
UPDATE `strategy_info` SET `comments` = '均值回归策略，适合震荡市场' WHERE `strategy_code` = 'MEAN_REVERSION';
UPDATE `strategy_info` SET `comments` = '趋势跟踪策略，适合单边市场' WHERE `strategy_code` = 'TREND_FOLLOWING';
UPDATE `strategy_info` SET `comments` = '突破策略，适合波动性扩张时期' WHERE `strategy_code` = 'BREAKOUT';
UPDATE `strategy_info` SET `comments` = '经典多头信号，适合牛市初期' WHERE `strategy_code` = 'GOLDEN_CROSS';
UPDATE `strategy_info` SET `comments` = '经典空头信号，适合熊市初期' WHERE `strategy_code` = 'DEATH_CROSS';
UPDATE `strategy_info` SET `comments` = '多指标组合，提高信号准确性' WHERE `strategy_code` = 'DUAL_MA_WITH_RSI';
UPDATE `strategy_info` SET `comments` = '趋势与震荡结合，适合复杂市场' WHERE `strategy_code` = 'MACD_WITH_BOLLINGER';
UPDATE `strategy_info` SET `comments` = '一目均衡表全套体系，提供完整交易框架' WHERE `strategy_code` = 'ICHIMOKU'; 