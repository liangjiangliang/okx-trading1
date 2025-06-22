-- 创建K线历史数据表
CREATE TABLE IF NOT EXISTS `candlestick_history` (
  `id` BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键ID',
  `symbol` VARCHAR(20) NOT NULL COMMENT '交易对，如BTC-USDT',
  `interval_val` VARCHAR(10) NOT NULL COMMENT 'K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M',
  `open_time` DATETIME NOT NULL COMMENT '开盘时间',
  `close_time` DATETIME COMMENT '收盘时间',
  `open` DECIMAL(30, 15) COMMENT '开盘价',
  `high` DECIMAL(30, 15) COMMENT '最高价',
  `low` DECIMAL(30, 15) COMMENT '最低价',
  `close` DECIMAL(30, 15) COMMENT '收盘价',
  `volume` DECIMAL(30, 15) COMMENT '成交量',
  `quote_volume` DECIMAL(30, 15) COMMENT '成交额',
  `trades` BIGINT COMMENT '成交笔数',
  `fetch_time` DATETIME COMMENT '数据获取时间',
  UNIQUE KEY `idx_symbol_interval_opentime` (`symbol`, `interval_val`, `open_time`),
  INDEX `idx_symbol_interval` (`symbol`, `interval_val`),
  INDEX `idx_open_time` (`open_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='历史K线数据';

-- 实时策略表
CREATE TABLE IF NOT EXISTS `real_time_strategy` (
  `id` BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键ID',
  `strategy_code` VARCHAR(50) NOT NULL UNIQUE COMMENT '实时策略唯一代码',
  `symbol` VARCHAR(20) NOT NULL COMMENT '交易对符号，如BTC-USDT',
  `interval_val` VARCHAR(10) NOT NULL COMMENT 'K线周期，如1m, 5m, 1h等',
  `start_time` DATETIME NOT NULL COMMENT '策略运行开始时间',
  `trade_amount` DOUBLE COMMENT '交易金额',
  `is_active` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否有效/启用',
  `status` VARCHAR(20) DEFAULT 'STOPPED' COMMENT '策略运行状态：RUNNING(运行中), STOPPED(已停止), COMPLETED(已完成), ERROR(错误)',
  `error_message` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '错误信息',
  `create_time` DATETIME NOT NULL COMMENT '创建时间',
  `update_time` DATETIME NOT NULL COMMENT '更新时间',
  INDEX `idx_strategy_code` (`strategy_code`),
  INDEX `idx_symbol` (`symbol`),
  INDEX `idx_status` (`status`),
  INDEX `idx_is_active` (`is_active`),
  INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实时运行策略表';

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


-- 策略对话记录表
CREATE TABLE IF NOT EXISTS `strategy_conversation` (
`id` BIGINT NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键ID',
`strategy_id` BIGINT NOT NULL COMMENT '关联的策略ID',
`user_input` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '用户输入的描述',
`ai_response` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'AI返回的完整响应',
`conversation_type` VARCHAR(20) NOT NULL COMMENT '对话类型：generate(生成) 或 update(更新)',
`create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
INDEX `idx_strategy_id` (`strategy_id`),
INDEX `idx_conversation_type` (`conversation_type`),
INDEX `idx_create_time` (`create_time`),
FOREIGN KEY (`strategy_id`) REFERENCES `strategy_info`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='策略对话记录表';
