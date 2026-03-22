package cn.gemrun.base.module.bpm.controller.admin.declare.vo;

import cn.gemrun.base.module.bpm.controller.admin.base.user.UserSimpleBaseVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Schema(description = "管理后台 - 根据业务ID查询任务状态 Response VO")
@Data
public class BpmTaskByBusinessRespVO {

    @Schema(description = "业务ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long businessId;

    @Schema(description = "流程实例ID", example = "1024")
    private String processInstanceId;

    @Schema(description = "流程定义Key", example = "declare_filing")
    private String processDefinitionKey;

    @Schema(description = "是否有待办任务", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean hasTodoTask;

    @Schema(description = "待办任务列表（当前用户的）")
    private List<TodoTask> todoTasks;

    @Schema(description = "已完成的任务列表（当前用户的）")
    private List<DoneTask> doneTasks;

    @Schema(description = "所有已完成的任务列表（用于审批记录展示）")
    private List<DoneTask> allDoneTasks;

    @Data
    @Schema(description = "待办任务信息")
    public static class TodoTask {

        @Schema(description = "任务编号", example = "1024")
        private String taskId;

        @Schema(description = "任务名称", example = "审批")
        private String taskName;

        @Schema(description = "任务定义Key", example = "Activity_approve")
        private String taskDefinitionKey;

        @Schema(description = "创建时间")
        private LocalDateTime createTime;

        @Schema(description = "操作按钮设置")
        private Map<Integer, ButtonSetting> buttonSettings;

        @Schema(description = "是否需要签名", example = "false")
        private Boolean signEnable;

        @Schema(description = "是否填写审批意见", example = "false")
        private Boolean reasonRequire;

        @Schema(description = "节点类型", example = "10")
        private Integer nodeType;

        @Schema(description = "表单编号", example = "1024")
        private Long formId;

        @Schema(description = "表单名称", example = "审批表单")
        private String formName;

        @Schema(description = "表单配置", example = "{\"fields\": []}")
        private String formConf;

    }

    @Data
    @Schema(description = "已完成任务信息")
    public static class DoneTask {

        @Schema(description = "任务编号", example = "1024")
        private String taskId;

        @Schema(description = "任务名称", example = "审批")
        private String taskName;

        @Schema(description = "任务定义Key", example = "Activity_approve")
        private String taskDefinitionKey;

        @Schema(description = "审批人信息")
        private UserSimpleBaseVO assigneeUser;

        @Schema(description = "审批时间")
        private LocalDateTime endTime;

        @Schema(description = "审批结果", example = "true=通过, false=不通过")
        private Boolean approved;

        @Schema(description = "任务状态：2=通过, 3=不通过, 5=退回", example = "2")
        private Integer status;

        @Schema(description = "审批意见", example = "同意")
        private String reason;

    }

    @Data
    @Schema(description = "操作按钮设置")
    public static class ButtonSetting {

        @Schema(description = "显示名称", example = "通过")
        private String displayName;

        @Schema(description = "是否启用", example = "true")
        private Boolean enable;

        @Schema(description = "业务状态标识", example = "PASS")
        private String bizStatus;

    }

}
