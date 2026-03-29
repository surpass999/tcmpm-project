package cn.gemrun.base.module.declare.controller.admin.hospital.vo;

import lombok.Data;

/**
 * 医院标签创建请求 VO
 *
 * @author Gemini
 */
@Data
public class HospitalTagCreateReqVO {

    /**
     * 标签编码
     */
    private String tagCode;

    /**
     * 标签名称
     */
    private String tagName;

    /**
     * 标签分类
     */
    private String tagCategory;

    /**
     * 标签类型：1=单选 2=多选
     */
    private Integer tagType;

    /**
     * 父标签ID
     */
    private Long parentId;

    /**
     * 排序
     */
    private Integer sort;

}
