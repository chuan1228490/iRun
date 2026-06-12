package com.ikeu.server.service;

import com.ikeu.model.dto.SetMaxOrdersDTO;
import com.ikeu.model.vo.RunnerInfoVO;
import com.ikeu.model.vo.RunnerRankingVO;
import com.ikeu.model.vo.RunnerPerformanceVO;

import java.util.List;

/**
 * 配送员档案服务接口，提供档案查询、上下线、接单数设置、排行榜和申请等功能。
 * @author ikeu
 * @since 2026/05/14
 */
public interface RunnerProfileService {

    /** 申请成为配送员，需已通过学生实名认证。 */
    void applyForRunner(Long userId);

    /** 获取当前用户的配送员档案信息。 */
    RunnerInfoVO getProfile(Long userId);

    /** 配送员上线，允许接收新订单。 */
    void goOnline(Long userId);

    /** 配送员下线，停止接收新订单。 */
    void goOffline(Long userId);

    /** 设置最大同时接单数量。 */
    void setMaxOrders(Long userId, SetMaxOrdersDTO dto);

    /** 原子减少当前接单数（完成或取消订单后调用）。 */
    void decrementCurrentOrders(Long userId);

    /** 获取指定配送员的表现数据（接单统计、评分等）。 */
    RunnerPerformanceVO getRunnerPerformance(Long runnerId);

    /** 获取配送员排行榜，支持按接单数或评分排序。 */
    List<RunnerRankingVO> getLeaderboard(String sortBy, int limit);

}
