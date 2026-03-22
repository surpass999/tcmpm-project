package cn.gemrun.base.module.bpm.controller.admin.declare.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "管理后台 - 批量查询流程任务状态 Request VO")
@Data
public class BpmTaskBatchStatusReqVO {

    @Schema(description = "流程实例ID列表", requiredMode = Schema.RequiredMode.REQUIRED, example = "[1, 2, 3]")
    @NotEmpty(message = "流程实例ID列表不能为空")
    private List<String> processInstanceIds;

}
