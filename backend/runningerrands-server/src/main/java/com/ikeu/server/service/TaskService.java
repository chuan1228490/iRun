package com.ikeu.server.service;

import com.ikeu.model.dto.CancelTaskDTO;
import com.ikeu.model.dto.TaskPublishDTO;
import com.ikeu.model.vo.TaskDetailVO;
import com.ikeu.model.vo.TaskListVO;
import com.ikeu.model.vo.TaskStatisticsVO;
import com.ikeu.common.result.PageResult;

import java.math.BigDecimal;

/**
 * 任务服务接口，提供任务发布、大厅浏览、搜索筛选、取消和统计等功能。
 * @author ikeu
 * @since 2026/05/14
 */
public interface TaskService {

    /** 用户发布新任务，保存任务记录和图片列表。 */
    void publishTask(Long userId, TaskPublishDTO dto);

    /** 用户分页查询任务大厅列表，支持按类型、报酬范围和地理位置筛选。 */
    PageResult<TaskListVO> listTasksHall(String type, String subType, BigDecimal minReward,
                                     BigDecimal maxReward, BigDecimal lng, BigDecimal lat,
                                     int page, int size);

    /** 用户查询当前自己发布的任务列表，支持按状态筛选。 */
    PageResult<TaskListVO> listMyPublishedTasks(Long userId, Integer status, int page, int size);

    /** 查询任务详细信息，关联发布者信息和图片列表。 */
    TaskDetailVO getTaskDetail(Long taskId, Long currentUserId);

    /** 取消任务，仅发布者可在待接单状态下取消。 */
    void cancelTask(Long userId, Long taskId, CancelTaskDTO cancelTaskDTO);

    /** 按关键词搜索任务（模糊匹配标题和描述）。 */
    PageResult<TaskListVO> searchTasks(String keyword, String type, BigDecimal minReward,
                                       BigDecimal maxReward, int page, int size);

    /** 按多条件筛选任务（地点、性别要求、报酬范围）。 */
    PageResult<TaskListVO> filterTasks(String type, String pickupAddress, String deliveryAddress,
                                       String requireSex, BigDecimal minReward, BigDecimal maxReward,
                                       int page, int size);

    /** 按经纬度搜索附近任务，使用 Haversine 公式计算距离。 */
    PageResult<TaskListVO> listTasksNearby(BigDecimal lng, BigDecimal lat, Double radiusKm,
                                           int page, int size);

    /** 统计当前用户发布任务的数量分布。 */
    TaskStatisticsVO getTaskStatistics(Long userId);
}
