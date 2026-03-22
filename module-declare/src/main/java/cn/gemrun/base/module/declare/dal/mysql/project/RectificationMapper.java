package cn.gemrun.base.module.declare.dal.mysql.project;

import java.util.List;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.mybatis.core.mapper.BaseMapperX;
import cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.gemrun.base.module.declare.dal.dataobject.project.RectificationDO;
import cn.gemrun.base.module.declare.controller.admin.project.vo.RectificationPageReqVO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 项目整改记录 Mapper
 *
 * @author Gemini
 */
@Mapper
public interface RectificationMapper extends BaseMapperX<RectificationDO> {

    /**
     * 根据项目ID查询整改记录列表
     *
     * @param projectId 项目ID
     * @return 整改记录列表
     */
    default List<RectificationDO> selectListByProjectId(Long projectId) {
        LambdaQueryWrapperX<RectificationDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(RectificationDO::getProjectId, projectId)
                .orderByDesc(RectificationDO::getDeadline);
        return selectList(wrapper);
    }

    /**
     * 根据过程记录ID查询整改记录列表
     *
     * @param processId 过程记录ID
     * @return 整改记录列表
     */
    default List<RectificationDO> selectListByProcessId(Long processId) {
        LambdaQueryWrapperX<RectificationDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(RectificationDO::getProcessId, processId)
                .orderByDesc(RectificationDO::getDeadline);
        return selectList(wrapper);
    }

    /**
     * 分页查询
     */
    default PageResult<RectificationDO> selectPage(RectificationPageReqVO reqVO) {
        LambdaQueryWrapperX<RectificationDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(RectificationDO::getProjectId, reqVO.getProjectId())
                .eqIfPresent(RectificationDO::getProcessId, reqVO.getProcessId())
                .eqIfPresent(RectificationDO::getRectifyType, reqVO.getRectifyType())
                .likeIfPresent(RectificationDO::getRectifyItem, reqVO.getRectifyItem())
                .eqIfPresent(RectificationDO::getCompleteStatus, reqVO.getCompleteStatus())
                .betweenIfPresent(RectificationDO::getDeadline, reqVO.getDeadline())
                .betweenIfPresent(RectificationDO::getCompleteTime, reqVO.getCompleteTime())
                .betweenIfPresent(RectificationDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(RectificationDO::getId);
        return selectPage(reqVO, wrapper);
    }

}
