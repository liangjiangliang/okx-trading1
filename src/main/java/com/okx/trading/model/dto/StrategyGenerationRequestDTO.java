package com.okx.trading.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 策略生成请求DTO
 */
@Data
@Schema(description = "策略生成请求参数")
public class StrategyGenerationRequestDTO {

    @Schema(description = "策略代码", requiredMode = Schema.RequiredMode.REQUIRED, example = "AI_GENERATED_001")
    @NotBlank(message = "策略代码不能为空")
    private String strategyCode;

    @Schema(description = "策略名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "AI生成的双均线策略")
    @NotBlank(message = "策略名称不能为空")
    private String strategyName;

    @Schema(description = "策略描述", requiredMode = Schema.RequiredMode.REQUIRED, example = "基于5日和20日移动平均线的交叉策略")
    @NotBlank(message = "策略描述不能为空")
    private String description;

    @Schema(description = "策略分类", example = "AI生成策略")
    private String category = "AI生成策略";

    @Schema(description = "参数说明", example = "无需参数")
    private String paramsDesc = "无需参数";

    @Schema(description = "默认参数", example = "")
    private String defaultParams = "";
}