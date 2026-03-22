package cn.gemrun.base.module.declare.controller.admin.project.vo;

import cn.gemrun.base.framework.common.pojo.PageParam;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.gemrun.base.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * 过程指标配置分页 Request VO
 *
 * @author Gemini
 */
@Data
public class ProcessIndicatorConfigPageReqVO extends PageParam {

    /**
     * 过程类型（1=建设过程，2=半年报，3=年度总结，4=中期评估，5=整改记录，6=验收申请）
     */
    private Integer processType;

    /**
     * 项目类型（0=全部，1=综合型，2=中医电子病历型，3=智慧中药房型，4=名老中医传承型，5=中医临床科研型，6=中医智慧医共体型）
     */
    private Integer projectType;

    /**
     * 指标ID
     */
    private Long indicatorId;

    /**
     * 指标名称（模糊搜索）
     */
    private String indicatorName;

}
