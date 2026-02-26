package cn.gemrun.base.module.declare.controller.admin.indicator.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 指标口径保存请求 VO
 *
 * @author Gemini
 */
@Data
public class DeclareIndicatorCaliberSaveReqVO {

    /**
     * 口径主键
     */
    private Long id;

    /**
     * 关联指标ID
     */
    @NotNull(message = "指标ID不能为空")
    private Long indicatorId;

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

}
