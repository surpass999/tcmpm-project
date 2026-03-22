package cn.gemrun.base.module.declare.framework.listener;

import cn.gemrun.base.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.gemrun.base.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.gemrun.base.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.gemrun.base.module.bpm.framework.flowable.core.enums.BpmnVariableConstants;
import cn.gemrun.base.module.declare.controller.admin.project.vo.ProjectProcessRespVO;
import cn.gemrun.base.module.declare.service.project.ProjectProcessService;
import cn.gemrun.base.module.declare.service.project.ProjectService;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 项目流程实例状态监听器
 * <p>
 * 监听项目相关流程的实例状态事件，主要处理：
 * 1. 主流程完成时的日志记录和状态更新
 * 2. 子流程完成时，自动反查主流程并通知继续
 *
 * @author Gemini
 */
@Component
@Slf4j
public class ProjectProcessStatusListener extends BpmProcessInstanceStatusEventListener {

    /**
     * 子流程变量名：父流程实例ID
     */
    private static final String PARENT_PROCESS_INSTANCE_ID_VAR = "PARENT_PROCESS_INSTANCE_ID";

    /**
     * 过程类型对应的流程定义Key前缀
     */
    private static final String PROCESS_DEFINITION_KEY_PREFIX = "declare_project_";

    @Resource
    private ProjectProcessService projectProcessService;

    @Resource
    private ProjectService projectService;

    @Resource
    private RuntimeService runtimeService;

    @Override
    protected String getProcessDefinitionKey() {
        // 监听所有流程，通用处理
        return "";
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        String processInstanceId = event.getId();
        String businessKey = event.getBusinessKey();
        String processDefinitionKey = event.getProcessDefinitionKey();

        // 1. 检查是否是子流程完成（通过 PARENT_PROCESS_INSTANCE_ID 变量判断）
        Object parentProcessInstanceId = runtimeService.getVariable(processInstanceId, PARENT_PROCESS_INSTANCE_ID_VAR);
        if (parentProcessInstanceId != null) {
            // 子流程完成，通知主流程继续
            handleChildProcessCompleted(event, processInstanceId, parentProcessInstanceId.toString(), businessKey);
            return;
        }

        // 2. 主流程处理逻辑
        if (businessKey == null) {
            return;
        }

        // 校验 businessKey 格式（只处理项目相关流程）
        if (!validateBusinessKey(event, PROCESS_DEFINITION_KEY_PREFIX)) {
            log.debug("[ProjectProcessStatusListener] businessKey 不是项目流程: {}", businessKey);
            return;
        }

        // 解析过程记录ID
        Long processId;
        try {
            processId = parseBusinessId(event);
        } catch (Exception e) {
            log.warn("[ProjectProcessStatusListener] 解析过程记录ID失败: businessKey={}", businessKey, e);
            return;
        }

        // 获取流程实例状态
        String bizStatus = event.getBizStatus();
        Integer status = event.getStatus();
        String reason = event.getReason();

        // 获取状态描述
        String statusName = getStatusName(status);

        // 记录流程实例完成日志
        log.info("[ProjectProcessStatusListener] 项目过程流程实例完成: processId={}, processInstanceId={}, status={}({}), bizStatus={}, reason={}",
                processId, processInstanceId, status, statusName, bizStatus, reason);

        // ===== 验收申请流程(processType=6) + 审批通过(status=2) 时，标记项目完成 =====
        if (BpmProcessInstanceStatusEnum.APPROVE.getStatus().equals(status)) {
            ProjectProcessRespVO processVO = projectProcessService.getProjectProcess(processId);
            if (processVO != null && processVO.getProjectId() != null && 6 == processVO.getProcessType()) {
                projectService.markProjectCompleted(processVO.getProjectId());
                log.info("[ProjectProcessStatusListener] 验收流程通过，标记项目完成: projectId={}, processId={}",
                        processVO.getProjectId(), processId);
            }
        }
    }

