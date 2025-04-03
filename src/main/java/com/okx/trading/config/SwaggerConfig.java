package com.okx.trading.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.view.InternalResourceViewResolver;

import java.nio.charset.StandardCharsets;
import java.util.Collections;

/**
 * Swagger配置类
 * 用于配置API文档
 */
@Configuration
@EnableWebMvc
public class SwaggerConfig implements WebMvcConfigurer {

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
                .apiInfo(apiInfo())
                .produces(Collections.singleton(MediaType.APPLICATION_JSON_VALUE))
                .consumes(Collections.singleton(MediaType.APPLICATION_JSON_VALUE))
                .useDefaultResponseMessages(false);
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
        InternalResourceViewResolver viewResolver = new InternalResourceViewResolver();
        viewResolver.setContentType("text/html;charset=UTF-8");
        return viewResolver;
    }
    
    /**
     * 配置静态资源处理
     * 确保Swagger UI静态资源能够被正确加载
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
} 