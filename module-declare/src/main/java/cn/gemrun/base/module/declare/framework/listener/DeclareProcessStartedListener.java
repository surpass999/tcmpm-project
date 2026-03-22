package cn.gemrun.base.module.declare.framework.listener;

import cn.gemrun.base.module.bpm.api.event.ProcessStartedEvent;
import cn.gemrun.base.module.declare.service.achievement.AchievementService;
import cn.gemrun.base.module.declare.service.bpm.ProjectBpmService;
import cn.gemrun.base.module.declare.service.filing.FilingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 流程发起事件监听器（统一入口）
 * <p>
 * 监听 {@link ProcessStartedEvent}，根据 businessType 分发到各业务 Service 更新状态。
 * <p>
 * 支持的 businessType：
 * <ul>
 *   <li>filing:approval / filing:hospital → FilingService.updateFilingProcessInstance()</li>
 *   <li>achievement:submit → AchievementService.updateAchievementProcessInstance()</li>
 *   <li>project_process:type:X → ProjectBpmService.updateProjectProcessInstance()</li>
 * </ul>
 *
 * @author jason
 */
@Component
@Slf4j
public class DeclareProcessStartedListener {

    @Resource
    private FilingService filingService;
    @Resource
    private AchievementService achievementService;
    @Resource
    private ProjectBpmService projectBpmService;

    @EventListener
    public void onProcessStarted(ProcessStartedEvent event) {
        String businessType = event.getBusinessType();
        Long businessId = event.getBusinessId();
        String processInstanceId = event.getProcessInstanceId();

        log.info("[DeclareProcessStartedListener] 收到流程发起事件: businessType={}, businessId={}, processInstanceId={}",
                businessType, businessId, processInstanceId);

        try {
            if (businessType == null) {
                log.warn("[DeclareProcessStartedListener] businessType 为空，跳过");
                return;
            }

            if (businessType.startsWith("filing:")) {
                // 备案流程（filing:approval / filing:hospital）
                filingService.updateFilingProcessInstance(businessId, processInstanceId, "SUBMITTED");
                log.info("[DeclareProcessStartedListener] 更新备案流程实例: filingId={}", businessId);

            } else if ("achievement:submit".equals(businessType)) {
                // 成果提交流程
                achievementService.updateAchievementProcessInstance(businessId, processInstanceId);
                log.info("[DeclareProcessStartedListener] 更新成果流程实例: achievementId={}", businessId);

            } else if (businessType.startsWith("project_process:type:")) {
                // 项目过程流程（年报、半年报、验收等）
                projectBpmService.updateProjectProcessInstance(businessId, processInstanceId);
                log.info("[DeclareProcessStartedListener] 更新项目过程流程实例: processId={}", businessId);

            } else {
                log.warn("[DeclareProcessStartedListener] 未知的业务类型，跳过: {}", businessType);
            }
        } catch (Exception e) {
            log.error("[DeclareProcessStartedListener] 处理流程发起事件失败: businessType={}, businessId={}",
                    businessType, businessId, e);
        }
    }
}
