package com.ikeu.server.controller.user;

import com.ikeu.common.context.BaseContext;
import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.result.Result;
import com.ikeu.model.dto.*;
import com.ikeu.model.vo.CertifyStatusVO;
import com.ikeu.model.vo.UserInfoVO;
import com.ikeu.model.vo.UserLoginVO;
import com.ikeu.server.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 用户端核心接口，提供注册、登录、个人信息管理、密码管理、实名认证和账户注销等功能。
 * @author ikeu
 * @since 2025/05/21
 */
@Tag(name = "用户端-用户相关接口", description = "用户注册、登录、个人信息相关接口")
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 发送短信验证码到用户手机。
     *
     * <p>委托 {@link UserService#sendCode} 生成6位随机数字验证码，
     * 调用短信服务发送到目标手机号，并将验证码存入 Redis 设置5分钟有效期。
     *
     * @param sendCodeDTO 发送验证码DTO（手机号）
     * @return 操作结果
     */
    @Operation(summary = "发送短信验证码")
    @PostMapping("/send")
    public Result<Void> sendCode(@Valid @RequestBody SendCodeDTO sendCodeDTO) {
        userService.sendCode(sendCodeDTO);
        return Result.success(MessageConstant.CODE_SEND_SUCCESS);
    }

    /**
     * 用户注册。
     *
     * <p>委托 {@link UserService#register} 校验验证码、用户名和密码非空、
     * 用户名和手机号唯一后，创建用户记录（加密存储密码），
     * 生成双令牌（access + refresh），refresh token 存入 Redis 后返回 UserLoginVO。
     *
     * @param userRegisterDTO 用户注册DTO（用户名、密码、手机号、验证码、性别）
     * @return 注册成功后的登录信息（双令牌）
     */
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<UserLoginVO> register(@Valid @RequestBody UserRegisterDTO userRegisterDTO) {
        UserLoginVO userLoginVO = userService.register(userRegisterDTO);
        return Result.success(userLoginVO);
    }

    /**
     * 用户通过账号密码登录。
     *
     * <p>委托 {@link UserService#login} 支持用户名/手机号+密码（loginType=1）
     * 或手机号+验证码（loginType=2）两种登录方式。
     * 校验账号状态正常后更新最后登录时间，生成双令牌返回。
     *
     * @param loginDTO 用户登录DTO（loginType、用户名/手机号、密码/验证码）
     * @return 登录结果（双令牌、用户信息、过期时间）
     */
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    public Result<UserLoginVO> login(@Valid @RequestBody UserLoginDTO loginDTO) {
        UserLoginVO userLoginVO = userService.login(loginDTO);
        return Result.success(userLoginVO);
    }

    /**
     * 微信小程序登录（首次登录自动注册）。
     *
     * <p>委托 {@link UserService#weChatLogin} 调用微信 code2Session 接口换取 openid，
     * 根据 openid 查找已有用户：存在则更新最后登录时间，
     * 不存在则自动创建新用户（随机昵称、默认密码、默认头像），
     * 最终生成双令牌返回 UserLoginVO。
     *
     * @param weChatLoginDTO 微信登录DTO（wx.login 返回的 code）
     * @return 登录结果（双令牌、用户信息）
     */
    @Operation(summary = "微信小程序登录（自动注册）")
    @PostMapping("/wechat/login")
    public Result<UserLoginVO> login(@Valid @RequestBody WeChatLoginDTO weChatLoginDTO) {
        return Result.success(userService.weChatLogin(weChatLoginDTO));
    }

    /**
     * 获取当前登录用户的个人信息。
     *
     * <p>委托 {@link UserService#getUserInfo} 根据 ThreadLocal 中的 userId
     * 查询用户基本信息，同时查询配送员认证状态，合并返回 UserInfoVO。
     *
     * @return 用户个人信息（含认证状态）
     */
    @Operation(summary = "获取当前登录用户信息")
    @GetMapping("/info")
    public Result<UserInfoVO> getUserInfo() {
        Long userId = BaseContext.getCurrentId();
        UserInfoVO userInfoVO = userService.getUserInfo(userId);
        return Result.success(userInfoVO);
    }

    /**
     * 使用 refresh token 刷新访问令牌。
     *
     * <p>委托 {@link UserService#refreshAccessToken} 解析 refresh token 校验
     * type=refresh、userId 和 jti，在 Redis 中反查确认存在后删除旧 token（令牌轮换），
     * 生成新的 access + refresh token 对，新 refresh token 存入 Redis。
     *
     * @param refreshToken 刷新令牌（从 X-Refresh-Token 请求头获取）
     * @return 新的双令牌对和过期时间
     */
    @Operation(summary = "刷新访问令牌")
    @PostMapping("/refresh")
    public Result<UserLoginVO> refresh(@RequestHeader("X-Refresh-Token") String refreshToken) {
        return Result.success(userService.refreshAccessToken(refreshToken));
    }

    /**
     * 当前用户退出登录。
     *
     * <p>委托 {@link UserService#logout} 通过模式匹配
     * {@code user:refresh:token:{userId}:*} 删除该用户在 Redis 中所有 refresh token，
     * 使所有终端的登录态同时失效。
     *
     * @return 操作结果
     */
    @Operation(summary = "退出登录")
    @PostMapping("/logout")
    public Result<Void> logout() {
        Long userId = BaseContext.getCurrentId();
        userService.logout(userId);
        return Result.success(MessageConstant.LOGOUT_SUCCESS);
    }

    /**
     * 修改当前用户的绑定手机号。
     *
     * <p>委托 {@link UserService#changePhone} 校验验证码正确和新手机号未被其他用户绑定后，
     * 更新用户手机号字段。
     *
     * @param changePhoneDTO 修改手机号DTO（新手机号、验证码）
     * @return 操作结果
     */
    @Operation(summary = "修改手机号")
    @PutMapping("/phone")
    public Result<Void> changePhone(@Valid @RequestBody ChangePhoneDTO changePhoneDTO) {
        Long userId = BaseContext.getCurrentId();
        userService.changePhone(userId, changePhoneDTO);
        return Result.success(MessageConstant.PHONE_CHANGE_SUCCESS);
    }

    /**
     * 修改当前用户的登录密码。
     *
     * <p>委托 {@link UserService#changePassword} 校验原密码正确后，
     * 将密码加密存储为新密码。
     *
     * @param changePasswordDTO 修改密码DTO（原密码、新密码）
     * @return 操作结果
     */
    @Operation(summary = "修改密码")
    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO changePasswordDTO) {
        Long userId = BaseContext.getCurrentId();
        userService.changePassword(userId, changePasswordDTO);
        return Result.success(MessageConstant.PASSWORD_CHANGE_SUCCESS);
    }

    /**
     * 查询当前用户是否已设置支付密码。
     *
     * <p>委托 {@link UserService#hasPayPassword} 检查用户记录的 pay_password 字段是否非空。
     *
     * @return 是否已设置支付密码
     */
    @Operation(summary = "查询用户是否已设置支付密码")
    @GetMapping("/pay-password/status")
    public Result<Boolean> hasPayPassword() {
        Long userId = BaseContext.getCurrentId();
        return Result.success(userService.hasPayPassword(userId));
    }

    /**
     * 首次设置支付密码。
     *
     * <p>委托 {@link UserService#setPayPassword} 校验用户存在、支付密码未设置、
     * 登录密码正确后，将支付密码加密存储。
     *
     * @param dto 设置支付密码DTO（支付密码、登录密码）
     * @return 操作结果
     */
    @Operation(summary = "设置支付密码")
    @PutMapping("/pay-password")
    public Result<Void> setPayPassword(@Valid @RequestBody SetPayPasswordDTO dto) {
        Long userId = BaseContext.getCurrentId();
        userService.setPayPassword(userId, dto);
        return Result.success(MessageConstant.PAY_PASSWORD_SET_SUCCESS);
    }

    /**
     * 修改已设置的支付密码。
     *
     * <p>委托 {@link UserService#changePayPassword} 校验用户存在、支付密码已设置、
     * 原支付密码正确后，将新支付密码加密存储。
     *
     * @param dto 修改支付密码DTO（原支付密码、新支付密码）
     * @return 操作结果
     */
    @Operation(summary = "修改支付密码")
    @PutMapping("/pay-password/change")
    public Result<Void> changePayPassword(@Valid @RequestBody ChangePayPasswordDTO dto) {
        Long userId = BaseContext.getCurrentId();
        userService.changePayPassword(userId, dto);
        return Result.success(MessageConstant.PAY_PASSWORD_CHANGE_SUCCESS);
    }

    /**
     * 提交实名认证（学生身份认证）申请。
     *
     * <p>委托 {@link UserService#submitRealNameAuth} 校验未认证通过且未在审核中后，
     * 将真实姓名、学号、学生证照片URL等信息保存到 user 表，认证状态更新为"审核中"。
     *
     * @param realNameAuthDTO 实名认证DTO（真实姓名、学号、学生证照片URL）
     * @return 操作结果
     */
    @Operation(summary = "提交实名认证")
    @PostMapping("/certify")
    public Result<Void> submitRealNameAuth(@Valid @RequestBody RealNameAuthDTO realNameAuthDTO) {
        Long userId = BaseContext.getCurrentId();
        userService.submitRealNameAuth(userId, realNameAuthDTO);
        return Result.success(MessageConstant.CERTIFY_SUBMITTED);
    }

    /**
     * 查询当前用户的实名认证状态。
     *
     * <p>委托 {@link UserService#getCertifyStatus} 返回用户实名认证状态和配送员认证状态，
     * 包含真实姓名、学号、认证照片URL、审核状态和审核备注。
     *
     * @return 认证状态信息
     */
    @Operation(summary = "查询用户实名认证状态")
    @GetMapping("/certify/status")
    public Result<CertifyStatusVO> getCertifyStatus() {
        Long userId = BaseContext.getCurrentId();
        return Result.success(userService.getCertifyStatus(userId));
    }

    /**
     * 修改当前用户的个人基本信息。
     *
     * <p>委托 {@link UserService#updateProfile} 支持修改昵称、头像URL、
     * 校区、性别、个性签名等字段，非空字段才更新。修改后返回最新的用户信息。
     *
     * @param updateProfileDTO 修改个人信息DTO
     * @return 更新后的用户完整信息
     */
    @Operation(summary = "修改个人基本信息")
    @PutMapping("/profile")
    public Result<UserInfoVO> updateProfile(@Valid @RequestBody UpdateProfileDTO updateProfileDTO) {
        Long userId = BaseContext.getCurrentId();
        userService.updateProfile(userId, updateProfileDTO);
        UserInfoVO updatedUserInfo = userService.getUserInfo(userId);
        return Result.success(updatedUserInfo);
    }

    /**
     * 注销当前用户账户。
     *
     * <p>委托 {@link UserService#deleteAccount} 删除用户关联的地址簿、配送员档案、
     * 评价记录、资金流水记录后，删除用户本身（物理删除）。
     *
     * @return 操作结果
     */
    @Operation(summary = "注销账户")
    @DeleteMapping("/account")
    public Result<Void> deleteAccount() {
        Long userId = BaseContext.getCurrentId();
        userService.deleteAccount(userId);
        return Result.success(MessageConstant.ACCOUNT_DELETED);
    }
}
