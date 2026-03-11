package cn.gemrun.base.module.declare.controller.admin.filing.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import javax.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;

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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validStartTime;

    @Schema(description = "有效期限结束时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "有效期限结束时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validEndTime;

    @Schema(description = "建设内容（备案方案核心）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "建设内容（备案方案核心）不能为空")
    private String constructionContent;

    @Schema(description = "备案状态：对应流程 DSL bizStatus，如 DRAFT/SUBMITTED/PROVINCE_APPROVED/EXPERT_APPROVED/NATION_APPROVED/ARCHIVED/RETURNED", requiredMode = Schema.RequiredMode.REQUIRED, example = "SUBMITTED")
    @NotNull(message = "备案状态不能为空")
    private String filingStatus;

    @Schema(description = "省级审核意见")
    private String provinceReviewOpinion;

    @Schema(description = "省级审核时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime provinceReviewTime;

    @Schema(description = "省级审核人ID（关联system_users.id）", example = "11954")
    private Long provinceReviewerId;

    @Schema(description = "专家论证意见")
    private String expertReviewOpinion;

    @Schema(description = "论证专家ID集合（逗号分隔，关联declare_expert.id）")
    private String expertReviewerIds;

    @Schema(description = "备案归档时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime filingArchiveTime;

    /**
     * 指标值列表（新增备案时包含指标值）
     */
    @Schema(description = "指标值列表")
    private List<IndicatorValueItem> indicatorValues;

    @Schema(description = "单个指标值")
    @Data
    public static class IndicatorValueItem {

        @Schema(description = "指标ID")
        @NotNull(message = "指标ID不能为空")
        private Long indicatorId;

        @Schema(description = "指标代号")
        @NotNull(message = "指标代号不能为空")
        private String indicatorCode;

        @Schema(description = "值类型：1=数字，2=字符串，3=布尔，4=日期，5=长文本，6=单选，7=多选，8=日期区间")
        @NotNull(message = "值类型不能为空")
        private Integer valueType;

        @Schema(description = "数字型值")
        private String valueNum;

        @Schema(description = "字符串型值（单选/多选值以逗号分隔）")
        private String valueStr;

        @Schema(description = "布尔型值")
        private Boolean valueBool;

        @Schema(description = "日期型值（格式：yyyy-MM-dd HH:mm:ss）")
        private String valueDate;

        @Schema(description = "日期区间-开始")
        private String valueDateStart;

        @Schema(description = "日期区间-结束")
        private String valueDateEnd;

        @Schema(description = "长文本型值")
        private String valueText;

    }

}