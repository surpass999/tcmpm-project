package cn.gemrun.base.module.declare.service.bpm.impl;

import cn.gemrun.base.module.bpm.api.event.ProcessStartedEvent;
import cn.gemrun.base.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.gemrun.base.module.bpm.service.business.BpmBusinessTypeService;
import cn.gemrun.base.module.bpm.service.task.BpmProcessInstanceService;
import cn.gemrun.base.module.declare.dal.dataobject.project.ProjectDO;
import cn.gemrun.base.module.declare.dal.dataobject.project.ProjectProcessDO;
import cn.gemrun.base.module.declare.dal.mysql.project.ProjectMapper;
import cn.gemrun.base.module.declare.dal.mysql.project.ProjectProcessMapper;
import cn.gemrun.base.module.declare.enums.ProcessType;
import cn.gemrun.base.module.declare.framework.bpm.DeclareBpmVariableConstants;
import cn.gemrun.base.module.declare.service.bpm.ProjectBpmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * 项目BPM流程服务实现类
 * 提供定时任务触发的流程启动能力
 *
 * @author Gemini
 */
@Service
@Slf4j
public class ProjectBpmServiceImpl implements ProjectBpmService {

    @Resource
    private BpmProcessInstanceService bpmProcessInstanceService;

    @Resource
    private BpmBusinessTypeService bpmBusinessTypeService;

    @Resource
    private ProjectProcessMapper projectProcessMapper;

    @Resource
    private ProjectMapper projectMapper;

