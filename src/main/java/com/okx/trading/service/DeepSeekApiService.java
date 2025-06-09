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
     * 生成交易策略代码
     * @param strategyDescription 策略描述
     * @return 生成的策略代码
     */
    public String generateStrategyCode(String strategyDescription) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new RuntimeException("DeepSeek API密钥未配置，请在application.yml中设置deepseek.api.key");
        }

        try {
            String prompt = buildPrompt(strategyDescription);
            String response = callDeepSeekApi(prompt);
            return extractCodeFromResponse(response);
        } catch (Exception e) {
            log.error("调用DeepSeek API生成策略代码失败: {}", e.getMessage(), e);
            throw new RuntimeException("生成策略代码失败: " + e.getMessage());
        }
    }

    /**
     * 构建提示词
     */
    private String buildPrompt(String strategyDescription) {
        return String.format(
            "请根据以下描述生成一个Ta4j交易策略的lambda函数代码：\n\n" +
            "策略描述：%s\n\n" +
            "要求：\n" +
            "1. 返回一个lambda函数，签名为：(BarSeries series, Map<String, Object> params) -> Strategy\n" +
            "2. 使用Ta4j库0.14版本的指标和规则\n" +
            "3. 包含买入和卖出规则\n" +
            "4. 代码要简洁且可编译\n" +
            "5. 只返回lambda函数代码，不要其他解释\n" +
            "6. 导入语句请使用完整包名\n" +
            "7. 使用new org.ta4j.core.BaseStrategy(buyRule, sellRule)构造策略，不要使用Builder模式\n\n" +
            "示例格式：\n" +
            "(series, params) -> {\n" +
            "    // 创建指标（如需要参数，请使用类型安全的方式获取）\n" +
            "    // Integer period1 = (Integer) params.get(\"period1\");\n" +
            "    // Integer period2 = (Integer) params.get(\"period2\");\n" +
            "    org.ta4j.core.indicators.SMAIndicator shortSma = new org.ta4j.core.indicators.SMAIndicator(series.getBarData(org.ta4j.core.BarData.CLOSE), 5);\n" +
            "    org.ta4j.core.indicators.SMAIndicator longSma = new org.ta4j.core.indicators.SMAIndicator(series.getBarData(org.ta4j.core.BarData.CLOSE), 20);\n" +
            "    \n" +
            "    // 买入规则\n" +
            "    org.ta4j.core.Rule buyRule = new org.ta4j.core.rules.CrossedUpIndicatorRule(shortSma, longSma);\n" +
            "    \n" +
            "    // 卖出规则\n" +
            "    org.ta4j.core.Rule sellRule = new org.ta4j.core.rules.CrossedDownIndicatorRule(shortSma, longSma);\n" +
            "    \n" +
            "    return new org.ta4j.core.BaseStrategy(buyRule, sellRule);\n" +
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
}
