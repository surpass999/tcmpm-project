<script lang="ts" setup>
/**
 * IndicatorInputTable 主入口组件
 * 整合所有 composables 和子组件，提供完整的指标填报功能
 *
 * 拆分后的结构：
 * - composables/useFormValues.ts      → 表单值 + 输入型选项
 * - composables/useContainerValues.ts → 容器值管理
 * - composables/useIndicatorData.ts   → 指标数据加载
 * - composables/useComputedIndicators.ts → 自动计算指标
 * - composables/useValidation.ts      → 统一错误结构 + 校验
 * - composables/useLogicRules.ts      → 逻辑规则
 * - composables/useInputTypeOptions.ts → 输入型选项
 * - composables/useFileUpload.ts       → 文件上传
 */

import { ref, computed, watch } from 'vue';
import dayjs from 'dayjs';

import { IconifyIcon, PlusOutlined } from '@vben/icons';
import { message } from 'ant-design-vue';
import { Upload } from 'ant-design-vue';

import { SafeNumberInput } from '#/components/safe-number-input';

import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import type { DynamicField } from './types';

// ==================== Composables 导入 ====================

// 统一错误结构
import {
  setFieldError,
  clearFieldError,
  clearAllErrors,
  toTopLevelKey,
  setDirty,
  getContainerFieldError,
  getTopLevelError,
  validateContainerFieldOnBlur,
  validateIndicator,
  validateAll,
  validateFilledData,
  markTopLevelDirty,
  markContainerFieldDirty,
} from './composables/useValidation';

// 错误 key 工具（toInputTypeKey）
import { toInputTypeKey, fieldErrors } from './composables/useErrorKeys';

// 表单值
import {
  formValues,
  isDirty,
  markDirty,
  resetDirty,
  inputTypeValues,
} from './composables/useFormValues';

// 容器值
import {
  containerValues,
  getContainerEntries,
  handleAddEntry,
  handleRemoveEntry,
  getVisibleFields,
  checkAndSyncLinkedAutoContainers,
  syncAllAutoEntryContainers,
  isAutoEntryVisible,
} from './composables/useContainerValues';

// 指标数据
import {
  indicators,
  lastPeriodValues,
  indicatorGroups,
  loadIndicatorData,
  reloadIndicatorData,
  loadJointRules,
} from './composables/useIndicatorData';

// 计算指标
import { recalculateComputedIndicators } from './composables/useComputedIndicators';

// 输入型选项
import {
  parseOptions,
  handleInputTypeRadioChange,
  handleCheckboxInputClick,
  getInputTypeRawValue,
  getInputTypeRawValues,
  getInputTypeOptionValue,
  getInputTypeOptionValues,
  isInputTypeOptionSelected,
  onInputTypeBlur,
} from './composables/useInputTypeOptions';

// 文件上传
import {
  fileListMap,
  handleFileUpload,
  handleFileRemove,
} from './composables/useFileUpload';

// 逻辑规则
import {
  validateLogicRules,
  validateLogicRuleForBlur,
} from './composables/useLogicRules';

// 工具函数
import {
  parseDynamicFields,
  getContainerType,
  getAutoEntryLink,
  extractEntryIndex,
} from './utils/container';
import {
  getNumberPrecision,
  getDateFormat,
  getBooleanLabels,
  getMaxLength,
  getShowSearch,
  getAcceptTypes,
  getMaxFileCount,
  getFileList,
  isRichText,
  parseExtraConfig,
} from './utils/indicator';

// 子组件
import ContainerFieldRenderer from './components/container-fields/ContainerFieldRenderer.vue';

// ==================== Props & Emits ====================

const props = withDefaults(
  defineProps<{
    hospitalId: number;
    projectType?: number;
    reportId?: number;
    reportYear?: number;
    reportBatch?: number;
    readonly?: boolean;
  }>(),
  {
    projectType: undefined,
    readonly: false,
  },
);

const emit = defineEmits<{
  loadingChange: [loading: boolean];
  update: [];
}>();

// ==================== 状态 ====================

const mounted = ref(false);

// ==================== 辅助函数 ====================

/** 获取输入型选项的错误信息（用 computed 包装确保响应式追踪） */
const inputTypeErrorMap = computed(() => {
  const result: Record<string, string | undefined> = {};
  for (const key of Object.keys(fieldErrors)) {
    result[key] = fieldErrors[key]?.message;
  }
  return result;
});

function getInputTypeError(indicatorCode: string, optionValue: string): string | null {
  const key = toInputTypeKey(indicatorCode, optionValue);
  return inputTypeErrorMap.value[key] ?? null;
}

const MAX_CONTAINER_ENTRIES = 99;

function isComputedIndicator(indicator: DeclareIndicatorApi.Indicator): boolean {
  return !!(indicator.calculationRule && indicator.calculationRule.trim());
}

function hasIndicatorSpec(indicator: DeclareIndicatorApi.Indicator): boolean {
  return !!(
    indicator.definition ||
    indicator.statisticScope ||
    indicator.dataSource ||
    indicator.fillRequire ||
    indicator.calculationExample
  );
}

