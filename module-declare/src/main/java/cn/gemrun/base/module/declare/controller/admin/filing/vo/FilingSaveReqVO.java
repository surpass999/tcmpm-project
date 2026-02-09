package cn.gemrun.base.module.declare.controller.admin.filing.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.util.*;
import javax.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 项目备案核心信息新增/修改 Request VO")
@Data
public class FilingSaveReqVO {

    @Schema(description = "备案主键（自增）", requiredMode = Schema.RequiredMode.REQUIRED, example = "13238")
    private Long id;

    @Schema(description = "统一社会信用代码", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "统一社会信用代码不能为空")
    private String socialCreditCode;

    @Schema(description = "医疗机构执业许可证号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "医疗机构执业许可证号不能为空")
    private String medicalLicenseNo;

    @Schema(description = "机构名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "赵六")
    @NotEmpty(message = "机构名称不能为空")
    private String orgName;

    @Schema(description = "项目类型：1=综合型，2=中医电子病历型，3=智慧中药房型，4=名老中医传承型，5=中医临床科研型，6=中医智慧医共体型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "项目类型：1=综合型，2=中医电子病历型，3=智慧中药房型，4=名老中医传承型，5=中医临床科研型，6=中医智慧医共体型不能为空")
    private Integer projectType;

    @Schema(description = "有效期限开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "有效期限开始时间不能为空")
    private LocalDateTime validStartTime;

    @Schema(description = "有效期限结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "有效期限结束时间不能为空")
    private LocalDateTime validEndTime;

    @Schema(description = "建设内容（备案方案核心）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "建设内容（备案方案核心）不能为空")
    private String constructionContent;

    @Schema(description = "备案状态：0=草稿，1=已提交，2=省级审核通过，3=专家论证通过，4=已归档，5=退回修改", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "备案状态：0=草稿，1=已提交，2=省级审核通过，3=专家论证通过，4=已归档，5=退回修改不能为空")
    private Integer filingStatus;

    @Schema(description = "省级审核意见")
    private String provinceReviewOpinion;

    @Schema(description = "省级审核时间")
    private LocalDateTime provinceReviewTime;

    @Schema(description = "省级审核人ID（关联system_users.id）", example = "11954")
    private Long provinceReviewerId;

    @Schema(description = "专家论证意见")
    private String expertReviewOpinion;

    @Schema(description = "论证专家ID集合（逗号分隔，关联declare_expert.id）")
    private String expertReviewerIds;

    @Schema(description = "备案归档时间")
    private LocalDateTime filingArchiveTime;

}