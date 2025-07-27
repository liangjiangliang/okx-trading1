package com.okx.trading.service.impl;

import com.okx.trading.model.entity.RealTimeStrategyEntity;
import com.okx.trading.model.market.Candlestick;
import com.okx.trading.model.market.Ticker;
import com.okx.trading.repository.RealTimeStrategyRepository;
import com.okx.trading.service.OkxApiService;
import com.okx.trading.service.RealTimeStrategyService;
import com.okx.trading.strategy.RealTimeStrategyManager;
import com.okx.trading.strategy.StrategyRegisterCenter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ta4j.core.Strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import static com.okx.trading.constant.IndicatorInfo.*;

/**
 * 实时运行策略服务实现类
 */
@Slf4j
@Service
public class RealTimeStrategyServiceImpl implements RealTimeStrategyService {

    private final RealTimeStrategyRepository realTimeStrategyRepository;
    private final RealTimeStrategyManager realTimeStrategyManager;
    private final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OkxApiService okxApiService;

    public RealTimeStrategyServiceImpl(RealTimeStrategyRepository realTimeStrategyRepository,
                                       RealTimeStrategyManager realTimeStrategyManager,
                                      @Lazy OkxApiService okxApiService) {
        this.realTimeStrategyRepository = realTimeStrategyRepository;
        this.realTimeStrategyManager = realTimeStrategyManager;
        this.okxApiService = okxApiService;
    }

    @Override
    public List<RealTimeStrategyEntity> getAllRealTimeStrategies() {
        return realTimeStrategyRepository.findAll();
    }

