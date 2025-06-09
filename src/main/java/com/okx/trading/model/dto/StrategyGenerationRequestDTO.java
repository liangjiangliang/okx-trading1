package com.okx.trading.model.dto;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;

/**
 * 策略生成请求DTO
 */
@Data
@ApiModel(description = "策略生成请求参数")
public class StrategyGenerationRequestDTO {

    @ApiModelProperty(value = "策略代码", required = true, example = "AI_GENERATED_001")
    @NotBlank(message = "策略代码不能为空")
    private String strategyCode;

    @ApiModelProperty(value = "策略名称", required = true, example = "AI生成的双均线策略")
    @NotBlank(message = "策略名称不能为空")
    private String strategyName;

    @ApiModelProperty(value = "策略描述", required = true, example = "基于5日和20日移动平均线的交叉策略")
    @NotBlank(message = "策略描述不能为空")
    private String description;

    @ApiModelProperty(value = "策略分类", example = "AI生成策略")
    private String category = "AI生成策略";

    @ApiModelProperty(value = "参数说明", example = "无需参数")
    private String paramsDesc = "无需参数";

    @ApiModelProperty(value = "默认参数", example = "")
    private String defaultParams = "";
}