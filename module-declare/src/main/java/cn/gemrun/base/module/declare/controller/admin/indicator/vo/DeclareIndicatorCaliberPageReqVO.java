package cn.gemrun.base.module.declare.controller.admin.indicator.vo;

import cn.gemrun.base.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 指标口径分页查询请求 VO
 *
 * @author Gemini
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeclareIndicatorCaliberPageReqVO extends PageParam {

    /**
     * 指标ID
     */
    private Long indicatorId;

    /**
     * 指标解释（模糊搜索）
     */
    private String definition;

}
