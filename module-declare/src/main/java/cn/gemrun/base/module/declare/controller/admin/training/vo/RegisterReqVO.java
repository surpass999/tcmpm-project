package cn.gemrun.base.module.declare.controller.admin.training.vo;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 报名请求 VO（用户端）
 *
 * @author Gemini
 */
@Data
public class RegisterReqVO {

    /**
     * 活动ID
     */
    @NotNull(message = "活动ID不能为空")
    private Long trainingId;

    /**
     * 职位/职称
     */
    private String position;

    /**
     * 联系电话
     */
    private String phone;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 备注
     */
    private String remark;

}
