package cn.gemrun.base.module.declare.controller.admin.hospital.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 医院信息响应VO
 *
 * @author Gemini
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeclareHospitalRespVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 关联部门ID
     */
    private Long deptId;

    /**
     * 部门名称
     */
    private String deptName;

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
     * 统一社会信用代码
     */
    private String unifiedSocialCreditCode;

    /**
     * 执业许可证登记号
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
     * 项目类型名称
     */
    private String projectTypeName;

    /**
     * 项目类型全称（来自 declare_project_type.title，如"综合型医院"）
     */
    private String projectTypeTitle;

    /**
     * 医院等级
     */
    private String hospitalLevel;

    /**
     * 医院类别
     */
    private String hospitalCategory;

    /**
     * 省份编码
     */
    private String provinceCode;

    /**
     * 省份名称
     */
    private String provinceName;

    /**
     * 城市编码
     */
    private String cityCode;

    /**
     * 城市名称
     */
    private String cityName;

    /**
     * 区县编码
     */
    private String districtCode;

    /**
     * 区县名称
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

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 标签名称（逗号分隔）
     */
    private String tagNames;

}
