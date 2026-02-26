package cn.gemrun.base.module.declare.service.indicator;

import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorValueDO;
import cn.gemrun.base.module.declare.dal.mysql.indicator.DeclareIndicatorValueMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 指标值 Service 实现
 *
 * @author Gemini
 */
@Slf4j
@Service
public class DeclareIndicatorValueServiceImpl implements DeclareIndicatorValueService {

    @Resource
    private DeclareIndicatorValueMapper indicatorValueMapper;

    @Override
    @Transactional
    public void saveIndicatorValue(Integer businessType, Long businessId, Long indicatorId,
                                   String indicatorCode, Integer valueType, Object value, Long fillerId) {
        // 检查是否已存在
        DeclareIndicatorValueDO existing = indicatorValueMapper.selectByBusinessAndIndicatorCode(
                businessType, businessId, indicatorCode);

        DeclareIndicatorValueDO indicatorValue = new DeclareIndicatorValueDO();
        indicatorValue.setBusinessType(businessType);
        indicatorValue.setBusinessId(businessId);
        indicatorValue.setIndicatorId(indicatorId);
        indicatorValue.setIndicatorCode(indicatorCode);
        indicatorValue.setValueType(valueType);
        indicatorValue.setFillerId(fillerId);
        indicatorValue.setFillTime(LocalDateTime.now());
        indicatorValue.setIsValid(true);

        // 根据值类型设置值
        setValueByType(indicatorValue, valueType, value);

        if (existing != null) {
            // 更新
            indicatorValue.setId(existing.getId());
            indicatorValueMapper.updateById(indicatorValue);
        } else {
            // 新增
            indicatorValueMapper.insert(indicatorValue);
        }
    }

    @Override
    @Transactional
    public void batchSaveIndicatorValues(Integer businessType, Long businessId,
                                         List<DeclareIndicatorValueDO> values, Long fillerId) {
        for (DeclareIndicatorValueDO value : values) {
            value.setBusinessType(businessType);
            value.setBusinessId(businessId);
            value.setFillerId(fillerId);
            value.setFillTime(LocalDateTime.now());
            value.setIsValid(true);

            // 检查是否已存在
            DeclareIndicatorValueDO existing = indicatorValueMapper.selectByBusinessAndIndicatorCode(
                    businessType, businessId, value.getIndicatorCode());

            if (existing != null) {
                value.setId(existing.getId());
                indicatorValueMapper.updateById(value);
            } else {
                indicatorValueMapper.insert(value);
            }
        }
    }

    @Override
    public List<DeclareIndicatorValueDO> getIndicatorValues(Integer businessType, Long businessId) {
        return indicatorValueMapper.selectByBusiness(businessType, businessId);
    }

    @Override
    public Map<String, DeclareIndicatorValueDO> getIndicatorValueMap(Integer businessType, Long businessId) {
        List<DeclareIndicatorValueDO> values = getIndicatorValues(businessType, businessId);
        Map<String, DeclareIndicatorValueDO> map = new HashMap<>();
        for (DeclareIndicatorValueDO value : values) {
            map.put(value.getIndicatorCode(), value);
        }
        return map;
    }

    @Override
    @Transactional
    public void deleteIndicatorValues(Integer businessType, Long businessId) {
        List<DeclareIndicatorValueDO> values = indicatorValueMapper.selectByBusiness(businessType, businessId);
        for (DeclareIndicatorValueDO value : values) {
            indicatorValueMapper.deleteById(value.getId());
        }
    }

    @Override
    public void updateIndicatorValue(Long id, Object value) {
        DeclareIndicatorValueDO existing = indicatorValueMapper.selectById(id);
        Assert.notNull(existing, "指标值不存在");

        setValueByType(existing, existing.getValueType(), value);
        indicatorValueMapper.updateById(existing);
    }

    /**
     * 根据值类型设置值
     */
    private void setValueByType(DeclareIndicatorValueDO indicatorValue, Integer valueType, Object value) {
        if (value == null) {
            return;
        }

        // 值类型：1=数字，2=字符串，3=布尔，4=日期，5=长文本，6=单选，7=多选，8=日期区间，9=文件上传
        switch (valueType) {
            case 1: // 数字
                if (value instanceof Number) {
                    indicatorValue.setValueNum(BigDecimal.valueOf(((Number) value).doubleValue()));
                } else if (value instanceof String) {
                    indicatorValue.setValueNum(new BigDecimal((String) value));
                }
                break;
            case 2: // 字符串
            case 6: // 单选
            case 7: // 多选
            case 9: // 文件上传
                indicatorValue.setValueStr(value.toString());
                break;
            case 3: // 布尔
                if (value instanceof Boolean) {
                    indicatorValue.setValueBool((Boolean) value);
                } else if (value instanceof String) {
                    indicatorValue.setValueBool("true".equalsIgnoreCase((String) value) || "1".equals(value));
                }
                break;
            case 4: // 日期
                if (value instanceof LocalDateTime) {
                    indicatorValue.setValueDate((LocalDateTime) value);
                } else if (value instanceof String) {
                    indicatorValue.setValueDate(LocalDateTime.parse((String) value));
                }
                break;
            case 8: // 日期区间 - 特殊处理，value 可能是 Map 包含 start 和 end
                if (value instanceof Map) {
                    Map<String, Object> dateRange = (Map<String, Object>) value;
                    if (dateRange.get("start") != null) {
                        Object start = dateRange.get("start");
                        if (start instanceof LocalDateTime) {
                            indicatorValue.setValueDateStart((LocalDateTime) start);
                        } else if (start instanceof String) {
                            indicatorValue.setValueDateStart(LocalDateTime.parse((String) start));
                        }
                    }
                    if (dateRange.get("end") != null) {
                        Object end = dateRange.get("end");
                        if (end instanceof LocalDateTime) {
                            indicatorValue.setValueDateEnd((LocalDateTime) end);
                        } else if (end instanceof String) {
                            indicatorValue.setValueDateEnd(LocalDateTime.parse((String) end));
                        }
                    }
                }
                break;
            case 5: // 长文本
                indicatorValue.setValueText(value.toString());
                break;
            default:
                log.warn("未知值类型: {}", valueType);
        }
    }

}
