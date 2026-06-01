package com.ikeu.server.task;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.constant.RedisConstant;
import com.ikeu.common.constant.StatusConstant;
import com.ikeu.model.entity.Task;
import com.ikeu.server.mapper.TaskMapper;
import com.ikeu.server.service.NotificationService;
import com.ikeu.server.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 任务超时取消定时任务，超过过期时间无人接单时自动取消并退款。
 * @author ikeu
 * @since 2026/05/14
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TaskTimeoutChecker {

    private final TaskMapper taskMapper;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final RedissonClient redissonClient;

    /**
     * 每分钟检查超时无人接单的任务，自动取消并退款给发布者。
     *
     * <p>执行流程：
     * <ol>
     *   <li>获取分布式锁 TASK_TIMEOUT_LOCK_KEY，防止多实例重复执行</li>
     *   <li>查询 status=待接单 且 expire_time ≤ now 的 task 列表</li>
     *   <li>逐个将任务状态更新为"已取消"，取消原因设为超时提示</li>
     *   <li>调用 PaymentService 退款给发布者</li>
     *   <li>发送系统通知告知发布者任务已取消并退款</li>
     * </ol>
     * 单个任务处理异常仅记录日志，不影响其他任务的处理。
     */
    @Scheduled(fixedRate = 60000, initialDelay = 30000)
    @Transactional
    public void cancelTimeoutTasks() {
        RLock lock = redissonClient.getLock(RedisConstant.TASK_TIMEOUT_LOCK_KEY);
        try {
            if (!lock.tryLock(0, 30, TimeUnit.SECONDS)) {
                return;
            }
            LocalDateTime now = LocalDateTime.now();
            List<Task> timeoutTasks = taskMapper.selectList(
                    new LambdaQueryWrapper<Task>()
                            .eq(Task::getStatus, StatusConstant.TASK_WAITING)
                            .le(Task::getExpireTime, now)
            );

            for (Task task : timeoutTasks) {
                try {
                    task.setStatus(StatusConstant.TASK_CANCELLED);
                    task.setCancelReason(MessageConstant.TASK_TIMEOUT_CANCEL);
                    task.setUpdatedAt(now);
                    taskMapper.updateById(task);

                    paymentService.refundForTask(task.getPublisherId(), task.getId(), task.getReward());

                    notificationService.sendNotification(
                            task.getPublisherId(),
                            StatusConstant.NOTICE_SYSTEM,
                            "任务超时取消",
                            "您的任务 " + task.getTaskNo() + " 已超过截止时间无人接单，已自动取消并退款",
                            task.getId()
                    );
                    log.info("任务 {} 超时无人接单（expire_time={}），已自动取消并退款", task.getId(), task.getExpireTime());
                } catch (Exception e) {
                    log.error("处理超时任务 {} 失败", task.getId(), e);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
