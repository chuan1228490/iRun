package com.ikeu.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ikeu.model.entity.RunnerProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 配送员档案 Mapper，提供档案 CRUD 及信用分、订单统计的原子更新。
 * @author ikeu
 * @since 2025/05/21
 */
@Mapper
public interface RunnerProfileMapper extends BaseMapper<RunnerProfile> {

    /**
     * 单次 JOIN 查询替代双阶段关键词搜索，关联 user 表匹配昵称或手机号。
     *
     * @param page 分页对象
     * @param verifyStatus 认证状态（可选）
     * @param keyword 关键词（可选，匹配昵称、手机号）
     * @return 分页结果
     */
    IPage<RunnerProfile> selectRunnersWithKeyword(Page<RunnerProfile> page,
                                                  @Param("verifyStatus") Integer verifyStatus,
                                                  @Param("keyword") String keyword);

    /**
     * 原子更新信用分，消除 read-before-write 竞态。结果不小于0。
     *
     * @param userId 用户ID
     * @param delta 信用分增量（可为负数）
     */
    @Update("UPDATE runner_profile SET credit_score = GREATEST(0, credit_score + #{delta}) WHERE user_id = #{userId}")
    void updateCreditScore(@Param("userId") Long userId, @Param("delta") int delta);

    /**
     * 原子更新配送员订单统计（总单+1、成功+1、当前接单-1）。
     *
     * @param userId 用户ID
     */
    @Update("UPDATE runner_profile SET total_orders = total_orders + 1, success_orders = success_orders + 1, " +
            "current_orders = GREATEST(current_orders - 1, 0) WHERE user_id = #{userId}")
    void incrementOrderStats(@Param("userId") Long userId);

    /**
     * 原子增加完成单数统计（不含 current_orders 变动）。
     *
     * @param userId 用户ID
     */
    @Update("UPDATE runner_profile SET total_orders = total_orders + 1, success_orders = success_orders + 1 " +
            "WHERE user_id = #{userId}")
    void incrementCompletedStats(@Param("userId") Long userId);

    /**
     * 原子增加当前接单数。
     *
     * @param userId 用户ID
     */
    @Update("UPDATE runner_profile SET current_orders = current_orders + 1 WHERE user_id = #{userId}")
    void incrementCurrentOrders(@Param("userId") Long userId);

    /**
     * 原子减少当前接单数（最小为0）。
     *
     * @param userId 用户ID
     */
    @Update("UPDATE runner_profile SET current_orders = GREATEST(current_orders - 1, 0) WHERE user_id = #{userId}")
    void decrementCurrentOrders(@Param("userId") Long userId);

    /**
     * 更新跑腿员平均评分。
     *
     * @param avgRating 新的平均评分
     * @param userId 用户ID
     */
    @Update("UPDATE runner_profile SET avg_rating = #{avgRating} WHERE user_id = #{userId}")
    void updateAvgRating(@Param("avgRating") java.math.BigDecimal avgRating, @Param("userId") Long userId);
}
