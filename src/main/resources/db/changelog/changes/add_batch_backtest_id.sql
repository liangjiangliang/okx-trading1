-- 添加批量回测ID字段到backtest_summary表
ALTER TABLE backtest_summary
ADD COLUMN batch_backtest_id VARCHAR(255) NULL COMMENT '批量回测ID，用于关联同一批次的所有回测';

-- 创建索引以提高根据batch_backtest_id查询的性能
CREATE INDEX idx_batch_backtest_id ON backtest_summary (batch_backtest_id); 