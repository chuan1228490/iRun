package com.ikeu.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.constant.StatusConstant;
import com.ikeu.common.context.BaseContext;
import com.ikeu.common.exception.BusinessException;
import com.ikeu.common.exception.NotFoundException;
import com.ikeu.common.result.PageResult;
import com.ikeu.model.dto.AdminCreateDTO;
import com.ikeu.model.dto.AdminPasswordResetDTO;
import com.ikeu.model.dto.AdminUpdateDTO;
import com.ikeu.model.entity.Admin;
import com.ikeu.model.vo.AdminListVO;
import com.ikeu.server.mapper.AdminMapper;
import com.ikeu.server.service.AdminEmployeeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminEmployeeServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminEmployeeService {

    private static final int ROLE_SUPER_ADMIN = 1;
    private static final int ROLE_NORMAL_ADMIN = 2;

    private final PasswordEncoder passwordEncoder;

    @Override
    public PageResult<AdminListVO> listEmployees(int page, int size) {
        Page<Admin> p = page(new Page<>(page, size),
                new LambdaQueryWrapper<Admin>().orderByDesc(Admin::getCreatedAt));
        List<AdminListVO> records = p.getRecords().stream().map(a -> AdminListVO.builder()
                .id(a.getId())
                .username(a.getUsername())
                .name(a.getName())
                .phone(a.getPhone())
                .sex(a.getSex())
                .role(a.getRole())
                .status(a.getStatus())
                .lastLoginTime(a.getLastLoginTime())
                .createdAt(a.getCreatedAt())
                .build()).collect(Collectors.toList());
        return new PageResult<>(p.getTotal(), records);
    }

    @Override
    public AdminListVO getEmployee(Long id) {
        Admin a = getById(id);
        if (a == null) throw new NotFoundException(MessageConstant.ADMIN_NOT_EXIST);
        return AdminListVO.builder()
                .id(a.getId()).username(a.getUsername()).name(a.getName())
                .phone(a.getPhone()).sex(a.getSex()).role(a.getRole())
                .status(a.getStatus()).lastLoginTime(a.getLastLoginTime())
                .createdAt(a.getCreatedAt()).build();
    }

    @Override
    @Transactional
    public void createEmployee(AdminCreateDTO dto) {
        boolean exists = lambdaQuery().eq(Admin::getUsername, dto.getUsername()).exists();
        if (exists) throw new BusinessException(MessageConstant.ADMIN_USERNAME_EXISTS);

        // 只能创建普通管理员，禁止创建超管
        if (dto.getRole() == null || dto.getRole() != ROLE_NORMAL_ADMIN) {
            throw new BusinessException(MessageConstant.ADMIN_MUST_BE_NORMAL);
        }

        Admin admin = new Admin();
        admin.setUsername(dto.getUsername());
        admin.setName(dto.getName());
        admin.setPassword(passwordEncoder.encode(dto.getPassword()));
        admin.setPhone(dto.getPhone());
        admin.setSex(dto.getSex() != null ? dto.getSex() : "");
        admin.setIdNumber(dto.getIdNumber() != null ? dto.getIdNumber() : "");
        admin.setRole(ROLE_NORMAL_ADMIN);
        admin.setStatus(StatusConstant.ENABLE);
        save(admin);
        log.info("管理员 {} 创建了普通管理员 {}", BaseContext.getCurrentId(), dto.getUsername());
    }

    @Override
    @Transactional
    public void updateEmployee(Long id, AdminUpdateDTO dto) {
        Admin admin = getById(id);
        if (admin == null) throw new NotFoundException(MessageConstant.ADMIN_NOT_EXIST);
        // 仅允许修改姓名、手机号、性别，角色保持不变
        admin.setName(dto.getName());
        admin.setPhone(dto.getPhone());
        admin.setSex(dto.getSex() != null ? dto.getSex() : admin.getSex());
        updateById(admin);
    }

    @Override
    @Transactional
    public void toggleStatus(Long id, Boolean enabled) {
        Admin admin = getById(id);
        if (admin == null) throw new NotFoundException(MessageConstant.ADMIN_NOT_EXIST);
        Long currentId = BaseContext.getCurrentId();
        // 不能停用自己的账号
        if (!enabled && currentId != null && currentId.equals(id)) {
            throw new BusinessException(MessageConstant.CANNOT_DISABLE_SELF);
        }
        admin.setStatus(enabled ? StatusConstant.ENABLE : StatusConstant.DISABLE);
        updateById(admin);
    }

    @Override
    @Transactional
    public void resetPassword(Long id, AdminPasswordResetDTO dto) {
        Admin admin = getById(id);
        if (admin == null) throw new NotFoundException(MessageConstant.ADMIN_NOT_EXIST);
        admin.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        updateById(admin);
    }

    @Override
    @Transactional
    public void deleteEmployee(Long id) {
        Long currentId = BaseContext.getCurrentId();
        if (currentId != null && currentId.equals(id)) {
            throw new BusinessException(MessageConstant.CANNOT_DELETE_SELF);
        }
        Admin admin = getById(id);
        if (admin == null) throw new NotFoundException(MessageConstant.ADMIN_NOT_EXIST);
        // 禁止删除超管
        if (admin.getRole() == ROLE_SUPER_ADMIN) {
            throw new BusinessException(MessageConstant.CANNOT_DELETE_SELF);
        }
        removeById(id);
        log.info("管理员 {} 删除了普通管理员 {}", currentId, admin.getUsername());
    }
}
