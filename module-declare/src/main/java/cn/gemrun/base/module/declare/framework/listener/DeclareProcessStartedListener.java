package cn.gemrun.base.module.declare.framework.listener;

import cn.gemrun.base.module.bpm.api.event.ProcessStartedEvent;
import cn.gemrun.base.module.declare.service.achievement.AchievementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 流程发起事件监听器
 *
 * @author Gemini
 */
@Component
@Slf4j
public class DeclareProcessStartedListener {

    @Resource
    private AchievementService achievementService;

    @EventListener
    public void onProcessStarted(ProcessStartedEvent event) {
        String businessType = event.getBusinessType();
        String businessKey = event.getBusinessKey();
        log.info("[DeclareProcessStartedListener] 流程发起事件: businessType={}, businessKey={}", businessType, businessKey);

        if ("achievement:submit".equals(businessType)) {
            Long achievementId = parseAchievementId(businessKey);
            if (achievementId != null) {
                achievementService.updateAchievementStatus(achievementId, "SUBMITTED");
            }
        }
    }

    private Long parseAchievementId(String businessKey) {
        if (businessKey == null) {
            return null;
        }
        try {
            String[] parts = businessKey.split(":");
            if (parts.length > 1) {
                return Long.parseLong(parts[parts.length - 1]);
            }
        } catch (Exception e) {
            log.warn("[parseAchievementId] 解析失败: businessKey={}", businessKey);
        }
        return null;
    }
}
