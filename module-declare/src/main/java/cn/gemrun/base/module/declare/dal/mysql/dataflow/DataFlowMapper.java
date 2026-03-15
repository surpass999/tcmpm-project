package cn.gemrun.base.module.declare.dal.mysql.dataflow;

import java.util.*;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.gemrun.base.framework.mybatis.core.mapper.BaseMapperX;
import cn.gemrun.base.module.declare.dal.dataobject.dataflow.DataFlowDO;
import org.apache.ibatis.annotations.Mapper;
import cn.gemrun.base.module.declare.controller.admin.dataflow.vo.*;

/**
 * 数据流通记录 Mapper
 *
 * 数据权限：使用系统默认的部门数据权限
 *
 * @author
 */
@Mapper
public interface DataFlowMapper extends BaseMapperX<DataFlowDO> {

    /**
     * 分页查询
     */
    default PageResult<DataFlowDO> selectPage(DataFlowPageReqVO reqVO) {
        LambdaQueryWrapperX<DataFlowDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(DataFlowDO::getProjectId, reqVO.getProjectId())
                .likeIfPresent(DataFlowDO::getDataName, reqVO.getDataName())
                .eqIfPresent(DataFlowDO::getDataType, reqVO.getDataType())
                .eqIfPresent(DataFlowDO::getFlowType, reqVO.getFlowType())
                .eqIfPresent(DataFlowDO::getStatus, reqVO.getStatus())
                .eqIfPresent(DataFlowDO::getHasAchievement, reqVO.getHasAchievement())
                .betweenIfPresent(DataFlowDO::getCreateTime, reqVO.getCreateTime())
                .orderByDesc(DataFlowDO::getId);
        return selectPage(reqVO, wrapper);
    }

    /**
     * 根据项目ID查询
     */
    default List<DataFlowDO> selectByProjectId(Long projectId) {
        LambdaQueryWrapperX<DataFlowDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(DataFlowDO::getProjectId, projectId)
                .orderByDesc(DataFlowDO::getCreateTime);
        return selectList(wrapper);
    }

}
