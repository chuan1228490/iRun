import request from '@/utils/request'

export function listOrders(params: { status?: number; page?: number; size?: number }) {
  return request({ url: '/admin/orders', method: 'get', params })
}
