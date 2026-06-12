package com.ikeu.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.constant.RedisConstant;
import com.ikeu.common.constant.StatusConstant;
import com.ikeu.common.exception.BusinessException;
import com.ikeu.common.exception.NotFoundException;
import com.ikeu.common.result.PageResult;
import com.ikeu.model.entity.RunnerProfile;
import com.ikeu.model.entity.User;
import com.ikeu.model.vo.RunnerManageVO;
import com.ikeu.server.mapper.RunnerProfileMapper;
import com.ikeu.server.mapper.TransactionRecordMapper;
import com.ikeu.server.mapper.UserMapper;
import com.ikeu.server.service.AdminRunnerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端跑腿员管理服务实现，处理跑腿员列表、详情、审核和封禁。
 * @author ikeu
 * @since 2026/06/11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRunnerServiceImpl implements AdminRunnerService {

    private final RunnerProfileMapper runnerProfileMapper;
    private final UserMapper userMapper;
    private final TransactionRecordMapper transactionRecordMapper;

    /**
     * 分页查询跑腿员列表，JOIN 用户表填充昵称/手机号，批量查询累计收入。
     */
    @Override
    public PageResult<RunnerManageVO> listRunners(Integer verifyStatus, String keyword, int page, int size) {
        String kw = (keyword != null && !keyword.isBlank()) ? keyword : null;
        Page<RunnerProfile> p = (Page<RunnerProfile>) runnerProfileMapper.selectRunnersWithKeyword(new Page<>(page, size), verifyStatus, kw);
        if (p.getRecords().isEmpty()) return new PageResult<>(0L, Collections.emptyList());

        List<Long> userIds = p.getRecords().stream().map(RunnerProfile::getUserId).collect(Collectors.toList());
        List<User> users = userMapper.selectBatchIds(userIds);
        Map<Long, User> userMap = users.stream().collect(Collectors.toMap(User::getId, u -> u));

        Map<Long, BigDecimal> incomeMap = transactionRecordMapper.sumIncomeByUserIds(userIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row.get("user_id"),
                        row -> (BigDecimal) row.get("total_income")));

        List<RunnerManageVO> records = p.getRecords().stream().map(rp -> {
            User u = userMap.get(rp.getUserId());
            RunnerManageVO vo = BeanUtil.copyProperties(rp, RunnerManageVO.class);
            vo.setProfileId(rp.getId());
            vo.setUserId(rp.getUserId());
            vo.setAvgRating(rp.getAvgRating() != null ? rp.getAvgRating().doubleValue() : null);
            vo.setTotalEarnings(incomeMap.getOrDefault(rp.getUserId(), BigDecimal.ZERO));
            if (u != null) {
                vo.setNickname(u.getNickname());
                vo.setPhone(u.getPhone());
            }
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(p.getTotal(), records);
    }

    /**
     * 查询跑腿员详情，含用户信息和 DB 侧 SUM 聚合的累计收入。
     */
    @Override
    public RunnerManageVO getRunnerDetail(Long profileId) {
        RunnerProfile rp = runnerProfileMapper.selectById(profileId);
        if (rp == null) throw new NotFoundException(MessageConstant.RUNNER_NOT_EXIST);
        User u = userMapper.selectById(rp.getUserId());

        BigDecimal totalIncome = transactionRecordMapper.sumIncomeByUserId(rp.getUserId());

        RunnerManageVO vo = BeanUtil.copyProperties(rp, RunnerManageVO.class);
        vo.setProfileId(rp.getId());
        vo.setUserId(rp.getUserId());
        vo.setAvgRating(rp.getAvgRating() != null ? rp.getAvgRating().doubleValue() : null);
        vo.setTotalEarnings(totalIncome);
        if (u != null) {
            vo.setNickname(u.getNickname());
            vo.setPhone(u.getPhone());
        }
        return vo;
    }

    /**
     * 审核跑腿员认证，仅“审核中”状态可操作，操作后清除仪表盘缓存。
     */
    @Override
    @Transactional
    @CacheEvict(value = RedisConstant.CACHE_DASHBOARD, allEntries = true)
    public void reviewRunnerCertification(Long runnerProfileId, Integer verifyStatus, String remark) {
        RunnerProfile profile = runnerProfileMapper.selectById(runnerProfileId);
        if (profile == null) throw new NotFoundException(MessageConstant.RUNNER_NOT_EXIST);
        if (!profile.getVerifyStatus().equals(StatusConstant.CERTIFY_AUDITING)) {
            throw new BusinessException(MessageConstant.RUNNER_NOT_IN_AUDITING_STATUS);
        }
        profile.setVerifyStatus(verifyStatus);
        profile.setVerifyRemark(remark != null ? remark : "");
        profile.setUpdatedAt(LocalDateTime.now());
        runnerProfileMapper.updateById(profile);

        log.info("管理员审核跑腿员 {} 结果为 {}", runnerProfileId, verifyStatus);
    }

    /**
     * 禁止/恢复跑腿员接单，操作后清除仪表盘缓存。
     */
    @Override
    @Transactional
    @CacheEvict(value = RedisConstant.CACHE_DASHBOARD, allEntries = true)
    public void toggleRunnerBan(Long profileId, boolean banned) {
        RunnerProfile profile = runnerProfileMapper.selectById(profileId);
        if (profile == null) throw new NotFoundException(MessageConstant.RUNNER_NOT_EXIST);
        profile.setIsBanned(banned ? 1 : 0);
        profile.setUpdatedAt(LocalDateTime.now());
        runnerProfileMapper.updateById(profile);
        log.info("管理员 {} 跑腿员 {} (profileId={})", banned ? "禁止" : "恢复", profile.getUserId(), profileId);
    }
}
