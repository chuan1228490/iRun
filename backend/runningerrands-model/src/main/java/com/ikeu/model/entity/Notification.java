package com.ikeu.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通知消息表实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("notification")
public class Notification implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    @TableField("user_id")
    private Long userId;

    /** 类型：1-系统通知，2-订单状态，3-活动提醒 */
    @TableField("type")
    private Integer type;

    /** 标题 */
    @TableField("title")
    private String title;

    /** 内容 */
    @TableField("content")
    private String content;

    /** 是否已读：0-未读，1-已读 */
    @TableField("is_read")
    private Integer isRead;

    /** 关联的业务ID，如task_id */
    @TableField("target_id")
    private Long targetId;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}