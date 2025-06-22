package com.okx.trading.service.impl;

import com.okx.trading.model.entity.RealTimeStrategyEntity;
import com.okx.trading.repository.RealTimeStrategyRepository;
import com.okx.trading.service.RealTimeStrategyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 实时运行策略服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RealTimeStrategyServiceImpl implements RealTimeStrategyService {

    private final RealTimeStrategyRepository realTimeStrategyRepository;

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
                                                         String symbol, String interval, Double tradeAmount) {
        // 参数验证
        if (StringUtils.isBlank(strategyCode) || StringUtils.isBlank(symbol) || StringUtils.isBlank(interval)) {
            throw new IllegalArgumentException("策略信息代码、交易对和K线周期不能为空");
        }
        // 创建实时策略实体
        RealTimeStrategyEntity realTimeStrategy = RealTimeStrategyEntity.builder()
                .strategyCode(strategyCode)
                .symbol(symbol)
                .interval(interval)
                .tradeAmount(tradeAmount)
                .status("STOPPED")
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
}
