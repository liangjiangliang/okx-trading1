package com.okx.trading.repository;

import com.okx.trading.model.entity.StrategyConversationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 策略对话记录Repository
 */
@Repository
public interface StrategyConversationRepository extends JpaRepository<StrategyConversationEntity, Long> {

    /**
     * 根据策略ID查询对话记录，按创建时间升序排列
     * @param strategyId 策略ID
     * @return 对话记录列表
     */
    @Query("SELECT c FROM StrategyConversationEntity c WHERE c.strategyId = :strategyId ORDER BY c.createTime ASC")
    List<StrategyConversationEntity> findByStrategyIdOrderByCreateTimeAsc(@Param("strategyId") Long strategyId);

    /**
     * 根据策略ID和对话类型查询对话记录
     * @param strategyId 策略ID
     * @param conversationType 对话类型
     * @return 对话记录列表
     */
    List<StrategyConversationEntity> findByStrategyIdAndConversationTypeOrderByCreateTimeAsc(Long strategyId, String conversationType);

    /**
     * 根据策略ID查询最新的对话记录
     * @param strategyId 策略ID
     * @param limit 限制数量
     * @return 对话记录列表
     */
    @Query("SELECT c FROM StrategyConversationEntity c WHERE c.strategyId = :strategyId ORDER BY c.createTime DESC")
    List<StrategyConversationEntity> findLatestByStrategyId(@Param("strategyId") Long strategyId);
}