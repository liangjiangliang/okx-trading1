package com.okx.trading.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI (Swagger) 配置类
 * 使用springdoc-openapi，兼容Spring Boot 3.x
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI okxTradingOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("OKX Trading API")
                        .description("OKX加密货币交易和回测系统API")
                        .version("v1.0.0")
                        .license(new License().name("Private").url("https://www.okx.com")))
                .externalDocs(new ExternalDocumentation()
                        .description("OKX Trading Documentation")
                        .url("https://www.okx.com/docs-v5/en"));
    }
}
