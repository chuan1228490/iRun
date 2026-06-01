export const ADMIN_ROLES: Record<number, string> = {
  1: '超级管理员',
  2: '普通管理员'
}

export const USER_STATUS: Record<number, string> = {
  0: '禁用',
  1: '正常'
}

export const CERTIFY_STATUS: Record<number, string> = {
  0: '未认证',
  1: '审核中',
  2: '已认证',
  3: '认证驳回'
}

export const TASK_STATUS: Record<number, string> = {
  1: '待接单',
  2: '已接单',
  3: '配送中',
  4: '待确认',
  5: '已完成',
  6: '已取消'
}

export const ORDER_STATUS: Record<number, string> = {
  1: '待取货',
  2: '配送中',
  3: '待确认',
  4: '已完成',
  5: '已取消'
}

export const TRANSACTION_TYPES: Record<number, string> = {
  1: '任务悬赏支出',
  2: '跑腿收入',
  3: '充值',
  4: '提现',
  5: '退款'
}

export const NOTIFICATION_TYPES: Record<number, string> = {
  1: '系统通知',
  2: '订单状态',
  3: '活动提醒'
}
