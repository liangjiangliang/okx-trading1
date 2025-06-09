package com.okx.trading.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 策略更新请求DTO
 */
@Data
@ApiModel(description = "策略更新请求参数")
public class StrategyUpdateRequestDTO {

    @ApiModelProperty(value = "策略ID", required = true, example = "1")
    @NotNull(message = "策略ID不能为空")
    private Long id;

    @ApiModelProperty(value = "策略代码", required = true, example = "AI_GENERATED_001")
    @NotBlank(message = "策略代码不能为空")
    private String strategyCode;

    @ApiModelProperty(value = "策略名称", example = "更新后的AI生成策略")
    private String strategyName;

    @ApiModelProperty(value = "策略描述", required = true, example = "更新后的策略描述")
    @NotBlank(message = "策略描述不能为空")
    private String description;

    @ApiModelProperty(value = "策略分类", example = "AI生成策略")
    private String category;

    @ApiModelProperty(value = "参数说明", example = "无需参数")
    private String paramsDesc;

    @ApiModelProperty(value = "默认参数", example = "")
    private String defaultParams;
}