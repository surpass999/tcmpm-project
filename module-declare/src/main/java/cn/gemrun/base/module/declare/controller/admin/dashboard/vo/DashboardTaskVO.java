package cn.gemrun.base.module.declare.controller.admin.dashboard.vo;

import lombok.Data;

import java.util.List;

/**
 * 驾驶舱待办任务响应VO
 *
 * @author Gemini
 */
@Data
public class DashboardTaskVO {

    /**
     * BPM流程待办任务
     */
    private TaskList bpmTasks;

    /**
     * 业务待办（草稿状态的任务）
     */
    private TaskList draftTasks;

    /**
     * 预警任务
     */
    private TaskList warningTasks;

    @Data
    public static class TaskList {
        /** 总数 */
        private Integer total;
        /** 任务列表 */
        private List<TaskItem> items;
    }

    @Data
    public static class TaskItem {
        /** 任务ID */
        private String taskId;
        /** BPM任务ID */
        private String bpmTaskId;
        /** 流程实例ID */
        private String processInstanceId;
        /** 任务名称 */
        private String taskName;
        /** 流程标题（显示用） */
        private String processTitle;
        /** 项目名称 */
        private String projectName;
        /** 项目ID */
        private Long projectId;
        /** 过程记录ID */
        private Long processId;
        /** 过程类型: 1=建设过程, 2=半年报, 3=年报, 4=中期评估, 5=整改记录, 6=验收申请 */
        private Integer processType;
        /** 任务类型: bpm_task=BPM流程任务, draft_report=草稿报告, warning=预警 */
        private String taskType;
        /** 创建时间 */
        private String createTime;
        /** 截止时间 */
        private String deadline;
        /** 状态: pending=待处理, processing=处理中, completed=已完成 */
        private String status;
        /** 是否紧急 */
        private Boolean urgent;
        /** 跳转链接 */
        private String jumpUrl;
    }
}
