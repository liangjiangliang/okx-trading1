package com.okx.trading.repository;

import com.okx.trading.model.entity.StrategyInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 策略信息存储库接口
 */
@Repository
public interface StrategyInfoRepository extends JpaRepository<StrategyInfoEntity, Long> {

    /**
     * 根据策略代码查询策略信息
     *
     * @param strategyCode 策略代码
     * @return 策略信息
     */
    Optional<StrategyInfoEntity> findByStrategyCode(String strategyCode);


    Optional<StrategyInfoEntity> findById(Long id);

    /**
     * 根据策略分类查询策略信息列表
     *
     * @param category 策略分类
     * @return 策略信息列表
     */
    List<StrategyInfoEntity> findByCategory(String category);

    /**
     * 根据策略分类查询策略信息列表，按策略名称排序
     *
     * @param category 策略分类
     * @return 策略信息列表
     */
    List<StrategyInfoEntity> findByCategoryOrderByStrategyNameAsc(String category);

    /**
     * 查询所有策略信息，按策略代码排序
     *
     * @return 策略信息列表
     */
    List<StrategyInfoEntity> findAllByOrderByStrategyCodeAsc();

    /**
     * 根据策略名称模糊查询策略信息列表
     *
     * @param strategyName 策略名称
     * @return 策略信息列表
     */
    List<StrategyInfoEntity> findByStrategyNameContaining(String strategyName);

    /**
     * 根据策略代码删除策略信息
     *
     * @param strategyCode 策略代码
     */
    void deleteByStrategyCode(String strategyCode);

    /**
     * 检查策略代码是否已存在
     *
     * @param strategyCode 策略代码
     * @return 如果存在返回true，否则返回false
     */
    boolean existsByStrategyCode(String strategyCode);
}
