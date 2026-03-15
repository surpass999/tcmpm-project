package cn.gemrun.base.module.bpm.api.event;

import cn.hutool.core.util.StrUtil;
import org.springframework.context.ApplicationListener;

/**
 * {@link BpmTaskStatusEvent} 的监听器抽象类
 * <p>
 * 业务系统可以继承此类监听特定流程的任务完成事件
 *
 * @author gemrun
 */
public abstract class BpmTaskStatusEventListener implements ApplicationListener<BpmTaskStatusEvent> {

    @Override
    public final void onApplicationEvent(BpmTaskStatusEvent event) {
        if (!StrUtil.equals(event.getProcessDefinitionKey(), getProcessDefinitionKey())) {
            return;
        }
        onEvent(event);
    }

    /**
     * @return 返回监听的流程定义 Key
     */
    protected abstract String getProcessDefinitionKey();

    /**
     * 处理任务完成事件
     *
     * @param event 事件
     */
    protected abstract void onEvent(BpmTaskStatusEvent event);

}
