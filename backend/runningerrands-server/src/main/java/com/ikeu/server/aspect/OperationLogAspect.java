package com.ikeu.server.aspect;

import com.ikeu.common.context.BaseContext;
import com.ikeu.model.entity.Admin;
import com.ikeu.model.entity.OperationLog;
import com.ikeu.server.mapper.AdminMapper;
import com.ikeu.server.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 操作日志切面，拦截标注 @OperationLog 的管理端方法，自动记录操作日志。
 * @author ikeu
 * @since 2026/05/31
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;
    private final AdminMapper adminMapper;

    @AfterReturning(pointcut = "@annotation(anno)", returning = "ret")
    public void logOperation(JoinPoint joinPoint, Object ret, com.ikeu.server.annotation.OperationLog anno) {
        try {
            Long adminId = BaseContext.getCurrentId();
            if (adminId == null) return;

            Admin admin = adminMapper.selectById(adminId);
            if (admin == null) return;

            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String[] paramNames = signature.getParameterNames();
            Object[] args = joinPoint.getArgs();

            String description = anno.description();
            if (paramNames != null && description != null && !description.isEmpty()) {
                for (int i = 0; i < paramNames.length; i++) {
                    if (args[i] != null) {
                        description = description.replace("#" + paramNames[i], args[i].toString());
                    }
                }
            }

            OperationLog log = new OperationLog();
            log.setAdminId(adminId);
            log.setAdminName(admin.getName());
            log.setModule(anno.module());
            log.setAction(anno.action());
            log.setDescription(description);

            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
                log.setRequestMethod(request.getMethod());
                log.setRequestUrl(request.getRequestURI());
                log.setIp(getClientIp(request));
            }

            log.setCreatedAt(LocalDateTime.now());
            operationLogService.save(log);
        } catch (Exception e) {
            log.error("操作日志记录失败", e);
        }
    }

    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attrs != null ? attrs.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "";
    }
}
