package com.okx.trading.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * DeepSeek API服务类
 * 用于调用DeepSeek API生成交易策略代码
 */
@Slf4j
@Service
public class DeepSeekApiService {

    @Value("${deepseek.api.key:}")
    private String apiKey;

    @Value("${deepseek.api.url:https://api.deepseek.com/v1/chat/completions}")
    private String apiUrl;

    private final OkHttpClient httpClient;

    public DeepSeekApiService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 生成完整的策略信息（带对话上下文）
     *
     * @param strategyDescription 策略描述
     * @param conversationContext 对话上下文
     * @return 包含策略名称、分类、默认参数、参数描述和策略代码的JSON对象
     */
    public JSONObject generateCompleteStrategyInfo(String strategyDescription, String currentStrategy, String conversationContext) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new RuntimeException("DeepSeek API密钥未配置，请在application.yml中设置deepseek.api.key");
        }

        try {
            String prompt = buildCompleteStrategyPrompt(strategyDescription, currentStrategy, conversationContext);
            String response = callDeepSeekApi(prompt);
            return extractStrategyInfoFromResponse(response);
        } catch (Exception e) {
            log.error("调用DeepSeek API生成完整策略信息失败: {}", e.getMessage(), e);
            throw new RuntimeException("生成完整策略信息失败: " + e.getMessage());
        }
    }

    /**
     * 构建完整策略信息提示词（带对话上下文）
     */
    private String buildCompleteStrategyPrompt(String strategyDescription, String currentStrategy, String conversationContext) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("请根据以下描述生成一个完整的Ta4j交易策略信息，返回JSON格式：\n\n");

        // 添加对话上下文（如果存在）
        if (conversationContext != null && !conversationContext.trim().isEmpty()) {
            promptBuilder.append(conversationContext).append("\n\n");
            promptBuilder.append("基于以上历史对话记录，请继续优化和改进策略。\n\n");
        }
        // 添加最新策略信息
        if (StringUtils.isNotBlank(currentStrategy)) {
            promptBuilder.append(currentStrategy).append("\n\n");
            promptBuilder.append("以上是最新的策略信息，请基于请求继续优化。\n\n");
        }

        promptBuilder.append("期望生成策略的描述：").append(strategyDescription).append("\n\n");
        promptBuilder.append("要求：\n");
        promptBuilder.append("1. 返回JSON格式，包含以下字段：\n");
        promptBuilder.append("   - strategyName: 策略名称（中文，简洁明了）\n");
        promptBuilder.append("   - strategyId: 前面是大写英文名称，用下划线区分单词，后面用uuid标记防止重复\n");
        promptBuilder.append("   - description: 对策略逻辑的简单介绍，比如使用什么计算方式，使用的参数周期等信息\n");
        promptBuilder.append("   - comments: 策略使用介绍，比如优缺点，适用场景，胜率，回测情况，短线还是长线使用等信息\n");
        promptBuilder.append("   - category: 策略分类（如：趋势策略、震荡策略、综合策略等）\n");
        promptBuilder.append("   - defaultParams: 默认参数（JSON对象格式）\n");
        promptBuilder.append("   - paramsDesc: 参数描述（JSON对象格式，key为参数名，value为中文描述）\n");
        promptBuilder.append("   - strategyCode: Ta4j策略lambda函数代码\n");
        promptBuilder.append("2. strategyCode要求：\n");
        promptBuilder.append("   - 生成一个完整的Java类，实现org.ta4j.core.Strategy接口\n");
        promptBuilder.append("   - 类名格式：Generated + 策略英文名 + Strategy（如：GeneratedSmaStrategy）\n");
        promptBuilder.append("   - 使用Ta4j库0.14版本的指标和规则\n");
        promptBuilder.append("   - 应用在加密货币领域进行回测\n");
        promptBuilder.append("   - 包含买入和卖出规则\n");
        promptBuilder.append("   - 代码要简洁且可编译，尽量不引入过多的类\n");
        promptBuilder.append("   - 返回代码要格式化，换行，缩进便于阅读\n");
        promptBuilder.append("   - 不需要package声明和import语句，直接使用完整类名\n");
        promptBuilder.append("   - 实现Strategy接口的所有方法：shouldEnter、shouldExit、and、or、opposite、getName、getEntryRule、getExitRule、setUnstablePeriod、isUnstableAt\n");
        promptBuilder.append("   - getName方法返回策略的名称字符串\n");
        promptBuilder.append("   - 在构造函数中初始化指标和规则\n");
        promptBuilder.append("   - 使用org.ta4j.core.BaseStrategy作为内部实现\n");
        promptBuilder.append("   - 【重要】只能使用以下包的类：\n");
        promptBuilder.append("     * org.ta4j.core.indicators.*\n");
        promptBuilder.append("     * org.ta4j.core.rules.*\n");
        promptBuilder.append("     * org.ta4j.core.BaseStrategy\n");
        promptBuilder.append("   - 【严禁】使用以下内容：\n");
        promptBuilder.append("     * 尽量避免helpers包或任何外部工具类\n");
        promptBuilder.append("     * 不要使用lambda表达式(->)，使用传统的方法调用\n");
        promptBuilder.append("   - 【数学运算辅助方法】如果需要使用multipliedBy、dividedBy、plus、minus等方法，请在策略类中实现这些辅助方法：\n");
        promptBuilder.append("     * 例如：private double multipliedBy(double value, double multiplier) { return value * multiplier; }\n");
        promptBuilder.append("     * 例如：private double dividedBy(double value, double divisor) { return value / divisor; }\n");
        promptBuilder.append("     * 例如：private double plus(double value1, double value2) { return value1 + value2; }\n");
        promptBuilder.append("     * 例如：private double minus(double value1, double value2) { return value1 - value2; }\n");
        promptBuilder.append("   - 数值比较必须使用OverIndicatorRule、UnderIndicatorRule等规则类\n");
        promptBuilder.append("   - 常用指标：SMAIndicator、EMAIndicator、RSIIndicator、VolumeIndicator等\n");
        promptBuilder.append("   - 【重要】RSI等指标需要先创建ClosePriceIndicator：new org.ta4j.core.indicators.helpers.ClosePriceIndicator(series)\n");
        promptBuilder.append("   - RSI指标正确用法：new org.ta4j.core.indicators.RSIIndicator(closePrice, period)\n");
        promptBuilder.append("   - ATR指标正确用法：new org.ta4j.core.indicators.ATRIndicator(series, period)\n");
        promptBuilder.append("   - SMA指标正确用法：new org.ta4j.core.indicators.SMAIndicator(indicator, period)\n");
        promptBuilder.append("   - 常用规则：CrossedUpIndicatorRule、CrossedDownIndicatorRule、OverIndicatorRule、UnderIndicatorRule等\n");
        promptBuilder.append("3. 只返回JSON，不要其他解释\n\n");
        promptBuilder.append("示例格式：\n");
        promptBuilder.append("{\n");
        promptBuilder.append("  \"strategyName\": \"RSI超买超卖策略\",\n");
        promptBuilder.append("  \"strategyId\": \"RSI_STRATEGY_b6bf3c73-496a-4053-85da-fb5845f3daf4\",\n");
        promptBuilder.append("  \"description\": \"基于RSI指标的超买超卖策略，当RSI低于30时买入，高于70时卖出\",\n");
        promptBuilder.append("  \"comments\": \"适用于震荡市场，短线交易策略，胜率较高但需要及时止损\",\n");
        promptBuilder.append("  \"category\": \"震荡策略\",\n");
        promptBuilder.append("  \"defaultParams\": {\"rsiPeriod\": 14, \"buyThreshold\": 30, \"sellThreshold\": 70},\n");
        promptBuilder.append("  \"paramsDesc\": {\"rsiPeriod\": \"RSI计算周期\", \"buyThreshold\": \"买入阈值\", \"sellThreshold\": \"卖出阈值\"},\n");
        promptBuilder.append("  \"strategyCode\": \"public class GeneratedRsiStrategy implements org.ta4j.core.Strategy {\n" +
                "    private final org.ta4j.core.Strategy baseStrategy;\n" +
                "    \n" +
                "    public GeneratedRsiStrategy(org.ta4j.core.BarSeries series) {\n" +
                "        org.ta4j.core.indicators.helpers.ClosePriceIndicator closePrice = new org.ta4j.core.indicators.helpers.ClosePriceIndicator(series);\n" +
                "        org.ta4j.core.indicators.RSIIndicator rsi = new org.ta4j.core.indicators.RSIIndicator(closePrice, 14);\n" +
                "        org.ta4j.core.Rule buyRule = new org.ta4j.core.rules.UnderIndicatorRule(rsi, 30);\n" +
                "        org.ta4j.core.Rule sellRule = new org.ta4j.core.rules.OverIndicatorRule(rsi, 70);\n" +
                "        this.baseStrategy = new org.ta4j.core.BaseStrategy(buyRule, sellRule);\n" +
                "    }\n" +
                "    \n" +
                "    @Override\n" +
                "    public org.ta4j.core.Rule getEntryRule() {\n" +
                "        return baseStrategy.getEntryRule();\n" +
                "    }\n" +
                "    \n" +
                "    @Override\n" +
                "    public org.ta4j.core.Rule getExitRule() {\n" +
                "        return baseStrategy.getExitRule();\n" +
                "    }\n" +
                "    \n" +
                "    @Override\n" +
                "    public boolean shouldEnter(int index) {\n" +
                "        return baseStrategy.shouldEnter(index);\n" +
                "    }\n" +
                "    \n" +
                "    @Override\n" +
                "    public boolean shouldEnter(int index, org.ta4j.core.TradingRecord tradingRecord) {\n" +
                "        return baseStrategy.shouldEnter(index, tradingRecord);\n" +
                "    }\n" +
                "    \n" +
                "    @Override\n" +
                "    public boolean shouldExit(int index) {\n" +
                "        return baseStrategy.shouldExit(index);\n" +
                "    }\n" +
                "    \n" +
                "    @Override\n" +
                "    public boolean shouldExit(int index, org.ta4j.core.TradingRecord tradingRecord) {\n" +
                "        return baseStrategy.shouldExit(index, tradingRecord);\n" +
                "    }\n" +
                "    \n" +
                "    @Override\n" +
                "    public org.ta4j.core.Strategy and(org.ta4j.core.Strategy strategy) {\n" +
                "        return baseStrategy.and(strategy);\n" +
                "    }\n" +
                "    \n" +
                "    @Override\n" +
                "    public org.ta4j.core.Strategy or(org.ta4j.core.Strategy strategy) {\n" +
                "        return baseStrategy.or(strategy);\n" +
                "    }\n" +
                "    \n" +
                "    @Override\n" +
                "    public org.ta4j.core.Strategy and(String name, org.ta4j.core.Strategy strategy, int unstableBars) {\n" +
                "        return baseStrategy.and(name, strategy, unstableBars);\n" +
                "    }\n" +
                "    \n" +
                "    @Override\n" +
                "    public org.ta4j.core.Strategy or(String name, org.ta4j.core.Strategy strategy, int unstableBars) {\n" +
                "        return baseStrategy.or(name, strategy, unstableBars);\n" +
                "    }\n" +
                "    \n" +
                "    @Override\n" +
                "    public org.ta4j.core.Strategy opposite() {\n" +
                "        return baseStrategy.opposite();\n" +
                "    }\n" +
                "    \n" +
                "    @Override\n" +
                "    public String getName() {\n" +
                "        return \"Generated RSI Strategy\";\n" +
                "    }\n" +
                "    \n" +
                "    @Override\n" +
                "    public void setUnstablePeriod(int unstablePeriod) {\n" +
                "        baseStrategy.setUnstablePeriod(unstablePeriod);\n" +
                "    }\n" +
                "    \n" +
                "    @Override\n" +
                "    public int getUnstablePeriod() {\n" +
                "        return baseStrategy.getUnstablePeriod();\n" +
                "    }\n" +
                "    \n" +
                "    @Override\n" +
                "    public boolean isUnstableAt(int index) {\n" +
                "        return baseStrategy.isUnstableAt(index);\n" +
                "    }\n" +
                "}\"\n");
        promptBuilder.append("}\n\n");
        promptBuilder.append("\n\n【重要提醒】\n");
        promptBuilder.append("1. 如果需要数学运算方法，请在策略类中自己实现辅助方法\n");
        promptBuilder.append("2. 成交量倍数比较示例：\n");
        promptBuilder.append("   方法1：使用自定义辅助方法 - private double multipliedBy(double value, double multiplier) { return value * multiplier; }\n");
        promptBuilder.append("   方法2：直接使用运算符 - double result = volumeAvg * 1.5;\n");
        promptBuilder.append("3. 数值运算优先使用基本运算符：+、-、*、/\n");
        promptBuilder.append("4. 复杂运算可以实现辅助方法，但要确保方法名和参数类型正确\n");
        promptBuilder.append("5. 所有辅助方法都应该是private的，避免与Ta4j库冲突\n");

        return promptBuilder.toString();
    }

    /**
     * 调用DeepSeek API
     */
    private String callDeepSeekApi(String prompt) throws IOException {
        JSONObject requestBody = new JSONObject();
        // 修复：添加必需的model参数，这是导致422错误的原因
        requestBody.put("model", "deepseek-chat"); // deepseek-reasoner  deepseek-chat

        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        messages.add(message);

        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 4000);
        requestBody.put("temperature", 0.1);

        RequestBody body = RequestBody.create(
                requestBody.toJSONString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        Request request = new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("API调用失败: " + response.code() + " " + response.message());
            }

            String responseBody = response.body().string();
            log.debug("DeepSeek API响应: {}", responseBody);
            return responseBody;
        }
    }

    /**
     * 从API响应中提取代码
     */
    private String extractCodeFromResponse(String response) {
        try {
            JSONObject jsonResponse = JSON.parseObject(response);
            JSONArray choices = jsonResponse.getJSONArray("choices");
            if (choices != null && choices.size() > 0) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject message = choice.getJSONObject("message");
                String content = message.getString("content");

                // 提取代码块
                if (content.contains("```")) {
                    int start = content.indexOf("```");
                    int end = content.lastIndexOf("```");
                    if (start != end && start != -1) {
                        String codeBlock = content.substring(start + 3, end).trim();
                        // 移除可能的语言标识符
                        if (codeBlock.startsWith("java")) {
                            codeBlock = codeBlock.substring(4).trim();
                        }
                        return codeBlock;
                    }
                }

                return content.trim();
            }
            throw new RuntimeException("API响应格式错误");
        } catch (Exception e) {
            log.error("解析API响应失败: {}", e.getMessage(), e);
            throw new RuntimeException("解析API响应失败: " + e.getMessage());
        }
    }

    /**
     * 从API响应中提取完整策略信息
     */
    private JSONObject extractStrategyInfoFromResponse(String response) {
        try {
            JSONObject jsonResponse = JSON.parseObject(response);
            JSONArray choices = jsonResponse.getJSONArray("choices");
            if (choices != null && choices.size() > 0) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject message = choice.getJSONObject("message");
                String content = message.getString("content");

                // 提取JSON内容
                String jsonContent = content.trim();

                // 如果包含代码块标记，提取其中的JSON
                if (jsonContent.contains("```")) {
                    int start = jsonContent.indexOf("```");
                    int end = jsonContent.lastIndexOf("```");
                    if (start != end && start != -1) {
                        jsonContent = jsonContent.substring(start + 3, end).trim();
                        // 移除可能的语言标识符
                        if (jsonContent.startsWith("json")) {
                            jsonContent = jsonContent.substring(4).trim();
                        }
                    }
                }

                // 解析JSON
                JSONObject strategyInfo = JSON.parseObject(jsonContent);

                // 验证必要字段
                if (!strategyInfo.containsKey("strategyName") ||
                        !strategyInfo.containsKey("category") ||
                        !strategyInfo.containsKey("defaultParams") ||
                        !strategyInfo.containsKey("paramsDesc") ||
                        !strategyInfo.containsKey("strategyCode")) {
                    throw new RuntimeException("API返回的策略信息缺少必要字段");
                }

                return strategyInfo;
            }
            throw new RuntimeException("API响应格式错误");
        } catch (Exception e) {
            log.error("解析完整策略信息失败: {}", e.getMessage(), e);
            throw new RuntimeException("解析完整策略信息失败: " + e.getMessage());
        }
    }
}
