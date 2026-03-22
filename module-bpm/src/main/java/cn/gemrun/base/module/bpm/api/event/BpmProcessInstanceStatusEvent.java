package cn.gemrun.base.module.bpm.api.event;

import lombok.Data;
import org.springframework.context.ApplicationEvent;

import javax.validation.constraints.NotNull;

/**
 * 流程实例的状态（结果）发生变化的 Event
 * <p>
 * 在流程实例完成（审批通过/拒绝/取消）时触发
 *
 * @author 芋道源码
 */
@SuppressWarnings("ALL")
@Data
public class BpmProcessInstanceStatusEvent extends ApplicationEvent {

    /**
     * 流程实例的编号
     */
    @NotNull(message = "流程实例的编号不能为空")
    private String id;
    /**
     * 流程实例的 key
     */
    @NotNull(message = "流程实例的 key 不能为空")
    private String processDefinitionKey;
    /**
     * 流程实例的结果
     */
    @NotNull(message = "流程实例的状态不能为空")
    private Integer status;
    /**
     * 流程实例结束的原因
     */
    private String reason;

    /**
     * 流程实例对应的业务标识
     * 例如说，请假
     */
    private String businessKey;

    /**
     * 业务状态
     * <p>
     * 对应最后一个审批节点按钮配置的 bizStatus
     * 例如：通过(PASS)、拒绝(REJECT)、退回(REJECT_TO_START)等
     * <p>
     * 格式支持：
     * - 普通格式：bizStatus（如：SUBMITTED、NATION_APPROVED）
     * - 带条件格式：bizStatus | condition（如：NATION_APPROVED | TO_PROJECT）
     */
    private String bizStatus;

    public BpmProcessInstanceStatusEvent(Object source) {
        super(source);
    }

}
