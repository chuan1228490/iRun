package com.ikeu.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "设置/修改支付密码请求DTO")
public class SetPayPasswordDTO implements Serializable {

    @NotBlank(message = "登录密码不能为空")
    @Schema(description = "登录密码（用于验证身份）")
    private String loginPassword;

    @NotBlank(message = "新支付密码不能为空")
    @Schema(description = "新支付密码")
    private String payPassword;
}
