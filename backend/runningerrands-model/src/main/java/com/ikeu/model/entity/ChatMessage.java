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
 * 聊天消息表实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("chat_message")
public class ChatMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 发送者id */
    @TableField("sender_id")
    private Long senderId;

    /** 接收者id */
    @TableField("receiver_id")
    private Long receiverId;

    /** 内容 */
    @TableField("content")
    private String content;

    /** 消息类型：1-文字 2-图片 */
    @TableField("message_type")
    private Integer messageType;

    /** 是否已读：0-未读 1-已读 */
    @TableField("is_read")
    private Integer isRead;

    /** 是否删除：0-未删除 1-已删除 */
    @TableField("is_deleted")
    private Integer isDeleted;

    /** 是否撤回——0-正常 1-已撤回 */
    @TableField("is_recalled")
    private Integer isRecalled;

    /** 创建时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
