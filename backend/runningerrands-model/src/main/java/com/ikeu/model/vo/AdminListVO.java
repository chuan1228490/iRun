package com.ikeu.model.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminListVO {
    private Long id;
    private String username;
    private String name;
    private String phone;
    private String sex;
    private Integer role;
    private Integer status;
    private LocalDateTime lastLoginTime;
    private LocalDateTime createdAt;
}
