package com.ikeu.server.service;

import com.ikeu.common.result.PageResult;
import com.ikeu.model.vo.TaskDetailVO;
import com.ikeu.model.vo.TaskListVO;

/**
 * 管理端任务管理服务接口，提供任务列表、详情和强制状态更新功能。
 * @author ikeu
 * @since 2026/06/11
 */
public interface AdminTaskService {

    /** 分页查询所有任务列表，支持按状态筛选。 */
    PageResult<TaskListVO> listAllTasks(Integer status, int page, int size);

    /** 获取任务详情。 */
    TaskDetailVO getTaskDetail(Long taskId);

    /** 管理员强制更新任务状态。 */
    void updateTaskStatus(Long taskId, Integer status);
}
