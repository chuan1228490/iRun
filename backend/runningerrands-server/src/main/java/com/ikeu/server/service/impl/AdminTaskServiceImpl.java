package com.ikeu.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.constant.RedisConstant;
import com.ikeu.common.constant.StatusConstant;
import com.ikeu.common.exception.NotFoundException;
import com.ikeu.common.result.PageResult;
import com.ikeu.model.entity.Task;
import com.ikeu.model.entity.User;
import com.ikeu.model.vo.TaskDetailVO;
import com.ikeu.model.vo.TaskListVO;
import com.ikeu.server.mapper.TaskMapper;
import com.ikeu.server.mapper.UserMapper;
import com.ikeu.server.service.AdminTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端任务管理服务实现，处理任务列表、详情和强制状态更新。
 * @author ikeu
 * @since 2026/06/11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminTaskServiceImpl implements AdminTaskService {

    private final TaskMapper taskMapper;
    private final UserMapper userMapper;

    /**
     * 分页查询所有任务，关联发布者信息填充昵称/头像，支持按状态筛选。
     */
    @Override
    public PageResult<TaskListVO> listAllTasks(Integer status, int page, int size) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(status != null, Task::getStatus, status)
                .orderByDesc(Task::getCreatedAt);

        Page<Task> p = taskMapper.selectPage(new Page<>(page, size), wrapper);
        if (p.getRecords().isEmpty()) {
            return new PageResult<>(0L, Collections.emptyList());
        }

        List<Long> publisherIds = p.getRecords().stream()
                .map(Task::getPublisherId).distinct().collect(Collectors.toList());
        List<User> publishers = userMapper.selectBatchIds(publisherIds);
        Map<Long, User> publisherMap = publishers.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<TaskListVO> records = p.getRecords().stream().map(task -> {
            TaskListVO vo = BeanUtil.copyProperties(task, TaskListVO.class, "imageUrls", "publishTime");
            vo.setTaskId(task.getId());
            vo.setPublishTime(task.getCreatedAt());
            User publisher = publisherMap.get(task.getPublisherId());
            vo.setPublisherNickname(publisher != null ? publisher.getNickname() : "");
            vo.setPublisherUsername(publisher != null ? publisher.getUsername() : "");
            vo.setPublisherAvatar(publisher != null ? publisher.getAvatarUrl() : "");
            vo.setImageUrls(task.getImageUrls() != null
                    ? JSONUtil.toList(task.getImageUrls(), String.class) : Collections.emptyList());
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(p.getTotal(), records);
    }

    /**
     * 查询任务详情（管理端，不脱敏），含发布者信息和图片列表。
     */
    @Override
    public TaskDetailVO getTaskDetail(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new NotFoundException(MessageConstant.TASK_NOT_EXIST);
        User publisher = userMapper.selectById(task.getPublisherId());

        TaskDetailVO vo = BeanUtil.copyProperties(task, TaskDetailVO.class, "imageUrls");
        vo.setTaskId(task.getId());
        vo.setPublishTime(task.getCreatedAt());
        if (StatusConstant.TASK_CANCELLED.equals(task.getStatus())) {
            vo.setCancelTime(task.getUpdatedAt());
        }
        vo.setImageUrls(task.getImageUrls() != null
                ? JSONUtil.toList(task.getImageUrls(), String.class) : Collections.emptyList());
        vo.setHasPickupCode(task.getPickupCode() != null && !task.getPickupCode().isBlank());
        if (publisher != null) {
            vo.setPublisherNickname(publisher.getNickname());
            vo.setPublisherUsername(publisher.getUsername());
            vo.setPublisherAvatar(publisher.getAvatarUrl());
        }
        return vo;
    }

    /**
     * 管理员强制更新任务状态，经状态机校验后写入，清除仪表盘缓存。
     */
    @Override
    @Transactional
    @CacheEvict(value = RedisConstant.CACHE_DASHBOARD, allEntries = true)
    public void updateTaskStatus(Long taskId, Integer status) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) throw new NotFoundException(MessageConstant.TASK_NOT_EXIST);
        com.ikeu.common.enums.TaskStateMachine.validate(task.getStatus(), status, "任务");
        task.setStatus(status);
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        log.info("管理员强制更新任务 {} 状态为 {} → {}", taskId, task.getStatus(), status);
    }
}
