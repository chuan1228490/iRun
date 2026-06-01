package com.ikeu.server.service;

import java.math.BigDecimal;

/**
 * 支付服务接口，提供支付密码校验、任务支付退款、报酬结算和充值提现等功能。
 * @author ikeu
 * @date 2026/05/14
 */
public interface PaymentService {

    /** 校验支付密码是否正确。 */
    void verifyPayPassword(Long userId, String rawPassword);

    /** 发布任务时冻结或扣除金额。 */
    void payForTask(Long userId, Long taskId, BigDecimal amount);

    /** 取消任务时退款给发布者，使用幂等记录防止重复退款。 */
    void refundForTask(Long userId, Long taskId, BigDecimal amount);

    /** 任务完成时支付报酬给跑腿员，幂等，返回 true 表示首次支付。 */
    boolean payToRunner(Long runnerId, Long taskId, BigDecimal amount);

    /** 用户账户充值。 */
    void recharge(Long userId, BigDecimal amount);

    /** 用户账户提现。 */
    void withdraw(Long userId, BigDecimal amount);
}
