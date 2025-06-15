-- 为backtest_summary表添加新的风险评估指标列
ALTER TABLE backtest_summary
ADD COLUMN sortino_ratio DECIMAL(10, 4) COMMENT 'Sortino比率（只考虑下行风险的风险调整收益指标）',
ADD COLUMN calmar_ratio DECIMAL(10, 4) COMMENT 'Calmar比率（年化收益与最大回撤的比值）',
ADD COLUMN maximum_loss DECIMAL(20, 8) COMMENT '最大损失（单笔交易中的最大亏损金额）'; 