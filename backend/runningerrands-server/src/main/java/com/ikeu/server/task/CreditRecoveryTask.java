package com.ikeu.server.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ikeu.common.constant.CreditConstant;
import com.ikeu.common.constant.RedisConstant;
import com.ikeu.model.entity.CreditLog;
import com.ikeu.model.entity.RunnerProfile;
import com.ikeu.server.mapper.CreditLogMapper;
import com.ikeu.server.mapper.RunnerProfileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 信用分恢复定时任务，每5分钟扫描冻结到期的跑腿员并原子恢复信用分。
 * @author ikeu
 * @since 2026/06/17
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CreditRecoveryTask {

    private final RunnerProfileMapper runnerProfileMapper;
    private final CreditLogMapper creditLogMapper;
    private final RedissonClient redissonClient;

    @Scheduled(cron = "0 */5 * * * ?")
    public void recoverExpiredFreezes() {
        RLock lock = redissonClient.getLock(RedisConstant.CREDIT_RECOVERY_LOCK_KEY);
        try {
            if (!lock.tryLock(0, 60, TimeUnit.SECONDS)) {
                return;
            }

            List<RunnerProfile> frozenRunners = runnerProfileMapper.selectList(
                    new LambdaQueryWrapper<RunnerProfile>()
                            .eq(RunnerProfile::getIsBanned, 1)
                            .isNotNull(RunnerProfile::getBanUntil)
                            .lt(RunnerProfile::getBanUntil, LocalDateTime.now()));

            int targetScore = CreditConstant.CREDIT_FREEZE_THRESHOLD;

            for (RunnerProfile profile : frozenRunners) {
                int oldScore = profile.getCreditScore() != null ? profile.getCreditScore() : CreditConstant.CREDIT_INITIAL;

                // 原子恢复，消除 TOCTOU 竞态：WHERE 二次校验 + CASE WHEN 防覆盖
                int rows = runnerProfileMapper.restoreCreditAndUnfreeze(profile.getUserId(), targetScore);
                if (rows == 0) continue; // 并发恢复或已被手动解冻

                RunnerProfile restored = runnerProfileMapper.selectById(profile.getId());
                int newScore = restored != null && restored.getCreditScore() != null
                        ? restored.getCreditScore() : targetScore;

                CreditLog creditLog = CreditLog.builder()
                        .runnerId(profile.getUserId())
                        .delta(newScore - oldScore)
                        .scoreBefore(oldScore)
                        .scoreAfter(newScore)
                        .reasonType(CreditConstant.ReasonType.RECOVER)
                        .reasonDetail("冻结期满，自动恢复至" + targetScore + "分")
                        .build();
                creditLogMapper.insert(creditLog);

                log.info("跑腿员 {} 冻结期满，信用分恢复: {} → {}", profile.getUserId(), oldScore, newScore);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
