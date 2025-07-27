package com.okx.trading.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FundDataDTO {
    
    private Long id;
    
    private BigDecimal totalInvestment;
    
    private BigDecimal totalProfit;
    
    private BigDecimal totalFund;
    
    private LocalDateTime recordTime;
} 