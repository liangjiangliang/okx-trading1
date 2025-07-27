package com.okx.trading.service;

import com.okx.trading.model.common.ApiResponse;
import com.okx.trading.model.dto.FundDataDTO;
import com.okx.trading.model.entity.FundDataEntity;
import com.okx.trading.repository.FundDataRepository;
import com.okx.trading.strategy.RealTimeStrategyManager;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FundCenterService {

    private final FundDataRepository fundDataRepository;

    private final RealTimeStrategyManager realTimeStrategyManager;
    private final RealTimeStrategyService realTimeStrategyService;

    /**
     * 每10分钟记录一次资金数据
     */
    @Scheduled(fixedRate = 600000) // 10分钟 = 600000毫秒
    public ApiResponse<FundDataEntity> recordFundData() {
        try {
            // 获取当前总投资金额和总收益
            Map<String, Object> realTimeStrategiesState = realTimeStrategyService.realTimeStrategiesState();
            Map<String, Object> statistics = (Map<String, Object>) realTimeStrategiesState.get("statistics");
            BigDecimal totalInvestment = (BigDecimal) statistics.get("totalInvestmentAmount");
            BigDecimal totalProfit = (BigDecimal) statistics.get("totalProfit");

            if (totalInvestment == null || totalProfit == null || totalInvestment.compareTo(BigDecimal.ZERO) <= 0) {
                return new ApiResponse<>(500, "获取持仓策略预估收益失败: 无效的统计数据", null);
            }

            // 计算总资金
            BigDecimal totalFund = totalInvestment.add(totalProfit);

            // 保存到数据库
            FundDataEntity entity = FundDataEntity.builder()
                    .totalInvestment(totalInvestment)
                    .totalProfit(totalProfit)
                    .totalFund(totalFund)
                    .build();

            FundDataEntity save = fundDataRepository.save(entity);
            return new ApiResponse<FundDataEntity>(200, "成功记录资金数据", save);
        } catch (Exception e) {
            // 记录异常但不中断应用
            System.err.println("记录资金数据失败: " + e.getMessage());
            return new ApiResponse<>(500, "记录资金数据失败", null);
        }
    }

    /**
     * 获取当天的资金数据
     */
    public List<FundDataDTO> getTodayFundData() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

        return fundDataRepository.findTodayData(startOfDay, endOfDay).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 获取最近7天的资金数据
     */
    public List<FundDataDTO> getLast7DaysFundData() {
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        LocalDateTime startOfDay = LocalDate.now().minusDays(6).atStartOfDay();

        return getFundDataWithSampling(startOfDay, endOfDay, 30); // 30分钟采样
    }

    /**
     * 获取最近30天的资金数据
     */
    public List<FundDataDTO> getLast30DaysFundData() {
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        LocalDateTime startOfDay = LocalDate.now().minusDays(29).atStartOfDay();

        return getFundDataWithSampling(startOfDay, endOfDay, 120); // 2小时采样
    }

    /**
     * 获取最近半年的资金数据
     */
    public List<FundDataDTO> getLast6MonthsFundData() {
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        LocalDateTime startOfDay = LocalDate.now().minusMonths(6).atStartOfDay();

        return getFundDataWithSampling(startOfDay, endOfDay, 720); // 12小时采样
    }

    /**
     * 根据时间范围获取资金数据，并进行采样以减少数据量
     */
    private List<FundDataDTO> getFundDataWithSampling(LocalDateTime startTime, LocalDateTime endTime, int sampleMinutes) {
        List<FundDataEntity> allData = fundDataRepository.findByRecordTimeBetweenOrderByRecordTimeAsc(startTime, endTime);

        // 如果数据少于100条，直接返回所有数据
        if (allData.size() < 100) {
            return allData.stream().map(this::convertToDTO).collect(Collectors.toList());
        }

        // 对数据进行采样
        LocalDateTime currentSampleTime = startTime;
        List<FundDataDTO> sampledData = allData.stream()
                .filter(data -> {
                    if (ChronoUnit.MINUTES.between(currentSampleTime, data.getRecordTime()) >= sampleMinutes) {
                        currentSampleTime.plus(sampleMinutes, ChronoUnit.MINUTES);
                        return true;
                    }
                    return false;
                })
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        // 确保包含最新的一条数据
        if (!allData.isEmpty()) {
            FundDataEntity latestData = allData.get(allData.size() - 1);
            sampledData.add(convertToDTO(latestData));
        }

        return sampledData;
    }

    /**
     * 将实体对象转换为DTO
     */
    private FundDataDTO convertToDTO(FundDataEntity entity) {
        return FundDataDTO.builder()
                .id(entity.getId())
                .totalInvestment(entity.getTotalInvestment())
                .totalProfit(entity.getTotalProfit())
                .totalFund(entity.getTotalFund())
                .recordTime(entity.getRecordTime())
                .build();
    }
}
