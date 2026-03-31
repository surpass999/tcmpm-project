package cn.gemrun.base.module.declare.controller.admin.indicator.vo;

import lombok.Data;

import javax.validation.constraints.NotEmpty;

/**
 * 指标分组基础 VO
 *
 * @author Gemini
 */
@Data
public class DeclareIndicatorGroupBaseVO {

    /**
     * 父分组ID（顶级为0）
     */
    private Long parentId;

    /**
     * 分组编码（唯一）
     */
    @NotEmpty(message = "分组编码不能为空")
    private String groupCode;

    /**
     * 分组名称
     */
    @NotEmpty(message = "分组名称不能为空")
    private String groupName;

    /**
     * 分组层级：1=一级 2=二级
     */
    private Integer groupLevel;

    /**
     * 关联项目类型（0-6），一级分组必须填写
     */
    private Integer projectType;

    /**
     * 分组前缀标识
     */
    private String groupPrefix;

    /**
     * 分组描述
     */
    private String description;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 状态：0-禁用 1=启用
     */
    private Integer status;

}
