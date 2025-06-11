package com.okx.trading.service;

import com.okx.trading.model.entity.StrategyInfoEntity;
import com.okx.trading.ta4j.strategy.StrategyFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.janino.ClassBodyEvaluator;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

/**
 * 动态策略服务类
 * 用于编译、加载和管理动态生成的策略
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicStrategyService {

    private final StrategyInfoService strategyInfoService;
    
    // 缓存已编译的策略函数
    private final Map<String, BiFunction<BarSeries, Map<String, Object>, Strategy>> compiledStrategies = new ConcurrentHashMap<>();

    /**
     * 编译策略代码并加载到StrategyFactory
     * @param strategyCode 策略代码
     * @param strategyEntity 策略实体
     * @return 编译后的策略函数
     */
    public BiFunction<BarSeries, Map<String, Object>, Strategy> compileAndLoadStrategy(
            String strategyCode, StrategyInfoEntity strategyEntity) {
        try {
            // 编译策略代码
            BiFunction<BarSeries, Map<String, Object>, Strategy> strategyFunction = compileStrategyCode(strategyCode);
            
            // 缓存策略函数
            compiledStrategies.put(strategyEntity.getStrategyCode(), strategyFunction);
            
            // 动态加载到StrategyFactory
            loadStrategyToFactory(strategyEntity.getStrategyCode(), strategyFunction);
            
            log.info("策略 {} 编译并加载成功", strategyEntity.getStrategyCode());
            return strategyFunction;
        } catch (Exception e) {
            log.error("编译策略代码失败: {}", e.getMessage(), e);
            throw new RuntimeException("编译策略代码失败: " + e.getMessage());
        }
    }

    /**
     * 编译策略代码
     */
    @SuppressWarnings("unchecked")
    private BiFunction<BarSeries, Map<String, Object>, Strategy> compileStrategyCode(String strategyCode) throws Exception {
        // 创建类体评估器
        ClassBodyEvaluator classBodyEvaluator = new ClassBodyEvaluator();
        
        // 设置实现的接口
        classBodyEvaluator.setImplementedInterfaces(new Class[] { BiFunction.class });
        
        // 设置导入的包
        classBodyEvaluator.setDefaultImports(new String[]{
            "org.ta4j.core.*",
            "org.ta4j.core.indicators.*",
            "org.ta4j.core.indicators.bollinger.*",
            "org.ta4j.core.indicators.keltner.*",
            "org.ta4j.core.indicators.ichimoku.*",
            "org.ta4j.core.indicators.adx.*",
            "org.ta4j.core.indicators.candles.*",
            "org.ta4j.core.indicators.helpers.*",
            "org.ta4j.core.indicators.statistics.*",
            "org.ta4j.core.indicators.volume.*",
            "org.ta4j.core.rules.*",
            "org.ta4j.core.num.*",
            "java.util.*",
            "java.util.function.*"
        });
        
        // 将lambda表达式转换为apply方法实现
        String methodBody;
        if (strategyCode.trim().startsWith("(") && strategyCode.contains("->")) {
            // 提取lambda表达式的参数和方法体
            String[] parts = strategyCode.split("->", 2);
            String params = parts[0].trim();
            String body = parts[1].trim();
            
            // 移除参数的括号
            if (params.startsWith("(") && params.endsWith(")")) {
                params = params.substring(1, params.length() - 1);
            }
            
            // 解析参数名
            String[] paramNames = params.split(",");
            String seriesParam = paramNames[0].trim();
            String paramsParam = paramNames.length > 1 ? paramNames[1].trim() : "params";
            
            // 移除方法体中的完全限定类名，使用简单类名
            // 同时处理参数获取的类型转换问题和RSI指标的参数替换
            String cleanBody = body.replaceAll("org\\.ta4j\\.core\\.indicators\\.helpers\\.", "")
                                    .replaceAll("org\\.ta4j\\.core\\.indicators\\.", "")
                                    .replaceAll("org\\.ta4j\\.core\\.rules\\.", "")
                                    .replaceAll("org\\.ta4j\\.core\\.", "")
                                    .replaceAll("java\\.util\\.", "")
                                    .replaceAll("\\(Integer\\)\\s*" + paramsParam + "\\.get\\(", "getInt(" + paramsParam + ", ")
                                    .replaceAll("\\(int\\)\\s*" + paramsParam + "\\.get\\(", "getInt(" + paramsParam + ", ")
                                    .replaceAll(paramsParam + "\\.get\\(([^)]+)\\)", "getInt(" + paramsParam + ", $1)")
                                    // 移除已有的ClosePriceIndicator定义，避免重复定义
                                    .replaceAll("ClosePriceIndicator\\s+\\w+\\s*=\\s*new\\s+ClosePriceIndicator\\([^;]+;\\s*", "")
                                    .replaceAll("ClosePriceIndicator\\s+closePrice\\s*=\\s*new\\s+ClosePriceIndicator\\([^;]+;\\s*", "")
                                    // 替换需要ClosePriceIndicator的指标，使用更精确的匹配
                                    .replaceAll("RSIIndicator\\(\\s*" + seriesParam + "\\s*,", "RSIIndicator(closePrice,")
                                    .replaceAll("new RSIIndicator\\(\\s*" + seriesParam + "\\s*,", "new RSIIndicator(closePrice,")
                                    .replaceAll("EMAIndicator\\(\\s*" + seriesParam + "\\s*,", "EMAIndicator(closePrice,")
                                    .replaceAll("new EMAIndicator\\(\\s*" + seriesParam + "\\s*,", "new EMAIndicator(closePrice,")
                                    .replaceAll("SMAIndicator\\(\\s*" + seriesParam + "\\s*,", "SMAIndicator(closePrice,")
                                    .replaceAll("new SMAIndicator\\(\\s*" + seriesParam + "\\s*,", "new SMAIndicator(closePrice,");
             
             // 构建apply方法，添加类型转换辅助方法和ClosePriceIndicator
             methodBody = String.format(
                 "private Integer getInt(Map<String, Object> params, String key) {\n" +
                 "    Object value = params.get(key);\n" +
                 "    if (value instanceof Integer) return (Integer) value;\n" +
                 "    if (value instanceof String) return Integer.parseInt((String) value);\n" +
                 "    if (value instanceof Number) return ((Number) value).intValue();\n" +
                 "    return 0;\n" +
                 "}\n" +
                 "\n" +
                 "public Object apply(Object arg0, Object arg1) {\n" +
                 "    BarSeries %s = (BarSeries) arg0;\n" +
                 "    Map<String, Object> %s = (Map<String, Object>) arg1;\n" +
                 "    ClosePriceIndicator closePrice = new ClosePriceIndicator(%s);\n" +
                 "    \n" +
                 "    %s\n" +
                 "}",
                 seriesParam, paramsParam, seriesParam, cleanBody
             );
        } else {
            // 如果不是lambda表达式，假设是完整的方法体
            methodBody = strategyCode;
        }
        
        // 编译类体
        classBodyEvaluator.cook(methodBody);
        
        // 创建实例
        Object instance = classBodyEvaluator.getClazz().newInstance();
        
        if (!(instance instanceof BiFunction)) {
            throw new RuntimeException("策略代码必须实现BiFunction<BarSeries, Map<String, Object>, Strategy>接口");
        }
        
        return (BiFunction<BarSeries, Map<String, Object>, Strategy>) instance;
    }

    /**
     * 将策略函数动态加载到StrategyFactory
     */
    private void loadStrategyToFactory(String strategyCode, BiFunction<BarSeries, Map<String, Object>, Strategy> strategyFunction) {
        try {
            // 通过反射获取StrategyFactory的strategyCreators字段
            Field strategyCreatorsField = StrategyFactory.class.getDeclaredField("strategyCreators");
            strategyCreatorsField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Map<String, BiFunction<BarSeries, Map<String, Object>, Strategy>> strategyCreators = 
                (Map<String, BiFunction<BarSeries, Map<String, Object>, Strategy>>) strategyCreatorsField.get(null);
            
            // 添加新策略
            strategyCreators.put(strategyCode, strategyFunction);
            
            log.info("策略 {} 已动态加载到StrategyFactory", strategyCode);
        } catch (Exception e) {
            log.error("动态加载策略到StrategyFactory失败: {}", e.getMessage(), e);
            throw new RuntimeException("动态加载策略失败: " + e.getMessage());
        }
    }

    /**
     * 从数据库加载所有动态策略
     */
    public void loadAllDynamicStrategies() {
        try {
            // 获取所有有源代码的策略
            strategyInfoService.findAll().stream()
                .filter(strategy -> strategy.getSourceCode() != null && !strategy.getSourceCode().trim().isEmpty())
                .forEach(strategy -> {
                    try {
                        compileAndLoadStrategy(strategy.getSourceCode(), strategy);
                        log.info("从数据库加载策略: {}", strategy.getStrategyCode());
                    } catch (Exception e) {
                        log.error("加载策略 {} 失败: {}", strategy.getStrategyCode(), e.getMessage());
                    }
                });
        } catch (Exception e) {
            log.error("加载动态策略失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 移除策略
     */
    public void removeStrategy(String strategyCode) {
        try {
            // 从缓存中移除
            compiledStrategies.remove(strategyCode);
            
            // 从StrategyFactory中移除
            Field strategyCreatorsField = StrategyFactory.class.getDeclaredField("strategyCreators");
            strategyCreatorsField.setAccessible(true);
            
            @SuppressWarnings("unchecked")
            Map<String, BiFunction<BarSeries, Map<String, Object>, Strategy>> strategyCreators = 
                (Map<String, BiFunction<BarSeries, Map<String, Object>, Strategy>>) strategyCreatorsField.get(null);
            
            strategyCreators.remove(strategyCode);
            
            log.info("策略 {} 已移除", strategyCode);
        } catch (Exception e) {
            log.error("移除策略失败: {}", e.getMessage(), e);
            throw new RuntimeException("移除策略失败: " + e.getMessage());
        }
    }

    /**
     * 获取已编译的策略函数
     */
    public BiFunction<BarSeries, Map<String, Object>, Strategy> getCompiledStrategy(String strategyCode) {
        return compiledStrategies.get(strategyCode);
    }

    /**
     * 检查策略是否已加载
     */
    public boolean isStrategyLoaded(String strategyCode) {
        return compiledStrategies.containsKey(strategyCode);
    }
}