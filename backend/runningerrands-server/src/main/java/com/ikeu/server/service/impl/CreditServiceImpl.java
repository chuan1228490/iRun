package com.ikeu.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ikeu.common.constant.CreditConstant;
import com.ikeu.model.entity.CreditLog;
import com.ikeu.model.entity.RunnerProfile;
import com.ikeu.model.entity.TaskOrder;
import com.ikeu.server.mapper.CreditLogMapper;
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
 * 信用分服务实现，处理订单完成后的信用分自动变更、通用加减分及冻结/解冻逻辑。
 * @author ikeu
 * @since 2026/05/14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CreditServiceImpl implements CreditService {

    private final TaskOrderMapper taskOrderMapper;
    private final RunnerProfileMapper runnerProfileMapper;
    private final CreditLogMapper creditLogMapper;

    /**
     * 根据订单完成情况自动处理跑腿员信用分，使用 REQUIRES_NEW 事务隔离。
     *
     * <p>评分规则：提前完成 +5，按时完成 +1，超时 0-30min -2，30-60min -5，60min+ -10。
     *
     * @param orderId 订单ID
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processCreditOnComplete(Long orderId) {
        TaskOrder order = taskOrderMapper.selectById(orderId);
        if (order == null || order.getExpectFinishTime() == null) return;

        // 使用实际确认时间而非当前时间，避免 afterCommit 回调延迟导致误判
        LocalDateTime now = order.getConfirmTime() != null ? order.getConfirmTime() : LocalDateTime.now();
        Long runnerId = order.getRunnerId();

        if (now.isBefore(order.getExpectFinishTime())) {
            recordAndApply(runnerId, CreditConstant.REWARD_EARLY,
                    CreditConstant.ReasonType.REWARD, "提前完成", orderId);
            return;
        }

        long minutesLate = Duration.between(order.getExpectFinishTime(), now).toMinutes();
        if (minutesLate <= 0) {
            recordAndApply(runnerId, CreditConstant.REWARD_ON_TIME,
                    CreditConstant.ReasonType.REWARD, "按时完成", orderId);
        } else if (minutesLate <= 30) {
            recordAndApply(runnerId, -2, CreditConstant.ReasonType.TIMEOUT,
                    "配送超时" + minutesLate + "分钟", orderId);
        } else if (minutesLate <= 60) {
            recordAndApply(runnerId, -5, CreditConstant.ReasonType.TIMEOUT,
                    "配送超时" + minutesLate + "分钟", orderId);
        } else {
            recordAndApply(runnerId, -10, CreditConstant.ReasonType.TIMEOUT,
                    "配送超时" + minutesLate + "分钟", orderId);
        }
    }

    /**
     * 通用扣除跑腿员信用分。
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deductCredit(Long runnerId, int penalty, String reason) {
        if (runnerId == null || penalty <= 0) return;
        recordAndApply(runnerId, -penalty, CreditConstant.ReasonType.MANUAL, reason, null);
    }

    /**
     * 通用增加跑腿员信用分。
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void addCredit(Long runnerId, int bonus, String reason) {
        if (runnerId == null || bonus <= 0) return;
        recordAndApply(runnerId, bonus, CreditConstant.ReasonType.REWARD, reason, null);
    }

    /**
     * 核心方法：查询当前信用分 → 写入变动日志 → 原子更新 + 冻结/解冻。
     *
     * <p>冻结规则：新信用分 < 60 时，设 is_banned=1 + ban_until=now+3天；
     * 回到 60 以上时，清除 is_banned 和 ban_until。
     */
    private void recordAndApply(Long runnerId, int delta, String reasonType, String reasonDetail, Long orderId) {
        RunnerProfile profile = runnerProfileMapper.selectOne(
                new LambdaQueryWrapper<RunnerProfile>().eq(RunnerProfile::getUserId, runnerId));
        if (profile == null) {
            log.warn("跑腿员 {} 档案不存在，跳过信用分变更", runnerId);
            return;
        }

        int scoreBefore = profile.getCreditScore() != null ? profile.getCreditScore() : CreditConstant.CREDIT_INITIAL;
        int scoreAfter = Math.max(0, scoreBefore + delta);

        CreditLog creditLog = CreditLog.builder()
                .runnerId(runnerId)
                .delta(delta)
                .scoreBefore(scoreBefore)
                .scoreAfter(scoreAfter)
                .reasonType(reasonType)
                .reasonDetail(reasonDetail)
                .relatedOrderId(orderId)
                .build();
        creditLogMapper.insert(creditLog);

        LocalDateTime banUntil = null;
        if (scoreAfter < CreditConstant.CREDIT_FREEZE_THRESHOLD) {
            boolean alreadyFrozen = profile.getIsBanned() != null && profile.getIsBanned() == 1
                    && profile.getBanUntil() != null && profile.getBanUntil().isAfter(LocalDateTime.now());
            if (alreadyFrozen) {
                // 已在冻结期内，后续扣分不重置 3 天计时
                banUntil = profile.getBanUntil();
            } else {
                banUntil = LocalDateTime.now().plusDays(CreditConstant.CREDIT_FREEZE_DAYS);
                log.warn("跑腿员 {} 信用分降至 {}（<60），冻结接单至 {}", runnerId, scoreAfter, banUntil);
            }
        } else if (scoreAfter >= CreditConstant.CREDIT_FREEZE_THRESHOLD
                && profile.getIsBanned() != null && profile.getIsBanned() == 1
                && profile.getBanUntil() != null) {
            log.info("跑腿员 {} 信用分恢复至 {}（≥60），解除冻结", runnerId, scoreAfter);
        }

        runnerProfileMapper.updateCreditScoreAndFreeze(runnerId, delta,
                CreditConstant.CREDIT_FREEZE_THRESHOLD, banUntil);

        log.info("跑腿员 {} 信用分变更: {} → {} ({}: {})", runnerId, scoreBefore, scoreAfter, reasonType, reasonDetail);
    }
}
