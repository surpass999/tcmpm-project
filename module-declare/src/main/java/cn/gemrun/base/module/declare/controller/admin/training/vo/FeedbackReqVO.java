package cn.gemrun.base.module.declare.controller.admin.training.vo;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

/**
 * 反馈评分请求 VO（用户端）
 *
 * @author Gemini
 */
@Data
public class FeedbackReqVO {

    /**
     * 报名记录ID
     */
    private Long registrationId;

    /**
     * 参与反馈
     */
    private String feedback;

    /**
     * 评分（1-5分）
     */
    @Min(value = 1, message = "评分最小为1分")
    @Max(value = 5, message = "评分最大为5分")
    private Integer rating;

}
