package com.ikeu.server.service;

import com.ikeu.common.result.PageResult;
import com.ikeu.model.vo.UserInfoVO;

/**
 * 管理端用户管理服务接口，提供用户列表、详情、状态切换和认证审核功能。
 * @author ikeu
 * @since 2026/06/11
 */
public interface AdminUserService {

    /** 分页查询用户管理列表，支持按状态、认证状态和关键词筛选。 */
    PageResult<UserInfoVO> listUsers(Integer status, Integer isCertify, String keyword, int page, int size);

    /** 封禁或解封指定用户账户。 */
    void toggleUserStatus(Long userId, Boolean enabled);

    /** 获取单个用户详细信息。 */
    UserInfoVO getUserDetail(Long userId);

    /** 审核用户实名认证（学生身份）申请。 */
    void reviewUserCertification(Long userId, Integer isCertify, String remark);
}
