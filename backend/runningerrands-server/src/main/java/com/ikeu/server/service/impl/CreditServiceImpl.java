package com.ikeu.server.service.impl;

import com.ikeu.model.entity.TaskOrder;
import com.ikeu.server.mapper.RunnerProfileMapper;
import com.ikeu.server.mapper.TaskOrderMapper;
import com.ikeu.server.service.CreditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 信用分服务实现，处理订单完成后的信用分自动扣减及通用加减分操作。
 * @author ikeu
 * @since 2026/05/14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditServiceImpl implements CreditService {

    private final TaskOrderMapper taskOrderMapper;
    private final RunnerProfileMapper runnerProfileMapper;

    /**
     * 根据订单完成情况自动处理跑腿员信用分，使用 REQUIRES_NEW 事务隔离。
     *
     * <p>校验和扣分逻辑：订单存在且 expectFinishTime 不为 null →
     * 当前时间未超过预计完成时间则跳过 →
     * 计算超时分钟数，分三级扣分：0-30分钟扣2分，30-60分钟扣5分，60分钟以上扣10分。
     * 通过原子 updateCreditScore 直接 SQL 更新，消除 read-before-write 竞态。
     *
     * @param orderId 订单ID
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processCreditOnComplete(Long orderId) {
        TaskOrder order = taskOrderMapper.selectById(orderId);
        if (order == null || order.getExpectFinishTime() == null) return;

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(order.getExpectFinishTime())) return;

        long minutesLate = Duration.between(order.getExpectFinishTime(), now).toMinutes();
        int penalty;
        if (minutesLate <= 30) penalty = 2;
        else if (minutesLate <= 60) penalty = 5;
        else penalty = 10;

        runnerProfileMapper.updateCreditScore(order.getRunnerId(), -penalty);
        log.warn("配送超时 {} 分钟，跑腿员 {} 信用分扣减 {}", minutesLate, order.getRunnerId(), penalty);
    }

    /**
     * 通用扣除跑腿员信用分，校验参数合法性后原子扣减（结果不低于0）。
     *
     * @param runnerId 跑腿员ID
     * @param penalty 扣除分数
     * @param reason 扣除原因（差评、违约等）
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deductCredit(Long runnerId, int penalty, String reason) {
        if (runnerId == null || penalty <= 0) return;
        runnerProfileMapper.updateCreditScore(runnerId, -penalty);
        log.warn("跑腿员 {} 因 {} 信用分扣减 {}", runnerId, reason, penalty);
    }

    /**
     * 通用增加跑腿员信用分，校验参数合法性后原子增加。
     *
     * @param runnerId 跑腿员ID
     * @param bonus 增加分数
     * @param reason 增加原因（好评等）
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void addCredit(Long runnerId, int bonus, String reason) {
        if (runnerId == null || bonus <= 0) return;
        runnerProfileMapper.updateCreditScore(runnerId, bonus);
        log.warn("跑腿员 {} 因 {} 信用分增加 {}", runnerId, reason, bonus);
    }
}
