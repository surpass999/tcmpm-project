package cn.gemrun.base.module.declare.controller.admin.hospital.vo;

import lombok.Data;

/**
 * 医院标签更新请求 VO
 *
 * @author Gemini
 */
@Data
public class HospitalTagUpdateReqVO {

    /**
     * 标签ID
     */
    private Long id;

    /**
     * 标签名称
     */
    private String tagName;

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

    /**
     * 状态：0=禁用 1=启用
     */
    private Integer status;

}
