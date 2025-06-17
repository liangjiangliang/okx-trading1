package com.okx.trading.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
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

    private String strategyCodeTemplate = "{\n" +
            "  \"strategyName\": \"EMA双线交叉策略\",\n" +
            "  \"strategyId\": \"EMA_CROSSOVER_STRATEGY_b6bf3c73-496a-4053-85da-fb5845f3daf4\",\n" +
            "  \"description\": \"基于快线EMA和慢线EMA的交叉策略，当快线上穿慢线时买入，当快线下穿慢线时卖出\",\n" +
            "  \"comments\": \"适用于趋势市场，经典的移动平均线交叉策略，信号明确，易于理解和实现\",\n" +
            "  \"category\": \"趋势策略\",\n" +
            "  \"defaultParams\": {\"fastPeriod\": 12, \"slowPeriod\": 26},\n" +
            "  \"paramsDesc\": {\"fastPeriod\": \"快线EMA周期\", \"slowPeriod\": \"慢线EMA周期\"},\n" +
            "  \"strategyCode\": \"" +
            "import org.ta4j.core.*;\\n" +
            "import org.ta4j.core.indicators.*;\\n" +
            "import org.ta4j.core.indicators.helpers.*;\\n" +
            "import org.ta4j.core.rules.*;\\n" +
            "\\n" +
            "public class GeneratedEmaCrossoverStrategy extends BaseStrategy {\\n" +
            "\\n" +
            "    public GeneratedEmaCrossoverStrategy(BarSeries series) {\\n" +
            "        super(\\n" +
            "            new CrossedUpIndicatorRule(new EMAIndicator(new ClosePriceIndicator(series), 12), new EMAIndicator(new ClosePriceIndicator(series), 26)),\\n" +
            "            new CrossedDownIndicatorRule(new EMAIndicator(new ClosePriceIndicator(series), 12), new EMAIndicator(new ClosePriceIndicator(series), 26))\\n" +
            "        );\\n" +
            "    }\\n" +
            "}\"\n" +
            "}\n";

    private String codeGenerateStrategy = ""
            + "   - strategyName: 策略名称（中文，简洁明了）\n"
            + "   - strategyId: 前面是大写英文名称，用下划线区分单词，后面用uuid标记防止重复\n"
            + "   - description: 对策略逻辑的简单介绍，比如使用什么计算方式，使用的参数周期等信息\n"
            + "   - comments: 策略使用介绍，比如优缺点，适用场景，胜率，回测情况，短线还是长线使用等信息\n"
            + "   - category: 策略分类（如：趋势策略、震荡策略、综合策略等）\n"
            + "   - defaultParams: 默认参数（JSON对象格式）\n"
            + "   - paramsDesc: 参数描述（JSON对象格式，key为参数名，value为中文描述）\n"
            + "   - strategyCode: Ta4j策略Java类代码\n"
            + "2. strategyCode要求：\n"
            + "   - 生成一个完整的Java类，继承org.ta4j.core.BaseStrategy类\n"
            + "   - 必须包含完整的import语句，导入所有使用的包\n"
            + "   - 类名格式：Generated + 策略英文名 + Strategy（如：GeneratedSmaStrategy）\n"
            + "   - 使用Ta4j库0.14版本的指标和规则\n"
            + "   - 包含买入和卖出规则\n"
            + "   - 代码要简洁且可编译，能够通过Janino编译器编译\n"
            + "   - 继承BaseStrategy类，在构造函数中调用super()传入买入规则和卖出规则\n"
            + "   - 构造函数只能接收BarSeries参数，不接受其他参数\n"
            + "   - 在构造函数中直接创建指标和规则\n"
            + "   - 【关键约束】绝对不要创建任何内部类、静态类、匿名类或自定义指标类！\n"
            + "   - 【关键约束】只能使用现有的Ta4j指标类，如SMAIndicator、EMAIndicator、RSIIndicator等\n"
            + "   - 【关键约束】只能使用简单的规则组合，不要创建复杂的数学运算\n"
            + "   - 【关键约束】策略类中只能包含一个构造函数，不能包含任何其他方法\n"
            + "   - 必须的import语句：\n"
            + "     * import org.ta4j.core.*;\n"
            + "     * import org.ta4j.core.indicators.*;\n"
            + "     * import org.ta4j.core.indicators.helpers.*;\n"
            + "     * import org.ta4j.core.rules.*;\n"
            + "   - 常用指标：SMAIndicator、EMAIndicator、RSIIndicator、MACDIndicator等\n"
            + "   - 常用规则：CrossedUpIndicatorRule、CrossedDownIndicatorRule、OverIndicatorRule、UnderIndicatorRule\n"
            + "   - 【重要】所有策略都必须严格按照以下模板格式编写：\n"
            + "     public class GeneratedXxxStrategy extends BaseStrategy {\n"
            + "         public GeneratedXxxStrategy(BarSeries series) {\n"
            + "             super(buyRule, sellRule);\n"
            + "         }\n"
            + "     }\n"
            + "   - 【ATR策略限制】对于ATR类型策略，由于复杂性问题，请使用简单的SMA/EMA突破策略代替\n";

    private String codeGeneratePromotion = "\n\n【严格代码规范】\n"
            + "1. 【绝对禁止】以下类型的代码结构：\n"
            + "   - 任何内部类、静态类或匿名类\n"
            + "   - 任何自定义方法（除了构造函数）\n"
            + "   - 任何参数化构造函数（除了单个BarSeries参数）\n"
            + "   - 复杂的数值运算和类型转换\n"
            + "   - 使用getValue()方法获取动态值\n"
            + "2. 【必须遵循】代码结构：\n"
            + "   - 只能有一个构造函数：public GeneratedXxxStrategy(BarSeries series)\n"
            + "   - 构造函数中只能调用super(buyRule, sellRule)\n"
            + "   - 买卖规则必须在构造函数中直接创建\n"
            + "   - 只能使用现有指标类和规则类\n"
            + "3. 【错误示例 - 绝对禁止】：\n"
            + "   private static Rule createEntryRule(...) // 禁止自定义方法\n"
            + "   public GeneratedStrategy(BarSeries series, int period) // 禁止多参数构造函数\n"
            + "   Num atrValue = atr.getValue(series.getEndIndex()) // 禁止动态值获取\n"
            + "   private static class CustomIndicator // 禁止内部类\n"
            + "4. 【正确示例 - 必须遵循】：\n"
            + "   public class GeneratedSmaStrategy extends BaseStrategy {\n"
            + "       public GeneratedSmaStrategy(BarSeries series) {\n"
            + "           super(\n"
            + "               new CrossedUpIndicatorRule(new EMAIndicator(new ClosePriceIndicator(series), 12), new SMAIndicator(new ClosePriceIndicator(series), 26)),\n"
            + "               new CrossedDownIndicatorRule(new EMAIndicator(new ClosePriceIndicator(series), 12), new SMAIndicator(new ClosePriceIndicator(series), 26))\n"
            + "           );\n"
            + "       }\n"
            + "   }\n"
            + "5. 【编译错误修复】如果涉及ATR策略，改用简单的SMA/EMA交叉策略\n";

    /**
     * 更新策略（带对话上下文）
     *
     * @param strategyDescription 策略描述
     * @param conversationContext 对话上下文
     * @return 包含策略名称、分类、默认参数、参数描述和策略代码的JSON对象
     */
    public JSONObject updateStrategyInfo(String strategyDescription, String currentStrategy, String conversationContext) {
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
     * 批量生成完整的策略信息
     *
     * @param strategyDescriptions 策略描述列表
     * @return 包含多个策略信息的JSON数组
     */
    public JSONArray generateBatchCompleteStrategyInfo(String[] strategyDescriptions) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new RuntimeException("DeepSeek API密钥未配置，请在application.yml中设置deepseek.api.key");
        }

        try {
            String prompt = buildBatchCompleteStrategyPrompt(strategyDescriptions);
            String response = callDeepSeekApi(prompt);
            return extractBatchStrategyInfoFromResponse(response);
        } catch (Exception e) {
            log.error("调用DeepSeek API批量生成策略信息失败: {}", e.getMessage(), e);
            throw new RuntimeException("批量生成策略信息失败: " + e.getMessage());
        }
    }

    /**
     * 更新策略信息提示词（带对话上下文）
     */
    private String buildCompleteStrategyPrompt(String strategyDescription, String currentStrategy, String conversationContext) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("请根据以下描述生成一个完整的Ta4j交易策略信息，返回JSON格式：\n\n");

        // 添加对话上下文（如果存在）
        if (conversationContext != null && !conversationContext.trim().isEmpty()) {
            promptBuilder.append(conversationContext).append("\n\n");
            promptBuilder.append("基于以上历史对话记录，请按照请求继续优化策略。\n\n");
        }
        // 添加最新策略信息
        if (StringUtils.isNotBlank(currentStrategy)) {
            promptBuilder.append("\n\n=== 当前策略信息 ===\n");
            promptBuilder.append(currentStrategy).append("\n\n");
            promptBuilder.append("\n\n=== 当前策略信息结束 ===\n");
            promptBuilder.append("基于以上最新策略信息，请按照请求继续优化策略。\n\n");
        }

        // 处理ATR相关请求，统一转换为简单策略
        String processedDescription = strategyDescription;
        if (strategyDescription.contains("atr") || strategyDescription.contains("ATR") ||
            strategyDescription.contains("波动") || strategyDescription.contains("突破")) {
            processedDescription = "生成一个简单的EMA双线交叉策略，作为波动突破策略的替代方案";
        }

        promptBuilder.append("期望生成策略的描述：").append(processedDescription).append("\n\n");
        promptBuilder.append("要求：\n");
        promptBuilder.append("1. 返回JSON格式，包含以下字段：\n");
        promptBuilder.append(codeGenerateStrategy);
        promptBuilder.append("3. 只返回JSON，不要其他解释\n\n");
        promptBuilder.append("示例格式：\n");
        promptBuilder.append(strategyCodeTemplate);
        promptBuilder.append(codeGeneratePromotion);

        return promptBuilder.toString();
    }

    /**
     * 构建批量策略信息提示词
     */
    private String buildBatchCompleteStrategyPrompt(String[] strategyDescriptions) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("请根据以下多个策略描述，一次性生成多个完整的Ta4j交易策略信息，返回JSON数组格式：\n\n");

        promptBuilder.append("需要生成的策略描述列表：\n");
        for (int i = 0; i < strategyDescriptions.length; i++) {
            promptBuilder.append(String.format("%d. %s\n", i + 1, strategyDescriptions[i]));
        }
        promptBuilder.append("\n");

        promptBuilder.append("要求：\n");
        promptBuilder.append("1. 返回JSON数组格式，数组中每个元素包含以下字段：\n");
        promptBuilder.append(codeGenerateStrategy);
        promptBuilder.append("3. 确保每个策略都有不同的strategyId和类名\n");
        promptBuilder.append("4. 只返回JSON数组，不要其他解释\n\n");
        promptBuilder.append("示例格式：\n");
        promptBuilder.append("[\n");
        promptBuilder.append(strategyCodeTemplate);
        promptBuilder.append("]\n");
        promptBuilder.append(codeGeneratePromotion);
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
        requestBody.put("max_tokens", 8000);
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

    /**
     * 从API响应中提取批量策略信息
     */
    private JSONArray extractBatchStrategyInfoFromResponse(String response) {
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

                // 尝试解析为JSON数组
                return JSON.parseArray(jsonContent);
            }
            throw new RuntimeException("API响应格式错误");
        } catch (Exception e) {
            log.error("解析批量API响应失败: {}", e.getMessage(), e);
            throw new RuntimeException("解析批量API响应失败: " + e.getMessage());
        }
    }
}
