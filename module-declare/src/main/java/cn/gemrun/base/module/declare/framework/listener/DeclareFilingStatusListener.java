package cn.gemrun.base.module.declare.framework.listener;

import cn.gemrun.base.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.gemrun.base.module.bpm.api.event.BpmProcessInstanceStatusEventListenerV2;
import cn.gemrun.base.module.declare.service.filing.FilingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 备案流程状态监听器
 * 监听备案流程事件，直接将 DSL 中定义的 bizStatus 更新到业务表状态字段
 *
 * @author Gemini
 */
@Component
@Slf4j
public class DeclareFilingStatusListener extends BpmProcessInstanceStatusEventListenerV2 {

    @Resource
    private FilingService filingService;

    @Override
    protected String getBusinessKeyPrefix() {
        return "declare:filing";
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        String businessKey = event.getBusinessKey();
        if (businessKey == null) {
            return;
        }

        // 解析 businessKey: declare:filing:create:1 -> 业务ID = 1
        String[] parts = businessKey.split(":");
        if (parts.length < 4) {
            log.warn("[DeclareFilingStatusListener] businessKey 格式不正确: {}", businessKey);
            return;
        }

        try {
            Long filingId = Long.parseLong(parts[3]);

            // 直接使用 DSL 中定义的 bizStatus 更新业务表状态
            String bizStatus = event.getBizStatus();
            if (bizStatus != null) {
                filingService.updateFilingStatus(filingId, bizStatus);
                log.info("[DeclareFilingStatusListener] 更新备案状态: filingId={}, bizStatus={}",
                        filingId, bizStatus);
            } else {
                log.warn("[DeclareFilingStatusListener] bizStatus 为空，跳过更新: filingId={}", filingId);
            }
        } catch (NumberFormatException e) {
            log.warn("[DeclareFilingStatusListener] 解析备案ID失败: businessKey={}", businessKey, e);
        }
    }

}
