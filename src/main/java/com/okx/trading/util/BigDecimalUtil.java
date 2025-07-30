package com.okx.trading.util;

import java.math.BigDecimal;

public class BigDecimalUtil {
    /**
     * 如果传入的str不存在或值为null，则返回BigDecimal.ZERO
     * @param str 字符串形式的数字
     * @return BigDecimal 对象，如果字符串为null或空字符串，则返回 BigDecimal.ZERO
     */
    public static BigDecimal safeGen(String str) {
        if (str == null || str.isEmpty()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(str);
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

}
