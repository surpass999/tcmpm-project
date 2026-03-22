package cn.gemrun.base.module.declare.controller.admin.training.vo;

import cn.gemrun.base.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 培训活动分页请求 VO
 *
 * @author Gemini
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class TrainingPageReqVO extends PageParam {

    /**
     * 关键词搜索（名称/组织单位/主讲人）
     */
    private String keyword;

    /**
     * 活动类型：1=推进会，2=专题研讨，3=系统演示，4=业务培训
     */
    private Integer type;

    /**
     * 状态：1=草稿，2=报名中，3=进行中，4=已结束，5=已取消
     */
    private Integer status;

    /**
     * 目标范围：1=全国，2=全省
     */
    private Integer targetScope;

    /**
     * 发布人ID
     */
    private Long publisherId;

    /**
     * 开始时间起
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTimeStart;

    /**
     * 开始时间止
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTimeEnd;

}
