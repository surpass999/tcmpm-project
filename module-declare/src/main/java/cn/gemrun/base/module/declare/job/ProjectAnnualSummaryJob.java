package cn.gemrun.base.module.declare.job;

import cn.gemrun.base.framework.quartz.core.handler.JobHandler;
import cn.gemrun.base.module.declare.dal.dataobject.project.ProjectDO;
import cn.gemrun.base.module.declare.enums.ProjectStatus;
import cn.gemrun.base.module.declare.service.project.ProjectProcessService;
import cn.gemrun.base.module.declare.service.project.ProjectService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 项目年度总结定时任务
 * 每年3-6月创建半年报，9-12月创建年报
 *
 * @author Gemini
 */
@Component("ProjectAnnualSummaryJob")
@Slf4j
public class ProjectAnnualSummaryJob implements JobHandler {

    @Resource
    private ProjectService projectService;

    @Resource
    private ProjectProcessService projectProcessService;

    @Override
    public String execute(String param) {
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();

        // 判断当前是半年报窗口还是年报窗口
        boolean isHalfYear = month >= 3 && month <= 6;  // 半年报：3-6月
        boolean isAnnual = month >= 9 && month <= 12;   // 年报：9-12月

        if (!isHalfYear && !isAnnual) {
            return "当前不在年报时间窗口";
        }

        // 查询所有"建设中"状态的项目
        List<ProjectDO> projects = projectService.getProjectsByStatus(ProjectStatus.CONSTRUCTION);
        if (projects == null || projects.isEmpty()) {
            return "没有在建项目";
        }

        int createdCount = 0;
        for (ProjectDO project : projects) {
            // 检查是否已创建本年度/半年总结
            Integer processType = isAnnual ? 3 : 2; // 2=半年报，3=年度总结
            if (projectProcessService.hasProcess(project.getId(), processType, year)) {
                continue;
            }

            // 创建过程记录
            String title = (isAnnual ? "年度总结" : "半年报") + year;
            projectProcessService.createProcessForProject(
                    project.getId(),
                    processType,
                    title,
                    LocalDateTime.of(year, isAnnual ? 12 : 6, 1, 0, 0), // 报告周期开始
                    LocalDateTime.of(year, isAnnual ? 12 : 6, 30, 23, 59, 59) // 报告周期结束
            );

            log.info("[ProjectAnnualSummaryJob] 为项目 {} 创建了 {} 过程记录", project.getId(), title);
            createdCount++;
        }

        return String.format("处理完成，共为 %d 个项目创建了 %s", createdCount, isAnnual ? "年报" : "半年报");
    }

}
