package cn.gemrun.base.module.declare.controller.admin.project.vo;

import lombok.Data;

/**
 * 项目类型更新请求 VO
 *
 * @author Gemini
 */
@Data
public class ProjectTypeUpdateReqVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 类型名称（如 综合型）
     */
    private String name;

    /**
     * 显示标题（如 综合型医院）
     */
    private String title;

    /**
     * 类型描述
     */
    private String description;

    /**
     * 图标名称或URL
     */
    private String icon;

    /**
     * 主题颜色（如 #1890ff）
     */
    private String color;

    /**
     * 排序（升序）
     */
    private Integer sort;

    /**
     * 状态：0=禁用，1=启用
     */
    private Integer status;

}
