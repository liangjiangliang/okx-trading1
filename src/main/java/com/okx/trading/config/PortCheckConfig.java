package com.okx.trading.config;

import com.okx.trading.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationStartingEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;

/**
 * 端口检查配置
 * 用于应用启动前检查并清理端口占用
 */
@Slf4j
@Configuration
public class PortCheckConfig implements ApplicationListener<ApplicationStartingEvent> {

    @Value("${server.port:8088}")
    private int serverPort;

    /**
     * 在应用启动前执行端口检查和清理
     *
     * @param event 应用启动事件
     */
    @Override
    public void onApplicationEvent(ApplicationStartingEvent event) {
        checkAndClearPort();
    }

    /**
     * 检查并清理端口占用
     */
    private void checkAndClearPort() {
        try {
            log.info("应用启动前检查端口 {} 占用情况", serverPort);
            String result = SystemUtil.checkAndKillPort(serverPort);
            log.info("端口检查结果: {}", result);
        } catch (Exception e) {
            log.error("端口检查过程中发生错误", e);
        }
    }
} 