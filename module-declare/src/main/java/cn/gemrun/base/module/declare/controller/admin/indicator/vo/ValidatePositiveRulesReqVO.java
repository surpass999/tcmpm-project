package cn.gemrun.base.module.declare.controller.admin.indicator.vo;

import lombok.Data;

import java.util.Map;

/**
 * 上期对比规则校验请求 VO
 */
@Data
public class ValidatePositiveRulesReqVO {
    /**
     * 项目类型
     */
    private Integer projectType;

    /**
     * 当前值 Map<indicatorCode, value>
     */
    private Map<String, Object> currentValues;

    /**
     * 上期值 Map<indicatorCode, value>
     */
    private Map<String, Object> lastPeriodValues;

    /**
     * 规则配置 JSON
     */
    private String ruleConfig;
}
