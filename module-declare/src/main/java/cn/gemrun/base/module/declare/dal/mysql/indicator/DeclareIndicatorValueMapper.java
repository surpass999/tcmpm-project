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
     * 根据业务类型、业务ID和分组查询指标值列表（XML实现）
     * 通过 INNER JOIN 关联 declare_indicator 表获取分组信息
     * @param businessType 业务类型：1=备案，2=立项，3=建设过程，4=年度总结，5=中期评估，6=验收申请，7=成果，8=流通交易
     * @param businessId 业务ID
     * @param groupId 分组ID
     * @return 指标值列表
     */
    List<DeclareIndicatorValueDO> selectByBusinessAndCategory(@Param("businessType") Integer businessType,
                                                             @Param("businessId") Long businessId,
                                                             @Param("groupId") Long groupId);

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

    /**
     * 查询动态容器（type=12）中 JSON 字段包含指定值的记录
     * 使用 MySQL JSON_CONTAINS 实现精确查询
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param indicatorCode 指标代号（可选）
     * @param fieldCode JSON 内字段名
     * @param jsonFieldValue JSON 格式的字段值（如 "\"字符串\"" 或 "123"）
     * @return 匹配的记录
     */
    List<DeclareIndicatorValueDO> selectByContainerField(
            @Param("businessType") Integer businessType,
            @Param("businessId") Long businessId,
            @Param("indicatorCode") String indicatorCode,
            @Param("fieldCode") String fieldCode,
            @Param("jsonFieldValue") String jsonFieldValue);

    /**
     * 统计动态容器总条目数量（基于 JSON_LENGTH 求和）
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param indicatorCode 指标代号（可选，为空则统计所有 type=12 的）
     * @return 总条目数量
     */
    Integer sumContainerLength(
            @Param("businessType") Integer businessType,
            @Param("businessId") Long businessId,
            @Param("indicatorCode") String indicatorCode);

    /**
     * 根据指标代号和业务信息查询指标值（用于 Java 层内存分组统计）
     * @param businessType 业务类型
     * @param businessId 业务ID
     * @param indicatorCode 指标代号（可选，为空则查询所有 type=12 的）
     * @return 指标值列表
     */
    List<DeclareIndicatorValueDO> selectByIndicatorCodeAndBusiness(
            @Param("businessType") Integer businessType,
            @Param("businessId") Long businessId,
            @Param("indicatorCode") String indicatorCode);

}
