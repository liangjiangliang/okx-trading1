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