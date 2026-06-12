package com.ikeu.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 分类占比 VO，用于仪表盘饼图/柱状图的分类数据。
 * @author ikeu
 * @since 2026/06/11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "分类占比")
public class CategoryPieVO implements Serializable {

    @Schema(description = "分类名称")
    private String name;

    @Schema(description = "数量")
    private Long value;
}
