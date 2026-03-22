package cn.gemrun.base.module.declare.controller.admin.review.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 评审结果创建/更新 Request VO")
@Data
public class ReviewResultSaveReqVO {

    @Schema(description = "结果主键（自增）", example = "1")
    private Long id;

    @Schema(description = "关联评审任务ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "任务ID不能为空")
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

    @Schema(description = "评分数据JSON（各指标评分详情）")
    private String scoreData;

    @Schema(description = "指标评分列表（结构化存储）")
    private List<IndicatorScore> indicatorScores;

    @Schema(description = "总分", example = "85.50")
    private BigDecimal totalScore;

    @Schema(description = "评审结论：通过/需整改/未通过", example = "通过")
    private String conclusion;

    @Schema(description = "评审意见")
    private String opinion;

    @Schema(description = "按钮ID（用于从BPMN模型获取bizStatus）", example = "1")
    private Integer buttonId;

    /**
     * 指标评分
     */
    @Data
    public static class IndicatorScore {

        @Schema(description = "指标ID")
        private Long indicatorId;

        @Schema(description = "指标代号")
        private String indicatorCode;

        @Schema(description = "满分")
        private BigDecimal maxScore;

        @Schema(description = "实际评分")
        private BigDecimal score;

        @Schema(description = "评分等级（满足/基本满足/部分满足/不满足）")
        private String scoreLevel;

        @Schema(description = "评分比例")
        private BigDecimal scoreRatio;

        @Schema(description = "评分说明")
        private String comment;

        @Schema(description = "评审意见")
        private String opinion;
    }

}
