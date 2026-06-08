package com.ikeu.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminPasswordResetDTO {
    @NotBlank(message = "新密码不能为空")
    private String newPassword;
}
