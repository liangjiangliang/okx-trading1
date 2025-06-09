-- 为strategy_info表添加source_code字段
ALTER TABLE strategy_info ADD COLUMN source_code TEXT COMMENT '策略源代码，存储lambda函数的序列化字符串';