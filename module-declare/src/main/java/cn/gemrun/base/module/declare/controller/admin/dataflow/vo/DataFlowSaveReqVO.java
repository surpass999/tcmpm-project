package cn.gemrun.base.module.declare.controller.admin.dataflow.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import javax.validation.constraints.*;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 数据流通记录新增/修改 Request VO")
@Data
public class DataFlowSaveReqVO {

    @Schema(description = "主键（自增）", example = "1")
    private Long id;

    @Schema(description = "流程实例ID")
    private String processInstanceId;

    @Schema(description = "关联项目ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "关联项目不能为空")
    private Long projectId;

    @Schema(description = "数据名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "患者诊疗数据")
    @NotEmpty(message = "数据名称不能为空")
    private String dataName;

    @Schema(description = "数据描述")
    private String dataDescription;

    @Schema(description = "数据类型（如：患者数据、诊疗数据）", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "数据类型不能为空")
    private String dataType;

    @Schema(description = "数据来源")
    private String dataSource;

    @Schema(description = "数据量规模（如：100万条、50GB）")
    private String dataVolume;

    @Schema(description = "数据质量评级：1=优，2=良，3=中，4=差")
    private Integer dataQuality;

    @Schema(description = "共享范围：1=院内，2=省级，3=全国")
    private Integer shareScope;

    @Schema(description = "流通开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "流通结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    @Schema(description = "流通类型：1=内部使用，2=对外共享，3=交易", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "流通类型不能为空")
    private Integer flowType;

    @Schema(description = "流通对象")
    private String flowObject;

    @Schema(description = "流通目的")
    private String flowPurpose;

    @Schema(description = "安全备案编号")
    private String securityFilingNo;

    @Schema(description = "安全备案时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime securityFilingTime;

    @Schema(description = "取得数据产品证书数（关联指标601）", example = "1")
    private Integer certificateCount;

    @Schema(description = "取得数据产权登记证数（关联指标602）", example = "1")
    private Integer propertyCount;

    @Schema(description = "状态：0=草稿，1=已提交，2=审核中，3=已通过，4=退回", example = "0")
    private Integer status;

    @Schema(description = "审核意见")
    private String auditOpinion;

    @Schema(description = "审核人ID")
    private Long auditorId;

    @Schema(description = "审核时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime auditTime;

    @Schema(description = "是否已生成成果展示：0=否，1=是")
    private Boolean hasAchievement;

}
