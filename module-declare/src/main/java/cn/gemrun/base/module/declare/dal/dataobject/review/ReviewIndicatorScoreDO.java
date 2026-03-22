package cn.gemrun.base.module.declare.dal.dataobject.review;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;

import java.math.BigDecimal;

/**
 * 评审指标评分 DO
 * 结构化存储每位专家对各指标的评分
 *
 * @author Gemini
 */
@TableName("declare_review_indicator_score")
@KeySequence("declare_review_indicator_score_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewIndicatorScoreDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;

    /**
     * 关联评审结果ID（declare_review_result.id）
     */
    private Long reviewResultId;

    /**
     * 指标ID（关联 declare_indicator.id）
     */
    private Long indicatorId;

    /**
     * 指标代号
     */
    private String indicatorCode;

    /**
     * 专家ID（关联 declare_expert.id）
     */
    private Long expertId;

    /**
     * 该指标满分（如30表示30分）
     */
    private BigDecimal maxScore;

    /**
     * 实际评分
     */
    private BigDecimal score;

    /**
     * 评分等级（满足/基本满足/部分满足/不满足）
     */
    private String scoreLevel;

    /**
     * 评分比例（如1.0表示100%，0.75表示75%）
     */
    private BigDecimal scoreRatio;

    /**
     * 评分说明
     */
    private String comment;

    /**
     * 评审意见（与该指标关联）
     */
    private String opinion;

    /**
     * 删除原因
     */
    private String deleteReason;

}
