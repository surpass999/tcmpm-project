package cn.gemrun.base.module.declare.controller.admin.indicator.vo;

import lombok.Data;

/**
 * 指标联合规则 Response VO
 *
 * @author Gemini
 */
@Data
public class DeclareIndicatorJointRuleRespVO {

    /**
     * 规则主键
     */
    private Long id;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 适用项目类型
     */
    private Integer projectType;

    /**
     * 触发时机
     */
    private String triggerTiming;

    /**
     * 适用流程节点
     */
    private String processNode;

    /**
     * 规则配置（JSON格式）
     */
    private String ruleConfig;

    /**
     * 规则状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    private java.time.LocalDateTime createTime;

}
