package cn.gemrun.base.module.bpm.api.event;

import cn.gemrun.base.module.bpm.framework.core.util.BusinessKeyParser;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;

import java.util.Arrays;
import java.util.List;

/**
 * {@link BpmTaskStatusEvent} 的监听器抽象类
 * <p>
 * 业务系统可以继承此类监听特定流程的任务完成事件
 *
 * @author gemrun
 */
@Slf4j
public abstract class BpmTaskStatusEventListener implements ApplicationListener<BpmTaskStatusEvent> {

    @Override
    public final void onApplicationEvent(BpmTaskStatusEvent event) {
        log.info("[BpmTaskStatusEventListener] 收到事件: processDefinitionKey={}, businessType={}, bizStatus={}",
            event.getProcessDefinitionKey(), event.getBusinessType(), event.getBizStatus());
    
        if (!matchesProcessDefinitionKey(event.getProcessDefinitionKey())) {
            return;
        }
        onEvent(event);
    }

    /**
     * @return 返回监听的流程定义 Key（支持多个，用逗号分隔）
     */
    protected String getProcessDefinitionKey() {
        return "";
    }

    /**
     * 检查流程定义Key是否匹配
     * 支持多种匹配模式：
     * 1. 单个精确匹配：如 "declare_filing"
     * 2. 多个精确匹配：用逗号分隔，如 "declare_project_half_year,declare_annual"
     * 3. 前缀匹配：以 "*" 结尾，如 "declare_*" 匹配所有以 declare_ 开头的流程
     * 4. 空字符串：监听所有流程（不推荐，可能影响性能）
     */
    protected boolean matchesProcessDefinitionKey(String eventProcessDefinitionKey) {
        String key = getProcessDefinitionKey();
        
        // 空字符串表示监听所有流程
        if (StrUtil.isEmpty(key)) {
            log.debug("[BpmTaskStatusEventListener] 监听所有流程事件: eventKey={}", eventProcessDefinitionKey);
            return true;
        }
        
        // 单个精确匹配
        if (StrUtil.equals(eventProcessDefinitionKey, key)) {
            return true;
        }
        
        // 多个精确匹配（逗号分隔）
        if (key.contains(",")) {
            List<String> keys = Arrays.asList(key.split(","));
            for (String k : keys) {
                String trimmed = k.trim();
                if (StrUtil.equals(eventProcessDefinitionKey, trimmed)) {
                    return true;
                }
            }
        }
        
        // 前缀匹配（以 "*" 结尾）
        if (key.endsWith("*")) {
            String prefix = key.substring(0, key.length() - 1).trim();
            if (StrUtil.startWith(eventProcessDefinitionKey, prefix)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * 处理任务完成事件
     *
     * @param event 事件
     */
    protected abstract void onEvent(BpmTaskStatusEvent event);

    /**
     * 从事件中解析业务ID
     *
     * @param event 事件
     * @return 业务ID
     */
    protected Long parseBusinessId(BpmTaskStatusEvent event) {
        return BusinessKeyParser.parseBusinessId(event.getBusinessKey(), event.getProcessDefinitionKey());
    }

    /**
     * 验证 businessKey 是否匹配期望的前缀
     *
     * @param event          事件
     * @param expectedPrefix 期望的流程定义Key前缀
     * @return 是否匹配
     */
    protected boolean validateBusinessKey(BpmTaskStatusEvent event, String expectedPrefix) {
        return BusinessKeyParser.matches(event.getBusinessKey(), expectedPrefix);
    }

    /**
     * 从事件中获取业务类型
     *
     * @param event 事件
     * @return 业务类型
     */
    protected String getBusinessType(BpmTaskStatusEvent event) {
        return event.getBusinessType();
    }

}
