package cn.gemrun.base.module.declare.service.indicator;

import cn.gemrun.base.framework.common.exception.util.ServiceExceptionUtil;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.gemrun.base.module.declare.controller.admin.indicator.vo.DeclareIndicatorJointRulePageReqVO;
import cn.gemrun.base.module.declare.controller.admin.indicator.vo.DeclareIndicatorJointRuleSaveReqVO;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorDO;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorJointRuleDO;
import cn.gemrun.base.module.declare.dal.mysql.indicator.DeclareIndicatorJointRuleMapper;
import cn.gemrun.base.module.declare.dal.mysql.indicator.DeclareIndicatorMapper;
import cn.gemrun.base.module.declare.enums.ErrorCodeConstants;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 指标上期对比规则 Service 实现
 *
 * @author Gemini
 */
@Slf4j
@Service
public class DeclareIndicatorJointRuleServiceImpl implements DeclareIndicatorJointRuleService {

    /** 值类型：数字 */
    private static final int VALUE_TYPE_NUMBER = 1;
    /** 值类型：单选 */
    private static final int VALUE_TYPE_RADIO = 6;
    /** 值类型：多选 */
    private static final int VALUE_TYPE_MULTI_SELECT = 7;

    @Resource
    private DeclareIndicatorJointRuleMapper jointRuleMapper;

    @Resource
    private DeclareIndicatorMapper indicatorMapper;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Long createJointRule(DeclareIndicatorJointRuleSaveReqVO reqVO) {
        DeclareIndicatorJointRuleDO jointRule = BeanUtils.toBean(reqVO, DeclareIndicatorJointRuleDO.class);
        jointRuleMapper.insert(jointRule);
        return jointRule.getId();
    }

    @Override
    public void updateJointRule(DeclareIndicatorJointRuleSaveReqVO reqVO) {
        ValidateJointRuleExists(reqVO.getId());
        DeclareIndicatorJointRuleDO updateObj = BeanUtils.toBean(reqVO, DeclareIndicatorJointRuleDO.class);
        jointRuleMapper.updateById(updateObj);
    }

    @Override
    public void deleteJointRule(Long id) {
        ValidateJointRuleExists(id);
        jointRuleMapper.deleteById(id);
    }

    @Override
    public DeclareIndicatorJointRuleDO getJointRule(Long id) {
        return jointRuleMapper.selectById(id);
    }

    @Override
    public List<DeclareIndicatorJointRuleDO> getJointRuleList() {
        return jointRuleMapper.selectList();
    }

    @Override
    public PageResult<DeclareIndicatorJointRuleDO> getJointRulePage(DeclareIndicatorJointRulePageReqVO pageReqVO) {
        return jointRuleMapper.selectPage(pageReqVO,
                new LambdaQueryWrapperX<DeclareIndicatorJointRuleDO>()
                        .likeIfPresent(DeclareIndicatorJointRuleDO::getRuleName, pageReqVO.getRuleName())
                        .eqIfPresent(DeclareIndicatorJointRuleDO::getProjectType, pageReqVO.getProjectType())
                        .eqIfPresent(DeclareIndicatorJointRuleDO::getStatus, pageReqVO.getStatus())
                        .orderByDesc(DeclareIndicatorJointRuleDO::getId));
    }

    @Override
    public List<DeclareIndicatorJointRuleDO> getEnabledJointRules(Integer projectType, String processNode, String triggerTiming) {
        return jointRuleMapper.selectEnabledRules(projectType, processNode, triggerTiming);
    }

    @Override
    public DeclareIndicatorDO getIndicatorByCode(String indicatorCode) {
        return indicatorMapper.selectByIndicatorCode(indicatorCode);
    }

