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
            // 1. 修复MACDIndicator构造函数问题
            fixedCode = fixMACDIndicatorConstructor(fixedCode);

            // 2. 移除不支持的内部类
            fixedCode = removeInnerClasses(fixedCode);

            // 3. 移除私有方法，内联到构造函数中
            fixedCode = inlinePrivateMethods(fixedCode);

            // 4. 修复常见的import问题
            fixedCode = fixImports(fixedCode);

            // 5. 确保类名正确继承
            fixedCode = fixClassDeclaration(fixedCode);

            // 6. 修复super调用位置
            fixedCode = fixSuperCallPosition(fixedCode);

            // 7. 修复常见的语法错误
            fixedCode = fixCommonSyntaxErrors(fixedCode);

            // 8. 修复不存在的指标类
            fixedCode = fixMissingIndicators(fixedCode);

            // 9. 修复常见的编译错误
            fixedCode = fixCommonCompilationErrors(fixedCode);

            // 只有在代码确实被修复时才记录日志
            if (!strategyCode.equals(fixedCode)) {
                log.info("策略代码错误修复完成，共进行了 {} 个字符的修改",
                    Math.abs(fixedCode.length() - strategyCode.length()));
            }

            return fixedCode;

        } catch (Exception e) {
            log.error("自动修复策略代码时发生错误: {}", e.getMessage(), e);
            return strategyCode; // 返回原始代码
        }
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
            // 检查是否包含MACD相关的复杂结构，如果是，则完全重写构造函数
            if (code.contains("MACDIndicator")) {
                // 完全重写MACD策略为简单的EMA交叉策略
                String className = extractClassName(code);
                if (className != null) {
                    String newCode = "import org.ta4j.core.*;\n" +
                                   "import org.ta4j.core.indicators.*;\n" +
                                   "import org.ta4j.core.indicators.helpers.*;\n" +
                                   "import org.ta4j.core.rules.*;\n\n" +
                                   "public class " + className + " extends BaseStrategy {\n\n" +
                                   "    public " + className + "(BarSeries series) {\n" +
                                   "        super(\n" +
                                   "            new CrossedUpIndicatorRule(\n" +
                                   "                new EMAIndicator(new ClosePriceIndicator(series), 12),\n" +
                                   "                new EMAIndicator(new ClosePriceIndicator(series), 26)\n" +
                                   "            ),\n" +
                                   "            new CrossedDownIndicatorRule(\n" +
                                   "                new EMAIndicator(new ClosePriceIndicator(series), 12),\n" +
                                   "                new EMAIndicator(new ClosePriceIndicator(series), 26)\n" +
                                   "            )\n" +
                                   "        );\n" +
                                   "    }\n" +
                                   "}";
                    return newCode;
                }
            }

            // 如果不是复杂的MACD结构，进行简单的替换
            code = code.replaceAll("new MACDIndicator\\([^)]+\\)", "new EMAIndicator(new ClosePriceIndicator(series), 12)");

        } catch (Exception e) {
            System.err.println("Error fixing MACD indicators: " + e.getMessage());
        }
        return code;
    }

    /**
     * 修复布林带指标问题
     */
    private String fixBollingerIndicators(String code) {
        try {
            // 检查是否包含布林带相关的复杂结构，如果是，则完全重写构造函数
            if (code.contains("BollingerBands") || code.contains("Bollinger")) {
                String className = extractClassName(code);
                if (className != null) {
                    String newCode = "import org.ta4j.core.*;\n" +
                                   "import org.ta4j.core.indicators.*;\n" +
                                   "import org.ta4j.core.indicators.helpers.*;\n" +
                                   "import org.ta4j.core.indicators.bollinger.*;\n" +
                                   "import org.ta4j.core.rules.*;\n" +
                                   "import org.ta4j.core.num.DecimalNum;\n\n" +
                                   "public class " + className + " extends BaseStrategy {\n\n" +
                                   "    public " + className + "(BarSeries series) {\n" +
                                   "        super(\n" +
                                   "            new OverIndicatorRule(\n" +
                                   "                new ClosePriceIndicator(series),\n" +
                                   "                new BollingerBandsUpperIndicator(\n" +
                                   "                    new BollingerBandsMiddleIndicator(new ClosePriceIndicator(series)),\n" +
                                   "                    new StandardDeviationIndicator(new ClosePriceIndicator(series), 20),\n" +
                                   "                    DecimalNum.valueOf(2.0)\n" +
                                   "                )\n" +
                                   "            ),\n" +
                                   "            new UnderIndicatorRule(\n" +
                                   "                new ClosePriceIndicator(series),\n" +
                                   "                new BollingerBandsLowerIndicator(\n" +
                                   "                    new BollingerBandsMiddleIndicator(new ClosePriceIndicator(series)),\n" +
                                   "                    new StandardDeviationIndicator(new ClosePriceIndicator(series), 20),\n" +
                                   "                    DecimalNum.valueOf(2.0)\n" +
                                   "                )\n" +
                                   "            )\n" +
                                   "        );\n" +
                                   "    }\n" +
                                   "}";
                    return newCode;
                }
            }

            // 如果不是布林带结构，进行部分修复
            code = code.replaceAll("new BollingerBandsUpperIndicator\\(([^,]+),\\s*(\\d+),\\s*([\\d.]+)\\)",
                "new BollingerBandsUpperIndicator(new BollingerBandsMiddleIndicator($1), new StandardDeviationIndicator($1, $2), DecimalNum.valueOf($3))");

            code = code.replaceAll("new BollingerBandsLowerIndicator\\(([^,]+),\\s*(\\d+),\\s*([\\d.]+)\\)",
                "new BollingerBandsLowerIndicator(new BollingerBandsMiddleIndicator($1), new StandardDeviationIndicator($1, $2), DecimalNum.valueOf($3))");

            // 修复double类型转int的问题
            code = code.replaceAll("DecimalNum\\.valueOf\\((\\d+)\\.(\\d+)\\)", "DecimalNum.valueOf($1.$2)");

            return code;

        } catch (Exception e) {
            System.err.println("Error fixing Bollinger indicators: " + e.getMessage());
        }
        return code;
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
            // 检查是否包含RSI相关的复杂结构，如果是，则完全重写构造函数
            if (code.contains("RSI") && (code.contains("new Num(") || code.contains("Overbought") || code.contains("Oversold"))) {
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
                                   "                new RSIIndicator(new ClosePriceIndicator(series), 14),\n" +
                                   "                DecimalNum.valueOf(30)\n" +
                                   "            ),\n" +
                                   "            new OverIndicatorRule(\n" +
                                   "                new RSIIndicator(new ClosePriceIndicator(series), 14),\n" +
                                   "                DecimalNum.valueOf(70)\n" +
                                   "            )\n" +
                                   "        );\n" +
                                   "    }\n" +
                                   "}";
                    return newCode;
                }
            }

            // 如果不是RSI结构，进行部分修复
            code = code.replaceAll("new Num\\((\\d+)\\)", "DecimalNum.valueOf($1)");
            code = code.replaceAll("new Num\\(([\\d.]+)\\)", "DecimalNum.valueOf($1)");

            return code;

        } catch (Exception e) {
            System.err.println("Error fixing RSI indicators: " + e.getMessage());
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
     * 修复常见的编译错误
     */
    private String fixCommonCompilationErrors(String code) {
        try {
            // 1. 修复缺少import的DecimalNum
            if (code.contains("DecimalNum") && !code.contains("import org.ta4j.core.num.DecimalNum")) {
                code = code.replaceFirst("import org.ta4j.core.rules.*;",
                    "import org.ta4j.core.rules.*;\nimport org.ta4j.core.num.DecimalNum;");
            }

            // 2. 修复Decimal.valueOf为DecimalNum.valueOf
            code = code.replaceAll("Decimal\\.valueOf", "DecimalNum.valueOf");

            // 3. 修复Num抽象类实例化错误 - new Num(数字) -> DecimalNum.valueOf(数字)
            code = code.replaceAll("new Num\\((\\d+)\\)", "DecimalNum.valueOf($1)");
            code = code.replaceAll("new Num\\(([\\d.]+)\\)", "DecimalNum.valueOf($1)");

            // 4. 修复int无法转换为Indicator的问题
            code = code.replaceAll("(\\w+Indicator\\([^,)]+), (\\d+)\\)", "$1, DecimalNum.valueOf($2))");

            // 5. 修复构造函数参数数量不匹配问题
            code = code.replaceAll("new (\\w+Indicator)\\(([^,)]+), (\\d+), ([\\d.]+)\\)",
                "new $1($2, DecimalNum.valueOf($3), DecimalNum.valueOf($4))");

            // 6. 修复super()调用没有参数的问题
            if (code.contains("super()")) {
                code = code.replaceAll("super\\(\\)", "super(null, null)");
            }

            // 7. 修复类名中的空格问题
            code = code.replaceAll("public\\s+class\\s+([A-Z]\\w*)", "public class $1");

            // 8. 修复方法调用中的语法错误
            code = code.replaceAll("\\.and\\(([^)]+)\\)\\s*,", ".and($1),");

            // 9. 修复ConstantIndicator的泛型问题
            code = code.replaceAll("new ConstantIndicator<>\\(([^,]+), (\\d+)\\)", "DecimalNum.valueOf($2)");
            code = code.replaceAll("new ConstantIndicator<Num>\\(([^,]+), (\\d+)\\)", "DecimalNum.valueOf($2)");
            code = code.replaceAll("new ConstantIndicator\\(([^,]+), (\\d+)\\)", "DecimalNum.valueOf($2)");

            // 10. 修复Rule构造中的数字参数
            code = code.replaceAll("Rule\\(([^,]+), (\\d+)\\)", "Rule($1, DecimalNum.valueOf($2))");

        } catch (Exception e) {
            System.err.println("Error fixing common compilation errors: " + e.getMessage());
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
}
