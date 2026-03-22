package cn.gemrun.base.module.declare.controller.admin.project.vo;

import java.time.LocalDateTime;

import lombok.Data;
import cn.gemrun.base.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;

import static cn.gemrun.base.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * 整改记录分页 Request VO
 *
 * @author Gemini
 */
@Data
public class RectificationPageReqVO extends PageParam {

    /**
     * 关联项目ID
     */
    private Long projectId;

    /**
     * 关联过程记录ID
     */
    private Long processId;

    /**
     * 整改来源
     */
    private Integer rectifyType;

    /**
     * 整改项编号（模糊查询）
     */
    private String rectifyItem;

    /**
     * 完成状态
     */
    private Integer completeStatus;

    /**
     * 整改期限
     */
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] deadline;

    /**
     * 整改完成时间
     */
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] completeTime;

    /**
     * 创建时间
     */
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
