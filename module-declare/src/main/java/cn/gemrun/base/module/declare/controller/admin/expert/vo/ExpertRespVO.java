package cn.gemrun.base.module.declare.controller.admin.expert.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

import static cn.gemrun.base.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 专家信息 Response VO")
@Data
public class ExpertRespVO {

    @Schema(description = "主键（自增）", example = "1")
    private Long id;

    @Schema(description = "关联系统用户ID（system_users.id）", example = "1")
    private Long userId;

    @Schema(description = "专家姓名", example = "张医生")
    private String expertName;

    @Schema(description = "身份证号", example = "110101199001011234")
    private String idCard;

    @Schema(description = "性别：1=男，2=女", example = "1")
    private Integer gender;

    @Schema(description = "出生日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate birthDate;

    @Schema(description = "联系电话", example = "13800138000")
    private String phone;

    @Schema(description = "电子邮箱", example = "expert@example.com")
    private String email;

    @Schema(description = "工作单位", example = "北京某医院")
    private String workUnit;

    @Schema(description = "职务", example = "主任医师")
    private String jobTitle;

    @Schema(description = "职称", example = "正高级")
    private String professionalTitle;

    @Schema(description = "所在部门/科室", example = "心内科")
    private String department;

    @Schema(description = "专家类型：1=技术专家，2=财务专家，3=管理专家，4=行业专家", example = "1")
    private Integer expertType;

    @Schema(description = "专业领域（逗号分隔，如：中医信息化、医疗大数据）", example = "中医信息化,医疗大数据")
    private String specialties;

    @Schema(description = "擅长方向", example = "中医信息化")
    private String expertiseAreas;

    @Schema(description = "学历", example = "博士")
    private String educationBackground;

    @Schema(description = "学位", example = "博士")
    private String degree;

    @Schema(description = "毕业院校", example = "北京中医药大学")
    private String graduatedFrom;

    @Schema(description = "资格证书编号", example = "CERT001")
    private String qualificationCert;

    @Schema(description = "资质证书附件ID（infra_file.id）", example = "1")
    private Long certAttachId;

    @Schema(description = "累计评审次数", example = "10")
    private Integer reviewCount;

    @Schema(description = "上次评审时间")
    private LocalDateTime lastReviewTime;

    @Schema(description = "平均评审评分", example = "4.85")
    private BigDecimal reviewScore;

    @Schema(description = "状态：1=在册，2=暂停，3=注销", example = "1")
    private Integer status;

    @Schema(description = "状态说明（如暂停原因）", example = "因个人原因暂停")
    private String statusRemark;

    @Schema(description = "备注", example = "备注信息")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "是否需要回避：true=是，false=否（当传入currentDeptId时有效）")
    private Boolean isAvoid;

}