    @Resource
    private ApplicationEventPublisher applicationEventPublisher;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String startReportProcess(Long processId, Long projectId, boolean isAnnual,
                                    LocalDateTime reportPeriodStart, LocalDateTime reportPeriodEnd) {
        Integer processType = isAnnual ? ProcessType.ANNUAL.getType() : ProcessType.HALF_YEAR.getType();
        String result = startProcess(processId, projectId, processType);

        if (result != null) {
            // 发送通知
            ProjectDO project = projectMapper.selectById(projectId);
            if (project != null && project.getLeaderUserId() != null) {
                String reportType = isAnnual ? "年报" : "半年报";
                String year = String.valueOf(reportPeriodEnd.getYear());
                String deadline = reportPeriodEnd.format(DATE_FORMATTER);
                String title = "您有新的" + reportType + "需要填写";
                String content = String.format("项目【%s】的%s提交截止日期为%s，请及时完成填写。",
                        project.getProjectName(), year + reportType, deadline);
                sendNotice(project.getLeaderUserId(), title, content);
            }
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String startAcceptanceProcess(Long processId, Long projectId) {
        String result = startProcess(processId, projectId, ProcessType.ACCEPTANCE.getType());

        if (result != null) {
            // 发送通知
            ProjectDO project = projectMapper.selectById(projectId);
            if (project != null && project.getLeaderUserId() != null) {
                String title = "项目已达到验收条件";
                String content = String.format("项目【%s】已满足验收条件，请在30天内提交验收申请。",
                        project.getProjectName());
                sendNotice(project.getLeaderUserId(), title, content);
            }
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String startProcess(Long processId, Long projectId, Integer processType) {
        log.info("[ProjectBpmService] 启动流程: processId={}, projectId={}, processType={}",
                processId, projectId, processType);

        // 1. 获取过程记录
        ProjectProcessDO process = projectProcessMapper.selectById(processId);
        if (process == null) {
            log.error("[ProjectBpmService] 过程记录不存在, processId: {}", processId);
            return null;
        }

        // 2. 检查是否已存在流程实例
        if (StringUtils.hasText(process.getProcessInstanceId())) {
            log.warn("[ProjectBpmService] 过程记录已存在流程实例, processId: {}, processInstanceId: {}",
                    processId, process.getProcessInstanceId());
            return process.getProcessInstanceId();
        }

        // 3. 获取项目信息
        ProjectDO project = projectMapper.selectById(projectId);
        if (project == null) {
            log.error("[ProjectBpmService] 项目不存在, projectId: {}", projectId);
            return null;
        }

        // 4. 获取流程定义Key
        String businessTypeKey = "project_process:type:" + processType;
        String processDefinitionKey = bpmBusinessTypeService.getProcessDefinitionKey(businessTypeKey);
        if (processDefinitionKey == null) {
            log.error("[ProjectBpmService] 流程定义不存在, businessType: {}", businessTypeKey);
            return null;
        }

        // 5. 构建 businessKey
        String businessKey = processDefinitionKey + "_" + processId;

        // 6. 构建流程变量
        Map<String, Object> variables = buildProcessVariables(process, project);

        // 7. 构建流程创建请求
        BpmProcessInstanceCreateReqDTO createReqDTO = new BpmProcessInstanceCreateReqDTO();
        createReqDTO.setProcessDefinitionKey(processDefinitionKey);
        createReqDTO.setBusinessKey(businessKey);
        createReqDTO.setVariables(variables);

        try {
            // 8. 启动流程
            Long userId = getSystemUserId();
            String processInstanceId = bpmProcessInstanceService.createProcessInstance(userId, createReqDTO);

            // 9. 发布流程发起事件（由 BpmProcessStartedListener 统一更新状态）
            applicationEventPublisher.publishEvent(new ProcessStartedEvent(
                    processInstanceId,
                    businessTypeKey,
                    businessKey,
                    processId,
                    processDefinitionKey,
                    userId
            ));

            log.info("[ProjectBpmService] 启动流程成功, processId: {}, processInstanceId: {}, businessKey: {}",
                    processId, processInstanceId, businessKey);

            return processInstanceId;
        } catch (Exception e) {
            log.error("[ProjectBpmService] 启动流程失败, processId: {}, error: {}", processId, e.getMessage(), e);
            return null;
        }
    }

    @Override
    public void sendNotice(Long userId, String title, String content) {
        if (userId == null) {
            log.warn("[ProjectBpmService] 用户ID为空，跳过发送通知");
            return;
        }
        // TODO: 接入消息通知服务
        // 暂时记录日志，后续接入消息通知模块
        log.info("[ProjectBpmService] 发送通知: userId={}, title={}, content={}", userId, title, content);
    }

    @Override
    public void updateProjectProcessInstance(Long processId, String processInstanceId) {
        ProjectProcessDO process = projectProcessMapper.selectById(processId);
        if (process == null) {
            log.warn("[updateProjectProcessInstance] 过程记录不存在: processId={}", processId);
            return;
        }
        // 幂等保护：已存在流程实例ID则跳过
        if (StringUtils.hasText(process.getProcessInstanceId())) {
            log.info("[updateProjectProcessInstance] 过程记录已有流程实例，跳过: processId={}, processInstanceId={}",
                    processId, process.getProcessInstanceId());
            return;
        }
        process.setProcessInstanceId(processInstanceId);
        process.setStatus("SUBMITTED");
        projectProcessMapper.updateById(process);
        log.info("[updateProjectProcessInstance] 更新过程记录流程实例: processId={}, processInstanceId={}",
                processId, processInstanceId);
    }

    /**
     * 构建流程变量
     * <p>
     * 使用 DeclareBpmVariableConstants 中定义的常量作为变量名，确保命名统一
     */
    private Map<String, Object> buildProcessVariables(ProjectProcessDO process, ProjectDO project) {
        Map<String, Object> variables = new HashMap<>();

        // 业务标识
        variables.put(DeclareBpmVariableConstants.BUSINESS_KEY, project.getProjectName());
        variables.put(DeclareBpmVariableConstants.BUSINESS_TYPE, "project");

        // 项目信息
        variables.put(DeclareBpmVariableConstants.PROJECT_ID, project.getId());
        variables.put(DeclareBpmVariableConstants.PROJECT_NAME, project.getProjectName());
        variables.put(DeclareBpmVariableConstants.HOSPITAL_ID, project.getDeptId());
        variables.put(DeclareBpmVariableConstants.HOSPITAL_NAME, project.getOrgName());

        // 过程信息
        variables.put(DeclareBpmVariableConstants.PROCESS_ID, process.getId());
        variables.put(DeclareBpmVariableConstants.PROCESS_TYPE, process.getProcessType());
        variables.put(DeclareBpmVariableConstants.PROCESS_TITLE, process.getProcessTitle());

        // 报告周期
        if (process.getReportPeriodStart() != null) {
            variables.put(DeclareBpmVariableConstants.REPORT_PERIOD_START,
                    process.getReportPeriodStart().format(DATE_FORMATTER));
        }
        if (process.getReportPeriodEnd() != null) {
            variables.put(DeclareBpmVariableConstants.REPORT_PERIOD_END,
                    process.getReportPeriodEnd().format(DATE_FORMATTER));
        }

        return variables;
    }

    /**
     * 获取系统用户ID（用于定时任务触发的流程启动）
     * 优先使用项目的医院用户ID，如果没有则使用系统管理员ID
     */
    private Long getSystemUserId() {
        // 优先使用医院用户ID发起流程
        // TODO: 需要从项目信息中获取医院用户ID
        // 目前返回1（系统管理员），后续需要从项目表中获取 hospital_user_id
        return 1L;
    }
}
