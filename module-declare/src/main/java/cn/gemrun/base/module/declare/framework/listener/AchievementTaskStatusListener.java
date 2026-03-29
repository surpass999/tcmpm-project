package cn.gemrun.base.module.declare.framework.listener;

import cn.gemrun.base.module.bpm.api.event.BpmTaskStatusEvent;
import cn.gemrun.base.module.bpm.api.event.BpmTaskStatusEventListener;
import cn.gemrun.base.module.declare.service.achievement.AchievementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 成果任务状态监听器
 *
 * @author Gemini
 */
@Component
@Slf4j
public class AchievementTaskStatusListener extends BpmTaskStatusEventListener {

    private static final String PROCESS_DEFINITION_KEY = "declare_achievement";

    @Resource
    private AchievementService achievementService;

    @Override
    protected String getProcessDefinitionKey() {
        return PROCESS_DEFINITION_KEY;
    }

    @Override
    protected void onEvent(BpmTaskStatusEvent event) {
        Long achievementId;
        try {
            achievementId = parseBusinessId(event);
        } catch (Exception e) {
            log.warn("[AchievementTaskStatusListener] 解析成果ID失败: businessKey={}", event.getBusinessKey());
            return;
        }

        String bizStatus = event.getBizStatus();
        log.info("[AchievementTaskStatusListener] 收到任务状态变更事件: achievementId={}, bizStatus={}",
                achievementId, bizStatus);

        if (bizStatus != null && !bizStatus.isEmpty()) {
            achievementService.updateAchievementStatus(achievementId, bizStatus);
        }
    }
}
