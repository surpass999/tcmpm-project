package cn.gemrun.base.module.bpm.controller.admin.businessType.vo;

import cn.gemrun.base.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.gemrun.base.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 业务类型分页 Request VO")
@Data
public class BpmBusinessTypePageReqVO extends PageParam {

    @Schema(description = "业务类型标识", example = "declare:filing:create")
    private String businessType;

    @Schema(description = "业务类型名称", example = "备案申请")
    private String businessName;

    @Schema(description = "流程定义Key", example = "projectFiling")
    private String processDefinitionKey;

    @Schema(description = "流程分类", example = "declare")
    private String processCategory;

    @Schema(description = "是否启用", example = "1")
    private Integer enabled;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    @Schema(description = "创建时间")
    private LocalDateTime[] createTime;

}
