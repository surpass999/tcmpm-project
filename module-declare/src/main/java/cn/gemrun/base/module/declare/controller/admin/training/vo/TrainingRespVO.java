package cn.gemrun.base.module.declare.controller.admin.training.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 培训活动详情响应 VO
 *
 * @author Gemini
 */
@Data
public class TrainingRespVO {

    /**
     * 主键
     */
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
     * 活动类型标签
     */
    private String typeLabel;

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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", locale = "zh_CN", timezone = "GMT+8")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", locale = "zh_CN", timezone = "GMT+8")
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
     * 目标范围标签
     */
    private String targetScopeLabel;

    /**
     * 目标省份
     */
    private String targetProvinces;

    /**
     * 报名截止时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", locale = "zh_CN", timezone = "GMT+8")
    private LocalDateTime registrationDeadline;

    /**
     * 最大参与人数
     */
    private Integer maxParticipants;

    /**
     * 当前报名人数
     */
    private Integer currentParticipants;

    /**
     * 附件URL列表（逗号分隔）
     */
    private String attachmentUrls;

    /**
     * 会议资料/培训材料
     */
    private String meetingMaterials;

    /**
     * 活动海报URL列表（逗号分隔）
     */
    private String posterUrls;

    /**
     * 状态：1=草稿，2=报名中，3=进行中，4=已结束，5=已取消
     */
    private Integer status;

    /**
     * 状态标签
     */
    private String statusLabel;

    /**
     * 发布时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", locale = "zh_CN", timezone = "GMT+8")
    private LocalDateTime publishTime;

    /**
     * 发布人ID
     */
    private Long publisherId;

    /**
     * 发布人名称
     */
    private String publisherName;

    /**
     * 备注
     */
    private String remark;

    /**
     * 创建时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", locale = "zh_CN", timezone = "GMT+8")
    private LocalDateTime createTime;

}
