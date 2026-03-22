package cn.gemrun.base.module.declare.dal.mysql.project;

import cn.gemrun.base.framework.mybatis.core.mapper.BaseMapperX;
import cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.gemrun.base.module.declare.dal.dataobject.project.ProcessIndicatorConfigDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 过程类型指标配置 Mapper
 *
 * @author Gemini
 */
@Mapper
public interface ProcessIndicatorConfigMapper extends BaseMapperX<ProcessIndicatorConfigDO> {

    /**
     * 项目类型 - 通用/全部类型
     * 对应字典 declare_project_type 的 value 值
     */
    String COMPREHENSIVE_PROJECT_TYPE = "0";

    /**
     * 根据过程类型和项目类型查询已配置的指标
     */
    default List<ProcessIndicatorConfigDO> selectByProcessTypeAndProjectType(Integer processType, Integer projectType) {
        return selectList(new LambdaQueryWrapperX<ProcessIndicatorConfigDO>()
                .eq(ProcessIndicatorConfigDO::getProcessType, processType)
                .and(w -> w.eq(ProcessIndicatorConfigDO::getProjectType, projectType)
                        .or()
                        .eq(ProcessIndicatorConfigDO::getProjectType, Integer.parseInt(COMPREHENSIVE_PROJECT_TYPE))) // 也查询"全部"类型的配置
                .orderByAsc(ProcessIndicatorConfigDO::getSort));
    }

    /**
     * 根据过程类型删除配置
     */
    default int deleteByProcessType(Integer processType) {
        return delete(new LambdaQueryWrapperX<ProcessIndicatorConfigDO>()
                .eq(ProcessIndicatorConfigDO::getProcessType, processType));
    }

    /**
     * 根据过程类型和项目类型删除配置
     */
    default int deleteByProcessTypeAndProjectType(Integer processType, Integer projectType) {
        return delete(new LambdaQueryWrapperX<ProcessIndicatorConfigDO>()
                .eq(ProcessIndicatorConfigDO::getProcessType, processType)
                .eq(ProcessIndicatorConfigDO::getProjectType, projectType));
    }

}
