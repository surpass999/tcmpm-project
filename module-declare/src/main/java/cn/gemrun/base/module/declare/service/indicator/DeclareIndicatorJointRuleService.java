package cn.gemrun.base.module.declare.service.indicator;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.declare.controller.admin.indicator.vo.DeclareIndicatorJointRulePageReqVO;
import cn.gemrun.base.module.declare.controller.admin.indicator.vo.DeclareIndicatorJointRuleSaveReqVO;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorDO;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorJointRuleDO;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 指标上期对比规则 Service 接口
 *
 * @author Gemini
 */
public interface DeclareIndicatorJointRuleService {

    /**
     * 创建上期对比规则
     */
    Long createJointRule(@Valid DeclareIndicatorJointRuleSaveReqVO reqVO);

    /**
     * 更新上期对比规则
     */
    void updateJointRule(@Valid DeclareIndicatorJointRuleSaveReqVO reqVO);

    /**
     * 删除上期对比规则
     */
    void deleteJointRule(Long id);

    /**
     * 获取上期对比规则
     */
    DeclareIndicatorJointRuleDO getJointRule(Long id);

    /**
     * 获取上期对比规则列表
     */
    List<DeclareIndicatorJointRuleDO> getJointRuleList();

    /**
     * 获取上期对比规则分页
     */
    PageResult<DeclareIndicatorJointRuleDO> getJointRulePage(DeclareIndicatorJointRulePageReqVO pageReqVO);

    /**
     * 根据项目类型获取启用的规则
     * @param projectType 项目类型，0表示全部项目
     * @param processNode 流程节点，可为空表示不限制
     * @param triggerTiming 触发时机，可为空表示不限制
     */
    List<DeclareIndicatorJointRuleDO> getEnabledJointRules(Integer projectType, String processNode, String triggerTiming);

    /**
     * 根据指标编码获取指标详情
     * @param indicatorCode 指标编码
     * @return 指标DO
     */
    DeclareIndicatorDO getIndicatorByCode(String indicatorCode);

    /**
     * 校验上期对比规则
     * @param currentValues 本期值 Map<indicatorCode, value>
     * @param lastPeriodValues 上期值 Map<indicatorCode, value>
     * @param ruleConfig 规则配置 JSON
     * @return 校验错误列表
     */
    List<PositiveRuleValidationError> validatePositiveRules(
            Map<String, Object> currentValues,
            Map<String, Object> lastPeriodValues,
            String ruleConfig);

    /**
     * 校验错误
     */
    class PositiveRuleValidationError {
        private final String ruleName;
        private final String indicatorCode;
        private final String message;

        public PositiveRuleValidationError(String ruleName, String indicatorCode, String message) {
            this.ruleName = ruleName;
            this.indicatorCode = indicatorCode;
            this.message = message;
        }

        public String getRuleName() { return ruleName; }
        public String getIndicatorCode() { return indicatorCode; }
        public String getMessage() { return message; }
    }

}