/** 获取自动条目容器关联的指标名称 */
function getLinkedIndicatorName(indicator: any): string {
  const link = getAutoEntryLink(indicator.valueOptions);
  if (!link) return '';
  const linkedIndicator = indicators.value.find((i) => i.indicatorCode === link);
  return linkedIndicator?.indicatorName || link;
}

// ==================== 事件处理 ====================

function handleNumberBlur(indicator: DeclareIndicatorApi.Indicator, _event: Event) {
  if (indicator.id !== undefined) markTopLevelDirty(indicator.id);
  checkAndSyncLinkedAutoContainers(indicator.indicatorCode, indicators.value);
  // 先设置 _formValue，确保 validateIndicator 能读取到当前值
  (indicator as any)._formValue = formValues[indicator.indicatorCode];
  // 清除该指标的旧错误（如果值已修复则错误消失；仍有问题会被 validateIndicator 重新设置）
  if (indicator.id !== undefined) clearFieldError(toTopLevelKey(indicator.id));
  validateIndicator(indicator);
  setTimeout(() => {
    recalculateComputedIndicators(indicators.value);
    validateLogicRuleForBlur(indicator, indicators.value, setFieldError, clearFieldError, setDirty);
  }, 0);
}

function handleMultiSelectChange(indicator: any, selectedValues: string[]) {
  const prevValues = Array.isArray(formValues[indicator.indicatorCode]) ? [...formValues[indicator.indicatorCode]] : [];
  const options = parseOptions(indicator.valueOptions);
  const exclusiveValues = new Set(options.filter((o) => o.exclusive).map((o) => o.value));
  if (exclusiveValues.size === 0) {
    formValues[indicator.indicatorCode] = selectedValues;
    onIndicatorChange(indicator);
    return;
  }
  const prevSet = new Set(prevValues);
  const addedValues = selectedValues.filter((v) => !prevSet.has(v));
  let result: string[];
  if (addedValues.some((v) => exclusiveValues.has(v))) {
    const exclusiveAdded = addedValues.find((v) => exclusiveValues.has(v));
    result = exclusiveAdded !== undefined ? [exclusiveAdded] : [...selectedValues];
  } else if (addedValues.length > 0) {
    result = selectedValues.filter((v) => !exclusiveValues.has(v));
  } else {
    result = [...selectedValues];
  }
  formValues[indicator.indicatorCode] = result;
  onIndicatorChange(indicator);
}

function onIndicatorChange(indicator: DeclareIndicatorApi.Indicator) {
  if (indicator.id !== undefined) {
    markTopLevelDirty(indicator.id);
    const key = toTopLevelKey(indicator.id);
    clearFieldError(key);
  }
  if (indicator.valueType === 12) {
    const entries = containerValues[indicator.indicatorCode] || [];
    formValues[indicator.indicatorCode] = JSON.stringify(entries);
  }
  checkAndSyncLinkedAutoContainers(indicator.indicatorCode, indicators.value);
  // 设置 _formValue，确保 validateIndicator 能读取到当前值
  (indicator as any)._formValue = formValues[indicator.indicatorCode];
  validateIndicator(indicator);
  setTimeout(() => {
    validateLogicRuleForBlur(indicator, indicators.value, setFieldError, clearFieldError, setDirty);
  }, 0);
}

function onContainerFieldChange(indicator: DeclareIndicatorApi.Indicator, entry: any, field: DynamicField) {
  const containerType = getContainerType(indicator.valueOptions);
  markContainerFieldDirty(indicator.indicatorCode, entry.rowKey, field.fieldCode, containerType);
  validateContainerFieldOnBlur(entry, indicator, field, containerValues[indicator.indicatorCode] || []);
  setTimeout(() => {
    recalculateComputedIndicators(indicators.value);
    validateLogicRuleForBlur(indicator, indicators.value, setFieldError, clearFieldError, setDirty);
  }, 0);
}

/**
 * 容器字段变更（仅基础验证，用于 @field-change 事件）
 * 不触发逻辑验证，避免与 blur 重复
 */
function onContainerFieldBasicChange(indicator: DeclareIndicatorApi.Indicator, entry: any, field: DynamicField) {
  const containerType = getContainerType(indicator.valueOptions);
  markContainerFieldDirty(indicator.indicatorCode, entry.rowKey, field.fieldCode, containerType);
  validateContainerFieldOnBlur(entry, indicator, field, containerValues[indicator.indicatorCode] || []);
}

// ==================== 生命周期 ====================

import { onMounted } from 'vue';

onMounted(async () => {
  mounted.value = true;
  if (props.projectType === undefined) return;
  emit('loadingChange', true);
  try {
    clearAllErrors();
    await loadIndicatorData(props.projectType, props.reportId, props.hospitalId, props.reportYear, props.reportBatch);
    await loadJointRules(props.projectType);
    recalculateComputedIndicators(indicators.value);
    validateLogicRules(indicators.value, setFieldError, clearFieldError);
  } catch (error) {
    console.error('加载指标数据失败:', error);
    message.error('加载指标数据失败');
  } finally {
    emit('loadingChange', false);
  }
});

