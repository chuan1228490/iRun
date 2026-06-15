package com.ikeu.server.service;

import com.ikeu.model.dto.NotificationBroadcastDTO;
import com.ikeu.model.dto.NotificationSendDTO;

/**
 * 管理端消息管理服务接口，提供向指定用户发送通知和全站广播通知功能。
 * @author ikeu
 * @since 2026/06/15
 */
public interface AdminNotificationService {

    /** 向指定用户列表发送通知，跳过不存在或已禁用的用户。 */
    void sendToUsers(NotificationSendDTO dto);

    /** 向所有启用状态的用户广播通知。 */
    void broadcastToAll(NotificationBroadcastDTO dto);
}
