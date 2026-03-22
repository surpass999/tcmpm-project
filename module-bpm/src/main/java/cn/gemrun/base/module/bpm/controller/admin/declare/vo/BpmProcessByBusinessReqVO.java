package cn.gemrun.base.module.bpm.controller.admin.declare.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 根据业务ID查询流程信息 Request VO")
@Data
public class BpmProcessByBusinessReqVO {

    @Schema(description = "业务表分类（对应 bpm_business_type.process_category）", requiredMode = Schema.RequiredMode.REQUIRED, example = "project")
    @NotBlank(message = "业务表分类不能为空")
    private String tableName;

    @Schema(description = "业务类型（对应 bpm_business_type.business_type，如 project_process:type:1）", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "project_process:type:1")
    private String businessType;

    @Schema(description = "业务ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "业务ID不能为空")
    private Long businessId;

}
