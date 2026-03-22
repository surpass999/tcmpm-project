package cn.gemrun.base.module.declare.controller.admin.review.vo;

import cn.gemrun.base.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.gemrun.base.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE;

@Schema(description = "管理后台 - 评审任务分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ReviewTaskPageReqVO extends PageParam {

    @Schema(description = "任务类型：1=备案论证，2=中期评估，3=验收评审，4=成果审核", example = "2")
    private Integer taskType;

    @Schema(description = "业务类型：1=备案，2=项目，3=成果", example = "2")
    private Integer businessType;

    @Schema(description = "关联业务ID", example = "1")
    private Long businessId;

    @Schema(description = "任务名称", example = "中期评估")
    private String taskName;

    @Schema(description = "任务状态：0=待分配，1=评审中，2=已完成", example = "1")
    private Integer status;

    @Schema(description = "专家ID", example = "1")
    private Long expertId;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE)
    private LocalDateTime[] createTime;

}
