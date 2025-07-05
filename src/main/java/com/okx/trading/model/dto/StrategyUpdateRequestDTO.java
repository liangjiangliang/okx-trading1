package com.okx.trading.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 策略更新请求DTO
 */
@Data
@Schema(description = "策略更新请求参数")
public class StrategyUpdateRequestDTO {

    @Schema(description = "策略ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "策略ID不能为空")
    private Long id;

    @Schema(description = "策略代码", requiredMode = Schema.RequiredMode.REQUIRED, example = "AI_GENERATED_001")
    @NotBlank(message = "策略代码不能为空")
    private String strategyCode;

    @Schema(description = "策略名称", example = "更新后的AI生成策略")
    private String strategyName;

    @Schema(description = "策略描述", requiredMode = Schema.RequiredMode.REQUIRED, example = "更新后的策略描述")
    @NotBlank(message = "策略描述不能为空")
    private String description;

    @Schema(description = "策略分类", example = "AI生成策略")
    private String category;

    @Schema(description = "参数说明", example = "无需参数")
    private String paramsDesc;

    @Schema(description = "默认参数", example = "")
    private String defaultParams;
}