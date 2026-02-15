package cn.gemrun.base.module.declare.controller.admin.indicator.vo;

import cn.gemrun.base.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 指标分页 Request VO
 *
 * @author Gemini
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeclareIndicatorPageReqVO extends PageParam {

    /**
     * 指标代号
     */
    private String indicatorCode;

    /**
     * 指标名称
     */
    private String indicatorName;

    /**
     * 指标分类
     */
    private Integer category;

    /**
     * 适用项目类型
     */
    private Integer projectType;

    /**
     * 适用业务类型
     */
    private String businessType;

}