watch(() => props.projectType, async (newProjectType) => {
  if (newProjectType === undefined) return;
  emit('loadingChange', true);
  try {
    clearAllErrors();
    await reloadIndicatorData(newProjectType, props.reportId, props.hospitalId, props.reportYear, props.reportBatch);
    await loadJointRules(newProjectType);
    recalculateComputedIndicators(indicators.value);
    validateLogicRules(indicators.value, setFieldError, clearFieldError);
  } catch (error) {
    console.error('加载指标数据失败:', error);
    message.error('加载指标数据失败');
  } finally {
    emit('loadingChange', false);
  }
}, { immediate: false });

watch(formValues as any, () => {
  const hasAnyValue = Object.values(formValues).some((v) => v !== undefined && v !== null && v !== '');
  if (hasAnyValue) markDirty();
  emit('update');
}, { deep: true });

// ==================== 暴露 API ====================

function getContainerValuesResult(): Record<string, string> {
  const result: Record<string, string> = {};
  for (const ind of indicators.value) {
    if (ind.valueType === 12) result[ind.indicatorCode] = JSON.stringify(containerValues[ind.indicatorCode] || []);
  }
  return result;
}

function getAllIndicatorValues(): Array<{
  indicatorId: number;
  indicatorCode: string;
  valueType: number;
  valueStr?: string;
  valueNum?: string;
  valueText?: string;
  valueBool?: boolean;
  valueDate?: string;
  valueDateStart?: string;
  valueDateEnd?: string;
}> {
  const result: any[] = [];
  for (const ind of indicators.value) {
    const code = ind.indicatorCode;
    const vt = ind.valueType;
    const rawValue = formValues[code];
    if (rawValue === undefined || rawValue === '') continue;
    const item: any = { indicatorId: ind.id!, indicatorCode: code, valueType: vt };
    if (vt === 12) item.valueStr = JSON.stringify(containerValues[code] || []);
    else if (vt === 1) item.valueNum = rawValue !== null ? String(rawValue) : null;
    else if (vt === 2 || vt === 6 || vt === 9 || vt === 10) item.valueStr = String(rawValue);
    else if (vt === 3) item.valueBool = !!rawValue;
    else if (vt === 4) item.valueDate = dayjs.isDayjs(rawValue) ? rawValue.format('YYYY-MM-DD HH:mm:ss') : String(rawValue);
    else if (vt === 5) item.valueText = String(rawValue);
    else if (vt === 7 || vt === 11) item.valueStr = Array.isArray(rawValue) ? rawValue.join(',') : String(rawValue);
    else if (vt === 8 && Array.isArray(rawValue)) {
      item.valueDateStart = dayjs.isDayjs(rawValue[0]) ? rawValue[0].format('YYYY-MM-DD HH:mm:ss') : String(rawValue[0] || '');
      item.valueDateEnd = dayjs.isDayjs(rawValue[1]) ? rawValue[1].format('YYYY-MM-DD HH:mm:ss') : String(rawValue[1] || '');
    }
    result.push(item);
  }
  return result;
}

function syncContainerValuesToForm() {
  for (const ind of indicators.value) {
    if (ind.valueType === 12) formValues[ind.indicatorCode] = JSON.stringify(containerValues[ind.indicatorCode] || []);
  }
}

function doValidateAll() {
  const { messages } = validateAll(indicators.value, setFieldError, clearFieldError);
  // 追加逻辑规则校验结果
  const logicErrors = validateLogicRules(indicators.value, setFieldError, clearFieldError);
  const allMessages = [...messages, ...logicErrors];
  return allMessages;
}

function doValidateFilledData() {
  return validateFilledData(indicators.value, setFieldError, clearFieldError);
}

function getFillProgress(): { total: number; filled: number; percentage: number } {
  const all = indicators.value;
  let filled = 0;
  for (const ind of all) {
    if (ind.valueType === 12) {
      const entries = containerValues[ind.indicatorCode] || [];
      if (entries.length > 0) {
        const fields = parseDynamicFields(ind.valueOptions);
        const hasAllRequired = fields
          .filter((f) => f.required)
          .every((f) => {
            const key = `${entries[0].rowKey}${f.fieldCode}`;
            const val = entries[0][key];
            return val !== undefined && val !== null && val !== '';
          });
        if (hasAllRequired) filled++;
      }
    } else {
      const val = formValues[ind.indicatorCode];
      if (val !== undefined && val !== null && val !== '') filled++;
    }
  }
  return {
    total: all.length,
    filled,
    percentage: all.length > 0 ? Math.round((filled / all.length) * 100) : 0,
  };
}

defineExpose({
  getContainerValues: getContainerValuesResult,
  getAllIndicatorValues,
  getAllIndicators: () => indicators.value,
  validateAll: doValidateAll,
  validateFilledData: doValidateFilledData,
  recalculateComputedIndicators,
  syncContainerValuesToForm,
  syncAllAutoEntryContainers,
  containerValues,
  isDirty,
  resetDirty,
  getFillProgress,
});


</script>

