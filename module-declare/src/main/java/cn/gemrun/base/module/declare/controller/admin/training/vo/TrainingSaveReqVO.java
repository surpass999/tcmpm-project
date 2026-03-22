package cn.gemrun.base.module.declare.controller.admin.training.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.format.annotation.DateTimeFormat;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 培训活动保存请求 VO
 *
 * @author Gemini
 */
@Data
public class TrainingSaveReqVO {

    /**
     * 主键（为空时为新增）
     */
    private Long id;

    /**
     * 活动名称
     */
    @NotBlank(message = "活动名称不能为空")
    private String name;

    /**
     * 活动类型：1=推进会，2=专题研讨，3=系统演示，4=业务培训
     */
    @NotNull(message = "活动类型不能为空")
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
    @NotNull(message = "开始时间不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", locale = "zh_CN", timezone = "GMT+8")
    private LocalDateTime startTime;

    /**
     * 结束时间
     */
    @NotNull(message = "结束时间不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
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
    @NotNull(message = "目标范围不能为空")
    private Integer targetScope;

    /**
     * 目标省份（逗号分隔）
     */
    private String targetProvinces;

    /**
     * 报名截止时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", locale = "zh_CN", timezone = "GMT+8")
    private LocalDateTime registrationDeadline;

    /**
     * 最大参与人数（空表示不限）
     */
    private Integer maxParticipants;

    /**
     * 附件URL列表（逗号分隔）
     */
    private String attachmentUrls;

    /**
     * 会议资料/培训材料
     */
    private String meetingMaterials;

    /**
     * 活动海报URL列表（多个逗号分隔）
     */
    private String posterUrls;

    /**
     * 备注
     */
    private String remark;

}
