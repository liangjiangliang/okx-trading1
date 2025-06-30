package com.okx.trading.service.impl;

import com.okx.trading.model.entity.RealTimeStrategyEntity;
import com.okx.trading.model.trade.Order;
import com.okx.trading.service.NotificationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Server酱通知服务实现类
 */
@Slf4j
@Service
public class ServerChanNotificationServiceImpl implements NotificationService {

    @Value("${server.chan.key:}")
    private String serverChanKey;

    @Value("${notification.trade.enabled:false}")
    private boolean tradeNotificationEnabled;

    @Value("${notification.error.enabled:false}")
    private boolean errorNotificationEnabled;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String SERVER_CHAN_URL = "https://sctapi.ftqq.com/";

    @Override
    public boolean sendTradeNotification(RealTimeStrategyEntity strategy, Order order, String side, String signalPrice) {
        if (!tradeNotificationEnabled || serverChanKey.isEmpty()) {
            return false;
        }

        try {
            // 构建通知内容
            String title = String.format("【交易提醒】%s %s 信号", strategy.getSymbol(), side);
            
            StringBuilder content = new StringBuilder();
            content.append("## 交易详情\n\n");
            content.append("- **策略名称**: ").append(strategy.getStrategyName()).append("\n");
            content.append("- **交易对**: ").append(strategy.getSymbol()).append("\n");
            content.append("- **K线周期**: ").append(strategy.getInterval()).append("\n");
            content.append("- **交易类型**: ").append(side).append("\n");
            content.append("- **信号价格**: ").append(signalPrice).append("\n");
            content.append("- **成交价格**: ").append(order.getPrice()).append("\n");
            
            if ("BUY".equals(side)) {
                content.append("- **买入金额**: ").append(order.getCummulativeQuoteQty()).append("\n");
                content.append("- **买入数量**: ").append(order.getExecutedQty()).append("\n");
            } else {
                content.append("- **卖出数量**: ").append(order.getExecutedQty()).append("\n");
                content.append("- **卖出金额**: ").append(order.getCummulativeQuoteQty()).append("\n");
                
                // 计算利润
                if (strategy.getLastTradeAmount() != null) {
                    BigDecimal profit = order.getCummulativeQuoteQty().subtract(BigDecimal.valueOf(strategy.getLastTradeAmount()));
                    BigDecimal profitRate = profit.divide(BigDecimal.valueOf(strategy.getLastTradeAmount()), 4, BigDecimal.ROUND_HALF_UP)
                            .multiply(BigDecimal.valueOf(100));
                    
                    content.append("- **本次利润**: ").append(profit).append(" (").append(profitRate).append("%)\n");
                }
            }
            
            content.append("- **手续费**: ").append(order.getFee()).append(" ").append(order.getFeeCurrency()).append("\n");
            content.append("- **成交时间**: ").append(FORMATTER.format(order.getCreateTime())).append("\n");
            
            // 发送消息
            return sendServerChanMessage(title, content.toString());
        } catch (Exception e) {
            log.error("发送交易通知失败: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean sendStrategyErrorNotification(RealTimeStrategyEntity strategy, String errorMessage) {
        if (!errorNotificationEnabled || serverChanKey.isEmpty()) {
            return false;
        }

        try {
            String title = String.format("【策略错误】%s - %s", strategy.getStrategyName(), strategy.getSymbol());
            
            StringBuilder content = new StringBuilder();
            content.append("## 错误详情\n\n");
            content.append("- **策略名称**: ").append(strategy.getStrategyName()).append("\n");
            content.append("- **策略代码**: ").append(strategy.getStrategyCode()).append("\n");
            content.append("- **交易对**: ").append(strategy.getSymbol()).append("\n");
            content.append("- **K线周期**: ").append(strategy.getInterval()).append("\n");
            content.append("- **错误时间**: ").append(FORMATTER.format(LocalDateTime.now())).append("\n");
            content.append("- **错误信息**: ").append(errorMessage).append("\n");
            
            return sendServerChanMessage(title, content.toString());
        } catch (Exception e) {
            log.error("发送策略错误通知失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 发送Server酱消息
     *
     * @param title 消息标题
     * @param content 消息内容 (支持Markdown格式)
     * @return 是否发送成功
     */
    private boolean sendServerChanMessage(String title, String content) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            // 构建请求URL
            String url = SERVER_CHAN_URL + serverChanKey + ".send";
            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded");
            
            // 构建请求参数
            String params = "title=" + java.net.URLEncoder.encode(title, "UTF-8") + 
                           "&desp=" + java.net.URLEncoder.encode(content, "UTF-8");
            
            StringEntity entity = new StringEntity(params);
            httpPost.setEntity(entity);
            
            // 发送请求
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getStatusLine().getStatusCode();
                String responseBody = EntityUtils.toString(response.getEntity());
                
                if (statusCode == 200 && responseBody.contains("\"code\":0")) {
                    log.info("Server酱通知发送成功: {}", title);
                    return true;
                } else {
                    log.error("Server酱通知发送失败: 状态码={}, 响应={}", statusCode, responseBody);
                    return false;
                }
            }
        } catch (Exception e) {
            log.error("发送Server酱通知失败: {}", e.getMessage(), e);
            return false;
        }
    }
} 