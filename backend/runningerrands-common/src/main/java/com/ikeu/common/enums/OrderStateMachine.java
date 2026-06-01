package com.ikeu.common.enums;

import com.ikeu.common.constant.StatusConstant;
import com.ikeu.common.exception.ForbiddenException;

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
            throw new ForbiddenException("状态不能为空");
        }
        for (OrderStateMachine state : values()) {
            if (state.currentStatus == currentStatus) {
                if (state.allowedNext.contains(targetStatus)) {
                    return;
                }
                throw new ForbiddenException(
                        entityName + " 不允许从状态 " + currentStatus + " 变更为 " + targetStatus);
            }
        }
        throw new ForbiddenException(entityName + " 未知状态: " + currentStatus);
    }
}
