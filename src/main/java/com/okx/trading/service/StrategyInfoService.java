package com.okx.trading.service;

import com.okx.trading.model.entity.StrategyInfoEntity;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 策略信息服务接口
 */
public interface StrategyInfoService {

    /**
     * 获取所有策略信息
     *
     * @return 策略信息列表
     */
    List<StrategyInfoEntity> getAllStrategies();

    /**
     * 获取所有策略信息（别名方法）
     *
     * @return 策略信息列表
     */
    default List<StrategyInfoEntity> findAll() {
        return getAllStrategies();
    }

    /**
     * 根据策略代码获取策略信息
     *
     * @param strategyCode 策略代码
     * @return 策略信息
     */
    Optional<StrategyInfoEntity> getStrategyByCode(String strategyCode);


    Optional<StrategyInfoEntity> getStrategyById(Long id);
    /**
     * 根据策略分类获取策略信息列表
     *
     * @param category 策略分类
     * @return 策略信息列表
     */
    List<StrategyInfoEntity> getStrategiesByCategory(String category);

    /**
     * 保存策略信息
     *
     * @param strategyInfo 策略信息
     * @return 保存后的策略信息
     */
    StrategyInfoEntity saveStrategy(StrategyInfoEntity strategyInfo);

    /**
     * 批量保存策略信息
     *
     * @param strategyInfoList 策略信息列表
     * @return 保存后的策略信息列表
     */
    List<StrategyInfoEntity> saveAllStrategies(List<StrategyInfoEntity> strategyInfoList);

    /**
     * 删除策略信息
     *
     * @param id 策略ID
     */
    void deleteStrategy(Long id);

    /**
     * 根据策略代码删除策略信息
     *
     * @param strategyCode 策略代码
     */
    void deleteStrategyByCode(String strategyCode);

    /**
     * 获取策略的默认参数
     *
     * @param strategyCode 策略代码
     * @return 默认参数字符串，如果策略不存在则返回空字符串
     */
    String getDefaultParams(String strategyCode);

    /**
     * 获取所有策略的信息，以Map形式返回
     * 键为策略代码，值为包含策略名称、描述和参数说明的Map
     *
     * @return 策略信息Map
     */
    Map<String, Map<String, String>> getStrategiesInfo();

    /**
     * 检查策略代码是否已存在
     *
     * @param strategyCode 策略代码
     * @return 如果存在返回true，否则返回false
     */
    boolean existsByStrategyCode(String strategyCode);
}
