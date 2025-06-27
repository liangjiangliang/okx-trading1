CREATE DATABASE IF NOT EXISTS `okx_trading`;

-- 回测结果表
CREATE TABLE IF NOT EXISTS `backtest_summary`
(`id`                  bigint         NOT NULL AUTO_INCREMENT,
 `average_profit`      decimal(10, 4) DEFAULT NULL COMMENT '平均利润',
 `backtest_id`         varchar(255)   NOT NULL,
 `create_time`         datetime       DEFAULT NULL,
 `end_time`            datetime       DEFAULT NULL,
 `final_amount`        decimal(20, 8) DEFAULT NULL COMMENT '最终金额',
 `initial_amount`      decimal(20, 8) NOT NULL COMMENT '初始金额',
 `interval_val`        varchar(255)   NOT NULL COMMENT 'K线间隔',
 `max_drawdown`        decimal(10, 4) DEFAULT NULL COMMENT '最大回撤',
 `number_of_trades`    int            DEFAULT NULL COMMENT '交易次数',
 `profitable_trades`   int            DEFAULT NULL COMMENT '盈利交易次数',
 `sharpe_ratio`        decimal(10, 4) DEFAULT NULL COMMENT '夏普比率（收益与风险的比值）',
 `start_time`          datetime       DEFAULT NULL,
 `strategy_name`       varchar(255)   NOT NULL,
 `strategy_code`       varchar(150)   DEFAULT NULL COMMENT '策略代码',
 `strategy_params`     varchar(255)   DEFAULT NULL COMMENT '策略参数',
 `symbol`              varchar(255)   NOT NULL COMMENT '交易对',
 `total_profit`        decimal(20, 8) DEFAULT NULL COMMENT '总利润',
 `total_return`        decimal(10, 4) DEFAULT NULL COMMENT '总收益率',
 `unprofitable_trades` int            DEFAULT NULL COMMENT '非盈利交易次数',
 `win_rate`            decimal(10, 4) DEFAULT NULL COMMENT '胜率',
 `total_fee`           decimal(20, 8) DEFAULT NULL COMMENT '总费用',
 `batch_backtest_id`   varchar(255)   DEFAULT NULL COMMENT '批次回测ID',
 `annualized_return`   decimal(10, 4) DEFAULT NULL COMMENT '年化收益率',
 `calmar_ratio`        decimal(10, 4) DEFAULT NULL COMMENT 'Calmar比率（收益与风险的比值）',
 `maximum_loss`        decimal(20, 8) DEFAULT NULL COMMENT '最大亏损',
 `sortino_ratio`       decimal(10, 4) DEFAULT NULL COMMENT 'Sortino比率（收益与风险的比值）',
 `volatility`          decimal(10, 4) DEFAULT NULL COMMENT '波动率（收益波动程度）',
 `omega`               decimal(10, 4) DEFAULT NULL COMMENT 'Omega比率（收益与风险的比值）',
 `alpha`               decimal(10, 4) DEFAULT NULL COMMENT 'Alpha值（超额收益）',
 `beta`                decimal(10, 4) DEFAULT NULL COMMENT 'Beta值（系统性风险）',
 `treynor_ratio`       decimal(10, 4) DEFAULT NULL COMMENT 'Treynor比率（风险调整收益指标）',
 `ulcer_index`         decimal(10, 4) DEFAULT NULL COMMENT 'Ulcer指数（回撤深度和持续时间的综合指标）',
 `skewness`            decimal(10, 4) DEFAULT NULL COMMENT '偏度（收益分布的偏斜程度）',
 `profit_factor`       decimal(10, 4) DEFAULT NULL COMMENT '盈利因子（总盈利/总亏损）',
 `comprehensive_score` decimal(3, 2)  DEFAULT NULL COMMENT '综合评分 (0-10分)',
 `kurtosis`            decimal(10, 4) DEFAULT NULL COMMENT '峰度（收益率分布的尾部风险）',
 `cvar`                decimal(10, 4) DEFAULT NULL COMMENT '条件风险价值（极端损失的期望值）',
 `var95`               decimal(10, 4) DEFAULT NULL COMMENT '95%置信度下的风险价值',
 `var99`               decimal(10, 4) DEFAULT NULL COMMENT '99%置信度下的风险价值',
 `information_ratio`   decimal(10, 4) DEFAULT NULL COMMENT '信息比率（超额收益相对于跟踪误差的比率）',
 `tracking_error`      decimal(10, 4) DEFAULT NULL COMMENT '跟踪误差（策略与基准收益率的标准差）',
 `sterling_ratio`      decimal(10, 4) DEFAULT NULL COMMENT 'Sterling比率（年化收益与平均最大回撤的比率）',
 `burke_ratio`         decimal(10, 4) DEFAULT NULL COMMENT 'Burke比率（年化收益与平方根回撤的比率）',
 `modified_sharpe_ratio` decimal(10, 4) DEFAULT NULL COMMENT '修正夏普比率（考虑偏度和峰度的夏普比率）',
 `downside_deviation`  decimal(10, 4) DEFAULT NULL COMMENT '下行偏差（只考虑负收益的标准差）',
 `uptrend_capture`     decimal(10, 4) DEFAULT NULL COMMENT '上涨捕获率（基准上涨时策略的表现）',
 `downtrend_capture`   decimal(10, 4) DEFAULT NULL COMMENT '下跌捕获率（基准下跌时策略的表现）',
 `max_drawdown_duration` decimal(10, 2) DEFAULT NULL COMMENT '最大回撤持续期（从峰值到恢复的最长时间）',
 `pain_index`          decimal(10, 4) DEFAULT NULL COMMENT '痛苦指数（回撤深度与持续时间的综合指标）',
 `risk_adjusted_return` decimal(10, 4) DEFAULT NULL COMMENT '风险调整收益（综合多种风险因素的收益评估）',
 PRIMARY KEY (`id`),
 UNIQUE KEY `UK_pejcjjk0mdb200ay5mffbomkt` (`backtest_id`),
 KEY `idx_backtest_summary_omega` (`omega`),
 KEY `idx_backtest_summary_alpha` (`alpha`),
 KEY `idx_backtest_summary_beta` (`beta`),
 KEY `idx_backtest_summary_profit_factor` (`profit_factor`),
 KEY `idx_backtest_summary_comprehensive_score` (`comprehensive_score`),
 KEY `idx_backtest_summary_information_ratio` (`information_ratio`),
 KEY `idx_backtest_summary_modified_sharpe_ratio` (`modified_sharpe_ratio`),
 KEY `idx_backtest_summary_pain_index` (`pain_index`)) ENGINE = InnoDB
                                                             AUTO_INCREMENT = 236
                                                             DEFAULT CHARSET = utf8mb4
                                                             COLLATE = utf8mb4_0900_ai_ci;

