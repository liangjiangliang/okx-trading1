-- 数据库迁移脚本：添加新的风险指标字段到 backtest_summary 表
-- 创建时间: 2024-12-28
-- 描述: 新增高级风险指标字段以支持更全面的回测分析

USE okx_trading;

-- 添加新的风险指标字段
ALTER TABLE `backtest_summary` 
ADD COLUMN `kurtosis` decimal(10, 4) DEFAULT NULL COMMENT '峰度（收益率分布的尾部风险）',
ADD COLUMN `cvar` decimal(10, 4) DEFAULT NULL COMMENT '条件风险价值（极端损失的期望值）',
ADD COLUMN `var95` decimal(10, 4) DEFAULT NULL COMMENT '95%置信度下的风险价值',
ADD COLUMN `var99` decimal(10, 4) DEFAULT NULL COMMENT '99%置信度下的风险价值',
ADD COLUMN `information_ratio` decimal(10, 4) DEFAULT NULL COMMENT '信息比率（超额收益相对于跟踪误差的比率）',
ADD COLUMN `tracking_error` decimal(10, 4) DEFAULT NULL COMMENT '跟踪误差（策略与基准收益率的标准差）',
ADD COLUMN `sterling_ratio` decimal(10, 4) DEFAULT NULL COMMENT 'Sterling比率（年化收益与平均最大回撤的比率）',
ADD COLUMN `burke_ratio` decimal(10, 4) DEFAULT NULL COMMENT 'Burke比率（年化收益与平方根回撤的比率）',
ADD COLUMN `modified_sharpe_ratio` decimal(10, 4) DEFAULT NULL COMMENT '修正夏普比率（考虑偏度和峰度的夏普比率）',
ADD COLUMN `downside_deviation` decimal(10, 4) DEFAULT NULL COMMENT '下行偏差（只考虑负收益的标准差）',
ADD COLUMN `uptrend_capture` decimal(10, 4) DEFAULT NULL COMMENT '上涨捕获率（基准上涨时策略的表现）',
ADD COLUMN `downtrend_capture` decimal(10, 4) DEFAULT NULL COMMENT '下跌捕获率（基准下跌时策略的表现）',
ADD COLUMN `max_drawdown_duration` decimal(10, 2) DEFAULT NULL COMMENT '最大回撤持续期（从峰值到恢复的最长时间）',
ADD COLUMN `pain_index` decimal(10, 4) DEFAULT NULL COMMENT '痛苦指数（回撤深度与持续时间的综合指标）',
ADD COLUMN `risk_adjusted_return` decimal(10, 4) DEFAULT NULL COMMENT '风险调整收益（综合多种风险因素的收益评估）';

-- 添加索引以优化查询性能
ALTER TABLE `backtest_summary` 
ADD INDEX `idx_backtest_summary_comprehensive_score` (`comprehensive_score`),
ADD INDEX `idx_backtest_summary_information_ratio` (`information_ratio`),
ADD INDEX `idx_backtest_summary_modified_sharpe_ratio` (`modified_sharpe_ratio`),
ADD INDEX `idx_backtest_summary_pain_index` (`pain_index`);

-- 创建复合索引用于综合查询
ALTER TABLE `backtest_summary` 
ADD INDEX `idx_comprehensive_analysis` (`comprehensive_score`, `total_return`, `sharpe_ratio`, `max_drawdown`);

-- 验证表结构
SELECT COLUMN_NAME, DATA_TYPE, IS_NULLABLE, COLUMN_DEFAULT, COLUMN_COMMENT 
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'okx_trading' 
  AND TABLE_NAME = 'backtest_summary' 
  AND COLUMN_NAME IN ('kurtosis', 'cvar', 'var95', 'var99', 'information_ratio', 'comprehensive_score')
ORDER BY ORDINAL_POSITION;

-- 输出迁移完成信息
SELECT CONCAT('✅ 数据库迁移完成：已成功添加 ', COUNT(*), ' 个新的风险指标字段') AS migration_status
FROM INFORMATION_SCHEMA.COLUMNS 
WHERE TABLE_SCHEMA = 'okx_trading' 
  AND TABLE_NAME = 'backtest_summary' 
  AND COLUMN_NAME IN ('kurtosis', 'cvar', 'var95', 'var99', 'information_ratio', 'tracking_error', 
                      'sterling_ratio', 'burke_ratio', 'modified_sharpe_ratio', 'downside_deviation',
                      'uptrend_capture', 'downtrend_capture', 'max_drawdown_duration', 'pain_index', 'risk_adjusted_return'); 