<template>
  <div class="indicator-form-wrapper">
    <!-- 按分组显示指标 -->
    <div
      v-for="group in indicatorGroups"
      :key="group.groupId"
      class="indicator-category-section"
    >
      <div
        class="category-title"
        :class="{ 'category-title--level2': group.groupLevel === 2 }"
      >
        {{ group.groupPrefix ? group.groupPrefix + ' ' : '' }}{{ group.groupName }}
      </div>
      <div class="indicator-list">
        <div
          v-for="indicator in group.indicators"
          :key="indicator.id"
          :data-indicator-id="'in_' + indicator.id"
          :data-indicator-code="indicator.indicatorCode"
          class="indicator-row"
          :class="{ 'indicator-row--switch': indicator.valueType === 3 }"
        >
          <!-- 左侧：指标名称 + 输入组件 -->
          <div
            class="indicator-main"
            :class="{ 'indicator-main--inline': indicator.valueType === 3 || indicator.valueType === 6 || indicator.valueType === 7 }"
          >
            <!-- 指标标签行 -->
            <div class="indicator-label-row">
              <div class="flex items-center gap-1 flex-wrap">
                <!-- 有口径时用 Popover 展示 -->
                <a-popover
                  v-if="hasIndicatorSpec(indicator)"
                  placement="right"
                  trigger="click"
                  :overlay-style="{ maxWidth: '420px' }"
                >
                  <template #content>
                    <div class="caliber-popover-content">
                      <div v-if="indicator.definition" class="caliber-item">
                        <div class="caliber-label">指标定义</div>
                        <div class="caliber-value">{{ indicator.definition }}</div>
                      </div>
                      <div v-if="indicator.statisticScope" class="caliber-item">
                        <div class="caliber-label">统计范围</div>
                        <div class="caliber-value">{{ indicator.statisticScope }}</div>
                      </div>
                      <div v-if="indicator.dataSource" class="caliber-item">
                        <div class="caliber-label">数据来源</div>
                        <div class="caliber-value">{{ indicator.dataSource }}</div>
                      </div>
                      <div v-if="indicator.fillRequire" class="caliber-item">
                        <div class="caliber-label">填报要求</div>
                        <div class="caliber-value">{{ indicator.fillRequire }}</div>
                      </div>
                      <div v-if="indicator.calculationExample" class="caliber-item">
                        <div class="caliber-label">计算示例</div>
                        <div class="caliber-value">{{ indicator.calculationExample }}</div>
                      </div>
                    </div>
                  </template>
                  <span class="label-text has-caliber" :title="indicator.indicatorName">
                    {{ indicator.indicatorCode }} - {{ indicator.indicatorName }}
                    <IconifyIcon icon="lucide:help-circle" class="caliber-icon" />
                  </span>
                </a-popover>
                <span v-else class="label-text" :title="indicator.indicatorName">
                  {{ indicator.indicatorCode }} - {{ indicator.indicatorName }}
                </span>
              </div>
              <a-tag v-if="isComputedIndicator(indicator)" color="orange" class="computed-tag">自动计算</a-tag>
            </div>

            <!-- 指标输入组件行 -->
            <div
              class="indicator-input-row"
              :class="{ 'input-row--inline': indicator.valueType === 3 || indicator.valueType === 6 || indicator.valueType === 7 }"
            >
              <!-- 数字类型 -->
              <SafeNumberInput
                v-if="indicator.valueType === 1"
                v-model="formValues[indicator.indicatorCode]"
                :disabled="readonly || isComputedIndicator(indicator)"
                :placeholder="isComputedIndicator(indicator) ? '自动计算' : '请输入数字'"
                :min="indicator.minValue ?? 0"
                :max="indicator.maxValue"
                :precision="getNumberPrecision(indicator)"
                :suffix="indicator.unit || parseExtraConfig(indicator.extraConfig).suffix"
                class="w-full number-input-auto-width"
                @blur="(e: Event) => handleNumberBlur(indicator, e)"
              />

              <!-- 字符串类型 -->
              <a-input
                v-else-if="indicator.valueType === 2"
                v-model="formValues[indicator.indicatorCode]"
                :disabled="readonly"
                :placeholder="`请输入${indicator.indicatorName}`"
                :maxlength="getMaxLength(indicator)"
                show-count
                class="w-full"
                @change="() => onIndicatorChange(indicator)"
              />

              <!-- 布尔类型（开关） -->
              <a-switch
                v-else-if="indicator.valueType === 3"
                v-model="formValues[indicator.indicatorCode]"
                :disabled="readonly"
                :checked-children="getBooleanLabels(indicator).true"
                :un-checked-children="getBooleanLabels(indicator).false"
                class="switch-auto-width"
                @change="() => onIndicatorChange(indicator)"
              />

              <!-- 日期类型 -->
              <a-date-picker
                v-else-if="indicator.valueType === 4"
                v-model="formValues[indicator.indicatorCode]"
                :disabled="readonly"
                :show-time="getDateFormat(indicator).includes('HH')"
                :format="getDateFormat(indicator)"
                class="w-full"
                @change="() => onIndicatorChange(indicator)"
              />

              <!-- 长文本类型 -->
              <a-textarea
                v-else-if="indicator.valueType === 5"
                v-model="formValues[indicator.indicatorCode]"
                :disabled="readonly"
                :placeholder="`请输入${indicator.indicatorName}`"
                :rows="isRichText(indicator) ? 4 : 2"
                :maxlength="getMaxLength(indicator)"
                :show-count="!!getMaxLength(indicator)"
                class="w-full"
                @change="() => onIndicatorChange(indicator)"
              />

              <!-- 单选类型 -->
              <a-radio-group
                v-else-if="indicator.valueType === 6"
                :value="getInputTypeOptionValue(getInputTypeRawValue(indicator.indicatorCode))"
                :disabled="readonly"
                class="flex flex-wrap gap-x-4 gap-y-2"
                @update:value="(val: string) => handleInputTypeRadioChange(indicator, val)"
                @change="() => onIndicatorChange(indicator)"
              >
                <template v-for="opt in parseOptions(indicator.valueOptions)" :key="opt.value">
                  <div class="flex items-center">
                    <a-radio :value="opt.value" :disabled="readonly">
                      {{ opt.label }}
                    </a-radio>
                    <a-input
                      v-if="opt.inputType && isInputTypeOptionSelected(indicator.indicatorCode, formValues[indicator.indicatorCode], opt.value)"
                      size="small"
                      class="w-36 ml-2"
                      placeholder="请输入"
                      :value="inputTypeValues[indicator.indicatorCode + '_' + opt.value]"
                      :status="isInputTypeOptionSelected(indicator.indicatorCode, formValues[indicator.indicatorCode], opt.value) ? getInputTypeError(indicator.indicatorCode, opt.value) : null"
                      @click.stop
                      @input="(e: any) => inputTypeValues[indicator.indicatorCode + '_' + opt.value] = e.target.value"
                      @blur="(e: any) => onInputTypeBlur(indicator, opt, e.target.value)"
                    />
                    <span v-if="readonly && opt.inputType && inputTypeValues[indicator.indicatorCode + '_' + opt.value]" class="ml-1 text-gray-500">
                      （{{ inputTypeValues[indicator.indicatorCode + '_' + opt.value] }}）
                    </span>
                  </div>
                </template>
              </a-radio-group>

              <!-- 多选类型 -->
              <a-checkbox-group
                v-else-if="indicator.valueType === 7"
                :value="getInputTypeOptionValues(getInputTypeRawValues(indicator.indicatorCode))"
                :disabled="readonly"
                class="flex flex-wrap gap-x-4 gap-y-2"
              >
                <template v-for="opt in parseOptions(indicator.valueOptions)" :key="opt.value">
                  <div class="flex items-center">
                    <a-checkbox :value="opt.value" :disabled="readonly" @click="(e: MouseEvent) => { handleCheckboxInputClick(indicator, opt.value, e); onIndicatorChange(indicator); }">
                      {{ opt.label }}
                    </a-checkbox>
                    <a-input
                      v-if="opt.inputType && isInputTypeOptionSelected(indicator.indicatorCode, formValues[indicator.indicatorCode], opt.value)"
                      size="small"
                      class="w-36 ml-2"
                      placeholder="请输入"
                      :value="inputTypeValues[indicator.indicatorCode + '_' + opt.value]"
                      :status="isInputTypeOptionSelected(indicator.indicatorCode, formValues[indicator.indicatorCode], opt.value) ? getInputTypeError(indicator.indicatorCode, opt.value) : null"
                      @click.stop
                      @input="(e: any) => inputTypeValues[indicator.indicatorCode + '_' + opt.value] = e.target.value"
                      @blur="(e: any) => onInputTypeBlur(indicator, opt, e.target.value)"
                    />
                    <span v-if="readonly && opt.inputType && inputTypeValues[indicator.indicatorCode + '_' + opt.value]" class="ml-1 text-gray-500">
                      （{{ inputTypeValues[indicator.indicatorCode + '_' + opt.value] }}）
                    </span>
                  </div>
                </template>
              </a-checkbox-group>

              <!-- 单选下拉类型 -->
              <a-select
                v-else-if="indicator.valueType === 10"
                v-model="formValues[indicator.indicatorCode]"
                :disabled="readonly"
                :placeholder="`请选择${indicator.indicatorName}`"
                class="w-full"
                :show-search="getShowSearch(indicator)"
                allow-clear
                @change="() => onIndicatorChange(indicator)"
              >
                <a-select-option v-for="opt in parseOptions(indicator.valueOptions)" :key="opt.value" :value="opt.value">{{ opt.label }}</a-select-option>
              </a-select>

              <!-- 多选下拉类型 -->
              <a-select
                v-else-if="indicator.valueType === 11"
                :value="formValues[indicator.indicatorCode] || []"
                :disabled="readonly"
                :placeholder="`请选择${indicator.indicatorName}`"
                mode="multiple"
                class="w-full"
                :show-search="getShowSearch(indicator)"
                allow-clear
                @change="(vals: string[]) => handleMultiSelectChange(indicator, vals)"
              >
                <a-select-option v-for="opt in parseOptions(indicator.valueOptions)" :key="opt.value" :value="opt.value">{{ opt.label }}</a-select-option>
              </a-select>

              <!-- 日期区间类型 -->
              <a-range-picker
                v-else-if="indicator.valueType === 8"
                v-model="formValues[indicator.indicatorCode]"
                :disabled="readonly"
                :show-time="getDateFormat(indicator).includes('HH')"
                :format="getDateFormat(indicator)"
                class="w-full"
                @change="() => onIndicatorChange(indicator)"
              />

              <!-- 文件上传类型 -->
              <div v-else-if="indicator.valueType === 9" class="file-upload-wrapper">
                <div v-if="getFileList(fileListMap, indicator.indicatorCode).length > 0" class="file-list">
                  <div v-for="(file, index) in getFileList(fileListMap, indicator.indicatorCode)" :key="index" class="file-item">
                    <IconifyIcon icon="lucide:file-text" class="file-icon" />
                    <span class="file-name" :title="file.name">{{ file.name }}</span>
                    <button v-if="!readonly" type="button" class="file-delete-btn" @click="handleFileRemove(indicator.indicatorCode, file)">
                      <IconifyIcon icon="lucide:x" />
                    </button>
                  </div>
                </div>
                <Upload
                  v-if="!readonly"
                  :before-upload="(file: any) => handleFileUpload(file, indicator)"
                  :show-upload-list="false"
                  :disabled="getFileList(fileListMap, indicator.indicatorCode).length >= getMaxFileCount(indicator)"
                  :accept="getAcceptTypes(indicator) ? '.' + getAcceptTypes(indicator).replace(/,/g, ',.') : undefined"
                  multiple
                >
                  <a-button type="dashed" :disabled="getFileList(fileListMap, indicator.indicatorCode).length >= getMaxFileCount(indicator)">
                    <IconifyIcon icon="lucide:plus" />
                    上传文件
                  </a-button>
                </Upload>
                <div v-if="!readonly" class="upload-hint">
                  <span v-if="getAcceptTypes(indicator)">支持 {{ getAcceptTypes(indicator) }}</span>
                  <span v-if="getAcceptTypes(indicator) && getMaxFileCount(indicator)">，</span>
                  <span v-if="getMaxFileCount(indicator)">最多 {{ getMaxFileCount(indicator) }} 个</span>
                  <span class="upload-count">({{ getFileList(fileListMap, indicator.indicatorCode).length }}/{{ getMaxFileCount(indicator) }})</span>
                </div>
              </div>

              <!-- 动态容器类型 -->
              <div v-else-if="indicator.valueType === 12">
                <!-- 条件容器 -->
                <div v-show="getContainerType(indicator.valueOptions) === 'conditional'" class="conditional-container-form">
                  <div
                    v-for="entry in getContainerEntries(indicator.indicatorCode)"
                    :key="entry.rowKey"
                    class="dynamic-entry-row mb-4"
                  >
                    <div class="entry-fields space-y-3">
                      <ContainerFieldRenderer
                        v-for="field in getVisibleFields(indicator.indicatorCode, indicator.valueOptions, entry)"
                        :key="field.fieldCode"
                        :entry="entry"
                        :indicator-code="indicator.indicatorCode"
                        :row-key="entry.rowKey"
                        :field="field"
                        :disabled="readonly"
                        :container-field-error="getContainerFieldError(entry, indicator.indicatorCode, field.fieldCode, 'conditional')"
                        @blur="() => onContainerFieldChange(indicator, entry, field)"
                        @field-change="() => onContainerFieldBasicChange(indicator, entry, field)"
                      />
                    </div>
                  </div>
                </div>

                <!-- 自动条目容器 -->
                <div v-show="getContainerType(indicator.valueOptions) === 'autoEntry' && isAutoEntryVisible(indicator)" class="auto-entry-container-form">
                  <div v-for="entry in getContainerEntries(indicator.indicatorCode)" :key="entry.rowKey" class="dynamic-entry-row mb-4">
                    <div class="entry-header flex items-center justify-between mb-2">
                      <span class="entry-label text-gray-500 text-sm font-medium">{{ entry.rowKey }} {{ indicator.indicatorName }}</span>
                    </div>
                    <div class="entry-fields space-y-3">
                      <ContainerFieldRenderer
                        v-for="field in getVisibleFields(indicator.indicatorCode, indicator.valueOptions, entry)"
                        :key="field.fieldCode"
                        :entry="entry"
                        :indicator-code="indicator.indicatorCode"
                        :row-key="entry.rowKey"
                        :field="field"
                        :disabled="readonly"
                        :container-field-error="getContainerFieldError(entry, indicator.indicatorCode, field.fieldCode, 'autoEntry')"
                        @blur="() => onContainerFieldChange(indicator, entry, field)"
                        @field-change="() => onContainerFieldBasicChange(indicator, entry, field)"
                      />
                    </div>
                  </div>
                  <div class="text-gray-400 text-xs mt-2">（由「{{ getLinkedIndicatorName(indicator) }}」指标自动生成，数量不可调整）</div>
                </div>

                <!-- 普通动态容器 -->
                <div v-show="getContainerType(indicator.valueOptions) === 'normal'" class="dynamic-container-form">
                  <div v-for="entry in getContainerEntries(indicator.indicatorCode)" :key="entry.rowKey" class="dynamic-entry-row mb-4">
                    <div class="entry-header flex items-center justify-between mb-2">
                      <span class="entry-label text-gray-500 text-sm font-medium">
                        {{ entry.rowKey }} {{ indicator.indicatorName }}
                        <span class="text-gray-500 text-xs font-italic">{{ indicator.isRequired ? '必须填写此项内容' : '如无特殊情况，请勿删除条目' }}</span>
                      </span>
                      <a-button
                        type="text"
                        danger
                        size="small"
                        :disabled="extractEntryIndex(entry.rowKey) === 1 && indicator.isRequired"
                        :title="extractEntryIndex(entry.rowKey) === 1 && indicator.isRequired ? '必填项，第一条不可删除' : ''"
                        @click="handleRemoveEntry(indicator.indicatorCode, entry.rowKey)"
                      >
                        <template #icon><IconifyIcon icon="lucide:trash-2" /></template>
                        删除条目
                      </a-button>
                    </div>
                    <div class="entry-fields space-y-3">
                      <ContainerFieldRenderer
                        v-for="field in getVisibleFields(indicator.indicatorCode, indicator.valueOptions, entry)"
                        :key="field.fieldCode"
                        :entry="entry"
                        :indicator-code="indicator.indicatorCode"
                        :row-key="entry.rowKey"
                        :field="field"
                        :disabled="readonly"
                        :container-field-error="getContainerFieldError(entry, indicator.indicatorCode, field.fieldCode, 'normal')"
                        @blur="() => onContainerFieldChange(indicator, entry, field)"
                        @field-change="() => onContainerFieldBasicChange(indicator, entry, field)"
                      />
                    </div>
                  </div>
                  <a-button type="dashed" class="w-full mt-2" :disabled="readonly" @click="handleAddEntry(indicator.indicatorCode)">
                    <template #icon><PlusOutlined /></template>
                    添加条目
                  </a-button>
                  <div v-if="getContainerEntries(indicator.indicatorCode).length > 0" class="text-gray-400 text-xs mt-1 text-right">
                    共 {{ getContainerEntries(indicator.indicatorCode).length }} 个条目（上限 {{ MAX_CONTAINER_ENTRIES }}）
                  </div>
                </div>
              </div>

              <!-- 未知类型 -->
              <span v-else class="text-gray-400 text-sm">暂不支持该类型</span>

              <!-- 错误提示 -->
              <div
                v-if="indicator.valueType !== 12 && indicator.id && getTopLevelError(indicator.id)"
                class="indicator-error"
              >
                {{ getTopLevelError(indicator.id) }}
              </div>
            </div>
          </div>

          <!-- 右侧：上期值 -->
          <div v-if="lastPeriodValues[indicator.indicatorCode]" class="indicator-last-value">
            <div class="last-value-label">上期值</div>
            <div class="last-value-content">{{ lastPeriodValues[indicator.indicatorCode] }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 暂无指标数据时的空状态 -->
    <div
      v-if="mounted && indicatorGroups.length === 0"
      style="text-align: center; color: #999; margin-top: 40px; font-size: 14px"
    >
      暂无指标数据，请检查指标配置
    </div>
  </div>
</template>

<style scoped>
.indicator-form-wrapper {
  padding: 4px 0;
}

.indicator-category-section {
  margin-bottom: 32px;
}

.indicator-category-section:last-child {
  margin-bottom: 0;
}

.category-title {
  font-size: 15px;
  font-weight: 600;
  color: hsl(var(--foreground));
  margin-bottom: 14px;
  padding-bottom: 10px;
  padding-left: 12px;
  border-bottom: 2px solid hsl(var(--primary));
  border-left: 4px solid hsl(var(--primary));
  letter-spacing: 0.3px;
}

.category-title--level2 {
  font-size: 14px;
  color: hsl(var(--muted-foreground));
  border-bottom-color: hsl(var(--primary-light) / 0.5);
  border-left-color: hsl(var(--primary-light));
}

.indicator-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.indicator-row {
  display: flex;
  align-items: flex-start;
  gap: 0;
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: 10px;
  padding: 14px 16px;
  transition: border-color 0.2s, box-shadow 0.2s;
  position: relative;
  overflow: visible;
}

.indicator-row::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 3px;
  background: hsl(var(--primary));
  border-radius: 10px 0 0 10px;
  opacity: 0;
  transition: opacity 0.2s;
}

