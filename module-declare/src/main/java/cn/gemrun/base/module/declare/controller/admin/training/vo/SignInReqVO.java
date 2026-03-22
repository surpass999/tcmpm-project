package cn.gemrun.base.module.declare.controller.admin.training.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 签到请求 VO
 *
 * @author Gemini
 */
@Data
public class SignInReqVO {

    /**
     * 报名记录ID
     */
    @NotNull(message = "报名记录ID不能为空")
    private Long registrationId;

}
