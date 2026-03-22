package cn.gemrun.base.module.declare.controller.admin.review.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 评审结果 Response VO")
@Data
public class ReviewResultRespVO {

    @Schema(description = "结果主键（自增）", example = "1")
    private Long id;

    @Schema(description = "关联评审任务ID", example = "1")
    private Long taskId;

    @Schema(description = "流程实例ID", example = "abc123")
    private String processInstanceId;

    @Schema(description = "Flowable任务ID", example = "task123")
    private String flowableTaskId;

    @Schema(description = "评审专家ID", example = "1")
    private Long expertId;

    @Schema(description = "业务类型：1=备案，2=项目，3=成果", example = "2")
    private Integer businessType;

    @Schema(description = "关联业务ID", example = "1")
    private Long businessId;

    @Schema(description = "评审状态：0=待评审，1=已接收，2=评审中，3=已提交，4=超时", example = "3")
    private Integer status;

    @Schema(description = "是否存在利益冲突", example = "false")
    private Boolean isConflict;

    @Schema(description = "冲突检测结果")
    private String conflictCheckResult;

    @Schema(description = "是否申请回避", example = "false")
    private Boolean isAvoid;

    @Schema(description = "回避原因")
    private String avoidReason;

    @Schema(description = "评分数据JSON")
    private String scoreData;

    @Schema(description = "总分", example = "85.50")
    private BigDecimal totalScore;

    @Schema(description = "评审结论：通过/需整改/未通过", example = "通过")
    private String conclusion;

    @Schema(description = "评审意见")
    private String opinion;

    @Schema(description = "接收任务时间")
    private LocalDateTime receiveTime;

    @Schema(description = "提交评审时间")
    private LocalDateTime submitTime;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    // 扩展字段
    @Schema(description = "专家姓名")
    private String expertName;

    @Schema(description = "专家工作单位")
    private String expertWorkUnit;

    @Schema(description = "专家联系电话")
    private String expertPhone;

    // ========== 任务相关扩展字段 ==========
    @Schema(description = "任务类型：1=备案论证，2=中期评估，3=验收评审，4=成果审核")
    private Integer taskType;

    @Schema(description = "业务名称（项目名称/备案名称/成果名称）")
    private String businessName;

    @Schema(description = "申报单位")
    private String declareUnit;

    @Schema(description = "项目编号")
    private String projectCode;

    @Schema(description = "结构化指标评分列表")
    private List<IndicatorScore> indicatorScores;

    @Schema(description = "指标评分（结构化）")
    @Data
    public static class IndicatorScore {
        @Schema(description = "指标ID")
        private Long indicatorId;

        @Schema(description = "指标编码")
        private String indicatorCode;

        @Schema(description = "指标名称")
        private String indicatorName;

        @Schema(description = "满分")
        private BigDecimal maxScore;

        @Schema(description = "得分")
        private BigDecimal score;

        @Schema(description = "评分等级：satisfied=满足，basic=基本满足，partial=部分满足，unsatisfied=不满足")
        private String scoreLevel;

        @Schema(description = "得分比例")
        private BigDecimal scoreRatio;

        @Schema(description = "评语")
        private String comment;

        @Schema(description = "意见")
        private String opinion;
    }

}
