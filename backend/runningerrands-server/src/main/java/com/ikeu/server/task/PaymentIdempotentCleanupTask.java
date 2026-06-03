package com.ikeu.server.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ikeu.model.entity.PaymentIdempotent;
import com.ikeu.server.mapper.PaymentIdempotentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 支付幂等记录清理定时任务，每日凌晨4点删除7天前的过期幂等记录。
 * @author ikeu
 * @since 2026/06/03
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentIdempotentCleanupTask {

    private final PaymentIdempotentMapper paymentIdempotentMapper;

    /**
     * 每日凌晨4点清理7天前的支付幂等记录，避免表数据无限膨胀。
     */
    @Scheduled(cron = "0 0 4 * * ?")
    public void cleanExpiredRecords() {
        LocalDateTime deadline = LocalDateTime.now().minusDays(7);
        int deleted = paymentIdempotentMapper.delete(
                new LambdaQueryWrapper<PaymentIdempotent>()
                        .lt(PaymentIdempotent::getCreatedAt, deadline));
        if (deleted > 0) {
            log.info("清理了 {} 条支付幂等记录", deleted);
        }
    }
}