package cn.gemrun.base.module.declare.controller.admin.project.vo;

import lombok.Data;

/**
 * 过程指标配置 Response VO
 *
 * @author Gemini
 */
@Data
public class ProcessIndicatorConfigRespVO {

    /**
     * 配置主键
     */
    private Long id;

    /**
     * 过程类型（1=建设过程，2=半年报，3=年度总结，4=中期评估，5=整改记录，6=验收申请）
     */
    private Integer processType;

    /**
     * 过程类型名称
     */
    private String processTypeName;

    /**
     * 项目类型（0=全部，1=综合型，2=中医电子病历型，3=智慧中药房型，4=名老中医传承型，5=中医临床科研型，6=中医智慧医共体型）
     */
    private Integer projectType;

    /**
     * 项目类型名称
     */
    private String projectTypeName;

    /**
     * 指标ID
     */
    private Long indicatorId;

    /**
     * 指标代号
     */
    private String indicatorCode;

    /**
     * 指标名称
     */
    private String indicatorName;

    /**
     * 指标单位
     */
    private String unit;

    /**
     * 指标分类
     */
    private Integer category;

    /**
     * 指标值类型
     */
    private Integer valueType;

    /**
     * 是否必填
     */
    private Boolean isRequired;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 满分值
     */
    private java.math.BigDecimal maxScore;

    /**
     * 满足(100%)的比例
     */
    private java.math.BigDecimal scoreRatioSatisfied;

    /**
     * 基本满足(75%)的比例
     */
    private java.math.BigDecimal scoreRatioBasic;

    /**
     * 部分满足(50%)的比例
     */
    private java.math.BigDecimal scoreRatioPartial;

    /**
     * 不满足(25%)的比例
     */
    private java.math.BigDecimal scoreRatioUnsatisfied;

}
