package cn.gemrun.base.module.declare.controller.admin.review.vo;

import cn.gemrun.base.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.gemrun.base.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE;

@Schema(description = "管理后台 - 评审结果分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ReviewResultPageReqVO extends PageParam {

    @Schema(description = "任务ID", example = "1")
    private Long taskId;

    @Schema(description = "专家ID", example = "1")
    private Long expertId;

    @Schema(description = "业务类型：1=备案，2=项目，3=成果", example = "2")
    private Integer businessType;

    @Schema(description = "关联业务ID", example = "1")
    private Long businessId;

    @Schema(description = "评审状态：0=待评审，1=已接收，2=评审中，3=已提交，4=超时", example = "0")
    private Integer status;

    @Schema(description = "是否申请回避", example = "false")
    private Boolean isAvoid;

    @Schema(description = "流程实例ID", example = "abc123")
    private String processInstanceId;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE)
    private LocalDateTime[] createTime;

}
