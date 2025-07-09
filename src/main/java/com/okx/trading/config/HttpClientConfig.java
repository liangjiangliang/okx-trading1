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
        if (proxyConfig.isEnabled()) {
            Proxy proxy = new Proxy(Proxy.Type.HTTP,
                    new InetSocketAddress(proxyConfig.getHost(), proxyConfig.getPort()));
            builder.proxy(proxy);
        }

        return builder.build();
    }

    /**
     * 创建WebSocket专用HTTP客户端
     * 优化：更快的连接超时和更频繁的ping间隔，提高连接稳定性
     *
     * @return WebSocket专用的OkHttpClient实例
     */
    @Bean(name = "webSocketHttpClient")
    public OkHttpClient webSocketHttpClient() {
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(Level.BASIC);

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)  // 减少连接超时时间，更快发现连接问题
                .readTimeout(30, TimeUnit.SECONDS)     // 减少读取超时时间
                .writeTimeout(15, TimeUnit.SECONDS)    // 减少写入超时时间
                .pingInterval(20, TimeUnit.SECONDS)    // 更频繁的ping间隔，更快检测连接状态
                .retryOnConnectionFailure(true)
                .followRedirects(false)
                .connectionPool(new ConnectionPool(8, 15, TimeUnit.MINUTES)) // 增加连接池大小
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
