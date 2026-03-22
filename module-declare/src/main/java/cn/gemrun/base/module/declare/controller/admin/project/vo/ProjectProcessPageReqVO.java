package cn.gemrun.base.module.declare.controller.admin.project.vo;

import java.time.LocalDateTime;

import lombok.Data;
import cn.gemrun.base.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.gemrun.base.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * 项目过程记录分页 Request VO
 *
 * @author Gemini
 */
@Data
public class ProjectProcessPageReqVO extends PageParam {

    /**
     * 关联项目ID
     */
    private Long projectId;

    /**
     * 过程类型
     */
    private Integer processType;

    /**
     * 状态
     */
    private String status;

    /**
     * 过程标题（模糊查询）
     */
    private String processTitle;

    /**
     * 报告周期开始时间
     */
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] reportPeriodStart;

    /**
     * 报告周期结束时间
     */
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] reportPeriodEnd;

    /**
     * 报告提交时间
     */
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] reportTime;

    /**
     * 创建时间
     */
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
