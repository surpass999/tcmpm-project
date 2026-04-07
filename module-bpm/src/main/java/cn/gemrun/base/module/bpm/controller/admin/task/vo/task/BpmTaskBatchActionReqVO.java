package cn.gemrun.base.module.bpm.controller.admin.task.vo.task;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 批量审批任务 Request VO
 *
 * @author jason
 */
@Schema(description = "管理后台 - 批量审批任务 Request VO")
@Data
@Accessors(chain = true)
public class BpmTaskBatchActionReqVO {

    @Schema(description = "任务操作类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "任务操作类型不能为空")
    private Integer actionType;

    @Schema(description = "任务列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "任务列表不能为空")
    @Valid
    private List<TaskItem> tasks;

    @Schema(description = "公共审批意见（批量审批时统一使用）", example = "同意")
    private String reason;

    @Schema(description = "公共签名图片URL（批量审批时��一使用，需要签名的场景）", example = "https://example.com/sign.png")
    private String signPicUrl;

    @Schema(description = "公共流程变量（批量审批时统一使用）", example = "{}")
    private java.util.Map<String, Object> variables;

    @Schema(description = "公共下一个节点审批人（批量审批时统一使用，选择专家场景）", example = "{}")
    private java.util.Map<String, List<Long>> nextAssignees;

    @Data
    @Accessors(chain = true)
    public static class TaskItem {

        @Schema(description = "任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
        @NotEmpty(message = "任务编号不能为空")
        private String id;

        @Schema(description = "按钮 ID（对应按钮设置的 Id，用于获取 bizStatus）", example = "1")
        private Integer buttonId;

        @Schema(description = "退回到的任务 Key（仅退回操作时使用）", example = "Activity_user_task_1")
        private String targetTaskDefinitionKey;
    }
}