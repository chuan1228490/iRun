package com.ikeu.common.enums;

import com.ikeu.common.constant.StatusConstant;
import com.ikeu.common.exception.BusinessException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 订单状态机：定义合法状态转移，非法转移抛出异常
 */
public enum OrderStateMachine {
    WAIT_PICKUP(StatusConstant.ORDER_WAIT_PICKUP, setOf(
            StatusConstant.ORDER_DELIVERING,
            StatusConstant.ORDER_CANCELLED
    )),
    DELIVERING(StatusConstant.ORDER_DELIVERING, setOf(
            StatusConstant.ORDER_WAIT_CONFIRM,
            StatusConstant.ORDER_CANCELLED
    )),
    WAIT_CONFIRM(StatusConstant.ORDER_WAIT_CONFIRM, setOf(
            StatusConstant.ORDER_COMPLETED
    )),
    COMPLETED(StatusConstant.ORDER_COMPLETED, Collections.emptySet()),
    CANCELLED(StatusConstant.ORDER_CANCELLED, Collections.emptySet());

    private final int currentStatus;
    private final Set<Integer> allowedNext;

    OrderStateMachine(int currentStatus, Set<Integer> allowedNext) {
        this.currentStatus = currentStatus;
        this.allowedNext = allowedNext;
    }

    private static Set<Integer> setOf(Integer... values) {
        Set<Integer> set = new HashSet<>();
        for (Integer v : values) set.add(v);
        return Collections.unmodifiableSet(set);
    }

    public static void validate(Integer currentStatus, Integer targetStatus, String entityName) {
        if (currentStatus == null || targetStatus == null) {
            throw new BusinessException("订单状态异常，请刷新后重试");
        }
        for (OrderStateMachine state : values()) {
            if (state.currentStatus == currentStatus) {
                if (state.allowedNext.contains(targetStatus)) {
                    return;
                }
                throw new BusinessException("当前订单状态不允许此操作");
            }
        }
        throw new BusinessException("订单状态异常，请刷新后重试");
    }
}
