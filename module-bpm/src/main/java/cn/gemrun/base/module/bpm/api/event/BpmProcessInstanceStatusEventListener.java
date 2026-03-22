package cn.gemrun.base.module.bpm.api.event;

import cn.gemrun.base.module.bpm.framework.core.util.BusinessKeyParser;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;

/**
 * {@link BpmProcessInstanceStatusEvent} 的监听器
 *
 * @author 芋道源码
 */
@Slf4j
public abstract class BpmProcessInstanceStatusEventListener
        implements ApplicationListener<BpmProcessInstanceStatusEvent> {

    @Override
    public final void onApplicationEvent(BpmProcessInstanceStatusEvent event) {
        // 空字符串表示监听所有流程
        String key = getProcessDefinitionKey();
        if (StrUtil.isEmpty(key)) {
            onEvent(event);
            return;
        }
        if (!StrUtil.equals(event.getProcessDefinitionKey(), key)) {
            return;
        }
        onEvent(event);
    }

    /**
     * @return 返回监听的流程定义 Key
     */
    protected abstract String getProcessDefinitionKey();

    /**
     * 处理事件
     *
     * @param event 事件
     */
    protected abstract void onEvent(BpmProcessInstanceStatusEvent event);

    /**
     * 从事件中解析业务ID
     * <p>
     * 格式: {processDefinitionKey}_{businessId}
     *
     * @param event 事件
     * @return 业务ID
     */
    protected Long parseBusinessId(BpmProcessInstanceStatusEvent event) {
        return BusinessKeyParser.parseBusinessId(event.getBusinessKey(), event.getProcessDefinitionKey());
    }

    /**
     * 验证 businessKey 是否匹配期望的前缀
     *
     * @param event          事件
     * @param expectedPrefix 期望的流程定义Key前缀
     * @return 是否匹配
     */
    protected boolean validateBusinessKey(BpmProcessInstanceStatusEvent event, String expectedPrefix) {
        return BusinessKeyParser.matches(event.getBusinessKey(), expectedPrefix);
    }

}
