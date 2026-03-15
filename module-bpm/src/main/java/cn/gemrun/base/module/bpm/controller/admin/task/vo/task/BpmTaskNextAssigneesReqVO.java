package cn.gemrun.base.module.bpm.controller.admin.task.vo.task;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 设置下一个节点审批人的 Request VO")
@Data
public class BpmTaskNextAssigneesReqVO {

    @Schema(description = "任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotEmpty(message = "任务编号不能为空")
    private String id;

    @Schema(description = "下一个节点审批人", example = "{nodeId:[1, 2]}")
    @NotEmpty(message = "下一个节点审批人不能为空")
    private Map<String, List<Long>> nextAssignees;

}
