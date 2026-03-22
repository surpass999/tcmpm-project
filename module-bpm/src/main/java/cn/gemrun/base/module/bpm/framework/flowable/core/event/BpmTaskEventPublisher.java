package cn.gemrun.base.module.bpm.framework.flowable.core.event;

import cn.gemrun.base.module.bpm.api.event.BpmTaskStatusEvent;
import lombok.AllArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.validation.annotation.Validated;

import javax.validation.Valid;

/**
 * {@link BpmTaskStatusEvent} 的生产者
 *
 * @author gemrun
 */
@AllArgsConstructor
@Validated
public class BpmTaskEventPublisher {

    private final ApplicationEventPublisher publisher;

    /**
     * 发布任务完成事件
     *
     * @param event 任务状态事件
     */
    public void sendTaskStatusEvent(@Valid BpmTaskStatusEvent event) {
        publisher.publishEvent(event);
    }

}
