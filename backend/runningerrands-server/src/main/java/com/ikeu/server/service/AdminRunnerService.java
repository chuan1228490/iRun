package com.ikeu.server.service;

import com.ikeu.common.result.PageResult;
import com.ikeu.model.vo.RunnerManageVO;

/**
 * 管理端跑腿员管理服务接口，提供跑腿员列表、详情、审核和封禁功能。
 * @author ikeu
 * @since 2026/06/11
 */
public interface AdminRunnerService {

    /** 分页查询配送员管理列表，支持按认证状态和关键词筛选。 */
    PageResult<RunnerManageVO> listRunners(Integer verifyStatus, String keyword, int page, int size);

    /** 获取跑腿员详情（含累计收入）。 */
    RunnerManageVO getRunnerDetail(Long profileId);

    /** 审核配送员认证申请，更新认证状态和备注。 */
    void reviewRunnerCertification(Long runnerProfileId, Integer verifyStatus, String remark);

    /** 禁止/恢复跑腿员接单。 */
    void toggleRunnerBan(Long profileId, boolean banned);
}
