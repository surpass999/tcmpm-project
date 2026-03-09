package cn.gemrun.base.module.bpm.framework.dsl;

import cn.gemrun.base.module.bpm.controller.admin.task.vo.instance.BpmApprovalDetailRespVO;
import cn.gemrun.base.module.bpm.dal.dataobject.BpmBusinessProcessDO;
import cn.gemrun.base.module.bpm.framework.dsl.config.DslConfig;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * DSL 退回策略处理器
 * 基于 DSL 的 backStrategy 配置处理退回逻辑
 *
 * @author Gemini
 */
@Slf4j
@Component
public class DslBackStrategyHandler {

    /**
     * 退回策略类型
     */
    public static final String TO_START = "TO_START";      // 退回起点
    public static final String TO_PREV = "TO_PREV";       // 退回上一节点
    public static final String TO_ANY = "TO_ANY";          // 退回任意节点
    public static final String TO_ROLE = "TO_ROLE";        // 退回指定角色

    /**
     * 获取退回策略
     *
     * @param dslConfig 节点 DSL 配置
     * @return 退回策略类型
     */
    public String getBackStrategy(DslConfig dslConfig) {
        if (dslConfig == null || StrUtil.isBlank(dslConfig.getBackStrategy())) {
            return TO_PREV; // 默认退回上一节点
        }
        return dslConfig.getBackStrategy();
    }

    /**
     * 获取可退回的节点列表
     *
     * @param currentNodeKey    当前节点 Key
     * @param dslNodes         所有 DSL 节点列表
     * @param backStrategy     退回策略
     * @return 可退回的节点列表
     */
    public List<DslBackTarget> getBackTargets(String currentNodeKey, List<DslConfig> dslNodes, String backStrategy) {
        List<DslBackTarget> targets = new ArrayList<>();

        if (dslNodes == null || dslNodes.isEmpty()) {
            return targets;
        }

        // 找到当前节点的索引
        int currentIndex = -1;
        for (int i = 0; i < dslNodes.size(); i++) {
            if (dslNodes.get(i).getNodeKey().equals(currentNodeKey)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex <= 0) {
            // 当前节点是第一个，无法退回
            return targets;
        }

        switch (backStrategy) {
            case TO_START:
                // 退回起点：只能退回到第一个节点（发起人节点）
                DslConfig firstNode = dslNodes.get(0);
                targets.add(new DslBackTarget(firstNode.getNodeKey(), firstNode.getName(), "起点"));
                break;

            case TO_PREV:
                // 退回上一节点：只能退回到前一个节点
                DslConfig prevNode = dslNodes.get(currentIndex - 1);
                targets.add(new DslBackTarget(prevNode.getNodeKey(), prevNode.getName(), "上一节点"));
                break;

            case TO_ANY:
                // 退回任意节点：可以退回到流程中任意已通过的节点
                for (int i = 0; i < currentIndex; i++) {
                    DslConfig node = dslNodes.get(i);
                    targets.add(new DslBackTarget(node.getNodeKey(), node.getName(), node.getCap()));
                }
                break;

            case TO_ROLE:
                // 退回指定角色：需要根据角色配置确定可退回的节点
                for (int i = 0; i < currentIndex; i++) {
                    DslConfig node = dslNodes.get(i);
                    if (node.getRoles() != null && node.getRoles().length > 0) {
                        targets.add(new DslBackTarget(node.getNodeKey(), node.getName(),
                                String.join(",", node.getRoles())));
                    }
                }
                break;

            default:
                // 默认退回上一节点
                DslConfig defaultPrevNode = dslNodes.get(currentIndex - 1);
                targets.add(new DslBackTarget(defaultPrevNode.getNodeKey(), defaultPrevNode.getName(), "上一节点"));
                break;
        }

        log.info("[DslBackStrategyHandler] 获取可退回节点: currentNodeKey={}, strategy={}, targets={}",
                currentNodeKey, backStrategy, targets.size());

        return targets;
    }

    /**
     * 从 DSL JSON 字符串获取退回策略
     *
     * @param dslJson DSL JSON 字符串
     * @return 退回策略类型
     */
    public String getBackStrategyFromJson(String dslJson) {
        if (StrUtil.isBlank(dslJson)) {
            return TO_PREV;
        }

        try {
            JSONObject jsonObject = JSONUtil.parseObj(dslJson);
            String backStrategy = jsonObject.getStr("backStrategy");
            return StrUtil.isBlank(backStrategy) ? TO_PREV : backStrategy;
        } catch (Exception e) {
            log.warn("[DslBackStrategyHandler] 解析退回策略失败: {}", e.getMessage());
            return TO_PREV;
        }
    }

    /**
     * 退回目标节点
     */
    @Data
    public static class DslBackTarget {
        /**
         * 节点 Key
         */
        private String nodeKey;

        /**
         * 节点名称
         */
        private String nodeName;

        /**
         * 节点描述（如角色、类型）
         */
        private String description;

        public DslBackTarget(String nodeKey, String nodeName, String description) {
            this.nodeKey = nodeKey;
            this.nodeName = nodeName;
            this.description = description;
        }
    }

}
