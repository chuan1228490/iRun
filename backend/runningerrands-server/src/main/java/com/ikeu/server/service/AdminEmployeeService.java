package com.ikeu.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ikeu.common.result.PageResult;
import com.ikeu.model.dto.AdminCreateDTO;
import com.ikeu.model.dto.AdminPasswordResetDTO;
import com.ikeu.model.dto.AdminUpdateDTO;
import com.ikeu.model.entity.Admin;
import com.ikeu.model.vo.AdminListVO;

/**
 * 管理员员工管理服务接口，提供管理员员工的 CRUD 和状态管理功能。
 * @author ikeu
 * @since 2026/06/03
 */
public interface AdminEmployeeService extends IService<Admin> {

    /** 分页查询管理员员工列表，按创建时间倒序排列。 */
    PageResult<AdminListVO> listEmployees(int page, int size);

    /** 根据ID查询管理员员工详情。 */
    AdminListVO getEmployee(Long id);

    /** 创建普通管理员，强制 role=2。 */
    void createEmployee(AdminCreateDTO dto);

    /** 修改管理员员工信息（姓名、手机号、性别）。 */
    void updateEmployee(Long id, AdminUpdateDTO dto);

    /** 启用/停用管理员员工，不能停用自己的账号。 */
    void toggleStatus(Long id, Boolean enabled);

    /** 重置管理员员工密码。 */
    void resetPassword(Long id, AdminPasswordResetDTO dto);

    /** 删除管理员员工，禁止删除超级管理员。 */
    void deleteEmployee(Long id);
}