-- 交易记录表
CREATE TABLE IF NOT EXISTS `backtest_trade`
(id                          bigint       NOT NULL AUTO_INCREMENT COMMENT '自增主键ID',
 `backtest_id`               varchar(255) NOT NULL COMMENT '关联的回测任务ID',
 closed                      bit(1)         DEFAULT NULL COMMENT '交易是否已关闭，0表示未关闭，1表示已关闭',
 `create_time`               datetime       DEFAULT NULL COMMENT '记录创建时间',
 `entry_amount`              decimal(20, 8) DEFAULT NULL COMMENT '建仓金额',
 `entry_position_percentage` decimal(10, 4) DEFAULT NULL COMMENT '建仓仓位百分比',
 `entry_price`               decimal(20, 8) DEFAULT NULL COMMENT '建仓价格',
 `entry_time`                datetime       DEFAULT NULL COMMENT '建仓时间',
 `exit_amount`               decimal(20, 8) DEFAULT NULL COMMENT '平仓金额',
 `exit_price`                decimal(20, 8) DEFAULT NULL COMMENT '平仓价格',
 `exit_time`                 datetime       DEFAULT NULL COMMENT '平仓时间',
 fee                         decimal(20, 8) DEFAULT NULL COMMENT '手续费',
 `trade_index`               int            DEFAULT NULL COMMENT '交易索引序号',
 `max_drawdown`              decimal(10, 4) DEFAULT NULL COMMENT '最大回撤',
 `max_loss`                  decimal(10, 4) DEFAULT NULL COMMENT '最大亏损',
 profit                      decimal(20, 8) DEFAULT NULL COMMENT '利润',
 `profit_percentage`         decimal(10, 4) DEFAULT NULL COMMENT '利润率百分比',
 remark                      varchar(500)   DEFAULT NULL COMMENT '备注信息',
 `strategy_name`             varchar(255) NOT NULL COMMENT '策略名称',
 `strategy_code`             varchar(150)   DEFAULT NULL COMMENT '策略代码',
 `strategy_params`           varchar(255)   DEFAULT NULL COMMENT '策略参数',
 symbol                      varchar(255) NOT NULL COMMENT '交易对符号，如BTC-USDT',
 `total_assets`              decimal(20, 8) DEFAULT NULL COMMENT '总资产',
 `trade_type`                varchar(255) NOT NULL COMMENT '交易类型，例如做多（LONG）或做空（SHORT）',
 volume                      decimal(20, 8) DEFAULT NULL COMMENT '成交量/交易数量',
 PRIMARY KEY (id)) ENGINE = InnoDB
                   AUTO_INCREMENT = 3795
                   DEFAULT CHARSET = utf8mb4
                   COLLATE = utf8mb4_0900_ai_ci;

