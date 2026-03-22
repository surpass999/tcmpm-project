package cn.gemrun.base.module.declare.controller.admin.indicator.vo;

import lombok.Data;

/**
 * 指标口径 Response VO
 *
 * @author Gemini
 */
@Data
public class DeclareIndicatorCaliberRespVO {

    /**
     * 口径主键
     */
    private Long id;

    /**
     * 关联指标ID
     */
    private Long indicatorId;

    /**
     * 指标名称（冗余字段）
     */
    private String indicatorName;

    /**
     * 指标解释
     */
    private String definition;

    /**
     * 统计范围
     */
    private String statisticScope;

    /**
     * 数据来源
     */
    private String dataSource;

    /**
     * 填报要求
     */
    private String fillRequire;

    /**
     * 计算公式
     */
    private String calculationExample;

    /**
     * 创建时间
     */
    private java.time.LocalDateTime createTime;

}
