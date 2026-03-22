package cn.gemrun.base.module.iot.mq.consumer.rule;

import cn.gemrun.base.module.iot.core.messagebus.core.IotMessageBus;
import cn.gemrun.base.module.iot.core.messagebus.core.IotMessageSubscriber;
import cn.gemrun.base.module.iot.core.mq.message.IotDeviceMessage;
import cn.gemrun.base.module.iot.service.rule.data.IotDataRuleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * 针对 {@link IotDeviceMessage} 的消费者，处理数据流转
 *
 * @author 芋道源码
 */
@Component
@Slf4j
public class IotDataRuleMessageHandler implements IotMessageSubscriber<IotDeviceMessage> {

    @Resource
    private IotDataRuleService dataRuleService;

    @Resource
    private IotMessageBus messageBus;

    @PostConstruct
    public void init() {
        messageBus.register(this);
    }

    @Override
    public String getTopic() {
        return IotDeviceMessage.MESSAGE_BUS_DEVICE_MESSAGE_TOPIC;
    }

    @Override
    public String getGroup() {
        return "iot_data_rule_consumer";
    }

    @Override
    public void onMessage(IotDeviceMessage message) {
        dataRuleService.executeDataRule(message);
    }

}
