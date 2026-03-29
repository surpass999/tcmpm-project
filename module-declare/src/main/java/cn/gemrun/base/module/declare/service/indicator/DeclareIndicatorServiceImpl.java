package cn.gemrun.base.module.declare.service.indicator;

import cn.gemrun.base.framework.common.util.object.BeanUtils;
import cn.gemrun.base.framework.common.pojo.PageResult;
import cn.gemrun.base.module.declare.controller.admin.indicator.vo.*;
import cn.gemrun.base.module.declare.dal.dataobject.indicator.DeclareIndicatorDO;
import cn.gemrun.base.module.declare.dal.mysql.indicator.DeclareIndicatorMapper;
import cn.gemrun.base.module.declare.enums.ErrorCodeConstants;
import cn.gemrun.base.framework.common.exception.util.ServiceExceptionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 指标体系 Service 实现
 *
 */
@Slf4j
@Service
public class DeclareIndicatorServiceImpl implements DeclareIndicatorService {

    /**
     * 值类型：单选
     */
    private static final Integer VALUE_TYPE_RADIO = 6;
    /**
     * 值类型：多选
     */
    private static final Integer VALUE_TYPE_CHECKBOX = 7;
    /**
     * 值类型：动态容器
     */
    private static final Integer VALUE_TYPE_DYNAMIC_CONTAINER = 12;

    @Resource
    private DeclareIndicatorMapper indicatorMapper;

    @Override
    public Long createIndicator(DeclareIndicatorSaveReqVO createReqVO) {
        // 校验指标配置
        validateIndicator(createReqVO);

        // 校验指标代号唯一
        DeclareIndicatorDO existing = indicatorMapper.selectByIndicatorCode(createReqVO.getIndicatorCode());
        if (existing != null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.INDICATOR_CODE_EXISTS, createReqVO.getIndicatorCode());
        }

        // 插入
        DeclareIndicatorDO indicator = BeanUtils.toBean(createReqVO, DeclareIndicatorDO.class);
        indicatorMapper.insert(indicator);
        return indicator.getId();
    }

    @Override
    public void updateIndicator(DeclareIndicatorSaveReqVO updateReqVO) {
        // 校验存在
        DeclareIndicatorDO existing = indicatorMapper.selectById(updateReqVO.getId());
        if (existing == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.INDICATOR_NOT_EXISTS);
        }

        // 校验指标配置
        validateIndicator(updateReqVO);

        // 校验指标代号唯一
        if (!existing.getIndicatorCode().equals(updateReqVO.getIndicatorCode())) {
            DeclareIndicatorDO sameCode = indicatorMapper.selectByIndicatorCode(updateReqVO.getIndicatorCode());
            if (sameCode != null) {
                throw ServiceExceptionUtil.exception(ErrorCodeConstants.INDICATOR_CODE_EXISTS, updateReqVO.getIndicatorCode());
            }
        }

        // 更新
        DeclareIndicatorDO indicator = BeanUtils.toBean(updateReqVO, DeclareIndicatorDO.class);
        indicatorMapper.updateById(indicator);
    }

    /**
     * 校验指标配置
     */
    private void validateIndicator(DeclareIndicatorSaveReqVO reqVO) {
        Integer valueType = reqVO.getValueType();

        // 单选或多选时，选项定义不能为空
        if (VALUE_TYPE_RADIO.equals(valueType) || VALUE_TYPE_CHECKBOX.equals(valueType)) {
            if (!StringUtils.hasText(reqVO.getValueOptions())) {
                throw ServiceExceptionUtil.exception(ErrorCodeConstants.INDICATOR_VALUE_OPTIONS_REQUIRED);
            }
        }

        // 动态容器指标校验
        if (VALUE_TYPE_DYNAMIC_CONTAINER.equals(valueType)) {
            validateDynamicIndicatorFields(reqVO.getValueOptions());
        }
    }

    /**
     * 校验动态容器指标的子字段定义
     */
    private void validateDynamicIndicatorFields(String valueOptions) {
        if (!StringUtils.hasText(valueOptions)) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.DYNAMIC_INDICATOR_FIELDS_REQUIRED);
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode fields = mapper.readTree(valueOptions);

            if (!fields.isArray() || fields.isEmpty()) {
                throw ServiceExceptionUtil.exception(ErrorCodeConstants.DYNAMIC_INDICATOR_FIELDS_REQUIRED);
            }

            java.util.Set<String> fieldCodes = new java.util.HashSet<>();
            java.util.List<String> validFieldTypes = java.util.Arrays.asList(
                "text", "number", "textarea", "radio", "checkbox",
                "select", "multiSelect", "date", "dateRange"
            );

            for (com.fasterxml.jackson.databind.JsonNode field : fields) {
                String fieldCode = field.has("fieldCode") ? field.get("fieldCode").asText() : null;
                String fieldType = field.has("fieldType") ? field.get("fieldType").asText() : null;

                if (!StringUtils.hasText(fieldCode)) {
                    throw ServiceExceptionUtil.exception(ErrorCodeConstants.DYNAMIC_INDICATOR_FIELD_CODE_EMPTY);
                }

                if (!fieldCodes.add(fieldCode)) {
                    throw ServiceExceptionUtil.exception(
                        ErrorCodeConstants.DYNAMIC_INDICATOR_FIELD_CODE_DUPLICATE, fieldCode);
                }

                if (!StringUtils.hasText(fieldType) || !validFieldTypes.contains(fieldType)) {
                    throw ServiceExceptionUtil.exception(
                        ErrorCodeConstants.DYNAMIC_INDICATOR_FIELD_TYPE_INVALID, fieldType);
                }
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.DYNAMIC_INDICATOR_FIELD_FORMAT_ERROR);
        }
    }

    @Override
    public void deleteIndicator(Long id) {
        // 校验存在
        DeclareIndicatorDO existing = indicatorMapper.selectById(id);
        if (existing == null) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.INDICATOR_NOT_EXISTS);
        }
        // 删除
        indicatorMapper.deleteById(id);
    }

    @Override
    public DeclareIndicatorDO getIndicator(Long id) {
        return indicatorMapper.selectById(id);
    }

    @Override
    public PageResult<DeclareIndicatorDO> getIndicatorPage(DeclareIndicatorPageReqVO pageReqVO) {
        return indicatorMapper.selectPage(pageReqVO);
    }

    @Override
    public List<DeclareIndicatorDO> getIndicatorsByProjectType(Integer projectType, String businessType) {
        return indicatorMapper.selectByProjectTypeAndBusinessType(projectType, businessType);
    }

    @Override
    public List<DeclareIndicatorDO> getIndicatorsByBusinessType(String businessType) {
        return indicatorMapper.selectByBusinessType(businessType);
    }

    @Override
    public DeclareIndicatorDO getIndicatorByCode(String indicatorCode) {
        return indicatorMapper.selectByIndicatorCode(indicatorCode);
    }

    @Override
    public List<DeclareIndicatorDO> getIndicatorList(Set<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        return indicatorMapper.selectBatchIds(ids);
    }

    @Override
    public Map<Long, DeclareIndicatorDO> getIndicatorMap(Set<Long> ids) {
        List<DeclareIndicatorDO> list = getIndicatorList(ids);
        return list.stream().collect(Collectors.toMap(DeclareIndicatorDO::getId, v -> v, (v1, v2) -> v1));
    }

    @Override
    public List<DeclareIndicatorDO> getIndicatorsByGroupId(Long groupId) {
        return indicatorMapper.selectByGroupId(groupId);
    }

}
