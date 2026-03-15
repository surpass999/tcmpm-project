package cn.gemrun.base.module.declare.framework.listener;

import cn.gemrun.base.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.gemrun.base.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.gemrun.base.module.declare.controller.admin.project.vo.ProjectProcessRespVO;
import cn.gemrun.base.module.declare.enums.ProjectProcessStatus;
import cn.gemrun.base.module.declare.enums.ProjectStatus;
import cn.gemrun.base.module.declare.service.project.ProjectProcessService;
import cn.gemrun.base.module.declare.service.project.ProjectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 项目流程状态监听器
 * 监听项目相关流程事件，根据流程状态更新项目过程记录和项目主表状态
 *
 * @author Gemini
 */
@Component
@Slf4j
public class ProjectProcessStatusListener extends BpmProcessInstanceStatusEventListener {

    @Resource
    private ProjectProcessService projectProcessService;

    @Resource
    private ProjectService projectService;

    @Override
    protected String getProcessDefinitionKey() {
        return "declare_project";
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        String businessKey = event.getBusinessKey();
        if (businessKey == null) {
            return;
        }

        // 解析 businessKey: declare:project:annual:101 -> processId = 101
        // 格式: declare:project:{processType}:{processId}
        String[] parts = businessKey.split(":");
        if (parts.length < 4) {
            log.warn("[ProjectProcessStatusListener] businessKey 格式不正确: {}", businessKey);
            return;
        }

        try {
            Long processId = Long.parseLong(parts[3]);
            String bizStatus = event.getStatus().toString();

            // 更新过程记录状态
            updateProcessStatus(processId, bizStatus);

            // 根据流程类型和状态更新项目主表状态
            String processType = parts[2];
            updateProjectStatus(processId, processType, bizStatus);

            log.info("[ProjectProcessStatusListener] 处理项目流程事件: processId={}, processType={}, bizStatus={}",
                    processId, processType, bizStatus);
        } catch (NumberFormatException e) {
            log.warn("[ProjectProcessStatusListener] 解析过程记录ID失败: businessKey={}", businessKey, e);
        }
    }

    /**
     * 更新过程记录状态
     */
    private void updateProcessStatus(Long processId, String bizStatus) {
        if (bizStatus == null) {
            return;
        }

        ProjectProcessStatus status = convertBizStatusToProcessStatus(bizStatus);
        if (status != null) {
            projectProcessService.updateProjectProcessStatus(processId, status.getStatus());
            log.info("[ProjectProcessStatusListener] 更新过程记录状态: processId={}, status={}", processId, status);
        }
    }

    /**
     * 根据流程类型和状态更新项目主表状态
     */
    private void updateProjectStatus(Long processId, String processType, String bizStatus) {
        if (bizStatus == null || !"APPROVED".equals(bizStatus)) {
            return;
        }

        // 获取过程记录关联的项目ID
        ProjectProcessRespVO process = projectProcessService.getProjectProcess(processId);
        if (process == null || process.getProjectId() == null) {
            log.warn("[ProjectProcessStatusListener] 过程记录不存在或无关联项目: processId={}", processId);
            return;
        }

        Long projectId = process.getProjectId();

        // 根据流程类型更新项目状态
        ProjectStatus newStatus = null;
        switch (processType) {
            case "construction":
                // 建设过程审核通过 -> 保持建设中
                break;
            case "annual":
            case "half_year":
                // 年度总结/半年报审核通过 -> 保持建设中
                break;
            case "midterm":
                // 中期评估审核通过 -> 更新为中期评估状态
                newStatus = ProjectStatus.MIDTERM;
                break;
            case "rectification":
                // 整改审核通过 -> 更新为建设中
                newStatus = ProjectStatus.CONSTRUCTION;
                break;
            case "acceptance":
                // 验收审核通过 -> 更新为已验收
                newStatus = ProjectStatus.ACCEPTED;
                break;
            default:
                log.warn("[ProjectProcessStatusListener] 未知的流程类型: {}", processType);
        }

        if (newStatus != null) {
            projectService.updateProjectStatus(projectId, newStatus.getStatus());
            log.info("[ProjectProcessStatusListener] 更新项目状态: projectId={}, status={}", projectId, newStatus);
        }
    }

    /**
     * 将 bizStatus 转换为过程记录状态
     */
    private ProjectProcessStatus convertBizStatusToProcessStatus(String bizStatus) {
        if (bizStatus == null) {
            return null;
        }
        switch (bizStatus) {
            case "DRAFT":
                return ProjectProcessStatus.DRAFT;
            case "SUBMITTED":
                return ProjectProcessStatus.SUBMITTED;
            case "AUDITING":
            case "PROVINCE_APPROVED":
            case "NATIONAL_APPROVED":
                return ProjectProcessStatus.AUDITING;
            case "APPROVED":
            case "APPEOVED": // 兼容拼写错误
                return ProjectProcessStatus.APPROVED;
            case "RETURNED":
            case "REJECTED":
                return ProjectProcessStatus.REJECTED;
            default:
                log.warn("[ProjectProcessStatusListener] 未知的 bizStatus: {}", bizStatus);
                return null;
        }
    }

}
