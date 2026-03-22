package cn.gemrun.base.module.declare.dal.mysql.policy;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.mybatis.core.mapper.BaseMapperX;
import cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.gemrun.base.module.declare.controller.admin.policy.vo.PolicyPageReqVO;
import cn.gemrun.base.module.declare.dal.dataobject.policy.PolicyDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 政策通知 Mapper
 *
 * @author Gemini
 */
@Mapper
public interface PolicyMapper extends BaseMapperX<PolicyDO> {

    default PageResult<PolicyDO> selectPage(PolicyPageReqVO reqVO) {
        LambdaQueryWrapperX<PolicyDO> wrapper = new LambdaQueryWrapperX<>();

        // 关键词搜索：标题或摘要
        String keyword = reqVO.getKeyword();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w
                    .like(PolicyDO::getPolicyTitle, keyword)
                    .or()
                    .like(PolicyDO::getPolicySummary, keyword));
        }

        // 适用项目类型搜索（模糊匹配）
        String targetProjectTypes = reqVO.getTargetProjectTypes();
        if (targetProjectTypes != null && !targetProjectTypes.isEmpty()) {
            wrapper.like(PolicyDO::getTargetProjectTypes, targetProjectTypes);
        }

        wrapper.eqIfPresent(PolicyDO::getPolicyType, reqVO.getPolicyType())
                .eqIfPresent(PolicyDO::getReleaseDept, reqVO.getReleaseDept())
                .eqIfPresent(PolicyDO::getTargetScope, reqVO.getTargetScope())
                .eqIfPresent(PolicyDO::getStatus, reqVO.getStatus())
                .eqIfPresent(PolicyDO::getPublisherId, reqVO.getPublisherId())
                .geIfPresent(PolicyDO::getReleaseTime, reqVO.getReleaseTimeStart())
                .leIfPresent(PolicyDO::getReleaseTime, reqVO.getReleaseTimeEnd())
                .orderByDesc(PolicyDO::getReleaseTime);

        return selectPage(reqVO, wrapper);
    }

    /**
     * 查询已发布的政策列表（用于通知推送）
     */
    default List<PolicyDO> selectPublishedList() {
        return selectList(new LambdaQueryWrapperX<PolicyDO>()
                .eq(PolicyDO::getStatus, 1) // 已发布
                .orderByDesc(PolicyDO::getReleaseTime));
    }

    /**
     * 根据目标范围查询已发布政策
     */
    default List<PolicyDO> selectPublishedByScope(Integer targetScope) {
        return selectList(new LambdaQueryWrapperX<PolicyDO>()
                .eq(PolicyDO::getStatus, 1)
                .eq(PolicyDO::getTargetScope, targetScope)
                .orderByDesc(PolicyDO::getReleaseTime));
    }

}
