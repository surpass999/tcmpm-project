package cn.gemrun.base.module.declare.controller.admin.achievement.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 成果与流通新增/修改 Request VO")
@Data
public class AchievementSaveReqVO {

    @Schema(description = "主键（自增）", example = "1")
    private Long id;

    @Schema(description = "流程实例ID")
    private String processInstanceId;

    @Schema(description = "关联项目ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "关联项目不能为空")
    private Long projectId;

    @Schema(description = "部门ID")
    private Long deptId;

    // ==================== 成果基本信息 ====================

    @Schema(description = "成果名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "中医智能审方系统")
    @NotEmpty(message = "成果名称不能为空")
    private String achievementName;

    @Schema(description = "成果类型：1=系统功能，2=数据集，3=科研成果，4=管理经验", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "成果类型不能为空")
    private Integer achievementType;

    @Schema(description = "应用领域", requiredMode = Schema.RequiredMode.REQUIRED, example = "中医诊疗")
    @NotEmpty(message = "应用领域不能为空")
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

    // ==================== 数据流通信息（从 data_flow 合并） ====================

    @Schema(description = "数据名称")
    private String dataName;

    @Schema(description = "数据描述")
    private String dataDescription;

    @Schema(description = "数据类型")
    private String dataType;

    @Schema(description = "数据来源")
    private String dataSource;

    @Schema(description = "数据量规模")
    private String dataVolume;

    @Schema(description = "数据质量评级：1=优，2=良，3=中，4=差")
    private Integer dataQuality;

    @Schema(description = "共享范围：1=院内，2=省级，3=全国")
    private Integer shareScope;

    @Schema(description = "流通类型：1=内部使用，2=对外共享，3=交易")
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

    @Schema(description = "流通开始时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;

    @Schema(description = "流通结束时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;

    // ==================== 证书信息（对应指标 601, 602） ====================

    @Schema(description = "数据产品证书数（指标601）")
    private Integer certificateCount;

    @Schema(description = "数据产权登记证数（指标602）")
    private Integer propertyCount;

    // ==================== 交易信息（对应指标 603, 604） ====================

    @Schema(description = "完成交易的数据产品数（指标603）")
    private Integer transactionCount;

    @Schema(description = "累计交易金额（万元，指标604）")
    private BigDecimal transactionAmount;

    @Schema(description = "交易对象")
    private String transactionObject;

    @Schema(description = "交易完成时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime transactionTime;

    @Schema(description = "交易合同路径")
    private String transactionContract;

    // ==================== 附件和状态 ====================

    @Schema(description = "附件ID集合")
    private String attachmentIds;

    @Schema(description = "成果状态：DRAFT=草稿，SUBMITTED=已提交，AUDITING=审核中，APPROVED=已通过，REJECTED=退回")
    private String status;

    @Schema(description = "审核意见")
    private String auditOpinion;

    @Schema(description = "审核状态：0=待审核，1=省级通过/待国家局审核，2=国家局审核中，3=已认定推广，4=退回", example = "0")
    private Integer auditStatus;

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

}
