package cn.gemrun.base.module.declare.controller.admin.review.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

import static cn.gemrun.base.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE;

@Schema(description = "管理后台 - 评审任务创建/更新 Request VO")
@Data
public class ReviewTaskSaveReqVO {

    @Schema(description = "任务主键（自增）", example = "1")
    private Long id;

    @Schema(description = "流程实例ID", example = "abc123")
    private String processInstanceId;

    @Schema(description = "节点Key", example = "expert_review")
    private String taskDefinitionKey;

    @Schema(description = "任务类型：1=备案论证，2=中期评估，3=验收评审，4=成果审核", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "任务类型不能为空")
    private Integer taskType;

    @Schema(description = "业务类型：1=备案，2=项目，3=成果", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "业务类型不能为空")
    private Integer businessType;

    @Schema(description = "关联业务ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "业务ID不能为空")
    private Long businessId;

    @Schema(description = "任务名称", example = "XX项目中期评估评审", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "任务名称不能为空")
    private String taskName;

    @Schema(description = "任务开始时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE)
    private LocalDateTime startTime;

    @Schema(description = "任务截止时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE)
    private LocalDateTime endTime;

    @Schema(description = "参与专家ID集合（逗号分隔）", example = "1,2,3", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "专家不能为空")
    private String expertIds;

    @Schema(description = "评审标准JSON")
    private String reviewStandard;

    @Schema(description = "评审要求", example = "重点评估四大模块功能落地情况及资金执行合规性")
    private String reviewRequirement;

}
