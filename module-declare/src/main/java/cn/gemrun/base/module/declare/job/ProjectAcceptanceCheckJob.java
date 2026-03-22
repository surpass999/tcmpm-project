package cn.gemrun.base.module.declare.job;

import cn.gemrun.base.framework.quartz.core.handler.JobHandler;
import cn.gemrun.base.module.declare.dal.dataobject.project.ProjectDO;
import cn.gemrun.base.module.declare.service.bpm.ProjectBpmService;
import cn.gemrun.base.module.declare.service.project.ProjectProcessService;
import cn.gemrun.base.module.declare.service.project.ProjectService;
import com.mzt.logapi.starter.annotation.LogRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

import static cn.gemrun.base.module.declare.enums.DeclareLogRecordConstants.*;

/**
 * 项目验收条件检查定时任务
 * 每天检查项目是否符合验收条件，符合条件则自动创建验收申请并启动BPM流程
 *
 * @author Gemini
 */
@Component("ProjectAcceptanceCheckJob")
@Slf4j
public class ProjectAcceptanceCheckJob implements JobHandler {

    @Resource
    private ProjectService projectService;

    @Resource
    private ProjectProcessService projectProcessService;

    @Resource
    private ProjectBpmService projectBpmService;

    @Override
    public String execute(String param) {
        // 查询所有"建设中"或"中期评估"状态的项目
        List<ProjectDO> projects = projectService.getProjectsForAcceptanceCheck();
        if (projects == null || projects.isEmpty()) {
            return "没有需要检查的项目";
        }

        int checkedCount = 0;
        int createdCount = 0;
        int processStartedCount = 0;

        for (ProjectDO project : projects) {
            checkedCount++;

            // 检查指标是否达标
            boolean conditionsMet = checkAcceptanceConditions(project.getId());
            if (!conditionsMet) {
                log.debug("[ProjectAcceptanceCheckJob] 项目 {} 验收条件未达标", project.getId());
                continue;
            }

            // 检查是否已存在验收流程实例
            if (projectProcessService.hasAcceptanceProcess(project.getId())) {
                log.debug("[ProjectAcceptanceCheckJob] 项目 {} 已存在验收流程", project.getId());
                continue;
            }

            // 1. 创建验收申请过程记录（状态为草稿，等待用户主动提交）
            Long processId = projectProcessService.createProcessForProject(
                    project.getId(),
                    6, // 验收申请
                    "验收申请",
                    project.getStartTime() != null ? project.getStartTime() : LocalDateTime.now().minusYears(1),
                    LocalDateTime.now()
            );

            log.info("[ProjectAcceptanceCheckJob] 为项目 {} 创建了验收申请流程, processId={}", project.getId(), processId);
            createdCount++;

            // 2. 自动启动BPM流程
            if (processId != null) {
                String processInstanceId = projectBpmService.startAcceptanceProcess(processId, project.getId());

                if (processInstanceId != null) {
                    processStartedCount++;
                    log.info("[ProjectAcceptanceCheckJob] 启动BPM流程成功, processId={}, processInstanceId={}",
                            processId, processInstanceId);
                } else {
                    log.warn("[ProjectAcceptanceCheckJob] 启动BPM流程失败, processId={}", processId);
                }
            }
        }

        return String.format("处理完成，共检查 %d 个项目，创建 %d 个验收申请，其中 %d 个启动了BPM流程",
                checkedCount, createdCount, processStartedCount);
    }

    /**
     * 处理单个项目的验收申请创建（带日志记录）
     */
    @LogRecord(type = FILING_TYPE, subType = JOB_ACCEPTANCE_CHECK_SUB_TYPE,
            bizNo = "{{#processId}}", success = JOB_ACCEPTANCE_CHECK_SUCCESS)
    public void processProjectAcceptance(Long processId, String projectName) {
        // 由 execute 方法调用，实际处理逻辑在 execute 中
    }

    /**
     * 检查项目是否符合验收条件
     * TODO: 需要根据实际指标服务实现
     *
     * @param projectId 项目ID
     * @return 是否符合验收条件
     */
    private boolean checkAcceptanceConditions(Long projectId) {
        // TODO: 接入指标服务，检查项目指标是否达标
        // 这里暂时返回 true，后续需要根据实际业务逻辑实现
        log.info("[ProjectAcceptanceCheckJob] 检查项目 {} 验收条件（TODO: 需要实现指标检查）", projectId);
        return true;
    }

}
