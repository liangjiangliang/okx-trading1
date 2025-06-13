package com.okx.trading.service;

import com.okx.trading.model.entity.StrategyConversationEntity;

import java.util.List;

/**
 * 策略对话记录服务接口
 */
public interface StrategyConversationService {

    /**
     * 保存对话记录
     * @param conversation 对话记录
     * @return 保存后的对话记录
     */
    StrategyConversationEntity saveConversation(StrategyConversationEntity conversation);

    /**
     * 保存对话记录（便捷方法）
     * @param strategyId 策略ID
     * @param userInput 用户输入
     * @param aiResponse AI响应
     * @param conversationType 对话类型
     * @return 保存后的对话记录
     */
    StrategyConversationEntity saveConversation(String strategyId, String userInput, String aiResponse, String conversationType);

    /**
     * 保存对话记录（包含编译错误信息）
     * @param strategyId 策略ID
     * @param userInput 用户输入
     * @param aiResponse AI响应
     * @param conversationType 对话类型
     * @param compileError 编译错误信息
     * @return 保存后的对话记录
     */
    StrategyConversationEntity saveConversation(String strategyId, String userInput, String aiResponse, String conversationType, String compileError);

    /**
     * 根据策略ID获取对话历史
     * @param strategyId 策略ID
     * @return 对话记录列表
     */
    List<StrategyConversationEntity> getConversationHistory(Long strategyId);

    /**
     * 根据策略ID和对话类型获取对话历史
     * @param strategyId 策略ID
     * @param conversationType 对话类型
     * @return 对话记录列表
     */
    List<StrategyConversationEntity> getConversationHistory(Long strategyId, String conversationType);

    /**
     * 构建对话上下文，用于发送给AI
     * @param strategyId 策略ID
     * @return 格式化的对话上下文字符串
     */
    String buildConversationContext(Long strategyId);
}