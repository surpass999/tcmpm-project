package cn.gemrun.base.module.declare.dal.mysql.review;

import cn.gemrun.base.framework.mybatis.core.mapper.BaseMapperX;
import cn.gemrun.base.module.declare.dal.dataobject.review.ReviewIndicatorScoreDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 评审指标评分 Mapper
 *
 * @author Gemini
 */
@Mapper
public interface ReviewIndicatorScoreMapper extends BaseMapperX<ReviewIndicatorScoreDO> {

    /**
     * 根据评审结果ID查询评分列表
     */
    @Select("SELECT * FROM declare_review_indicator_score WHERE review_result_id = #{reviewResultId} AND deleted = 0")
    List<ReviewIndicatorScoreDO> selectListByReviewResultId(@Param("reviewResultId") Long reviewResultId);

    /**
     * 根据专家ID查询评分列表
     */
    @Select("SELECT * FROM declare_review_indicator_score WHERE expert_id = #{expertId} AND deleted = 0")
    List<ReviewIndicatorScoreDO> selectListByExpertId(@Param("expertId") Long expertId);

    /**
     * 根据评审结果ID删除评分
     */
    @Update("UPDATE declare_review_indicator_score SET deleted = b'1' WHERE review_result_id = #{reviewResultId}")
    void deleteByReviewResultId(@Param("reviewResultId") Long reviewResultId);

}
