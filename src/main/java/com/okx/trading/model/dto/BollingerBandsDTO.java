package com.okx.trading.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 布林带数据传输对象
 * 用于前端展示布林带数据
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BollingerBandsDTO {
    /**
     * 时间戳
     */
    private LocalDateTime timestamp;
    
    /**
     * 收盘价
     */
    private BigDecimal price;
    
    /**
     * 中轨值 (SMA)
     */
    private BigDecimal middle;
    
    /**
     * 上轨值
     */
    private BigDecimal upper;
    
    /**
     * 下轨值
     */
    private BigDecimal lower;
    
    /**
     * 带宽 (Upper - Lower) / Middle
     */
    private BigDecimal bandwidth;
    
    /**
     * 百分比B值 (Price - Lower) / (Upper - Lower)
     * 表示价格在布林带中的相对位置，值在0-1之间
     * 低于0表示价格低于下轨，高于1表示价格高于上轨
     */
    private BigDecimal percentB;
} 