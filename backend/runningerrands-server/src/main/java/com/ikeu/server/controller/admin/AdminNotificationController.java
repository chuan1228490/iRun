package com.ikeu.server.controller.admin;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ikeu.common.constant.MessageConstant;
import com.ikeu.common.result.Result;
import com.ikeu.model.dto.NotificationBroadcastDTO;
import com.ikeu.model.dto.NotificationSendDTO;
import com.ikeu.model.entity.User;
import com.ikeu.server.annotation.OperationLog;
import com.ikeu.server.annotation.RequireRole;
import com.ikeu.server.mapper.UserMapper;
import com.ikeu.server.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "管理端-消息管理")
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final NotificationService notificationService;
    private final UserMapper userMapper;

    @OperationLog(module = "消息管理", action = "发送", description = "向指定用户发送通知")
    @RequireRole({1, 2})
    @Operation(summary = "向指定用户发送通知")
    @PostMapping("/notifications/send")
    public Result<Void> send(@Valid @RequestBody NotificationSendDTO dto) {
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
        return Result.success(MessageConstant.SUCCESS);
    }

    @OperationLog(module = "消息管理", action = "广播", description = "向所有用户广播通知")
    @RequireRole({1, 2})
    @Operation(summary = "向所有用户广播通知")
    @PostMapping("/notifications/broadcast")
    public Result<Void> broadcast(@Valid @RequestBody NotificationBroadcastDTO dto) {
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
        return Result.success(MessageConstant.SUCCESS);
    }
}
