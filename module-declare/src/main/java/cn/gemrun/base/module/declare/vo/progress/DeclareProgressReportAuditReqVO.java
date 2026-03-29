package cn.gemrun.base.module.declare.vo.progress;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 进度填报审核请求 VO
 *
 * @author Gemini
 */
@Data
public class DeclareProgressReportAuditReqVO {

    /**
     * 记录ID
     */
    @NotNull(message = "记录ID不能为空")
    private Long id;

    /**
     * 审核结果
     */
    @NotNull(message = "审核结果不能为空")
    private Boolean approved;

    /**
     * 审核意见
     */
    private String opinion;

}
