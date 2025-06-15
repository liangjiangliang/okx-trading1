-- 添加年化收益率字段到回测汇总表
ALTER TABLE backtest_summary
ADD COLUMN annualized_return DECIMAL(10,4) COMMENT '年化收益率（百分比）';

-- 更新现有记录的年化收益率
-- 注意：这里只是一个简单的更新，实际计算需要考虑时间跨度
UPDATE backtest_summary
SET annualized_return = total_return
WHERE annualized_return IS NULL; 