-- 创建K线历史数据表
CREATE TABLE IF NOT EXISTS `candlestick_history`
(`id`           BIGINT      NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键ID',
 `symbol`       VARCHAR(20) NOT NULL COMMENT '交易对，如BTC-USDT',
    `interval_val` VARCHAR(10) NOT NULL COMMENT 'K线间隔，如1m, 5m, 15m, 30m, 1H, 2H, 4H, 6H, 12H, 1D, 1W, 1M',
    `open_time`    DATETIME    NOT NULL COMMENT '开盘时间',
    `close_time`   DATETIME COMMENT '收盘时间',
    `open`         DECIMAL(30, 15) COMMENT '开盘价',
    `high`         DECIMAL(30, 15) COMMENT '最高价',
    `low`          DECIMAL(30, 15) COMMENT '最低价',
    `close`        DECIMAL(30, 15) COMMENT '收盘价',
    `volume`       DECIMAL(30, 15) COMMENT '成交量',
    `quote_volume` DECIMAL(30, 15) COMMENT '成交额',
    `trades`       BIGINT COMMENT '成交笔数',
    `fetch_time`   DATETIME COMMENT '数据获取时间',
    UNIQUE KEY `idx_symbol_interval_opentime` (`symbol`, `interval_val`, `open_time`),
    INDEX `idx_symbol_interval` (`symbol`, `interval_val`),
    INDEX `idx_open_time` (`open_time`)) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='历史K线数据';

-- 创建实时订单表
CREATE TABLE IF NOT EXISTS `real_time_orders`
(`id`                bigint      NOT NULL AUTO_INCREMENT COMMENT '自增主键ID',
 `amount`            decimal(20, 8) DEFAULT NULL COMMENT '订单总金额',
 `client_order_id` varchar(50)    DEFAULT NULL COMMENT '客户端订单ID',
 `create_time`     datetime(6) NOT NULL COMMENT '订单创建时间',
 `executed_amount` decimal(20, 8) DEFAULT NULL COMMENT '已执行金额',
 `executed_qty`    decimal(20, 8) DEFAULT NULL COMMENT '已执行数量',
 `fee`               decimal(20, 8) DEFAULT NULL COMMENT '交易手续费',
 `fee_currency`    varchar(10)    DEFAULT NULL COMMENT '手续费币种',
 `order_id`        varchar(50)    DEFAULT NULL COMMENT '交易所订单ID',
 `order_type`      varchar(10) NOT NULL COMMENT '订单类型，如LIMIT(限价单), MARKET(市价单)',
 `price`             decimal(20, 8) DEFAULT NULL COMMENT '订单价格',
 `quantity`          decimal(20, 8) DEFAULT NULL COMMENT '订单数量',
 `remark`           varchar(500)   DEFAULT NULL COMMENT '订单备注信息',
 `side`              varchar(10) NOT NULL COMMENT '交易方向：BUY(买入)/LONG，SELL/SHORT',
 `signal_price`    decimal(20, 8) DEFAULT NULL COMMENT '触发订单的信号价格',
 `signal_type`     varchar(20)    DEFAULT NULL COMMENT '触发订单的信号类型',
 `status`            varchar(20)    DEFAULT NULL COMMENT '订单状态：NEW(新订单), PARTIALLY_FILLED(部分成交), FILLED(完全成交), CANCELED(已取消), REJECTED(被拒)',
 `strategy_code`   varchar(50) NOT NULL COMMENT '关联策略代码',
 `symbol`            varchar(20) NOT NULL COMMENT '交易对符号，如BTC-USDT',
 `update_time`     datetime(6)    DEFAULT NULL COMMENT '订单最后更新时间',
 PRIMARY KEY (id)) ENGINE = InnoDB
                   DEFAULT CHARSET = utf8mb4
                   COLLATE = utf8mb4_0900_ai_ci;

