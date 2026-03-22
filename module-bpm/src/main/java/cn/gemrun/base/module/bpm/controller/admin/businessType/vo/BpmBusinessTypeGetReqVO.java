package cn.gemrun.base.module.bpm.controller.admin.businessType.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Schema(description = "管理后台 - 业务类型查询 Request VO")
@Data
public class BpmBusinessTypeGetReqVO {

    @Schema(description = "业务类型标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "declare:filing:create")
    @NotBlank(message = "业务类型标识不能为空")
    private String businessType;

}
