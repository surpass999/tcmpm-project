package cn.gemrun.base.module.declare.controller.admin.project.vo;

import java.time.LocalDateTime;

import lombok.Data;
import cn.gemrun.base.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.gemrun.base.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * 项目分页 Request VO
 *
 * @author Gemini
 */
@Data
public class ProjectPageReqVO extends PageParam {

    /**
     * 项目名称（模糊查询）
     */
    private String projectName;

    /**
     * 项目类型
     */
    private Integer projectType;

    /**
     * 项目状态（字典值）
     */
    private String projectStatus;

    /**
     * 项目负责人ID
     */
    private Long leaderUserId;

    /**
     * 负责人手机号
     */
    private String leaderMobile;

    /**
     * 负责人姓名（模糊查询）
     */
    private String leaderName;

    /**
     * 立项时间
     */
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] startTime;

    /**
     * 计划完成时间
     */
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] planEndTime;

    /**
     * 创建时间
     */
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
