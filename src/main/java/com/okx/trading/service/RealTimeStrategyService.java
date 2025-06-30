package com.okx.trading.service;

import com.okx.trading.model.entity.RealTimeOrderEntity;
import com.okx.trading.model.entity.RealTimeStrategyEntity;
import com.okx.trading.strategy.RealTimeStrategyManager;
import org.ta4j.core.Strategy;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 实时运行策略服务接口
 */
public interface RealTimeStrategyService {

    /**
     * 获取所有实时策略
     *
     * @return 实时策略列表
     */
    List<RealTimeStrategyEntity> getAllRealTimeStrategies();

    /**
     * 获取所有有效的实时策略
     *
     * @return 有效的实时策略列表
     */
    List<RealTimeStrategyEntity> getActiveRealTimeStrategies();

    /**
     * 根据策略代码获取实时策略
     *
     * @param strategyCode 策略代码
     * @return 实时策略信息
     */
    Optional<RealTimeStrategyEntity> getRealTimeStrategyByCode(String strategyCode);

    /**
     * 根据ID获取实时策略
     *
     * @param id 策略ID
     * @return 实时策略信息
     */
    Optional<RealTimeStrategyEntity> getRealTimeStrategyById(Long id);

    /**
     * 根据策略信息代码获取有效的实时策略
     *
     * @param strategyCode 策略信息代码
     * @return 有效的实时策略列表
     */
    List<RealTimeStrategyEntity> getActiveRealTimeStrategiesByCode(String strategyCode);

    /**
     * 根据交易对获取有效的实时策略
     *
     * @param symbol 交易对符号
     * @return 有效的实时策略列表
     */
    List<RealTimeStrategyEntity> getActiveRealTimeStrategiesBySymbol(String symbol);

    /**
     * 根据状态获取实时策略
     *
     * @param status 运行状态
     * @return 实时策略列表
     */
    List<RealTimeStrategyEntity> getRealTimeStrategiesByStatus(String status);

    /**
     * 获取正在运行的实时策略
     *
     * @return 正在运行的实时策略列表
     */
    List<RealTimeStrategyEntity> getRunningRealTimeStrategies();

    /**
     * 根据交易对和状态获取实时策略
     *
     * @param symbol 交易对符号
     * @param status 运行状态
     * @return 实时策略列表
     */
    List<RealTimeStrategyEntity> getRealTimeStrategiesBySymbolAndStatus(String symbol, String status);

    /**
     * 获取指定时间范围内创建的实时策略
     *
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @return 实时策略列表
     */
    List<RealTimeStrategyEntity> getRealTimeStrategiesByTimeRange(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 保存实时策略
     *
     * @param realTimeStrategy 实时策略实体
     * @return 保存后的实时策略
     */
    RealTimeStrategyEntity saveRealTimeStrategy(RealTimeStrategyEntity realTimeStrategy);

    /**
     * 更新实时策略
     *
     * @param realTimeStrategy 实时策略实体
     * @return 更新后的实时策略
     */
    RealTimeStrategyEntity updateRealTimeStrategy(RealTimeStrategyEntity realTimeStrategy);

    /**
     * 启动实时策略
     *
     * @param strategyCode 策略代码
     * @return 是否启动成功
     */
    boolean updateStrategyStatus(Long id, String status);

    boolean startRealTimeStrategy(Long id);

    /**
     * 停止实时策略
     *
     * @param strategyCode 策略代码
     * @return 是否停止成功
     */
    boolean stopRealTimeStrategy(String strategyCode);


    /**
     * 更新策略状态和错误信息
     *
     * @param strategyCode 策略代码
     * @param status       新状态
     * @param errorMessage 错误信息
     * @return 是否更新成功
     */
    boolean updateStrategyStatusWithError(String strategyCode, String status, String errorMessage);

    /**
     * 激活策略
     *
     * @param strategyCode 策略代码
     * @return 是否激活成功
     */
    boolean activateStrategy(String strategyCode);

    /**
     * 停用策略
     *
     * @param strategyCode 策略代码
     * @return 是否停用成功
     */
    boolean deactivateStrategy(String strategyCode);

    /**
     * 删除实时策略
     *
     * @param strategyCode 策略代码
     * @return 是否删除成功
     */
    boolean deleteRealTimeStrategy(String strategyId);

    /**
     * 根据策略信息代码删除相关的实时策略
     *
     * @param strategyCode 策略信息代码
     * @return 删除的策略数量
     */
    int deleteRealTimeStrategiesByCode(String strategyCode);

    /**
     * 检查策略代码是否已存在
     *
     * @param strategyCode 策略代码
     * @return 是否存在
     */
    boolean existsByStrategyCode(String strategyCode);


    boolean existsByStrategyCodeAndSymbolAndInterval(String strategyCode, String symbol, String interval);

    /**
     * 检查是否存在运行中的策略
     *
     * @param strategyCode 策略信息代码
     * @param symbol       交易对符号
     * @return 是否存在运行中的策略
     */
    boolean hasRunningStrategy(String strategyCode, String symbol);

    /**
     * 获取需要自动启动的策略（程序启动时加载）
     *
     * @return 需要自动启动的策略列表
     */
    List<RealTimeStrategyEntity> getStrategiesToAutoStart();

    /**
     * 创建新的实时策略
     *
     * @param strategyCode 策略代码
     * @param strategyCode 策略信息代码
     * @param symbol       交易对符号
     * @param interval     K线周期
     * @param description  描述
     * @param isSimulated  是否模拟交易
     * @param orderType    订单类型
     * @param tradeAmount  交易金额
     * @return 创建的实时策略
     */
    RealTimeStrategyEntity createRealTimeStrategy(RealTimeStrategyEntity realTimeStrategy);

    /**
     * 更新策略交易信息
     *
     * @param strategyCode  策略代码
     * @param tradeType     交易类型（BUY/SELL）
     * @param tradePrice    交易价格
     * @param tradeQuantity 交易数量
     * @param profit        本次交易利润
     * @param fees          本次交易手续费
     * @return 是否更新成功
     */
    RealTimeStrategyEntity updateTradeInfo(RealTimeStrategyEntity state);

    Map<String, Object> executeRealTimeBacktest(RealTimeStrategyEntity realTimeStrategy);
}
