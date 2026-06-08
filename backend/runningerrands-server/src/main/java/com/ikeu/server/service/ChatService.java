package com.ikeu.server.service;

import com.ikeu.model.vo.ChatVO;
import com.ikeu.model.vo.ContactVO;

import java.util.List;

/**
 * 聊天服务接口，提供消息收发、聊天记录查询、联系人管理和消息操作等功能。
 * @author ikeu
 * @since 2026/05/14
 */
public interface ChatService {

    /** 发送消息并持久化，通过 WebSocket 推送给接收者。 */
    void sendMessage(Long senderId, Long receiverId, String content, Integer messageType);

    /** 分页查询与指定用户的聊天记录，按时间正序排列。 */
    List<ChatVO> getHistory(Long userId, Long targetUserId, int page, int size);

    /** 获取联系人列表，包含最近消息摘要和未读计数。 */
    List<ContactVO> getContacts(Long userId);

    /** 标记某发送者的所有未读消息为已读。 */
    void markRead(Long userId, Long senderId);

    /** 软删除消息，仅发送者可操作。 */
    void deleteMessage(Long userId, Long messageId);

    /** 撤回消息（5分钟内），内容替换为撤回占位文本。 */
    void recallMessage(Long userId, Long messageId);
}
