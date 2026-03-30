package cn.gemrun.base.module.declare.dal.mysql.achievement;

import java.util.*;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.gemrun.base.framework.mybatis.core.mapper.BaseMapperX;
import cn.gemrun.base.module.declare.dal.dataobject.achievement.AchievementDO;
import org.apache.ibatis.annotations.Mapper;
import cn.gemrun.base.module.declare.controller.admin.achievement.vo.*;

/**
 * 成果与流通 Mapper
 *
 * @author
 */
@Mapper
public interface AchievementMapper extends BaseMapperX<AchievementDO> {

    /**
     * 分页查询
     */
    default PageResult<AchievementDO> selectPage(AchievementPageReqVO reqVO) {
        LambdaQueryWrapperX<AchievementDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(AchievementDO::getProjectId, reqVO.getProjectId())
                .eqIfPresent(AchievementDO::getDeptId, reqVO.getDeptId())
                .likeIfPresent(AchievementDO::getAchievementName, reqVO.getAchievementName())
                .eqIfPresent(AchievementDO::getAchievementType, reqVO.getAchievementType())
                .likeIfPresent(AchievementDO::getDataName, reqVO.getDataName())
                .eqIfPresent(AchievementDO::getDataType, reqVO.getDataType())
                .eqIfPresent(AchievementDO::getFlowType, reqVO.getFlowType())
                .eqIfPresent(AchievementDO::getShareScope, reqVO.getShareScope())
                .eqIfPresent(AchievementDO::getDataQuality, reqVO.getDataQuality())
                .eqIfPresent(AchievementDO::getStatus, reqVO.getStatus())
                .eqIfPresent(AchievementDO::getAuditStatus, reqVO.getAuditStatus())
                .eqIfPresent(AchievementDO::getRecommendStatus, reqVO.getRecommendStatus())
                .betweenIfPresent(AchievementDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(AchievementDO::getId);
        return selectPage(reqVO, wrapper);
    }

    /**
     * 根据项目ID查询
     */
    default List<AchievementDO> selectByProjectId(Long projectId) {
        LambdaQueryWrapperX<AchievementDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(AchievementDO::getProjectId, projectId)
                .orderByDesc(AchievementDO::getCreateTime);
        return selectList(wrapper);
    }

    /**
     * 查询已纳入推广库的成果列表（公开接口）
     * 筛选条件：recommendStatus=2（已纳入推广库，遴选完成）
     */
    default List<AchievementDO> selectPublishedList() {
        LambdaQueryWrapperX<AchievementDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(AchievementDO::getRecommendStatus, 2)
                .orderByDesc(AchievementDO::getCreateTime);
        return selectList(wrapper);
    }

    /**
     * 查询已纳入推广库的成果详情（公开接口）
     * 筛选条件：recommendStatus=2（已纳入推广库，遴选完成）
     */
    default AchievementDO selectPublishedById(Long id) {
        LambdaQueryWrapperX<AchievementDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(AchievementDO::getId, id)
                .eq(AchievementDO::getRecommendStatus, 2);
        return selectOne(wrapper);
    }

    /**
     * 统计指定医院（deptId）的成果总数
     */
    default long selectCountByDeptId(Long deptId) {
        LambdaQueryWrapperX<AchievementDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(AchievementDO::getDeptId, deptId);
        return selectList(wrapper).size();
    }

    /**
     * 统计指定医院（deptId）已通过/已认定推广的成果数
     * 已认定推广：status=NATIONAL_APPROVED 或 recommendStatus=2
     */
    default long selectCountApprovedByDeptId(Long deptId) {
        LambdaQueryWrapperX<AchievementDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(AchievementDO::getDeptId, deptId)
                .and(w -> w.eq(AchievementDO::getStatus, "NATIONAL_APPROVED")
                        .or()
                        .eq(AchievementDO::getRecommendStatus, 2));
        return selectList(wrapper).size();
    }

}
