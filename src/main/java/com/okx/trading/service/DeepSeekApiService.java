package com.okx.trading.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
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
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    /**
     * 生成完整的策略信息
     *
     * @param strategyDescription 策略描述
     * @return 包含策略名称、分类、默认参数、参数描述和策略代码的JSON对象
     */
    public JSONObject generateCompleteStrategyInfo(String strategyDescription) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new RuntimeException("DeepSeek API密钥未配置，请在application.yml中设置deepseek.api.key");
        }

        try {
            String prompt = buildCompleteStrategyPrompt(strategyDescription);
            String response = callDeepSeekApi(prompt);
            return extractStrategyInfoFromResponse(response);
        } catch (Exception e) {
            log.error("调用DeepSeek API生成完整策略信息失败: {}", e.getMessage(), e);
            throw new RuntimeException("生成完整策略信息失败: " + e.getMessage());
        }
    }

    /**
     * 构建完整策略信息提示词
     */
    private String buildCompleteStrategyPrompt(String strategyDescription) {
        return String.format(
                "请根据以下描述生成一个完整的Ta4j交易策略信息，返回JSON格式：\n\n" +
                        "策略描述：%s\n\n" +
                        "要求：\n" +
                        "1. 返回JSON格式，包含以下字段：\n" +
                        "   - strategyName: 策略名称（中文，简洁明了）\n" +
                        "   - strategyId: 前面是大写英文名称，用下划线区分单词，后面用uuid标记防止重复\n" +
                        "   - description: 对策略的简单介绍\n" +
                        "   - category: 策略分类（如：趋势策略、震荡策略、综合策略等）\n" +
                        "   - defaultParams: 默认参数（JSON对象格式）\n" +
                        "   - paramsDesc: 参数描述（JSON对象格式，key为参数名，value为中文描述）\n" +
                        "   - strategyCode: Ta4j策略lambda函数代码\n" +
                        "2. strategyCode要求：\n" +
                        "   - lambda函数签名：(BarSeries series, Map<String, Object> params) -> Strategy\n" +
                        "   - 使用Ta4j库0.14版本的指标和规则\n" +
                        "   - params参数是个空的map，直接使用默认值，不从里面拿数据\n" +
                        "   - 包含买入和卖出规则\n" +
                        "   - 代码要简洁且可编译，尽量不引入过多的类\n" +
                        "   - 导入语句请使用完整包名\n" +
                        "   - 使用new org.ta4j.core.BaseStrategy(buyRule, sellRule)构造策略\n" +
                        "3. 只返回JSON，不要其他解释\n\n" +
                        "示例格式：\n" +
                        "{\n" +
                        "  \"strategyName\": \"双均线交叉策略\",\n" +
                        "  \"strategyId\": \"BOLLINGER_b6bf3c73-496a-4053-85da-fb5845f3daf4\",\n" +
                        "  \"description\": \" 基于RSI超买超卖策略，当RSI低于30时买入，高于70时卖出\",\n" +
                        "  \"category\": \"趋势策略\",\n" +
                        "  \"defaultParams\": {\"shortPeriod\": 5, \"longPeriod\": 20},\n" +
                        "  \"paramsDesc\": {\"shortPeriod\": \"短期均线周期\", \"longPeriod\": \"长期均线周期\"},\n" +
                        "  \"strategyCode\": \"(series, params) -> { org.ta4j.core.indicators.ClosePriceIndicator closePrice = new org.ta4j.core.indicators.ClosePriceIndicator(series); Integer shortPeriod = (Integer) params.get(\\\"shortPeriod\\\"); Integer longPeriod = (Integer) params.get(\\\"longPeriod\\\"); org.ta4j.core.indicators.SMAIndicator shortSma = new org.ta4j.core.indicators.SMAIndicator(closePrice, shortPeriod); org.ta4j.core.indicators.SMAIndicator longSma = new org.ta4j.core.indicators.SMAIndicator(closePrice, longPeriod); org.ta4j.core.Rule buyRule = new org.ta4j.core.rules.CrossedUpIndicatorRule(shortSma, longSma); org.ta4j.core.Rule sellRule = new org.ta4j.core.rules.CrossedDownIndicatorRule(shortSma, longSma); return new org.ta4j.core.BaseStrategy(buyRule, sellRule); }\"\n" +
                        "}",
                strategyDescription
        );
    }

    /**
     * 调用DeepSeek API
     */
    private String callDeepSeekApi(String prompt) throws IOException {
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "deepseek-chat");

        JSONArray messages = new JSONArray();
        JSONObject message = new JSONObject();
        message.put("role", "user");
        message.put("content", prompt);
        messages.add(message);

        requestBody.put("messages", messages);
        requestBody.put("max_tokens", 2000);
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
