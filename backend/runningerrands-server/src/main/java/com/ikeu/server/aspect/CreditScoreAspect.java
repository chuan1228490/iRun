package com.ikeu.server.aspect;

import com.ikeu.server.service.CreditService;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * 信用分切面，在订单确认完成事务提交后自动处理信用分变更。
 * @author ikeu
 * @since 2026/05/14
 */
@Aspect
@Component
@RequiredArgsConstructor
public class CreditScoreAspect {

    private final CreditService creditService;

    /**
     * 后置通知，在订单确认完成后注册事务同步回调处理信用分。
     *
     * <p>实现逻辑：不直接在切面中处理信用分，而是通过
     * {@link TransactionSynchronizationManager#registerSynchronization} 注册 afterCommit 回调。
     * 等外层事务提交释放 runner_profile 行锁后，再由 {@link CreditService#processCreditOnComplete}
     *（REQUIRES_NEW 事务）在独立事务中处理信用分变更，避免两个事务并发更新同一行导致 MySQL 行锁超时。
     *
     * @param publisherId 发布者ID（切点参数，未直接使用，由回调中的 creditService 自行查询）
     * @param orderId 订单ID
     */
    @AfterReturning("execution(* com.ikeu.server.service.TaskOrderService.confirmComplete(..)) && args(publisherId, orderId)")
    public void afterConfirmComplete(Long publisherId, Long orderId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                creditService.processCreditOnComplete(orderId);
            }
        });
    }

}
