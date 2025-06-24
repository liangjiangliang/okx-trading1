package com.okx.trading.service.impl;

import com.okx.trading.model.entity.RealTimeStrategyEntity;
import com.okx.trading.repository.RealTimeStrategyRepository;
import com.okx.trading.service.RealTimeStrategyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ta4j.core.Strategy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 实时运行策略服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealTimeStrategyServiceImpl implements RealTimeStrategyService {

    private final RealTimeStrategyRepository realTimeStrategyRepository;
    private final RealTimeStrategyManager realTimeStrategyManager;
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    @Override
    public List<RealTimeStrategyEntity> getAllRealTimeStrategies() {
        return realTimeStrategyRepository.findAll();
    }

    @Override
    public List<RealTimeStrategyEntity> getActiveRealTimeStrategies() {
        return realTimeStrategyRepository.findByIsActiveTrueOrderByCreateTimeDesc();
    }

    @Override
    public Optional<RealTimeStrategyEntity> getRealTimeStrategyByCode(String strategyCode) {
        if (StringUtils.isBlank(strategyCode)) {
            return Optional.empty();
        }
        return realTimeStrategyRepository.findByStrategyCode(strategyCode);
    }

    @Override
    public Optional<RealTimeStrategyEntity> getRealTimeStrategyById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return realTimeStrategyRepository.findById(id);
    }

    @Override
    public List<RealTimeStrategyEntity> getActiveRealTimeStrategiesByCode(String strategyCode) {
        if (StringUtils.isBlank(strategyCode)) {
            return Collections.emptyList();
        }
        return realTimeStrategyRepository.findByStrategyCodeAndIsActiveTrueOrderByCreateTimeDesc(strategyCode);
    }

    @Override
    public List<RealTimeStrategyEntity> getActiveRealTimeStrategiesBySymbol(String symbol) {
        if (StringUtils.isBlank(symbol)) {
            return Collections.emptyList();
        }
        return realTimeStrategyRepository.findBySymbolAndIsActiveTrueOrderByCreateTimeDesc(symbol);
    }

    @Override
    public List<RealTimeStrategyEntity> getRealTimeStrategiesByStatus(String status) {
        if (StringUtils.isBlank(status)) {
            return Collections.emptyList();
        }
        return realTimeStrategyRepository.findByStatusOrderByCreateTimeDesc(status);
    }

    @Override
    public List<RealTimeStrategyEntity> getRunningRealTimeStrategies() {
        return realTimeStrategyRepository.findByStatusAndIsActiveTrueOrderByCreateTimeDesc("RUNNING");
    }

    @Override
    public List<RealTimeStrategyEntity> getRealTimeStrategiesBySymbolAndStatus(String symbol, String status) {
        if (StringUtils.isBlank(symbol) || StringUtils.isBlank(status)) {
            return Collections.emptyList();
        }
        return realTimeStrategyRepository.findBySymbolAndStatusAndIsActiveTrueOrderByCreateTimeDesc(symbol, status);
    }

    @Override
    public List<RealTimeStrategyEntity> getRealTimeStrategiesByTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime == null || endTime == null) {
            return Collections.emptyList();
        }
        return realTimeStrategyRepository.findByCreateTimeBetweenOrderByCreateTimeDesc(startTime, endTime);
    }

    @Override
    @Transactional
    public RealTimeStrategyEntity saveRealTimeStrategy(RealTimeStrategyEntity realTimeStrategy) {
        if (realTimeStrategy == null) {
            throw new IllegalArgumentException("实时策略不能为空");
        }

        try {
            RealTimeStrategyEntity saved = realTimeStrategyRepository.save(realTimeStrategy);
            log.info("保存实时策略成功: {}", saved.getStrategyCode());
            return saved;
        } catch (Exception e) {
            log.error("保存实时策略失败: {}", realTimeStrategy.getStrategyCode(), e);
            throw new RuntimeException("保存实时策略失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public RealTimeStrategyEntity updateRealTimeStrategy(RealTimeStrategyEntity realTimeStrategy) {
        if (realTimeStrategy == null || realTimeStrategy.getId() == null) {
            throw new IllegalArgumentException("实时策略ID不能为空");
        }

        try {
            RealTimeStrategyEntity updated = realTimeStrategyRepository.save(realTimeStrategy);
            log.info("更新实时策略成功: {}", updated.getStrategyCode());
            return updated;
        } catch (Exception e) {
            log.error("更新实时策略失败: {}", realTimeStrategy.getStrategyCode(), e);
            throw new RuntimeException("更新实时策略失败: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public boolean startRealTimeStrategy(String strategyCode) {
        return updateStrategyStatus(strategyCode, "RUNNING");
    }

    @Override
    @Transactional
    public boolean stopRealTimeStrategy(String strategyCode) {
        Optional<RealTimeStrategyEntity> optionalStrategy = getRealTimeStrategyByCode(strategyCode);
        if (optionalStrategy.isPresent()) {
            RealTimeStrategyEntity strategy = optionalStrategy.get();
            strategy.setStatus("STOPPED");
            strategy.setEndTime(LocalDateTime.now());
            realTimeStrategyRepository.save(strategy);
            log.info("停止实时策略成功: {}", strategyCode);
            return true;
        }
        log.warn("停止实时策略失败，策略不存在: {}", strategyCode);
        return false;
    }

    @Override
    @Transactional
    public boolean updateStrategyStatus(String strategyCode, String status) {
        if (StringUtils.isBlank(strategyCode) || StringUtils.isBlank(status)) {
            return false;
        }

        Optional<RealTimeStrategyEntity> optionalStrategy = getRealTimeStrategyByCode(strategyCode);
        if (optionalStrategy.isPresent()) {
            RealTimeStrategyEntity strategy = optionalStrategy.get();
            strategy.setStatus(status);
            strategy.setErrorMessage(null); // 清除错误信息
            realTimeStrategyRepository.save(strategy);
            log.info("更新策略状态成功: {} -> {}", strategyCode, status);
            return true;
        }
        log.warn("更新策略状态失败，策略不存在: {}", strategyCode);
        return false;
    }

    @Override
    @Transactional
    public boolean updateStrategyStatusWithError(String strategyCode, String status, String errorMessage) {
        if (StringUtils.isBlank(strategyCode) || StringUtils.isBlank(status)) {
            return false;
        }

        Optional<RealTimeStrategyEntity> optionalStrategy = getRealTimeStrategyByCode(strategyCode);
        if (optionalStrategy.isPresent()) {
            RealTimeStrategyEntity strategy = optionalStrategy.get();
            strategy.setStatus(status);
            strategy.setErrorMessage(errorMessage);
            realTimeStrategyRepository.save(strategy);
            log.info("更新策略状态和错误信息成功: {} -> {}, 错误: {}", strategyCode, status, errorMessage);
            return true;
        }
        log.warn("更新策略状态失败，策略不存在: {}", strategyCode);
        return false;
    }

    @Override
    @Transactional
    public boolean activateStrategy(String strategyCode) {
        if (StringUtils.isBlank(strategyCode)) {
            return false;
        }

        Optional<RealTimeStrategyEntity> optionalStrategy = getRealTimeStrategyByCode(strategyCode);
        if (optionalStrategy.isPresent()) {
            RealTimeStrategyEntity strategy = optionalStrategy.get();
            strategy.setIsActive(true);
            realTimeStrategyRepository.save(strategy);
            log.info("激活策略成功: {}", strategyCode);
            return true;
        }
        log.warn("激活策略失败，策略不存在: {}", strategyCode);
        return false;
    }

    @Override
    @Transactional
    public boolean deactivateStrategy(String strategyCode) {
        if (StringUtils.isBlank(strategyCode)) {
            return false;
        }

        Optional<RealTimeStrategyEntity> optionalStrategy = getRealTimeStrategyByCode(strategyCode);
        if (optionalStrategy.isPresent()) {
            RealTimeStrategyEntity strategy = optionalStrategy.get();
            strategy.setIsActive(false);
            strategy.setStatus("STOPPED"); // 停用时同时停止运行
            strategy.setEndTime(LocalDateTime.now());
            realTimeStrategyRepository.save(strategy);
            log.info("停用策略成功: {}", strategyCode);
            return true;
        }
        log.warn("停用策略失败，策略不存在: {}", strategyCode);
        return false;
    }

    @Override
    @Transactional
    public boolean deleteRealTimeStrategy(String strategyCode) {
        if (StringUtils.isBlank(strategyCode)) {
            return false;
        }

        try {
            realTimeStrategyRepository.deleteByStrategyCode(strategyCode);
            log.info("删除实时策略成功: {}", strategyCode);
            return true;
        } catch (Exception e) {
            log.error("删除实时策略失败: {}", strategyCode, e);
            return false;
        }
    }

    @Override
    @Transactional
    public int deleteRealTimeStrategiesByCode(String strategyCode) {
        if (StringUtils.isBlank(strategyCode)) {
            return 0;
        }

        try {
            List<RealTimeStrategyEntity> strategies = getActiveRealTimeStrategiesByCode(strategyCode);
            int count = strategies.size();
            realTimeStrategyRepository.deleteByStrategyCode(strategyCode);
            log.info("根据策略信息代码删除实时策略成功: {}, 删除数量: {}", strategyCode, count);
            return count;
        } catch (Exception e) {
            log.error("根据策略信息代码删除实时策略失败: {}", strategyCode, e);
            return 0;
        }
    }

    @Override
    public boolean existsByStrategyCode(String strategyCode) {
        if (StringUtils.isBlank(strategyCode)) {
            return false;
        }
        return realTimeStrategyRepository.existsByStrategyCode(strategyCode);
    }

    @Override
    public boolean existsByStrategyCodeAndSymbolAndInterval(String strategyCode, String symbol, String interval) {
        if (StringUtils.isBlank(strategyCode) || StringUtils.isBlank(symbol) || StringUtils.isBlank(interval)) {
            return false;
        }
        return realTimeStrategyRepository.existsByStrategyCodeAndSymbolAndInterval(strategyCode, symbol, interval);
    }

    @Override
    public boolean hasRunningStrategy(String strategyCode, String symbol) {
        if (StringUtils.isBlank(strategyCode) || StringUtils.isBlank(symbol)) {
            return false;
        }
        return realTimeStrategyRepository.existsByStrategyCodeAndSymbolAndStatusAndIsActiveTrue(
                strategyCode, symbol, "RUNNING");
    }

    @Override
    public List<RealTimeStrategyEntity> getStrategiesToAutoStart() {
        return realTimeStrategyRepository.findStrategiesToAutoStart();
    }

    @Override
    @Transactional
    public RealTimeStrategyEntity createRealTimeStrategy(String strategyCode,
                                                         String symbol, String interval, Double tradeAmount, String strategyName ) {
        // 参数验证
        if (StringUtils.isBlank(strategyCode) || StringUtils.isBlank(symbol) || StringUtils.isBlank(interval)) {
            throw new IllegalArgumentException("策略信息代码、交易对和K线周期不能为空");
        }
        // 创建实时策略实体
        RealTimeStrategyEntity realTimeStrategy = RealTimeStrategyEntity.builder()
                .strategyCode(strategyCode)
                .strategyName(strategyName)
                .symbol(symbol)
                .interval(interval)
                .tradeAmount(tradeAmount)
                .status("RUNNING")
                .isActive(true)
                .build();
        // 检查策略代码是否已存在
        if (!existsByStrategyCodeAndSymbolAndInterval(strategyCode, symbol, interval)) {
            return saveRealTimeStrategy(realTimeStrategy);
        } else {
            log.warn("相同策略代码，相同币对，相同周期的实时策略已存在: {}, 跳过创建重复", strategyCode);
            return realTimeStrategy;
        }


    }

    /**
     * 生成策略代码
     * 格式: {strategyCode}_{symbol}_{interval}_{timestamp}
     */
    private String generateStrategyCode(String strategyCode, String symbol, String interval) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return String.format("%s_%s_%s_%s", strategyCode, symbol.replace("-", ""), interval, timestamp);
    }

    /**
     * 执行实时回测逻辑
     */
    /**
     * 执行实时回测逻辑
     * 使用WebSocket订阅方式替代轮询获取K线数据
     */
    public Map<String, Object> executeRealTimeBacktest(Strategy strategy, Map<String, Object> state) {
        String strategyCode = (String) state.get("strategyCode");
        String strategyName = (String) state.get("strategyName");
        String symbol = (String) state.get("symbol");
        String interval = (String) state.get("interval");
        LocalDateTime startTime = LocalDateTime.parse((String) state.get("startTime"), dateFormat);
        BigDecimal tradeAmount = (BigDecimal) state.get("tradeAmount");

        try {
            // 启动实时策略管理器
            String strategyKey = realTimeStrategyManager.startRealTimeStrategy(
                    strategyCode, symbol, interval, strategy, tradeAmount, startTime, strategyName);

            log.info("实时策略已启动，策略键: {}", strategyKey);

            // 创建CompletableFuture用于异步等待结果
            CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();

            // 获取策略运行状态并设置Future
            RealTimeStrategyManager.StrategyRunningState runningState =
                    realTimeStrategyManager.getRunningStrategy(strategyCode, symbol, interval);
            if (runningState != null) {
                runningState.setFuture(future);
            }
            // 等待策略执行完成或超时，支持重启机制
            int maxRetries = 3; // 最大重试次数
            int retryCount = 0;

            while (retryCount <= maxRetries) {
                try {
                    // 计算超时时间（到结束时间的毫秒数 + 额外缓冲时间）
                    return future.get(120000, TimeUnit.MILLISECONDS);
                } catch (TimeoutException e) {
                    retryCount++;
                    log.warn("实时策略执行超时 (第{}次), strategyCode={}, symbol={}, interval={}",
                            retryCount, strategyCode, symbol, interval);

                    if (retryCount <= maxRetries) {
                        log.info("尝试重启策略 (第{}次重试): strategyCode={}, symbol={}, interval={}",
                                retryCount, strategyCode, symbol, interval);

                        // 停止当前策略
                        realTimeStrategyManager.stopRealTimeStrategy(strategyCode, symbol, interval);

                        // 等待一段时间后重启
                        try {
                            Thread.sleep(2000); // 等待2秒
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }

                        // 重新启动策略
                        try {
                            String newStrategyKey = realTimeStrategyManager.startRealTimeStrategy(
                                    strategyCode, symbol, interval, strategy, tradeAmount, startTime, strategyName);
                            log.info("策略重启成功，新策略键: {}", newStrategyKey);

                            // 创建新的CompletableFuture
                            future = new CompletableFuture<>();

                            // 获取新的策略运行状态并设置Future
                            runningState = realTimeStrategyManager.getRunningStrategy(strategyCode, symbol, interval);
                            if (runningState != null) {
                                runningState.setFuture(future);
                            }
                        } catch (Exception restartEx) {
                            log.error("策略重启失败: {}", restartEx.getMessage(), restartEx);
                            break;
                        }
                    } else {
                        log.error("策略超时重试次数已达上限，强制停止: strategyCode={}, symbol={}, interval={}",
                                strategyCode, symbol, interval);
                        realTimeStrategyManager.stopRealTimeStrategy(strategyCode, symbol, interval);

                        // 返回超时状态
                        if (runningState != null) {
                            Map<String, Object> result = new HashMap<>();
                            result.put("status", "TIMEOUT_AFTER_RETRIES");
                            result.put("retryCount", retryCount - 1);
                            result.put("totalTrades", runningState.getTotalTrades());
                            result.put("successfulTrades", runningState.getSuccessfulTrades());
                            result.put("successRate", runningState.getTotalTrades() > 0 ?
                                    (double) runningState.getSuccessfulTrades() / runningState.getTotalTrades() : 0.0);
                            result.put("orders", runningState.getOrders());
                            result.put("endTime", LocalDateTime.now());
                            return result;
                        }
                        break;
                    }
                }
            }

        } catch (Exception e) {
            log.error("实时回测执行异常: {}", e.getMessage(), e);
            // 确保清理资源
            try {
                realTimeStrategyManager.stopRealTimeStrategy(strategyCode, symbol, interval);
            } catch (Exception cleanupEx) {
                log.error("清理实时策略失败: {}", cleanupEx.getMessage());
            }
        }

        // 返回默认结果
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ERROR");
        result.put("totalTrades", 0);
        result.put("successfulTrades", 0);
        result.put("successRate", 0.0);
        result.put("orders", new ArrayList<>());
        result.put("endTime", LocalDateTime.now());
        return result;
    }

}
