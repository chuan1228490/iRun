/**
 * 资金模块 API
 */
import { get, post } from '@/utils/request'

/**
 * 交易流水查询
 * @param {object} params - type(1-5), start, end, page, size
 */
export function getTransactions(params = {}) {
  return get('/user/transaction', params)
}

/** 充值（需支付密码校验） */
export function recharge(amount, payPassword) {
  return post('/user/transaction/recharge', { amount, payPassword })
}

/** 提现（需支付密码校验） */
export function withdraw(amount, payPassword) {
  return post('/user/transaction/withdraw', { amount, payPassword })
}
