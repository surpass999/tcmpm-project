package cn.gemrun.base.module.declare.controller.admin.hospital.vo;

import lombok.Data;

/**
 * 医院信息分页请求VO
 *
 * @author Gemini
 */
@Data
public class DeclareHospitalPageReqVO {

    /**
     * 页码
     */
    private Integer pageNo = 1;

    /**
     * 每页条数
     */
    private Integer pageSize = 10;

    /**
     * 医院名称
     */
    private String hospitalName;

    /**
     * 医院简称
     */
    private String shortName;

    /**
     * 关联部门ID
     */
    private Long deptId;

    /**
     * 省份编码
     */
    private String provinceCode;

    /**
     * 城市编码
     */
    private String cityCode;

    /**
     * 医院等级
     */
    private String hospitalLevel;


    /**
     * 医院类别
     */
    private String hospitalCategory;

    /**
     * 项目类型
     */
    private Integer projectType;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 统一社会信用代码
     */
    private String unifiedSocialCreditCode;

}
