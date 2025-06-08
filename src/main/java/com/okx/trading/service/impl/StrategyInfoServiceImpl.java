package com.okx.trading.service.impl;

import com.okx.trading.model.entity.StrategyInfoEntity;
import com.okx.trading.repository.StrategyInfoRepository;
import com.okx.trading.service.StrategyInfoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 策略信息服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyInfoServiceImpl implements StrategyInfoService {

    private final StrategyInfoRepository strategyInfoRepository;

    @Override
    public List<StrategyInfoEntity> getAllStrategies() {
        return strategyInfoRepository.findAllByOrderByStrategyCodeAsc();
    }

    @Override
    public Optional<StrategyInfoEntity> getStrategyByCode(String strategyCode) {
        return strategyInfoRepository.findByStrategyCode(strategyCode);
    }

    @Override
    public List<StrategyInfoEntity> getStrategiesByCategory(String category) {
        return strategyInfoRepository.findByCategoryOrderByStrategyNameAsc(category);
    }

    @Override
    public StrategyInfoEntity saveStrategy(StrategyInfoEntity strategyInfo) {
        return strategyInfoRepository.save(strategyInfo);
    }

    @Override
    public List<StrategyInfoEntity> saveAllStrategies(List<StrategyInfoEntity> strategyInfoList) {
        return strategyInfoRepository.saveAll(strategyInfoList);
    }

    @Override
    public void deleteStrategy(Long id) {
        strategyInfoRepository.deleteById(id);
    }

    @Override
    public String getDefaultParams(String strategyCode) {
        return strategyInfoRepository.findByStrategyCode(strategyCode)
                .map(StrategyInfoEntity::getDefaultParams)
                .orElse("");
    }

    @Override
    public Map<String, Map<String, String>> getStrategiesInfo() {
        List<StrategyInfoEntity> strategies = getAllStrategies();
        Map<String, Map<String, String>> result = new HashMap<>();

        for (StrategyInfoEntity strategy : strategies) {
            Map<String, String> strategyInfo = new HashMap<>();
            strategyInfo.put("name", strategy.getStrategyName());
            strategyInfo.put("description", strategy.getDescription());
            strategyInfo.put("default_params", strategy.getDefaultParams());
            strategyInfo.put("category", strategy.getCategory());
            strategyInfo.put("params", strategy.getParamsDesc());
            strategyInfo.put("strategy_code", strategy.getStrategyCode());
            result.put(strategy.getStrategyCode(), strategyInfo);
        }

        return result;
    }
}
