package com.ikeu.server.service;

import com.ikeu.common.result.PageResult;
import com.ikeu.model.entity.TransactionRecord;
import com.ikeu.model.vo.TransactionVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资金流水服务接口，提供流水记录、用户查询和管理员查询等功能。
 * @author ikeu
 * @since 2026/05/22
 */
public interface TransactionService {

    /** 记录一笔资金流水（收支明细）。 */
    void recordTransaction(Long userId, Long taskId, BigDecimal amount, Integer type,
                           BigDecimal balanceBefore, BigDecimal balanceAfter);

    /** 用户分页查询自己的资金流水，支持按类型和时间范围筛选。 */
    PageResult<TransactionVO> listUserTransactions(Long userId, Integer type, LocalDateTime start, LocalDateTime end,
                                                   int page, int size);

    /** 管理员分页查询所有用户的资金流水，支持按类型、用户和时间范围筛选。 */
    PageResult<TransactionVO> listAllTransactions(Integer type, Long userId, LocalDateTime start, LocalDateTime end, int page, int size);
}
