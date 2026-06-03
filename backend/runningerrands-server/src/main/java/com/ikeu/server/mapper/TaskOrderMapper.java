package com.ikeu.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ikeu.model.entity.TaskOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 任务订单 Mapper，提供订单实体的基础 CRUD 操作。
 * @author ikeu
 * @since 2025/05/21
 */
@Mapper
public interface TaskOrderMapper extends BaseMapper<TaskOrder> {

    /**
     * 条件更新订单状态：仅在订单当前状态符合预期时才更新，用于防止并发覆盖。
     *
     * @param orderId 订单ID
     * @param expectedStatus 期望的当前状态
     * @param targetStatus 目标状态
     * @param cancelReason 取消原因
     * @return 影响行数（0表示状态已被其他操作变更）
     */
    @Update("UPDATE task_order SET status = #{targetStatus}, cancel_reason = #{cancelReason} " +
            "WHERE id = #{orderId} AND status = #{expectedStatus}")
    int updateStatusIf(@Param("orderId") Long orderId,
                       @Param("expectedStatus") Integer expectedStatus,
                       @Param("targetStatus") Integer targetStatus,
                       @Param("cancelReason") String cancelReason);
}
