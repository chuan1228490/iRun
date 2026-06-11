package com.ikeu.server.service;

import com.ikeu.model.dto.AdminLoginDTO;
import com.ikeu.model.vo.AdminLoginVO;

/**
 * 管理端认证服务接口，提供登录、令牌刷新、信息查询和退出功能。
 * @author ikeu
 * @since 2026/06/11
 */
public interface AdminAuthService {

    /** 管理员登录，校验用户名密码后生成双令牌。 */
    AdminLoginVO login(AdminLoginDTO dto);

    /** 刷新管理员访问令牌（令牌轮换）。 */
    AdminLoginVO refreshAccessToken(String refreshToken);

    /** 获取当前登录管理员信息（不含令牌）。 */
    AdminLoginVO getAdminInfo();

    /** 管理员退出登录，清除所有 refresh token。 */
    void logout(Long adminId);
}
