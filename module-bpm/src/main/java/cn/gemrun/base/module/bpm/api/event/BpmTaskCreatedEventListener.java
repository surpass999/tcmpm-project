package cn.gemrun.base.module.bpm.api.event;

import cn.gemrun.base.module.bpm.framework.core.util.BusinessKeyParser;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;

/**
 * {@link BpmTaskCreatedEvent} 的监听器抽象类
 * <p>
 * 业务系统可以继承此类监听特定流程的任务创建事件
 *
 * @author jason
 */
@Slf4j
public abstract class BpmTaskCreatedEventListener implements ApplicationListener<BpmTaskCreatedEvent> {

    @Override
    public final void onApplicationEvent(BpmTaskCreatedEvent event) {
        if (StrUtil.isNotEmpty(getProcessDefinitionKey())
                && !StrUtil.equals(event.getProcessDefinitionKey(), getProcessDefinitionKey())) {
            return;
        }
        onEvent(event);
    }

    /**
     * @return 返回监听的流程定义 Key，null 表示监听所有流程
     */
    protected String getProcessDefinitionKey() {
        return null;
    }

    /**
     * 处理任务创建事件
     *
     * @param event 事件
     */
    protected abstract void onEvent(BpmTaskCreatedEvent event);

    /**
     * 从事件中解析业务ID
     *
     * @param event 事件
     * @return 业务ID
     */
    protected Long parseBusinessId(BpmTaskCreatedEvent event) {
        return BusinessKeyParser.parseBusinessId(event.getBusinessKey(), event.getProcessDefinitionKey());
    }

}
