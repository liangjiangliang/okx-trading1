package com.okx.trading.repository;

import com.okx.trading.model.entity.RealTimeStrategyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 实时运行策略存储库接口
 */
@Repository
public interface RealTimeStrategyRepository extends JpaRepository<RealTimeStrategyEntity, Long> {

    /**
     * 根据策略代码查询实时策略
     *
     * @param strategyCode 策略代码
     * @return 实时策略信息
     */
    Optional<RealTimeStrategyEntity> findByStrategyCode(String strategyCode);

    /**
     * 根据状态查询实时策略
     *
     * @param status 运行状态
     * @return 实时策略列表
     */
    List<RealTimeStrategyEntity> findByStatusOrderByCreateTimeDesc(String status);


    /**
     * 查询指定时间范围内创建的实时策略
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 实时策略列表
     */
    List<RealTimeStrategyEntity> findByCreateTimeBetweenOrderByCreateTimeDesc(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 检查策略代码是否已存在
     *
     * @param strategyCode 策略代码
     * @return 是否存在
     */
    boolean existsByStrategyCode(String strategyCode);


    boolean existsByStrategyCodeAndSymbolAndInterval(String strategyCode, String symbol, String interval);

    /**
     * 查询需要自动启动的策略（程序启动时加载）
     * 查询有效且状态为RUNNING的策略
     *
     * @return 需要自动启动的策略列表
     */
    @Query("SELECT r FROM RealTimeStrategyEntity r WHERE r.status = 'RUNNING' ORDER BY r.createTime DESC")
    List<RealTimeStrategyEntity> findStrategiesToAutoStart();

    /**
     * 根据策略代码删除实时策略
     *
     * @param strategyCode 策略代码
     */
    void deleteByStrategyCode(String strategyCode);
}
