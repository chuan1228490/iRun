package com.ikeu.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ikeu.common.constant.RedisConstant;
import com.ikeu.common.constant.StatusConstant;
import com.ikeu.model.entity.RunnerProfile;
import com.ikeu.model.entity.Task;
import com.ikeu.model.entity.TaskOrder;
import com.ikeu.model.entity.User;
import com.ikeu.model.vo.CategoryPieVO;
import com.ikeu.model.vo.DashboardVO;
import com.ikeu.model.vo.TrendPointVO;
import com.ikeu.server.mapper.RunnerProfileMapper;
import com.ikeu.server.mapper.TaskMapper;
import com.ikeu.server.mapper.TaskOrderMapper;
import com.ikeu.server.mapper.UserMapper;
import com.ikeu.server.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 管理端仪表盘服务实现，按统计维度拆分为独立缓存方法。
 * @author ikeu
 * @since 2026/06/11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final UserMapper userMapper;
    private final TaskMapper taskMapper;
    private final TaskOrderMapper taskOrderMapper;
    private final RunnerProfileMapper runnerProfileMapper;

    /**
     * 统计卡片摘要：用户/任务/订单总数、认证/在线跑腿员数、今日新增/完成量、今日交易额。
     * 缓存 3 分钟，写操作通过 {@code @CacheEvict(allEntries = true)} 清除。
     */
    @Override
    @Cacheable(value = RedisConstant.CACHE_DASHBOARD, key = "'summary'")
    public DashboardVO getDashboardSummary() {
        Long userCount = userMapper.selectCount(null);
        Long taskCount = taskMapper.selectCount(null);
        Long orderCount = taskOrderMapper.selectCount(null);
        Long runnerCount = runnerProfileMapper.selectCount(
                new LambdaQueryWrapper<RunnerProfile>().eq(RunnerProfile::getVerifyStatus, StatusConstant.CERTIFY_APPROVED));
        Long onlineRunnerCount = runnerProfileMapper.selectCount(
                new LambdaQueryWrapper<RunnerProfile>().eq(RunnerProfile::getIsOnline, 1));

        LocalDateTime todayStart = LocalDateTime.of(LocalDate.now(), LocalTime.MIN);

        Long todayNewUsers = userMapper.selectCount(
                new LambdaQueryWrapper<User>().ge(User::getCreatedAt, todayStart));
        Long todayNewTasks = taskMapper.selectCount(
                new LambdaQueryWrapper<Task>().ge(Task::getCreatedAt, todayStart));
        Long todayCompletedOrders = taskOrderMapper.selectCount(
                new LambdaQueryWrapper<TaskOrder>().eq(TaskOrder::getStatus, StatusConstant.ORDER_COMPLETED)
                        .ge(TaskOrder::getConfirmTime, todayStart));

        BigDecimal todayRevenue = taskMapper.sumRewardByStatusSince(StatusConstant.TASK_COMPLETED, todayStart);

        return DashboardVO.builder()
                .userCount(userCount).taskCount(taskCount)
                .orderCount(orderCount).runnerCount(runnerCount)
                .onlineRunnerCount(onlineRunnerCount)
                .todayRevenue(todayRevenue).todayNewUsers(todayNewUsers)
                .todayNewTasks(todayNewTasks).todayCompletedOrders(todayCompletedOrders)
                .build();
    }

    /**
     * 近 7 天新增用户趋势，逐日统计 {@code created_at} 落入当天的用户数，补零填充无数据日期。
     */
    @Override
    @Cacheable(value = RedisConstant.CACHE_DASHBOARD, key = "'userTrend'")
    public List<TrendPointVO> getUserTrend() {
        LocalDateTime sevenDaysAgo = LocalDateTime.of(LocalDate.now().minusDays(6), LocalTime.MIN);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        List<TrendPointVO> result = new ArrayList<>();
        LocalDate cursor = sevenDaysAgo.toLocalDate();
        LocalDate today = LocalDate.now();
        while (!cursor.isAfter(today)) {
            LocalDateTime dayStart = cursor.atStartOfDay();
            LocalDateTime dayEnd = cursor.plusDays(1).atStartOfDay();
            Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                    .ge(User::getCreatedAt, dayStart).lt(User::getCreatedAt, dayEnd));
            result.add(TrendPointVO.builder().date(cursor.format(fmt)).value(count).build());
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    /**
     * 近 7 天收入趋势，调用 {@code taskMapper.sumRewardPerDay()} 聚合已完成任务报酬，补零填充。
     */
    @Override
    @Cacheable(value = RedisConstant.CACHE_DASHBOARD, key = "'revenueTrend'")
    public List<TrendPointVO> getRevenueTrend() {
        LocalDateTime sevenDaysAgo = LocalDateTime.of(LocalDate.now().minusDays(6), LocalTime.MIN);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");
        List<Map<String, Object>> rows = taskMapper.sumRewardPerDay(StatusConstant.TASK_COMPLETED, sevenDaysAgo);
        Map<String, BigDecimal> dateSum = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String date = row.get("date").toString();
            BigDecimal val = row.get("value") != null ? new BigDecimal(row.get("value").toString()) : BigDecimal.ZERO;
            dateSum.put(date, val);
        }
        List<TrendPointVO> result = new ArrayList<>();
        LocalDate cursor = sevenDaysAgo.toLocalDate();
        LocalDate today = LocalDate.now();
        while (!cursor.isAfter(today)) {
            String key = cursor.toString();
            Long value = dateSum.containsKey(key) ? dateSum.get(key).longValue() : 0L;
            result.add(TrendPointVO.builder().date(cursor.format(fmt)).value(value).build());
            cursor = cursor.plusDays(1);
        }
        return result;
    }

    /**
     * 任务分类占比，调用 {@code taskMapper.countTasksByType()} 按类型分组统计。
     */
    @Override
    @Cacheable(value = RedisConstant.CACHE_DASHBOARD, key = "'taskCategories'")
    public List<CategoryPieVO> getTaskCategories() {
        List<Map<String, Object>> rows = taskMapper.countTasksByType();
        return rows.stream().map(row -> {
            String name = row.get("name") != null ? row.get("name").toString() : "";
            Long value = row.get("value") != null ? Long.parseLong(row.get("value").toString()) : 0L;
            return CategoryPieVO.builder().name(name).value(value).build();
        }).collect(Collectors.toList());
    }

    /**
     * 订单状态分布，遍历 1~5 状态值统计各状态未删除订单数。
     */
    @Override
    @Cacheable(value = RedisConstant.CACHE_DASHBOARD, key = "'orderStatusDistribution'")
    public List<CategoryPieVO> getOrderStatusDistribution() {
        String[] labels = {"待取货", "配送中", "待确认", "已完成", "已取消"};
        List<CategoryPieVO> result = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Long count = taskOrderMapper.selectCount(
                    new LambdaQueryWrapper<TaskOrder>().eq(TaskOrder::getStatus, i)
                            .eq(TaskOrder::getIsDeleted, 0));
            result.add(CategoryPieVO.builder().name(labels[i - 1]).value(count).build());
        }
        return result;
    }
}
