//package com.okx.trading.controller;
//
//
//import com.okx.trading.model.entity.RealTimeStrategyEntity;
//import com.okx.trading.service.RealTimeStrategyService;
//import io.swagger.annotations.*;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.lang3.StringUtils;
//import org.springframework.format.annotation.DateTimeFormat;
//import org.springframework.web.bind.annotation.*;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.Optional;
//
///**
// * 实时运行策略控制器
// * 提供实时策略的CRUD操作和状态管理接口
// */
//@Slf4j
//@RestController
//@RequestMapping("/api/real-time-strategy")
//@RequiredArgsConstructor
//@Api(tags = "实时运行策略管理")
//public class RealTimeStrategyController {
//
//    private final RealTimeStrategyService realTimeStrategyService;
//
//    /**
//     * 获取所有实时策略
//     */
//    @GetMapping("/list")
//    @ApiOperation(value = "获取所有实时策略", notes = "获取系统中所有的实时策略列表，包括已激活和未激活的策略")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "获取成功"),
//            @ApiResponse(code = 500, message = "服务器内部错误")
//    })
//    public com.okx.trading.util.ApiResponse<List<RealTimeStrategyEntity>> getAllRealTimeStrategies() {
//        try {
//            List<RealTimeStrategyEntity> strategies = realTimeStrategyService.getAllRealTimeStrategies();
//            return com.okx.trading.util.ApiResponse.success(strategies);
//        } catch (Exception e) {
//            log.error("获取所有实时策略失败", e);
//            return com.okx.trading.util.ApiResponse.error(503, "获取实时策略列表失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 获取所有有效的实时策略
//     */
//    @GetMapping("/active")
//    @ApiOperation(value = "获取所有有效的实时策略", notes = "获取系统中所有已激活状态的实时策略列表")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "获取成功"),
//            @ApiResponse(code = 500, message = "服务器内部错误")
//    })
//    public com.okx.trading.util.ApiResponse<List<RealTimeStrategyEntity>> getActiveRealTimeStrategies() {
//        try {
//            List<RealTimeStrategyEntity> strategies = realTimeStrategyService.getActiveRealTimeStrategies();
//            return com.okx.trading.util.ApiResponse.success(strategies);
//        } catch (Exception e) {
//            log.error("获取有效实时策略失败", e);
//            return com.okx.trading.util.ApiResponse.error(503, "获取有效实时策略列表失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 获取正在运行的实时策略
//     */
//    @GetMapping("/running")
//    @ApiOperation(value = "获取正在运行的实时策略", notes = "获取系统中所有状态为RUNNING的实时策略列表")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "获取成功"),
//            @ApiResponse(code = 500, message = "服务器内部错误")
//    })
//    public com.okx.trading.util.ApiResponse<List<RealTimeStrategyEntity>> getRunningRealTimeStrategies() {
//        try {
//            List<RealTimeStrategyEntity> strategies = realTimeStrategyService.getRunningRealTimeStrategies();
//            return com.okx.trading.util.ApiResponse.success(strategies);
//        } catch (Exception e) {
//            log.error("获取运行中实时策略失败", e);
//            return com.okx.trading.util.ApiResponse.error(503, "获取运行中实时策略列表失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 根据策略代码获取实时策略
//     */
//    @GetMapping("/code/{strategyCode}")
//    @ApiOperation(value = "根据策略代码获取实时策略", notes = "通过唯一的策略代码查询特定的实时策略详情")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "获取成功"),
//            @ApiResponse(code = 400, message = "策略代码不能为空"),
//            @ApiResponse(code = 404, message = "策略不存在"),
//            @ApiResponse(code = 500, message = "服务器内部错误")
//    })
//    public com.okx.trading.util.ApiResponse<RealTimeStrategyEntity> getRealTimeStrategyByCode(
//            @ApiParam(value = "策略代码", required = true, example = "STRATEGY_001") @PathVariable String strategyCode) {
//        try {
//            if (StringUtils.isBlank(strategyCode)) {
//                return com.okx.trading.util.ApiResponse.error(503, "策略代码不能为空");
//            }
//
//            Optional<RealTimeStrategyEntity> strategy = realTimeStrategyService.getRealTimeStrategyByCode(strategyCode);
//            if (strategy.isPresent()) {
//                return com.okx.trading.util.ApiResponse.success(strategy.get());
//            } else {
//                return com.okx.trading.util.ApiResponse.error(503, "策略不存在: " + strategyCode);
//            }
//        } catch (Exception e) {
//            log.error("根据策略代码获取实时策略失败: {}", strategyCode, e);
//            return com.okx.trading.util.ApiResponse.error(503, "获取实时策略失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 根据ID获取实时策略
//     */
//    @GetMapping("/id/{id}")
//    @ApiOperation(value = "根据ID获取实时策略", notes = "通过数据库主键ID查询特定的实时策略详情")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "获取成功"),
//            @ApiResponse(code = 400, message = "策略ID不能为空"),
//            @ApiResponse(code = 404, message = "策略不存在"),
//            @ApiResponse(code = 500, message = "服务器内部错误")
//    })
//    public com.okx.trading.util.ApiResponse<RealTimeStrategyEntity> getRealTimeStrategyById(
//            @ApiParam(value = "策略ID", required = true, example = "1") @PathVariable Long id) {
//        try {
//            if (id == null) {
//                return com.okx.trading.util.ApiResponse.error(503, "策略ID不能为空");
//            }
//
//            Optional<RealTimeStrategyEntity> strategy = realTimeStrategyService.getRealTimeStrategyById(id);
//            if (strategy.isPresent()) {
//                return com.okx.trading.util.ApiResponse.success(strategy.get());
//            } else {
//                return com.okx.trading.util.ApiResponse.error(503, "策略不存在，ID: " + id);
//            }
//        } catch (Exception e) {
//            log.error("根据ID获取实时策略失败: {}", id, e);
//            return com.okx.trading.util.ApiResponse.error(503, "获取实时策略失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 根据策略信息代码获取有效的实时策略
//     */
//    @GetMapping("/info-code/{strategyCode}")
//    @ApiOperation(value = "根据策略信息代码获取有效的实时策略", notes = "通过策略信息代码查询所有关联的有效实时策略")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "获取成功"),
//            @ApiResponse(code = 400, message = "策略信息代码不能为空"),
//            @ApiResponse(code = 500, message = "服务器内部错误")
//    })
//    public com.okx.trading.util.ApiResponse<List<RealTimeStrategyEntity>> getActiveRealTimeStrategiesByInfoCode(
//            @ApiParam(value = "策略信息代码", required = true, example = "MA_CROSS_STRATEGY") @PathVariable String strategyCode) {
//        try {
//            if (StringUtils.isBlank(strategyCode)) {
//                return com.okx.trading.util.ApiResponse.error(503, "策略信息代码不能为空");
//            }
//
//            List<RealTimeStrategyEntity> strategies = realTimeStrategyService.getActiveRealTimeStrategiesByCode(strategyCode);
//            return com.okx.trading.util.ApiResponse.success(strategies);
//        } catch (Exception e) {
//            log.error("根据策略信息代码获取实时策略失败: {}", strategyCode, e);
//            return com.okx.trading.util.ApiResponse.error(503, "获取实时策略失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 根据交易对获取有效的实时策略
//     */
//    @GetMapping("/symbol/{symbol}")
//    @ApiOperation(value = "根据交易对获取有效的实时策略", notes = "通过交易对符号查询所有关联的有效实时策略")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "获取成功"),
//            @ApiResponse(code = 400, message = "交易对符号不能为空"),
//            @ApiResponse(code = 500, message = "服务器内部错误")
//    })
//    public com.okx.trading.util.ApiResponse<List<RealTimeStrategyEntity>> getActiveRealTimeStrategiesBySymbol(
//            @ApiParam(value = "交易对符号", required = true, example = "BTC-USDT") @PathVariable String symbol) {
//        try {
//            if (StringUtils.isBlank(symbol)) {
//                return com.okx.trading.util.ApiResponse.error(503, "交易对符号不能为空");
//            }
//
//            List<RealTimeStrategyEntity> strategies = realTimeStrategyService.getActiveRealTimeStrategiesBySymbol(symbol);
//            return com.okx.trading.util.ApiResponse.success(strategies);
//        } catch (Exception e) {
//            log.error("根据交易对获取实时策略失败: {}", symbol, e);
//            return com.okx.trading.util.ApiResponse.error(503, "获取实时策略失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 根据状态获取实时策略
//     */
//    @GetMapping("/status/{status}")
//    @ApiOperation(value = "根据状态获取实时策略", notes = "通过运行状态查询所有匹配的实时策略")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "获取成功"),
//            @ApiResponse(code = 400, message = "运行状态不能为空"),
//            @ApiResponse(code = 500, message = "服务器内部错误")
//    })
//    public com.okx.trading.util.ApiResponse<List<RealTimeStrategyEntity>> getRealTimeStrategiesByStatus(
//            @ApiParam(value = "运行状态", required = true, example = "RUNNING", allowableValues = "RUNNING,STOPPED,COMPLETED,ERROR") @PathVariable String status) {
//        try {
//            if (StringUtils.isBlank(status)) {
//                return com.okx.trading.util.ApiResponse.error(503, "运行状态不能为空");
//            }
//
//            List<RealTimeStrategyEntity> strategies = realTimeStrategyService.getRealTimeStrategiesByStatus(status);
//            return com.okx.trading.util.ApiResponse.success(strategies);
//        } catch (Exception e) {
//            log.error("根据状态获取实时策略失败: {}", status, e);
//            return com.okx.trading.util.ApiResponse.error(503, "获取实时策略失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 获取指定时间范围内创建的实时策略
//     */
//    @GetMapping("/time-range")
//    @ApiOperation(value = "获取指定时间范围内创建的实时策略", notes = "查询在指定时间范围内创建的所有实时策略")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "获取成功"),
//            @ApiResponse(code = 400, message = "时间参数错误"),
//            @ApiResponse(code = 500, message = "服务器内部错误")
//    })
//    public com.okx.trading.util.ApiResponse<List<RealTimeStrategyEntity>> getRealTimeStrategiesByTimeRange(
//            @ApiParam(value = "开始时间", required = true, example = "2024-01-01 00:00:00") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
//            @ApiParam(value = "结束时间", required = true, example = "2024-12-31 23:59:59") @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
//        try {
//            if (startTime == null || endTime == null) {
//                return com.okx.trading.util.ApiResponse.error(503, "开始时间和结束时间不能为空");
//            }
//
//            if (startTime.isAfter(endTime)) {
//                return com.okx.trading.util.ApiResponse.error(503, "开始时间不能晚于结束时间");
//            }
//
//            List<RealTimeStrategyEntity> strategies = realTimeStrategyService.getRealTimeStrategiesByTimeRange(startTime, endTime);
//            return com.okx.trading.util.ApiResponse.success(strategies);
//        } catch (Exception e) {
//            log.error("根据时间范围获取实时策略失败: {} - {}", startTime, endTime, e);
//            return com.okx.trading.util.ApiResponse.error(503, "获取实时策略失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 创建新的实时策略
//     */
//    @PostMapping("/create")
//    @ApiOperation(value = "创建新的实时策略", notes = "创建一个新的实时策略实例，可以指定策略代码或自动生成")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "创建成功"),
//            @ApiResponse(code = 400, message = "参数错误"),
//            @ApiResponse(code = 500, message = "服务器内部错误")
//    })
//    public com.okx.trading.util.ApiResponse<RealTimeStrategyEntity> createRealTimeStrategy(
//            @ApiParam(value = "策略代码", example = "STRATEGY_001") @RequestParam(required = false) String strategyCode,
//            @ApiParam(value = "策略名称", example = "海龟交易策略") @RequestParam(required = false) String strategyName,
//            @ApiParam(value = "交易对符号", required = true, example = "BTC-USDT") @RequestParam String symbol,
//            @ApiParam(value = "K线周期", required = true, example = "1m", allowableValues = "1m,5m,15m,30m,1h,4h,1d") @RequestParam String interval,
//            @ApiParam(value = "交易金额", example = "100.0") @RequestParam(required = false) Double tradeAmount) {
//        try {
//            if (StringUtils.isBlank(strategyCode)) {
//                return com.okx.trading.util.ApiResponse.error(503, "策略代码不能为空");
//            }
//            if (StringUtils.isBlank(symbol)) {
//                return com.okx.trading.util.ApiResponse.error(503, "交易对符号不能为空");
//            }
//            if (StringUtils.isBlank(interval)) {
//                return com.okx.trading.util.ApiResponse.error(503, "K线周期不能为空");
//            }
//
//            RealTimeStrategyEntity strategy = realTimeStrategyService.createRealTimeStrategy(
//                    strategyCode, symbol, interval, tradeAmount, strategyName);
//
//            return com.okx.trading.util.ApiResponse.success(strategy);
//        } catch (Exception e) {
//            log.error("创建实时策略失败: {}", strategyName, e);
//            return com.okx.trading.util.ApiResponse.error(503, "创建实时策略失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 更新实时策略
//     */
//    @PutMapping("/update")
//    @ApiOperation(value = "更新实时策略", notes = "更新现有实时策略的配置信息")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "更新成功"),
//            @ApiResponse(code = 400, message = "参数错误"),
//            @ApiResponse(code = 404, message = "策略不存在"),
//            @ApiResponse(code = 500, message = "服务器内部错误")
//    })
//    public com.okx.trading.util.ApiResponse<RealTimeStrategyEntity> updateRealTimeStrategy(
//            @ApiParam(value = "实时策略实体", required = true) @RequestBody RealTimeStrategyEntity realTimeStrategy) {
//        try {
//            if (realTimeStrategy == null) {
//                return com.okx.trading.util.ApiResponse.error(503, "实时策略不能为空");
//            }
//            if (realTimeStrategy.getId() == null) {
//                return com.okx.trading.util.ApiResponse.error(503, "策略ID不能为空");
//            }
//
//            RealTimeStrategyEntity updated = realTimeStrategyService.updateRealTimeStrategy(realTimeStrategy);
//            return com.okx.trading.util.ApiResponse.success(updated);
//        } catch (Exception e) {
//            log.error("更新实时策略失败", e);
//            return com.okx.trading.util.ApiResponse.error(503, "更新实时策略失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 启动实时策略
//     */
//    @PostMapping("/start/{strategyCode}")
//    @ApiOperation(value = "启动实时策略", notes = "启动指定的实时策略，将状态设置为RUNNING")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "启动成功"),
//            @ApiResponse(code = 400, message = "策略代码不能为空"),
//            @ApiResponse(code = 404, message = "策略不存在"),
//            @ApiResponse(code = 500, message = "服务器内部错误")
//    })
//    public com.okx.trading.util.ApiResponse<String> startRealTimeStrategy(
//            @ApiParam(value = "策略代码", required = true, example = "STRATEGY_001") @PathVariable String strategyCode) {
//        try {
//            if (StringUtils.isBlank(strategyCode)) {
//                return com.okx.trading.util.ApiResponse.error(503, "策略代码不能为空");
//            }
//
//            boolean success = realTimeStrategyService.startRealTimeStrategy(strategyCode);
//            if (success) {
//                return com.okx.trading.util.ApiResponse.success("启动策略成功: " + strategyCode);
//            } else {
//                return com.okx.trading.util.ApiResponse.error(503, "启动策略失败: " + strategyCode);
//            }
//        } catch (Exception e) {
//            log.error("启动实时策略失败: {}", strategyCode, e);
//            return com.okx.trading.util.ApiResponse.error(503, "启动策略失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 停止实时策略
//     */
//    @PostMapping("/stop/{strategyCode}")
//    @ApiOperation(value = "停止实时策略", notes = "停止指定的实时策略，将状态设置为STOPPED")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "停止成功"),
//            @ApiResponse(code = 400, message = "策略代码不能为空"),
//            @ApiResponse(code = 404, message = "策略不存在"),
//            @ApiResponse(code = 500, message = "服务器内部错误")
//    })
//    public com.okx.trading.util.ApiResponse<String> stopRealTimeStrategy(
//            @ApiParam(value = "策略代码", required = true, example = "STRATEGY_001") @PathVariable String strategyCode) {
//        try {
//            if (StringUtils.isBlank(strategyCode)) {
//                return com.okx.trading.util.ApiResponse.error(503, "策略代码不能为空");
//            }
//
//            boolean success = realTimeStrategyService.stopRealTimeStrategy(strategyCode);
//            if (success) {
//                return com.okx.trading.util.ApiResponse.success("停止策略成功: " + strategyCode);
//            } else {
//                return com.okx.trading.util.ApiResponse.error(503, "停止策略失败: " + strategyCode);
//            }
//        } catch (Exception e) {
//            log.error("停止实时策略失败: {}", strategyCode, e);
//            return com.okx.trading.util.ApiResponse.error(503, "停止策略失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 激活策略
//     */
//    @PostMapping("/activate/{strategyCode}")
//    @ApiOperation(value = "激活策略", notes = "激活指定的实时策略，将isActive设置为true")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "激活成功"),
//            @ApiResponse(code = 400, message = "策略代码不能为空"),
//            @ApiResponse(code = 404, message = "策略不存在"),
//            @ApiResponse(code = 500, message = "服务器内部错误")
//    })
//    public com.okx.trading.util.ApiResponse<String> activateStrategy(
//            @ApiParam(value = "策略代码", required = true, example = "STRATEGY_001") @PathVariable String strategyCode) {
//        try {
//            if (StringUtils.isBlank(strategyCode)) {
//                return com.okx.trading.util.ApiResponse.error(503, "策略代码不能为空");
//            }
//
//            boolean success = realTimeStrategyService.activateStrategy(strategyCode);
//            if (success) {
//                return com.okx.trading.util.ApiResponse.success("激活策略成功: " + strategyCode);
//            } else {
//                return com.okx.trading.util.ApiResponse.error(503, "激活策略失败: " + strategyCode);
//            }
//        } catch (Exception e) {
//            log.error("激活策略失败: {}", strategyCode, e);
//            return com.okx.trading.util.ApiResponse.error(503, "激活策略失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 停用策略
//     */
//    @PostMapping("/deactivate/{strategyCode}")
//    @ApiOperation(value = "停用策略", notes = "停用指定的实时策略，将isActive设置为false")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "停用成功"),
//            @ApiResponse(code = 400, message = "策略代码不能为空"),
//            @ApiResponse(code = 404, message = "策略不存在"),
//            @ApiResponse(code = 500, message = "服务器内部错误")
//    })
//    public com.okx.trading.util.ApiResponse<String> deactivateStrategy(
//            @ApiParam("策略代码") @PathVariable String strategyCode) {
//        try {
//            if (StringUtils.isBlank(strategyCode)) {
//                return com.okx.trading.util.ApiResponse.error(503, "策略代码不能为空");
//            }
//
//            boolean success = realTimeStrategyService.deactivateStrategy(strategyCode);
//            if (success) {
//                return com.okx.trading.util.ApiResponse.success("停用策略成功: " + strategyCode);
//            } else {
//                return com.okx.trading.util.ApiResponse.error(503, "停用策略失败: " + strategyCode);
//            }
//        } catch (Exception e) {
//            log.error("停用策略失败: {}", strategyCode, e);
//            return com.okx.trading.util.ApiResponse.error(503, "停用策略失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 删除实时策略
//     */
//    @DeleteMapping("/delete/{strategyCode}")
//    @ApiOperation(value = "删除实时策略", notes = "永久删除指定的实时策略记录")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "删除成功"),
//            @ApiResponse(code = 400, message = "策略代码不能为空"),
//            @ApiResponse(code = 404, message = "策略不存在"),
//            @ApiResponse(code = 500, message = "服务器内部错误")
//    })
//    public com.okx.trading.util.ApiResponse<String> deleteRealTimeStrategy(
//            @ApiParam("策略代码") @PathVariable String strategyCode) {
//        try {
//            if (StringUtils.isBlank(strategyCode)) {
//                return com.okx.trading.util.ApiResponse.error(503, "策略代码不能为空");
//            }
//
//            boolean success = realTimeStrategyService.deleteRealTimeStrategy(strategyCode);
//            if (success) {
//                return com.okx.trading.util.ApiResponse.success("删除策略成功: " + strategyCode);
//            } else {
//                return com.okx.trading.util.ApiResponse.error(503, "删除策略失败: " + strategyCode);
//            }
//        } catch (Exception e) {
//            log.error("删除实时策略失败: {}", strategyCode, e);
//            return com.okx.trading.util.ApiResponse.error(503, "删除策略失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 检查策略代码是否已存在
//     */
//    @GetMapping("/exists/{strategyCode}")
//    @ApiOperation(value = "检查策略代码是否已存在", notes = "验证指定的策略代码是否已被使用")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "检查成功"),
//            @ApiResponse(code = 400, message = "策略代码不能为空"),
//            @ApiResponse(code = 500, message = "服务器内部错误")
//    })
//    public com.okx.trading.util.ApiResponse<Boolean> existsByStrategyCode(
//            @ApiParam("策略代码") @PathVariable String strategyCode) {
//        try {
//            if (StringUtils.isBlank(strategyCode)) {
//                return com.okx.trading.util.ApiResponse.error(503, "策略代码不能为空");
//            }
//
//            boolean exists = realTimeStrategyService.existsByStrategyCode(strategyCode);
//            return com.okx.trading.util.ApiResponse.success(exists);
//        } catch (Exception e) {
//            log.error("检查策略代码是否存在失败: {}", strategyCode, e);
//            return com.okx.trading.util.ApiResponse.error(503, "检查策略代码失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 检查是否存在运行中的策略
//     */
//    @GetMapping("/has-running")
//    @ApiOperation(value = "检查是否存在运行中的策略", notes = "检查指定策略信息代码和交易对是否有正在运行的策略")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "检查成功"),
//            @ApiResponse(code = 400, message = "参数不能为空"),
//            @ApiResponse(code = 500, message = "服务器内部错误")
//    })
//    public com.okx.trading.util.ApiResponse<Boolean> hasRunningStrategy(
//            @ApiParam(value = "策略信息代码", required = true, example = "MA_CROSS_STRATEGY") @RequestParam String strategyInfoCode,
//            @ApiParam(value = "交易对符号", required = true, example = "BTC-USDT") @RequestParam String symbol) {
//        try {
//            if (StringUtils.isBlank(strategyInfoCode)) {
//                return com.okx.trading.util.ApiResponse.error(503, "策略信息代码不能为空");
//            }
//            if (StringUtils.isBlank(symbol)) {
//                return com.okx.trading.util.ApiResponse.error(503, "交易对符号不能为空");
//            }
//
//            boolean hasRunning = realTimeStrategyService.hasRunningStrategy(strategyInfoCode, symbol);
//            return com.okx.trading.util.ApiResponse.success(hasRunning);
//        } catch (Exception e) {
//            log.error("检查运行中策略失败: {} - {}", strategyInfoCode, symbol, e);
//            return com.okx.trading.util.ApiResponse.error(503, "检查运行中策略失败: " + e.getMessage());
//        }
//    }
//
//    /**
//     * 获取需要自动启动的策略
//     */
//    @GetMapping("/auto-start")
//    @ApiOperation(value = "获取需要自动启动的策略", notes = "获取所有标记为自动启动且状态为RUNNING的策略列表")
//    @ApiResponses({
//            @ApiResponse(code = 200, message = "获取成功"),
//            @ApiResponse(code = 500, message = "服务器内部错误")
//    })
//    public com.okx.trading.util.ApiResponse<List<RealTimeStrategyEntity>> getStrategiesToAutoStart() {
//        try {
//            List<RealTimeStrategyEntity> strategies = realTimeStrategyService.getStrategiesToAutoStart();
//            return com.okx.trading.util.ApiResponse.success(strategies);
//        } catch (Exception e) {
//            log.error("获取自动启动策略失败", e);
//            return com.okx.trading.util.ApiResponse.error(503, "获取自动启动策略失败: " + e.getMessage());
//        }
//    }
//}
