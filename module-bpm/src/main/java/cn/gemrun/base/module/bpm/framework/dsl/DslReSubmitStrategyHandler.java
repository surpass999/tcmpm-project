package cn.gemrun.base.module.bpm.framework.dsl;

import cn.gemrun.base.module.bpm.framework.dsl.config.DslConfig;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * DSL 重新提交策略处理器
 * 基于 DSL 的 reSubmit 配置处理重新提交逻辑
 *
 * @author Gemini
 */
@Slf4j
@Component
public class DslReSubmitStrategyHandler {

    /**
     * 重新提交策略类型
     */
    public static final String RESTART = "RESTART";      // 重新开始：从第一个节点重新发起
    public static final String RESUME = "RESUME";        // 从当前节点继续

    /**
     * 获取重新提交策略
     *
     * @param dslConfig 节点 DSL 配置
     * @return 重新提交策略类型
     */
    public String getReSubmitStrategy(DslConfig dslConfig) {
        if (dslConfig == null || StrUtil.isBlank(dslConfig.getReSubmit())) {
            return RESUME; // 默认从当前节点继续
        }
        return dslConfig.getReSubmit();
    }

    /**
     * 判断是否需要重新开始流程
     *
     * @param dslConfig 节点 DSL 配置
     * @return 是否需要重新开始
     */
    public boolean isRestartRequired(DslConfig dslConfig) {
        return RESTART.equals(getReSubmitStrategy(dslConfig));
    }

    /**
     * 获取重新提交后的目标节点
     *
     * @param dslConfig 节点 DSL 配置
     * @param businessProcess 业务流程记录
     * @return 目标节点 Key，如果为 null 表示从第一个节点重新开始
     */
    public String getReSubmitTargetNode(DslConfig dslConfig, cn.gemrun.base.module.bpm.dal.dataobject.BpmBusinessProcessDO businessProcess) {
        String strategy = getReSubmitStrategy(dslConfig);

        if (RESTART.equals(strategy)) {
            // 重新开始：从第一个节点重新发起
            log.info("[DslReSubmitStrategyHandler] 重新提交策略: RESTART, 需要从第一个节点重新开始");
            return null;
        }

        // RESUME: 从当前节点继续
        // 需要先撤销当前任务，然后重新发起
        if (businessProcess != null) {
            String currentNodeKey = businessProcess.getCurrentNodeKey();
            log.info("[DslReSubmitStrategyHandler] 重新提交策略: RESUME, 从当前节点继续, nodeKey={}", currentNodeKey);
            return currentNodeKey;
        }

        return null;
    }

    /**
     * 从 DSL JSON 字符串获取重新提交策略
     *
     * @param dslJson DSL JSON 字符串
     * @return 重新提交策略类型
     */
    public String getReSubmitStrategyFromJson(String dslJson) {
        if (StrUtil.isBlank(dslJson)) {
            return RESUME;
        }

        try {
            JSONObject jsonObject = JSONUtil.parseObj(dslJson);
            String reSubmit = jsonObject.getStr("reSubmit");
            return StrUtil.isBlank(reSubmit) ? RESUME : reSubmit;
        } catch (Exception e) {
            log.warn("[DslReSubmitStrategyHandler] 解析重新提交策略失败: {}", e.getMessage());
            return RESUME;
        }
    }

    /**
     * 处理重新提交逻辑
     *
     * @param dslConfig 节点 DSL 配置
     * @param businessProcess 业务流程记录
     * @param processInstanceId 流程实例 ID
     * @return 重新提交结果
     */
    public ReSubmitResult handleReSubmit(DslConfig dslConfig,
                                        cn.gemrun.base.module.bpm.dal.dataobject.BpmBusinessProcessDO businessProcess,
                                        String processInstanceId) {
        String strategy = getReSubmitStrategy(dslConfig);

        ReSubmitResult result = new ReSubmitResult();
        result.setStrategy(strategy);
        result.setSuccess(true);

        if (RESTART.equals(strategy)) {
            // 重新开始：需要终止当前流程，从第一个节点重新发起
            result.setRestartRequired(true);
            result.setMessage("流程将重新开始，需要从第一个节点重新发起");
            log.info("[DslReSubmitStrategyHandler] 处理重新提交: strategy=RESTART, processInstanceId={}", processInstanceId);
        } else {
            // 从当前节点继续：需要先撤销当前任务，然后继续执行
            result.setRestartRequired(false);
            result.setTargetNodeKey(businessProcess != null ? businessProcess.getCurrentNodeKey() : null);
            result.setMessage("流程将从当前节点继续");
            log.info("[DslReSubmitStrategyHandler] 处理重新提交: strategy=RESUME, processInstanceId={}", processInstanceId);
        }

        return result;
    }

    /**
     * 重新提交结果
     */
    public static class ReSubmitResult {
        /**
         * 策略类型
         */
        private String strategy;

        /**
         * 是否成功
         */
        private boolean success;

        /**
         * 是否需要重新开始
         */
        private boolean restartRequired;

        /**
         * 目标节点 Key
         */
        private String targetNodeKey;

        /**
         * 结果消息
         */
        private String message;

        // Getters and Setters
        public String getStrategy() {
            return strategy;
        }

        public void setStrategy(String strategy) {
            this.strategy = strategy;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public boolean isRestartRequired() {
            return restartRequired;
        }

        public void setRestartRequired(boolean restartRequired) {
            this.restartRequired = restartRequired;
        }

        public String getTargetNodeKey() {
            return targetNodeKey;
        }

        public void setTargetNodeKey(String targetNodeKey) {
            this.targetNodeKey = targetNodeKey;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

}
