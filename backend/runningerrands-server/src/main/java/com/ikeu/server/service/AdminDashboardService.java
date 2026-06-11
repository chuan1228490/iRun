package com.ikeu.server.service;

import com.ikeu.model.vo.CategoryPieVO;
import com.ikeu.model.vo.DashboardVO;
import com.ikeu.model.vo.TrendPointVO;

import java.util.List;

/**
 * 管理端仪表盘服务接口，按统计维度拆分为独立方法。
 * @author ikeu
 * @since 2026/06/11
 */
public interface AdminDashboardService {

    /** 统计卡片摘要数据（用户数、任务数、订单数、今日收益等标量指标）。 */
    DashboardVO getDashboardSummary();

    /** 近7天新增用户趋势。 */
    List<TrendPointVO> getUserTrend();

    /** 近7天收入趋势。 */
    List<TrendPointVO> getRevenueTrend();

    /** 任务分类占比。 */
    List<CategoryPieVO> getTaskCategories();

    /** 订单状态分布。 */
    List<CategoryPieVO> getOrderStatusDistribution();
}
