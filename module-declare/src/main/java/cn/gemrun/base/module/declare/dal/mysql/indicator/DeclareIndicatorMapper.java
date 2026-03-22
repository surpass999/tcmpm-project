package cn.gemrun.base.module.declare.dal.mysql.indicator;

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

    /**
     * 项目类型 - 通用/全部类型
     */
    Integer COMPREHENSIVE_PROJECT_TYPE = 0;

    default PageResult<DeclareIndicatorDO> selectPage(DeclareIndicatorPageReqVO reqVO) {
        LambdaQueryWrapperX<DeclareIndicatorDO> wrapper = new LambdaQueryWrapperX<DeclareIndicatorDO>();
        
        // 搜索条件：indicatorCode 或 indicatorName 模糊匹配
        final String keyword = reqVO.getIndicatorCode();
        final String keyword2 = reqVO.getIndicatorName();
        if (keyword != null || keyword2 != null) {
            final String searchKey = keyword != null ? keyword : keyword2;
            wrapper.and(w -> w
                    .like(DeclareIndicatorDO::getIndicatorCode, searchKey)
                    .or()
                    .like(DeclareIndicatorDO::getIndicatorName, searchKey));
        }
        
        wrapper.eqIfPresent(DeclareIndicatorDO::getCategory, reqVO.getCategory())
                .eqIfPresent(DeclareIndicatorDO::getProjectType, reqVO.getProjectType())
                .eqIfPresent(DeclareIndicatorDO::getBusinessType, reqVO.getBusinessType())
                .orderByDesc(DeclareIndicatorDO::getId);
        
        return selectPage(reqVO, wrapper);
    }

    default List<DeclareIndicatorDO> selectByProjectTypeAndBusinessType(Integer projectType, String businessType) {
        return selectList(new LambdaQueryWrapperX<DeclareIndicatorDO>()
                .like(DeclareIndicatorDO::getBusinessType, businessType)  // 业务类型模糊匹配
                .and(wrapper -> wrapper
                        .eq(DeclareIndicatorDO::getProjectType, projectType)
                        .or()
                        .eq(DeclareIndicatorDO::getProjectType, COMPREHENSIVE_PROJECT_TYPE))  // 通用类型适用于所有项目类型
                .orderByAsc(DeclareIndicatorDO::getCategory)
                .orderByAsc(DeclareIndicatorDO::getSort));
    }

    default List<DeclareIndicatorDO> selectByBusinessType(String businessType) {
        return selectList(new LambdaQueryWrapperX<DeclareIndicatorDO>()
                .like(DeclareIndicatorDO::getBusinessType, businessType)
                .orderByAsc(DeclareIndicatorDO::getCategory)
                .orderByAsc(DeclareIndicatorDO::getSort));
    }

    default DeclareIndicatorDO selectByIndicatorCode(String indicatorCode) {
        return selectOne(new LambdaQueryWrapperX<DeclareIndicatorDO>()
                .eq(DeclareIndicatorDO::getIndicatorCode, indicatorCode));
    }

    /**
     * 根据指标分类查询指标列表
     * @param category 指标分类：1=基本情况，2=项目管理，3=系统功能，4=建设成效，5=数据集建设，6=数据交易，7=信息安全
     * @return 指标列表
     */
    default List<DeclareIndicatorDO> selectByCategory(Integer category) {
        return selectList(new LambdaQueryWrapperX<DeclareIndicatorDO>()
                .eq(DeclareIndicatorDO::getCategory, category)
                .orderByAsc(DeclareIndicatorDO::getSort));
    }

}
