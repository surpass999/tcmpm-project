package cn.gemrun.base.module.bpm.api.task.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 审批详情响应 DTO
 *
 * @author Gemini
 */
@Data
public class BpmApprovalDetailRespDTO {

    /**
     * 流程实例的状态
     */
    private Integer status;

    /**
     * 活动节点列表
     */
    private List<ActivityNode> activityNodes;

    /**
     * 表单字段权限
     */
    private Map<String, String> formFieldsPermission;

    /**
     * 待办任务
     */
    private BpmTodoTaskDTO todoTask;

    /**
     * 流程定义信息
     */
    private BpmProcessDefinitionDTO processDefinition;

    /**
     * 流程实例信息
     */
    private BpmProcessInstanceDTO processInstance;

    @Data
    public static class ActivityNode {

        private String id;
        private String name;
        private Integer nodeType;
        private Integer status;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private List<ActivityNodeTask> tasks;
        private Integer candidateStrategy;
        private List<Long> candidateUserIds;
        private List<BpmUserDTO> candidateUsers;
        private String processInstanceId;

    }

    @Data
    public static class ActivityNodeTask {

        private String id;
        private Long owner;
        private BpmUserDTO ownerUser;
        private Long assignee;
        private BpmUserDTO assigneeUser;
        private Integer status;
        private String reason;
        private String signPicUrl;

    }

    @Data
    public static class BpmTodoTaskDTO {
        private String id;
        private String name;
        private String taskDefinitionKey;
    }

    @Data
    public static class BpmProcessDefinitionDTO {
        private String id;
        private String name;
        private String key;
        private Integer version;
        private String description;
    }

    @Data
    public static class BpmProcessInstanceDTO {
        private String id;
        private String name;
        private String startUserId;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private Integer status;
    }

    @Data
    public static class BpmUserDTO {
        private Long id;
        private String nickname;
        private String avatar;
    }

}
