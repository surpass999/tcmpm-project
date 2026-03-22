package cn.gemrun.base.module.declare.dal.dataobject.training;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;
import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;
import lombok.*;

/**
 * 活动报名 DO
 *
 * @author Gemini
 */
@TableName("declare_training_registration")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingRegistrationDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 活动ID
     */
    private Long trainingId;

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
