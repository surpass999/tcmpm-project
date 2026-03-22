package cn.gemrun.base.module.declare.controller.admin.review.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 评审汇总（按流程阶段分组）
 *
 * @author Gemini
 */
@Schema(description = "管理后台 - 评审汇总")
@Data
public class ReviewSummaryRespVO {

    @Schema(description = "流程类型")
    private Integer processType;

    @Schema(description = "流程类型名称")
    private String processTypeName;

    @Schema(description = "专家数量")
    private Integer expertCount;

    @Schema(description = "平均分")
    private java.math.BigDecimal averageScore;

    @Schema(description = "通过状态")
    private String passStatus;

    @Schema(description = "评审时间")
    private String reviewTime;

    @Schema(description = "评审结果列表")
    private List<ReviewResultRespVO> results;

}