    /**
     * 处理子流程完成事件
     * 通过 PARENT_PROCESS_INSTANCE_ID 自动反查主流程并通知继续
     */
    private void handleChildProcessCompleted(BpmProcessInstanceStatusEvent event,
                                            String childProcessInstanceId,
                                            String parentProcessInstanceId,
                                            String businessKey) {
        log.info("[ProjectProcessStatusListener] 子流程完成: childProcessInstanceId={}, parentProcessInstanceId={}, businessKey={}, status={}",
                childProcessInstanceId, parentProcessInstanceId, businessKey, event.getStatus());

        // 从子流程变量中获取 businessId（过程记录ID）
        Object businessIdObj = runtimeService.getVariable(childProcessInstanceId, "businessId");
        if (businessIdObj == null) {
            log.warn("[ProjectProcessStatusListener] 子流程没有businessId: childProcessInstanceId={}", childProcessInstanceId);
            return;
        }

        Long processId;
        try {
            if (businessIdObj instanceof Long) {
                processId = (Long) businessIdObj;
            } else {
                processId = Long.parseLong(businessIdObj.toString());
            }
        } catch (NumberFormatException e) {
            log.warn("[ProjectProcessStatusListener] 子流程businessId格式错误: {}", businessIdObj);
            return;
        }

        // 判断子流程是否审批通过
        boolean isApproved = BpmProcessInstanceStatusEnum.APPROVE.getStatus().equals(event.getStatus());
        String statusName = isApproved ? "审批通过" : "审批不通过";

        log.info("[ProjectProcessStatusListener] 子流程{}完成, processId={}, parentProcessInstanceId={}",
                statusName, processId, parentProcessInstanceId);

        // 通知主流程继续（通过更新主流程的业务状态）
        // 主流程如果有等待子流程完成的逻辑，可以通过监听主流程的任务状态事件来处理
        notifyMainProcess(processId, parentProcessInstanceId, isApproved, event);
    }

    /**
     * 通知主流程继续执行
     * <p>
     * 这里采用灵活的机制：
     * 1. 如果主流程使用 CallActivity 调用子流程，Flowable 会自动处理父子同步
     * 2. 如果是独立启动的子流程，通过设置主流程变量来通知
     */
    private void notifyMainProcess(Long processId, String parentProcessInstanceId, boolean isApproved, BpmProcessInstanceStatusEvent event) {
        // 从子流程变量中获取 businessType
        Object businessTypeObj = runtimeService.getVariable(event.getId(), "businessType");

        // 构建通知信息
        String notificationKey = "CHILD_PROCESS_" + event.getId() + "_RESULT";
        String notificationValue = isApproved ? "APPROVED" : "REJECTED";

        runtimeService.setVariable(parentProcessInstanceId, notificationKey, notificationValue);
        runtimeService.setVariable(parentProcessInstanceId, "CHILD_PROCESS_RESULT", isApproved ? "APPROVED" : "REJECTED");
        runtimeService.setVariable(parentProcessInstanceId, "CHILD_PROCESS_ID", event.getId());
        runtimeService.setVariable(parentProcessInstanceId, "CHILD_PROCESS_STATUS", event.getStatus());

        // 如果主流程有待办任务（等待子流程完成），可以通过事件机制触发
        log.info("[ProjectProcessStatusListener] 已通知主流程: parentProcessInstanceId={}, result={}, processId={}",
                parentProcessInstanceId, notificationValue, processId);

        // 更新过程记录状态
        if (processId != null) {
            String newStatus = isApproved ? "EXPERT_REVIEW_APPROVED" : "EXPERT_REVIEW_REJECTED";
            projectProcessService.updateProjectProcessStatus(processId, newStatus);
            log.info("[ProjectProcessStatusListener] 已更新过程记录状态: processId={}, status={}", processId, newStatus);
        }
    }

    /**
     * 获取状态描述
     */
    private String getStatusName(Integer statusCode) {
        if (statusCode == null) {
            return "未知";
        }
        BpmProcessInstanceStatusEnum status = BpmProcessInstanceStatusEnum.valueOf(statusCode);
        if (status == null) {
            return "未知";
        }
        return status.getDesc();
    }

}
