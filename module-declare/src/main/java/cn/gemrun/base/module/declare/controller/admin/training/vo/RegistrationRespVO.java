package cn.gemrun.base.module.declare.controller.admin.training.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 报名详情响应 VO
 *
 * @author Gemini
 */
@Data
public class RegistrationRespVO {

    /**
     * 主键
     */
    private Long id;

    /**
     * 活动ID
     */
    private Long trainingId;

    /**
     * 活动名称
     */
    private String trainingName;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 报名人姓名
     */
    private String userName;

    /**
     * 所属单位
     */
    private String organization;

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
     * 状态：1=已报名，2=已签到，3=已取消，4=未出席
     */
    private Integer status;

    /**
     * 状态标签
     */
    private String statusLabel;

    /**
     * 报名时间
     */
    private LocalDateTime registerTime;

    /**
     * 签到时间
     */
    private LocalDateTime signInTime;

    /**
     * 取消时间
     */
    private LocalDateTime cancelTime;

    /**
     * 参与反馈
     */
    private String feedback;

    /**
     * 评分（1-5分）
     */
    private Integer rating;

    /**
     * 参与证明编号
     */
    private String attendanceCertificate;

    /**
     * 备注
     */
    private String remark;

}
