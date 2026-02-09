package cn.gemrun.base.module.declare.controller.admin.filing.vo;

import lombok.*;
import java.util.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.gemrun.base.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

import static cn.gemrun.base.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 项目备案核心信息分页 Request VO")
@Data
public class FilingPageReqVO extends PageParam {

    @Schema(description = "统一社会信用代码")
    private String socialCreditCode;

    @Schema(description = "医疗机构执业许可证号")
    private String medicalLicenseNo;

    @Schema(description = "机构名称", example = "赵六")
    private String orgName;

    @Schema(description = "项目类型：1=综合型，2=中医电子病历型，3=智慧中药房型，4=名老中医传承型，5=中医临床科研型，6=中医智慧医共体型", example = "1")
    private Integer projectType;

    @Schema(description = "有效期限开始时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] validStartTime;

    @Schema(description = "有效期限结束时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] validEndTime;

    @Schema(description = "建设内容（备案方案核心）")
    private String constructionContent;

    @Schema(description = "备案状态：0=草稿，1=已提交，2=省级审核通过，3=专家论证通过，4=已归档，5=退回修改", example = "1")
    private Integer filingStatus;

    @Schema(description = "省级审核意见")
    private String provinceReviewOpinion;

    @Schema(description = "省级审核时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] provinceReviewTime;

    @Schema(description = "省级审核人ID（关联system_users.id）", example = "11954")
    private Long provinceReviewerId;

    @Schema(description = "专家论证意见")
    private String expertReviewOpinion;

    @Schema(description = "论证专家ID集合（逗号分隔，关联declare_expert.id）")
    private String expertReviewerIds;

    @Schema(description = "备案归档时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] filingArchiveTime;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}