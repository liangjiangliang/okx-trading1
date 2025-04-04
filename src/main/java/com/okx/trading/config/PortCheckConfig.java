package com.okx.trading.config;

import com.okx.trading.util.SystemUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * 端口检查配置
 * 用于应用启动前检查并清理端口占用
 */
@Slf4j
@Configuration
public class PortCheckConfig implements ApplicationListener<ApplicationPreparedEvent>{

    /**
     * 在应用启动前执行端口检查和清理
     *
     * @param event 应用启动事件
     */
    @Override
    public void onApplicationEvent(ApplicationPreparedEvent event){
        // 从事件中获取Environment对象
        Environment environment = event.getApplicationContext().getEnvironment();
        // 获取服务器端口配置
        String serverPortStr = environment.getProperty("server.port");
        if (serverPortStr == null || serverPortStr.isEmpty()) {
            log.warn("未找到server.port配置，使用默认端口8088");
            serverPortStr = "8088";
        }
        
        int serverPort = Integer.parseInt(serverPortStr);
        checkAndClearPort(serverPort);
    }

    /**
     * 检查并清理端口占用
     * 
     * @param serverPort 需要检查的服务器端口
     */
    private void checkAndClearPort(int serverPort){
        try{
            log.info("应用启动前检查端口 {} 占用情况", serverPort);
            String result = SystemUtil.checkAndKillPort(serverPort);
            log.info("端口检查结果: {}", result);
        }catch(Exception e){
            log.error("端口检查过程中发生错误", e);
        }
    }
}
