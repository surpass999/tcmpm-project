package cn.gemrun.base.module.bpm.api.event;

import lombok.Data;
import org.springframework.context.ApplicationEvent;

/**
 * 流程发起事件
 * <p>
 * 在流程实例创建成功后触发，用于通知各业务模块更新业务状态（如从 DRAFT → SUBMITTED）
 *
 * @author jason
 */
@Data
public class ProcessStartedEvent extends ApplicationEvent {

    /**
     * 流程实例ID
     */
    private String processInstanceId;

    /**
     * 业务类型标识，用于分发到对应的业务Service
     * 例如：achievement:submit, filing:approval, project_process:type:2
     */
    private String businessType;

    /**
     * 业务Key，格式: {processDefinitionKey}_{businessId}
     */
    private String businessKey;

    /**
     * 业务ID
     */
    private Long businessId;

    /**
     * 流程定义Key
     */
    private String processDefinitionKey;

    /**
     * 发起人用户ID
     */
    private Long userId;

    public ProcessStartedEvent(Object source) {
        super(source);
    }

    public ProcessStartedEvent(String processInstanceId, String businessType, String businessKey,
                               Long businessId, String processDefinitionKey, Long userId) {
        super(processInstanceId);
        this.processInstanceId = processInstanceId;
        this.businessType = businessType;
        this.businessKey = businessKey;
        this.businessId = businessId;
        this.processDefinitionKey = processDefinitionKey;
        this.userId = userId;
    }
}
