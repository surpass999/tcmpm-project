package cn.gemrun.base.module.declare.controller.admin.hospital.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 医院 Excel 导入 VO
 *
 * @author Gemini
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HospitalImportExcelVO {

    @ExcelProperty("医院编码")
    private String hospitalCode;

    @ExcelProperty("医院全称")
    private String hospitalName;

    @ExcelProperty("医院简称")
    private String shortName;

    @ExcelProperty("关联部门ID")
    private Long deptId;

    @ExcelProperty("医院等级")
    private String hospitalLevel;

    @ExcelProperty("医院类别")
    private String hospitalCategory;

    @ExcelProperty("项目类型")
    private Integer projectType;

    @ExcelProperty("省份编码")
    private String provinceCode;

    @ExcelProperty("城市编码")
    private String cityCode;

    @ExcelProperty("区县编码")
    private String districtCode;

    @ExcelProperty("详细地址")
    private String address;

    @ExcelProperty("联系人")
    private String contactPerson;

    @ExcelProperty("联系电话")
    private String contactPhone;

    @ExcelProperty("邮箱")
    private String contactEmail;

    @ExcelProperty("官方网站")
    private String website;

    @ExcelProperty("编制床位数")
    private Integer bedCount;

    @ExcelProperty("在职职工人数")
    private Integer employeeCount;

    @ExcelProperty("统一社会信用代码")
    private String unifiedSocialCreditCode;

    @ExcelProperty("执业许可证号")
    private String medicalLicenseNo;

    @ExcelProperty("执业许可证有效期")
    private String medicalLicenseExpire;

    @ExcelProperty("状态")
    private Integer status;

}
