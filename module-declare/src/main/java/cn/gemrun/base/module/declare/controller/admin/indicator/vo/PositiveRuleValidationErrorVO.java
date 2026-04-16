package cn.gemrun.base.module.declare.controller.admin.indicator.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 上期对比规则校验错误 VO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PositiveRuleValidationErrorVO {
    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 指标编码
     */
    private String indicatorCode;

    /**
     * 错误消息
     */
    private String message;
}
