package cn.gemrun.base.module.declare.service.training;

import lombok.Data;

/**
 * 培训活动统计响应 VO
 *
 * @author Gemini
 */
@Data
public class TrainingStatisticsRespVO {

    /**
     * 总报名人数
     */
    private Long totalRegistrations;

    /**
     * 已签到人数
     */
    private Long signedInCount;

    /**
     * 已取消人数
     */
    private Long cancelledCount;

    /**
     * 未出席人数
     */
    private Long absentCount;

}
