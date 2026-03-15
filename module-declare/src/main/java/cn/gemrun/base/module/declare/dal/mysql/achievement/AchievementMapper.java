package cn.gemrun.base.module.declare.dal.mysql.achievement;

import java.util.*;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.gemrun.base.framework.mybatis.core.mapper.BaseMapperX;
import cn.gemrun.base.module.declare.dal.dataobject.achievement.AchievementDO;
import org.apache.ibatis.annotations.Mapper;
import cn.gemrun.base.module.declare.controller.admin.achievement.vo.*;

/**
 * 成果信息 Mapper
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
                .likeIfPresent(AchievementDO::getAchievementName, reqVO.getAchievementName())
                .eqIfPresent(AchievementDO::getAchievementType, reqVO.getAchievementType())
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

}
