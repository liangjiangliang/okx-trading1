package com.okx.trading.config;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.RequiredArgsConstructor;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import okhttp3.logging.HttpLoggingInterceptor.Level;

/**
 * HTTP客户端配置类
 * 配置OkHttp客户端，包括代理设置、超时时间等
 */
@Configuration
@RequiredArgsConstructor
public class HttpClientConfig {

    private final OkxApiConfig okxApiConfig;
    private final ProxyConfig proxyConfig;

    /**
     * 创建并配置OkHttp客户端
     *
     * @return 配置好的OkHttpClient实例
     */
    @Bean
    public OkHttpClient okHttpClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(Level.BODY);

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(okxApiConfig.getTimeout(), TimeUnit.SECONDS)
                .readTimeout(okxApiConfig.getTimeout(), TimeUnit.SECONDS)
                .writeTimeout(okxApiConfig.getTimeout(), TimeUnit.SECONDS)
                .pingInterval(20, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .followRedirects(false)
                .connectionPool(new ConnectionPool(10, 10, TimeUnit.MINUTES))
                .addInterceptor(loggingInterceptor);

        // 如果启用代理，则设置代理 ,http 都启用 代理 ,订阅不需要代理
        if (true) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP,
                    new InetSocketAddress(proxyConfig.getHost(), proxyConfig.getPort()));
            builder.proxy(proxy);
        }

        return builder.build();
    }

    /**
     * 创建WebSocket专用HTTP客户端
     * 为WebSocket连接配置更长的超时时间和更宽松的设置
     *
     * @return WebSocket专用的OkHttpClient实例
     */
    @Bean(name = "webSocketHttpClient")
    public OkHttpClient webSocketHttpClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(Level.BASIC);

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .pingInterval(15, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .followRedirects(false)
                .connectionPool(new ConnectionPool(5, 10, TimeUnit.MINUTES))
                .addInterceptor(loggingInterceptor);

        // 如果启用代理，则设置代理
        if (proxyConfig.isEnabled()) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP,
                    new InetSocketAddress(proxyConfig.getHost(), proxyConfig.getPort()));
            builder.proxy(proxy);
        }

        return builder.build();
    }
}
