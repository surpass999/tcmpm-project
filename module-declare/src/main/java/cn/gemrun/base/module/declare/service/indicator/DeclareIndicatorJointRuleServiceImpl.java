package cn.gemrun.base.module.declare.service.indicator;

import cn.gemrun.base.framework.common.exception.util.ServiceExceptionUtil;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.gemrun.base.module.declare.controller.admin.indicator.vo.DeclareIndicatorJointRulePageReqVO;
import cn.gemrun.base.module.declare.controller.admin.indicator.vo.DeclareIndicatorJointRuleSaveReqVO;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorJointRuleDO;
import cn.gemrun.base.module.declare.dal.mysql.indicator.DeclareIndicatorJointRuleMapper;
import cn.gemrun.base.module.declare.enums.ErrorCodeConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 指标联合规则 Service 实现
 *
 * @author Gemini
 */
@Slf4j
@Service
public class DeclareIndicatorJointRuleServiceImpl implements DeclareIndicatorJointRuleService {

    @Resource
    private DeclareIndicatorJointRuleMapper jointRuleMapper;

    @Override
    public Long createJointRule(DeclareIndicatorJointRuleSaveReqVO reqVO) {
        DeclareIndicatorJointRuleDO jointRule = BeanUtils.toBean(reqVO, DeclareIndicatorJointRuleDO.class);
        jointRuleMapper.insert(jointRule);
        return jointRule.getId();
    }

    @Override
    public void updateJointRule(DeclareIndicatorJointRuleSaveReqVO reqVO) {
        // 校验存在
        ValidateJointRuleExists(reqVO.getId());

        // 更新
        DeclareIndicatorJointRuleDO updateObj = BeanUtils.toBean(reqVO, DeclareIndicatorJointRuleDO.class);
        jointRuleMapper.updateById(updateObj);
    }

    @Override
    public void deleteJointRule(Long id) {
        // 校验存在
        ValidateJointRuleExists(id);
        // 删除
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

    private void ValidateJointRuleExists(Long id) {
        if (jointRuleMapper.selectById(id) == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.JOINT_RULE_NOT_EXISTS);
        }
    }

}
