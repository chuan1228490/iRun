package com.ikeu.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "通知信息VO")
public class NotificationVO implements Serializable {
    @Schema(description = "通知ID")
    private Long id;

    @Schema(description = "通知类型", example = "1")
    private Integer type;        // 1-系统通知 2-订单状态 3-活动提醒

    @Schema(description = "通知标题")
    private String title;

    @Schema(description = "通知内容")
    private String content;

    @Schema(description = "是否已读", example = "0")
    private Integer isRead;      // 0-未读 1-已读

    @Schema(description = "关联业务ID")
    private Long targetId;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}