package cn.gemrun.base.module.declare.service.indicator;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.declare.controller.admin.indicator.vo.DeclareIndicatorJointRulePageReqVO;
import cn.gemrun.base.module.declare.controller.admin.indicator.vo.DeclareIndicatorJointRuleSaveReqVO;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorDO;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorJointRuleDO;

import javax.validation.Valid;
import java.util.List;

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

}
