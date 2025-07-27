package com.okx.trading.controller;

import com.okx.trading.model.common.ApiResponse;
import com.okx.trading.model.dto.FundDataDTO;
import com.okx.trading.model.entity.FundDataEntity;
import com.okx.trading.service.FundCenterService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/fund-center")
@RequiredArgsConstructor
public class FundCenterController {

    private final FundCenterService fundCenterService;

    @GetMapping("/recordFundDataManually")
    public ApiResponse<FundDataEntity> recordFundDataManually() {
        return fundCenterService.recordFundData();
    }

    /**
     * 获取当天的资金数据
     */
    @GetMapping("/today")
    public ApiResponse<List<FundDataDTO>> getTodayFundData() {
        return ApiResponse.success(fundCenterService.getTodayFundData());
    }

    /**
     * 获取最近7天的资金数据
     */
    @GetMapping("/week")
    public ApiResponse<List<FundDataDTO>> getWeekFundData() {
        return ApiResponse.success(fundCenterService.getLast7DaysFundData());
    }

    /**
     * 获取最近30天的资金数据
     */
    @GetMapping("/month")
    public ApiResponse<List<FundDataDTO>> getMonthFundData() {
        return ApiResponse.success(fundCenterService.getLast30DaysFundData());
    }

    /**
     * 获取最近半年的资金数据
     */
    @GetMapping("/half-year")
    public ApiResponse<List<FundDataDTO>> getHalfYearFundData() {
        return ApiResponse.success(fundCenterService.getLast6MonthsFundData());
    }
}
