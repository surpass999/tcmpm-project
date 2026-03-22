package cn.gemrun.base.module.declare.controller.admin.indicator.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 指标值列表响应 VO
 *
 * @author Gemini
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeclareIndicatorValueListRespVO extends DeclareIndicatorValueRespVO {

    /**
     * 指标值列表
     */
    private List<DeclareIndicatorValueRespVO> values;

}
