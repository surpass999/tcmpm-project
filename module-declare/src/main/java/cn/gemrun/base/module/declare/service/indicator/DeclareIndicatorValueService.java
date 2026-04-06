package cn.gemrun.base.module.declare.service.indicator;

import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorValueDO;

import java.util.List;
import java.util.Map;

/**
 * 指标值 Service 接口
 *
 * @author Gemini
 */
public interface DeclareIndicatorValueService {

    /**
     * 保存指标值
     *
     * @param businessType 业务类型
     * @param businessId   业务ID
     * @param indicatorId  指标ID
     * @param indicatorCode 指标代号
     * @param valueType    值类型
     * @param value        值（根据类型可以是 BigDecimal, String, Boolean, LocalDateTime 等）
     * @param fillerId     填报人ID
     */
    void saveIndicatorValue(Integer businessType, Long businessId, Long indicatorId,
                           String indicatorCode, Integer valueType, Object value, Long fillerId);

    /**
     * 批量保存指标值
     *
     * @param businessType 业务类型
     * @param businessId   业务ID
     * @param values       指标值列表 (indicatorId, indicatorCode, valueType, value)
     * @param fillerId     填报人ID
     */
    void batchSaveIndicatorValues(Integer businessType, Long businessId,
                                  List<DeclareIndicatorValueDO> values, Long fillerId);

    /**
     * 获取业务的所有指标值
     *
     * @param businessType 业务类型
     * @param businessId   业务ID
     * @return 指标值列表
     */
    List<DeclareIndicatorValueDO> getIndicatorValues(Integer businessType, Long businessId);

    /**
     * 获取业务的所有指标值（Map 形式，key 为 indicatorCode）
     *
     * @param businessType 业务类型
     * @param businessId   业务ID
     * @return 指标值 Map
     */
    Map<String, DeclareIndicatorValueDO> getIndicatorValueMap(Integer businessType, Long businessId);

    /**
     * 删除业务的所有指标值
     *
     * @param businessType 业务类型
     * @param businessId   业务ID
     */
    void deleteIndicatorValues(Integer businessType, Long businessId);

    /**
     * 获取填报指标值（上期）
     *
     * @param hospitalId 医院ID
     * @param reportYear  填报年度
     * @param reportBatch 填报批次
     * @param businessType 业务类型（默认为3：进度填报）
     * @return 上期指标值 Map（key=indicatorCode, value=valueNum）
     */
    Map<String, String> getLastPeriodIndicatorValues(Long hospitalId, Integer reportYear, Integer reportBatch, Integer businessType);

    /**
     * 更新指标值
     *
     * @param id     指标值ID
     * @param value  新值
     */
    void updateIndicatorValue(Long id, Object value);

    // ==================== 动态容器（type=12）查询统计方法 ====================

    /**
     * 查询动态容器中包含特定字段值的记录（MySQL JSON 函数，数据库层完成过滤）
     * @param businessType 业务类型
     * @param businessId   业务ID
     * @param indicatorCode 指标代号
     * @param fieldCode    JSON 内字段名
     * @param fieldValue   要匹配的值（自动转 JSON 字符串）
     * @return 匹配的记录列表
     */
    List<DeclareIndicatorValueDO> selectByContainerField(
            Integer businessType, Long businessId,
            String indicatorCode, String fieldCode, String fieldValue);

    /**
     * 统计动态容器总条目数量（基于 JSON_LENGTH）
     * @param businessType  业务类型
     * @param businessId    业务ID
     * @param indicatorCode 指标代号（可选，为空则统计所有 type=12 的）
     * @return 总条目数量
     */
    Integer countContainerEntries(Integer businessType, Long businessId, String indicatorCode);

    /**
     * 按动态容器指定字段分组统计（Java 层内存计算，用于无法在 SQL 层完成的 GROUP BY 场景）
     * @param businessType  业务类型
     * @param businessId    业务ID
     * @param indicatorCode 指标代号
     * @param fieldCode     分组字段名
     * @return 分组统计结果（fieldValue -> count）
     */
    Map<String, Long> countGroupByContainerField(
            Integer businessType, Long businessId,
            String indicatorCode, String fieldCode);

    /**
     * 根据填报记录ID列表批量查询指标值（用于导出）
     *
     * @param reportIds 填报记录ID列表
     * @param businessType 业务类型（Integer，如3=进度填报process）
     * @return Map<reportId, Map<indicatorCode, DeclareIndicatorValueDO>>
     */
    Map<Long, Map<String, DeclareIndicatorValueDO>> getValueMapByReports(
            List<Long> reportIds, Integer businessType);

}
