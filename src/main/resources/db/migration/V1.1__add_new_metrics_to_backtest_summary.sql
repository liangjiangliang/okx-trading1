-- 为backtest_summary表添加新增指标字段
-- 执行时间：2024年
-- 描述：添加Omega、Alpha、Beta、Treynor比率、Ulcer指数、偏度、盈利因子等新指标字段

ALTER TABLE backtest_summary 
ADD COLUMN omega DECIMAL(10,4) COMMENT 'Omega比率（收益与风险的比值）',
ADD COLUMN alpha DECIMAL(10,4) COMMENT 'Alpha值（超额收益）',
ADD COLUMN beta DECIMAL(10,4) COMMENT 'Beta值（系统性风险）',
ADD COLUMN treynor_ratio DECIMAL(10,4) COMMENT 'Treynor比率（风险调整收益指标）',
ADD COLUMN ulcer_index DECIMAL(10,4) COMMENT 'Ulcer指数（回撤深度和持续时间的综合指标）',
ADD COLUMN skewness DECIMAL(10,4) COMMENT '偏度（收益分布的偏斜程度）',
ADD COLUMN profit_factor DECIMAL(10,4) COMMENT '盈利因子（总盈利/总亏损）';

-- 为新字段创建索引（可选，根据查询需求）
CREATE INDEX idx_backtest_summary_omega ON backtest_summary(omega);
CREATE INDEX idx_backtest_summary_alpha ON backtest_summary(alpha);
CREATE INDEX idx_backtest_summary_beta ON backtest_summary(beta);
CREATE INDEX idx_backtest_summary_profit_factor ON backtest_summary(profit_factor);