    private void ValidateJointRuleExists(Long id) {
        if (jointRuleMapper.selectById(id) == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.JOINT_RULE_NOT_EXISTS);
        }
    }

    // ==================== 上期值校验相关方法 ====================

    /**
     * 校验上期对比规则
     * @param currentValues 本期值 Map<indicatorCode, value>
     * @param lastPeriodValues 上期值 Map<indicatorCode, value>
     * @param ruleConfig 规则配置 JSON
     * @return 校验错误列表
     */
    @Override
    public List<PositiveRuleValidationError> validatePositiveRules(
            Map<String, Object> currentValues,
            Map<String, Object> lastPeriodValues,
            String ruleConfig) {
        List<PositiveRuleValidationError> errors = new ArrayList<>();

        if (StringUtils.isEmpty(ruleConfig)) {
            return errors;
        }

        try {
            PositiveRuleConfig config = objectMapper.readValue(ruleConfig, PositiveRuleConfig.class);
            if (config.getRules() == null || config.getRules().isEmpty()) {
                return errors;
            }

            for (PositiveRuleItem rule : config.getRules()) {
                PositiveRuleValidationError error = validateSinglePositiveRule(rule, currentValues, lastPeriodValues);
                if (error != null) {
                    errors.add(error);
                }
            }
        } catch (Exception e) {
            log.error("解析上期对比规则配置失败: {}", ruleConfig, e);
        }

        return errors;
    }

    /**
     * 校验单条上期对比规则
     */
    private PositiveRuleValidationError validateSinglePositiveRule(
            PositiveRuleItem rule,
            Map<String, Object> currentValues,
            Map<String, Object> lastPeriodValues) {

        String indicatorCode = rule.getIndicatorCode();
        if (indicatorCode == null) {
            return null;
        }

        Object currentValue = currentValues.get(indicatorCode);
        Object lastValue = lastPeriodValues.get(indicatorCode);

        // 如果上期值为空，跳过校验
        if (lastValue == null || StringUtils.isEmpty(lastValue.toString())) {
            return null;
        }

        // 如果当前值为空，跳过校验
        if (currentValue == null || StringUtils.isEmpty(currentValue.toString())) {
            return null;
        }

        Integer valueType = rule.getValueType();
        String compareMode = rule.getCompareMode();
        String compareType = rule.getCompareType();

        // 数字类型校验
        if (valueType != null && valueType == VALUE_TYPE_NUMBER) {
            return validateNumberRule(rule, currentValue, lastValue);
        }

        // 单选类型校验
        if (valueType != null && valueType == VALUE_TYPE_RADIO && "radio".equals(compareType)) {
            return validateRadioRule(rule, currentValue, lastValue);
        }

        // 多选类型校验
        if (valueType != null && valueType == VALUE_TYPE_MULTI_SELECT) {
            return validateMultiSelectRule(rule, currentValue, lastValue);
        }

        return null;
    }

    /**
     * 校验数字类型规则
     */
    private PositiveRuleValidationError validateNumberRule(
            PositiveRuleItem rule,
            Object currentValue,
            Object lastValue) {

        try {
            BigDecimal current = new BigDecimal(currentValue.toString());
            BigDecimal last = new BigDecimal(lastValue.toString());
            String compareMode = rule.getCompareMode();

            boolean valid = "positive".equals(compareMode)
                    ? current.compareTo(last) >= 0
                    : current.compareTo(last) <= 0;

            if (!valid) {
                String operator = "positive".equals(compareMode) ? ">=" : "<=";
                return new PositiveRuleValidationError(
                        rule.getName(),
                        rule.getIndicatorCode(),
                        String.format("当前值%s上期值（上期：%s，本期：%s）",
                                "positive".equals(compareMode) ? "不能小于" : "不能大于",
                                lastValue, currentValue)
                );
            }
        } catch (NumberFormatException e) {
            log.warn("数字类型校验失败，值格式错误: current={}, last={}", currentValue, lastValue);
        }

        return null;
    }

    /**
     * 校验单选类型规则
     * @param rule 规则配置
     * @param currentValue 本期值
     * @param lastValue 上期值
     * @return 校验错误信息
     */
    private PositiveRuleValidationError validateRadioRule(
            PositiveRuleItem rule,
            Object currentValue,
            Object lastValue) {

        List<PositiveRuleOption> options = rule.getOptions();
        if (options == null || options.isEmpty()) {
            return null;
        }

        // 构建等级映射：选项位置(0开始)即为等级（越靠上等级越高，数值越大）
        Map<String, Integer> levelMap = new HashMap<>();
        for (int i = 0; i < options.size(); i++) {
            levelMap.put(options.get(i).getValue(), i + 1);
        }

        Integer currentLevel = levelMap.get(currentValue.toString());
        Integer lastLevel = levelMap.get(lastValue.toString());

        if (currentLevel == null || lastLevel == null) {
            return null;
        }

        String compareMode = rule.getCompareMode();
        String currentLabel = getOptionLabel(options, currentValue.toString());
        String lastLabel = getOptionLabel(options, lastValue.toString());

        boolean valid;
        String errorMsg;

        if ("negative".equals(compareMode)) {
            // 负向：当前等级必须 <= 上期等级（数值越小越好）
            valid = currentLevel <= lastLevel;
            errorMsg = String.format("选项等级不能高于上期（上期：%s[等级%d]，本期：%s[等级%d]）",
                    lastLabel, lastLevel, currentLabel, currentLevel);
        } else {
            // 正向（默认）：当前等级必须 >= 上期等级（数值越大越好）
            valid = currentLevel >= lastLevel;
            errorMsg = String.format("选项等级不能低于上期（上期：%s[等级%d]，本期：%s[等级%d]）",
                    lastLabel, lastLevel, currentLabel, currentLevel);
        }

        if (!valid) {
            return new PositiveRuleValidationError(
                    rule.getName(),
                    rule.getIndicatorCode(),
                    errorMsg
            );
        }

        return null;
    }

    /**
     * 校验多选类型规则
     */
    private PositiveRuleValidationError validateMultiSelectRule(
            PositiveRuleItem rule,
            Object currentValue,
            Object lastValue) {

        List<PositiveRuleOption> options = rule.getOptions();
        String compareType = rule.getCompareType();
        Set<String> excludeSet = rule.getExcludeOptions() != null
                ? new HashSet<>(rule.getExcludeOptions())
                : new HashSet<>();

        // 解析选项值
        Set<String> currentSet = parseMultiSelectValue(currentValue);
        Set<String> lastSet = parseMultiSelectValue(lastValue);

        // 排除指定选项
        currentSet.removeAll(excludeSet);
        lastSet.removeAll(excludeSet);

        if ("min_level".equals(compareType)) {
            return validateMinLevelRule(rule, currentSet, lastSet, options, excludeSet);
        } else if ("max_level".equals(compareType)) {
            return validateMaxLevelRule(rule, currentSet, lastSet, options, excludeSet);
        } else if ("count".equals(compareType)) {
            return validateCountRule(rule, currentSet, lastSet);
        } else if ("new_count".equals(compareType)) {
            return validateNewCountRule(rule, currentSet, lastSet);
        } else if ("keep_required".equals(compareType)) {
            return validateKeepRequiredRule(rule, currentSet, lastSet);
        }

        return null;
    }

    /**
     * 最低等级校验
     */
    private PositiveRuleValidationError validateMinLevelRule(
            PositiveRuleItem rule,
            Set<String> currentSet,
            Set<String> lastSet,
            List<PositiveRuleOption> options,
            Set<String> excludeSet) {

        if (options == null || options.isEmpty()) {
            return null;
        }

        Map<String, Integer> levelMap = buildLevelMap(options, excludeSet);

        Integer currentMinLevel = getMinLevel(currentSet, levelMap);
        Integer lastMinLevel = getMinLevel(lastSet, levelMap);

        if (currentMinLevel == null || lastMinLevel == null) {
            return null;
        }

        if (currentMinLevel < lastMinLevel) {
            return new PositiveRuleValidationError(
                    rule.getName(),
                    rule.getIndicatorCode(),
                    "选中选项等级不能低于上期"
            );
        }

        return null;
    }

    /**
     * 最高等级校验
     */
    private PositiveRuleValidationError validateMaxLevelRule(
            PositiveRuleItem rule,
            Set<String> currentSet,
            Set<String> lastSet,
            List<PositiveRuleOption> options,
            Set<String> excludeSet) {

        if (options == null || options.isEmpty()) {
            return null;
        }

        Map<String, Integer> levelMap = buildLevelMap(options, excludeSet);

        Integer currentMaxLevel = getMaxLevel(currentSet, levelMap);
        Integer lastMaxLevel = getMaxLevel(lastSet, levelMap);

        if (currentMaxLevel == null || lastMaxLevel == null) {
            return null;
        }

        if (currentMaxLevel < lastMaxLevel) {
            return new PositiveRuleValidationError(
                    rule.getName(),
                    rule.getIndicatorCode(),
                    "选中选项等级不能低于上期"
            );
        }

        return null;
    }

    /**
     * 数量校验
     */
    private PositiveRuleValidationError validateCountRule(
            PositiveRuleItem rule,
            Set<String> currentSet,
            Set<String> lastSet) {

        String compareMode = rule.getCompareMode();
        boolean valid = "positive".equals(compareMode)
                ? currentSet.size() >= lastSet.size()
                : currentSet.size() <= lastSet.size();

        if (!valid) {
            return new PositiveRuleValidationError(
                    rule.getName(),
                    rule.getIndicatorCode(),
                    String.format("选中数量不能%s上期（上期：%d项，本期：%d项）",
                            "positive".equals(compareMode) ? "少于" : "多于",
                            lastSet.size(), currentSet.size())
            );
        }

        return null;
    }

    /**
     * 新增数量校验
     */
    private PositiveRuleValidationError validateNewCountRule(
            PositiveRuleItem rule,
            Set<String> currentSet,
            Set<String> lastSet) {

        Integer minNewCount = rule.getMinNewCount() != null ? rule.getMinNewCount() : 1;

        // 新增 = 本期 - (上期 ∩ 本期)
        Set<String> added = new HashSet<>(currentSet);
        added.removeAll(lastSet);

        if (added.size() < minNewCount) {
            return new PositiveRuleValidationError(
                    rule.getName(),
                    rule.getIndicatorCode(),
                    String.format("本期需新增至少%d个选项（当前新增：%d）", minNewCount, added.size())
            );
        }

        return null;
    }

    /**
     * 上期必须保持校验
     */
    private PositiveRuleValidationError validateKeepRequiredRule(
            PositiveRuleItem rule,
            Set<String> currentSet,
            Set<String> lastSet) {

        // 上期选中的选项本期必须继续保持选中
        Set<String> removed = new HashSet<>(lastSet);
        removed.removeAll(currentSet);

        if (!removed.isEmpty()) {
            return new PositiveRuleValidationError(
                    rule.getName(),
                    rule.getIndicatorCode(),
                    String.format("上期选中的选项 %s 已取消选择，必须保持选中", removed)
            );
        }

        return null;
    }

    // ==================== 辅助方法 ====================

    private String getOptionLabel(List<PositiveRuleOption> options, String value) {
        return options.stream()
                .filter(o -> o.getValue().equals(value))
                .findFirst()
                .map(PositiveRuleOption::getLabel)
                .orElse(value);
    }

    private Set<String> parseMultiSelectValue(Object value) {
        Set<String> result = new HashSet<>();
        if (value == null) {
            return result;
        }

        if (value instanceof List) {
            for (Object item : (List<?>) value) {
                result.add(item.toString());
            }
        } else if (value instanceof String) {
            String str = (String) value;
            if (str.startsWith("[")) {
                try {
                    List<String> list = objectMapper.readValue(str, new TypeReference<List<String>>() {});
                    result.addAll(list);
                } catch (Exception e) {
                    result.add(str);
                }
            } else {
                result.add(str);
            }
        } else {
            result.add(value.toString());
        }

        return result;
    }

    private Map<String, Integer> buildLevelMap(List<PositiveRuleOption> options, Set<String> excludeSet) {
        Map<String, Integer> levelMap = new HashMap<>();
        int level = 1;
        for (PositiveRuleOption option : options) {
            if (!excludeSet.contains(option.getValue())) {
                levelMap.put(option.getValue(), level);
            }
            level++;
        }
        return levelMap;
    }

    private Integer getMinLevel(Set<String> values, Map<String, Integer> levelMap) {
        Integer minLevel = null;
        for (String value : values) {
            Integer level = levelMap.get(value);
            if (level != null) {
                if (minLevel == null || level < minLevel) {
                    minLevel = level;
                }
            }
        }
        return minLevel;
    }

    private Integer getMaxLevel(Set<String> values, Map<String, Integer> levelMap) {
        Integer maxLevel = null;
        for (String value : values) {
            Integer level = levelMap.get(value);
            if (level != null) {
                if (maxLevel == null || level > maxLevel) {
                    maxLevel = level;
                }
            }
        }
        return maxLevel;
    }

    // ==================== 内部类：规则配置结构 ====================

    /**
     * 上期对比规则配置（对应 ruleConfig JSON）
     */
    public static class PositiveRuleConfig {
        private String groupName;
        private Integer priority;
        private List<PositiveRuleItem> rules;

        public String getGroupName() { return groupName; }
        public void setGroupName(String groupName) { this.groupName = groupName; }
        public Integer getPriority() { return priority; }
        public void setPriority(Integer priority) { this.priority = priority; }
        public List<PositiveRuleItem> getRules() { return rules; }
        public void setRules(List<PositiveRuleItem> rules) { this.rules = rules; }
    }

    /**
     * 单条上期对比规则
     */
    public static class PositiveRuleItem {
        private String name;
        private String indicatorCode;
        private Integer valueType;
        private String compareMode;
        private String compareType;
        private List<PositiveRuleOption> options;
        private List<String> excludeOptions;
        private Integer minNewCount;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getIndicatorCode() { return indicatorCode; }
        public void setIndicatorCode(String indicatorCode) { this.indicatorCode = indicatorCode; }
        public Integer getValueType() { return valueType; }
        public void setValueType(Integer valueType) { this.valueType = valueType; }
        public String getCompareMode() { return compareMode; }
        public void setCompareMode(String compareMode) { this.compareMode = compareMode; }
        public String getCompareType() { return compareType; }
        public void setCompareType(String compareType) { this.compareType = compareType; }
        public List<PositiveRuleOption> getOptions() { return options; }
        public void setOptions(List<PositiveRuleOption> options) { this.options = options; }
        public List<String> getExcludeOptions() { return excludeOptions; }
        public void setExcludeOptions(List<String> excludeOptions) { this.excludeOptions = excludeOptions; }
        public Integer getMinNewCount() { return minNewCount; }
        public void setMinNewCount(Integer minNewCount) { this.minNewCount = minNewCount; }
    }

    /**
     * 规则选项
     */
    public static class PositiveRuleOption {
        private String value;
        private String label;

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }
    }

}
