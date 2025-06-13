package com.okx.trading.service.impl;

import com.okx.trading.model.entity.StrategyConversationEntity;
import com.okx.trading.repository.StrategyConversationRepository;
import com.okx.trading.service.StrategyConversationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 策略对话记录服务实现类
 */
@Slf4j
@Service
public class StrategyConversationServiceImpl implements StrategyConversationService {

    @Autowired
    private StrategyConversationRepository strategyConversationRepository;

    @Override
    public StrategyConversationEntity saveConversation(StrategyConversationEntity conversation) {
        log.info("保存策略对话记录，策略ID: {}, 对话类型: {}", conversation.getStrategyId(), conversation.getConversationType());
        return strategyConversationRepository.save(conversation);
    }

    @Override
    public StrategyConversationEntity saveConversation(String strategyId, String userInput, String aiResponse, String conversationType) {
        StrategyConversationEntity conversation = new StrategyConversationEntity();

        // 如果strategyId不为空，尝试转换为Long类型
        if (strategyId != null && !strategyId.trim().isEmpty()) {
            try {
                // 首先尝试直接转换为Long（如果是数字ID）
                conversation.setStrategyId(Long.parseLong(strategyId));
            } catch (NumberFormatException e) {
                // 如果不是数字，可能是策略代码，需要查询对应的ID
                // 这里暂时设置为null，后续可以根据需要添加查询逻辑
                conversation.setStrategyId(null);
            }
        }

        conversation.setUserInput(userInput);
        conversation.setAiResponse(aiResponse);
        conversation.setConversationType(conversationType);
        conversation.setCreateTime(LocalDateTime.now());

        return strategyConversationRepository.save(conversation);
    }

    @Override
    public StrategyConversationEntity saveConversation(String strategyId, String userInput, String aiResponse, String conversationType, String compileError) {
        StrategyConversationEntity conversation = new StrategyConversationEntity();

        // 如果strategyId不为空，尝试转换为Long类型
        if (strategyId != null && !strategyId.trim().isEmpty()) {
            try {
                // 首先尝试直接转换为Long（如果是数字ID）
                conversation.setStrategyId(Long.parseLong(strategyId));
            } catch (NumberFormatException e) {
                // 如果不是数字，可能是策略代码，需要查询对应的ID
                // 这里暂时设置为null，后续可以根据需要添加查询逻辑
                conversation.setStrategyId(null);
            }
        }

        conversation.setUserInput(userInput);
        conversation.setAiResponse(aiResponse);
        conversation.setConversationType(conversationType);
        conversation.setCompileError(compileError);
        conversation.setCreateTime(LocalDateTime.now());

        return strategyConversationRepository.save(conversation);
    }

    @Override
    public List<StrategyConversationEntity> getConversationHistory(Long strategyId) {
        log.info("获取策略对话历史，策略ID: {}", strategyId);
        return strategyConversationRepository.findByStrategyIdOrderByCreateTimeAsc(strategyId);
    }

    @Override
    public List<StrategyConversationEntity> getConversationHistory(Long strategyId, String conversationType) {
        log.info("获取策略对话历史，策略ID: {}, 对话类型: {}", strategyId, conversationType);
        return strategyConversationRepository.findByStrategyIdAndConversationTypeOrderByCreateTimeAsc(strategyId, conversationType);
    }

    @Override
    public String buildConversationContext(Long strategyId) {
        log.info("构建策略对话上下文，策略ID: {}", strategyId);

        List<StrategyConversationEntity> conversations = getConversationHistory(strategyId);
        if (conversations.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("\n\n=== 历史对话记录 ===\n");

        for (int i = 0; i < conversations.size(); i++) {
            StrategyConversationEntity conv = conversations.get(i);
            context.append(String.format("\n第%d轮对话 (%s):\n", i + 1, conv.getConversationType()));
            context.append("用户输入: ").append(conv.getUserInput()).append("\n");

            // 只显示AI响应的关键信息，避免过长
            String aiResponse = conv.getAiResponse();
            context.append("AI响应: ").append(aiResponse).append("\n");
        }

        context.append("\n=== 历史对话记录结束 ===\n\n");

        log.info("构建对话上下文完成，策略ID: {}, 对话记录数: {}", strategyId, conversations.size());
        return context.toString();
    }
}
