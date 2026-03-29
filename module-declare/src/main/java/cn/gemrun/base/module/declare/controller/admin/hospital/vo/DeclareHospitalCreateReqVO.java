package cn.gemrun.base.module.declare.controller.admin.hospital.vo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * 医院信息创建请求VO
 *
 * @author Gemini
 */
@Data
public class DeclareHospitalCreateReqVO {

    /**
     * 关联部门ID
     */
    private Long deptId;

    /**
     * 医院编码
     */
    private String hospitalCode;

    /**
     * 医院全称
     */
    @NotBlank(message = "医院名称不能为空")
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
    @NotNull(message = "状态不能为空")
    private Integer status;

}
