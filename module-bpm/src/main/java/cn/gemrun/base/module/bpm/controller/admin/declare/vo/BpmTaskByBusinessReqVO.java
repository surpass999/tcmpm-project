package cn.gemrun.base.module.bpm.controller.admin.declare.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "管理后台 - 根据业务ID查询任务状态 Request VO")
@Data
public class BpmTaskByBusinessReqVO {

    @Schema(description = "业务表分类（对应 bpm_business_type.process_category）", requiredMode = Schema.RequiredMode.REQUIRED, example = "project")
    @NotBlank(message = "业务表分类不能为空")
    private String tableName;

    @Schema(description = "业务ID列表", requiredMode = Schema.RequiredMode.REQUIRED, example = "[1, 2, 3]")
    @NotEmpty(message = "业务ID列表不能为空")
    private List<Long> businessIds;

}
