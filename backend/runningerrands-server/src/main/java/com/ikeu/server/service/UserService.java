package com.ikeu.server.service;

import com.ikeu.model.dto.*;
import com.ikeu.model.vo.CertifyStatusVO;
import com.ikeu.model.vo.UserInfoVO;
import com.ikeu.model.vo.UserLoginVO;

/**
 * 用户服务接口，提供注册登录、信息管理、实名认证、支付密码和账户操作等功能。
 * @author ikeu
 * @since 2026/05/14
 */
public interface UserService {

    /** 发送短信验证码，存入 Redis 并设置5分钟有效期。 */
    void sendCode(SendCodeDTO sendCodeDTO);

    /** 用户注册，校验验证码和唯一性后创建用户并生成双令牌。 */
    UserLoginVO register(UserRegisterDTO userRegisterDTO);

    /** 用户登录，支持账号密码和手机验证码两种方式，返回双令牌。 */
    UserLoginVO login(UserLoginDTO loginDTO);

    /** 根据用户ID查询用户基本信息及配送员认证状态。 */
    UserInfoVO getUserInfo(Long userId);

    /** 刷新访问令牌（令牌轮换），删除旧 refresh token 并生成新令牌对。 */
    UserLoginVO refreshAccessToken(String refreshToken);

    /** 退出登录，清除 Redis 中该用户所有 refresh token。 */
    void logout(Long userId);

    /** 注销账户，删除关联数据后物理删除用户。 */
    void deleteAccount(Long userId);

    /** 修改手机号，校验验证码和新手机号唯一性。 */
    void changePhone(Long userId, ChangePhoneDTO changePhoneDTO);

    /** 修改登录密码，校验原密码后加密存储新密码。 */
    void changePassword(Long userId, ChangePasswordDTO changePasswordDTO);

    /** 提交实名认证（学生身份），保存认证信息并将状态设为审核中。 */
    void submitRealNameAuth(Long userId, RealNameAuthDTO realNameAuthDTO);

    /** 修改个人基本信息（昵称、头像、校区、性别、签名等），非空字段才更新。 */
    void updateProfile(Long userId, UpdateProfileDTO updateProfileDTO);

    /** 微信小程序登录，首次自动注册，返回双令牌。 */
    UserLoginVO weChatLogin(WeChatLoginDTO dto);

    /** 首次设置支付密码，需校验登录密码。 */
    void setPayPassword(Long userId, SetPayPasswordDTO dto);

    /** 修改支付密码，需校验原支付密码。 */
    void changePayPassword(Long userId, ChangePayPasswordDTO dto);

    /** 查询用户是否已设置支付密码。 */
    boolean hasPayPassword(Long userId);

    /** 查询用户实名认证状态和配送员认证状态。 */
    CertifyStatusVO getCertifyStatus(Long userId);
}
