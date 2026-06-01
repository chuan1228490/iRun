package com.ikeu.server.service;

/**
 * 信用分服务接口，提供订单完成信用分处理及通用加减分操作。
 * @author ikeu
 * @since 2026/05/14
 */
public interface CreditService {

    /** 根据订单完成情况处理跑腿员信用分变更。 */
    void processCreditOnComplete(Long orderId);

    /** 通用扣分（差评、违约等）。 */
    void deductCredit(Long runnerId, int penalty, String reason);

    /** 通用加分（好评等）。 */
    void addCredit(Long runnerId, int reward, String reason);
}
