package com.ikeu.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 幂等拦截表实体类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("payment_idempotent")
public class PaymentIdempotent implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 幂等拦截key */
    @TableField("idempotent_key")
    private String idempotentKey;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
