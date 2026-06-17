package com.ikeu.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("credit_log")
public class CreditLog implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("runner_id")
    private Long runnerId;

    @TableField("delta")
    private Integer delta;

    @TableField("score_before")
    private Integer scoreBefore;

    @TableField("score_after")
    private Integer scoreAfter;

    @TableField("reason_type")
    private String reasonType;

    @TableField("reason_detail")
    private String reasonDetail;

    @TableField("related_order_id")
    private Long relatedOrderId;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
