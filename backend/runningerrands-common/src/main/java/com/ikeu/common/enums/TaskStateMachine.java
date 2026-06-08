package com.ikeu.common.enums;

import com.ikeu.common.constant.StatusConstant;
import com.ikeu.common.exception.BusinessException;

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
            throw new BusinessException("任务状态异常，请刷新后重试");
        }
        for (TaskStateMachine state : values()) {
            if (state.currentStatus == currentStatus) {
                if (state.allowedNext.contains(targetStatus)) {
                    return;
                }
                throw new BusinessException("当前任务状态不允许此操作");
            }
        }
        throw new BusinessException("任务状态异常，请刷新后重试");
    }
}
