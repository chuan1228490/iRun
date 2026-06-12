package com.ikeu.server.service;

import com.ikeu.common.result.PageResult;
import com.ikeu.model.vo.OrderDetailVO;
import com.ikeu.model.vo.OrderManageVO;

/**
 * 管理端订单管理服务接口，提供订单列表、详情和强制状态更新功能。
 * @author ikeu
 * @since 2026/06/11
 */
public interface AdminOrderService {

    /** 分页查询所有订单列表，支持按状态筛选。 */
    PageResult<OrderManageVO> listAllOrders(Integer status, int page, int size);

    /** 根据订单ID查询订单详情。 */
    OrderDetailVO getOrderDetail(Long orderId);

    /** 强制更新订单状态（状态机校验）。 */
    void updateOrderStatus(Long orderId, Integer status);
}
