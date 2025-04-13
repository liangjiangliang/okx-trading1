package com.okx.trading.config;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.SwaggerResource;
import springfox.documentation.swagger.web.SwaggerResourcesProvider;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * Swagger配置类
 * 提供API文档自动生成功能
 */
@Configuration
@EnableSwagger2
public class SwaggerConfig {

    /**
     * 创建API文档配置
     *
     * @return Docket对象
     */
    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo())
                .select()
                // 指定要扫描的包
                .apis(RequestHandlerSelectors.basePackage("com.okx.trading.controller"))
                .paths(PathSelectors.any())
                .build()
                // 注册通用的数据类型，解决dataTypeClass为Void的问题
                .directModelSubstitute(LocalDateTime.class, String.class)
                .directModelSubstitute(BigDecimal.class, Double.class)
                // 设置字符集为UTF-8，确保中文正常显示
                .pathMapping("/");
    }

    /**
     * API文档基本信息
     *
     * @return API信息对象
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                .title("OKX Trading API")
                .description("OKX交易平台API接口文档")
                .contact(new Contact("开发团队", "https://github.com/ralph-wren", "ralph_jungle@163.com"))
                .version("1.0.0")
                .build();
    }

//    /**
//     * 配置Swagger UI的资源处理器
//     * 确保中文正常显示
//     */
//    @Bean
//    public SwaggerResourcesProcessor swaggerResourcesProcessor() {
//        return new SwaggerResourcesProcessor();
//    }

    @Bean
    @Primary  // 让 Spring 选择这个 Bean
    public SwaggerResourcesProvider swaggerResourcesProcessor() {
        return () -> {
            List<SwaggerResource> resources = new ArrayList<>();
            SwaggerResource swaggerResource = new SwaggerResource();
            swaggerResource.setName("API Docs");
            swaggerResource.setLocation("/v2/api-docs");
            swaggerResource.setSwaggerVersion("2.0");
            resources.add(swaggerResource);
            return resources;
        };
    }

    /**
     * Swagger资源处理器内部类
     * 用于处理Swagger UI中的中文显示问题
     */
    public static class SwaggerResourcesProcessor implements springfox.documentation.swagger.web.SwaggerResourcesProvider {
        @Override
        public java.util.List<springfox.documentation.swagger.web.SwaggerResource> get() {
            // 创建一个默认的资源列表
            springfox.documentation.swagger.web.SwaggerResource resource = new springfox.documentation.swagger.web.SwaggerResource();
            resource.setName("default");
            resource.setUrl("/v2/api-docs");
            resource.setSwaggerVersion("2.0");

            // 返回资源列表
            return java.util.Collections.singletonList(resource);
        }
    }
}
