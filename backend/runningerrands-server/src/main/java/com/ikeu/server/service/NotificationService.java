package com.ikeu.server.service;

import com.ikeu.model.vo.NotificationVO;
import com.ikeu.common.result.PageResult;

/**
 * 通知服务接口，提供通知发送、列表查询、已读标记、删除和过期清理等功能。
 * @author ikeu
 * @since 2026/05/14
 */
public interface NotificationService {

    /** 发送通知并持久化，同时通过 WebSocket 推送。 */
    void sendNotification(Long userId, Integer type, String title, String content, Long targetId);

    /** 分页查询当前用户的通知列表，支持按已读状态筛选。 */
    PageResult<NotificationVO> listNotifications(Long userId, Integer isRead, int page, int size);

    /** 标记单条通知为已读，校验通知归属当前用户。 */
    void markAsRead(Long userId, Long notificationId);

    /** 将当前用户的所有通知标记为已读。 */
    void markAllAsRead(Long userId);

    /** 删除指定通知，校验通知归属当前用户。 */
    void deleteNotification(Long userId, Long notificationId);

    /** 分批清理过期通知，返回当次删除行数。 */
    int cleanupExpiredNotifications(int batchSize);

}
