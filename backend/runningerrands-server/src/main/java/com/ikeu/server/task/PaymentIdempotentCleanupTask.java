package com.ikeu.server.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ikeu.model.entity.PaymentIdempotent;
import com.ikeu.server.mapper.PaymentIdempotentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentIdempotentCleanupTask {

    private final PaymentIdempotentMapper paymentIdempotentMapper;

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
