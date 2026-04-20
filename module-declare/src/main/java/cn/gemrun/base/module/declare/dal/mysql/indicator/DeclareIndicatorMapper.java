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
        
        wrapper.eqIfPresent(DeclareIndicatorDO::getProjectType, reqVO.getProjectType())
                .eqIfPresent(DeclareIndicatorDO::getBusinessType, reqVO.getBusinessType())
                .eqIfPresent(DeclareIndicatorDO::getGroupId, reqVO.getGroupId())
                .orderByAsc(DeclareIndicatorDO::getSort)
                .orderByAsc(DeclareIndicatorDO::getProjectType)
                .orderByAsc(DeclareIndicatorDO::getId);
        
        return selectPage(reqVO, wrapper);
    }

    default List<DeclareIndicatorDO> selectByProjectTypeAndBusinessType(Integer projectType, String businessType) {
        return selectList(new LambdaQueryWrapperX<DeclareIndicatorDO>()
                .like(DeclareIndicatorDO::getBusinessType, businessType)  // 业务类型模糊匹配
                .eq(DeclareIndicatorDO::getProjectType, projectType)
                .orderByAsc(DeclareIndicatorDO::getProjectType)
                .orderByAsc(DeclareIndicatorDO::getSort));
    }

    default List<DeclareIndicatorDO> selectByBusinessType(String businessType) {
        return selectList(new LambdaQueryWrapperX<DeclareIndicatorDO>()
                .like(DeclareIndicatorDO::getBusinessType, businessType)
                .orderByAsc(DeclareIndicatorDO::getProjectType)
                .orderByAsc(DeclareIndicatorDO::getSort));
    }

    default DeclareIndicatorDO selectByIndicatorCode(String indicatorCode) {
        return selectOne(new LambdaQueryWrapperX<DeclareIndicatorDO>()
                .eq(DeclareIndicatorDO::getIndicatorCode, indicatorCode));
    }
    /**
     * 根据指标代号和项目类型查询（用于校验唯一性）
     * @param indicatorCode 指标代号
     * @param projectType 项目类型（可为null）
     * @return 指标
     */
    default DeclareIndicatorDO selectByIndicatorCodeAndProjectType(String indicatorCode, Integer projectType) {
        return selectOne(new LambdaQueryWrapperX<DeclareIndicatorDO>()
                .eq(DeclareIndicatorDO::getIndicatorCode, indicatorCode)
                .eqIfPresent(DeclareIndicatorDO::getProjectType, projectType));
    }

    /**
     * 根据分组ID查询指标列表
     * @param groupId 分组ID
     * @return 指标列表
     */
    default List<DeclareIndicatorDO> selectByGroupId(Long groupId) {
        return selectList(new LambdaQueryWrapperX<DeclareIndicatorDO>()
                .eq(DeclareIndicatorDO::getGroupId, groupId)
                .orderByAsc(DeclareIndicatorDO::getSort));
    }

}
