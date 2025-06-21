-- 初始化数据库脚本
CREATE DATABASE IF NOT EXISTS okx_trading DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE okx_trading;

-- 创建K线数据表
CREATE TABLE IF NOT EXISTS `candlestick_history` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
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
  `trades` BIGINT(20) COMMENT '成交笔数',
  `fetch_time` DATETIME COMMENT '数据获取时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_symbol_interval_opentime` (`symbol`, `interval_val`, `open_time`),
  INDEX `idx_symbol_interval` (`symbol`, `interval_val`),
  INDEX `idx_opentime` (`open_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='K线历史数据';

-- 创建实时策略表
CREATE TABLE IF NOT EXISTS `real_time_strategy` (
  `id` BIGINT(20) NOT NULL AUTO_INCREMENT,
  `strategy_code` VARCHAR(50) NOT NULL COMMENT '实时策略唯一代码',
  `strategy_info_code` VARCHAR(50) NOT NULL COMMENT '关联的策略信息代码',
  `symbol` VARCHAR(20) NOT NULL COMMENT '交易对符号，如BTC-USDT',
  `interval_val` VARCHAR(10) NOT NULL COMMENT 'K线周期，如1m, 5m, 1h等',
  `start_time` DATETIME NOT NULL COMMENT '策略运行开始时间',
  `end_time` DATETIME COMMENT '策略运行结束时间',
  `is_active` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否有效/启用',
  `is_simulated` BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否为模拟交易',
  `order_type` VARCHAR(10) DEFAULT 'market' COMMENT '订单类型：market(市价), limit(限价)',
  `trade_amount` DOUBLE COMMENT '交易金额',
  `status` VARCHAR(20) DEFAULT 'STOPPED' COMMENT '策略运行状态：RUNNING(运行中), STOPPED(已停止), COMPLETED(已完成), ERROR(错误)',
  `description` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '策略描述',
  `error_message` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '错误信息',
  `create_time` DATETIME NOT NULL COMMENT '创建时间',
  `update_time` DATETIME NOT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_strategy_code` (`strategy_code`),
  INDEX `idx_strategy_info_code` (`strategy_info_code`),
  INDEX `idx_symbol` (`symbol`),
  INDEX `idx_status` (`status`),
  INDEX `idx_is_active` (`is_active`),
  INDEX `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='实时运行策略表';
