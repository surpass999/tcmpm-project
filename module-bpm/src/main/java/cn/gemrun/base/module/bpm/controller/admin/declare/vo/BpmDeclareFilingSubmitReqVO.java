package cn.gemrun.base.module.bpm.controller.admin.declare.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 备案流程提交 Request VO")
@Data
public class BpmDeclareFilingSubmitReqVO {

    @Schema(description = "备案ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "备案ID不能为空")
    private Long filingId;

}
