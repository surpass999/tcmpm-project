package cn.gemrun.base.module.bpm.framework.dsl;

import cn.gemrun.base.module.bpm.framework.dsl.config.DslAssign;
import cn.gemrun.base.module.bpm.framework.dsl.config.DslConfig;
import cn.gemrun.base.module.bpm.framework.dsl.config.DslVars;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * DSL 节点能力(Cap)处理器
 * 根据 DSL 的 cap 字段处理不同类型的节点逻辑
 *
 * @author Gemini
 */
@Slf4j
@Component
public class DslCapHandler {

    /**
     * 节点能力类型
     */
    public static final String AUDIT = "AUDIT";              // 单人审批
    public static final String COUNTERSIGN = "COUNTERSIGN";  // 会签
    public static final String EXPERT_SELECT = "EXPERT_SELECT"; // 选择专家
    public static final String FILL = "FILL";                // 填报
    public static final String MODIFY = "MODIFY";            // 补正
    public static final String CONFIRM = "CONFIRM";          // 确认
    public static final String ARCHIVE = "ARCHIVE";          // 归档
    public static final String PUBLISH = "PUBLISH";          // 发布

    /**
     * 获取节点能力
     *
     * @param dslConfig 节点 DSL 配置
     * @return 节点能力类型
     */
    public String getCap(DslConfig dslConfig) {
        if (dslConfig == null || dslConfig.getCap() == null) {
            return AUDIT; // 默认单人审批
        }
        return dslConfig.getCap().toUpperCase();
    }

    /**
     * 判断是否是审批节点
     *
     * @param dslConfig 节点 DSL 配置
     * @return 是否是审批节点
     */
    public boolean isAudit(DslConfig dslConfig) {
        return AUDIT.equals(getCap(dslConfig));
    }

    /**
     * 判断是否会签节点
     *
     * @param dslConfig 节点 DSL 配置
     * @return 是否会签节点
     */
    public boolean isCountersign(DslConfig dslConfig) {
        return COUNTERSIGN.equals(getCap(dslConfig));
    }

    /**
     * 判断是否选择专家节点
     *
     * @param dslConfig 节点 DSL 配置
     * @return 是否选择专家节点
     */
    public boolean isExpertSelect(DslConfig dslConfig) {
        return EXPERT_SELECT.equals(getCap(dslConfig));
    }

    /**
     * 判断是否填报节点
     *
     * @param dslConfig 节点 DSL 配置
     * @return 是否填报节点
     */
    public boolean isFill(DslConfig dslConfig) {
        return FILL.equals(getCap(dslConfig));
    }

    /**
     * 判断是否补正节点
     *
     * @param dslConfig 节点 DSL 配置
     * @return 是否补正节点
     */
    public boolean isModify(DslConfig dslConfig) {
        return MODIFY.equals(getCap(dslConfig));
    }

    /**
     * 判断是否确认节点
     *
     * @param dslConfig 节点 DSL 配置
     * @return 是否确认节点
     */
    public boolean isConfirm(DslConfig dslConfig) {
        return CONFIRM.equals(getCap(dslConfig));
    }

    /**
     * 获取会签规则
     *
     * @param dslConfig 节点 DSL 配置
     * @return 会签规则
     */
    public String getSignRule(DslConfig dslConfig) {
        if (dslConfig == null || dslConfig.getSignRule() == null) {
            return "ALL"; // 默认全部通过
        }
        return dslConfig.getSignRule();
    }

    /**
     * 判断会签是否需要全部通过
     *
     * @param dslConfig 节点 DSL 配置
     * @return 是否需要全部通过
     */
    public boolean isSignAllRequired(DslConfig dslConfig) {
        return "ALL".equals(getSignRule(dslConfig));
    }

    /**
     * 获取会签通过比例
     *
     * @param dslConfig 节点 DSL 配置
     * @return 通过比例 (0-100)
     */
    public int getSignThreshold(DslConfig dslConfig) {
        String signRule = getSignRule(dslConfig);
        switch (signRule) {
            case "ALL":
                return 100;
            case "ANY":
                return 0;
            case "MAJORITY":
                return 50;
            case "2/3":
                return 67;
            default:
                return 100;
        }
    }

    /**
     * 获取专家选择配置
     *
     * @param dslConfig 节点 DSL 配置
     * @return 专家选择配置
     */
    public ExpertSelectConfig getExpertSelectConfig(DslConfig dslConfig) {
        ExpertSelectConfig config = new ExpertSelectConfig();

        DslVars vars = dslConfig != null ? dslConfig.getVars() : null;
        if (vars != null) {
            config.setMin(vars.getMin() != null ? vars.getMin() : 0);
            config.setMax(vars.getMax() != null ? vars.getMax() : 999);
            config.setTargetVar(vars.getTargetVar());
        }

        return config;
    }

    /**
     * 获取补正可修改字段
     *
     * @param dslConfig 节点 DSL 配置
     * @return 可修改字段列表
     */
    public List<String> getModifyFields(DslConfig dslConfig) {
        if (dslConfig == null) {
            return null;
        }

        DslVars vars = dslConfig.getVars();
        if (vars != null) {
            return java.util.Arrays.asList(vars.getModifyFields());
        }

        return null;
    }

    /**
     * 判断是否需要选择专家
     *
     * @param dslConfig 节点 DSL 配置
     * @return 是否需要选择专家
     */
    public boolean requiresExpertSelection(DslConfig dslConfig) {
        return isExpertSelect(dslConfig);
    }

    /**
     * 专家选择配置
     */
    @Data
    public static class ExpertSelectConfig {
        /**
         * 最少选择数量
         */
        private int min;

        /**
         * 最多选择数量
         */
        private int max;

        /**
         * 目标变量名
         */
        private String targetVar;
    }

}
