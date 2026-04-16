package cn.gemrun.base.module.declare.service.indicator;

import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorDO;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorValueDO;
import cn.gemrun.base.module.declare.dal.mysql.indicator.DeclareIndicatorMapper;
import cn.gemrun.base.module.declare.dal.mysql.DeclareProgressReportMapper;
import cn.gemrun.base.module.declare.dal.dataobject.progress.DeclareProgressReportDO;
import cn.gemrun.base.module.declare.dal.mysql.indicator.DeclareIndicatorValueMapper;
import cn.gemrun.base.framework.mybatis.core.query.LambdaQueryWrapperX;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
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

    @Resource
    private DeclareIndicatorMapper indicatorMapper;

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
                // 使用 UpdateWrapper 显式更新，可以处理 null 值的字段
                // 因为 MyBatis-Plus 的 updateById 默认忽略 null 字段
                com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DeclareIndicatorValueDO> updateWrapper =
                        new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<DeclareIndicatorValueDO>()
                                .eq(DeclareIndicatorValueDO::getId, existing.getId())
                                .set(value.getValueNum() != null, DeclareIndicatorValueDO::getValueNum, value.getValueNum())
                                .set(value.getValueNum() == null, DeclareIndicatorValueDO::getValueNum, null)
                                .set(value.getValueStr() != null, DeclareIndicatorValueDO::getValueStr, value.getValueStr())
                                .set(value.getValueStr() == null, DeclareIndicatorValueDO::getValueStr, null)
                                .set(value.getValueBool() != null, DeclareIndicatorValueDO::getValueBool, value.getValueBool())
                                .set(value.getValueBool() == null, DeclareIndicatorValueDO::getValueBool, null)
                                .set(value.getValueDate() != null, DeclareIndicatorValueDO::getValueDate, value.getValueDate())
                                .set(value.getValueDate() == null, DeclareIndicatorValueDO::getValueDate, null)
                                .set(value.getValueDateStart() != null, DeclareIndicatorValueDO::getValueDateStart, value.getValueDateStart())
                                .set(value.getValueDateStart() == null, DeclareIndicatorValueDO::getValueDateStart, null)
                                .set(value.getValueDateEnd() != null, DeclareIndicatorValueDO::getValueDateEnd, value.getValueDateEnd())
                                .set(value.getValueDateEnd() == null, DeclareIndicatorValueDO::getValueDateEnd, null)
                                .set(value.getValueText() != null, DeclareIndicatorValueDO::getValueText, value.getValueText())
                                .set(value.getValueText() == null, DeclareIndicatorValueDO::getValueText, null)
                                .set(DeclareIndicatorValueDO::getFillTime, value.getFillTime())
                                .set(DeclareIndicatorValueDO::getFillerId, value.getFillerId())
                                .set(DeclareIndicatorValueDO::getIsValid, value.getIsValid());
                indicatorValueMapper.update(null, updateWrapper);
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

    @Resource
    private DeclareProgressReportMapper progressReportMapper;

    @Override
    public Map<String, String> getLastPeriodIndicatorValues(Long hospitalId, Integer reportYear, Integer reportBatch, Integer businessType) {
        if (businessType == null) {
            businessType = 3; // 进度填报默认 businessType
        }
        // 计算上期：同一年度，上一批次；第1期则回退到上年第4期
        int lastBatch = reportBatch - 1;
        int lastYear = reportYear;
        if (lastBatch < 1) {
            lastBatch = 4;
            lastYear = reportYear - 1;
        }

        // 查询上期填报记录（跨年回退：reportYear -> lastYear）
        DeclareProgressReportDO lastReport = progressReportMapper.selectByHospitalAndPeriod(hospitalId, lastYear, lastBatch);
        if (lastReport == null) {
            return new HashMap<>();
        }

        // 根据传入的 businessType 查询（移除硬编码的 1，改为参数化）
        List<DeclareIndicatorValueDO> values = indicatorValueMapper.selectByBusiness(businessType, lastReport.getId());
        Map<String, String> result = new HashMap<>();
        for (DeclareIndicatorValueDO value : values) {
            String displayValue = getDisplayValue(value);
            if (displayValue != null) {
                result.put(value.getIndicatorCode(), displayValue);
            }
        }
        return result;
    }

    @Override
    public Map<String, Map<String, String>> getLastPeriodIndicatorValuesWithRaw(Long hospitalId, Integer reportYear, Integer reportBatch, Integer businessType) {
        if (businessType == null) {
            businessType = 3;
        }
        int lastBatch = reportBatch - 1;
        int lastYear = reportYear;
        if (lastBatch < 1) {
            lastBatch = 4;
            lastYear = reportYear - 1;
        }
        DeclareProgressReportDO lastReport = progressReportMapper.selectByHospitalAndPeriod(hospitalId, lastYear, lastBatch);
        if (lastReport == null) {
            Map<String, Map<String, String>> result = new HashMap<>();
            result.put("display", new HashMap<>());
            result.put("raw", new HashMap<>());
            return result;
        }
        List<DeclareIndicatorValueDO> values = indicatorValueMapper.selectByBusiness(businessType, lastReport.getId());
        Map<String, String> displayResult = new HashMap<>();
        Map<String, String> rawResult = new HashMap<>();
        for (DeclareIndicatorValueDO value : values) {
            String displayValue = getDisplayValue(value);
            if (displayValue != null) {
                displayResult.put(value.getIndicatorCode(), displayValue);
            }
            // 原始 valueStr（包含 inputType 的 ∵ 分隔符）
            if (value.getValueStr() != null) {
                rawResult.put(value.getIndicatorCode(), value.getValueStr());
            }
        }
        Map<String, Map<String, String>> result = new HashMap<>();
        result.put("display", displayResult);
        result.put("raw", rawResult);
        return result;
    }

    private String getDisplayValue(DeclareIndicatorValueDO value) {
        if (value == null) {
            return null;
        }
        switch (value.getValueType()) {
            case 1: // 数字
                return value.getValueNum() != null ? value.getValueNum().toPlainString() : null;
            case 2: // 字符串
                return value.getValueStr();
            case 9: { // 文件上传
                String valueStr = value.getValueStr();
                if (valueStr == null || valueStr.trim().isEmpty()) return null;
                // 解析 JSON 数组，提取文件名列表
                try {
                    ObjectMapper mapper = new ObjectMapper();
                    List<Map<String, Object>> files = mapper.readValue(valueStr,
                        new TypeReference<List<Map<String, Object>>>() {});
                    if (files == null || files.isEmpty()) return null;
                    List<String> fileNames = new java.util.ArrayList<>();
                    for (Map<String, Object> file : files) {
                        Object name = file.get("name");
                        if (name != null) {
                            fileNames.add(name.toString());
                        }
                    }
                    return String.join("、", fileNames);
                } catch (Exception e) {
                    // 解析失败，返回原始字符串
                    return valueStr;
                }
            }
            case 3: // 布尔
                if (value.getValueBool() != null) {
                    return value.getValueBool() ? "1" : "0";
                }
                return null;
            case 4: // 日期
                return value.getValueDate() != null
                    ? value.getValueDate().toLocalDate().toString()
                    : null;
            case 5: // 长文本
                if (value.getValueText() == null) {
                    return null;
                }
                return value.getValueText().length() > 50
                    ? value.getValueText().substring(0, 50) + "..."
                    : value.getValueText();
            case 6: // 单选 → 映射 value → label
                return mapSingleValueToLabel(value.getIndicatorId(), value.getValueStr());
            case 7: // 多选 → 映射 value → label，用顿号连接
            case 11: // 多选下拉
                return mapMultiValueToLabel(value.getIndicatorId(), value.getValueStr());
            case 8: // 日期区间
                if (value.getValueDateStart() == null && value.getValueDateEnd() == null) {
                    return null;
                }
                String start = value.getValueDateStart() != null
                    ? value.getValueDateStart().toLocalDate().toString() : "";
                String end = value.getValueDateEnd() != null
                    ? value.getValueDateEnd().toLocalDate().toString() : "";
                return start + " ~ " + end;
            case 10: // 单选下拉 → 映射 value → label
                return mapSingleValueToLabel(value.getIndicatorId(), value.getValueStr());
            case 12: // 动态容器 - 返回原始 JSON，前端负责 label 格式化
                return value.getValueStr();
            default:
                return value.getValueStr();
        }
    }

    /**
     * 单选/单选下拉：将原始值映射为 label
     */
    private String mapSingleValueToLabel(Long indicatorId, String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) return null;
        return getOptionLabelById(indicatorId, rawValue, false);
    }

    /**
     * 多选/多选下拉：将逗号分隔的原始值映射为 label，用顿号连接
     */
    private String mapMultiValueToLabel(Long indicatorId, String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) return null;
        return getOptionLabelById(indicatorId, rawValue, true);
    }

    /**
     * 根据指标ID和原始值，映射为 label（或 label 列表）
     * @param indicatorId 指标ID（唯一，不会跨医院重复）
     * @param rawValue 原始值（单选传 "1"，多选传 "1,2,3"，带 inputType 时格式为 "1∵输入内容,2"）
     * @param isMulti 是否多选模式
     * @return label（或用顿号连接的 label 列表），找不到则返回原始值
     */
    private String getOptionLabelById(Long indicatorId, String rawValue, boolean isMulti) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return isMulti ? "" : null;
        }
        DeclareIndicatorDO indicator = indicatorMapper.selectById(indicatorId);
        if (indicator == null || indicator.getValueOptions() == null || indicator.getValueOptions().trim().isEmpty()) {
            return isMulti ? rawValue : rawValue;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            // valueOptions 为数组格式 [{value:"1",label:"选项1"},...]
            TypeReference<List<Map<String, Object>>> typeRef =
                    new TypeReference<List<Map<String, Object>>>() {};
            List<Map<String, Object>> options = mapper.readValue(indicator.getValueOptions(), typeRef);
            if (options == null || options.isEmpty()) {
                return isMulti ? rawValue : rawValue;
            }
            if (isMulti) {
                StringBuilder sb = new StringBuilder();
                String[] parts = rawValue.split(",");
                for (int i = 0; i < parts.length; i++) {
                    // 去掉 inputType 的分隔符及其后面的内容，只保留纯 value
                    String trimmed = extractPureValue(parts[i].trim());
                    for (Map<String, Object> opt : options) {
                        if (String.valueOf(opt.get("value")).equals(trimmed)) {
                            sb.append(opt.get("label"));
                            break;
                        }
                    }
                    if (i < parts.length - 1) sb.append("、");
                }
                return sb.toString();
            } else {
                // 去掉 inputType 的分隔符及其后面的内容
                String pureValue = extractPureValue(rawValue);
                for (Map<String, Object> opt : options) {
                    if (String.valueOf(opt.get("value")).equals(pureValue)) {
                        return String.valueOf(opt.get("label"));
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[getOptionLabelById] 解析指标 options 失败: indicatorId={}", indicatorId, e);
        }
        return isMulti ? rawValue : rawValue;
    }

    /**
     * 提取纯 value（去掉 inputType 的 ∵ 分隔符及其后面的内容）
     * 例如："1∵输入内容" -> "1"
     */
    private String extractPureValue(String value) {
        if (value == null) return null;
        int idx = value.indexOf("∵");
        return idx >= 0 ? value.substring(0, idx) : value;
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
            case 12: // 动态容器 - 将复杂对象序列化为 JSON 字符串
                if (value instanceof Map) {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper =
                            new com.fasterxml.jackson.databind.ObjectMapper();
                        indicatorValue.setValueStr(mapper.writeValueAsString(value));
                    } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                        indicatorValue.setValueStr(value.toString());
                    }
                } else if (value instanceof String) {
                    indicatorValue.setValueStr((String) value);
                } else {
                    indicatorValue.setValueStr(value.toString());
                }
                break;
            default:
                log.warn("未知值类型: {}", valueType);
        }
    }

    // ==================== 动态容器（type=12）查询统计实现 ====================

    @Override
    public List<DeclareIndicatorValueDO> selectByContainerField(
            Integer businessType, Long businessId,
            String indicatorCode, String fieldCode, String fieldValue) {
        if (businessType == null || businessId == null) {
            return new java.util.ArrayList<>();
        }
        String jsonFieldValue;
        if (fieldValue == null) {
            jsonFieldValue = "null";
        } else if (fieldValue.matches("-?\\d+(\\.\\d+)?")) {
            jsonFieldValue = fieldValue;
        } else {
            jsonFieldValue = "\"" + fieldValue.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
        }
        return indicatorValueMapper.selectByContainerField(
                businessType, businessId, indicatorCode, fieldCode, jsonFieldValue);
    }

    @Override
    public Integer countContainerEntries(Integer businessType, Long businessId, String indicatorCode) {
        if (businessType == null || businessId == null) {
            return 0;
        }
        return indicatorValueMapper.sumContainerLength(businessType, businessId, indicatorCode);
    }

    @Override
    public Map<String, Long> countGroupByContainerField(
            Integer businessType, Long businessId,
            String indicatorCode, String fieldCode) {
        Map<String, Long> result = new HashMap<>();
        if (businessType == null || businessId == null) {
            return result;
        }
        List<DeclareIndicatorValueDO> records =
                indicatorValueMapper.selectByIndicatorCodeAndBusiness(businessType, businessId, indicatorCode);
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>> typeRef =
                new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {};

        for (DeclareIndicatorValueDO record : records) {
            if (record.getValueStr() == null || record.getValueStr().isEmpty()) {
                continue;
            }
            try {
                List<Map<String, Object>> items = mapper.readValue(record.getValueStr(), typeRef);
                for (Map<String, Object> item : items) {
                    Object fv = item.get(fieldCode);
                    String key = fv != null ? String.valueOf(fv) : "(空)";
                    result.merge(key, 1L, Long::sum);
                }
            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                log.warn("[countGroupByContainerField] JSON解析失败: id={}", record.getId(), e);
            }
        }
        return result;
    }

    @Override
    public Map<Long, Map<String, DeclareIndicatorValueDO>> getValueMapByReports(
            List<Long> reportIds, Integer businessType) {
        if (reportIds == null || reportIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // 1. 批量查询指标值
        LambdaQueryWrapperX<DeclareIndicatorValueDO> queryWrapper = new LambdaQueryWrapperX<>();
        queryWrapper.in(DeclareIndicatorValueDO::getBusinessId, reportIds)
                .eq(DeclareIndicatorValueDO::getBusinessType, businessType);

        List<DeclareIndicatorValueDO> allValues = indicatorValueMapper.selectList(queryWrapper);

        // 2. 按 reportId 和 indicatorCode 构建双层Map
        Map<Long, Map<String, DeclareIndicatorValueDO>> result = new LinkedHashMap<>();
        for (Long reportId : reportIds) {
            result.put(reportId, new LinkedHashMap<>());
        }

        for (DeclareIndicatorValueDO value : allValues) {
            Map<String, DeclareIndicatorValueDO> reportValues = result.get(value.getBusinessId());
            if (reportValues != null) {
                reportValues.put(value.getIndicatorCode(), value);
            }
        }

        return result;
    }

    /**
     * 将动态容器子字段的原始值转换为可读文本
     * @param rawValue 原始值（如 "1", "2,3"）
     * @param fieldCode 字段编码
     * @param indicatorCode 容器指标编码
     * @return 可读文本（如 "选项A" 或 "选项A、选项B"）
     */
    private String convertDynamicFieldValue(String rawValue, String fieldCode, Long indicatorId) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return null;
        }
        DeclareIndicatorDO indicator = indicatorMapper.selectById(indicatorId);
        if (indicator == null || indicator.getValueOptions() == null || indicator.getValueOptions().trim().isEmpty()) {
            return rawValue;
        }
        try {
            ObjectMapper mapper = new ObjectMapper();
            TypeReference<Map<String, Object>> typeRef =
                    new TypeReference<Map<String, Object>>() {};
            Map<String, Object> config = mapper.readValue(indicator.getValueOptions(), typeRef);
            if (config == null || !config.containsKey("fields")) {
                return rawValue;
            }
            List<Map<String, Object>> fields = (List<Map<String, Object>>) config.get("fields");
            for (Map<String, Object> field : fields) {
                if (!fieldCode.equals(String.valueOf(field.get("fieldCode")))) {
                    continue;
                }
                String fieldType = String.valueOf(field.get("fieldType"));
                List<Map<String, Object>> options = (List<Map<String, Object>>) field.get("options");
                if (options == null || options.isEmpty()) {
                    return rawValue;
                }
                // radio/select 类型
                if ("radio".equals(fieldType) || "select".equals(fieldType) || "multiSelect".equals(fieldType)) {
                    if (rawValue.contains(",")) {
                        // 多选
                        StringBuilder sb = new StringBuilder();
                        String[] parts = rawValue.split(",");
                        for (int i = 0; i < parts.length; i++) {
                            for (Map<String, Object> opt : options) {
                                if (String.valueOf(opt.get("value")).equals(parts[i])) {
                                    sb.append(opt.get("label"));
                                    break;
                                }
                            }
                            if (i < parts.length - 1) sb.append("、");
                        }
                        return sb.toString();
                    } else {
                        // 单选
                        for (Map<String, Object> opt : options) {
                            if (String.valueOf(opt.get("value")).equals(rawValue)) {
                                return String.valueOf(opt.get("label"));
                            }
                        }
                    }
                }
                return rawValue;
            }
        } catch (Exception e) {
            log.warn("[convertDynamicFieldValue] 解析子字段 options 失败: indicatorId={}, fieldCode={}", indicatorId, fieldCode, e);
        }
        return rawValue;
    }

}