.indicator-row:hover {
  border-color: hsl(var(--primary) / 0.4);
  box-shadow: 0 2px 12px hsl(var(--primary) / 0.08);
}

.indicator-row:hover::before {
  opacity: 1;
}

.indicator-main {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-right: 30px;
  flex: 1;
  min-width: 0;
}

.indicator-main--inline {
  flex: none;
  align-self: center;
  width: 100%;
}

.indicator-label-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.label-text {
  font-size: 14px;
  font-weight: 500;
  color: hsl(var(--foreground));
  line-height: 1.4;
}

.label-text.has-caliber {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  font-weight: 600;
  transition: color 0.15s;
}

.label-text.has-caliber:hover {
  color: hsl(var(--primary-dark));
}

.caliber-icon {
  font-size: 13px;
  flex-shrink: 0;
  opacity: 0.7;
}

.caliber-popover-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 400px;
  overflow-y: auto;
  padding: 4px 0;
}

.caliber-item {
  display: flex;
  flex-direction: column;
  gap: 3px;
}

.caliber-label {
  font-size: 12px;
  font-weight: 600;
  color: hsl(var(--muted-foreground));
  line-height: 1.4;
}

.caliber-value {
  font-size: 13px;
  color: hsl(var(--foreground));
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}

.computed-tag {
  font-size: 11px;
  padding: 0 6px;
  height: 20px;
  line-height: 18px;
  border-radius: 4px;
  font-weight: 600;
  background: hsl(var(--warning) / 0.1);
  color: hsl(var(--warning));
  border-color: hsl(var(--warning) / 0.2);
}

