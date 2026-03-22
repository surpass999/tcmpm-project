package cn.gemrun.base.module.declare.dal.mysql.indicator;

import cn.gemrun.base.framework.mybatis.core.mapper.BaseMapperX;
import cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorCaliberDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 指标口径 Mapper
 *
 * @author Gemini
 */
@Mapper
public interface DeclareIndicatorCaliberMapper extends BaseMapperX<DeclareIndicatorCaliberDO> {

    default List<DeclareIndicatorCaliberDO> selectByIndicatorId(Long indicatorId) {
        return selectList(new LambdaQueryWrapperX<DeclareIndicatorCaliberDO>()
                .eq(DeclareIndicatorCaliberDO::getIndicatorId, indicatorId));
    }

    default DeclareIndicatorCaliberDO selectByIndicatorIdSingle(Long indicatorId) {
        return selectOne(new LambdaQueryWrapperX<DeclareIndicatorCaliberDO>()
                .eq(DeclareIndicatorCaliberDO::getIndicatorId, indicatorId));
    }

}