-- 实时策略表
CREATE TABLE IF NOT EXISTS `real_time_strategy`
(`id`            BIGINT      NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键ID',
 `strategy_code` VARCHAR(50) NOT NULL UNIQUE COMMENT '实时策略唯一代码',
    `symbol`        VARCHAR(20) NOT NULL COMMENT '交易对符号，如BTC-USDT',
    `interval_val`  VARCHAR(10) NOT NULL COMMENT 'K线周期，如1m, 5m, 1h等',
    `start_time`    DATETIME    NOT NULL COMMENT '策略运行开始时间',
    `trade_amount`  DOUBLE COMMENT '交易金额',
    `is_active`     BOOLEAN     NOT NULL DEFAULT TRUE COMMENT '是否有效/启用',
    `status`        VARCHAR(20)          DEFAULT 'STOPPED' COMMENT '策略运行状态：RUNNING(运行中), STOPPED(已停止), COMPLETED(已完成), ERROR(错误)',
    `error_message` TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '错误信息',
    `create_time`   DATETIME    NOT NULL COMMENT '创建时间',
    `update_time`   DATETIME    NOT NULL COMMENT '更新时间',
    INDEX `idx_strategy_code` (`strategy_code`),
    INDEX `idx_symbol` (`symbol`),
    INDEX `idx_status` (`status`),
    INDEX `idx_is_active` (`is_active`),
    INDEX `idx_create_time` (`create_time`)) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='实时运行策略表';


-- 创建策略信息表
CREATE TABLE IF NOT EXISTS `strategy_info`
(`id`             BIGINT       NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键ID',
 `strategy_code`  VARCHAR(50)  NOT NULL COMMENT '策略代码，如SMA, MACD等',
    `strategy_name`  VARCHAR(100) NOT NULL COMMENT '策略名称，如简单移动平均线策略',
    `description`    TEXT COMMENT '策略描述',
    `params_desc`    TEXT COMMENT '参数说明',
    `default_params` VARCHAR(255) COMMENT '默认参数值',
    `category`       VARCHAR(50) COMMENT '策略分类，如移动平均线、震荡指标等',
    `comments`       TEXT COMMENT '策略介绍，比如优缺点，适用场景，胜率等，回测，短线还是长线使用等信息',
    `source_code`    TEXT COMMENT '策略源代码，存储lambda函数的序列化字符串',
    `load_error`     TEXT COMMENT '策略加载错误信息',
    `create_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY `idx_strategy_code` (`strategy_code`),
    INDEX `idx_category` (`category`)) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='交易策略信息表';


-- 策略对话记录表
CREATE TABLE IF NOT EXISTS `strategy_conversation`
(`id`                BIGINT      NOT NULL PRIMARY KEY AUTO_INCREMENT COMMENT '自增主键ID',
 `strategy_id`       BIGINT      NOT NULL COMMENT '关联的策略ID',
 `user_input`        TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '用户输入的描述',
 `ai_response`       TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT 'AI返回的完整响应',
 `conversation_type` VARCHAR(20) NOT NULL COMMENT '对话类型：generate(生成) 或 update(更新)',
    `create_time`       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX `idx_strategy_id` (`strategy_id`),
    INDEX `idx_conversation_type` (`conversation_type`),
    INDEX `idx_create_time` (`create_time`),
    FOREIGN KEY (`strategy_id`)
    REFERENCES `strategy_info` (`id`)
    ON DELETE CASCADE) ENGINE = InnoDB
    DEFAULT CHARSET = utf8mb4 COMMENT ='策略对话记录表';