.switch-auto-width {
  width: auto !important;
}

.indicator-input-row {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.input-row--inline {
  display: flex;
  flex-direction: row;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 8px;
  width: 100%;
}

.input-row--inline :deep(.ant-switch) {
  width: auto !important;
  min-width: 0 !important;
}

.input-row--inline :deep(.ant-radio-wrapper),
.input-row--inline :deep(.ant-checkbox-wrapper) {
  margin-inline-end: 0 !important;
  white-space: normal !important;
  word-break: break-word !important;
}

.w-full {
  width: 100%;
}

.number-input-auto-width {
  width: auto;
  max-width: 8ch;
  min-width: 160px;
}

.indicator-error {
  display: flex;
  align-items: center;
  gap: 6px;
  color: hsl(var(--destructive));
  font-size: 12.5px;
  margin-top: 6px;
  padding: 6px 10px;
  background: hsl(var(--destructive) / 0.06);
  border: 1px solid hsl(var(--destructive) / 0.2);
  border-radius: 6px;
  line-height: 1.4;
  min-width: 160px;
  width: fit-content;
}

.indicator-last-value {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  border-left: 1px solid hsl(var(--border));
  padding-left: 16px;
  gap: 4px;
}

.last-value-label {
  font-size: 11px;
  color: hsl(var(--muted-foreground));
  font-weight: 500;
  letter-spacing: 0.3px;
  text-transform: uppercase;
}

.last-value-content {
  font-size: 16px;
  font-weight: 600;
  color: hsl(var(--success));
  line-height: 1.2;
}

.file-upload-wrapper {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.upload-hint {
  font-size: 12px;
  color: hsl(var(--muted-foreground));
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 0;
}

.upload-count {
  color: hsl(var(--primary));
  font-weight: 500;
}

.file-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.file-item {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 8px;
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: 6px;
  max-width: 200px;
}

.file-icon {
  flex-shrink: 0;
  color: hsl(var(--primary));
}

.file-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 12px;
  color: hsl(var(--foreground));
}

.file-delete-btn {
  flex-shrink: 0;
  padding: 0;
  border: none;
  background: none;
  cursor: pointer;
  color: hsl(var(--destructive));
  display: flex;
  align-items: center;
  opacity: 0.7;
  transition: opacity 0.15s;
}

.file-delete-btn:hover {
  opacity: 1;
}

.dynamic-container-form {
  display: flex;
  flex-direction: column;
}

.dynamic-entry-row {
  background: hsl(var(--card));
  border: 1px solid hsl(var(--border));
  border-radius: 8px;
  padding: 16px;
}

/* 容器表单内部布局 */
.conditional-container-form,
.auto-entry-container-form,
.dynamic-container-form {
  max-width: 100%;
}

.entry-fields {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.entry-header {
  border-bottom: 1px solid hsl(var(--border));
  padding-bottom: 8px;
  margin-bottom: 12px;
}

.entry-label {
  font-weight: 600;
  color: hsl(var(--muted-foreground));
}

.text-red-500 {
  color: hsl(var(--destructive));
  font-weight: 600;
}

.mb-4 {
  margin-bottom: 16px;
}
</style>

<style>
.indicator-input-row.input-row--inline .ant-checkbox-group,
.indicator-input-row.input-row--inline .ant-radio-group {
  display: flex !important;
  flex-wrap: wrap !important;
  width: 100% !important;
  gap: 8px 16px !important;
  line-height: 1.57 !important;
}

.indicator-input-row.input-row--inline .ant-checkbox-group .ant-checkbox-wrapper,
.indicator-input-row.input-row--inline .ant-radio-group .ant-radio-wrapper {
  display: flex !important;
  width: auto !important;
  min-width: 0 !important;
  white-space: normal !important;
  line-height: 1.57 !important;
  font-size: 14px !important;
  list-style: none !important;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, 'Noto Sans', sans-serif !important;
}
</style>
