package cn.gemrun.base.module.declare.controller.admin.hospital.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 医院标签 VO
 *
 * @author Gemini
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HospitalTagVO {

    /**
     * 标签ID
     */
    private Long id;

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

    /**
     * 状态：0=禁用 1=启用
     */
    private Integer status;

    /**
     * 关联医院数量
     */
    private Long hospitalCount;

}
