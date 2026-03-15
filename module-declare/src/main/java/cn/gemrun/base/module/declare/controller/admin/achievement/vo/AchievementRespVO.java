package cn.gemrun.base.module.declare.controller.admin.achievement.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 成果信息 Response VO")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AchievementRespVO {

    @Schema(description = "主键（自增）", example = "1")
    private Long id;

    @Schema(description = "流程实例ID")
    private String processInstanceId;

    @Schema(description = "关联项目ID", example = "1")
    private Long projectId;

    @Schema(description = "项目名称")
    private String projectName;

    @Schema(description = "成果名称", example = "中医智能审方系统")
    private String achievementName;

    @Schema(description = "成果类型：1=系统功能，2=数据集，3=科研成果，4=管理经验", example = "1")
    private Integer achievementType;

    @Schema(description = "应用领域", example = "中医诊疗")
    private String applicationField;

    @Schema(description = "成果描述")
    private String description;

    @Schema(description = "应用效果描述")
    private String effectDescription;

    @Schema(description = "可复制性评估：1=高，2=中，3=低")
    private Integer replicationValue;

    @Schema(description = "推广范围：1=院内，2=省级，3=全国")
    private Integer promotionScope;

    @Schema(description = "推广次数")
    private Integer promotionCount;

    @Schema(description = "转化类型：1=标准规范，2=创新模式，3=典型案例")
    private Integer transformType;

    @Schema(description = "审核状态：0=待审核，1=省级通过/待国家局审核，2=国家局审核中，3=已认定推广，4=退回", example = "0")
    private Integer auditStatus;

    @Schema(description = "审核意见")
    private String auditOpinion;

    @Schema(description = "审核人ID")
    private Long auditorId;

    @Schema(description = "审核时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;

    @Schema(description = "推荐状态：0=未推荐，1=已推荐至国家局，2=已纳入推广库", example = "0")
    private Integer recommendStatus;

    @Schema(description = "推荐意见")
    private String recommendOpinion;

    @Schema(description = "推荐人ID")
    private Long recommenderId;

    @Schema(description = "推荐时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recommendTime;

    @Schema(description = "创建时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    @Schema(description = "创建者")
    private Long creator;

}
