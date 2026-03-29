package cn.gemrun.base.module.declare.dal.dataobject.hospital;

import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 医院信息表
 *
 * @author Gemini
 */
@TableName("declare_hospital")
@Data
@EqualsAndHashCode(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeclareHospitalDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联部门ID（对应system_dept.id）
     */
    private Long deptId;

    /**
     * 医院编码
     */
    private String hospitalCode;

    /**
     * 医院全称
     */
    private String hospitalName;

    /**
     * 医院简称
     */
    private String shortName;

    /**
     * 统一社会信用代码（18位，唯一标识）
     */
    private String unifiedSocialCreditCode;

    /**
     * 医疗机构执业许可证登记号
     */
    private String medicalLicenseNo;

    /**
     * 执业许可证有效期
     */
    private LocalDate medicalLicenseExpire;


    /**
     * 项目类型：1=综合型，2=中医电子病历型，3=智慧中药房型，4=名老中医传承型，5=中医临床科研型，6=中医智慧医共体型
     */
    private Integer projectType;

    /**
     * 医院等级：三级甲等、三级乙等、二级甲等、二级乙等、一级甲等、一级乙等、未定级等
     */
    private String hospitalLevel;

    /**
     * 医院类别：综合医院、中医医院、中西医结合医院、民族医医院、专科医院等
     */
    private String hospitalCategory;

    /**
     * 所属省份编码
     */
    private String provinceCode;

    /**
     * 所属省份名称
     */
    private String provinceName;

    /**
     * 所属城市编码
     */
    private String cityCode;

    /**
     * 所属城市名称
     */
    private String cityName;

    /**
     * 所属区县编码
     */
    private String districtCode;

    /**
     * 所属区县名称
     */
    private String districtName;

    /**
     * 详细地址
     */
    private String address;

    /**
     * 联系人
     */
    private String contactPerson;

    /**
     * 联系电话
     */
    private String contactPhone;

    /**
     * 邮箱
     */
    private String contactEmail;

    /**
     * 官方网站
     */
    private String website;

    /**
     * 编制床位数
     */
    private Integer bedCount;

    /**
     * 在职职工人数
     */
    private Integer employeeCount;

    /**
     * 状态：0-停用 1-启用
     */
    private Integer status;

}
