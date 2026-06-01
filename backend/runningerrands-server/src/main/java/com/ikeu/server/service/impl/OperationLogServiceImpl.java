package com.ikeu.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ikeu.common.result.PageResult;
import com.ikeu.model.entity.OperationLog;
import com.ikeu.server.mapper.OperationLogMapper;
import com.ikeu.server.service.OperationLogService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OperationLogServiceImpl extends ServiceImpl<OperationLogMapper, OperationLog> implements OperationLogService {

    @Override
    public PageResult<OperationLog> listLogs(String module, Long adminId, LocalDateTime start, LocalDateTime end, int page, int size) {
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(module != null && !module.isBlank(), OperationLog::getModule, module)
                .eq(adminId != null, OperationLog::getAdminId, adminId)
                .ge(start != null, OperationLog::getCreatedAt, start)
                .le(end != null, OperationLog::getCreatedAt, end)
                .orderByDesc(OperationLog::getCreatedAt);

        Page<OperationLog> p = page(new Page<>(page, size), wrapper);
        return new PageResult<>(p.getTotal(), p.getRecords());
    }
}
