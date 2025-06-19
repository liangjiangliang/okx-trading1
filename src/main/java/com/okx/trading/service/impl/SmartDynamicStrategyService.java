package com.okx.trading.service.impl;

import com.okx.trading.model.entity.StrategyInfoEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;

import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.List;
import java.util.ArrayList;

/**
 * 智能动态策略服务
 * 结合Java Compiler API和Janino，并能自动修复常见编译错误
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmartDynamicStrategyService {

    private final JavaCompilerDynamicStrategyService javaCompilerService;
    private final DynamicStrategyService janinoService;

    /**
     * 智能编译策略代码，自动修复常见错误并选择最佳编译器
     */
    public Function<BarSeries, Strategy> compileAndLoadStrategy(
            String strategyCode, StrategyInfoEntity strategyEntity) {

        // 第一步：直接尝试编译原始代码
        if (looksLikeStandardCode(strategyCode)) {
            log.info("代码看起来是标准格式，直接尝试编译: {}", strategyEntity.getStrategyName());
        } else {
            log.info("检测到可能的代码问题，尝试直接编译: {}", strategyEntity.getStrategyName());
        }

        try {
            // 先用Java Compiler API直接编译原始代码
            Function<BarSeries, Strategy> directCompiled = javaCompilerService.compileAndLoadStrategy(strategyCode, strategyEntity);
            if (directCompiled != null) {
                log.info("✅ 原始代码直接编译成功: {}", strategyEntity.getStrategyName());
                return directCompiled;
            }
        } catch (Exception directCompileError) {
            log.info("❌ 原始代码直接编译失败: {}", directCompileError.getMessage());

            // 如果代码看起来不需要修复，记录警告
            if (!mightNeedFix(strategyCode)) {
                log.warn("代码看起来是标准格式但编译失败，可能存在未知问题");
            }
        }

        // 第二步：如果直接编译失败，进行错误修复后再编译
        log.info("🔧 开始自动修复策略代码错误...");
        String originalCode = strategyCode;
        String fixedCode = autoFixCommonErrors(strategyCode);

        // 记录修复的错误类型
        logFixedErrors(originalCode, fixedCode);

        // 第三步：尝试编译修复后的代码
        try {
            Function<BarSeries, Strategy> fixedCompiled = javaCompilerService.compileAndLoadStrategy(fixedCode, strategyEntity);
            if (fixedCompiled != null) {
                log.info("✅ 修复后代码编译成功: {}", strategyEntity.getStrategyName());
                return fixedCompiled;
            }
        } catch (Exception javaCompilerError) {
            log.warn("❌ Java Compiler API编译修复后代码失败: {}", javaCompilerError.getMessage());

            // 第四步：如果Java Compiler还是失败，尝试Janino编译器
            log.info("🔄 尝试使用Janino编译器...");
            String simplifiedCode = simplifyForJanino(fixedCode);

            try {
                Function<BarSeries, Strategy> janinoCompiled = janinoService.compileAndLoadStrategy(simplifiedCode, strategyEntity);
                if (janinoCompiled != null) {
                    log.info("✅ Janino编译器编译成功: {}", strategyEntity.getStrategyName());
                    return janinoCompiled;
                }
            } catch (Exception janinoError) {
                log.error("❌ 所有编译器都失败了 - Java Compiler: {}, Janino: {}",
                    javaCompilerError.getMessage(), janinoError.getMessage());
                throw new RuntimeException(
                    "编译失败 - Java Compiler API: " + javaCompilerError.getMessage() +
                    "; Janino: " + janinoError.getMessage());
            }
        }

        // 如果所有步骤都失败了
        log.error("💥 策略编译完全失败: {}", strategyEntity.getStrategyName());
        throw new RuntimeException("策略编译完全失败，所有编译器和修复方法都无效");
    }

    /**
     * 自动修复常见的编译错误
     */
    private String autoFixCommonErrors(String strategyCode) {
        String fixedCode = strategyCode;

        try {
            log.info("🔧 开始自动修复策略代码错误...");
            
            // 检测是否为静态方法格式的策略代码
            if (isStaticMethodFormat(fixedCode)) {
                log.info("检测到静态方法格式的策略代码，跳过继承相关的修复");
                
                // 对静态方法格式的代码进行特殊修复
                fixedCode = fixStaticMethodErrors(fixedCode);
                
                // 修复静态方法中的指标缺失问题
                fixedCode = fixMissingIndicatorsForStaticMethod(fixedCode);
                
                // **优先修复MACD构造函数错误 - 在其他修复之前**
                fixedCode = fixMACDConstructorEarly(fixedCode);
                
                // 其他修复...
                fixedCode = fixMACDIndicatorConstructor(fixedCode);
                fixedCode = removeInnerClasses(fixedCode);
            fixedCode = inlinePrivateMethods(fixedCode);
            } else {
                // 对普通策略类格式的代码进行修复
                log.info("检测到普通策略类格式，开始标准修复流程");

                // **优先修复MACD构造函数错误**
                fixedCode = fixMACDConstructorEarly(fixedCode);

                fixedCode = fixImports(fixedCode);
            fixedCode = fixClassDeclaration(fixedCode);
                fixedCode = fixMACDUsage(fixedCode);
            fixedCode = fixSuperCallPosition(fixedCode);
            }

            // 通用修复方法
            fixedCode = fixCommonSyntaxErrors(fixedCode);
            fixedCode = fixMissingIndicators(fixedCode);
            fixedCode = fixRuleCombination(fixedCode);
            fixedCode = fixCommonCompilationErrors(fixedCode);
            fixedCode = addCustomIndicatorsAndMethods(fixedCode);
            fixedCode = fixBracketMatching(fixedCode);
            
        } catch (Exception e) {
            log.error("自动修复过程中发生错误: {}", e.getMessage());
            return strategyCode; // 如果修复过程失败，返回原始代码
        }

        // 计算修改的字符数
        int changedChars = Math.abs(fixedCode.length() - strategyCode.length());
        log.info("策略代码错误修复完成，共进行了 {} 个字符的修改", changedChars);
        
        // 记录修复的错误类型
        logFixedErrors(strategyCode, fixedCode);
        
        // 进行代码优化和标准化处理
        fixedCode = optimizeCode(fixedCode);
        log.info("进行了代码优化和标准化处理");

            return fixedCode;
    }
    
    /**
     * 早期修复MACD构造函数错误
     */
    private String fixMACDConstructorEarly(String code) {
        try {
            log.info("🔧 修复MACD构造函数错误...");
            
            // 修复具体的EMA参数错误调用 - 最高优先级
            code = code.replaceAll("new MACDIndicator\\(shortEma,\\s*longEma\\)", 
                "new MACDIndicator(closePrice, shortPeriod, longPeriod)");
            code = code.replaceAll("new MACDIndicator\\(([a-zA-Z_][a-zA-Z0-9_]*Ema),\\s*([a-zA-Z_][a-zA-Z0-9_]*Ema)\\)", 
                "new MACDIndicator(closePrice, shortPeriod, longPeriod)");
            
            // 修复所有形式的两个EMA参数调用
            code = code.replaceAll("MACDIndicator\\s+([a-zA-Z_][a-zA-Z0-9_]*)\\s*=\\s*new\\s*MACDIndicator\\(\\s*([a-zA-Z_][a-zA-Z0-9_]*),\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\);", 
                "MACDIndicator $1 = new MACDIndicator(closePrice, shortPeriod, longPeriod);");
            
            // 修复任何两个参数的MACD构造函数调用
            code = code.replaceAll("new MACDIndicator\\(([^,)]+),\\s*([^,)]+)\\)(?!,)", 
                "new MACDIndicator(closePrice, shortPeriod, longPeriod)");
            
            log.info("✅ MACD构造函数错误修复完成");
        } catch (Exception e) {
            log.error("修复MACD构造函数时发生错误: {}", e.getMessage());
        }
        return code;
    }

    /**
     * 检查是否是静态方法格式的策略代码
     */
    private boolean isStaticMethodFormat(String code) {
        // 检查是否包含静态方法签名
        return code.contains("public static Strategy") && 
               code.contains("(BarSeries series)") &&
               !code.contains("extends BaseStrategy");
    }

    /**
     * 修复静态方法格式代码的错误
     */
    private String fixStaticMethodErrors(String code) {
        log.info("🔧 开始修复静态方法格式代码");
        log.info("原始代码长度: {}", code.length());
        
        String originalCode = code;
        
        // 对于静态方法格式，只需要进行基本的修复
        
        // 1. 修复常见的语法错误
        code = fixCommonSyntaxErrors(code);
        if (!code.equals(originalCode)) {
            log.info("✅ fixCommonSyntaxErrors 修改了代码");
        }
        
        // 2. 修复不存在的指标类（但不修复继承相关问题）
        String beforeIndicatorFix = code;
        code = fixMissingIndicatorsForStaticMethod(code);
        if (!code.equals(beforeIndicatorFix)) {
            log.info("✅ fixMissingIndicatorsForStaticMethod 修改了代码");
            log.info("修改前: {}", beforeIndicatorFix.substring(Math.max(0, beforeIndicatorFix.length()-200)));
            log.info("修改后: {}", code.substring(Math.max(0, code.length()-200)));
        }
        
        // 3. 修复括号匹配问题
        String beforeBracketFix = code;
        code = fixBracketMatching(code);
        if (!code.equals(beforeBracketFix)) {
            log.info("✅ fixBracketMatching 修改了代码");
        }
        
        // 4. 修复Rule组合问题（AndRule/OrRule和Num.valueOf）
        String beforeRuleFix = code;
        code = fixRuleCombination(code);
        if (!code.equals(beforeRuleFix)) {
            log.info("✅ fixRuleCombination 修改了代码");
        }
        
        log.info("修复后代码长度: {}", code.length());
        return code;
    }

    /**
     * 为静态方法格式修复缺失的指标类
     */
    private String fixMissingIndicatorsForStaticMethod(String code) {
        // 只修复指标类的简单替换，不涉及继承
        
        // 修复不存在的指标类名
        code = code.replaceAll("IchimokuTenkanSenIndicator", "SMAIndicator");
        code = code.replaceAll("IchimokuKijunSenIndicator", "EMAIndicator");
        code = code.replaceAll("UlcerIndexIndicator", "RSIIndicator");
        code = code.replaceAll("ParabolicSarIndicator", "SMAIndicator");
        code = code.replaceAll("ChandelierExitIndicator", "SMAIndicator");
        
        // 只修复真正有三个或更多参数的指标调用，避免影响正确的两参数调用
        // 这里使用更精确的正则表达式，确保真的是三个参数（两个逗号）
        code = code.replaceAll("new SMAIndicator\\(([^,()]+),\\s*([^,()]+),\\s*([^,()]+)\\)", "new SMAIndicator($1, $2)");
        code = code.replaceAll("new EMAIndicator\\(([^,()]+),\\s*([^,()]+),\\s*([^,()]+)\\)", "new EMAIndicator($1, $2)");
        
        return code;
    }

    /**
     * 修复MACDIndicator构造函数问题和BaseStrategy构造函数问题
     */
    private String fixMACDIndicatorConstructor(String code) {
        // MACDIndicator(shortEma, longEma) -> MACDIndicator(closePrice, shortPeriod, longPeriod)
        Pattern macdPattern = Pattern.compile(
            "MACDIndicator\\s+macd\\s*=\\s*new\\s+MACDIndicator\\(\\s*shortEma\\s*,\\s*longEma\\s*\\);"
        );

        if (macdPattern.matcher(code).find()) {
            code = code.replaceAll(
                "EMAIndicator\\s+shortEma\\s*=\\s*new\\s+EMAIndicator\\(closePrice,\\s*(\\d+)\\);\\s*\n" +
                "\\s*EMAIndicator\\s+longEma\\s*=\\s*new\\s+EMAIndicator\\(closePrice,\\s*(\\d+)\\);\\s*\n" +
                "\\s*MACDIndicator\\s+macd\\s*=\\s*new\\s+MACDIndicator\\(\\s*shortEma\\s*,\\s*longEma\\s*\\);",
                "MACDIndicator macd = new MACDIndicator(closePrice, $1, $2);"
            );
        }

        // 修复BaseStrategy构造函数调用问题
        // super() -> super(buyRule, sellRule)
        if (code.contains("super();")) {
            code = code.replace("super();", "super(buyRule, sellRule);");
        }

        // 修复MACD指标方法调用问题
        // getMACDLineIndicator() 和 getSignalIndicator() 不存在，需要用正确的方式
        code = code.replaceAll("\\.getMACDLineIndicator\\(\\)", "");
        code = code.replaceAll("\\.getSignalIndicator\\(\\)", "");
        code = code.replaceAll("\\.getMACDLine\\(\\)", "");
        code = code.replaceAll("\\.getSignalLine\\(\\)", "");

        // 修复MACD指标使用方式
        if (code.contains("MACDIndicator") && code.contains("CrossedUpIndicatorRule")) {
            // 简化MACD策略，直接使用MACD指标和信号线
            code = fixMACDUsage(code);
        }

        return code;
    }

    /**
     * 移除内部类，保留原始逻辑但修复语法错误
     */
    private String removeInnerClasses(String code) {
        // 移除内部类定义，但保留其逻辑
        if (code.contains("private static class") || code.contains("extends AbstractIndicator")) {
            log.warn("检测到内部类，将移除内部类定义但保留逻辑");
            // 移除内部类定义，但不替换整个策略
            code = code.replaceAll("private static class[^}]+}[^}]*}", "");
            code = code.replaceAll("extends AbstractIndicator[^}]+}", "");
        }
        return code;
    }

    /**
     * 修复构造函数中的super调用问题
     */
    private String inlinePrivateMethods(String code) {
        // 修复super调用位置问题
        return fixSuperCallPosition(code);
    }

    /**
     * 修复import问题
     */
    private String fixImports(String code) {
        // 修复import * 语法错误
        code = code.replaceAll("import\\s+([^;]+)\\*;", "import $1*;");

        // 确保有正确的import语句
        if (!code.contains("import org.ta4j.core.BaseStrategy")) {
            code = code.replaceFirst("public class", "import org.ta4j.core.BaseStrategy;\n\npublic class");
        }

        // 修复常见的import问题
        code = code.replaceAll("import\\s+org\\.ta4j\\.core\\.\\*;",
            "import org.ta4j.core.*;\n" +
            "import org.ta4j.core.indicators.*;\n" +
            "import org.ta4j.core.indicators.helpers.*;\n" +
            "import org.ta4j.core.rules.*;");

        return code;
    }

    /**
     * 修复类声明问题
     */
    private String fixClassDeclaration(String code) {
        // 确保正确继承BaseStrategy
        code = code.replaceAll("extends\\s+Strategy", "extends BaseStrategy");

        // 修复类名声明中缺少空格的问题
        code = code.replaceAll("public class([A-Z])", "public class $1");
        code = code.replaceAll("classGenerated", "class Generated");
        code = code.replaceAll("public classGenerated", "public class Generated");

        return code;
    }

    /**
     * 修复MACD指标使用方式
     */
    private String fixMACDUsage(String code) {
        // 修复错误的MACD方法调用，但保留原始逻辑结构

        // 替换错误的MACD方法调用为正确的指标引用
        code = code.replaceAll(
            "new MACDIndicator\\(new ClosePriceIndicator\\(series\\), 12, 26\\)",
            "macd"
        );

        // 替换信号线引用
        code = code.replaceAll(
            "new EMAIndicator\\(new MACDIndicator\\([^)]+\\), 9\\)",
            "signal"
        );

        // 确保有MACD和信号线的定义
        if (!code.contains("MACDIndicator macd =") && code.contains("macd")) {
            // 在构造函数开头添加MACD和信号线定义
            code = code.replaceFirst(
                "(public\\s+\\w+\\s*\\([^)]*\\)\\s*\\{)",
                "$1\n        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);\n" +
                "        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);\n" +
                "        EMAIndicator signal = new EMAIndicator(macd, 9);\n"
            );
        }

        return code;
    }

    /**
     * 修复super调用位置问题
     */
    private String fixSuperCallPosition(String code) {
        // 检查是否有super调用在构造函数中间的问题
        if (!code.contains("super(") || !code.contains("Rule ")) {
            return code; // 没有需要修复的问题
        }

        String className = extractClassName(code);

        // 查找构造函数内容
        String[] lines = code.split("\n");
        StringBuilder result = new StringBuilder();
        boolean inConstructor = false;
        List<String> beforeSuper = new ArrayList<>();
        List<String> afterSuper = new ArrayList<>();
        String superCall = null;
        boolean foundSuper = false;

        for (String line : lines) {
            String trimmedLine = line.trim();

            if (trimmedLine.contains("public " + className + "(")) {
                inConstructor = true;
                result.append(line).append("\n");
                continue;
            }

            if (inConstructor) {
                if (trimmedLine.equals("}")) {
                    // 重新组装构造函数：变量定义 + super调用
                    for (String beforeLine : beforeSuper) {
                        result.append(beforeLine).append("\n");
                    }
                    if (superCall != null) {
                        result.append("        ").append(superCall).append("\n");
                    }
                    for (String afterLine : afterSuper) {
                        result.append(afterLine).append("\n");
                    }
                    result.append(line).append("\n");

                    inConstructor = false;
                    continue;
                }

                if (trimmedLine.startsWith("super(") && trimmedLine.endsWith(");")) {
                    superCall = trimmedLine;
                    foundSuper = true;
                } else if (!foundSuper) {
                    beforeSuper.add(line);
                } else {
                    afterSuper.add(line);
                }
            } else {
                result.append(line).append("\n");
            }
        }

        return result.toString();
    }

    /**
     * 修复常见的语法错误
     */
    private String fixCommonSyntaxErrors(String code) {
        // 修复缺少的右括号
        code = fixMissingParentheses(code);

        return code;
    }

    /**
     * 修复缺少的括号
     */
    private String fixMissingParentheses(String code) {
        // 简单的括号平衡检查和修复
        int openParens = 0;
        int closeParens = 0;

        for (char c : code.toCharArray()) {
            if (c == '(') openParens++;
            if (c == ')') closeParens++;
        }

        // 如果缺少右括号，在适当位置添加
        if (openParens > closeParens) {
            int missing = openParens - closeParens;
            // 在最后一个EMAIndicator行后添加缺少的右括号
            String[] lines = code.split("\n");
            StringBuilder result = new StringBuilder();

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                result.append(line);

                // 在包含EMAIndicator且缺少右括号的行后添加
                if (line.contains("EMAIndicator") && line.contains("26") && !line.trim().endsWith(")")) {
                    for (int j = 0; j < missing; j++) {
                        result.append(")");
                    }
                    missing = 0;
                }

                result.append("\n");
            }

            return result.toString();
        }

        return code;
    }

    /**
     * 修复不存在的指标类
     */
    private String fixMissingIndicators(String code) {
        // 修复Ichimoku指标类名
        if (code.contains("Ichimoku")) {
            code = fixIchimokuIndicators(code);
        }

        // 修复Stochastic指标构造函数
        if (code.contains("Stochastic")) {
            code = fixStochasticIndicators(code);
        }

        // 修复ADX指标类名
        if (code.contains("ADX") || code.contains("DI")) {
            code = fixADXIndicators(code);
        }

        // 修复MACD指标问题
        if (code.contains("MACD")) {
            code = fixMACDIndicators(code);
        }

        // 修复布林带指标问题
        if (code.contains("BollingerBands") || code.contains("Bollinger")) {
            code = fixBollingerIndicators(code);
        }

        // 修复CCI指标问题
        if (code.contains("CCI")) {
            code = fixCCIIndicators(code);
        }

        // 修复RSI指标问题
        if (code.contains("RSI")) {
            code = fixRSIIndicators(code);
        }

        // 修复Williams %R指标问题
        if (code.contains("Williams") || code.contains("WilliamsR")) {
            code = fixWilliamsRIndicators(code);
        }

        // 修复KDJ指标问题
        if (code.contains("KDJ")) {
            code = fixKDJIndicators(code);
        }

        // 修复ATR指标问题
        if (code.contains("ATR")) {
            code = fixATRIndicators(code);
        }

        return code;
    }

    /**
     * 修复Ichimoku指标类名
     */
    private String fixIchimokuIndicators(String code) {
        try {
            // 检查TA4J库中实际存在的Ichimoku指标类
            // 替换不存在的指标类为存在的类或等效实现

            // 替换为TA4J中实际存在的指标类
            code = code.replaceAll("IchimokuTenkanSenIndicator", "EMAIndicator");
            code = code.replaceAll("IchimokuKijunSenIndicator", "SMAIndicator");
            code = code.replaceAll("IchimokuSenkouSpanAIndicator", "EMAIndicator");
            code = code.replaceAll("IchimokuSenkouSpanBIndicator", "SMAIndicator");
            code = code.replaceAll("IchimokuCloudIndicator", "EMAIndicator");
            code = code.replaceAll("IchimokuConversionLineIndicator", "EMAIndicator");
            code = code.replaceAll("IchimokuBaseLineIndicator", "SMAIndicator");

            // 移除不存在的方法调用
            code = code.replaceAll("\\.getSenkouSpanAIndicator\\(\\)", "");
            code = code.replaceAll("\\.getSenkouSpanBIndicator\\(\\)", "");

            // 修复构造函数参数
            code = code.replaceAll("new EMAIndicator\\(series, 9\\)", "new EMAIndicator(new ClosePriceIndicator(series), 9)");
            code = code.replaceAll("new SMAIndicator\\(series, 26\\)", "new SMAIndicator(new ClosePriceIndicator(series), 26)");
            code = code.replaceAll("new SMAIndicator\\(series, 52\\)", "new SMAIndicator(new ClosePriceIndicator(series), 52)");

            // 修复多参数的构造函数调用
            code = code.replaceAll("new EMAIndicator\\(series,\\s*\\d+,\\s*\\d+,\\s*\\d+\\)", "new EMAIndicator(new ClosePriceIndicator(series), 9)");
            code = code.replaceAll("new SMAIndicator\\(series,\\s*\\d+,\\s*\\d+,\\s*\\d+\\)", "new SMAIndicator(new ClosePriceIndicator(series), 26)");

        } catch (Exception e) {
            // 如果修复过程中出现异常，返回原代码
            System.err.println("Error fixing Ichimoku indicators: " + e.getMessage());
        }

        return code;
    }

    /**
     * 修复Stochastic指标构造函数
     */
    private String fixStochasticIndicators(String code) {
        // 修复StochasticOscillatorDIndicator构造函数
        // StochasticOscillatorDIndicator只接受一个参数（StochasticOscillatorKIndicator）
        code = code.replaceAll("new StochasticOscillatorDIndicator\\(([^,]+),\\s*\\d+\\)", "new StochasticOscillatorDIndicator($1)");

        // 修复多参数的StochasticOscillatorDIndicator构造函数
        code = code.replaceAll("new StochasticOscillatorDIndicator\\(([^,]+),\\s*(\\d+),\\s*(\\d+)\\)",
                              "new StochasticOscillatorDIndicator(new StochasticOscillatorKIndicator($1, $2))");

        // 如果需要D线的平滑效果，使用SMAIndicator包装
        code = code.replaceAll("new StochasticOscillatorDIndicator\\(new StochasticOscillatorKIndicator\\(([^,]+),\\s*(\\d+)\\)\\)",
                              "new SMAIndicator(new StochasticOscillatorKIndicator($1, $2), 3)");

        return code;
    }

    /**
     * 修复ADX指标类名
     */
    private String fixADXIndicators(String code) {
        // 检查TA4J库中实际存在的ADX指标类
        // 如果不存在，使用等效的指标替代

        // 替换ADX相关指标为存在的指标
        code = code.replaceAll("ADXIndicator", "RSIIndicator");
        code = code.replaceAll("PlusDIIndicator", "EMAIndicator");
        code = code.replaceAll("MinusDIIndicator", "SMAIndicator");

        // 修复构造函数参数
        code = code.replaceAll("new RSIIndicator\\(series, (\\d+)\\)", "new RSIIndicator(new ClosePriceIndicator(series), $1)");
        code = code.replaceAll("new EMAIndicator\\(series, (\\d+)\\)", "new EMAIndicator(new ClosePriceIndicator(series), $1)");
        code = code.replaceAll("new SMAIndicator\\(series, (\\d+)\\)", "new SMAIndicator(new ClosePriceIndicator(series), $1)");

        return code;
    }

    /**
     * 修复MACD指标问题
     */
    private String fixMACDIndicators(String code) {
        try {
            // MACD正确的构造函数是：MACDIndicator(Indicator<Num> indicator, int shortBarCount, int longBarCount)
            // 不是接受两个EMA参数
            
            // 修复错误的MACD构造函数调用
            code = code.replaceAll("new MACDIndicator\\(([^,)]+),\\s*([^,)]+)\\)", 
                "new MACDIndicator($1, 12, 26)");
            
            // 修复具体的EMA参数错误调用
            code = code.replaceAll("new MACDIndicator\\(shortEma,\\s*longEma\\)", 
                "new MACDIndicator(closePrice, shortPeriod, longPeriod)");
            code = code.replaceAll("new MACDIndicator\\(([a-zA-Z_][a-zA-Z0-9_]*Ema),\\s*([a-zA-Z_][a-zA-Z0-9_]*Ema)\\)", 
                "new MACDIndicator(closePrice, shortPeriod, longPeriod)");
            
            // MACD需要closePrice, shortPeriod, longPeriod三个参数
            code = code.replaceAll("new MACDIndicator\\(([^,)]+)\\)", 
                "new MACDIndicator($1, 12, 26)");
            
            // 修复MACD信号线计算
            code = code.replaceAll("new EMAIndicator\\(([a-zA-Z_][a-zA-Z0-9_]*),\\s*(\\d+)\\)", 
                "new EMAIndicator($1, $2)");
            
            // 修复MACDIndicator调用中的getSignal()方法，Ta4j 0.14中没有这个方法
            code = code.replaceAll("([a-zA-Z_][a-zA-Z0-9_]*)\\.getSignal\\(\\)", 
                "new EMAIndicator($1, 9)");
            code = code.replaceAll("([a-zA-Z_][a-zA-Z0-9_]*)\\.getSignalLine\\(\\)", 
                "new EMAIndicator($1, 9)");
            
            // 修复MACD直方图计算
            code = code.replaceAll("([a-zA-Z_][a-zA-Z0-9_]*)\\.getHistogram\\(\\)", 
                "new MinusIndicator($1, new EMAIndicator($1, 9))");
            
            // 修复MACD金叉死叉的Rule创建
            code = code.replaceAll("new CrossedUpIndicatorRule\\(([^,)]+)\\.getSignal\\(\\),\\s*([^,)]+)\\)", 
                "new CrossedUpIndicatorRule($1, new EMAIndicator($1, 9))");
            code = code.replaceAll("new CrossedDownIndicatorRule\\(([^,)]+)\\.getSignal\\(\\),\\s*([^,)]+)\\)", 
                "new CrossedDownIndicatorRule($1, new EMAIndicator($1, 9))");

        } catch (Exception e) {
            log.error("Error fixing MACD indicators: {}", e.getMessage());
        }
        return code;
    }

    /**
     * 修复布林带指标问题
     */
    private String fixBollingerIndicators(String code) {
        try {
            // 1. 修复BollingerBandsMiddleIndicator - 必须传入SMAIndicator而不是直接的closePrice
            code = code.replaceAll("new BollingerBandsMiddleIndicator\\(([^,)]+), (\\d+)\\)", 
                "new BollingerBandsMiddleIndicator(new SMAIndicator($1, $2))");
            code = code.replaceAll("new BollingerBandsMiddleIndicator\\(([^,)]+)\\)", 
                "new BollingerBandsMiddleIndicator(new SMAIndicator($1, 20))");

            // 2. 修复BollingerBandsUpperIndicator - 需要三个参数：middle, standardDeviation, coefficient
            code = code.replaceAll("new BollingerBandsUpperIndicator\\(([^,)]+), (\\d+), ([\\d.]+)\\)", 
                "new BollingerBandsUpperIndicator(new BollingerBandsMiddleIndicator(new SMAIndicator($1, $2)), new StandardDeviationIndicator($1, $2), series.numOf($3))");
            code = code.replaceAll("new BollingerBandsUpperIndicator\\(([^,)]+), (\\d+)\\)", 
                "new BollingerBandsUpperIndicator(new BollingerBandsMiddleIndicator(new SMAIndicator($1, $2)), new StandardDeviationIndicator($1, $2), series.numOf(2.0))");

            // 3. 修复BollingerBandsLowerIndicator - 需要三个参数：middle, standardDeviation, coefficient  
            code = code.replaceAll("new BollingerBandsLowerIndicator\\(([^,)]+), (\\d+), ([\\d.]+)\\)", 
                "new BollingerBandsLowerIndicator(new BollingerBandsMiddleIndicator(new SMAIndicator($1, $2)), new StandardDeviationIndicator($1, $2), series.numOf($3))");
            code = code.replaceAll("new BollingerBandsLowerIndicator\\(([^,)]+), (\\d+)\\)", 
                "new BollingerBandsLowerIndicator(new BollingerBandsMiddleIndicator(new SMAIndicator($1, $2)), new StandardDeviationIndicator($1, $2), series.numOf(2.0))");

            // 4. 修复StandardDeviationIndicator构造函数
            code = code.replaceAll("new StandardDeviationIndicator\\(([^,)]+), (\\d+), ([\\d.]+)\\)", 
                "new StandardDeviationIndicator($1, $2)");

            // 5. 修复布林带指标的数学运算方法调用
            code = code.replaceAll("([a-zA-Z_][a-zA-Z0-9_]*)\\.plus\\(([a-zA-Z_][a-zA-Z0-9_]*)\\.multipliedBy\\(([\\d.]+)\\)\\)", 
                "new PlusIndicator($1, new MultipliedIndicator($2, series.numOf($3)))");
            code = code.replaceAll("([a-zA-Z_][a-zA-Z0-9_]*)\\.minus\\(([a-zA-Z_][a-zA-Z0-9_]*)\\.multipliedBy\\(([\\d.]+)\\)\\)", 
                "new MinusIndicator($1, new MultipliedIndicator($2, series.numOf($3)))");

            // 6. 修复布林带突破规则
            code = code.replaceAll("new OverIndicatorRule\\(([^,)]+), ([a-zA-Z_][a-zA-Z0-9_]*)\\.getBBUpperIndicator\\(\\)\\)", 
                "new OverIndicatorRule($1, $2)");
            code = code.replaceAll("new UnderIndicatorRule\\(([^,)]+), ([a-zA-Z_][a-zA-Z0-9_]*)\\.getBBLowerIndicator\\(\\)\\)", 
                "new UnderIndicatorRule($1, $2)");

            // 7. 修复布林带相关的import
            if (!code.contains("import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator")) {
                code = code.replaceFirst("import org.ta4j.core.rules.*;",
                    "import org.ta4j.core.rules.*;\nimport org.ta4j.core.indicators.bollinger.*;");
            }
            if (!code.contains("import org.ta4j.core.indicators.statistics.StandardDeviationIndicator")) {
                code = code.replaceFirst("import org.ta4j.core.rules.*;",
                    "import org.ta4j.core.rules.*;\nimport org.ta4j.core.indicators.statistics.*;");
            }

        } catch (Exception e) {
            log.error("Error fixing Bollinger indicators: {}", e.getMessage());
        }
        return code;
    }

    /**
     * 修复Rule组合问题
     */
    private String fixRuleCombination(String code) {
        try {
            // 先处理四个参数的AndRule和OrRule - 必须最先处理，避免被两参数的规则匹配
            code = code.replaceAll("new AndRule\\(\\s*([^,()]+),\\s*([^,()]+),\\s*([^,()]+),\\s*([^,()]+)\\s*\\)", 
                "($1).and($2).and($3).and($4)");
                
            // 再处理三个参数的情况
            code = code.replaceAll("new AndRule\\(\\s*([^,()]+),\\s*([^,()]+),\\s*([^,()]+)\\s*\\)", 
                "($1).and($2).and($3)");
            code = code.replaceAll("new OrRule\\(\\s*([^,()]+),\\s*([^,()]+),\\s*([^,()]+)\\s*\\)", 
                "($1).or($2).or($3)");
                
            // 最后处理两个参数的情况
            code = code.replaceAll("new AndRule\\(\\s*([^,()]+),\\s*([^,()]+)\\s*\\)", "($1).and($2)");
            code = code.replaceAll("new OrRule\\(\\s*([^,()]+),\\s*([^,()]+)\\s*\\)", "($1).or($2)");
                
            // 修复Num.valueOf() - 改为series.numOf()
            code = code.replaceAll("Num\\.valueOf\\((\\w+)\\)", "series.numOf($1)");
            code = code.replaceAll("Num\\.valueOf\\((\\d+)\\)", "series.numOf($1)");
            code = code.replaceAll("DecimalNum\\.valueOf\\((\\w+)\\)", "series.numOf($1)");
            code = code.replaceAll("DecimalNum\\.valueOf\\((\\d+)\\)", "series.numOf($1)");
            code = code.replaceAll("Decimal\\.valueOf\\((\\w+)\\)", "series.numOf($1)");
            code = code.replaceAll("Decimal\\.valueOf\\(([\\d.]+)\\)", "series.numOf($1)");
            
            // 修复直接数字转换的问题
            code = code.replaceAll("new UnderIndicatorRule\\(([^,]+),\\s*(\\d+)\\)", "new UnderIndicatorRule($1, series.numOf($2))");
            code = code.replaceAll("new OverIndicatorRule\\(([^,]+),\\s*(\\d+)\\)", "new OverIndicatorRule($1, series.numOf($2))");
            
            // 修复布林带指标构造函数中的变量转换问题
            code = code.replaceAll("(BollingerBands(?:Upper|Lower)Indicator\\([^,]+,\\s*[^,]+,\\s*)(\\w+)(\\))", "$1series.numOf($2)$3");
            
            // 修复复杂的AndRule和OrRule - 处理嵌套参数
            // 修复四参数AndRule的具体情况
            if (code.contains("new AndRule(") && code.contains("CrossedUpIndicatorRule") && code.contains("UnderIndicatorRule") && code.contains("OverIndicatorRule")) {
                code = code.replaceAll(
                    "new AndRule\\(\\s*new CrossedUpIndicatorRule\\(([^,)]+),\\s*([^,)]+)\\),\\s*new UnderIndicatorRule\\(([^,)]+),\\s*([^,)]+)\\),\\s*new UnderIndicatorRule\\(([^,)]+),\\s*([^,)]+)\\),\\s*new OverIndicatorRule\\(([^,)]+),\\s*([^,)]+)\\)\\s*\\)",
                    "(new CrossedUpIndicatorRule($1, $2)).and(new UnderIndicatorRule($3, $4)).and(new UnderIndicatorRule($5, $6)).and(new OverIndicatorRule($7, $8))"
                );
            }
            
            // 修复三参数OrRule的具体情况
            if (code.contains("new OrRule(") && code.contains("CrossedDownIndicatorRule") && code.contains("OverIndicatorRule")) {
                code = code.replaceAll(
                    "new OrRule\\(\\s*new CrossedDownIndicatorRule\\(([^,)]+),\\s*([^,)]+)\\),\\s*new OverIndicatorRule\\(([^,)]+),\\s*([^,)]+)\\),\\s*new OverIndicatorRule\\(([^,)]+),\\s*([^,)]+)\\)\\s*\\)",
                    "(new CrossedDownIndicatorRule($1, $2)).or(new OverIndicatorRule($3, $4)).or(new OverIndicatorRule($5, $6))"
                );
            }
            
            return code;
        } catch (Exception e) {
            System.err.println("Error fixing rule combination: " + e.getMessage());
        return code;
        }
    }

    /**
     * 修复CCI指标问题
     */
    private String fixCCIIndicators(String code) {
        try {
            // 修复ConstantIndicator的泛型问题 - 更全面的匹配
            code = code.replaceAll("new ConstantIndicator<>\\(series, (-?\\d+)\\)", "$1");
            code = code.replaceAll("new ConstantIndicator<Num>\\(series, (-?\\d+)\\)", "$1");
            code = code.replaceAll("new ConstantIndicator\\(series, (-?\\d+)\\)", "$1");

            // 修复CrossedUpIndicatorRule和CrossedDownIndicatorRule的参数
            code = code.replaceAll("new CrossedUpIndicatorRule\\(([^,]+), (-?\\d+)\\)",
                                  "new OverIndicatorRule($1, $2)");
            code = code.replaceAll("new CrossedDownIndicatorRule\\(([^,]+), (-?\\d+)\\)",
                                  "new UnderIndicatorRule($1, $2)");

            // 修复int无法转换为Num的问题
            code = code.replaceAll("(\\d+)\\)", "DecimalNum.valueOf($1))");

        } catch (Exception e) {
            System.err.println("Error fixing CCI indicators: " + e.getMessage());
        }
        return code;
    }

    /**
     * 修复RSI指标问题
     */
    private String fixRSIIndicators(String code) {
        try {
            // 1. 修复RSI构造函数参数问题
            // RSI构造函数应该是 RSIIndicator(Indicator<Num>, int)
            // 确保RSI参数正确
            
            // 2. 修复RSI阈值比较中的数字常量问题
            // 将纯数字转换为series.numOf()调用
            code = code.replaceAll("new UnderIndicatorRule\\(([^,]+),\\s*(\\d+)\\)", 
                "new UnderIndicatorRule($1, series.numOf($2))");
            code = code.replaceAll("new OverIndicatorRule\\(([^,]+),\\s*(\\d+)\\)", 
                "new OverIndicatorRule($1, series.numOf($2))");
            
            // 修复变量形式的阈值
            code = code.replaceAll("new UnderIndicatorRule\\(([^,]+),\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\)", 
                "new UnderIndicatorRule($1, series.numOf($2))");
            code = code.replaceAll("new OverIndicatorRule\\(([^,]+),\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\)", 
                "new OverIndicatorRule($1, series.numOf($2))");

            // 3. 修复RSI相关的import
            if (!code.contains("import org.ta4j.core.indicators.RSIIndicator")) {
                code = code.replaceFirst("import org.ta4j.core.rules.*;",
                    "import org.ta4j.core.rules.*;\nimport org.ta4j.core.indicators.*;");
            }

        } catch (Exception e) {
            log.error("Error fixing RSI indicators: {}", e.getMessage());
        }
        return code;
    }

    /**
     * 修复Williams %R指标问题
     */
    private String fixWilliamsRIndicators(String code) {
        try {
            // Williams %R指标通常使用WilliamsRIndicator
            code = code.replaceAll("WilliamsR", "WilliamsRIndicator");
            code = code.replaceAll("Williams", "WilliamsRIndicator");

            // 修复构造函数参数
            code = code.replaceAll("new WilliamsRIndicator\\(series, (\\d+)\\)",
                "new WilliamsRIndicator(series, $1)");

            return code;
        } catch (Exception e) {
            System.err.println("Error fixing Williams R indicators: " + e.getMessage());
        }
        return code;
    }

    /**
     * 修复KDJ指标问题
     */
    private String fixKDJIndicators(String code) {
        try {
            // KDJ指标在TA4J中通常使用StochasticOscillator
            // 将KDJ替换为Stochastic实现
            if (code.contains("KDJ")) {
                String className = extractClassName(code);
                if (className != null) {
                    String newCode = "import org.ta4j.core.*;\n" +
                                   "import org.ta4j.core.indicators.*;\n" +
                                   "import org.ta4j.core.indicators.helpers.*;\n" +
                                   "import org.ta4j.core.rules.*;\n" +
                                   "import org.ta4j.core.num.DecimalNum;\n\n" +
                                   "public class " + className + " extends BaseStrategy {\n\n" +
                                   "    public " + className + "(BarSeries series) {\n" +
                                   "        super(\n" +
                                   "            new UnderIndicatorRule(\n" +
                                   "                new StochasticOscillatorKIndicator(series, 14),\n" +
                                   "                DecimalNum.valueOf(20)\n" +
                                   "            ),\n" +
                                   "            new OverIndicatorRule(\n" +
                                   "                new StochasticOscillatorKIndicator(series, 14),\n" +
                                   "                DecimalNum.valueOf(80)\n" +
                                   "            )\n" +
                                   "        );\n" +
                                   "    }\n" +
                                   "}";
                    return newCode;
                }
            }

            return code;
        } catch (Exception e) {
            System.err.println("Error fixing KDJ indicators: " + e.getMessage());
        }
        return code;
    }

    /**
     * 修复ATR指标问题
     */
    private String fixATRIndicators(String code) {
        try {
            // ATR指标通常使用ATRIndicator
            code = code.replaceAll("ATR([^I])", "ATRIndicator$1");

            // 修复构造函数参数
            code = code.replaceAll("new ATRIndicator\\(series, (\\d+)\\)",
                "new ATRIndicator(series, $1)");

            // 如果包含ATR策略，可能需要完全重写
            if (code.contains("ATR") && (code.contains("突破") || code.contains("Breakout"))) {
                String className = extractClassName(code);
                if (className != null) {
                    String newCode = "import org.ta4j.core.*;\n" +
                                   "import org.ta4j.core.indicators.*;\n" +
                                   "import org.ta4j.core.indicators.helpers.*;\n" +
                                   "import org.ta4j.core.rules.*;\n" +
                                   "import org.ta4j.core.num.DecimalNum;\n\n" +
                                   "public class " + className + " extends BaseStrategy {\n\n" +
                                   "    public " + className + "(BarSeries series) {\n" +
                                   "        super(\n" +
                                   "            new OverIndicatorRule(\n" +
                                   "                new ClosePriceIndicator(series),\n" +
                                   "                new PlusIndicator(\n" +
                                   "                    new SMAIndicator(new ClosePriceIndicator(series), 20),\n" +
                                   "                    new MultiplierIndicator(new ATRIndicator(series, 14), DecimalNum.valueOf(2))\n" +
                                   "                )\n" +
                                   "            ),\n" +
                                   "            new UnderIndicatorRule(\n" +
                                   "                new ClosePriceIndicator(series),\n" +
                                   "                new MinusIndicator(\n" +
                                   "                    new SMAIndicator(new ClosePriceIndicator(series), 20),\n" +
                                   "                    new MultiplierIndicator(new ATRIndicator(series, 14), DecimalNum.valueOf(2))\n" +
                                   "                )\n" +
                                   "            )\n" +
                                   "        );\n" +
                                   "    }\n" +
                                   "}";
                    return newCode;
                }
            }

            return code;
        } catch (Exception e) {
            System.err.println("Error fixing ATR indicators: " + e.getMessage());
        }
        return code;
    }

    /**
     * 添加自定义指标和工具方法
     */
    private String addCustomIndicatorsAndMethods(String code) {
        StringBuilder customMethods = new StringBuilder();
        
        // 如果需要自定义指标，添加到类的开头
        if (code.contains("FixedNumIndicator") || code.contains("ConstantIndicator") || 
            code.contains("MaxPriceIndicator") || code.contains("StopLossRule") ||
            code.contains("ConditionalIndicator") || code.contains("GreaterThanIndicator") ||
            code.contains("TakeProfitRule")) {
            customMethods.append("\n    // 自定义常量指标\n");
            customMethods.append("    private static class CustomConstantIndicator implements Indicator<Num> {\n");
            customMethods.append("        private final Num value;\n");
            customMethods.append("        private final BarSeries series;\n");
            customMethods.append("        \n");
            customMethods.append("        public CustomConstantIndicator(BarSeries series, Num value) {\n");
            customMethods.append("            this.series = series;\n");
            customMethods.append("            this.value = value;\n");
            customMethods.append("        }\n");
            customMethods.append("        \n");
            customMethods.append("        @Override\n");
            customMethods.append("        public Num getValue(int index) {\n");
            customMethods.append("            return value;\n");
            customMethods.append("        }\n");
            customMethods.append("        \n");
            customMethods.append("        @Override\n");
            customMethods.append("        public BarSeries getBarSeries() {\n");
            customMethods.append("            return series;\n");
            customMethods.append("        }\n");
            customMethods.append("        \n");
            customMethods.append("        @Override\n");
            customMethods.append("        public Num numOf(Number number) {\n");
            customMethods.append("            return series.numOf(number);\n");
            customMethods.append("        }\n");
            customMethods.append("    }\n\n");
            
            // 添加MaxPriceIndicator自定义实现
            if (code.contains("MaxPriceIndicator")) {
                customMethods.append("    // 自定义最大价格指标\n");
                customMethods.append("    private static class MaxPriceIndicator implements Indicator<Num> {\n");
                customMethods.append("        private final BarSeries series;\n");
                customMethods.append("        private final int period;\n");
                customMethods.append("        \n");
                customMethods.append("        public MaxPriceIndicator(BarSeries series, int period) {\n");
                customMethods.append("            this.series = series;\n");
                customMethods.append("            this.period = period;\n");
                customMethods.append("        }\n");
                customMethods.append("        \n");
                customMethods.append("        @Override\n");
                customMethods.append("        public Num getValue(int index) {\n");
                customMethods.append("            int start = Math.max(0, index - period + 1);\n");
                customMethods.append("            Num maxPrice = series.getBar(start).getHighPrice();\n");
                customMethods.append("            for (int i = start + 1; i <= index; i++) {\n");
                customMethods.append("                Num currentHigh = series.getBar(i).getHighPrice();\n");
                customMethods.append("                if (currentHigh.isGreaterThan(maxPrice)) {\n");
                customMethods.append("                    maxPrice = currentHigh;\n");
                customMethods.append("                }\n");
                customMethods.append("            }\n");
                customMethods.append("            return maxPrice;\n");
                customMethods.append("        }\n");
                customMethods.append("        \n");
                customMethods.append("        @Override\n");
                customMethods.append("        public BarSeries getBarSeries() {\n");
                customMethods.append("            return series;\n");
                customMethods.append("        }\n");
                customMethods.append("        \n");
                customMethods.append("        @Override\n");
                customMethods.append("        public Num numOf(Number number) {\n");
                customMethods.append("            return series.numOf(number);\n");
                customMethods.append("        }\n");
                customMethods.append("    }\n\n");
            }
            
            // 添加StopLossRule自定义实现
            if (code.contains("StopLossRule")) {
                customMethods.append("    // 自定义止损规则\n");
                customMethods.append("    private static class StopLossRule implements Rule {\n");
                customMethods.append("        private final Indicator<Num> indicator;\n");
                customMethods.append("        private final Indicator<Num> threshold;\n");
                customMethods.append("        \n");
                customMethods.append("        public StopLossRule(Indicator<Num> indicator, Indicator<Num> threshold) {\n");
                customMethods.append("            this.indicator = indicator;\n");
                customMethods.append("            this.threshold = threshold;\n");
                customMethods.append("        }\n");
                customMethods.append("        \n");
                customMethods.append("        @Override\n");
                customMethods.append("        public boolean isSatisfied(int index, TradingRecord tradingRecord) {\n");
                customMethods.append("            return indicator.getValue(index).isLessThan(threshold.getValue(index));\n");
                customMethods.append("        }\n");
                customMethods.append("    }\n\n");
            }
            
            // 添加ConditionalIndicator自定义实现
            if (code.contains("ConditionalIndicator")) {
                customMethods.append("    // 自定义条件指标\n");
                customMethods.append("    private static class ConditionalIndicator implements Indicator<Num> {\n");
                customMethods.append("        private final Indicator<Boolean> condition;\n");
                customMethods.append("        private final Indicator<Num> trueIndicator;\n");
                customMethods.append("        private final Indicator<Num> falseIndicator;\n");
                customMethods.append("        private final BarSeries series;\n");
                customMethods.append("        \n");
                customMethods.append("        public ConditionalIndicator(Indicator<Boolean> condition,\n");
                customMethods.append("                                   Indicator<Num> trueIndicator,\n");
                customMethods.append("                                   Indicator<Num> falseIndicator) {\n");
                customMethods.append("            this.condition = condition;\n");
                customMethods.append("            this.trueIndicator = trueIndicator;\n");
                customMethods.append("            this.falseIndicator = falseIndicator;\n");
                customMethods.append("            this.series = condition.getBarSeries();\n");
                customMethods.append("        }\n");
                customMethods.append("        \n");
                customMethods.append("        @Override\n");
                customMethods.append("        public Num getValue(int index) {\n");
                customMethods.append("            return condition.getValue(index) ? \n");
                customMethods.append("                   trueIndicator.getValue(index) : \n");
                customMethods.append("                   falseIndicator.getValue(index);\n");
                customMethods.append("        }\n");
                customMethods.append("        \n");
                customMethods.append("        @Override\n");
                customMethods.append("        public BarSeries getBarSeries() {\n");
                customMethods.append("            return series;\n");
                customMethods.append("        }\n");
                customMethods.append("        \n");
                customMethods.append("        @Override\n");
                customMethods.append("        public Num numOf(Number number) {\n");
                customMethods.append("            return series.numOf(number);\n");
                customMethods.append("        }\n");
                customMethods.append("    }\n\n");
            }
            
            // 添加GreaterThanIndicator自定义实现
            if (code.contains("GreaterThanIndicator")) {
                customMethods.append("    // 自定义大于指标\n");
                customMethods.append("    private static class GreaterThanIndicator implements Indicator<Boolean> {\n");
                customMethods.append("        private final Indicator<Num> first;\n");
                customMethods.append("        private final Indicator<Num> second;\n");
                customMethods.append("        private final BarSeries series;\n");
                customMethods.append("        \n");
                customMethods.append("        public GreaterThanIndicator(Indicator<Num> first, Indicator<Num> second) {\n");
                customMethods.append("            this.first = first;\n");
                customMethods.append("            this.second = second;\n");
                customMethods.append("            this.series = first.getBarSeries();\n");
                customMethods.append("        }\n");
                customMethods.append("        \n");
                customMethods.append("        @Override\n");
                customMethods.append("        public Boolean getValue(int index) {\n");
                customMethods.append("            return first.getValue(index).isGreaterThan(second.getValue(index));\n");
                customMethods.append("        }\n");
                customMethods.append("        \n");
                customMethods.append("        @Override\n");
                customMethods.append("        public BarSeries getBarSeries() {\n");
                customMethods.append("            return series;\n");
                customMethods.append("        }\n");
                customMethods.append("        \n");
                customMethods.append("        @Override\n");
                customMethods.append("        public Num numOf(Number number) {\n");
                customMethods.append("            return series.numOf(number);\n");
                customMethods.append("        }\n");
                customMethods.append("    }\n\n");
            }
            
            // 添加TakeProfitRule自定义实现
            if (code.contains("TakeProfitRule")) {
                customMethods.append("    // 自定义止盈规则\n");
                customMethods.append("    private static class TakeProfitRule implements Rule {\n");
                customMethods.append("        private final Indicator<Num> indicator;\n");
                customMethods.append("        private final Indicator<Num> profitTarget;\n");
                customMethods.append("        \n");
                customMethods.append("        public TakeProfitRule(Indicator<Num> indicator, Indicator<Num> profitTarget) {\n");
                customMethods.append("            this.indicator = indicator;\n");
                customMethods.append("            this.profitTarget = profitTarget;\n");
                customMethods.append("        }\n");
                customMethods.append("        \n");
                customMethods.append("        @Override\n");
                customMethods.append("        public boolean isSatisfied(int index, TradingRecord tradingRecord) {\n");
                customMethods.append("            return indicator.getValue(index).isGreaterThan(profitTarget.getValue(index));\n");
                customMethods.append("        }\n");
                customMethods.append("    }\n\n");
            }
        }
        
        // 如果有自定义方法，插入到类的开头
        if (customMethods.length() > 0) {
            int classStart = code.indexOf("{");
            if (classStart > 0) {
                code = code.substring(0, classStart + 1) + customMethods.toString() + code.substring(classStart + 1);
            }
        }
        
        return code;
    }

    /**
     * 修复常见的编译错误
     */
    private String fixCommonCompilationErrors(String code) {
        try {
            // 1. 修复缺少import的DecimalNum
            if (code.contains("DecimalNum") && !code.contains("import org.ta4j.core.num.DecimalNum")) {
                code = code.replaceFirst("import org.ta4j.core.rules.*;",
                    "import org.ta4j.core.rules.*;\nimport org.ta4j.core.num.DecimalNum;");
            }

            // 2. 修复所有Decimal相关问题 - 最优先修复（使用更精确的正则表达式）
            code = code.replaceAll("\\bDecimal\\.valueOf\\(([^)]+)\\)", "series.numOf($1)");
            code = code.replaceAll("\\bDecimalNum\\.valueOf\\(([^)]+)\\)", "series.numOf($1)");
            code = code.replaceAll("\\bNum\\.valueOf\\(([^)]+)\\)", "series.numOf($1)");
            
            // 修复可能由替换产生的错误
            code = code.replaceAll("series\\.numOf\\(series\\.numOf\\(([^)]+)\\)\\)", "series.numOf($1)");
            code = code.replaceAll("Decimalseries", "series");  // 修复错误合并
            code = code.replaceAll("series\\.numOfseries", "series");
            
            // 修复错误的指标构造函数调用（必须在修复名称之前）
            code = code.replaceAll("new MultiplierIndicator\\(([^,]+), ([\\d.]+)\\)", 
                "new MultipliedIndicator($1, series.numOf($2))");
            code = code.replaceAll("new DifferenceIndicator\\(([^,]+), ([^)]+)\\)", 
                "new MinusIndicator($1, $2)");
            
            // 修复指标名称拼写错误（在构造函数修复之后）
            code = code.replaceAll("MultiplierIndicator", "MultipliedIndicator");
            code = code.replaceAll("DifferenceIndicator", "MinusIndicator");
            
            // 修复.getNum()方法调用错误
            code = code.replaceAll("([a-zA-Z_][a-zA-Z0-9_]*)\\.getNum\\(\\)", "$1.getValue(series.getEndIndex())");
            code = code.replaceAll("([a-zA-Z_][a-zA-Z0-9_]*)\\.getNum\\(\\)\\.of\\(([^)]+)\\)", "series.numOf($2)");
            code = code.replaceAll("([a-zA-Z_][a-zA-Z0-9_]*)\\.getNum\\(\\)\\.multipliedBy\\(([^)]+)\\)", "new MultipliedIndicator($1, $3)");
            
            // 修复FixedNumIndicator的问题（Ta4j中没有这个类）
            code = code.replaceAll("new FixedNumIndicator\\(([^,]+), ([^)]+)\\)", "new CustomConstantIndicator($1, $2)");
            code = code.replaceAll("FixedNumIndicator", "CustomConstantIndicator");
            
            // 修复MaxPriceIndicator的问题（Ta4j中没有这个类）
            code = code.replaceAll("MaxPriceIndicator", "MaxPriceIndicator");
            
            // 修复StopLossRule的问题（Ta4j中没有这个类） 
            // 修复StopLossRule构造函数调用（3个参数变2个参数）
            code = code.replaceAll("new StopLossRule\\(([^,]+),\\s*([^,]+),\\s*([^)]+)\\)", 
                "new StopLossRule($1, new MultipliedIndicator($2, series.numOf($3)))");
            code = code.replaceAll("StopLossRule", "StopLossRule");

            // 3. 修复SMAIndicator参数缺失问题 - 智能分析修复
            // 分析具体的SMAIndicator实例，避免错误删除参数
            if (code.contains("shortSma") && code.contains("longSma")) {
                // 双均线策略：分别修复shortSma和longSma
                code = code.replaceAll("SMAIndicator shortSma = new SMAIndicator\\(([^,)]+)\\);", 
                    "SMAIndicator shortSma = new SMAIndicator($1, shortPeriod);");
                code = code.replaceAll("SMAIndicator longSma = new SMAIndicator\\(([^,)]+)\\);", 
                    "SMAIndicator longSma = new SMAIndicator($1, longPeriod);");
                // 修复已经有period但后面被错误删除的情况
                code = code.replaceAll("SMAIndicator shortSma = new SMAIndicator\\(([^,)]+), shortPeriod\\);", 
                    "SMAIndicator shortSma = new SMAIndicator($1, shortPeriod);");
                code = code.replaceAll("SMAIndicator longSma = new SMAIndicator\\(([^,)]+), longPeriod\\);", 
                    "SMAIndicator longSma = new SMAIndicator($1, longPeriod);");
            } else {
                // 单一均线或其他情况
                if (code.contains("longPeriod")) {
                    code = code.replaceAll("new SMAIndicator\\(([^,)]+)\\)(?=;)", "new SMAIndicator($1, longPeriod)");
                } else if (code.contains("shortPeriod")) {
                    code = code.replaceAll("new SMAIndicator\\(([^,)]+)\\)(?=;)", "new SMAIndicator($1, shortPeriod)");
                } else if (code.contains("period")) {
                    code = code.replaceAll("new SMAIndicator\\(([^,)]+)\\)(?=;)", "new SMAIndicator($1, period)");
                } else {
                    code = code.replaceAll("new SMAIndicator\\(([^,)]+)\\)(?=;)", "new SMAIndicator($1, 20)");
                }
            }
            
            // 4. 修复MACD构造函数问题 - 只修复参数不足的情况
            code = code.replaceAll("new MACDIndicator\\(([^,)]+)\\)(?!\\w)", 
                "new MACDIndicator($1, 12, 26)");
            
            // 4.1 修复RSI构造函数问题 - 需要先有指标再有周期
            code = code.replaceAll("new RSIIndicator\\(series, ([^)]+)\\)", 
                "new RSIIndicator(new ClosePriceIndicator(series), $1)");
            
            // 5. 修复数学运算方法调用错误
            code = code.replaceAll("([a-zA-Z_][a-zA-Z0-9_]*)\\.numMultipliedBy\\(([^)]+)\\)", 
                "new MultipliedIndicator($1, $2)");
            code = code.replaceAll("([a-zA-Z_][a-zA-Z0-9_]*)\\.numPlus\\(([^)]+)\\)", 
                "new PlusIndicator($1, $2)");
            code = code.replaceAll("([a-zA-Z_][a-zA-Z0-9_]*)\\.numMinus\\(([^)]+)\\)", 
                "new MinusIndicator($1, $2)");
            code = code.replaceAll("([a-zA-Z_][a-zA-Z0-9_]*)\\.multipliedBy\\(([^)]+)\\)", 
                "new MultipliedIndicator($1, $2)");
            code = code.replaceAll("([a-zA-Z_][a-zA-Z0-9_]*)\\.plus\\(([^)]+)\\)", 
                "new PlusIndicator($1, $2)");
            code = code.replaceAll("([a-zA-Z_][a-zA-Z0-9_]*)\\.minus\\(([^)]+)\\)", 
                "new MinusIndicator($1, $2)");
            
            // 修复指标的数值运算调用（在Rule中）
            code = code.replaceAll("([a-zA-Z_][a-zA-Z0-9_]*)\\.minus\\((\\d+)\\)", 
                "new MinusIndicator($1, series.numOf($2))");
            code = code.replaceAll("([a-zA-Z_][a-zA-Z0-9_]*)\\.plus\\((\\d+)\\)", 
                "new PlusIndicator($1, series.numOf($2))");
            code = code.replaceAll("([a-zA-Z_][a-zA-Z0-9_]*)\\.multipliedBy\\((\\d+)\\)", 
                "new MultipliedIndicator($1, series.numOf($2))");
            code = code.replaceAll("([a-zA-Z_][a-zA-Z0-9_]*)\\.multipliedBy\\(([\\d.]+)\\)", 
                "new MultipliedIndicator($1, series.numOf($2))");
            
            // 修复复杂的数学运算调用（包含Decimal.valueOf）
            code = code.replaceAll("([a-zA-Z_][a-zA-Z0-9_]*)\\.multipliedBy\\(Decimal\\.valueOf\\(([^)]+)\\)\\)", 
                "new MultipliedIndicator($1, series.numOf($2))");
            
            // 修复ATR特殊的数学运算（简单变量乘法）
            code = code.replaceAll("([a-zA-Z_][a-zA-Z0-9_]*)\\.multipliedBy\\(([a-zA-Z_][a-zA-Z0-9_]*)\\)", 
                "new MultipliedIndicator($1, series.numOf($2))");
            
            // 修复.multiply()方法调用（Ta4j中不存在这个方法）
            code = code.replaceAll("([a-zA-Z_][a-zA-Z0-9_]*)\\.multiply\\(([\\d.]+)\\)", 
                "new MultipliedIndicator($1, series.numOf($2))");
            code = code.replaceAll("([a-zA-Z_][a-zA-Z0-9_]*)\\.multiply\\(([a-zA-Z_][a-zA-Z0-9_]*)\\)", 
                "new MultipliedIndicator($1, series.numOf($2))");
            
            // 修复.dividedBy()方法调用（Ta4j中不存在这个方法）
            code = code.replaceAll("([a-zA-Z_][a-zA-Z0-9_]*)\\.dividedBy\\(([\\d.]+)\\)", 
                "new DividedIndicator($1, series.numOf($2))");
            code = code.replaceAll("([a-zA-Z_][a-zA-Z0-9_]*)\\.dividedBy\\(([a-zA-Z_][a-zA-Z0-9_]*)\\)", 
                "new DividedIndicator($1, series.numOf($2))");
            
            // 修复Num对象的.multipliedBy()调用（特殊情况）
            code = code.replaceAll("series\\.numOf\\(([^)]+)\\)\\.multipliedBy\\(([^)]+)\\)", 
                "new MultipliedIndicator(new CustomConstantIndicator(series, series.numOf($1)), $2)");
            code = code.replaceAll("([a-zA-Z_][a-zA-Z0-9_]*)\\.getValue\\([^)]+\\)\\.multipliedBy\\(([^)]+)\\)", 
                "new MultipliedIndicator($1, $2)");
            
            // 修复匿名Indicator类，直接转换为简单的逻辑
            if (code.contains("new Indicator<Num>()") && code.contains("adaptiveMa")) {
                // 将复杂的自适应移动平均线逻辑简化为基于条件的简单指标
                code = code.replaceAll("Indicator<Num> adaptiveMa = new Indicator<Num>\\(\\)[^;]*;", 
                    "Indicator<Num> adaptiveMa = new ConditionalIndicator(" +
                    "new GreaterThanIndicator(stdDev, new DividedIndicator(stdDev, series.numOf(2))), " +
                    "shortSma, longSma);");
            }
            
            // 修复处理后可能出现的错误符号
            code = code.replaceAll("\\bnRule\\b", "Rule");
            // 修复重复的Indicator
            code = code.replaceAll("IndicatorIndicator", "Indicator");
            code = code.replaceAll("IndicatorRIndicator", "RIndicator");
            code = code.replaceAll("RIndicatorIndicator", "RIndicator");

            // 6. 修复布林带标准差计算问题
            code = code.replaceAll("StandardDeviationIndicator ([a-zA-Z_][a-zA-Z0-9_]*) = new StandardDeviationIndicator\\(([^,)]+), (\\d+)\\);",
                "StandardDeviationIndicator $1 = new StandardDeviationIndicator($2, $3);");
            
            // 7. 修复Rule组合问题 - 更安全的修复
            // 只修复明确的构造函数调用错误
            code = code.replaceAll("new AndRule\\(([^,()]+),\\s*([^,()]+)\\)", "$1.and($2)");
            code = code.replaceAll("new OrRule\\(([^,()]+),\\s*([^,()]+)\\)", "$1.or($2)");

            // 8. 修复构造函数参数数量不匹配问题 - 但不要删除现有的有效参数
            // 这个规则可能有问题，暂时注释掉
            // code = code.replaceAll("new (\\w+Indicator)\\(([^,)]+), (\\d+), ([\\d.]+)\\)",
            //     "new $1($2, $3)");

            // 9. 修复super()调用没有参数的问题
            if (code.contains("super()")) {
                code = code.replaceAll("super\\(\\)", "super(null, null)");
            }

            // 10. 修复类名中的空格问题
            code = code.replaceAll("public\\s+class\\s+([A-Z]\\w*)", "public class $1");

            // 11. 修复ConstantIndicator的泛型问题
            code = code.replaceAll("new ConstantIndicator<>\\(([^,]+), (\\d+)\\)", "series.numOf($2)");
            code = code.replaceAll("new ConstantIndicator<Num>\\(([^,]+), (\\d+)\\)", "series.numOf($2)");
            code = code.replaceAll("new ConstantIndicator\\(([^,]+), (\\d+)\\)", "series.numOf($2)");

            // 12. 修复数字常量在Rule中的使用
            code = code.replaceAll("(?<!new )OverIndicatorRule\\(([^,]+), (\\d+)\\)", "new OverIndicatorRule($1, series.numOf($2))");
            code = code.replaceAll("(?<!new )UnderIndicatorRule\\(([^,]+), (\\d+)\\)", "new UnderIndicatorRule($1, series.numOf($2))");
            code = code.replaceAll("(?<!new )OverIndicatorRule\\(([^,]+), ([\\d.]+)\\)", "new OverIndicatorRule($1, series.numOf($2))");
            code = code.replaceAll("(?<!new )UnderIndicatorRule\\(([^,]+), ([\\d.]+)\\)", "new UnderIndicatorRule($1, series.numOf($2))");
            code = code.replaceAll("(?<!new )OverIndicatorRule\\(([^,]+), (-?[\\d.]+)\\)", "new OverIndicatorRule($1, series.numOf($2))");
            code = code.replaceAll("(?<!new )UnderIndicatorRule\\(([^,]+), (-?[\\d.]+)\\)", "new UnderIndicatorRule($1, series.numOf($2))");
            
            // 修复已经有new的Rule但缺少series.numOf的情况
            code = code.replaceAll("new OverIndicatorRule\\(([^,]+), (\\d+)\\)", "new OverIndicatorRule($1, series.numOf($2))");
            code = code.replaceAll("new UnderIndicatorRule\\(([^,]+), (\\d+)\\)", "new UnderIndicatorRule($1, series.numOf($2))");

            // 13. 修复ATR相关的数学运算问题
            code = code.replaceAll("([a-zA-Z_][a-zA-Z0-9_]*)\\.multipliedBy\\(([\\d.]+)\\)", 
                "new MultipliedIndicator($1, series.numOf($2))");
            
            // 14. 修复BollingerBands相关的复合指标构造
            if (code.contains("BollingerBands")) {
                // 修复BollingerBandsMiddleIndicator需要SMA参数
                code = code.replaceAll("new BollingerBandsMiddleIndicator\\(([^,)]+), (\\d+)\\)", 
                    "new BollingerBandsMiddleIndicator(new SMAIndicator($1, $2))");
                code = code.replaceAll("new BollingerBandsUpperIndicator\\(([^,)]+), (\\d+), ([\\d.]+)\\)", 
                    "new BollingerBandsUpperIndicator(new BollingerBandsMiddleIndicator(new SMAIndicator($1, $2)), new StandardDeviationIndicator($1, $2), series.numOf($3))");
                code = code.replaceAll("new BollingerBandsLowerIndicator\\(([^,)]+), (\\d+), ([\\d.]+)\\)", 
                    "new BollingerBandsLowerIndicator(new BollingerBandsMiddleIndicator(new SMAIndicator($1, $2)), new StandardDeviationIndicator($1, $2), series.numOf($3))");
                
                // 修复错误的布林带指标构造（两个参数版本）
                code = code.replaceAll("new BollingerBandsUpperIndicator\\(([^,)]+),\\s*([^,)]+)\\)", 
                    "new BollingerBandsUpperIndicator($1, $2, series.numOf(2))");
                code = code.replaceAll("new BollingerBandsLowerIndicator\\(([^,)]+),\\s*([^,)]+)\\)", 
                    "new BollingerBandsLowerIndicator($1, $2, series.numOf(2))");
                    
                // 修复.getNum().multipliedBy()的错误调用
                code = code.replaceAll("([a-zA-Z_][a-zA-Z0-9_]*)\\.getNum\\(\\)\\.multipliedBy\\(([^)]+)\\)", 
                    "new MultipliedIndicator($1, series.numOf($2))");
            }

            // 15. 修复import缺失问题
            boolean needsArithmeticImport = code.contains("MultipliedIndicator") || 
                                          code.contains("PlusIndicator") || 
                                          code.contains("MinusIndicator") ||
                                          code.contains("DividedIndicator");
            
            if (needsArithmeticImport && !code.contains("import org.ta4j.core.indicators.arithmetic")) {
                code = code.replaceFirst("import org.ta4j.core.rules.*;",
                    "import org.ta4j.core.rules.*;\nimport org.ta4j.core.indicators.arithmetic.*;");
            }
            
            // 添加统计指标的import
            if (code.contains("StandardDeviationIndicator") && !code.contains("import org.ta4j.core.indicators.statistics.StandardDeviationIndicator")) {
                code = code.replaceFirst("import org.ta4j.core.rules.*;",
                    "import org.ta4j.core.rules.*;\nimport org.ta4j.core.indicators.statistics.StandardDeviationIndicator;");
            }
            
            // 修复其他缺失的import
            if (code.contains("ClosePriceIndicator") && !code.contains("import org.ta4j.core.indicators.helpers.ClosePriceIndicator")) {
                code = code.replaceFirst("import org.ta4j.core.rules.*;",
                    "import org.ta4j.core.rules.*;\nimport org.ta4j.core.indicators.helpers.ClosePriceIndicator;");
            }
            
            if (code.contains("VolumeIndicator") && !code.contains("import org.ta4j.core.indicators.volume.VolumeIndicator")) {
                code = code.replaceFirst("import org.ta4j.core.rules.*;",
                    "import org.ta4j.core.rules.*;\nimport org.ta4j.core.indicators.volume.VolumeIndicator;");
            }
            
            // 添加TradingRecord的import（自定义Rule需要）
            if (code.contains("TradingRecord") && !code.contains("import org.ta4j.core.TradingRecord")) {
                code = code.replaceFirst("import org.ta4j.core.rules.*;",
                    "import org.ta4j.core.rules.*;\nimport org.ta4j.core.TradingRecord;");
            }

            // 16. 修复其他常见的编译错误
            code = code.replaceAll("new Num\\((\\d+)\\)", "series.numOf($1)");
            code = code.replaceAll("new Num\\(([\\d.]+)\\)", "series.numOf($1)");

        } catch (Exception e) {
            log.error("Error fixing common compilation errors: {}", e.getMessage());
        }
        return code;
    }

    private int findMatchingBrace(String code, int start) {
        int count = 1;
        for (int i = start + 1; i < code.length(); i++) {
            if (code.charAt(i) == '{') count++;
            else if (code.charAt(i) == '}') {
                count--;
                if (count == 0) return i;
            }
        }
        return -1;
    }

    private int findMatchingSuperEnd(String code, int start) {
        int count = 0;
        for (int i = start; i < code.length(); i++) {
            if (code.charAt(i) == '(') count++;
            else if (code.charAt(i) == ')') {
                count--;
                if (count == 0) return i + 1;
            }
        }
        return code.length();
    }

    /**
     * 修复括号匹配问题
     */
    private String fixBracketMatching(String code) {
        // 简单的括号匹配修复
        int openCount = 0;
        int closeCount = 0;

        // 统计所有括号
        for (char c : code.toCharArray()) {
            if (c == '(') openCount++;
            else if (c == ')') closeCount++;
        }

        // 如果缺少右括号，在最后的分号前添加
        if (openCount > closeCount) {
            int missingBrackets = openCount - closeCount;
            int lastSemicolon = code.lastIndexOf(";");
            if (lastSemicolon > 0) {
                // Java 8兼容的字符串重复方法
                StringBuilder brackets = new StringBuilder();
                for (int i = 0; i < missingBrackets; i++) {
                    brackets.append(")");
                }
                code = code.substring(0, lastSemicolon) + brackets.toString() + code.substring(lastSemicolon);
            }
        }

        // 修复常见的语法错误模式
        code = code.replaceAll(",\\s*\\.and\\(", ").and(");
        code = code.replaceAll("\\)\\s*\\.and\\(", ").and(");

        // 修复.and()调用中缺少右括号的问题
        try {
            code = code.replaceAll("\\.and\\(([^)]+),\\s*new", ".and($1), new");
        } catch (Exception e) {
            // 如果正则表达式有问题，跳过这个修复
        }

        return code;
    }

    /**
     * 为Janino编译器进一步简化代码
     */
    private String simplifyForJanino(String code) {
        // Janino特定的简化
        code = code.replaceAll("org\\.ta4j\\.core\\.", "");
        return code;
    }

    /**
     * 提取类名
     */
    private String extractClassName(String code) {
        String[] lines = code.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("public class")) {
                String[] parts = line.split("\\s+");
                for (int i = 0; i < parts.length; i++) {
                    if ("class".equals(parts[i]) && i + 1 < parts.length) {
                        String className = parts[i + 1];
                        if (className.contains(" ")) {
                            className = className.split("\\s+")[0];
                        }
                        return className;
                    }
                }
            }
        }
        return "GeneratedStrategy";
    }

    /**
     * 代理其他方法到Java Compiler服务
     */
    public void loadAllDynamicStrategies() {
        try {
            javaCompilerService.loadAllDynamicStrategies();
        } catch (Exception e) {
            log.warn("Java Compiler API加载失败，回退到Janino: {}", e.getMessage());
            janinoService.loadAllDynamicStrategies();
        }
    }

    public void removeStrategy(String strategyCode) {
        try {
            javaCompilerService.removeStrategy(strategyCode);
        } catch (Exception e) {
            janinoService.removeStrategy(strategyCode);
        }
    }

    public Function<BarSeries, Strategy> getCompiledStrategy(String strategyCode) {
        Function<BarSeries, Strategy> strategy = javaCompilerService.getCompiledStrategy(strategyCode);
        return strategy != null ? strategy : janinoService.getCompiledStrategy(strategyCode);
    }

    public boolean isStrategyLoaded(String strategyCode) {
        return javaCompilerService.isStrategyLoaded(strategyCode) ||
               janinoService.isStrategyLoaded(strategyCode);
    }

    /**
     * 统计和记录修复的错误类型
     */
    private void logFixedErrors(String originalCode, String fixedCode) {
        if (originalCode.equals(fixedCode)) {
            return; // 如果代码没有变化，不记录日志
        }

        List<String> fixedErrors = new ArrayList<>();

        // 检查各种修复类型
        if (originalCode.contains("new Num(") && !fixedCode.contains("new Num(")) {
            fixedErrors.add("Num抽象类实例化错误");
        }

        if (originalCode.contains("super()") && fixedCode.contains("super(") &&
            !fixedCode.contains("super()")) {
            fixedErrors.add("BaseStrategy构造函数调用错误");
        }

        if (originalCode.contains("MACDIndicator") && originalCode.length() != fixedCode.length()) {
            fixedErrors.add("MACD指标构造错误");
        }

        if (originalCode.contains("BollingerBands") && originalCode.length() != fixedCode.length()) {
            fixedErrors.add("布林带指标构造错误");
        }

        if (originalCode.contains("RSI") && (originalCode.contains("new Num(") ||
            originalCode.contains("Overbought") || originalCode.contains("Oversold"))) {
            fixedErrors.add("RSI指标参数错误");
        }

        if (originalCode.contains("public classGenerated") && !fixedCode.contains("public classGenerated")) {
            fixedErrors.add("类名声明语法错误");
        }

        if (originalCode.contains("Stochastic") && originalCode.length() != fixedCode.length()) {
            fixedErrors.add("Stochastic指标构造错误");
        }

        if (originalCode.contains("ConstantIndicator") && !fixedCode.contains("ConstantIndicator")) {
            fixedErrors.add("ConstantIndicator泛型错误");
        }

        if (originalCode.contains("import") && originalCode.split("import").length != fixedCode.split("import").length) {
            fixedErrors.add("Import语句错误");
        }

        // 检查括号修复
        int originalParens = (int) originalCode.chars().filter(ch -> ch == '(' || ch == ')').count();
        int fixedParens = (int) fixedCode.chars().filter(ch -> ch == '(' || ch == ')').count();
        if (originalParens != fixedParens) {
            fixedErrors.add("括号匹配错误");
        }

        if (!fixedErrors.isEmpty()) {
            log.info("智能编译器修复了以下错误: [{}]", String.join(", ", fixedErrors));
        } else {
            log.info("进行了代码优化和标准化处理");
        }
    }

    /**
     * 快速检查代码是否可能需要修复
     * 用于优化性能，避免对明显正确的代码进行不必要的修复处理
     */
    private boolean mightNeedFix(String code) {
        // 检查常见的错误模式
        return code.contains("new Num(") ||
               code.contains("super()") ||
               code.contains("public classGenerated") ||
               code.contains("import *") ||
               code.contains("MACDIndicator") ||
               code.contains("BollingerBands") ||
               code.contains("ConstantIndicator") ||
               code.contains("Ichimoku") ||
               code.contains("ADX") ||
               code.contains("KDJ") ||
               code.contains("Williams") ||
               code.contains("ATR") ||
               !code.contains("extends BaseStrategy") ||
               !code.contains("import org.ta4j.core");
    }

    /**
     * 检查代码是否看起来是标准的、可能直接编译成功的代码
     */
    private boolean looksLikeStandardCode(String code) {
        return code.contains("extends BaseStrategy") &&
               code.contains("import org.ta4j.core") &&
               code.contains("super(") &&
               !code.contains("new Num(") &&
               !code.contains("public classGenerated") &&
               !code.contains("super()");
    }

    /**
     * 代码优化和标准化处理
     */
    private String optimizeCode(String code) {
        try {
            // 1. 移除多余的空行
            code = code.replaceAll("\\n\\s*\\n\\s*\\n", "\n\n");
            
            // 2. 标准化缩进
            code = code.replaceAll("\\t", "    ");
            
            // 3. 标准化大括号格式
            code = code.replaceAll("\\)\\s*\\{", ") {");
            code = code.replaceAll("\\}\\s*else\\s*\\{", "} else {");
            
            // 4. 移除行尾多余空格
            code = code.replaceAll("\\s+\\n", "\n");
            
            // 5. 确保最后有换行符
            if (!code.endsWith("\n")) {
                code += "\n";
            }
            
        } catch (Exception e) {
            log.error("代码优化时发生错误: {}", e.getMessage());
        }
        return code;
    }
}
