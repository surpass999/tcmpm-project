package cn.gemrun.base.module.bpm.controller.admin.declare;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - 选择专家 Request VO")
@Data
public class SelectExpertReqVO {

    @Schema(description = "任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotEmpty(message = "任务编号不能为空")
    private String id;

    @Schema(description = "选择的专家用户ID列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "专家用户ID列表不能为空")
    private List<Long> userIds;

    @Schema(description = "按钮ID，用于更新业务状态", example = "8")
    private Integer buttonId;

}
