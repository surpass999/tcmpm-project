package cn.gemrun.base.module.bpm.api.event;

import lombok.Data;
import org.springframework.context.ApplicationEvent;

/**
 * 任务创建事件
 * <p>
 * 在 Flowable 任务创建时触发，用于通知业务系统任务已被创建（如专家评审任务创建后，更新评审任务表）
 *
 * @author jason
 */
@Data
public class BpmTaskCreatedEvent extends ApplicationEvent {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 流程定义Key
     */
    private String processDefinitionKey;

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 任务定义Key（节点ID）
     */
    private String taskDefinitionKey;

    /**
     * 业务标识
     * 格式: {processDefinitionKey}_{businessId}
     */
    private String businessKey;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 审批人ID
     */
    private String assignee;

    /**
     * 业务状态，对应按钮配置的 bizStatus
     * 例如：EXPERT_REVIEWING 表示当前是专家评审阶段
     */
    private String bizStatus;

    public BpmTaskCreatedEvent(Object source) {
        super(source);
    }

}
