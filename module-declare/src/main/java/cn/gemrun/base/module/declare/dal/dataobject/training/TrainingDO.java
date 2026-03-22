package cn.gemrun.base.module.declare.dal.dataobject.training;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.*;
import cn.gemrun.base.framework.mybatis.core.dataobject.BaseDO;
import lombok.*;

/**
 * 培训交流活动 DO
 *
 * @author Gemini
 */
@TableName("declare_training")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingDO extends BaseDO {

    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 活动名称
     */
    private String name;

    /**
     * 活动类型：1=推进会，2=专题研讨，3=系统演示，4=业务培训
     */
    private Integer type;

    /**
     * 活动详情（富文本）
     */
    private String content;

    /**
     * 组织单位
     */
    private String organizer;

    /**
     * 主讲人/嘉宾
     */
    private String speaker;

    /**
     * 开始时间
     */
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    private LocalDateTime endTime;

    /**
     * 活动地点
     */
    private String location;

    /**
     * 线上参与链接
     */
    private String onlineLink;

    /**
     * 目标范围：1=全国，2=全省
     */
    private Integer targetScope;

    /**
     * 目标省份（逗号分隔）
     */
    private String targetProvinces;

    /**
     * 报名截止时间
     */
    private LocalDateTime registrationDeadline;

    /**
     * 最大参与人数（空表示不限）
     */
    private Integer maxParticipants;

    /**
     * 当前报名人数
     */
    private Integer currentParticipants;

    /**
     * 附件（多个逗号分隔，存储URL）
     */
    private String attachments;

    /**
     * 会议资料/培训材料
     */
    private String meetingMaterials;

    /**
     * 活动海报URL（多个逗号分隔）
     */
    private String posterUrls;

    /**
     * 状态：1=草稿，2=报名中，3=进行中，4=已结束，5=已取消
     */
    private Integer status;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 发布人ID
     */
    private Long publisherId;

    /**
     * 发布人姓名
     */
    private String publisherName;

    /**
     * 备注
     */
    private String remark;

}
