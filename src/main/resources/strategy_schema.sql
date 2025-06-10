-- 创建策略信息表
CREATE TABLE IF NOT EXISTS `strategy_info` (
  `id` BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键ID',
  `strategy_code` VARCHAR(50) NOT NULL COMMENT '策略代码，如SMA, MACD等',
  `strategy_name` VARCHAR(100) NOT NULL COMMENT '策略名称，如简单移动平均线策略',
  `description` TEXT COMMENT '策略描述',
  `params_desc` TEXT COMMENT '参数说明',
  `default_params` VARCHAR(255) COMMENT '默认参数值',
  `category` VARCHAR(50) COMMENT '策略分类，如移动平均线、震荡指标等',
  `comments` TEXT COMMENT '策略介绍，比如优缺点，适用场景，胜率等，回测，短线还是长线使用等信息',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY `idx_strategy_code` (`strategy_code`),
  INDEX `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交易策略信息表';
