package cn.gemrun.base.module.declare.dal.mysql.project;

import java.util.List;
import java.util.Set;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.mybatis.core.mapper.BaseMapperX;
import cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.gemrun.base.module.declare.dal.dataobject.project.ProjectDO;
import cn.gemrun.base.module.declare.controller.admin.project.vo.ProjectPageReqVO;
import cn.gemrun.base.module.declare.enums.ProjectStatus;
import org.apache.ibatis.annotations.Mapper;

/**
 * 已立项项目核心信息 Mapper
 *
 * @author Gemini
 */
@Mapper
public interface ProjectMapper extends BaseMapperX<ProjectDO> {

    /**
     * 根据备案ID查询项目列表
     *
     * @param filingId 备案ID
     * @return 项目列表
     */
    default List<ProjectDO> selectListByFilingId(Long filingId) {
        LambdaQueryWrapperX<ProjectDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(ProjectDO::getFilingId, filingId)
                .orderByDesc(ProjectDO::getId);
        return selectList(wrapper);
    }

    /**
     * 带数据权限过滤的查询
     *
     * @param reqVO         查询参数
     * @param businessIds   参与的业务ID列表
     * @return 分页结果
     */
    default PageResult<ProjectDO> selectPageWithDataPermission(ProjectPageReqVO reqVO, Set<Long> businessIds) {
        LambdaQueryWrapperX<ProjectDO> wrapper = new LambdaQueryWrapperX<>();

        // 条件：参与过的业务（businessIds 不为空时）
        if (businessIds != null && !businessIds.isEmpty()) {
            wrapper.in(ProjectDO::getId, businessIds);
        }

        wrapper.likeIfPresent(ProjectDO::getProjectName, reqVO.getProjectName())
                .eqIfPresent(ProjectDO::getProjectStatus, reqVO.getProjectStatus())
                .eqIfPresent(ProjectDO::getLeaderUserId, reqVO.getLeaderUserId())
                .eqIfPresent(ProjectDO::getLeaderMobile, reqVO.getLeaderMobile())
                .likeIfPresent(ProjectDO::getLeaderName, reqVO.getLeaderName())
                .betweenIfPresent(ProjectDO::getStartTime, reqVO.getStartTime())
                .betweenIfPresent(ProjectDO::getPlanEndTime, reqVO.getPlanEndTime())
                .betweenIfPresent(ProjectDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ProjectDO::getId);

        return selectPage(reqVO, wrapper);
    }

    /**
     * 不带数据权限过滤的查询（管理员用）
     */
    default PageResult<ProjectDO> selectPage(ProjectPageReqVO reqVO) {
        LambdaQueryWrapperX<ProjectDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.likeIfPresent(ProjectDO::getProjectName, reqVO.getProjectName())
                .eqIfPresent(ProjectDO::getProjectStatus, reqVO.getProjectStatus())
                .eqIfPresent(ProjectDO::getLeaderUserId, reqVO.getLeaderUserId())
                .eqIfPresent(ProjectDO::getLeaderMobile, reqVO.getLeaderMobile())
                .likeIfPresent(ProjectDO::getLeaderName, reqVO.getLeaderName())
                .betweenIfPresent(ProjectDO::getStartTime, reqVO.getStartTime())
                .betweenIfPresent(ProjectDO::getPlanEndTime, reqVO.getPlanEndTime())
                .betweenIfPresent(ProjectDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ProjectDO::getId);
        return selectPage(reqVO, wrapper);
    }

    /**
     * 根据项目状态查询项目列表
     *
     * @param status 项目状态（字典值）
     * @return 项目列表
     */
    default List<ProjectDO> selectListByStatus(String status) {
        LambdaQueryWrapperX<ProjectDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(ProjectDO::getProjectStatus, status)
                .orderByDesc(ProjectDO::getId);
        return selectList(wrapper);
    }

    /**
     * 获取需要验收检查的项目列表
     * 查询"建设中"或"中期评估"状态的项目
     *
     * @return 项目列表
     */
    default List<ProjectDO> selectListForAcceptanceCheck() {
        LambdaQueryWrapperX<ProjectDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.in(ProjectDO::getProjectStatus, ProjectStatus.CONSTRUCTION.getStatus(), ProjectStatus.MIDTERM.getStatus()) // 建设中，中期评估
                .orderByDesc(ProjectDO::getId);
        return selectList(wrapper);
    }

}
