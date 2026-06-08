package com.ikeu.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

@Data
@Schema(description = "设置支付密码请求DTO")
public class SetPayPasswordDTO implements Serializable {

    @Schema(description = "登录密码（用于验证身份，微信用户可传code代替）")
    private String loginPassword;

    @Schema(description = "短信验证码（微信注册用户首次设置时使用，需先绑定手机号）")
    private String code;

    @NotBlank(message = "新支付密码不能为空")
    @Schema(description = "新支付密码")
    private String payPassword;
}
