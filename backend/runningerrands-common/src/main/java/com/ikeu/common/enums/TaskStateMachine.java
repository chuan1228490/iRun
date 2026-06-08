package com.ikeu.common.enums;

import com.ikeu.common.constant.StatusConstant;
import com.ikeu.common.exception.ForbiddenException;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 任务状态机：定义合法状态转移，非法转移抛出异常
 */
public enum TaskStateMachine {
    WAITING(StatusConstant.TASK_WAITING, setOf(
            StatusConstant.TASK_ACCEPTED,
            StatusConstant.TASK_DELIVERING,
            StatusConstant.TASK_CANCELLED
    )),
    ACCEPTED(StatusConstant.TASK_ACCEPTED, setOf(
            StatusConstant.TASK_DELIVERING,
            StatusConstant.TASK_WAITING,
            StatusConstant.TASK_CANCELLED
    )),
    DELIVERING(StatusConstant.TASK_DELIVERING, setOf(
            StatusConstant.TASK_WAIT_CONFIRM,
            StatusConstant.TASK_WAITING,
            StatusConstant.TASK_CANCELLED
    )),
    WAIT_CONFIRM(StatusConstant.TASK_WAIT_CONFIRM, setOf(
            StatusConstant.TASK_COMPLETED
    )),
    COMPLETED(StatusConstant.TASK_COMPLETED, Collections.emptySet()),
    CANCELLED(StatusConstant.TASK_CANCELLED, Collections.emptySet());

    private final int currentStatus;
    private final Set<Integer> allowedNext;

    TaskStateMachine(int currentStatus, Set<Integer> allowedNext) {
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
        for (TaskStateMachine state : values()) {
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
