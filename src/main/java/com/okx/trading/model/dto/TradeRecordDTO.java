package com.okx.trading.model.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 交易记录数据传输对象
 */
public class TradeRecordDTO {
    
    /**
     * 交易序号
     */
    private int index;
    
    /**
     * 交易类型（买入/卖出）
     */
    private String type;
    
    /**
     * 进场时间
     */
    private LocalDateTime entryTime;
    
    /**
     * 进场价格
     */
    private BigDecimal entryPrice;
    
    /**
     * 进场金额
     */
    private BigDecimal entryAmount;
    
    /**
     * 退出时间
     */
    private LocalDateTime exitTime;
    
    /**
     * 退出价格
     */
    private BigDecimal exitPrice;
    
    /**
     * 退出金额
     */
    private BigDecimal exitAmount;
    
    /**
     * 盈亏金额
     */
    private BigDecimal profit;
    
    /**
     * 盈亏百分比
     */
    private BigDecimal profitPercentage;
    
    /**
     * 交易是否完成
     */
    private boolean closed;
    
    /**
     * 交易手续费
     */
    private BigDecimal fee;

    public TradeRecordDTO() {
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getEntryTime() {
        return entryTime;
    }

    public void setEntryTime(LocalDateTime entryTime) {
        this.entryTime = entryTime;
    }

    public BigDecimal getEntryPrice() {
        return entryPrice;
    }

    public void setEntryPrice(BigDecimal entryPrice) {
        this.entryPrice = entryPrice;
    }

    public BigDecimal getEntryAmount() {
        return entryAmount;
    }

    public void setEntryAmount(BigDecimal entryAmount) {
        this.entryAmount = entryAmount;
    }

    public LocalDateTime getExitTime() {
        return exitTime;
    }

    public void setExitTime(LocalDateTime exitTime) {
        this.exitTime = exitTime;
    }

    public BigDecimal getExitPrice() {
        return exitPrice;
    }

    public void setExitPrice(BigDecimal exitPrice) {
        this.exitPrice = exitPrice;
    }

    public BigDecimal getExitAmount() {
        return exitAmount;
    }

    public void setExitAmount(BigDecimal exitAmount) {
        this.exitAmount = exitAmount;
    }

    public BigDecimal getProfit() {
        return profit;
    }

    public void setProfit(BigDecimal profit) {
        this.profit = profit;
    }

    public BigDecimal getProfitPercentage() {
        return profitPercentage;
    }

    public void setProfitPercentage(BigDecimal profitPercentage) {
        this.profitPercentage = profitPercentage;
    }

    public boolean isClosed() {
        return closed;
    }

    public void setClosed(boolean closed) {
        this.closed = closed;
    }
    
    public BigDecimal getFee() {
        return fee;
    }
    
    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }
} 