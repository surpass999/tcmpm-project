package cn.gemrun.base.module.bpm.controller.admin.task.vo.task;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 批量审批任务 Response VO
 *
 * @author jason
 */
@Schema(description = "管理后台 - 批量审批任务 Response VO")
@Data
@Accessors(chain = true)
public class BpmTaskBatchActionRespVO {

    @Schema(description = "总任务数", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    private Integer totalCount;

    @Schema(description = "成功任务数", requiredMode = Schema.RequiredMode.REQUIRED, example = "8")
    private Integer successCount;

    @Schema(description = "失败任务数", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    private Integer failCount;

    @Schema(description = "跳过任务数（无权操作或任务不存在）", requiredMode = Schema.RequiredMode.NOT_REQUIRED, example = "0")
    private Integer skipCount;

    @Schema(description = "任务详情列表", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private List<TaskResult> results;

    @Data
    @Accessors(chain = true)
    public static class TaskResult {

        @Schema(description = "任务编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
        private String taskId;

        @Schema(description = "业务ID（用于前端定位记录）", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        private Long businessId;

        @Schema(description = "是否成功", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
        private Boolean success;

        @Schema(description = "错误编码", example = "TASK_NOT_FOUND")
        private String errorCode;

        @Schema(description = "错误信息", example = "任务不存在或无权操作")
        private String errorMsg;
    }
}