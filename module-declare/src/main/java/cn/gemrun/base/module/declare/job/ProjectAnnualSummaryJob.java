package cn.gemrun.base.module.declare.job;

import cn.gemrun.base.framework.quartz.core.handler.JobHandler;
import cn.gemrun.base.module.declare.dal.dataobject.project.ProjectDO;
import cn.gemrun.base.module.declare.enums.ProjectStatus;
import cn.gemrun.base.module.declare.service.bpm.ProjectBpmService;
import cn.gemrun.base.module.declare.service.project.ProjectProcessService;
import cn.gemrun.base.module.declare.service.project.ProjectService;
import com.mzt.logapi.starter.annotation.LogRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static cn.gemrun.base.module.declare.enums.DeclareLogRecordConstants.*;

/**
 * 项目年度总结定时任务
 * 每年3-6月创建半年报，9-12月创建年报
 * 创建后自动启动BPM流程，并发送通知
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

    @Resource
    private ProjectBpmService projectBpmService;

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
        int processStartedCount = 0;

        for (ProjectDO project : projects) {
            // 检查是否已创建本年度/半年总结
            Integer processType = isAnnual ? 3 : 2; // 2=半年报，3=年度总结
            if (projectProcessService.hasProcess(project.getId(), processType, year)) {
                log.debug("[ProjectAnnualSummaryJob] 项目 {} 已创建本年度总结，跳过", project.getId());
                continue;
            }

            // 1. 创建过程记录
            String title = (isAnnual ? "年度总结" : "半年报") + year;
            LocalDateTime periodEnd = LocalDateTime.of(year, isAnnual ? 12 : 6, 30, 23, 59, 59);
            Long processId = projectProcessService.createProcessForProject(
                    project.getId(),
                    processType,
                    title,
                    LocalDateTime.of(year, isAnnual ? 7 : 1, 1, 0, 0), // 报告周期开始
                    periodEnd // 报告周期结束
            );

            log.info("[ProjectAnnualSummaryJob] 为项目 {} 创建了 {} 过程记录, processId={}",
                    project.getId(), title, processId);
            createdCount++;

            // 2. 自动启动BPM流程
            if (processId != null) {
                String processInstanceId = projectBpmService.startReportProcess(
                        processId,
                        project.getId(),
                        isAnnual,
                        LocalDateTime.of(year, isAnnual ? 7 : 1, 1, 0, 0),
                        periodEnd
                );

                if (processInstanceId != null) {
                    processStartedCount++;
                    log.info("[ProjectAnnualSummaryJob] 启动BPM流程成功, processId={}, processInstanceId={}",
                            processId, processInstanceId);
                } else {
                    log.warn("[ProjectAnnualSummaryJob] 启动BPM流程失败, processId={}", processId);
                }
            }
        }

        return String.format("处理完成，共为 %d 个项目创建了 %s，其中 %d 个启动了BPM流程",
                createdCount, isAnnual ? "年报" : "半年报", processStartedCount);
    }

    /**
     * 处理单个项目的年报/半年报创建（带日志记录）
     */
    @LogRecord(type = FILING_TYPE, subType = JOB_ANNUAL_SUMMARY_SUB_TYPE,
            bizNo = "{{#processId}}", success = JOB_ANNUAL_SUMMARY_SUCCESS)
    public void processProjectAnnualSummary(Long processId, String projectName, boolean isAnnual) {
        // 由 execute 方法调用，实际处理逻辑在 execute 中
    }

}
