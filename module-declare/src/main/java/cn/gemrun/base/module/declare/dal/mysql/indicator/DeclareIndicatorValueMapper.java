package cn.gemrun.base.module.declare.dal.mysql.indicator;

import cn.gemrun.base.framework.mybatis.core.mapper.BaseMapperX;
import cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorValueDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    /**
     * 根据业务类型、业务ID和指标分类查询指标值列表（XML实现）
     * 通过 INNER JOIN 关联 declare_indicator 表获取分类信息
     * @param businessType 业务类型：1=备案，2=立项，3=建设过程，4=年度总结，5=中期评估，6=验收申请，7=成果，8=流通交易
     * @param businessId 业务ID
     * @param category 指标分类：1=基本情况，2=项目管理，3=系统功能，4=建设成效，5=数据集建设，6=数据交易，7=信息安全
     * @return 指标值列表
     */
    List<DeclareIndicatorValueDO> selectByBusinessAndCategory(@Param("businessType") Integer businessType,
                                                             @Param("businessId") Long businessId,
                                                             @Param("category") Integer category);

    /**
     * 根据业务类型、业务ID和指标代号列表查询指标值列表（XML实现）
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param indicatorCodes 指标代号列表
     * @return 指标值列表
     */
    List<DeclareIndicatorValueDO> selectByIndicatorCodes(@Param("businessType") Integer businessType,
                                                        @Param("businessId") Long businessId,
                                                        @Param("indicatorCodes") List<String> indicatorCodes);

}
