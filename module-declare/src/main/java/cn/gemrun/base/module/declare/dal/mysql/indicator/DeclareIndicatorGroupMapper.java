package cn.gemrun.base.module.declare.dal.mysql.indicator;

import cn.gemrun.base.framework.mybatis.core.mapper.BaseMapperX;
import cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorGroupDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 指标分组 Mapper
 *
 * @author Gemini
 */
@Mapper
public interface DeclareIndicatorGroupMapper extends BaseMapperX<DeclareIndicatorGroupDO> {

    /**
     * 根据分组编码查询
     */
    default DeclareIndicatorGroupDO selectByGroupCode(String groupCode) {
        return selectOne(new LambdaQueryWrapperX<DeclareIndicatorGroupDO>()
                .eq(DeclareIndicatorGroupDO::getGroupCode, groupCode));
    }

    /**
     * 查询所有启用的分组（按排序）
     */
    default List<DeclareIndicatorGroupDO> selectAllEnabled() {
        return selectList(new LambdaQueryWrapperX<DeclareIndicatorGroupDO>()
                .eq(DeclareIndicatorGroupDO::getStatus, 1)
                .orderByAsc(DeclareIndicatorGroupDO::getSort));
    }

    /**
     * 根据父ID查询子分组
     */
    default List<DeclareIndicatorGroupDO> selectByParentId(Long parentId) {
        return selectList(new LambdaQueryWrapperX<DeclareIndicatorGroupDO>()
                .eq(parentId != null, DeclareIndicatorGroupDO::getParentId, parentId)
                .orderByAsc(DeclareIndicatorGroupDO::getSort));
    }

    /**
     * 根据项目类型查询一级分组
     */
    default List<DeclareIndicatorGroupDO> selectByProjectType(Integer projectType) {
        return selectList(new LambdaQueryWrapperX<DeclareIndicatorGroupDO>()
                .eq(DeclareIndicatorGroupDO::getProjectType, projectType)
                .orderByAsc(DeclareIndicatorGroupDO::getSort));
    }

    /**
     * 根据分组层级查询
     */
    default List<DeclareIndicatorGroupDO> selectByGroupLevel(Integer groupLevel) {
        return selectList(new LambdaQueryWrapperX<DeclareIndicatorGroupDO>()
                .eq(DeclareIndicatorGroupDO::getGroupLevel, groupLevel)
                .orderByAsc(DeclareIndicatorGroupDO::getSort));
    }

}
