package cn.gemrun.base.module.bpm.controller.admin.action.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - 撤回流程请求")
@Data
public class BpmActionWithdrawReqVO {

    @Schema(description = "业务类型，如 declare:filing:create", requiredMode = Schema.RequiredMode.REQUIRED, example = "declare:filing:create")
    @NotBlank(message = "业务类型不能为空")
    private String businessType;

    @Schema(description = "业务ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "业务ID不能为空")
    private Long businessId;

}
