/**
 * 通知模块 API
 */
import { get, put, del } from '@/utils/request'

/**
 * 通知列表
 * @param {object} params - isRead(0/1), page, size
 */
export function getNotifications(params = {}, opts = {}) {
  return get('/notification', params, opts)
}

/** 标记单条已读 */
export function markRead(notificationId) {
  return put(`/notification/${notificationId}/read`)
}

/** 全部已读 */
export function markAllRead() {
  return put('/notification/read-all')
}

/** 删除通知 */
export function deleteNotification(notificationId) {
  return del(`/notification/${notificationId}`)
}
