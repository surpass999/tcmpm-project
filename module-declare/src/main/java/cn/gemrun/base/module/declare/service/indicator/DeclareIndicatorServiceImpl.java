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
import java.util.Objects;
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

        // 校验指标代号唯一（按项目类型校验）
        DeclareIndicatorDO existing = indicatorMapper.selectByIndicatorCodeAndProjectType(
                createReqVO.getIndicatorCode(), createReqVO.getProjectType());
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

        // 校验指标代号唯一（按项目类型校验，排除自己）
        if (!existing.getIndicatorCode().equals(updateReqVO.getIndicatorCode())
                || !Objects.equals(existing.getProjectType(), updateReqVO.getProjectType())) {
            DeclareIndicatorDO sameCode = indicatorMapper.selectByIndicatorCodeAndProjectType(
                    updateReqVO.getIndicatorCode(), updateReqVO.getProjectType());
            if (sameCode != null && !sameCode.getId().equals(updateReqVO.getId())) {
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
            validateDynamicIndicatorFields(reqVO.getValueOptions(), reqVO.getProjectType());
        }
    }

    /**
     * 校验动态容器指标的子字段定义
     */
    private void validateDynamicIndicatorFields(String valueOptions, Integer projectType) {
        if (!StringUtils.hasText(valueOptions)) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.DYNAMIC_INDICATOR_FIELDS_REQUIRED);
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(valueOptions);

            // 新对象格式：{ mode: "normal"|"conditional"|"autoEntry", link?: "xxx", fields: [...] }
            if (root.isObject()) {
                com.fasterxml.jackson.databind.JsonNode modeNode = root.get("mode");
                String mode = modeNode != null ? modeNode.asText() : "normal";

                // 自动条目容器：mode=autoEntry 且 link 必填
                if ("autoEntry".equals(mode)) {
                    com.fasterxml.jackson.databind.JsonNode linkNode = root.get("link");
                    if (linkNode == null || !StringUtils.hasText(linkNode.asText())) {
                        throw ServiceExceptionUtil.exception(
                            ErrorCodeConstants.DYNAMIC_INDICATOR_AUTO_ENTRY_LINK_REQUIRED);
                    }
                    // 校验关联指标：必须是普通指标（valueType != 12）且同项目类型
                    validateAutoEntryLink(linkNode.asText(), projectType);
                }

                // 校验 fields
                com.fasterxml.jackson.databind.JsonNode fieldsNode = root.get("fields");
                if (fieldsNode == null || !fieldsNode.isArray() || fieldsNode.isEmpty()) {
                    throw ServiceExceptionUtil.exception(ErrorCodeConstants.DYNAMIC_INDICATOR_FIELDS_REQUIRED);
                }
                validateContainerFields(fieldsNode, mapper);

                // 条件容器：校验 showCondition.watchField 必须在 fields 中
                if ("conditional".equals(mode)) {
                    validateConditionalShowConditions(fieldsNode, mapper);
                }
                return;
            }

            // 旧数组格式兼容：直接是字段数组
            if (!root.isArray() || root.isEmpty()) {
                throw ServiceExceptionUtil.exception(ErrorCodeConstants.DYNAMIC_INDICATOR_FIELDS_REQUIRED);
            }
            validateContainerFields(root, mapper);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw ServiceExceptionUtil.exception(ErrorCodeConstants.DYNAMIC_INDICATOR_FIELD_FORMAT_ERROR);
        }
    }

    /**
     * 校验容器字段列表
     */
    private void validateContainerFields(com.fasterxml.jackson.databind.JsonNode fieldsNode,
                                         com.fasterxml.jackson.databind.ObjectMapper mapper) {
        java.util.Set<String> fieldCodes = new java.util.HashSet<>();
        java.util.List<String> validFieldTypes = java.util.Arrays.asList(
            "text", "number", "textarea", "radio", "checkbox",
            "select", "multiSelect", "date", "dateRange", "boolean"
        );

        for (com.fasterxml.jackson.databind.JsonNode field : fieldsNode) {
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
    }

    /**
     * 校验条件容器的 showCondition.watchField 必须指向 fields 中的字段
     */
    private void validateConditionalShowConditions(com.fasterxml.jackson.databind.JsonNode fieldsNode,
                                                    com.fasterxml.jackson.databind.ObjectMapper mapper) {
        java.util.Set<String> fieldCodes = new java.util.HashSet<>();
        fieldsNode.forEach(f -> {
            if (f.has("fieldCode")) {
                fieldCodes.add(f.get("fieldCode").asText());
            }
        });

        for (com.fasterxml.jackson.databind.JsonNode field : fieldsNode) {
            com.fasterxml.jackson.databind.JsonNode conditionNode = field.get("showCondition");
            if (conditionNode != null && !conditionNode.isNull()) {
                com.fasterxml.jackson.databind.JsonNode watchFieldNode = conditionNode.get("watchField");
                if (watchFieldNode != null && StringUtils.hasText(watchFieldNode.asText())) {
                    String watchField = watchFieldNode.asText();
                    if (!fieldCodes.contains(watchField)) {
                        throw ServiceExceptionUtil.exception(
                            ErrorCodeConstants.DYNAMIC_INDICATOR_CONDITIONAL_FIELD_NOT_IN_CONTAINER, watchField);
                    }
                }
            }
        }
    }

    /**
     * 校验自动条目容器的关联指标：必须是普通指标（valueType != 12）且同项目类型
     */
    private void validateAutoEntryLink(String linkIndicatorCode, Integer projectType) {
        if (!StringUtils.hasText(linkIndicatorCode)) {
            return; // link 校验已在上一步完成
        }

        DeclareIndicatorDO linkedIndicator = indicatorMapper.selectByIndicatorCodeAndProjectType(linkIndicatorCode, projectType);
        if (linkedIndicator == null) {
            throw ServiceExceptionUtil.exception(
                ErrorCodeConstants.DYNAMIC_INDICATOR_AUTO_ENTRY_LINK_NOT_EXISTS, linkIndicatorCode);
        }

        // 不能是容器指标
        if (VALUE_TYPE_DYNAMIC_CONTAINER.equals(linkedIndicator.getValueType())) {
            throw ServiceExceptionUtil.exception(
                ErrorCodeConstants.DYNAMIC_INDICATOR_AUTO_ENTRY_LINK_CANNOT_BE_CONTAINER);
        }

        // 必须同项目类型
        if (!Objects.equals(linkedIndicator.getProjectType(), projectType)) {
            throw ServiceExceptionUtil.exception(
                ErrorCodeConstants.DYNAMIC_INDICATOR_AUTO_ENTRY_LINK_PROJECT_TYPE_MISMATCH);
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
    public List<DeclareIndicatorDO> getIndicators(Integer projectType, String businessType) {
        return indicatorMapper.selectByProjectTypeAndBusinessType(projectType, businessType);
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
