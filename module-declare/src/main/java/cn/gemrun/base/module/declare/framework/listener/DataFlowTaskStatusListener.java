package cn.gemrun.base.module.declare.framework.listener;

import cn.gemrun.base.module.bpm.api.event.BpmTaskStatusEvent;
import cn.gemrun.base.module.bpm.api.event.BpmTaskStatusEventListener;
import cn.gemrun.base.module.declare.service.dataflow.DataFlowService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 数据流通任务状态监听器
 * 监听数据流通流程的任务完成事件，更新数据流通状态
 *
 * @author
 */
@Component
@Slf4j
public class DataFlowTaskStatusListener extends BpmTaskStatusEventListener {

    @Resource
    private DataFlowService dataFlowService;

    @Override
    protected String getProcessDefinitionKey() {
        return "declare_data_flow";
    }

    @Override
    protected void onEvent(BpmTaskStatusEvent event) {
        String businessKey = event.getBusinessKey();
        if (businessKey == null) {
            return;
        }

        // 解析 businessKey: declare:dataflow:submit:1 -> 业务ID = 1
        String[] splitParts = businessKey.split(":");
        if (splitParts.length < 4) {
            log.warn("[DataFlowTaskStatusListener] businessKey 格式不正确: {}", businessKey);
            return;
        }

        Long dataFlowId;
        try {
            dataFlowId = Long.parseLong(splitParts[splitParts.length - 1]);
        } catch (NumberFormatException e) {
            log.warn("[DataFlowTaskStatusListener] 解析数据流通ID失败: businessKey={}", businessKey, e);
            return;
        }

        String taskDefinitionKey = event.getTaskDefinitionKey();
        String bizStatus = event.getBizStatus();

        log.info("[DataFlowTaskStatusListener] 收到任务状态变更事件: dataFlowId={}, taskKey={}, bizStatus={}",
                dataFlowId, taskDefinitionKey, bizStatus);

        // 获取业务状态，如果有则更新
        if (bizStatus != null && !bizStatus.isEmpty()) {
            dataFlowService.updateDataFlowStatus(dataFlowId, bizStatus);
            log.info("[DataFlowTaskStatusListener] 更新数据流通状态为{}: dataFlowId={}, taskKey={}",
                    bizStatus, dataFlowId, taskDefinitionKey);
        }
    }

}
