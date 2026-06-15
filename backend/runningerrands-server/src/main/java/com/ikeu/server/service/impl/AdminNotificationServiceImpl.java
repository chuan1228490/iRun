package com.ikeu.server.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ikeu.model.dto.NotificationBroadcastDTO;
import com.ikeu.model.dto.NotificationSendDTO;
import com.ikeu.model.entity.User;
import com.ikeu.server.mapper.UserMapper;
import com.ikeu.server.service.AdminNotificationService;
import com.ikeu.server.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 管理端消息管理服务实现，处理向指定用户发送通知和全站广播通知。
 * @author ikeu
 * @since 2026/06/15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNotificationServiceImpl implements AdminNotificationService {

    private final NotificationService notificationService;
    private final UserMapper userMapper;

    @Override
    public void sendToUsers(NotificationSendDTO dto) {
        for (Long userId : dto.getUserIds()) {
            try {
                User user = userMapper.selectById(userId);
                if (user == null || !user.getStatus().equals(1)) {
                    log.warn("用户 {} 不存在或已禁用，跳过发送通知", userId);
                    continue;
                }
                notificationService.sendNotification(userId, dto.getType(), dto.getTitle(), dto.getContent(), null);
            } catch (Exception e) {
                log.error("向用户 {} 发送通知失败", userId, e);
            }
        }
        log.info("管理员向 {} 个用户发送了通知", dto.getUserIds().size());
    }

    @Override
    public void broadcastToAll(NotificationBroadcastDTO dto) {
        List<User> users = userMapper.selectList(
                new LambdaQueryWrapper<User>().eq(User::getStatus, 1));
        for (User user : users) {
            try {
                notificationService.sendNotification(user.getId(), dto.getType(), dto.getTitle(), dto.getContent(), null);
            } catch (Exception e) {
                log.error("向用户 {} 广播通知失败", user.getId(), e);
            }
        }
        log.info("管理员向 {} 个用户广播了通知", users.size());
    }
}
