package cn.gemrun.base.module.declare.controller.admin.indicator.vo;

import cn.gemrun.base.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 指标联合规则分页查询请求 VO
 *
 * @author Gemini
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeclareIndicatorJointRulePageReqVO extends PageParam {

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 适用项目类型
     */
    private Integer projectType;

    /**
     * 规则状态
     */
    private Integer status;

}
