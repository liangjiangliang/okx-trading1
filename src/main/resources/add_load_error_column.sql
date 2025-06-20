-- 为strategy_info表添加load_error字段
ALTER TABLE strategy_info ADD COLUMN load_error TEXT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '策略加载错误信息';

-- 查看表结构确认字段已添加
DESC strategy_info;