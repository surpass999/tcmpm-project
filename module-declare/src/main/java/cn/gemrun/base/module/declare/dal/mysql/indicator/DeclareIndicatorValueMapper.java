package cn.gemrun.base.module.declare.dal.mysql.indicator;

import cn.gemrun.base.framework.mybatis.core.mapper.BaseMapperX;
import cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorValueDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 指标值 Mapper
 *
 * @author Gemini
 */
@Mapper
public interface DeclareIndicatorValueMapper extends BaseMapperX<DeclareIndicatorValueDO> {

    default List<DeclareIndicatorValueDO> selectByBusiness(Integer businessType, Long businessId) {
        return selectList(new LambdaQueryWrapperX<DeclareIndicatorValueDO>()
                .eq(DeclareIndicatorValueDO::getBusinessType, businessType)
                .eq(DeclareIndicatorValueDO::getBusinessId, businessId));
    }

    default List<DeclareIndicatorValueDO> selectByBusinessAndIndicatorCodes(Integer businessType, Long businessId, List<String> indicatorCodes) {
        return selectList(new LambdaQueryWrapperX<DeclareIndicatorValueDO>()
                .eq(DeclareIndicatorValueDO::getBusinessType, businessType)
                .eq(DeclareIndicatorValueDO::getBusinessId, businessId)
                .in(DeclareIndicatorValueDO::getIndicatorCode, indicatorCodes));
    }

    default DeclareIndicatorValueDO selectByBusinessAndIndicatorCode(Integer businessType, Long businessId, String indicatorCode) {
        return selectOne(new LambdaQueryWrapperX<DeclareIndicatorValueDO>()
                .eq(DeclareIndicatorValueDO::getBusinessType, businessType)
                .eq(DeclareIndicatorValueDO::getBusinessId, businessId)
                .eq(DeclareIndicatorValueDO::getIndicatorCode, indicatorCode));
    }

}
