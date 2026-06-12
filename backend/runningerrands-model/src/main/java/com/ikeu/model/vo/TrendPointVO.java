package com.ikeu.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 趋势数据点 VO，用于仪表盘折线图/柱状图的数据序列。
 * @author ikeu
 * @since 2026/06/11
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "趋势数据点")
public class TrendPointVO implements Serializable {

    @Schema(description = "日期")
    private String date;

    @Schema(description = "数量")
    private Long value;
}
