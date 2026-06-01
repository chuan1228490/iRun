package com.ikeu.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.constant.RedisConstant;
import com.ikeu.common.constant.StatusConstant;
import com.ikeu.common.constant.TaskTypeConstant;
import com.ikeu.common.enums.TaskStateMachine;
import com.ikeu.common.exception.BusinessException;
import com.ikeu.common.exception.ParamErrorException;
import com.ikeu.common.result.PageResult;
import com.ikeu.model.dto.CancelTaskDTO;
import com.ikeu.model.dto.TaskPublishDTO;

import com.ikeu.model.entity.*;
import com.ikeu.model.vo.TaskDetailVO;
import com.ikeu.model.vo.TaskListVO;
import com.ikeu.model.vo.TaskStatisticsVO;
import com.ikeu.server.mapper.*;
import com.ikeu.server.service.PaymentService;
import com.ikeu.server.service.TaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.cache.Cache;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.cache.CacheManager;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;


/**
 * 任务服务实现，处理任务的发布、查询、修改、取消及搜索筛选等功能。
 * @author ikeu
 * @since 2026/05/14
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskServiceImpl extends ServiceImpl<TaskMapper, Task> implements TaskService {

    private final TaskMapper taskMapper;
    private final UserMapper userMapper;
    private final UserAddressMapper userAddressMapper;
    private final TaskImageMapper taskImageMapper;
    private final ReviewMapper reviewMapper;
    private final PaymentService paymentService;
    private final RedissonClient redissonClient;
    private final CacheManager cacheManager;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 根据任务小类列表返回默认送达地址，若所有小类均须由用户选择则返回null
     *
     * @param subType 任务小类列表
     * @return String 默认送达地址，若无可选项则返回null
     */
    private String getDefaultDeliveryAddress(String subType) {
        if (subType == null || subType.isEmpty()) return null;
        return TaskTypeConstant.DEFAULT_DELIVERY_MAP.get(subType);
    }

    /**
     * 将任务实体转换为任务列表VO
     *
     * @param task 任务实体
     * @param publisherMap 发布者Map映射
     * @return TaskListVO 任务列表VO
     */
    private TaskListVO convertToTaskListVO(Task task, Map<Long, User> publisherMap) {
        TaskListVO taskListVO = BeanUtil.copyProperties(task, TaskListVO.class,
                "imageUrls", "publishTime");
        taskListVO.setTaskId(task.getId());
        taskListVO.setPublishTime(task.getCreatedAt());

        User publisher = publisherMap.get(task.getPublisherId());
        taskListVO.setPublisherNickname(publisher != null ? publisher.getNickname() : "");
        taskListVO.setPublisherAvatar(publisher != null ? publisher.getAvatarUrl() : "");

        taskListVO.setImageUrls(task.getImageUrls() != null
                ? JSONUtil.toList(task.getImageUrls(), String.class)
                : Collections.emptyList());

        return taskListVO;
    }

    /**
     * 执行分页查询并构建TaskListVO结果
     *
     * @param page 页码
     * @param size 每页条数
     * @param wrapper 查询条件包装器
     * @return PageResult<TaskListVO> 分页任务列表结果
     */
    private PageResult<TaskListVO> buildTaskListResult(int page, int size, LambdaQueryWrapper<Task> wrapper) {
        Page<Task> p = page(new Page<>(page, size), wrapper);
        if (p.getRecords().isEmpty()) {
            return new PageResult<>(0L, Collections.emptyList());
        }

        List<Long> publisherIds = p.getRecords().stream()
                .map(Task::getPublisherId).distinct().collect(Collectors.toList());
        List<User> publishers = userMapper.selectBatchIds(publisherIds);
        Map<Long, User> publisherMap = publishers.stream().collect(Collectors.toMap(User::getId, u -> u));
        List<TaskListVO> records = p.getRecords().stream()
                .map(task -> convertToTaskListVO(task, publisherMap))
                .collect(Collectors.toList());
        return new PageResult<>(p.getTotal(), records);
    }

    /**
     * 构建任务详情 VO。
     */
    private TaskDetailVO buildTaskDetailVO(Task task, Long currentUserId) {
        User publisher = userMapper.selectById(task.getPublisherId());
        boolean isOwner = currentUserId != null && currentUserId.equals(task.getPublisherId());

        TaskDetailVO vo = BeanUtil.copyProperties(task, TaskDetailVO.class, "imageUrls", "publishTime");
        vo.setTaskId(task.getId());
        vo.setPublishTime(task.getCreatedAt());
        vo.setIsOwner(isOwner);
        vo.setCancelReason(task.getCancelReason());
        if (task.getStatus() != null && task.getStatus().equals(StatusConstant.TASK_CANCELLED)) {
            vo.setCancelTime(task.getUpdatedAt());
        }
        vo.setPublisherNickname(publisher != null ? publisher.getNickname() : "");
        vo.setPublisherAvatar(publisher != null ? publisher.getAvatarUrl() : "");
        vo.setImageUrls(task.getImageUrls() != null
                ? JSONUtil.toList(task.getImageUrls(), String.class)
                : Collections.emptyList());
        vo.setHasPickupCode(task.getPickupCode() != null && !task.getPickupCode().isEmpty());
        if (!isOwner) {
            vo.setPrivateNote("");
        }
        return vo;
    }

    /**
     * 对非发布者脱敏：取件码隐藏、联系人姓名/电话脱敏。
     */
    private void applyTaskDetailMasking(TaskDetailVO vo) {
        if (vo.getIsOwner() != null && vo.getIsOwner()) return;
        vo.setIsOwner(false);
        vo.setPickupCode(null);

        String contactName = vo.getContactName();
        if (contactName != null && !contactName.isEmpty()) {
            vo.setContactName(contactName.length() == 1
                    ? contactName + "*"
                    : contactName.charAt(0) + "*".repeat(contactName.length() - 1));
        }

        String contactPhone = vo.getContactPhone();
        if (contactPhone != null && contactPhone.length() > 4) {
            vo.setContactPhone("*".repeat(contactPhone.length() - 4)
                    + contactPhone.substring(contactPhone.length() - 4));
        }
    }

    /**
     * 用户发布任务需求方法
     *  校验：支付密码正确，用户状态正常，余额充足
     *  逻辑：处理取件地址和送达地址（支持地址簿选择和默认地址），
     *  生成任务编号，设置过期时间，保存任务并支付报酬
     *
     * @param userId 用户ID
     * @param taskPublishDTO 任务发布DTO
     */
    @Override
    @Transactional
    public void publishTask(Long userId, TaskPublishDTO taskPublishDTO) {
        // 校验支付密码
        paymentService.verifyPayPassword(userId, taskPublishDTO.getPayPassword());
        // 校验用户状态
        User user = userMapper.selectById(userId);
        if (user == null || Objects.equals(user.getStatus(), StatusConstant.DISABLE)) {
            throw new BusinessException(MessageConstant.USER_NOT_EXIST);
        }
        // 校验余额
        if (user.getBalance().compareTo(taskPublishDTO.getReward()) < 0) {
            throw new BusinessException(MessageConstant.BALANCE_NOT_ENOUGH);
        }

        // 处理取件地址（如果没传，可用默认值）
        String pickupAddress = taskPublishDTO.getPickupAddress();
        if (pickupAddress == null || pickupAddress.isBlank()) {
            // 针对不需取件的任务
            pickupAddress = "无需取件";
        }

        // 处理送达地址
        String deliveryAddress;
        String contactName;
        String contactPhone;
        if (taskPublishDTO.getDeliveryAddressId() != null) {
            UserAddress address = userAddressMapper.selectById(taskPublishDTO.getDeliveryAddressId());
            if (address == null || !address.getUserId().equals(userId)) {
                throw new BusinessException(MessageConstant.ADDRESS_NOT_EXISTS);
            }
            deliveryAddress = address.getDetail();
            contactName = address.getContactName() != null ? address.getContactName() : "";
            contactPhone = address.getContactPhone() != null ? address.getContactPhone() : "";
        } else {
            // 没有选择地址ID时，根据任务小类生成默认送达地址
            deliveryAddress = getDefaultDeliveryAddress(taskPublishDTO.getSubType());
            if (deliveryAddress == null) {
                throw new ParamErrorException(MessageConstant.NEED_SELECT_DELIVERY_ADDRESS);
            }
            // 使用手动输入的联系人信息
            contactName = taskPublishDTO.getContactName() != null ? taskPublishDTO.getContactName() : "";
            contactPhone = taskPublishDTO.getContactPhone() != null ? taskPublishDTO.getContactPhone() : "";
        }

        // 生成任务编号
        String taskNo = "T" + System.currentTimeMillis() / 1000 + IdUtil.fastSimpleUUID().substring(0, 6);
        // 设置任务过期时间
        LocalDateTime expireTime = LocalDateTime.now().plusMinutes(taskPublishDTO.getExpireMinutes());

        Task task = BeanUtil.copyProperties(taskPublishDTO, Task.class);

        task.setTaskNo(taskNo);
        task.setPublisherId(userId);

        if (taskPublishDTO.getSubType() != null) {
            task.setSubType(taskPublishDTO.getSubType());
        }

        if (taskPublishDTO.getTaskSpecs() != null) {
            task.setTaskSpecs(taskPublishDTO.getTaskSpecs());
        }

        if (taskPublishDTO.getImageUrls() != null) {
            task.setImageUrls(JSONUtil.toJsonStr(taskPublishDTO.getImageUrls()));
        }

        task.setPickupAddress(pickupAddress);
        task.setDeliveryAddress(deliveryAddress);
        task.setContactName(contactName);
        task.setContactPhone(contactPhone);

        task.setExpireTime(expireTime);
        task.setStatus(StatusConstant.TASK_WAITING);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.insert(task);

        // 保存任务图片到 task_image 表
        if (taskPublishDTO.getImageUrls() != null && !taskPublishDTO.getImageUrls().isEmpty()) {
            List<String> urls = taskPublishDTO.getImageUrls();
            for (int i = 0; i < urls.size(); i++) {
                TaskImage image = TaskImage.builder()
                        .taskId(task.getId())
                        .url(urls.get(i))
                        .sortOrder(i)
                        .build();
                taskImageMapper.insert(image);
            }
        }

        // 支付任务报酬
        paymentService.payForTask(userId, task.getId(), task.getReward());

        log.info("用户 {} 发布任务 {} 并支付报酬 {}", userId, task.getId(), task.getReward());

        // 事务提交后清除缓存，避免未提交数据被缓存
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                Objects.requireNonNull(cacheManager.getCache(RedisConstant.CACHE_TASK_HALL)).clear();
                Objects.requireNonNull(cacheManager.getCache(RedisConstant.CACHE_TASK_DETAIL)).clear();
            }
        });
    }

    /**
     * 配送员获取任务大厅列表方法
     *  逻辑：支持按类型、子类型、报酬范围条件筛选任务，
     *  查询状态为"待接单"且未过期的任务，按发布时间倒序排列，
     *  关联查询发布者信息填充昵称和头像，结果缓存到Redis
     *
     * @param type 任务类型
     * @param subType 任务子类型
     * @param minReward 最小报酬
     * @param maxReward 最大报酬
     * @param lng 经度（附近任务预留）
     * @param lat 纬度（附近任务预留）
     * @param page 页码
     * @param size 每页条数
     * @return PageResult<TaskListVO> 分页任务大厅列表
     */
    @Override
    public PageResult<TaskListVO> listTasksHall(String type, String subType, BigDecimal minReward,
                                            BigDecimal maxReward, BigDecimal lng, BigDecimal lat,
                                            int page, int size) {
        boolean canCache = type == null && subType == null && minReward == null
                && maxReward == null && page == 1;
        String cacheKey = "default";

        if (canCache) {
            Cache cache = cacheManager.getCache(RedisConstant.CACHE_TASK_HALL);
            if (cache != null) {
                Cache.ValueWrapper cached = cache.get(cacheKey);
                if (cached != null && cached.get() instanceof String) {
                    JSONObject json = JSONUtil.parseObj((String) cached.get());
                    Long total = json.getLong("total");
                    if (total != null && total > 0) {
                        List<TaskListVO> cachedRecords = json.getJSONArray("records").toList(TaskListVO.class);
                        return new PageResult<>(total, cachedRecords);
                    }
                }
            }

            // 击穿防护：缓存未命中时抢锁重建，避免大量请求同时打 DB
            RLock hallLock = redissonClient.getLock(RedisConstant.TASK_HALL_LOCK_KEY);
            try {
                if (hallLock.tryLock(RedisConstant.LOCK_WAIT_TIME, RedisConstant.LOCK_EXPIRE, TimeUnit.SECONDS)) {
                    // double-check
                    if (cache != null) {
                        Cache.ValueWrapper cached = cache.get(cacheKey);
                        if (cached != null && cached.get() instanceof String) {
                            JSONObject json = JSONUtil.parseObj((String) cached.get());
                            Long total = json.getLong("total");
                            if (total != null && total > 0) {
                                List<TaskListVO> cachedRecords = json.getJSONArray("records").toList(TaskListVO.class);
                                return new PageResult<>(total, cachedRecords);
                            }
                        }
                    }
                    PageResult<TaskListVO> result = queryHallTasks(page, size);
                    if (result.getTotal() > 0 && cache != null) {
                        cache.put(cacheKey, JSONUtil.toJsonStr(result));
                    }
                    return result;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                if (hallLock.isHeldByCurrentThread()) {
                    hallLock.unlock();
                }
            }
        }

        return queryHallTasks(page, size);
    }

    /**
     * 查询任务大厅列表（无缓存），可被带锁分支调用或作为非缓存请求的降级路径。
     */
    private PageResult<TaskListVO> queryHallTasks(int page, int size) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getStatus, StatusConstant.TASK_WAITING)
                .gt(Task::getExpireTime, LocalDateTime.now())
                .orderByDesc(Task::getCreatedAt);

        Page<Task> pageObj = new Page<>(page, size);
        IPage<Task> taskPage = taskMapper.selectPage(pageObj, wrapper);

        if (taskPage.getRecords().isEmpty()) {
            return new PageResult<>(0L, Collections.emptyList());
        }

        List<Long> publisherIds = taskPage.getRecords().stream()
                .map(Task::getPublisherId)
                .distinct()
                .collect(Collectors.toList());

        List<User> publishers = userMapper.selectBatchIds(publisherIds);
        Map<Long, User> publisherMap = publishers.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<TaskListVO> records = taskPage.getRecords().stream()
                .map(task -> convertToTaskListVO(task, publisherMap))
                .collect(Collectors.toList());

        return new PageResult<>(taskPage.getTotal(), records);
    }

    /**
     * 获取用户发布的任务列表方法
     *  逻辑：根据用户ID和状态条件分页查询任务，关联查询发布者信息
     *
     * @param userId 用户ID
     * @param status 任务状态筛选（可为null）
     * @param page 页码
     * @param size 每页条数
     * @return PageResult<TaskListVO> 分页任务列表
     */
    @Override
    public PageResult<TaskListVO> listMyPublishedTasks(Long userId, Integer status, int page, int size) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Task::getPublisherId, userId)
                .eq(status != null, Task::getStatus, status)
                .orderByDesc(Task::getCreatedAt);

        Page<Task> p = page(new Page<>(page, size), wrapper);

        if (p.getRecords().isEmpty()) {
            return new PageResult<>(0L, Collections.emptyList());
        }

        List<Long> publisherIds = p.getRecords().stream()
                .map(Task::getPublisherId)
                .distinct()
                .collect(Collectors.toList());

        List<User> publishers = userMapper.selectBatchIds(publisherIds);
        Map<Long, User> publisherMap = publishers.stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 查询当前用户已评价的任务ID
        List<Long> taskIds = p.getRecords().stream().map(Task::getId).collect(Collectors.toList());
        Set<Long> reviewedTaskIds = new HashSet<>(reviewMapper.selectReviewedTaskIds(taskIds, userId));

        List<TaskListVO> records = p.getRecords().stream()
                .map(task -> {
                    TaskListVO vo = convertToTaskListVO(task, publisherMap);
                    vo.setHasReviewed(reviewedTaskIds.contains(task.getId()));
                    return vo;
                })
                .collect(Collectors.toList());

        return new PageResult<>(p.getTotal(), records);
    }

    /**
     * 获取任务详情方法（手动缓存，防穿透 + 防击穿）。
     *
     * <p>缓存 key 仅使用 taskId，同任务的公共数据只存一份。
     * 对非发布者的脱敏处理（取件码、联系人姓名/电话）在缓存读回后执行。
     *
     * <p>穿透防护：对不存在的 taskId 写入空标记（60s TTL）。
     * <p>击穿防护：缓存未命中时基于 taskId 粒度的分布式锁重建。
     *
     * @param taskId 任务ID
     * @param currentUserId 当前登录用户ID
     * @return TaskDetailVO 任务详情VO
     */
    @Override
    public TaskDetailVO getTaskDetail(Long taskId, Long currentUserId) {
        String cacheKey = String.valueOf(taskId);

        // 1. 查 Spring Cache
        Cache cache = cacheManager.getCache(RedisConstant.CACHE_TASK_DETAIL);
        if (cache != null) {
            Cache.ValueWrapper cached = cache.get(cacheKey);
            if (cached != null && cached.get() instanceof String) {
                TaskDetailVO vo = JSONUtil.toBean((String) cached.get(), TaskDetailVO.class);
                applyTaskDetailMasking(vo);
                return vo;
            }
        }

        // 2. 穿透防护：查空标记
        String nullKey = RedisConstant.TASK_NOT_EXIST_PREFIX + taskId;
        if (stringRedisTemplate.hasKey(nullKey)) {
            throw new BusinessException(MessageConstant.TASK_NOT_EXIST);
        }

        // 3. 击穿防护：缓存未命中 → 分布式锁重建
        String lockKey = RedisConstant.TASK_DETAIL_LOCK_PREFIX + taskId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (lock.tryLock(RedisConstant.LOCK_WAIT_TIME, RedisConstant.LOCK_EXPIRE, TimeUnit.SECONDS)) {
                if (cache != null) {
                    Cache.ValueWrapper cached = cache.get(cacheKey);
                    if (cached != null && cached.get() instanceof String) {
                        TaskDetailVO vo = JSONUtil.toBean((String) cached.get(), TaskDetailVO.class);
                        applyTaskDetailMasking(vo);
                        return vo;
                    }
                }
                if (stringRedisTemplate.hasKey(nullKey)) {
                    throw new BusinessException(MessageConstant.TASK_NOT_EXIST);
                }

                Task task = getById(taskId);
                if (task == null) {
                    stringRedisTemplate.opsForValue().set(nullKey, "1",
                            RedisConstant.TASK_NOT_EXIST_TTL, TimeUnit.SECONDS);
                    throw new BusinessException(MessageConstant.TASK_NOT_EXIST);
                }

                TaskDetailVO vo = buildTaskDetailVO(task, currentUserId);
                if (cache != null) {
                    cache.put(cacheKey, JSONUtil.toJsonStr(vo));
                }
                return vo;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(MessageConstant.ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }

        // 4. 未获锁 → 降级直 DB（仍检查空标记防止穿透）
        if (stringRedisTemplate.hasKey(nullKey)) {
            throw new BusinessException(MessageConstant.TASK_NOT_EXIST);
        }
        Task task = getById(taskId);
        if (task == null) {
            stringRedisTemplate.opsForValue().set(nullKey, "1",
                    RedisConstant.TASK_NOT_EXIST_TTL, TimeUnit.SECONDS);
            throw new BusinessException(MessageConstant.TASK_NOT_EXIST);
        }
        return buildTaskDetailVO(task, currentUserId);
    }

    /**
     * 用户取消任务，仅待接单状态可取消。
     *
     * <p>校验和实现逻辑：
     * <ol>
     *   <li>Redisson 分布式锁并发控制</li>
     *   <li>校验任务存在且当前用户为发布者</li>
     *   <li>若任务已被接单或配送中，拒绝取消，提示联系配送员先取消订单</li>
     *   <li>状态机验证通过后更新任务状态为已取消</li>
     *   <li>退还任务报酬给发布者</li>
     *   <li>事务提交后清除任务缓存</li>
     * </ol>
     *
     * @param userId 用户ID
     * @param taskId 任务ID
     * @param cancelDTO 取消任务DTO（包含取消原因）
     */
    @Override
    @Transactional
    public void cancelTask(Long userId, Long taskId, CancelTaskDTO cancelDTO) {
        String lockKey = RedisConstant.ORDER_LOCK_KEY + taskId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            if (!lock.tryLock(RedisConstant.LOCK_WAIT_TIME, RedisConstant.LOCK_EXPIRE, TimeUnit.SECONDS)) {
                throw new BusinessException(MessageConstant.SYSTEM_BUSY);
            }
            Task task = getById(taskId);
            if (task == null) {
                throw new BusinessException(MessageConstant.TASK_NOT_EXIST);
            }
            if (!task.getPublisherId().equals(userId)) {
                throw new BusinessException(MessageConstant.TASK_CANCEL_FAILED);
            }
            // 已被接单或配送中的任务不允许发布者直接取消，需由配送员先取消订单
            if (StatusConstant.TASK_ACCEPTED.equals(task.getStatus())
                    || StatusConstant.TASK_DELIVERING.equals(task.getStatus())) {
                throw new BusinessException(MessageConstant.TASK_CANCEL_NOT_BE_ALLOWED);
            }
            TaskStateMachine.validate(task.getStatus(), StatusConstant.TASK_CANCELLED, "Task");
            task.setStatus(StatusConstant.TASK_CANCELLED);
            task.setCancelReason(cancelDTO.getReason());
            task.setUpdatedAt(LocalDateTime.now());
            updateById(task);

            paymentService.refundForTask(userId, taskId, task.getReward());
            log.info("任务 {} 已取消，退款 {} 元给用户 {}", taskId, task.getReward(), userId);

            // 事务提交后清除缓存
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    Objects.requireNonNull(cacheManager.getCache(RedisConstant.CACHE_TASK_HALL)).clear();
                    Objects.requireNonNull(cacheManager.getCache(RedisConstant.CACHE_TASK_DETAIL)).clear();
                }
            });
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(MessageConstant.ERROR);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 关键词搜索任务方法
     *  逻辑：根据关键词模糊匹配任务描述字段，支持按类型和报酬范围筛选，
     *  仅查询状态为"待接单"且未过期的任务
     *
     * @param keyword 搜索关键词
     * @param type 任务类型（可为null）
     * @param minReward 最小报酬（可为null）
     * @param maxReward 最大报酬（可为null）
     * @param page 页码
     * @param size 每页条数
     * @return PageResult<TaskListVO> 分页搜索结果
     */
    @Override
    public PageResult<TaskListVO> searchTasks(String keyword, String type, BigDecimal minReward,
                                              BigDecimal maxReward, int page, int size) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.like(keyword != null, Task::getPublicDesc, keyword)
                .eq(type != null, Task::getType, type)
                .ge(minReward != null, Task::getReward, minReward)
                .le(maxReward != null, Task::getReward, maxReward)
                .eq(Task::getStatus, StatusConstant.TASK_WAITING)
                .gt(Task::getExpireTime, LocalDateTime.now())
                .orderByDesc(Task::getCreatedAt);

        return buildTaskListResult(page, size, wrapper);
    }

    /**
     * 条件筛选任务方法
     *  逻辑：按地点（取件地址/送达地址）、性别要求、报酬范围等条件筛选任务，
     *  仅查询状态为"待接单"且未过期的任务
     *
     * @param type 任务类型（可为null）
     * @param pickupAddress 取件地址（可为null）
     * @param deliveryAddress 送达地址（可为null）
     * @param requireSex 性别限制（可为null）
     * @param minReward 最小报酬（可为null）
     * @param maxReward 最大报酬（可为null）
     * @param page 页码
     * @param size 每页条数
     * @return PageResult<TaskListVO> 分页筛选结果
     */
    @Override
    public PageResult<TaskListVO> filterTasks(String type, String pickupAddress, String deliveryAddress,
                                              String requireSex, BigDecimal minReward, BigDecimal maxReward,
                                              int page, int size) {
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(type != null, Task::getType, type)
                .like(pickupAddress != null, Task::getPickupAddress, pickupAddress)
                .like(deliveryAddress != null, Task::getDeliveryAddress, deliveryAddress)
                .eq(requireSex != null, Task::getRequireSex, requireSex)
                .ge(minReward != null, Task::getReward, minReward)
                .le(maxReward != null, Task::getReward, maxReward)
                .eq(Task::getStatus, StatusConstant.TASK_WAITING)
                .gt(Task::getExpireTime, LocalDateTime.now())
                .orderByDesc(Task::getCreatedAt);

        return buildTaskListResult(page, size, wrapper);
    }

    /**
     * 搜索附近任务列表方法
     *  逻辑：根据经纬度和半径范围查询附近任务，使用SQL空间计算过滤，
     *  关联查询发布者信息
     *
     * @param lng 经度
     * @param lat 纬度
     * @param radiusKm 搜索半径（公里）
     * @param page 页码
     * @param size 每页条数
     * @return PageResult<TaskListVO> 分页附近任务列表
     */
    @Override
    public PageResult<TaskListVO> listTasksNearby(BigDecimal lng, BigDecimal lat, Double radiusKm,
                                                  int page, int size) {
        int offset = (page - 1) * size;
        List<Task> tasks = taskMapper.selectNearbyTasks(lng, lat, radiusKm, offset, size);
        Long total = taskMapper.countNearbyTasks(lng, lat, radiusKm);
        if (tasks.isEmpty()) {
            return new PageResult<>(0L, Collections.emptyList());
        }

        List<Long> publisherIds = tasks.stream().map(Task::getPublisherId).distinct().collect(Collectors.toList());
        List<User> publishers = userMapper.selectBatchIds(publisherIds);
        Map<Long, User> publisherMap = publishers.stream().collect(Collectors.toMap(User::getId, u -> u));

        List<TaskListVO> records = tasks.stream()
                .map(task -> convertToTaskListVO(task, publisherMap))
                .collect(Collectors.toList());
        return new PageResult<>(total, records);
    }

    /**
     * 获取用户任务统计信息方法
     *  逻辑：统计用户发布任务的总数、待接单数、进行中数、已完成数和已取消数
     *
     * @param userId 用户ID
     * @return TaskStatisticsVO 任务统计VO
     */
    @Override
    public TaskStatisticsVO getTaskStatistics(Long userId) {
        List<Map<String, Object>> rows = taskMapper.countTasksByStatus(userId);
        Map<Integer, Long> statusCountMap = new HashMap<>();
        long total = 0;
        for (Map<String, Object> row : rows) {
            Integer status = (Integer) row.get("COALESCE(status, 0)");
            Long cnt = ((Number) row.get("COUNT(*)")).longValue();
            statusCountMap.put(status, cnt);
            total += cnt;
        }
        long waiting = statusCountMap.getOrDefault(StatusConstant.TASK_WAITING, 0L);
        long completed = statusCountMap.getOrDefault(StatusConstant.TASK_COMPLETED, 0L);
        long cancelled = statusCountMap.getOrDefault(StatusConstant.TASK_CANCELLED, 0L);
        long inProgress = total - waiting - completed - cancelled;

        return TaskStatisticsVO.builder()
                .totalPublished(total)
                .waitingCount(waiting)
                .inProgressCount(inProgress)
                .completedCount(completed)
                .cancelledCount(cancelled)
                .build();
    }

}