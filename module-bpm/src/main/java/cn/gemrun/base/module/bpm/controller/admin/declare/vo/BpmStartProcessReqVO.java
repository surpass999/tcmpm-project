package cn.gemrun.base.module.bpm.controller.admin.declare.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 发起流程 Request VO")
@Data
public class BpmStartProcessReqVO {

    @Schema(description = "流程定义的标识（二选一，与 businessType 互斥）", example = "project_midterm")
    private String processDefinitionKey;

    @Schema(description = "业务类型标识（二选一，与 processDefinitionKey 互斥，用于自动查找流程定义）", example = "declare:filing:create")
    private String businessType;

    @Schema(description = "业务ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "业务ID不能为空")
    private Long businessId;

    @Schema(description = "业务唯一标识（可选，默认：processDefinitionKey + '_' + businessId）", example = "project_midterm_1")
    private String businessKey;

    @Schema(description = "流程变量（动态表单数据）")
    private Map<String, Object> variables;

    @Schema(description = "发起人自选审批人 Map", example = "{ \"taskKey1\": [1, 2] }")
    private Map<String, List<Long>> startUserSelectAssignees;

}
