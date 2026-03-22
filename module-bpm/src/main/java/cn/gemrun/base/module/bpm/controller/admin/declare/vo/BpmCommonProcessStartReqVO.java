package cn.gemrun.base.module.bpm.controller.admin.declare.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 通用流程发起 Request VO")
@Data
public class BpmCommonProcessStartReqVO {

    @Schema(description = "业务ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "业务ID不能为空")
    private Long businessId;

    @Schema(description = "业务类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "filing")
    @NotEmpty(message = "业务类型不能为空")
    private String businessType;

    @Schema(description = "流程定义的标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "filing_approval")
    @NotEmpty(message = "流程定义标识不能为空")
    private String processDefinitionKey;

}
