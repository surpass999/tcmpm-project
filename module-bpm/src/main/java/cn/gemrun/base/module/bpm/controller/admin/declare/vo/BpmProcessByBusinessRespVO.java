package cn.gemrun.base.module.bpm.controller.admin.declare.vo;

import cn.gemrun.base.module.bpm.controller.admin.base.user.UserSimpleBaseVO;
import cn.gemrun.base.module.bpm.controller.admin.task.vo.instance.BpmApprovalDetailRespVO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 根据业务ID查询流程信息 Response VO")
@Data
public class BpmProcessByBusinessRespVO {

    @Schema(description = "流程实例ID", example = "1024")
    private String processInstanceId;

    @Schema(description = "流程定义Key", example = "declare_project_annual")
    private String processDefinitionKey;

    @Schema(description = "流程定义名称", example = "年度申报审批")
    private String processDefinitionName;

    @Schema(description = "流程状态", example = "RUNNING")
    private String status; // RUNNING: 运行中, ENDED: 已结束

    @Schema(description = "启动时间")
    private LocalDateTime startTime;

    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    @Schema(description = "所有任务节点列表")
    private List<ActivityNodeVO> activityNodes;

    @Schema(description = "活动节点信息")
    @Data
    public static class ActivityNodeVO {

        @Schema(description = "节点编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "StartUserNode")
        private String id;

        @Schema(description = "节点名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "发起人")
        private String name;

        @Schema(description = "节点类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        private Integer nodeType;

        @Schema(description = "节点状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
        private Integer status;

        @Schema(description = "节点的开始时间")
        private LocalDateTime startTime;

        @Schema(description = "节点的结束时间")
        private LocalDateTime endTime;

        @Schema(description = "任务信息列表")
        private List<ActivityNodeTaskVO> tasks;

    }

    @Schema(description = "活动节点的任务信息")
    @Data
    public static class ActivityNodeTaskVO {

        @Schema(description = "任务编号", example = "1")
        private String id;

        @Schema(description = "任务所属人")
        private UserSimpleBaseVO ownerUser;

        @Schema(description = "任务分配人")
        private UserSimpleBaseVO assigneeUser;

        @Schema(description = "审批意见", example = "同意")
        private String reason;

    }

}
