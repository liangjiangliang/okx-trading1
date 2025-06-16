-- 为backtest_summary表添加波动率列
ALTER TABLE backtest_summary
ADD COLUMN volatility DECIMAL(10, 4) COMMENT '波动率（收盘价标准差）'; 