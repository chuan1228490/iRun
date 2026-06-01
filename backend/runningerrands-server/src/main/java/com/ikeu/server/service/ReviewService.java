package com.ikeu.server.service;

import com.ikeu.model.dto.ReviewCreateDTO;
import com.ikeu.model.dto.ReviewUpdateDTO;
import com.ikeu.model.vo.ReviewVO;
import java.util.List;

/**
 * 评价服务接口，提供评价创建、修改、删除、查看和追加评价等功能。
 * @author ikeu
 * @since 2026/05/14
 */
public interface ReviewService {

    /** 创建评价，校验订单已完成且未重复评价。 */
    void createReview(Long reviewerId, ReviewCreateDTO reviewCreateDTODTO);

    /** 修改评价，仅本人可修改且未超修改时限。 */
    void updateReview(Long reviewerId, Long reviewId, ReviewUpdateDTO reviewUpdateDTO);

    /** 删除评价，仅本人可删除。 */
    void deleteReview(Long reviewerId, Long reviewId);

    /** 查看指定用户收到的所有评价（含追评）。 */
    List<ReviewVO> listUserReviews(Long targetUserId);

    /** 查看指定任务关联的所有评价。 */
    List<ReviewVO> getTaskReviews(Long taskId);

    /** 对已有评价进行追加评论，需为评价目标用户且未重复追加。 */
    void createFollowUp(Long reviewerId, Long parentReviewId, String content);
}
