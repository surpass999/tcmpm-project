package cn.gemrun.base.module.declare.framework.listener;

import cn.gemrun.base.module.bpm.api.event.BpmTaskStatusEvent;
import cn.gemrun.base.module.bpm.api.event.BpmTaskStatusEventListener;
import cn.gemrun.base.module.declare.service.filing.FilingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 备案任务状态监听器
 * 监听备案流程的任务完成事件，更新备案状态
 * 支持 bizStatus 格式：
 * - 普通格式：bizStatus（如：SUBMITTED、NATION_APPROVED）
 * - 带条件格式：bizStatus | condition（如：NATION_APPROVED | TO_PROJECT）
 *
 * @author Gemini
 */
@Component
@Slf4j
public class DeclareFilingTaskStatusListener extends BpmTaskStatusEventListener {

    /**
     * bizStatus 中的条件分隔符
     */
    private static final String STATUS_CONDITION_SEPARATOR = "\\|";

    /**
     * 转项目条件标识
     */
    private static final String CONDITION_TO_PROJECT = "TO_PROJECT";

    @Resource
    private FilingService filingService;

    @Override
    protected String getProcessDefinitionKey() {
        return "filing_approval";
    }

    @Override
    protected void onEvent(BpmTaskStatusEvent event) {
        String businessKey = event.getBusinessKey();
        if (businessKey == null) {
            return;
        }

        // 解析 businessKey: filing_approval_1 -> 业务ID = 1
        String[] splitParts = businessKey.split("_");
        if (splitParts.length < 2) {
            log.warn("[DeclareFilingTaskStatusListener] businessKey 格式不正确: {}", businessKey);
            return;
        }

        Long filingId;
        try {
            filingId = Long.parseLong(splitParts[splitParts.length - 1]);
        } catch (NumberFormatException e) {
            log.warn("[DeclareFilingTaskStatusListener] 解析备案ID失败: businessKey={}", businessKey, e);
            return;
        }

        String taskDefinitionKey = event.getTaskDefinitionKey();
        String bizStatus = event.getBizStatus();

        // 记录接收到的状态变更事件
        log.info("[DeclareFilingTaskStatusListener] 收到任务状态变更事件: filingId={}, taskKey={}, bizStatus={}",
                filingId, taskDefinitionKey, bizStatus);

        // 获取业务状态，如果有则更新
        if (bizStatus != null && !bizStatus.isEmpty()) {
            // 解析 bizStatus 格式：状态值 | 条件标识（如：NATION_APPROVED | TO_PROJECT）
            if (bizStatus.contains("|")) {
                String[] statusParts = bizStatus.split(STATUS_CONDITION_SEPARATOR);
                String filingStatus = statusParts[0].trim();
                String condition = statusParts.length > 1 ? statusParts[1].trim() : null;

                // 更新备案状态（内部会自动处理转项目逻辑：当状态为 NATION_APPROVED 时）
                filingService.updateFilingStatus(filingId, filingStatus);
                log.info("[DeclareFilingTaskStatusListener] 更新备案状态为{}，条件={}: filingId={}",
                        filingStatus, condition, filingId);

                // 如果条件是 TO_PROJECT，且状态不是 NATION_APPROVED，手动触发转项目
                // （FilingServiceImpl.updateFilingStatus 只在 NATION_APPROVED 时自动转项目）
                if (CONDITION_TO_PROJECT.equals(condition) && !"NATION_APPROVED".equals(filingStatus)) {
                    log.warn("[DeclareFilingTaskStatusListener] 条件为TO_PROJECT但状态不是NATION_APPROVED，请检查流程配置: filingId={}, status={}",
                            filingId, filingStatus);
                }
            } else {
                // 兼容旧格式：直接使用 bizStatus 作为备案状态
                filingService.updateFilingStatus(filingId, bizStatus);
                log.info("[DeclareFilingTaskStatusListener] 更新备案状态为{}: filingId={}, taskKey={}",
                        bizStatus, filingId, taskDefinitionKey);
            }
        } else {
            // 兼容旧逻辑：发起人节点没有指定状态时使用默认值 SUBMITTED
            if (isStartUserNode(taskDefinitionKey)) {
                filingService.updateFilingStatus(filingId, "SUBMITTED");
                log.info("[DeclareFilingTaskStatusListener] 发起人节点完成，更新备案状态为SUBMITTED: filingId={}, taskKey={}",
                        filingId, taskDefinitionKey);
            }
        }
    }

    /**
     * 判断是否是发起人节点
     */
    private boolean isStartUserNode(String taskDefinitionKey) {
        return "StartUserNode".equals(taskDefinitionKey)
                || "startEvent1".equals(taskDefinitionKey)
                || (taskDefinitionKey != null && taskDefinitionKey.toLowerCase().contains("start"));
    }
}
