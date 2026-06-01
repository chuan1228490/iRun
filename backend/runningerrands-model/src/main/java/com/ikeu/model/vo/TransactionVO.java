package com.ikeu.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "交易资金流水记录VO")
public class TransactionVO {

    @Schema(name = "交易流水记录ID")
    private Long id;

    @Schema(name = "关联的用户ID")
    private Long userId;

    @Schema(name = "用户昵称")
    private String userNickname;

    @Schema(name = "关联的任务ID")
    private Long taskId;

    @Schema(name = "变动金额（正为收入，负为支出）")
    private BigDecimal amount;

    @Schema(name = "类型：1-任务悬赏支出，2-跑腿收入，3-充值，4-提现，5-退款")
    private Integer type;

    @Schema(name = "变动前余额")
    private BigDecimal balanceBefore;

    @Schema(name = "变动后余额")
    private BigDecimal balanceAfter;

    @Schema(name = "发生时间")
    private LocalDateTime createdAt;
}
