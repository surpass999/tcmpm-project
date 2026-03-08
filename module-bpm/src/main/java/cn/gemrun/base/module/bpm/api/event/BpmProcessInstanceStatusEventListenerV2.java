package cn.gemrun.base.module.bpm.api.event;

import cn.hutool.core.util.StrUtil;
import org.springframework.context.ApplicationListener;

/**
 * {@link BpmProcessInstanceStatusEvent} 的监听器（支持 businessType 过滤）
 *
 * @author Gemini
 */
public abstract class BpmProcessInstanceStatusEventListenerV2
        implements ApplicationListener<BpmProcessInstanceStatusEvent> {

    @Override
    public final void onApplicationEvent(BpmProcessInstanceStatusEvent event) {
        // 检查 businessKey 是否匹配
        String businessKey = event.getBusinessKey();
        if (businessKey == null || !matchBusinessKey(businessKey)) {
            return;
        }
        onEvent(event);
    }

    /**
     * 判断 businessKey 是否匹配
     * 默认实现：检查 businessKey 是否以指定前缀开头
     *
     * @param businessKey 业务键，格式: businessType:businessId
     * @return 是否匹配
     */
    protected boolean matchBusinessKey(String businessKey) {
        String prefix = getBusinessKeyPrefix();
        if (prefix == null) {
            return false;
        }
        return businessKey.startsWith(prefix);
    }

    /**
     * @return 返回监听的业务键前缀，如 "declare:filing"
     */
    protected abstract String getBusinessKeyPrefix();

    /**
     * 处理事件
     *
     * @param event 事件
     */
    protected abstract void onEvent(BpmProcessInstanceStatusEvent event);

}
