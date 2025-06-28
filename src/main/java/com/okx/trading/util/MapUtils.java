package com.okx.trading.util;

import java.util.HashMap;
import java.util.Map;

/**
 * Map工具类
 * 提供便捷的Map创建方法
 */
public class MapUtils {
    
    /**
     * 创建Map的便捷方法
     * @param params 键值对参数，必须是偶数个
     * @param <T> 值的类型
     * @return 创建的Map
     * @throws IllegalArgumentException 如果参数个数不是偶数
     */
    public static <T> Map<String, T> of(Object... params) {
        if (params.length % 2 != 0) {
            throw new IllegalArgumentException("params must be even");
        }
        HashMap<String, T> hashMap = new HashMap<>();
        for (int i = 0; i < params.length; i += 2) {
            hashMap.put(params[i].toString(), (T) params[i + 1]);
        }
        return hashMap;
    }
}
