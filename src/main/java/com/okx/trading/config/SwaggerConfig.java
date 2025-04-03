package com.okx.trading.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

/**
 *  
 * 用于配置API文档
 */
@Configuration
@EnableWebMvc
public class SwaggerConfig {

    /**
     * 创建API文档配置
     *
     * @return Docket实例
     */
    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("com.okx.trading.controller"))
                .paths(PathSelectors.any())
                .build()
                .apiInfo(apiInfo());
    }

    /**
     * 创建API信息
     *
     * @return ApiInfo实例
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("OKX Trading API")
                .description("OKX交易所API接口调用服务")
                .version("1.0.0")
                .contact(new Contact("Dev Team", "https://github.com/ralph-wren", "ralph_jungle@163.com"))
                .build();
    }
    
    /**
     * 配置视图解析器
     * 解决SpringFox与Spring Boot 2.7.x的兼容性问题
     *
     * @return InternalResourceViewResolver实例
     */
    @Bean
    public InternalResourceViewResolver defaultViewResolver() {
        return new InternalResourceViewResolver();
    }
} 