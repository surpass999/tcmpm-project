package cn.gemrun.base.module.bpm.controller.admin.task.vo.instance;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.Map;

@Schema(description = "管理后台 - 更新流程实例变量 Request VO")
@Data
public class BpmProcessInstanceUpdateVariablesReqVO {

    @Schema(description = "流程实例编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotEmpty(message = "流程实例编号不能为空")
    private String id;

    @Schema(description = "流程变量 Map", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "流程变量不能为空")
    private Map<String, Object> variables;

}
