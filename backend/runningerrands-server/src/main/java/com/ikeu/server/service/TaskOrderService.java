package com.ikeu.server.service;

import com.ikeu.common.result.PageResult;
import com.ikeu.model.dto.CancelOrderDTO;
import com.ikeu.model.dto.ProofImageDTO;
import com.ikeu.model.vo.OrderDetailVO;
import com.ikeu.model.vo.OrderListVO;

/**
 * 订单服务接口，提供接单、取货、送达、确认完成、详情查询和删除等订单流程操作。
 * @author ikeu
 * @since 2026/05/14
 */
public interface TaskOrderService {

    /** 配送员接单，使用 Redis 分布式锁防止并发抢单。 */
    void acceptOrder(Long runnerId, Long taskId);

    /** 配送员确认取货，上传取货凭证并更新订单状态。 */
    void confirmPickup(Long runnerId, Long orderId, ProofImageDTO proof);

    /** 配送员确认送达，上传送达凭证并更新订单状态。 */
    void confirmDeliver(Long runnerId, Long orderId, ProofImageDTO proof);

    /** 发布者确认订单完成，触发报酬结算和信用分更新。 */
    void confirmComplete(Long publisherId, Long orderId);

    /** 通过订单ID查询订单详情，校验查看权限。 */
    OrderDetailVO getOrderDetailByOrderId(Long orderId, Long currentUserId);

    /** 通过任务ID查询关联订单详情，校验查看权限。 */
    OrderDetailVO getOrderDetailByTaskId(Long taskId, Long currentUserId);

    /** 配送员查看已接订单列表，支持按状态筛选。 */
    PageResult<OrderListVO> listMyAcceptOrders(Long userId, Integer status, int page, int size);

    /** 配送员接单后5分钟内取消订单，关联任务回退至待接单状态。 */
    void cancelOrder(Long runnerId, Long orderId, CancelOrderDTO dto);

    /** 发布者或配送员软删除已完成超过7天的订单。 */
    void deleteOrder(Long userId, Long orderId);
}
