package com.okx.trading.controller;

import com.okx.trading.model.dto.IndicatorValueDTO;
import com.okx.trading.service.TechnicalIndicatorService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 技术指标控制器
 * 提供计算和查询技术指标的REST API
 */
@RestController
@RequestMapping("/indicator")
@Api(tags = "技术指标API", description = "提供技术指标计算和查询功能")
public class TechnicalIndicatorController {

    @Autowired
    private TechnicalIndicatorService technicalIndicatorService;

    /**
     * 计算指定交易对和时间间隔的最新K线技术指标值
     *
     * @param symbol        交易对
     * @param interval      时间间隔
     * @param indicatorType 指标类型
     * @param params        指标参数
     * @return 技术指标值DTO
     */
    @GetMapping("/calculate")
    @ApiOperation(value = "计算技术指标", notes = "计算指定交易对和时间间隔的最新K线技术指标值")
    public ResponseEntity<IndicatorValueDTO> calculateIndicator(
            @ApiParam(value = "交易对", required = true, example = "BTC-USDT")
            @RequestParam String symbol,
            
            @ApiParam(value = "时间间隔", required = true, example = "1h")
            @RequestParam String interval,
            
            @ApiParam(value = "指标类型", required = true, example = "MACD")
            @RequestParam String indicatorType,
            
            @ApiParam(value = "指标参数", required = true, example = "12,26,9")
            @RequestParam String params) {
        
        IndicatorValueDTO result = technicalIndicatorService.calculateLastIndicator(symbol, interval, indicatorType, params);
        return ResponseEntity.ok(result);
    }

    /**
     * 批量计算多个指标的值
     *
     * @param symbol     交易对
     * @param interval   时间间隔
     * @param indicators 指标类型和参数的映射
     * @return 指标类型到值的映射
     */
    @PostMapping("/calculate-batch")
    @ApiOperation(value = "批量计算技术指标", notes = "计算指定交易对和时间间隔的多个技术指标值")
    public ResponseEntity<Map<String, IndicatorValueDTO>> calculateMultipleIndicators(
            @ApiParam(value = "交易对", required = true, example = "BTC-USDT")
            @RequestParam String symbol,
            
            @ApiParam(value = "时间间隔", required = true, example = "1h")
            @RequestParam String interval,
            
            @ApiParam(value = "指标类型和参数的映射", required = true)
            @RequestBody Map<String, String> indicators) {
        
        Map<String, IndicatorValueDTO> result = technicalIndicatorService.calculateMultipleIndicators(symbol, interval, indicators);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取所有支持的技术指标类型
     *
     * @return 支持的技术指标类型列表
     */
    @GetMapping("/supported")
    @ApiOperation(value = "获取支持的指标类型", notes = "获取系统支持的所有技术指标类型")
    public ResponseEntity<List<String>> getSupportedIndicators() {
        List<String> indicators = technicalIndicatorService.getSupportedIndicators();
        return ResponseEntity.ok(indicators);
    }

    /**
     * 获取指定技术指标的参数说明
     *
     * @param indicatorType 指标类型
     * @return 参数说明
     */
    @GetMapping("/params-description")
    @ApiOperation(value = "获取指标参数说明", notes = "获取指定技术指标的参数说明")
    public ResponseEntity<Map<String, String>> getIndicatorParamsDescription(
            @ApiParam(value = "指标类型", required = true, example = "MACD")
            @RequestParam String indicatorType) {
        
        String description = technicalIndicatorService.getIndicatorParamsDescription(indicatorType);
        
        Map<String, String> response = new HashMap<>();
        response.put("indicatorType", indicatorType);
        response.put("description", description);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 获取所有技术指标的参数说明
     *
     * @return 指标类型到参数说明的映射
     */
    @GetMapping("/all-params-descriptions")
    @ApiOperation(value = "获取所有指标参数说明", notes = "获取所有支持的技术指标参数说明")
    public ResponseEntity<Map<String, String>> getAllIndicatorParamsDescriptions() {
        List<String> indicators = technicalIndicatorService.getSupportedIndicators();
        Map<String, String> descriptions = new HashMap<>();
        
        for (String indicator : indicators) {
            descriptions.put(indicator, technicalIndicatorService.getIndicatorParamsDescription(indicator));
        }
        
        return ResponseEntity.ok(descriptions);
    }
} 