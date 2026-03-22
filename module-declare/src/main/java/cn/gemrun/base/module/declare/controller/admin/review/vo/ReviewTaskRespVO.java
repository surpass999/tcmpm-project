package cn.gemrun.base.module.declare.controller.admin.review.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - 评审任务详情 Response VO")
@Data
public class ReviewTaskRespVO {

    @Schema(description = "任务主键（自增）", example = "1")
    private Long id;

    @Schema(description = "流程实例ID", example = "abc123")
    private String processInstanceId;

    @Schema(description = "节点Key", example = "expert_review")
    private String taskDefinitionKey;

    @Schema(description = "任务类型：1=备案论证，2=中期评估，3=验收评审，4=成果审核", example = "2")
    private Integer taskType;

    @Schema(description = "业务类型：1=备案，2=项目，3=成果", example = "2")
    private Integer businessType;

    @Schema(description = "关联业务ID", example = "1")
    private Long businessId;

    @Schema(description = "任务名称", example = "XX项目中期评估评审")
    private String taskName;

    @Schema(description = "任务开始时间")
    private LocalDateTime startTime;

    @Schema(description = "任务截止时间")
    private LocalDateTime endTime;

    @Schema(description = "参与专家ID集合（逗号分隔）", example = "1,2,3")
    private String expertIds;

    @Schema(description = "评审标准JSON")
    private String reviewStandard;

    @Schema(description = "评审要求", example = "重点评估四大模块功能落地情况")
    private String reviewRequirement;

    @Schema(description = "综合评分", example = "85.50")
    private BigDecimal totalScore;

    @Schema(description = "评审结论：通过/需整改/未通过", example = "通过")
    private String reviewConclusion;

    @Schema(description = "任务状态：0=待分配，1=评审中，2=已完成", example = "1")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    // 扩展字段
    @Schema(description = "专家列表")
    private List<ExpertSimpleVO> experts;

    @Schema(description = "业务名称（如项目名称）")
    private String businessName;

    @Schema(description = "业务所属单位")
    private String businessUnitName;

    @Schema(description = "业务所属省份")
    private String businessProvinceName;

    @Schema(description = "评审结果列表")
    private List<ReviewResultRespVO> results;

    @Data
    public static class ExpertSimpleVO {
        private Long id;
        private String expertName;
        private String workUnit;
        private Integer status;
    }

}
