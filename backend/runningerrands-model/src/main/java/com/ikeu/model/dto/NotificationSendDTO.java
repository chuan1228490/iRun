package com.ikeu.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class NotificationSendDTO {
    @NotEmpty(message = "用户ID列表不能为空")
    private List<Long> userIds;
    @NotNull(message = "通知类型不能为空")
    private Integer type;
    @NotBlank(message = "标题不能为空")
    private String title;
    @NotBlank(message = "内容不能为空")
    private String content;
}
