package com.ikeu.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ikeu.common.result.PageResult;
import com.ikeu.model.dto.AdminCreateDTO;
import com.ikeu.model.dto.AdminPasswordResetDTO;
import com.ikeu.model.dto.AdminUpdateDTO;
import com.ikeu.model.entity.Admin;
import com.ikeu.model.vo.AdminListVO;

public interface AdminEmployeeService extends IService<Admin> {

    PageResult<AdminListVO> listEmployees(int page, int size);

    AdminListVO getEmployee(Long id);

    void createEmployee(AdminCreateDTO dto);

    void updateEmployee(Long id, AdminUpdateDTO dto);

    void toggleStatus(Long id, Boolean enabled);

    void resetPassword(Long id, AdminPasswordResetDTO dto);

    void deleteEmployee(Long id);
}
