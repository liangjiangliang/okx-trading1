-- 创建K线历史数据表
CREATE TABLE IF NOT EXISTS `candlestick_history` (
  `id` VARCHAR(100) NOT NULL COMMENT '复合主键ID (symbol + interval + openTime)',
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
  PRIMARY KEY (`id`),
  INDEX `idx_symbol_interval` (`symbol`, `interval_val`),
  INDEX `idx_open_time` (`open_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='历史K线数据'; 