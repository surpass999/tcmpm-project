package cn.gemrun.base.module.declare.dal.mysql.indicator;

import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.framework.mybatis.core.mapper.BaseMapperX;
import cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.gemrun.base.module.declare.controller.admin.indicator.vo.DeclareIndicatorPageReqVO;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 指标体系 Mapper
 *
 * @author Gemini
 */
@Mapper
public interface DeclareIndicatorMapper extends BaseMapperX<DeclareIndicatorDO> {

    default PageResult<DeclareIndicatorDO> selectPage(DeclareIndicatorPageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<DeclareIndicatorDO>()
                .likeIfPresent(DeclareIndicatorDO::getIndicatorCode, reqVO.getIndicatorCode())
                .likeIfPresent(DeclareIndicatorDO::getIndicatorName, reqVO.getIndicatorName())
                .eqIfPresent(DeclareIndicatorDO::getCategory, reqVO.getCategory())
                .eqIfPresent(DeclareIndicatorDO::getProjectType, reqVO.getProjectType())
                .eqIfPresent(DeclareIndicatorDO::getBusinessType, reqVO.getBusinessType())
                .orderByDesc(DeclareIndicatorDO::getId));
    }

    default List<DeclareIndicatorDO> selectByProjectTypeAndBusinessType(Integer projectType, String businessType) {
        return selectList(new LambdaQueryWrapperX<DeclareIndicatorDO>()
                .eq(DeclareIndicatorDO::getBusinessType, businessType)
                .and(wrapper -> wrapper
                        .eq(DeclareIndicatorDO::getProjectType, projectType)
                        .or()
                        .eq(DeclareIndicatorDO::getProjectType, 0))
                .orderByAsc(DeclareIndicatorDO::getCategory)
                .orderByAsc(DeclareIndicatorDO::getSort));
    }

    default List<DeclareIndicatorDO> selectByBusinessType(String businessType) {
        return selectList(new LambdaQueryWrapperX<DeclareIndicatorDO>()
                .eq(DeclareIndicatorDO::getBusinessType, businessType)
                .orderByAsc(DeclareIndicatorDO::getCategory)
                .orderByAsc(DeclareIndicatorDO::getSort));
    }

    default DeclareIndicatorDO selectByIndicatorCode(String indicatorCode) {
        return selectOne(new LambdaQueryWrapperX<DeclareIndicatorDO>()
                .eq(DeclareIndicatorDO::getIndicatorCode, indicatorCode));
    }

}
