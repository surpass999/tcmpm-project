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
     * 更新指标值
     *
     * @param id     指标值ID
     * @param value  新值
     */
    void updateIndicatorValue(Long id, Object value);

}
