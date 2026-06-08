package com.ikeu.server.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ikeu.common.result.PageResult;
import com.ikeu.model.entity.OperationLog;

import java.time.LocalDateTime;

/**
 * 操作日志服务接口，提供操作日志的分页查询功能。
 * @author ikeu
 * @since 2026/06/03
 */
public interface OperationLogService extends IService<OperationLog> {

    /** 分页查询操作日志，支持按模块、管理员ID、时间范围筛选 */
    PageResult<OperationLog> listLogs(String module, Long adminId, LocalDateTime start, LocalDateTime end, int page, int size);
}
