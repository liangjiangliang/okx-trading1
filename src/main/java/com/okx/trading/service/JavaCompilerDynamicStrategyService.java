package com.okx.trading.service;

import com.okx.trading.model.entity.StrategyInfoEntity;
import com.okx.trading.ta4j.strategy.StrategyFactory1;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.ta4j.core.BarSeries;
import org.ta4j.core.Strategy;

import javax.tools.*;
import java.io.*;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 基于Java Compiler API的动态策略服务
 * 相比Janino具有更好的错误信息和完整的Java语法支持
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JavaCompilerDynamicStrategyService {

    private final StrategyInfoService strategyInfoService;

    // 缓存已编译的策略函数
    private final Map<String, Function<BarSeries, Strategy>> compiledStrategies = new ConcurrentHashMap<>();

    // 临时编译目录
    private final Path tempCompileDir = Paths.get(System.getProperty("java.io.tmpdir"), "okx-trading-compiled-strategies");

    // Java编译器
    private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

    /**
     * 编译策略代码并加载到StrategyFactory
     */
    public Function<BarSeries, Strategy> compileAndLoadStrategy(
            String strategyCode, StrategyInfoEntity strategyEntity) {
        try {
            // 检查编译器可用性
            if (compiler == null) {
                throw new RuntimeException("Java Compiler API不可用，请确保运行在JDK而非JRE环境中");
            }

            // 编译策略代码
            Function<BarSeries, Strategy> strategyFunction = compileStrategyCode(strategyCode, strategyEntity.getStrategyCode());

            // 缓存策略函数
            compiledStrategies.put(strategyEntity.getStrategyCode(), strategyFunction);

            // 动态加载到StrategyFactory
            loadStrategyToFactory(strategyEntity.getStrategyCode(), strategyFunction);

            log.info("策略 {} 使用Java Compiler API编译并加载成功", strategyEntity.getStrategyCode());
            return strategyFunction;
        } catch (Exception e) {
            log.error("使用Java Compiler API编译策略代码失败: {}, 编译的代码: {}", e.getMessage(), strategyCode);
            throw new RuntimeException("编译策略代码失败: " + e.getMessage());
        }
    }

    /**
     * 编译策略代码
     */
    private Function<BarSeries, Strategy> compileStrategyCode(String strategyCode, String strategyId) throws Exception {
        // 确保临时目录存在
        if (!Files.exists(tempCompileDir)) {
            Files.createDirectories(tempCompileDir);
        }

        // 从代码中提取类名
        String className = extractClassName(strategyCode);

        // 准备完整的源代码
        String fullSourceCode = prepareFullSourceCode(strategyCode);

        // 创建源文件
        Path sourceFile = tempCompileDir.resolve(className + ".java");
        Files.write(sourceFile, fullSourceCode.getBytes("UTF-8"));

        // 准备编译选项 - 禁用注解处理器以避免Lombok冲突
        List<String> options = Arrays.asList(
            "-classpath", buildClasspath(),
            "-d", tempCompileDir.toString(),
            "-proc:none"  // 禁用注解处理器
        );

        // 获取文件管理器
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        // 获取编译单元
        Iterable<? extends JavaFileObject> compilationUnits = fileManager.getJavaFileObjects(sourceFile.toFile());

        // 创建编译任务
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavaCompiler.CompilationTask task = compiler.getTask(
            null, fileManager, diagnostics, options, null, compilationUnits);

        // 执行编译
        boolean success = task.call();

        if (!success) {
            StringBuilder errorMessage = new StringBuilder("编译失败:\n");
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                errorMessage.append(String.format("Line %d, Column %d: %s\n",
                    diagnostic.getLineNumber(),
                    diagnostic.getColumnNumber(),
                    diagnostic.getMessage(null)));
            }
            throw new RuntimeException(errorMessage.toString());
        }

        // 加载编译后的类
        URL[] urls = {tempCompileDir.toUri().toURL()};
        URLClassLoader classLoader = new URLClassLoader(urls, this.getClass().getClassLoader());
        Class<?> strategyClass = classLoader.loadClass(className);

        // 创建策略函数
        return (series) -> {
            try {
                return (Strategy) strategyClass.getConstructor(BarSeries.class).newInstance(series);
            } catch (Exception e) {
                throw new RuntimeException("创建策略实例失败: " + e.getMessage(), e);
            }
        };
    }

    /**
     * 准备完整的源代码，包含所有必要的import
     */
    private String prepareFullSourceCode(String strategyCode) {
        StringBuilder fullCode = new StringBuilder();

        // 添加必要的import语句
        fullCode.append("import org.ta4j.core.*;\n");
        fullCode.append("import org.ta4j.core.indicators.*;\n");
        fullCode.append("import org.ta4j.core.indicators.helpers.*;\n");
        fullCode.append("import org.ta4j.core.indicators.bollinger.*;\n");
        fullCode.append("import org.ta4j.core.indicators.statistics.*;\n");
        fullCode.append("import org.ta4j.core.indicators.volume.*;\n");
        fullCode.append("import org.ta4j.core.rules.*;\n");
        fullCode.append("import org.ta4j.core.num.*;\n");
        fullCode.append("import java.util.*;\n");
        fullCode.append("import java.math.*;\n");
        fullCode.append("\n");

        // 添加策略代码
        fullCode.append(strategyCode);

        return fullCode.toString();
    }

    /**
     * 构建类路径
     */
    private String buildClasspath() {
        StringBuilder classpath = new StringBuilder();

        // 获取当前类加载器的类路径
        String javaClassPath = System.getProperty("java.class.path");
        classpath.append(javaClassPath);

        return classpath.toString();
    }

    /**
     * 从类代码中提取类名
     */
    private String extractClassName(String classCode) {
        String[] lines = classCode.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("public class")) {
                String[] parts = line.split("\\s+");
                for (int i = 0; i < parts.length; i++) {
                    if ("class".equals(parts[i]) && i + 1 < parts.length) {
                        String className = parts[i + 1];
                        // 移除可能的extends部分
                        if (className.contains(" ")) {
                            className = className.split("\\s+")[0];
                        }
                        return className;
                    }
                }
            }
        }
        throw new RuntimeException("无法从代码中提取类名");
    }

    /**
     * 将策略函数动态加载到StrategyFactory
     */
    private void loadStrategyToFactory(String strategyCode, Function<BarSeries, Strategy> strategyFunction) {
        try {
            // 通过反射获取StrategyFactory的strategyCreators字段
            Field strategyCreatorsField = StrategyFactory1.class.getDeclaredField("strategyCreators");
            strategyCreatorsField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<String, Function<BarSeries, Strategy>> strategyCreators =
                    (Map<String, Function<BarSeries, Strategy>>) strategyCreatorsField.get(null);

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
                    .filter(strategy ->
                            strategy.getSourceCode() != null &&
                            !strategy.getSourceCode().trim().isEmpty() &&
                            strategy.getSourceCode().contains("public class"))
                    .forEach(strategy -> {
                        try {
                            compileAndLoadStrategy(strategy.getSourceCode(), strategy);
                            // 加载成功，清除之前的错误信息
                            if (strategy.getLoadError() != null) {
                                strategy.setLoadError(null);
                                strategyInfoService.saveStrategy(strategy);
                            }
                            log.info("使用Java Compiler API从数据库加载策略: {}", strategy.getStrategyCode());
                        } catch (Exception e) {
                            String errorMessage = "使用Java Compiler API加载策略失败: " + e.getMessage();
                            log.error("加载策略 {} 失败: {}", strategy.getStrategyCode(), e.getMessage());

                            // 将错误信息保存到数据库
                            try {
                                strategy.setLoadError(errorMessage);
                                strategyInfoService.saveStrategy(strategy);
                                log.info("策略 {} 的错误信息已保存到数据库", strategy.getStrategyCode());
                            } catch (Exception saveException) {
                                log.error("保存策略 {} 的错误信息失败: {}", strategy.getStrategyCode(), saveException.getMessage());
                            }
                        }
                    });
        } catch (Exception e) {
            log.error("使用Java Compiler API加载动态策略失败: {}", e.getMessage(), e);
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
            Field strategyCreatorsField = StrategyFactory1.class.getDeclaredField("strategyCreators");
            strategyCreatorsField.setAccessible(true);

            @SuppressWarnings("unchecked")
            Map<String, Function<BarSeries, Strategy>> strategyCreators =
                    (Map<String, Function<BarSeries, Strategy>>) strategyCreatorsField.get(null);

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
    public Function<BarSeries, Strategy> getCompiledStrategy(String strategyCode) {
        return compiledStrategies.get(strategyCode);
    }

    /**
     * 检查策略是否已加载
     */
    public boolean isStrategyLoaded(String strategyCode) {
        return compiledStrategies.containsKey(strategyCode);
    }

    /**
     * 清理临时文件
     */
    public void cleanup() {
        try {
            if (Files.exists(tempCompileDir)) {
                Files.walk(tempCompileDir)
                        .sorted((a, b) -> b.compareTo(a)) // 逆序，先删除文件再删除目录
                        .forEach(path -> {
                            try {
                                Files.delete(path);
                            } catch (IOException e) {
                                log.warn("删除临时文件失败: {}", path, e);
                            }
                        });
            }
        } catch (Exception e) {
            log.warn("清理临时编译目录失败", e);
        }
    }
}
