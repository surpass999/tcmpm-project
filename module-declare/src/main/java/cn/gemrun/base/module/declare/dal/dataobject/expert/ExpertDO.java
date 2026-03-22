package cn.gemrun.base.module.declare.dal.dataobject.expert;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 专家信息 DO
 *
 * @author Gemini
 */
@TableName("declare_expert")
@KeySequence("declare_expert_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpertDO extends BaseDO {

    /**
     * 主键（自增）
     */
    @TableId
    private Long id;

    /**
     * 关联系统用户ID（system_users.id）
     */
    private Long userId;

    /**
     * 专家姓名
     */
    private String expertName;

    /**
     * 身份证号
     */
    private String idCard;

    /**
     * 性别：1=男，2=女
     */
    private Integer gender;

    /**
     * 出生日期
     */
    private LocalDate birthDate;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 电子邮箱
     */
    private String email;

    /**
     * 工作单位
     */
    private String workUnit;

    /**
     * 职务
     */
    private String jobTitle;

    /**
     * 职称
     */
    private String professionalTitle;

    /**
     * 所在部门/科室
     */
    private String department;

    /**
     * 专家类型：1=技术专家，2=财务专家，3=管理专家，4=行业专家
     */
    private Integer expertType;

    /**
     * 专业领域（逗号分隔，如：中医信息化、医疗大数据）
     */
    private String specialties;

    /**
     * 擅长方向
     */
    private String expertiseAreas;

    /**
     * 学历
     */
    private String educationBackground;

    /**
     * 学位
     */
    private String degree;

    /**
     * 毕业院校
     */
    private String graduatedFrom;

    /**
     * 资格证书编号
     */
    private String qualificationCert;

    /**
     * 资质证书附件ID（infra_file.id）
     */
    private Long certAttachId;

    /**
     * 累计评审次数
     */
    private Integer reviewCount;

    /**
     * 上次评审时间
     */
    private LocalDateTime lastReviewTime;

    /**
     * 平均评审评分
     */
    private BigDecimal reviewScore;

    /**
     * 状态：1=在册，2=暂停，3=注销
     */
    private Integer status;

    /**
     * 状态说明（如暂停原因）
     */
    private String statusRemark;

    /**
     * 备注
     */
    private String remark;

    /**
     * 乐观锁版本号
     */
    private Integer version;

    /**
     * 删除原因
     */
    private String deleteReason;

}
