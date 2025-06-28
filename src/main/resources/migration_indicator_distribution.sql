-- 创建指标分布表
CREATE TABLE IF NOT EXISTS `indicator_distribution` (
    `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    `indicator_name` VARCHAR(100) NOT NULL COMMENT '指标名称',
    `indicator_display_name` VARCHAR(100) DEFAULT NULL COMMENT '指标中文名称',
    `indicator_type` VARCHAR(20) NOT NULL COMMENT '指标类型: POSITIVE(越大越好), NEGATIVE(越小越好), NEUTRAL(中性)',
    `sample_count` INT NOT NULL COMMENT '样本总数',
    `min_value` DECIMAL(20,8) DEFAULT NULL COMMENT '最小值',
    `max_value` DECIMAL(20,8) DEFAULT NULL COMMENT '最大值',
    `avg_value` DECIMAL(20,8) DEFAULT NULL COMMENT '平均值',
    `p10` DECIMAL(20,8) DEFAULT NULL COMMENT '10%分位数',
    `p20` DECIMAL(20,8) DEFAULT NULL COMMENT '20%分位数',
    `p30` DECIMAL(20,8) DEFAULT NULL COMMENT '30%分位数',
    `p40` DECIMAL(20,8) DEFAULT NULL COMMENT '40%分位数',
    `p50` DECIMAL(20,8) DEFAULT NULL COMMENT '50%分位数(中位数)',
    `p60` DECIMAL(20,8) DEFAULT NULL COMMENT '60%分位数',
    `p70` DECIMAL(20,8) DEFAULT NULL COMMENT '70%分位数',
    `p80` DECIMAL(20,8) DEFAULT NULL COMMENT '80%分位数',
    `p90` DECIMAL(20,8) DEFAULT NULL COMMENT '90%分位数',
    `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `version` BIGINT NOT NULL COMMENT '版本号',
    `is_current` BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否为当前版本',
    
    -- 索引
    INDEX `idx_indicator_name` (`indicator_name`),
    INDEX `idx_indicator_current` (`indicator_name`, `is_current`),
    INDEX `idx_version` (`version`),
    INDEX `idx_is_current` (`is_current`),
    UNIQUE INDEX `idx_indicator_version` (`indicator_name`, `version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='指标分布表 - 存储各个指标的分位数分布信息'; 