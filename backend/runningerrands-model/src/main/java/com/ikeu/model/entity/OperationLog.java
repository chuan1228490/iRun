package com.ikeu.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("operation_log")
public class OperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long adminId;
    private String adminName;
    private String module;
    private String action;
    private String description;
    private String requestMethod;
    private String requestUrl;
    private String requestParams;
    private String ip;
    private LocalDateTime createdAt;
}
