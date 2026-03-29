package cn.gemrun.base.module.declare.controller.admin.indicator.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 指标含上期数据 VO
 *
 * @author Gemini
 */
@Data
public class IndicatorWithLastPeriodVO {

    /**
     * 指标ID
     */
    private Long indicatorId;

    /**
     * 指标编码
     */
    private String indicatorCode;

    /**
     * 指标名称
     */
    private String indicatorName;

    /**
     * 本期值
     */
    private BigDecimal currentValue;

    /**
     * 上期值
     */
    private BigDecimal lastPeriodValue;

    /**
     * 变化量
     */
    private BigDecimal changeAmount;

    /**
     * 变化率
     */
    private BigDecimal changeRatio;

}
