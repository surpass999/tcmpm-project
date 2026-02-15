package cn.gemrun.base.module.declare.api.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 指标值查询 Request DTO
 *
 * @author Gemini
 */
@Data
public class DeclareIndicatorValueGetReqDTO {

    /**
     * 业务类型：1=备案，2=立项，3=建设过程，4=年度总结，5=中期评估，6=验收申请，7=成果，8=流通交易
     */
    @NotNull(message = "业务类型不能为空")
    private Integer businessType;

    /**
     * 关联业务主键
     */
    @NotNull(message = "业务ID不能为空")
    private Long businessId;

}
