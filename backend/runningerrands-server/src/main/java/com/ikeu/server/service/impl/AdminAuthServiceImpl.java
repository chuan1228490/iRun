package com.ikeu.server.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ikeu.common.constant.JwtClaimsConstant;
import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.constant.RedisConstant;
import com.ikeu.common.constant.StatusConstant;
import com.ikeu.common.context.BaseContext;
import com.ikeu.common.exception.BusinessException;
import com.ikeu.common.exception.UnauthorizedException;
import com.ikeu.common.properties.JwtProperties;
import com.ikeu.common.utils.JwtUtil;
import com.ikeu.model.dto.AdminLoginDTO;
import com.ikeu.model.entity.Admin;
import com.ikeu.model.vo.AdminLoginVO;
import com.ikeu.server.mapper.AdminMapper;
import com.ikeu.server.service.AdminAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 管理端认证服务实现，处理管理员登录、令牌刷新和退出。
 * @author ikeu
 * @since 2026/06/11
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl extends ServiceImpl<AdminMapper, Admin> implements AdminAuthService {

    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final StringRedisTemplate redisTemplate;

    /**
     * 管理员登录，校验失败计数（防暴力破解）→ 校验密码 → 校验状态，
     * 通过后生成双令牌（access+refresh），refresh token 写入 Redis 支持轮换。
     */
    @Override
    @Transactional
    public AdminLoginVO login(AdminLoginDTO dto) {
        String failKey = RedisConstant.ADMIN_LOGIN_FAIL_PREFIX + dto.getUsername();
        String failCount = redisTemplate.opsForValue().get(failKey);
        if (failCount != null && Integer.parseInt(failCount) >= RedisConstant.LOGIN_MAX_FAIL_COUNT) {
            throw new BusinessException(MessageConstant.LOGIN_FAIL_LOCKED);
        }

        Admin admin = lambdaQuery().eq(Admin::getUsername, dto.getUsername()).one();
        if (admin == null || !passwordEncoder.matches(dto.getPassword(), admin.getPassword())) {
            redisTemplate.opsForValue().increment(failKey);
            redisTemplate.expire(failKey, RedisConstant.LOGIN_LOCK_SECONDS, TimeUnit.SECONDS);
            throw new BusinessException(MessageConstant.ADMIN_LOGIN_FAILED);
        }

        redisTemplate.delete(failKey);
        if (Objects.equals(admin.getStatus(), StatusConstant.DISABLE)) {
            throw new BusinessException(MessageConstant.ADMIN_DISABLED);
        }
        admin.setLastLoginTime(LocalDateTime.now());
        updateById(admin);

        String accessToken = jwtUtil.generateAdminAccessToken(Map.of(JwtClaimsConstant.ADMIN_ID, admin.getId()));
        String refreshToken = jwtUtil.generateAdminRefreshToken(Map.of(JwtClaimsConstant.ADMIN_ID, admin.getId()));

        var refreshClaims = jwtUtil.parseAdminRefreshToken(refreshToken);
        String jti = refreshClaims.get(JwtClaimsConstant.JTI, String.class);
        long ttlSeconds = jwtProperties.getAdminRefreshTtl() / 1000;
        redisTemplate.opsForValue().set(
                RedisConstant.ADMIN_REFRESH_TOKEN_PREFIX + admin.getId() + ":" + jti,
                refreshToken,
                ttlSeconds,
                TimeUnit.SECONDS);

        return AdminLoginVO.builder()
                .adminId(admin.getId())
                .username(admin.getUsername())
                .name(admin.getName())
                .role(admin.getRole())
                .token(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtProperties.getAdminAccessTtl() / 1000)
                .build();
    }

    /**
     * 获取当前登录管理员信息（不含令牌），管理员不存在或已禁用则抛异常。
     */
    @Override
    public AdminLoginVO getAdminInfo() {
        Long adminId = BaseContext.getCurrentId();
        Admin admin = getById(adminId);
        if (admin == null || Objects.equals(admin.getStatus(), StatusConstant.DISABLE)) {
            throw new UnauthorizedException(MessageConstant.ACCOUNT_DISABLED_OR_NOT_EXIST);
        }
        return AdminLoginVO.builder()
                .adminId(admin.getId())
                .username(admin.getUsername())
                .name(admin.getName())
                .role(admin.getRole())
                .build();
    }

    /**
     * 令牌轮换：解析 refresh token → 校验 type/jti → Redis 反查 →
     * 删除旧 token → 颁发新 token 对，旧 refresh token 立即失效。
     */
    @Override
    public AdminLoginVO refreshAccessToken(String refreshToken) {
        var claims = jwtUtil.parseAdminRefreshToken(refreshToken);
        String type = claims.get(JwtClaimsConstant.TOKEN_TYPE, String.class);
        if (!JwtClaimsConstant.TOKEN_TYPE_REFRESH.equals(type)) {
            throw new UnauthorizedException(MessageConstant.REFRESH_TOKEN_INVALID);
        }
        Long adminId = jwtUtil.getAdminIdFromClaims(claims);
        if (adminId == null) {
            throw new UnauthorizedException(MessageConstant.REFRESH_TOKEN_INVALID);
        }
        String jti = claims.get(JwtClaimsConstant.JTI, String.class);

        String redisKey = RedisConstant.ADMIN_REFRESH_TOKEN_PREFIX + adminId + ":" + jti;
        String storedToken = redisTemplate.opsForValue().get(redisKey);
        if (storedToken == null) {
            throw new UnauthorizedException(MessageConstant.REFRESH_TOKEN_EXPIRED);
        }

        redisTemplate.delete(redisKey);

        Admin admin = getById(adminId);
        if (admin == null || Objects.equals(admin.getStatus(), StatusConstant.DISABLE)) {
            throw new UnauthorizedException(MessageConstant.ACCOUNT_DISABLED_OR_NOT_EXIST);
        }

        String newAccessToken = jwtUtil.generateAdminAccessToken(Map.of(JwtClaimsConstant.ADMIN_ID, adminId));
        String newRefreshToken = jwtUtil.generateAdminRefreshToken(Map.of(JwtClaimsConstant.ADMIN_ID, adminId));

        var newClaims = jwtUtil.parseAdminRefreshToken(newRefreshToken);
        String newJti = newClaims.get(JwtClaimsConstant.JTI, String.class);
        long ttlSeconds = jwtProperties.getAdminRefreshTtl() / 1000;
        redisTemplate.opsForValue().set(
                RedisConstant.ADMIN_REFRESH_TOKEN_PREFIX + adminId + ":" + newJti,
                newRefreshToken,
                ttlSeconds,
                TimeUnit.SECONDS);

        return AdminLoginVO.builder()
                .adminId(admin.getId())
                .username(admin.getUsername())
                .name(admin.getName())
                .role(admin.getRole())
                .token(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtProperties.getAdminAccessTtl() / 1000)
                .build();
    }

    /**
     * 管理员退出，通过 {@code admin:refresh:token:{id}:*} 通配清除所有 token，
     * 使所有终端同时失效。
     */
    @Override
    public void logout(Long adminId) {
        if (adminId == null) return;
        String pattern = RedisConstant.ADMIN_REFRESH_TOKEN_PREFIX + adminId + ":*";
        var keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        log.info("管理员 {} 退出登录，已清除 refresh token", adminId);
    }
}
