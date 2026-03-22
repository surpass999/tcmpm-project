package cn.gemrun.base.module.declare.controller.admin.expert.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.Email;
import javax.validation.constraints.Pattern;
import java.time.LocalDate;

import static cn.gemrun.base.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - 专家创建/更新 Request VO")
@Data
public class ExpertSaveReqVO {

    @Schema(description = "主键（自增）", example = "1")
    private Long id;

    @Schema(description = "关联系统用户ID（system_users.id）", example = "1")
    private Long userId;

    @Schema(description = "专家姓名", example = "张医生", requiredMode = Schema.RequiredMode.REQUIRED)
    private String expertName;

    @Schema(description = "身份证号", example = "110101199001011234")
    @Pattern(regexp = "^[1-9]\\d{5}(18|19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[0-9Xx]$|^$", message = "身份证号格式不正确")
    private String idCard;

    @Schema(description = "性别：1=男，2=女", example = "1")
    private Integer gender;

    @Schema(description = "出生日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate birthDate;

    @Schema(description = "联系电话", example = "13800138000")
    @Pattern(regexp = "^1[3-9]\\d{9}$|^$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "电子邮箱", example = "expert@example.com")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "工作单位", example = "北京某医院")
    private String workUnit;

    @Schema(description = "职务", example = "主任医师")
    private String jobTitle;

    @Schema(description = "职称", example = "正高级")
    private String professionalTitle;

    @Schema(description = "所在部门/科室", example = "心内科")
    private String department;

    @Schema(description = "专家类型：1=技术专家，2=财务专家，3=管理专家，4=行业专家", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
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

    @Schema(description = "状态：1=在册，2=暂停，3=注销", example = "1")
    private Integer status;

    @Schema(description = "状态说明（如暂停原因）", example = "因个人原因暂停")
    private String statusRemark;

    @Schema(description = "备注", example = "备注信息")
    private String remark;

}
