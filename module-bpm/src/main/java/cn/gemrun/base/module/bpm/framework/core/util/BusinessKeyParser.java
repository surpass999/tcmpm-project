package cn.gemrun.base.module.bpm.framework.core.util;

import cn.hutool.core.lang.Assert;

import java.util.Objects;

/**
 * 统一 businessKey 格式解析器
 * <p>
 * 格式规范: {processDefinitionKey}_{businessId}
 * 例如: declare_filing_1, declare_project_half_year_101, achievement_submit_5
 *
 * @author gemrun
 */
public class BusinessKeyParser {

    private static final String SEPARATOR = "_";

    /**
     * 解析 businessKey，获取业务ID
     *
     * @param businessKey         businessKey
     * @param processDefinitionKey 流程定义Key（用于验证）
     * @return 业务ID
     */
    public static Long parseBusinessId(String businessKey, String processDefinitionKey) {
        Assert.notBlank(businessKey, "businessKey 不能为空");
        Assert.notBlank(processDefinitionKey, "流程定义Key不能为空");

        String prefix = processDefinitionKey + SEPARATOR;
        if (businessKey.startsWith(prefix)) {
            String businessIdStr = businessKey.substring(prefix.length());
            return Long.parseLong(businessIdStr);
        }

        // 兼容旧格式：直接取最后一段作为 businessId
        String[] parts = businessKey.split(SEPARATOR);
        if (parts.length > 0) {
            String lastPart = parts[parts.length - 1];
            if (lastPart.matches("\\d+")) {
                return Long.parseLong(lastPart);
            }
        }
        throw new IllegalArgumentException("businessKey 格式错误: " + businessKey + ", 期望前缀: " + prefix);
    }

    /**
     * 构建 businessKey
     *
     * @param processDefinitionKey 流程定义Key
     * @param businessId           业务ID
     * @return businessKey
     */
    public static String build(String processDefinitionKey, Long businessId) {
        Assert.notBlank(processDefinitionKey, "流程定义Key不能为空");
        Assert.notNull(businessId, "业务ID不能为空");
        return processDefinitionKey + SEPARATOR + businessId;
    }

    /**
     * 从 businessKey 中提取流程定义Key前缀
     *
     * @param businessKey businessKey
     * @return 流程定义Key前缀
     */
    public static String extractProcessDefinitionKey(String businessKey) {
        Assert.notBlank(businessKey, "businessKey 不能为空");

        String[] parts = businessKey.split(SEPARATOR);
        if (parts.length < 2) {
            return businessKey;
        }

        // 尝试找到最后一段是数字的下标，分隔符前面的是流程定义Key
        for (int i = parts.length - 1; i >= 0; i--) {
            if (!parts[i].matches("\\d+")) {
                // 找到第一个非数字部分，构建前缀
                StringBuilder prefix = new StringBuilder();
                for (int j = 0; j <= i; j++) {
                    if (j > 0) {
                        prefix.append(SEPARATOR);
                    }
                    prefix.append(parts[j]);
                }
                return prefix.toString();
            }
        }
        return businessKey;
    }

    /**
     * 验证 businessKey 格式是否正确
     *
     * @param businessKey         businessKey
     * @param processDefinitionKey 期望的流程定义Key
     * @return 是否匹配
     */
    public static boolean matches(String businessKey, String processDefinitionKey) {
        if (Objects.isNull(businessKey) || Objects.isNull(processDefinitionKey)) {
            return false;
        }
        return businessKey.startsWith(processDefinitionKey + SEPARATOR);
    }

    /**
     * 判断 businessKey 是否为新格式
     *
     * @param businessKey businessKey
     * @return true 表示是新格式（包含分隔符且最后一段是数字）
     */
    public static boolean isNewFormat(String businessKey) {
        if (Objects.isNull(businessKey) || businessKey.isEmpty()) {
            return false;
        }
        String[] parts = businessKey.split(SEPARATOR);
        return parts.length >= 2 && parts[parts.length - 1].matches("\\d+");
    }
}
