package cn.gemrun.base.module.declare.controller.admin.project.vo;

import lombok.Data;

/**
 * 项目类型创建请求 VO
 *
 * @author Gemini
 */
@Data
public class ProjectTypeCreateReqVO {

    /**
     * 类型编码（唯一标识，如 PROJECT_01）
     */
    private String typeCode;

    /**
     * 类型值（业务使用：1=综合型，2=示范型等）
     */
    private Integer typeValue;

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

}
