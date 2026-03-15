package cn.gemrun.base.module.declare.framework.listener;

import cn.gemrun.base.module.bpm.api.event.BpmTaskStatusEvent;
import cn.gemrun.base.module.bpm.api.event.BpmTaskStatusEventListener;
import cn.gemrun.base.module.declare.service.achievement.AchievementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 成果转化任务状态监听器
 * 监听成果转化流程的任务完成事件，更新成果状态
 *
 * @author
 */
@Component
@Slf4j
public class AchievementTaskStatusListener extends BpmTaskStatusEventListener {

    @Resource
    private AchievementService achievementService;

    @Override
    protected String getProcessDefinitionKey() {
        return "declare_achievement";
    }

    @Override
    protected void onEvent(BpmTaskStatusEvent event) {
        String businessKey = event.getBusinessKey();
        if (businessKey == null) {
            return;
        }

        // 解析 businessKey: declare:achievement:submit:1 -> 业务ID = 1
        String[] splitParts = businessKey.split(":");
        if (splitParts.length < 4) {
            log.warn("[AchievementTaskStatusListener] businessKey 格式不正确: {}", businessKey);
            return;
        }

        Long achievementId;
        try {
            achievementId = Long.parseLong(splitParts[splitParts.length - 1]);
        } catch (NumberFormatException e) {
            log.warn("[AchievementTaskStatusListener] 解析成果ID失败: businessKey={}", businessKey, e);
            return;
        }

        String taskDefinitionKey = event.getTaskDefinitionKey();
        String bizStatus = event.getBizStatus();

        log.info("[AchievementTaskStatusListener] 收到任务状态变更事件: achievementId={}, taskKey={}, bizStatus={}",
                achievementId, taskDefinitionKey, bizStatus);

        // 获取业务状态，如果有则更新
        if (bizStatus != null && !bizStatus.isEmpty()) {
            achievementService.updateAchievementStatus(achievementId, bizStatus);
            log.info("[AchievementTaskStatusListener] 更新成果状态为{}: achievementId={}, taskKey={}",
                    bizStatus, achievementId, taskDefinitionKey);
        }
    }

}
