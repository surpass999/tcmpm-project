package cn.gemrun.base.module.declare.controller.admin.policy.vo;

import cn.gemrun.base.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.gemrun.base.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

/**
 * 政策通知分页请求 VO
 *
 * @author Gemini
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PolicyPageReqVO extends PageParam {

    /**
     * 关键词搜索（标题/摘要）
     */
    private String keyword;

    /**
     * 类型：1=政策文件，2=工作通知
     */
    private Integer policyType;

    /**
     * 发布单位
     */
    private String releaseDept;

    /**
     * 目标范围：1=全国，2=全省
     */
    private Integer targetScope;

    /**
     * 适用项目类型（多个用逗号分隔，如1,2）
     */
    private String targetProjectTypes;

    /**
     * 状态：1=已发布，2=已下架
     */
    private Integer status;

    /**
     * 发布人ID
     */
    private Long publisherId;

    /**
     * 发布时间开始
     */
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime releaseTimeStart;

    /**
     * 发布时间结束
     */
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime releaseTimeEnd;

}
