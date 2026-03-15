package cn.gemrun.base.module.declare.dal.mysql.project;

import java.util.List;
import java.util.Set;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.mybatis.core.mapper.BaseMapperX;
import cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.gemrun.base.module.declare.dal.dataobject.project.ProjectProcessDO;
import cn.gemrun.base.module.declare.controller.admin.project.vo.ProjectProcessPageReqVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目过程记录 Mapper
 *
 * @author Gemini
 */
@Mapper
public interface ProjectProcessMapper extends BaseMapperX<ProjectProcessDO> {

    /**
     * 根据项目ID查询过程记录列表
     *
     * @param projectId 项目ID
     * @return 过程记录列表
     */
    default List<ProjectProcessDO> selectListByProjectId(Long projectId) {
        LambdaQueryWrapperX<ProjectProcessDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(ProjectProcessDO::getProjectId, projectId)
                .orderByDesc(ProjectProcessDO::getReportTime);
        return selectList(wrapper);
    }

    /**
     * 带数据权限过滤的查询
     */
    default PageResult<ProjectProcessDO> selectPageWithDataPermission(ProjectProcessPageReqVO reqVO, Set<Long> businessIds) {
        LambdaQueryWrapperX<ProjectProcessDO> wrapper = new LambdaQueryWrapperX<>();

        if (businessIds != null && !businessIds.isEmpty()) {
            wrapper.in(ProjectProcessDO::getId, businessIds);
        }

        wrapper.eqIfPresent(ProjectProcessDO::getProjectId, reqVO.getProjectId())
                .eqIfPresent(ProjectProcessDO::getProcessType, reqVO.getProcessType())
                .eqIfPresent(ProjectProcessDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ProjectProcessDO::getProcessTitle, reqVO.getProcessTitle())
                .betweenIfPresent(ProjectProcessDO::getReportPeriodStart, reqVO.getReportPeriodStart())
                .betweenIfPresent(ProjectProcessDO::getReportPeriodEnd, reqVO.getReportPeriodEnd())
                .betweenIfPresent(ProjectProcessDO::getReportTime, reqVO.getReportTime())
                .betweenIfPresent(ProjectProcessDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ProjectProcessDO::getId);

        return selectPage(reqVO, wrapper);
    }

    /**
     * 根据项目ID查询过程记录
     */
    default PageResult<ProjectProcessDO> selectPageByProjectId(ProjectProcessPageReqVO reqVO) {
        LambdaQueryWrapperX<ProjectProcessDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(ProjectProcessDO::getProjectId, reqVO.getProjectId())
                .eqIfPresent(ProjectProcessDO::getProcessType, reqVO.getProcessType())
                .eqIfPresent(ProjectProcessDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ProjectProcessDO::getProcessTitle, reqVO.getProcessTitle())
                .betweenIfPresent(ProjectProcessDO::getReportPeriodStart, reqVO.getReportPeriodStart())
                .betweenIfPresent(ProjectProcessDO::getReportPeriodEnd, reqVO.getReportPeriodEnd())
                .betweenIfPresent(ProjectProcessDO::getReportTime, reqVO.getReportTime())
                .betweenIfPresent(ProjectProcessDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(ProjectProcessDO::getId);
        return selectPage(reqVO, wrapper);
    }

    /**
     * 检查是否存在指定项目、类型和年份的过程记录
     *
     * @param projectId   项目ID
     * @param processType 过程类型
     * @param year        年份
     * @return 是否存在
     */
    default boolean existsByProjectIdAndTypeAndYear(Long projectId, Integer processType, Integer year) {
        LambdaQueryWrapperX<ProjectProcessDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(ProjectProcessDO::getProjectId, projectId)
                .eq(ProjectProcessDO::getProcessType, processType);
        // 根据年份过滤
        if (year != null) {
            wrapper.apply("YEAR(report_period_start) = {0}", year);
        }
        return selectCount(wrapper) > 0;
    }

    /**
     * 检查是否存在指定项目和类型的过程记录
     *
     * @param projectId   项目ID
     * @param processType 过程类型
     * @return 是否存在
     */
    default boolean existsByProjectIdAndType(Long projectId, Integer processType) {
        LambdaQueryWrapperX<ProjectProcessDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(ProjectProcessDO::getProjectId, projectId)
                .eq(ProjectProcessDO::getProcessType, processType);
        return selectCount(wrapper) > 0;
    }

}
