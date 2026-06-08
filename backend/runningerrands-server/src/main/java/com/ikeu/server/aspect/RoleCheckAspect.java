package com.ikeu.server.aspect;

import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.context.BaseContext;
import com.ikeu.common.exception.ForbiddenException;
import com.ikeu.model.entity.Admin;
import com.ikeu.server.annotation.RequireRole;
import com.ikeu.server.mapper.AdminMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 角色权限检查切面，拦截标注 @RequireRole 的方法，校验当前管理员角色。
 * @author ikeu
 * @since 2026/05/31
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RoleCheckAspect {

    private final AdminMapper adminMapper;

    @Before("@annotation(requireRole)")
    public void checkRole(JoinPoint joinPoint, RequireRole requireRole) {
        Long adminId = BaseContext.getCurrentId();
        if (adminId == null) {
            throw new ForbiddenException(MessageConstant.ACCESS_DENIED);
        }

        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new ForbiddenException(MessageConstant.ACCESS_DENIED);
        }

        int currentRole = admin.getRole();
        boolean hasPermission = Arrays.stream(requireRole.value()).anyMatch(r -> r == currentRole);
        if (!hasPermission) {
            log.warn("管理员 {} (角色:{}) 尝试越权访问 {}", admin.getUsername(), currentRole, joinPoint.getSignature());
            throw new ForbiddenException(MessageConstant.ACCESS_DENIED);
        }
    }
}
