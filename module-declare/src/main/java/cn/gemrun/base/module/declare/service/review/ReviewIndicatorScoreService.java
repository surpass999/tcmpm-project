package cn.gemrun.base.module.declare.service.review;

import cn.gemrun.base.module.declare.dal.dataobject.review.ReviewIndicatorScoreDO;

import java.util.List;

/**
 * 评审指标评分 Service 接口
 *
 * @author Gemini
 */
public interface ReviewIndicatorScoreService {

    /**
     * 批量保存评分数据
     *
     * @param reviewResultId 评审结果ID
     * @param scores 评分列表
     */
    void saveScores(Long reviewResultId, List<ReviewIndicatorScoreDO> scores);

    /**
     * 根据评审结果ID获取评分列表
     *
     * @param reviewResultId 评审结果ID
     * @return 评分列表
     */
    List<ReviewIndicatorScoreDO> getScoresByReviewResultId(Long reviewResultId);

    /**
     * 根据专家ID获取评分列表
     *
     * @param expertId 专家ID
     * @return 评分列表
     */
    List<ReviewIndicatorScoreDO> getScoresByExpertId(Long expertId);

    /**
     * 根据评审结果ID删除评分
     *
     * @param reviewResultId 评审结果ID
     */
    void deleteByReviewResultId(Long reviewResultId);

}
