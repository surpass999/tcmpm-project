package cn.gemrun.base.module.declare.controller.admin.indicator.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotEmpty;

/**
 * 指标分组保存 Request VO
 *
 * @author Gemini
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DeclareIndicatorGroupSaveReqVO extends DeclareIndicatorGroupBaseVO {

    /**
     * 主键ID（更新时必填）
     */
    private Long id;

}

