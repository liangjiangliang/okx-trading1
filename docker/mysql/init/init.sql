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