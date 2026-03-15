package cn.gemrun.base.module.declare.job;

import cn.gemrun.base.framework.quartz.core.handler.JobHandler;
import cn.gemrun.base.module.declare.dal.dataobject.project.ProjectDO;
import cn.gemrun.base.module.declare.service.project.ProjectProcessService;
import cn.gemrun.base.module.declare.service.project.ProjectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目验收条件检查定时任务
 * 每天检查项目是否符合验收条件，符合条件则创建验收申请流程
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

    @Override
    public String execute(String param) {
        // 查询所有"建设中"或"中期评估"状态的项目
        List<ProjectDO> projects = projectService.getProjectsForAcceptanceCheck();
        if (projects == null || projects.isEmpty()) {
            return "没有需要检查的项目";
        }

        int createdCount = 0;
        for (ProjectDO project : projects) {
            // 检查指标是否达标（TODO: 需要根据实际指标服务实现）
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

            // 创建验收申请过程记录（状态为草稿，等待用户主动提交）
            projectProcessService.createProcessForProject(
                    project.getId(),
                    6, // 验收申请
                    "验收申请",
                    project.getStartTime(), // 从项目立项开始
                    LocalDateTime.now() // 至今
            );

            log.info("[ProjectAcceptanceCheckJob] 为项目 {} 创建了验收申请流程", project.getId());
            createdCount++;
        }

        return String.format("处理完成，共为 %d 个项目创建了验收申请", createdCount);
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
