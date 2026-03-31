package cn.gemrun.base.module.declare.controller.admin.indicator.vo;

import lombok.Data;

/**
 * 指标分组 Response VO
 *
 * @author Gemini
 */
@Data
public class DeclareIndicatorGroupRespVO {

    /**
     * 主键ID
     */
    private Long id;

    /**
     * 父分组ID（顶级为0）
     */
    private Long parentId;

    /**
     * 分组编码（唯一）
     */
    private String groupCode;

    /**
     * 分组名称
     */
    private String groupName;

    /**
     * 分组层级：1=一级 2=二级
     */
    private Integer groupLevel;

    /**
     * 关联项目类型（0-6）
     */
    private Integer projectType;

    /**
     * 项目类型名称
     */
    private String projectTypeName;

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

    /**
     * 创建时间
     */
    private java.time.LocalDateTime createTime;

    /**
     * 子分组列表
     */
    private java.util.List<DeclareIndicatorGroupRespVO> children;

}
