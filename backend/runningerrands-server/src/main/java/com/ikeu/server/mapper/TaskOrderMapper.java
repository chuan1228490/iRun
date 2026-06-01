package com.ikeu.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ikeu.model.entity.TaskOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 任务订单 Mapper，提供订单实体的基础 CRUD 操作。
 * @author ikeu
 * @since 2025/05/21
 */
@Mapper
public interface TaskOrderMapper extends BaseMapper<TaskOrder> {
}
