package com.okx.trading.util;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * OKX API签名工具类
 * 用于生成OKX API请求所需的签名
 */
@Slf4j
public class SignatureUtil {

    private static final String HMAC_SHA256 = "HmacSHA256";
    private static final Logger logger = LoggerFactory.getLogger(SignatureUtil.class);

    /**
     * 获取当前ISO时间戳
     *
     * @return ISO 8601格式的时间戳
     */
    public static String getIsoTimestamp() {
        // OKX API要求的时间戳格式为：yyyy-MM-dd'T'HH:mm:ss.SSSZ
        // 例如：2023-01-09T08:15:39.924Z
        
        // 获取当前UTC时间的毫秒时间戳
        Instant now = Instant.now();
        
        // 使用DateTimeFormatter确保生成精确的毫秒格式
        return DateTimeFormatter
                .ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .withZone(ZoneOffset.UTC)
                .format(now);
    }

    /**
     * 生成签名
     *
     * @param timestamp ISO格式的时间戳
     * @param method    HTTP请求方法，如GET、POST
     * @param requestPath API请求路径
     * @param body      请求体，对于GET请求为空字符串
     * @param secretKey 密钥
     * @return 计算得到的签名
     */
    public static String sign(String timestamp, String method, String requestPath, String body, String secretKey) {
        try {
            String preHash = timestamp + method.toUpperCase() + requestPath + (body == null ? "" : body);
            byte[] secretKeyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec keySpec = new SecretKeySpec(secretKeyBytes, HMAC_SHA256);
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(keySpec);
            byte[] signatureBytes = mac.doFinal(preHash.getBytes(StandardCharsets.UTF_8));
            return Base64.encodeBase64String(signatureBytes);
        } catch (Exception e) {
            logger.error("签名计算出错", e);
            throw new RuntimeException("签名计算异常", e);
        }
    }
} 