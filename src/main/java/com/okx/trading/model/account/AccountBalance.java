package com.okx.trading.model.account;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * 账户余额模型
 * 用于存储交易账户的余额信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountBalance {

    /**
     * 总资产价值（折算为USDT）
     */
    private BigDecimal totalEquity;

    /**
     * 账户类型（0:现货账户, 1:模拟账户）
     */
    private Integer accountType;

    /**
     * 账户ID
     */
    private String accountId;

    /**
     * 可用余额（折算为USDT）
     */
    private BigDecimal availableBalance;

    /**
     * 冻结余额（折算为USDT）
     */
    private BigDecimal frozenBalance;

    /**
     * 各币种余额列表
     */
    private List<AssetBalance> assetBalances;

    /**
     * 币种余额模型
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AssetBalance {

        /**
         * 币种，如BTC, ETH
         */
        private String asset;

        /**
         * 可用余额
         */
        private BigDecimal available;

        /**
         * 冻结余额
         */
        private BigDecimal frozen;

        /**
         * 总余额 = 可用余额 + 冻结余额
         */
        private BigDecimal total;

        /**
         * 美元价值
         */
        private BigDecimal usdValue;
    }

    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }
}
