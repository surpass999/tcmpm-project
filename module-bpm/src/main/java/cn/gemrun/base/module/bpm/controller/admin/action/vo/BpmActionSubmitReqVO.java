package cn.gemrun.base.module.bpm.controller.admin.action.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - 提交BPM操作 Request VO")
@Data
public class BpmActionSubmitReqVO {

    @Schema(description = "业务类型", example = "declare:filing:create")
    @NotBlank(message = "业务类型不能为空")
    private String businessType;

    @Schema(description = "业务ID", example = "1")
    @NotNull(message = "业务ID不能为空")
    private Long businessId;

    @Schema(description = "操作标识", example = "submit")
    @NotBlank(message = "操作标识不能为空")
    private String actionKey;

    @Schema(description = "审批意见", example = "提交审核")
    private String reason;

    @Schema(description = "选择的专家用户ID列表（选择专家操作时使用）", example = "[1, 2, 3]")
    private List<Long> expertUserIds;

    @Schema(description = "退回时的目标节点（退回操作时使用，backStrategy为TO_ANY时必填）")
    private String targetNodeKey;

}
