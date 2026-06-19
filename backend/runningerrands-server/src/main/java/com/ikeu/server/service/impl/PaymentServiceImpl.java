package com.ikeu.server.service.impl;

import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.constant.StatusConstant;
import com.ikeu.common.exception.BusinessException;
import com.ikeu.common.exception.NotFoundException;
import com.ikeu.model.entity.PaymentIdempotent;
import com.ikeu.model.entity.User;
import com.ikeu.server.mapper.PaymentIdempotentMapper;
import com.ikeu.server.mapper.UserMapper;
import com.ikeu.server.service.PaymentService;
import com.ikeu.server.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付服务实现，处理支付密码验证、任务支付退款、报酬结算、充值和提现等资金操作。
 * @author ikeu
 * @since 2026/05/14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final UserMapper userMapper;
    private final TransactionService transactionService;
    private final PaymentIdempotentMapper paymentIdempotentMapper;
    private final PasswordEncoder passwordEncoder;

    private static final String PAY_TASK_KEY_PREFIX = "PAY:task:";
    private static final String REFUND_TASK_KEY_PREFIX = "REFUND:task:";
    private static final String INCOME_TASK_KEY_PREFIX = "INCOME:task:";
    private static final String RECHARGE_USER_KEY_PREFIX = "RECHARGE:user:";
    private static final String WITHDRAW_USER_KEY_PREFIX = "WITHDRAW:user:";

    /**
     * 幂等校验，利用数据库唯一键约束防止重复处理。
     *
     * <p>向 payment_idempotent 表插入幂等键记录，利用 DuplicateKeyException 判断是否重复。
     * 返回 true 表示首次执行，false 表示重复请求已被幂等拦截。
     *
     * @param key 幂等键（由前缀+业务ID组成）
     * @return true 首次执行，false 重复请求
     */
    private boolean checkIdempotent(String key) {
        PaymentIdempotent record = new PaymentIdempotent();
        record.setIdempotentKey(key);
        record.setCreatedAt(LocalDateTime.now());
        try {
            paymentIdempotentMapper.insert(record);
            return true;
        } catch (DuplicateKeyException e) {
            log.warn("幂等拦截: {}", key);
            return false;
        }
    }

    /**
     * 发布任务时扣减用户余额。
     *
     * <p>校验金额 > 0 → 幂等校验（key=PAY:task:{taskId}）→
     * SELECT FOR UPDATE 行锁查询 → 余额充足检查 → 扣减余额并记录支出流水。
     *
     * @param userId 用户ID
     * @param taskId 任务ID
     * @param amount 支付金额
     */
    @Override
    @Transactional
    public void payForTask(Long userId, Long taskId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(MessageConstant.AMOUNT_MUST_GREATER_THAN_ZERO);
        }
        if (!checkIdempotent(PAY_TASK_KEY_PREFIX + taskId)) return;

        User user = userMapper.selectByIdForUpdate(userId);
        if (user == null) throw new NotFoundException(MessageConstant.USER_NOT_EXIST);
        if (user.getBalance().compareTo(amount) < 0) {
            throw new BusinessException(MessageConstant.BALANCE_NOT_ENOUGH);
        }

        BigDecimal before = user.getBalance();
        BigDecimal after = before.subtract(amount);
        user.setBalance(after);
        userMapper.updateById(user);
        transactionService.recordTransaction(userId, taskId, amount.negate(),
                StatusConstant.TRANSACTION_EXPENSE, before, after);
    }

    /**
     * 取消任务时退款给发布者。
     *
     * <p>校验金额 > 0 → 幂等校验（key=REFUND:task:{taskId}）→
     * SELECT FOR UPDATE → 增加余额并记录退款流水。
     *
     * @param userId 用户ID
     * @param taskId 任务ID
     * @param amount 退款金额
     */
    @Override
    @Transactional
    public void refundForTask(Long userId, Long taskId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(MessageConstant.AMOUNT_MUST_GREATER_THAN_ZERO);
        }
        if (!checkIdempotent(REFUND_TASK_KEY_PREFIX + taskId)) return;

        User user = userMapper.selectByIdForUpdate(userId);
        if (user == null) throw new NotFoundException(MessageConstant.USER_NOT_EXIST);

        BigDecimal before = user.getBalance();
        BigDecimal after = before.add(amount);
        user.setBalance(after);
        userMapper.updateById(user);
        transactionService.recordTransaction(userId, taskId, amount,
                StatusConstant.TRANSACTION_REFUND, before, after);
    }

    /**
     * 任务完成时支付报酬给跑腿员，幂等返回是否首次执行。
     *
     * <p>校验金额 > 0 → 幂等校验（key=INCOME:task:{taskId}:runner:{runnerId}）→
     * SELECT FOR UPDATE → 增加余额并记录收入流水 → 返回 true。
     * 幂等拦截时返回 false，调用方据此决定是否更新统计。
     *
     * @param runnerId 跑腿员ID
     * @param taskId 任务ID
     * @param amount 报酬金额
     * @return true 首次支付，false 幂等拦截
     */
    @Override
    @Transactional
    public boolean payToRunner(Long runnerId, Long taskId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(MessageConstant.AMOUNT_MUST_GREATER_THAN_ZERO);
        }
        if (!checkIdempotent(INCOME_TASK_KEY_PREFIX + taskId + ":runner:" + runnerId)) return false;

        User runner = userMapper.selectByIdForUpdate(runnerId);
        if (runner == null) throw new NotFoundException(MessageConstant.USER_NOT_EXIST);

        BigDecimal before = runner.getBalance();
        BigDecimal after = before.add(amount);
        runner.setBalance(after);
        userMapper.updateById(runner);
        transactionService.recordTransaction(runnerId, taskId, amount,
                StatusConstant.TRANSACTION_INCOME, before, after);
        log.info("配送员 {} 完成任务 {}， 收入报酬 {}", runnerId, taskId, amount);
        return true;
    }

    /**
     * 用户充值，使用1秒窗口幂等 + SELECT FOR UPDATE 防止重复提交。
     *
     * @param userId 用户ID
     * @param amount 充值金额
     */
    @Override
    @Transactional
    public void recharge(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(MessageConstant.AMOUNT_MUST_GREATER_THAN_ZERO);
        }
        if (!checkIdempotent(RECHARGE_USER_KEY_PREFIX + userId + ":" + (System.currentTimeMillis() / 1000))) return;

        User user = userMapper.selectByIdForUpdate(userId);
        if (user == null) throw new NotFoundException(MessageConstant.USER_NOT_EXIST);

        BigDecimal before = user.getBalance();
        BigDecimal after = before.add(amount);
        user.setBalance(after);
        userMapper.updateById(user);
        transactionService.recordTransaction(userId, null, amount,
                StatusConstant.TRANSACTION_RECHARGE, before, after);
        log.info("用户 {} 充值 {} 元", userId, amount);
    }

    /**
     * 用户提现，使用1秒窗口幂等 + SELECT FOR UPDATE，校验余额充足。
     *
     * @param userId 用户ID
     * @param amount 提现金额
     */
    @Override
    @Transactional
    public void withdraw(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(MessageConstant.AMOUNT_MUST_GREATER_THAN_ZERO);
        }
        if (!checkIdempotent(WITHDRAW_USER_KEY_PREFIX + userId + ":" + (System.currentTimeMillis() / 1000))) return;

        User user = userMapper.selectByIdForUpdate(userId);
        if (user == null) throw new NotFoundException(MessageConstant.USER_NOT_EXIST);
        if (user.getBalance().compareTo(amount) < 0) {
            throw new BusinessException(MessageConstant.BALANCE_NOT_ENOUGH);
        }

        BigDecimal before = user.getBalance();
        BigDecimal after = before.subtract(amount);
        user.setBalance(after);
        userMapper.updateById(user);
        transactionService.recordTransaction(userId, null, amount.negate(),
                StatusConstant.TRANSACTION_WITHDRAW, before, after);
        log.info("用户 {} 提现 {} 元", userId, amount);
    }

    /**
     * 验证支付密码。
     *
     * <p>校验支付密码非空 → 用户存在 → 已设置支付密码 → BCrypt 密码匹配。
     * 任一条件不满足抛出对应的业务异常。
     *
     * @param userId 用户ID
     * @param rawPassword 原始支付密码（明文）
     */
    @Override
    public void verifyPayPassword(Long userId, String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new BusinessException(MessageConstant.PAY_PASSWORD_CANNOT_BE_NULL);
        }
        User user = userMapper.selectById(userId);
        if (user == null) throw new NotFoundException(MessageConstant.USER_NOT_EXIST);
        if (user.getPayPassword() == null) {
            throw new BusinessException(MessageConstant.PAY_PASSWORD_NOT_SET);
        }
        if (!passwordEncoder.matches(rawPassword, user.getPayPassword())) {
            throw new BusinessException(MessageConstant.PAY_PASSWORD_ERROR);
        }
    }
}
