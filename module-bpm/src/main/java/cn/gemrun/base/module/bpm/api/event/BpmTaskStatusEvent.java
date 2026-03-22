package cn.gemrun.base.module.bpm.api.event;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.context.ApplicationEvent;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;

/**
 * 任务完成（状态变化）的 Event
 * <p>
 * 在任务完成时触发，用于通知业务系统每个审批节点的处理结果
 *
 * @author gemrun
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class BpmTaskStatusEvent extends ApplicationEvent {

    /**
     * 任务ID
     */
    @NotNull(message = "任务ID不能为空")
    private String taskId;

    /**
     * 流程定义Key
     */
    @NotBlank(message = "流程定义Key不能为空")
    private String processDefinitionKey;

    /**
     * 流程实例ID
     */
    @NotBlank(message = "流程实例ID不能为空")
    private String processInstanceId;

    /**
     * 任务定义Key（节点ID）
     */
    @NotBlank(message = "任务定义Key不能为空")
    private String taskDefinitionKey;

    /**
     * 任务状态
     * <p>
     * 对应 BpmTaskStatusEnum 枚举值
     * 例如：1=审批中, 2=通过, 3=不通过
     */
    private Integer status;

    /**
     * 业务状态
     * <p>
     * 对应操作按钮配置的 bizStatus
     * 例如：通过(PASS)、拒绝(REJECT)、退回(REJECT_TO_START)等
     */
    private String bizStatus;

    /**
     * 业务标识
     * <p>
     * 对应业务表的主键ID
     */
    @NotBlank(message = "业务标识不能为空")
    private String businessKey;

    /**
     * 业务类型
     * 对应 bpm_business_type 表的 business_type 字段
     * 例如：project_process:type:1
     */
    @NotBlank(message = "业务类型不能为空")
    private String businessType;

    /**
     * 任务完成原因
     */
    private String reason;

    /**
     * 操作人ID
     */
    private Long userId;

    /**
     * 操作人名称
     */
    private String userName;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 操作按钮ID
     * 对应操作按钮配置的 buttonId，用于判断触发的是哪个按钮
     */
    private Integer buttonId;

    /**
     * 流程变量（用于传递按钮扩展参数，如整改流程定义 Key）
     * 从 BpmTaskApproveReqVO.variables 中提取，并在事件发布时构建
     */
    private Map<String, Object> variables;

    public BpmTaskStatusEvent(Object source) {
        super(source);
    }

}
