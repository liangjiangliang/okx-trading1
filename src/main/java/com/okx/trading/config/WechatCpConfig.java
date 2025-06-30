package com.okx.trading.config;

import me.chanjar.weixin.cp.api.WxCpService;
import me.chanjar.weixin.cp.api.impl.WxCpServiceImpl;
import me.chanjar.weixin.cp.config.impl.WxCpDefaultConfigImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 企业微信配置类
 * 仅在选择企业微信通知方式时生效
 */
@Configuration
@ConditionalOnProperty(name = "notification.type", havingValue = "wechat_cp")
public class WechatCpConfig {

    @Value("${wechat.cp.corpId}")
    private String corpId;

    @Value("${wechat.cp.agentId}")
    private Integer agentId;

    @Value("${wechat.cp.secret}")
    private String secret;

    @Bean
    public WxCpService wxCpService() {
        WxCpDefaultConfigImpl config = new WxCpDefaultConfigImpl();
        config.setCorpId(corpId);
        config.setCorpSecret(secret);
        config.setAgentId(agentId);

        WxCpService wxCpService = new WxCpServiceImpl();
        wxCpService.setWxCpConfigStorage(config);
        return wxCpService;
    }
} 