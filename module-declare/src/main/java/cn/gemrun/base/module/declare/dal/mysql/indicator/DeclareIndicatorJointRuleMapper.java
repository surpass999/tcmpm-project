package cn.gemrun.base.module.declare.dal.mysql.indicator;

import cn.gemrun.base.framework.mybatis.core.mapper.BaseMapperX;
import cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorJointRuleDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 指标联合规则 Mapper
 *
 * @author Gemini
 */
@Mapper
public interface DeclareIndicatorJointRuleMapper extends BaseMapperX<DeclareIndicatorJointRuleDO> {

    /**
     * 根据项目类型获取启用的规则
     * @param projectType 项目类型，0表示全部项目
     * @param processNode 流程节点，可为空表示不限制
     * @param triggerTiming 触发时机，可为空表示不限制
     */
    default List<DeclareIndicatorJointRuleDO> selectEnabledRules(Integer projectType, String processNode, String triggerTiming) {
        LambdaQueryWrapperX<DeclareIndicatorJointRuleDO> wrapper = new LambdaQueryWrapperX<DeclareIndicatorJointRuleDO>()
                .eq(DeclareIndicatorJointRuleDO::getStatus, 1); // 启用状态

        // 项目类型过滤：查询指定类型或通用类型(0)
        if (projectType != null) {
            wrapper.and(w -> w
                    .eq(DeclareIndicatorJointRuleDO::getProjectType, projectType)
                    .or()
                    .eq(DeclareIndicatorJointRuleDO::getProjectType, 0));
        }

        // 流程节点过滤
        if (processNode != null && !processNode.isEmpty()) {
            wrapper.and(w -> w
                    .eq(DeclareIndicatorJointRuleDO::getProcessNode, processNode)
                    .or()
                    .eq(DeclareIndicatorJointRuleDO::getProcessNode, ""));
        }

        // 触发时机过滤
        if (triggerTiming != null && !triggerTiming.isEmpty()) {
            wrapper.eq(DeclareIndicatorJointRuleDO::getTriggerTiming, triggerTiming);
        }

        return selectList(wrapper.orderByDesc(DeclareIndicatorJointRuleDO::getId));
    }

}