    @Override
    public List<RealTimeStrategyEntity> getActiveRealTimeStrategies() {
        return realTimeStrategyRepository.findStrategiesToAutoStart();
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
    public boolean startRealTimeStrategy(Long id) {
        return updateStrategyStatus(id, "RUNNING");
    }

    @Override
    @Transactional
    public boolean stopRealTimeStrategy(String id) {
        Optional<RealTimeStrategyEntity> optionalStrategy = getRealTimeStrategyById(Long.parseLong(id));
        if (optionalStrategy.isPresent()) {
            RealTimeStrategyEntity strategy = optionalStrategy.get();
            strategy.setStatus("STOPPED");
            strategy.setIsActive(false);
            strategy.setEndTime(LocalDateTime.now());
            realTimeStrategyRepository.save(strategy);
            if (StringUtils.isNotBlank(strategy.getLastTradeType()) && strategy.getLastTradeType().equals(BUY)) {
                realTimeStrategyManager.executeTradeSignal(strategy, new Candlestick(BigDecimal.ZERO), SELL);
            }
            realTimeStrategyManager.getRunningStrategies().remove(Long.parseLong(id));
            log.info("停止实时策略成功: {}", id);
            return true;
        }
        log.warn("停止实时策略失败，策略不存在: {}", id);
        return false;
    }

    @Override
    @Transactional
    public boolean updateStrategyStatus(Long id, String status) {
        if (id == 0 || StringUtils.isBlank(status)) {
            return false;
        }

        Optional<RealTimeStrategyEntity> optionalStrategy = getRealTimeStrategyById(id);
        if (optionalStrategy.isPresent()) {
            RealTimeStrategyEntity strategy = optionalStrategy.get();
            strategy.setStatus(status);
            strategy.setIsActive(true);
            RealTimeStrategyEntity realTimeStrategyEntity = realTimeStrategyRepository.save(strategy);
            realTimeStrategyManager.startExecuteRealTimeStrategy(realTimeStrategyEntity);
            log.info("更新策略状态成功: {} -> {}", strategy.getStrategyName(), status);
            return true;
        }
        log.warn("更新策略状态失败，策略不存在: {}", id);
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
            strategy.setStatus("RUNNING");
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
    public boolean deleteRealTimeStrategy(String id) {
        if (StringUtils.isBlank(id)) {
            return false;
        }

        try {
            realTimeStrategyRepository.deleteById(Long.parseLong(id));
            RealTimeStrategyEntity strategy = realTimeStrategyManager.getRunningStrategies().get(Long.parseLong(id));
            if (strategy != null && strategy.getLastTradeType().equals(BUY)) {
                realTimeStrategyManager.executeTradeSignal(strategy, new Candlestick(BigDecimal.ZERO), SELL);
            }
            realTimeStrategyManager.getRunningStrategies().remove(Long.parseLong(id));
            log.info("删除实时策略成功: {}", id);
            return true;
        } catch (Exception e) {
            log.error("删除实时策略失败: {}", id, e);
            return false;
        }
    }

    @Override
    @Transactional
    public int deleteRealTimeStrategiesByCode(String id) {
        if (StringUtils.isBlank(id)) {
            return 0;
        }

        try {
            List<RealTimeStrategyEntity> strategies = getActiveRealTimeStrategiesByCode(id);
            int count = strategies.size();
            realTimeStrategyRepository.deleteById(Long.parseLong(id));
            log.info("根据策略信息代码删除实时策略成功: {}, 删除数量: {}", id, count);
            return count;
        } catch (Exception e) {
            log.error("根据策略信息代码删除实时策略失败: {}", id, e);
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
    public RealTimeStrategyEntity createRealTimeStrategy(RealTimeStrategyEntity realTimeStrategy) {
        return saveRealTimeStrategy(realTimeStrategy);
    }

    /**
     * 生成策略代码
     * 格式: {strategyCode}_{symbol}_{interval}_{timestamp}
     */
    private String generateStrategyCode(String strategyCode, String symbol, String interval) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        return String.format("%s_%s_%s_%s", strategyCode, symbol.replace("-", ""), interval, timestamp);
    }

    @Override
    @Transactional
    public RealTimeStrategyEntity updateTradeInfo(RealTimeStrategyEntity state) {
        RealTimeStrategyEntity entity = realTimeStrategyRepository.save(state);
        log.info("更新策略交易信息成功: strategyCode={}, tradeType={}, price={}, quantity={}, profit={}, fees={}",
                state.getStrategyCode(), state.getLastTradeType(), state.getLastTradePrice(), state.getLastTradeQuantity(), state.getTotalProfit(), state.getTotalFees());
        return entity;
    }

    @Override
    @Transactional
    public RealTimeStrategyEntity copyRealTimeStrategy(Long strategyId, String interval, String symbol, Double tradeAmount) {
        // 获取原策略
        Optional<RealTimeStrategyEntity> optionalStrategy = getRealTimeStrategyById(strategyId);
        if (!optionalStrategy.isPresent()) {
            log.warn("复制策略失败，原策略不存在: {}", strategyId);
            throw new IllegalArgumentException("策略不存在");
        }

        // 获取原策略
        RealTimeStrategyEntity originalStrategy = optionalStrategy.get();

        // 使用传入的值或原策略值
        String newInterval = interval != null && !interval.trim().isEmpty() ? interval : originalStrategy.getInterval();
        String newSymbol = symbol != null && !symbol.trim().isEmpty() ? symbol : originalStrategy.getSymbol();
        Double newTradeAmount = tradeAmount != null ? tradeAmount : originalStrategy.getTradeAmount();

        RealTimeStrategyEntity newStrategy = new RealTimeStrategyEntity(
                originalStrategy.getStrategyCode(),
                newSymbol,
                newInterval,
                LocalDateTime.now(),
                newTradeAmount,
                originalStrategy.getStrategyName()
        );

        Map<String, Object> response = realTimeStrategyManager.startExecuteRealTimeStrategy(newStrategy);
        RealTimeStrategyEntity savedStrategy = realTimeStrategyManager.getRunningStrategies().get(response.get("id"));
        log.info("复制策略成功: {} -> {}, interval={}, symbol={}, tradeAmount={}",
                originalStrategy.getId(), savedStrategy.getId(), newInterval, newSymbol, newTradeAmount);
        return savedStrategy;
    }

    @Override
    public Map<String, Object> realTimeStrategiesState() {
        // 获取所有正在运行的策略
        Map<Long, RealTimeStrategyEntity> allRunningStrategies = realTimeStrategyManager.getAllRunningStrategies();
        List<Map<String, Object>> strategiesList = new ArrayList<>();

        // 统计指标
        BigDecimal totalEstimatedProfit = BigDecimal.ZERO;
        BigDecimal totalInvestmentAmount = BigDecimal.ZERO;
        BigDecimal totalRealizedProfit = BigDecimal.ZERO;
        int holdingStrategiesCount = 0;
        int runningStrategiesCount = 0;

        // 筛选出最后交易类型为买入(BUY)的策略
        for (RealTimeStrategyEntity strategy : allRunningStrategies.values()) {
            // 统计运行中策略总数
            runningStrategiesCount++;

            // 累计已实现总收益
            if (strategy.getTotalProfit() != null) {
                totalRealizedProfit = totalRealizedProfit.add(BigDecimal.valueOf(strategy.getTotalProfit()));
            }

            // 累计总投资金额
            if (strategy.getTradeAmount() != null) {
                totalInvestmentAmount = totalInvestmentAmount.add(BigDecimal.valueOf(strategy.getTradeAmount()));
            }

            // 处理持仓中策略
            if ("BUY".equals(strategy.getLastTradeType())) {
                holdingStrategiesCount++;
                Map<String, Object> strategyProfit = new HashMap<>();

                try {
                    // 获取基本信息
                    strategyProfit.put("strategyId", strategy.getId());
                    strategyProfit.put("strategyCode", strategy.getStrategyCode());
                    strategyProfit.put("strategyName", strategy.getStrategyName());
                    strategyProfit.put("symbol", strategy.getSymbol());
                    strategyProfit.put("interval", strategy.getInterval());

                    // 入场信息
                    strategyProfit.put("entryPrice", strategy.getLastTradePrice());
                    strategyProfit.put("entryAmount", new BigDecimal(strategy.getLastTradeAmount()).setScale(8, RoundingMode.HALF_UP));
                    strategyProfit.put("entryTime", strategy.getLastTradeTime().format(dateFormat));

                    // 获取最新价格
                    Ticker latestTicker = okxApiService.getTicker(strategy.getSymbol());
                    BigDecimal currentPrice = latestTicker != null ? latestTicker.getLastPrice() : null;

                    if (currentPrice != null && strategy.getLastTradePrice() != null) {
                        // 计算当前持仓价值
                        double entryPrice = strategy.getLastTradePrice();
                        double quantity = strategy.getLastTradeQuantity();
                        double currentValue = currentPrice.doubleValue() * quantity;
                        double entryValue = entryPrice * quantity;

                        // 计算预估收益和收益率
                        double estimatedProfit = currentValue - entryValue;
                        double profitPercentage = (estimatedProfit / entryValue) * 100;

                        // 添加预估收益信息 - 格式化为4位小数
                        strategyProfit.put("currentPrice", currentPrice);
                        strategyProfit.put("quantity", new BigDecimal(quantity).setScale(8, RoundingMode.HALF_UP));
                        strategyProfit.put("currentValue", new BigDecimal(currentValue).setScale(8, RoundingMode.HALF_UP));
                        strategyProfit.put("estimatedProfit", new BigDecimal(estimatedProfit).setScale(8, RoundingMode.HALF_UP));
                        strategyProfit.put("profitPercentage", new BigDecimal(profitPercentage).setScale(4, RoundingMode.HALF_UP) + "%");

                        // 累加预估收益到总预估收益
                        totalEstimatedProfit = totalEstimatedProfit.add(new BigDecimal(estimatedProfit));

                        // 计算持仓时长
                        Duration holdingDuration = Duration.between(strategy.getLastTradeTime(), LocalDateTime.now());
                        long days = holdingDuration.toDays();
                        long hours = holdingDuration.minusDays(days).toHours();
                        long minutes = holdingDuration.minusDays(days).minusHours(hours).toMinutes();

                        String durationStr = "";
                        if (days > 0) {
                            durationStr += days + "天";
                        }
                        if (hours > 0 || days > 0) {
                            durationStr += hours + "小时";
                        }
                        durationStr += minutes + "分钟";

                        strategyProfit.put("holdingDuration", durationStr);
                    } else {
                        // 如果无法获取最新价格
                        strategyProfit.put("currentPrice", "未知");
                        strategyProfit.put("estimatedProfit", "未知");
                        strategyProfit.put("profitPercentage", "未知");
                    }

                    strategiesList.add(strategyProfit);
                } catch (Exception e) {
                    log.error("计算策略{}的预估收益时出错: {}", strategy.getStrategyCode(), e.getMessage());
                    // 继续处理下一个策略
                }
            }
        }

        // 按预估收益率降序排序
        strategiesList.sort((a, b) -> {
            Object aProfit = a.get("profitPercentage");
            Object bProfit = b.get("profitPercentage");

            // 处理可能的字符串情况
            if (aProfit instanceof String && bProfit instanceof String) {
                if (aProfit.equals("未知") && bProfit.equals("未知")) {
                    return 0;
                } else if (aProfit.equals("未知")) {
                    return 1;
                } else if (bProfit.equals("未知")) {
                    return -1;
                }
            }

            // 如果都是数值，则比较
            if (aProfit instanceof String && ((String) aProfit).endsWith("%")) {
                aProfit = new BigDecimal(((String) aProfit).replace("%", ""));
            }
            if (bProfit instanceof String && ((String) bProfit).endsWith("%")) {
                bProfit = new BigDecimal(((String) bProfit).replace("%", ""));
            }

            if (aProfit instanceof BigDecimal && bProfit instanceof BigDecimal) {
                return ((BigDecimal) bProfit).compareTo((BigDecimal) aProfit);
            }

            return 0;
        });

        // 创建包含策略列表和统计信息的返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("strategies", strategiesList);

        // 添加统计信息
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("totalEstimatedProfit", totalEstimatedProfit.setScale(8, RoundingMode.HALF_UP));
        statistics.put("totalRealizedProfit", totalRealizedProfit.setScale(8, RoundingMode.HALF_UP));
        statistics.put("totalInvestmentAmount", totalInvestmentAmount.setScale(8, RoundingMode.HALF_UP));
        statistics.put("holdingStrategiesCount", holdingStrategiesCount);
        statistics.put("runningStrategiesCount", runningStrategiesCount);

        // 计算总收益(已实现收益 + 未实现收益)
        BigDecimal totalProfit = totalRealizedProfit.add(totalEstimatedProfit);
        statistics.put("totalProfit", totalProfit.setScale(8, RoundingMode.HALF_UP));

        // 计算总收益率
        if (totalInvestmentAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal totalProfitRate = totalProfit.multiply(new BigDecimal("100")).divide(totalInvestmentAmount, 4, RoundingMode.HALF_UP);
            statistics.put("totalProfitRate", totalProfitRate + "%");
        } else {
            statistics.put("totalProfitRate", "0.00%");
        }

        result.put("statistics", statistics);
        return result;
    }
}
