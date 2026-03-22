package cn.gemrun.base.module.declare.service.review.impl;

import cn.gemrun.base.module.declare.dal.dataobject.review.ReviewIndicatorScoreDO;
import cn.gemrun.base.module.declare.dal.mysql.review.ReviewIndicatorScoreMapper;
import cn.gemrun.base.module.declare.service.review.ReviewIndicatorScoreService;
import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;
import cn.gemrun.base.framework.security.core.util.SecurityFrameworkUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.List;

/**
 * 评审指标评分 Service 实现
 *
 * @author Gemini
 */
@Service
@RequiredArgsConstructor
public class ReviewIndicatorScoreServiceImpl implements ReviewIndicatorScoreService {

    private final ReviewIndicatorScoreMapper reviewIndicatorScoreMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveScores(Long reviewResultId, List<ReviewIndicatorScoreDO> scores) {
        if (CollectionUtils.isEmpty(scores)) {
            return;
        }
        // 先删除原有评分
        deleteByReviewResultId(reviewResultId);

        // 设置评审结果ID
        String username = getCurrentUsername();
        for (ReviewIndicatorScoreDO score : scores) {
            score.setReviewResultId(reviewResultId);
            score.setCreator(username);
            score.setUpdater(username);
        }

        // 批量插入
        reviewIndicatorScoreMapper.insertBatch(scores);
    }

    @Override
    public List<ReviewIndicatorScoreDO> getScoresByReviewResultId(Long reviewResultId) {
        return reviewIndicatorScoreMapper.selectListByReviewResultId(reviewResultId);
    }

    @Override
    public List<ReviewIndicatorScoreDO> getScoresByExpertId(Long expertId) {
        return reviewIndicatorScoreMapper.selectListByExpertId(expertId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByReviewResultId(Long reviewResultId) {
        reviewIndicatorScoreMapper.deleteByReviewResultId(reviewResultId);
    }

    private String getCurrentUsername() {
        try {
            return SecurityFrameworkUtils.getLoginUserNickname();
        } catch (Exception e) {
            return "system";
        }
    }

}
