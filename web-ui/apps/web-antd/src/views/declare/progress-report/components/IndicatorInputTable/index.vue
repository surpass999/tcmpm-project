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

import { ref, computed, watch, nextTick } from 'vue';
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

import { fieldErrors } from './composables/useErrorKeys';


// 表单值
import {
  formValues,
  isDirty,
  markDirty,
  resetDirty,
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
  lastPeriodRawValues,
  indicatorGroups,
  jointRules,
  loadIndicatorData,
  reloadIndicatorData,
  loadJointRules,
  getIndicatorsInDisplayOrder,
  loadLastPeriodValues,
} from './composables/useIndicatorData';

// 计算指标
import { recalculateComputedIndicators } from './composables/useComputedIndicators';

// 联动
import {
  isIndicatorVisible,
  isIndicatorDisabled,
  isIndicatorRequired,
} from './composables/useLinkage';

// 工具函数
import { parseOptions } from './utils/options';
import { formatLastValueNumber, parseInputTypeLastPeriodRaw } from './utils/lastValue';

// 输入型选项
import {
  handleInputTypeRadioChange,
  handleCheckboxInputClick,
  onInputTypeBlur,
  getInputTypeLastPeriodContent,
  setOnMultiSelectChangeCallback,
  setOnIndicatorChangeCallback,
  syncInputTypeContent,
  serializeInputTypeArrayForSave,
} from './composables/useInputTypeOptions';
import { inputTypeValues, getPureCheckboxValues, serializeInputTypeValue } from './composables/useFormValues';

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

// 上期对比规则
import {
  validateAllPositiveRules,
  validatePositiveRuleForIndicator,
  hasAnyLastPeriodValue,
} from './composables/usePositiveRules';

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
    /** 真正的医院 ID，用于查询上期值（区别于 deptId） */
    realHospitalId?: number;
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
const rootRef = ref<HTMLElement>();

/** 当前聚焦的输入型选项信息（用于 blur 时恢复值） */

// ==================== 辅助函数 ====================

/** 容器上期值（支持多条目），fieldValues 使用 fullKey（rowKey+fieldCode）以便精确匹配 */
interface ContainerEntryLastValue {
  rowKey: string;
  fieldValues: Record<string, string>;
}
const containerLastPeriodValues = computed(() => {
  const result: Record<string, ContainerEntryLastValue[]> = {};
  for (const [indicatorCode, rawValue] of Object.entries(lastPeriodValues.value)) {
    if (!rawValue) continue;
    try {
      const parsed = JSON.parse(rawValue);
      if (Array.isArray(parsed) && parsed.length > 0) {
        result[indicatorCode] = parsed.map((row: any, idx: number) => {
          const rowKey = row.rowKey || `last_${idx}`;
          const fieldValues: Record<string, string> = {};
          for (const [key, val] of Object.entries(row)) {
            if (key !== 'rowKey' && val != null) {
              // fieldCode 可能是 fullKey（带 rowKey 前缀），也可能是纯 fieldCode，全部保留
              fieldValues[key] = String(val);
            }
          }
          return { rowKey, fieldValues };
        });
      }
    } catch { /* ignore */ }
  }
  return result;
});

/** 单选/多选指标的 inputType 输入内容：{ indicatorCode: { optionValue: inputContent } } */
const lastPeriodRawContents = computed(() => {
  const result: Record<string, Record<string, string>> = {};
  for (const ind of indicators.value) {
    if (ind.valueType !== 6 && ind.valueType !== 7) continue;
    const raw = lastPeriodRawValues.value[ind.indicatorCode];
    if (raw) {
      result[ind.indicatorCode] = parseInputTypeLastPeriodRaw(raw, ind.valueType);
    }
  }
  return result;
});

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

/** 格式化单选/多选上期值（拼接 inputType 输入内容） */
function getFormattedLastPeriodValue(indicator: DeclareIndicatorApi.Indicator): string {
  const label = lastPeriodValues.value[indicator.indicatorCode] || '';
  const contents = lastPeriodRawContents.value[indicator.indicatorCode] || {};
  if (Object.keys(contents).length === 0) return label;
  const options = parseOptions(indicator.valueOptions);
  const labelParts = label.split('、');
  const parts: string[] = [];
  for (const part of labelParts) {
    const trimmed = part.trim();
    if (!trimmed) continue;
    const opt = options.find((o) => o.label === trimmed);
    if (opt && contents[opt.value]) {
      parts.push(`${trimmed}（${contents[opt.value]}）`);
    } else {
      parts.push(trimmed);
    }
  }
  return parts.join('、');
}

// ==================== 事件处理 ====================

/** 注册指标变化回调（在 setup 顶层同步执行，确保组件创建时就可用） */
setOnMultiSelectChangeCallback((indicator) => {
  onIndicatorChange(indicator);
});
setOnIndicatorChangeCallback((indicator) => {
  onIndicatorChange(indicator);
});

function handleNumberBlur(indicator: DeclareIndicatorApi.Indicator, _event: Event) {
  const currentValue = formValues[indicator.indicatorCode];
  if (indicator.id !== undefined) markTopLevelDirty(indicator.id);
  checkAndSyncLinkedAutoContainers(indicator.indicatorCode, indicators.value);
  if (indicator.id !== undefined) {
    const key = toTopLevelKey(indicator.id);
    clearFieldError(key, true);
  }
  nextTick(() => {
    validateIndicator(indicator);
    recalculateComputedIndicators(indicators.value);
    validateLogicRuleForBlur(indicator, indicators.value, setFieldError, clearFieldError, setDirty);
    if (hasAnyLastPeriodValue()) {
      validatePositiveRuleForIndicator(indicator.indicatorCode, indicators.value);
    }
  });
}

function handleTextBlur(indicator: DeclareIndicatorApi.Indicator, _event: Event) {
  if (indicator.id !== undefined) markTopLevelDirty(indicator.id);
  if (indicator.id !== undefined) {
    const key = toTopLevelKey(indicator.id);
    clearFieldError(key, true);
  }
  nextTick(() => {
    validateIndicator(indicator);
  });
}

function handleMultiSelectChange(indicator: any, selectedValues: string[]) {
  // 处理互斥逻辑
  const options = parseOptions(indicator.valueOptions);
  const exclusiveValues = new Set(options.filter((o) => o.exclusive).map((o) => o.value));
  let finalValues: string[];
  if (selectedValues.some((v) => exclusiveValues.has(v))) {
    // 如果选中了互斥项，只保留第一个互斥项
    finalValues = [selectedValues.find((v) => exclusiveValues.has(v))!];
  } else {
    finalValues = selectedValues;
  }
  // 序列化所有选中的值（添加 inputType 内容）
  const serialized = finalValues.map((v) => {
    const inputKey = indicator.indicatorCode + '_' + v;
    const inputContent = inputTypeValues[inputKey] || '';
    return serializeInputTypeValue(v, inputContent);
  });
  formValues[indicator.indicatorCode] = serialized;
  onIndicatorChange(indicator);
}

function onIndicatorChange(indicator: DeclareIndicatorApi.Indicator, rawValue?: any) {
  // 如果传入了 rawValue，说明是从 @change 事件中直接获取的值（v-model 还未同步）
  if (rawValue !== undefined) {
    formValues[indicator.indicatorCode] = rawValue;
  }
  if (indicator.id !== undefined) {
    markTopLevelDirty(indicator.id);
    const key = toTopLevelKey(indicator.id);
    clearFieldError(key, true);
  }
  if (indicator.valueType === 12) {
    const entries = containerValues[indicator.indicatorCode] || [];
    formValues[indicator.indicatorCode] = JSON.stringify(entries);
  }
  checkAndSyncLinkedAutoContainers(indicator.indicatorCode, indicators.value);
  // nextTick 确保 v-model 的值已同步到 formValues（ant-design @change 在值更新前触发）
  nextTick(() => {
    validateIndicator(indicator);
    validateLogicRuleForBlur(indicator, indicators.value, setFieldError, clearFieldError, setDirty);
    if (hasAnyLastPeriodValue()) {
      validatePositiveRuleForIndicator(indicator.indicatorCode, indicators.value);
    }
  });
}

/** 输入型选项失焦：恢复值并触发父级指标校验 */
function handleInputTypeBlur(indicator: DeclareIndicatorApi.Indicator, opt: any, _value: string) {
  const inputKey = indicator.indicatorCode + '_' + opt.value;
  onInputTypeBlur(indicator, opt, inputTypeValues[inputKey] || '');
  onIndicatorChange(indicator);
}

function onContainerFieldChange(indicator: DeclareIndicatorApi.Indicator, entry: any, field: DynamicField) {
  const containerType = getContainerType(indicator.valueOptions);
  markContainerFieldDirty(indicator.indicatorCode, entry.rowKey, field.fieldCode, containerType);
  validateContainerFieldOnBlur(entry, indicator, field, containerValues[indicator.indicatorCode] || []);
  nextTick(() => {
    recalculateComputedIndicators(indicators.value);
    validateLogicRuleForBlur(indicator, indicators.value, setFieldError, clearFieldError, setDirty);
    if (hasAnyLastPeriodValue()) {
      validatePositiveRuleForIndicator(indicator.indicatorCode, indicators.value);
    }
  });
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

// realHospitalId 异步就位后，单独加载上期值
// （hospitalId 在 onMounted 时还是 deptId，需等 getHospitalByDeptId 返回后才变成真正的医院 ID）
watch(
  () => [props.realHospitalId, props.reportYear, props.reportBatch],
  ([newRealHospitalId, newReportYear, newReportBatch]) => {
    if (!newRealHospitalId || newReportYear === undefined || newReportBatch === undefined) {
      return;
    }
    loadLastPeriodValues(newRealHospitalId, newReportYear, newReportBatch);
  },
  { immediate: true },
);

watch(formValues as any, () => {
  const hasAnyValue = Object.values(formValues).some((v) => v !== undefined && v !== null && v !== '');
  if (hasAnyValue) markDirty();
  emit('update');
}, { deep: true });

// 调试：专门监听 formValues['702'] 的变化
// watch(() => formValues['702'], (newVal, oldVal) => {
//   console.log('[DEBUG watch formValues["702"]] changed', { oldVal, newVal });
// }, { immediate: false });

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
    if (rawValue === undefined || rawValue === '') {
      continue;
    }
    const item: any = { indicatorId: ind.id!, indicatorCode: code, valueType: vt };
    if (vt === 12) item.valueStr = JSON.stringify(containerValues[code] || []);
    else if (vt === 1) item.valueNum = rawValue !== null ? String(rawValue) : null;
    else if (vt === 2 || vt === 6 || vt === 9 || vt === 10) item.valueStr = String(rawValue);
    else if (vt === 3) item.valueBool = !!rawValue;
    else if (vt === 4) item.valueDate = dayjs.isDayjs(rawValue) ? rawValue.format('YYYY-MM-DD HH:mm:ss') : String(rawValue);
    else if (vt === 5) item.valueText = String(rawValue);
    else if (vt === 7 || vt === 11) item.valueStr = Array.isArray(rawValue) ? serializeInputTypeArrayForSave(code, rawValue) : String(rawValue);
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
  // 收集所有错误，统一转换为 ErrorItem 格式
  const allErrors: any[] = [];

  // ==================== 第1级：基础验证 ====================
  // 必填、类型、最大值、最小值、精度、文本格式
  const { messages } = validateAll(indicators.value, setFieldError, clearFieldError);
  if (messages.length > 0) {
    // 基础验证错误需要转换为 ErrorItem 格式
    for (const msg of messages) {
      allErrors.push({
        indicatorId: msg.indicatorId,
        indicatorCode: msg.indicatorCode,
        indicatorName: msg.indicatorName || '',
        message: msg.message,
        errorType: msg.errorType || 'format',
      });
    }
  }

  const orderedIndicators = getIndicatorsInDisplayOrder();

  // ==================== 第2级：逻辑验证 ====================
  // IF条件表达式、简单比较、容器内规则
  const logicErrors = validateLogicRules(orderedIndicators, setFieldError, clearFieldError);
  if (logicErrors.length > 0) {
    // 逻辑验证错误已经是 ErrorItem 格式
    allErrors.push(...logicErrors);
  }

  // ==================== 第3级：上期值验证 ====================
  // 仅在有上期值时执行
  if (hasAnyLastPeriodValue()) {
    const positiveErrors = validateAllPositiveRules(orderedIndicators);
    if (positiveErrors.length > 0) {
      // 转换为 ErrorItem 格式，同时写入 fieldErrors（持久化显示）
      for (const e of positiveErrors) {
        const indicator = indicators.value.find((i) => i.indicatorCode === e.indicatorCode);
        // 写入 fieldErrors 以便页面上显示（使用 joint 类型，dirty=false）
        setFieldError(e.errorKey, `${e.ruleName}：${e.message}`, 'joint', false);
        allErrors.push({
          indicatorId: e.indicatorId,
          indicatorCode: e.indicatorCode,
          indicatorName: indicator?.indicatorName || '',
          message: `${e.ruleName}：${e.message}`,
          errorType: 'joint' as const,
        });
      }
    }
  }

  // 返回所有错误
  if (allErrors.length > 0) {
    return allErrors;
  }

  return [];
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
  scrollToField,
  // 联动相关
  isIndicatorVisible,
  isIndicatorDisabled,
  isIndicatorRequired,
});

/** 滚动到指定字段（容器字段或顶层指标） */
function scrollToField(containerFieldKey?: string, indicatorCode?: string) {
  let el: Element | null = null;
  if (containerFieldKey) {
    el = rootRef.value?.querySelector(`[data-container-field-key="${containerFieldKey}"]`) ?? null;
  }
  if (!el && indicatorCode) {
    el = rootRef.value?.querySelector(`[data-indicator-code="${indicatorCode}"]`) ?? null;
  }
  if (!el) return;

  nextTick(() => {
    scrollToEl(el!);
  });
}

/** 对单个元素执行滚动和高亮 */
function scrollToEl(el: Element) {
  const rect = el.getBoundingClientRect();
  if (!rect || rect.width === 0 || rect.height === 0) {
    const indicatorRow = (el as HTMLElement).closest?.('[data-indicator-code]') as HTMLElement | null;
    if (indicatorRow) {
      indicatorRow.scrollIntoView({ behavior: 'smooth', block: 'center' });
      addHighlight(indicatorRow);
    }
    return;
  }

  // 找到真正的滚动容器（遍历 .indicator-area 的祖先链，找第一个 overflow 为 auto/scroll 的元素）
  let scrollContainer: HTMLElement | null = null;
  let cur: HTMLElement | null = document.querySelector('.indicator-area');
  while (cur) {
    const overflow = getComputedStyle(cur).overflowY;
    if (overflow === 'auto' || overflow === 'scroll') {
      scrollContainer = cur;
      break;
    }
    cur = cur.parentElement;
  }
  if (!scrollContainer) {
    el.scrollIntoView({ behavior: 'smooth', block: 'center' });
    addHighlight(el);
    return;
  }

  const targetRect = el.getBoundingClientRect();
  const containerRect = scrollContainer.getBoundingClientRect();
  // 元素在容器坐标系中的 scrollTop = 容器当前 scrollTop + (元素视口top - 容器视口top)
  const relativeTop = scrollContainer.scrollTop + (targetRect.top - containerRect.top);
  // 不做居中，防止对顶部元素计算出负值被 clamp 为 0
  const newScrollTop = Math.max(0, relativeTop - 20);
  scrollContainer.scrollTo({ top: newScrollTop, behavior: 'smooth' });
  addHighlight(el);
}

/** 高亮元素 */
function addHighlight(el: Element) {
  el.classList.add('indicator-highlight');
  setTimeout(() => {
    el.classList.remove('indicator-highlight');
  }, 1500);
}


</script>

<template>
  <div ref="rootRef" class="indicator-form-wrapper">
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
          v-show="isIndicatorVisible(indicator.indicatorCode)"
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
                    <span v-if="isIndicatorRequired(indicator.indicatorCode, indicator.isRequired)" class="text-red-500">*</span>
                  </span>
                </a-popover>
                <span v-else class="label-text" :title="indicator.indicatorName">
                  {{ indicator.indicatorCode }} - {{ indicator.indicatorName }}
                  <span v-if="isIndicatorRequired(indicator.indicatorCode, indicator.isRequired)" class="text-red-500">*</span>
                </span>
              </div>
              <a-tag v-if="isComputedIndicator(indicator)" color="orange" class="computed-tag">自动计算</a-tag>
            </div>

            <!-- 指标输入组件行 -->
            <div
              class="indicator-input-row"
              :class="{ 'input-row--inline': indicator.valueType === 3 || indicator.valueType === 6 || indicator.valueType === 7 }"
            >
              <!-- 数字类型：输入框、上期值、错误提示各独立一行 -->
              <div
                v-if="indicator.valueType === 1"
                style="display: flex; flex-direction: column; gap: 4px; align-items: flex-start;"
              >
                <SafeNumberInput
                  v-model="formValues[indicator.indicatorCode]"
                  :disabled="isIndicatorDisabled(indicator.indicatorCode, readonly) || isComputedIndicator(indicator)"
                  :placeholder="isComputedIndicator(indicator) ? '自动计算' : '请输入数字'"
                  :min="indicator.minValue ?? 0"
                  :max="indicator.maxValue"
                  :precision="getNumberPrecision(indicator)"
                  :suffix="indicator.unit || parseExtraConfig(indicator.extraConfig).suffix"
                  class="number-input-auto-width"
                  @blur="(e: Event) => handleNumberBlur(indicator, e)"
                />
                <div
                  v-if="lastPeriodValues[indicator.indicatorCode]"
                  class="inline-last-value"
                >
                  <span class="last-value-prefix">上期：</span>
                  <span class="last-value-text">{{ formatLastValueNumber(lastPeriodValues[indicator.indicatorCode], getNumberPrecision(indicator)) }}</span>
                </div>
                <div
                  v-if="indicator.id && getTopLevelError(indicator.id)"
                  class="indicator-error"
                >
                  {{ getTopLevelError(indicator.id) }}
                </div>
              </div>

              <!-- 非数字/单选多选类型：每种控件独立一行显示上期值 -->
              <div
                v-else-if="indicator.valueType === 2"
                style="display: flex; flex-direction: column; gap: 4px; align-items: flex-start;"
              >
                <a-input
                  :value="formValues[indicator.indicatorCode]"
                  :disabled="isIndicatorDisabled(indicator.indicatorCode, readonly)"
                  :placeholder="`请输入${indicator.indicatorName}`"
                  :maxlength="getMaxLength(indicator)"
                  show-count
                  class="w-full"
                  @blur="(e: Event) => handleTextBlur(indicator, e)"
                  @update:value="(v: string) => { formValues[indicator.indicatorCode] = v; onIndicatorChange(indicator, v); }"
                />
                <div
                  v-if="lastPeriodValues[indicator.indicatorCode]"
                  class="inline-last-value"
                >
                  <span class="last-value-prefix">上期：</span>
                  <span class="last-value-text">{{ lastPeriodValues[indicator.indicatorCode] }}</span>
                </div>
                <div
                  v-if="indicator.id && getTopLevelError(indicator.id)"
                  class="indicator-error"
                >
                  {{ getTopLevelError(indicator.id) }}
                </div>
              </div>
              <div
                v-else-if="indicator.valueType === 3"
                style="display: flex; flex-direction: column; gap: 4px; align-items: flex-start;"
              >
                <a-switch
                  v-model="formValues[indicator.indicatorCode]"
                  :disabled="isIndicatorDisabled(indicator.indicatorCode, readonly)"
                  :checked-children="getBooleanLabels(indicator).true"
                  :un-checked-children="getBooleanLabels(indicator).false"
                  class="switch-auto-width"
                  @change="(val: any) => { formValues[indicator.indicatorCode] = val; onIndicatorChange(indicator, val); }"
                />
                <div
                  v-if="lastPeriodValues[indicator.indicatorCode]"
                  class="inline-last-value"
                >
                  <span class="last-value-prefix">上期：</span>
                  <span class="last-value-text">{{ lastPeriodValues[indicator.indicatorCode] }}</span>
                </div>
                <div
                  v-if="indicator.id && getTopLevelError(indicator.id)"
                  class="indicator-error"
                >
                  {{ getTopLevelError(indicator.id) }}
                </div>
              </div>
              <div
                v-else-if="indicator.valueType === 4"
                style="display: flex; flex-direction: column; gap: 4px; align-items: flex-start;"
              >
                <a-date-picker
                  v-model="formValues[indicator.indicatorCode]"
                  :disabled="isIndicatorDisabled(indicator.indicatorCode, readonly)"
                  :show-time="getDateFormat(indicator).includes('HH')"
                  :format="getDateFormat(indicator)"
                  @change="(val: any) => { formValues[indicator.indicatorCode] = val; onIndicatorChange(indicator, val); }"
                />
                <div
                  v-if="lastPeriodValues[indicator.indicatorCode]"
                  class="inline-last-value"
                >
                  <span class="last-value-prefix">上期：</span>
                  <span class="last-value-text">{{ lastPeriodValues[indicator.indicatorCode] }}</span>
                </div>
                <div
                  v-if="indicator.id && getTopLevelError(indicator.id)"
                  class="indicator-error"
                >
                  {{ getTopLevelError(indicator.id) }}
                </div>
              </div>
              <div
                v-else-if="indicator.valueType === 5"
                style="display: flex; flex-direction: column; gap: 4px; align-items: flex-start;"
              >
                <a-textarea
                  v-model="formValues[indicator.indicatorCode]"
                  :disabled="isIndicatorDisabled(indicator.indicatorCode, readonly)"
                  :placeholder="`请输入${indicator.indicatorName}`"
                  :rows="isRichText(indicator) ? 4 : 2"
                  :maxlength="getMaxLength(indicator)"
                  :show-count="!!getMaxLength(indicator)"
                  class="w-full"
                  @change="(e: any) => { const v = e.target.value; formValues[indicator.indicatorCode] = v; onIndicatorChange(indicator, v); }"
                />
                <div
                  v-if="lastPeriodValues[indicator.indicatorCode]"
                  class="inline-last-value"
                >
                  <span class="last-value-prefix">上期：</span>
                  <span class="last-value-text">{{ lastPeriodValues[indicator.indicatorCode] }}</span>
                </div>
                <div
                  v-if="indicator.id && getTopLevelError(indicator.id)"
                  class="indicator-error"
                >
                  {{ getTopLevelError(indicator.id) }}
                </div>
              </div>
              <div
                v-else-if="indicator.valueType === 10"
                style="display: flex; flex-direction: column; gap: 4px; align-items: flex-start;"
              >
                <a-select
                  v-model="formValues[indicator.indicatorCode]"
                  :disabled="isIndicatorDisabled(indicator.indicatorCode, readonly)"
                  :placeholder="`请选择${indicator.indicatorName}`"
                  :show-search="getShowSearch(indicator)"
                  allow-clear
                  @change="(val: any) => { formValues[indicator.indicatorCode] = val; onIndicatorChange(indicator, val); }"
                >
                  <a-select-option v-for="opt in parseOptions(indicator.valueOptions)" :key="opt.value" :value="opt.value">{{ opt.label }}</a-select-option>
                </a-select>
                <div
                  v-if="lastPeriodValues[indicator.indicatorCode]"
                  class="inline-last-value"
                >
                  <span class="last-value-prefix">上期：</span>
                  <span class="last-value-text">{{ lastPeriodValues[indicator.indicatorCode] }}</span>
                </div>
                <div
                  v-if="indicator.id && getTopLevelError(indicator.id)"
                  class="indicator-error"
                >
                  {{ getTopLevelError(indicator.id) }}
                </div>
              </div>
              <div
                v-else-if="indicator.valueType === 11"
                style="display: flex; flex-direction: column; gap: 4px; align-items: flex-start;"
              >
                <a-select
                  :value="formValues[indicator.indicatorCode] || []"
                  :disabled="isIndicatorDisabled(indicator.indicatorCode, readonly)"
                  :placeholder="`请选择${indicator.indicatorName}`"
                  mode="multiple"
                  :show-search="getShowSearch(indicator)"
                  allow-clear
                  @change="(vals: string[]) => handleMultiSelectChange(indicator, vals)"
                >
                  <a-select-option v-for="opt in parseOptions(indicator.valueOptions)" :key="opt.value" :value="opt.value">{{ opt.label }}</a-select-option>
                </a-select>
                <div
                  v-if="lastPeriodValues[indicator.indicatorCode]"
                  class="inline-last-value"
                >
                  <span class="last-value-prefix">上期：</span>
                  <span class="last-value-text">{{ lastPeriodValues[indicator.indicatorCode] }}</span>
                </div>
                <div
                  v-if="indicator.id && getTopLevelError(indicator.id)"
                  class="indicator-error"
                >
                  {{ getTopLevelError(indicator.id) }}
                </div>
              </div>
              <div
                v-else-if="indicator.valueType === 8"
                style="display: flex; flex-direction: column; gap: 4px; align-items: flex-start;"
              >
                <a-range-picker
                  v-model="formValues[indicator.indicatorCode]"
                  :disabled="isIndicatorDisabled(indicator.indicatorCode, readonly)"
                  :show-time="getDateFormat(indicator).includes('HH')"
                  :format="getDateFormat(indicator)"
                  @change="(val: any) => { formValues[indicator.indicatorCode] = val; onIndicatorChange(indicator, val); }"
                />
                <div
                  v-if="lastPeriodValues[indicator.indicatorCode]"
                  class="inline-last-value"
                >
                  <span class="last-value-prefix">上期：</span>
                  <span class="last-value-text">{{ lastPeriodValues[indicator.indicatorCode] }}</span>
                </div>
                <div
                  v-if="indicator.id && getTopLevelError(indicator.id)"
                  class="indicator-error"
                >
                  {{ getTopLevelError(indicator.id) }}
                </div>
              </div>

              <!-- 单选类型（valueType=6）：独立一行显示上期值 -->
              <div
                v-else-if="indicator.valueType === 6"
                style="display: flex; flex-direction: column; gap: 4px; align-items: flex-start;"
              >
                <div
                  class="indicator-input-row"
                  :class="{ 'input-row--inline': true }"
                >
                  <a-radio-group
                    :value="formValues[indicator.indicatorCode]"
                    :disabled="isIndicatorDisabled(indicator.indicatorCode, readonly)"
                    @change="(e: any) => { handleInputTypeRadioChange(indicator, e.target.value); onIndicatorChange(indicator); }"
                  >
                    <template v-for="opt in parseOptions(indicator.valueOptions)" :key="opt.value">
                      <a-radio :value="opt.value">
                        {{ opt.label }}
                      </a-radio>
                      <!-- 输入型选项：选中后显示输入框 -->
                      <a-input
                        v-if="opt.inputType && formValues[indicator.indicatorCode] === opt.value"
                        :value="inputTypeValues[indicator.indicatorCode + '_' + opt.value] || ''"
                        :disabled="isIndicatorDisabled(indicator.indicatorCode, readonly)"
                        :placeholder="getInputTypeLastPeriodContent(lastPeriodRawValues, indicator.indicatorCode, opt.value) ? '上期：' + getInputTypeLastPeriodContent(lastPeriodRawValues, indicator.indicatorCode, opt.value) : '请输入补充内容'"
                        style="display: inline-block; width: 200px; padding: 0px 0px 0px 4px; border-radius: 4px; vertical-align: middle;"
                        @input="(e: any) => { const v = e.target.value; inputTypeValues[indicator.indicatorCode + '_' + opt.value] = v; syncInputTypeContent(indicator, opt, v); }"
                        @blur.stop="(e: any) => { handleInputTypeBlur(indicator, opt, e.target.value); }"
                      />
                    </template>
                  </a-radio-group>
                </div>
                <div
                  v-if="lastPeriodValues[indicator.indicatorCode]"
                  class="inline-last-value"
                >
                  <span class="last-value-prefix">上期：</span>
                  <span class="last-value-text">{{ getFormattedLastPeriodValue(indicator) }}</span>
                </div>
                <div
                  v-if="indicator.id && getTopLevelError(indicator.id)"
                  class="indicator-error"
                >
                  {{ getTopLevelError(indicator.id) }}
                </div>
              </div>

              <!-- 多选类型（valueType=7）：独立一行显示上期值 -->
              <div
                v-else-if="indicator.valueType === 7"
                style="display: flex; flex-direction: column; gap: 4px; align-items: flex-start;"
              >
                <div
                  class="indicator-input-row"
                  :class="{ 'input-row--inline': true }"
                >
                  <!-- 所有选项按原始顺序渲染 -->
                  <template v-for="opt in parseOptions(indicator.valueOptions)" :key="'opt-' + opt.value">
                    <a-checkbox
                      v-if="!opt.inputType"
                      :checked="getPureCheckboxValues(indicator.indicatorCode).includes(opt.value)"
                      :disabled="isIndicatorDisabled(indicator.indicatorCode, readonly)"
                      :class="{ 'checkbox-tight': true }"
                      @click="(e: MouseEvent) => handleCheckboxInputClick(indicator, opt.value, e)"
                    >
                      {{ opt.label }}
                    </a-checkbox>
                    <template v-else-if="opt.inputType">
                      <a-checkbox
                        :checked="getPureCheckboxValues(indicator.indicatorCode).includes(opt.value)"
                        :disabled="isIndicatorDisabled(indicator.indicatorCode, readonly)"
                        :class="{ 'checkbox-tight': true }"
                        @click="(e: MouseEvent) => handleCheckboxInputClick(indicator, opt.value, e)"
                      >
                        {{ opt.label }}
                      </a-checkbox>
                      <a-input
                        v-if="getPureCheckboxValues(indicator.indicatorCode).includes(opt.value)"
                        :value="inputTypeValues[indicator.indicatorCode + '_' + opt.value] || ''"
                        :disabled="isIndicatorDisabled(indicator.indicatorCode, readonly)"
                        :placeholder="getInputTypeLastPeriodContent(lastPeriodRawValues, indicator.indicatorCode, opt.value) ? '上期：' + getInputTypeLastPeriodContent(lastPeriodRawValues, indicator.indicatorCode, opt.value) : '请输入补充内容'"
                        style="display: inline-block; width: 140px; padding: 0px 0px 0px 4px; border-radius: 4px; vertical-align: middle;"
                        @input="(e: any) => { const v = e.target.value; inputTypeValues[indicator.indicatorCode + '_' + opt.value] = v; syncInputTypeContent(indicator, opt, v); }"
                        @blur.stop="(e: any) => { handleInputTypeBlur(indicator, opt, e.target.value); }"
                      />
                    </template>
                  </template>
                </div>
                <div
                  v-if="lastPeriodValues[indicator.indicatorCode]"
                  class="inline-last-value"
                >
                  <span class="last-value-prefix">上期：</span>
                  <span class="last-value-text">{{ getFormattedLastPeriodValue(indicator) }}</span>
                </div>
                <div
                  v-if="indicator.id && getTopLevelError(indicator.id)"
                  class="indicator-error"
                >
                  {{ getTopLevelError(indicator.id) }}
                </div>
              </div>

              <!-- 文件上传类型（valueType=9 无错误内联，在此兜底） -->
              <template v-else-if="indicator.valueType === 9">
                <div v-if="getFileList(fileListMap, indicator.indicatorCode).length > 0" class="file-list">
                  <div v-for="(file, index) in getFileList(fileListMap, indicator.indicatorCode)" :key="index" class="file-item">
                    <IconifyIcon icon="lucide:file-text" class="file-icon" />
                    <span class="file-name" :title="file.name">{{ file.name }}</span>
                    <button v-if="!readonly" type="button" class="file-delete-btn" @click="handleFileRemove(indicator.indicatorCode, file, indicator)">
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
                <!-- 上期值显示 -->
                <div
                  v-if="lastPeriodValues[indicator.indicatorCode]"
                  class="inline-last-value"
                >
                  <span class="last-value-prefix">上期：</span>
                  <span class="last-value-text">{{ lastPeriodValues[indicator.indicatorCode] }}</span>
                </div>
                <!-- 错误提示 -->
                <div
                  v-if="indicator.id && getTopLevelError(indicator.id)"
                  class="indicator-error"
                >
                  {{ getTopLevelError(indicator.id) }}
                </div>
              </template>

              <!-- 动态容器类型 -->
              <template v-else-if="indicator.valueType === 12">
                <!-- 条件容器 -->
                <div v-show="getContainerType(indicator.valueOptions) === 'conditional'" class="conditional-container-form">
                  <div
                    v-for="(entry, entryIdx) in getContainerEntries(indicator.indicatorCode)"
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
                        :disabled="isIndicatorDisabled(indicator.indicatorCode, readonly)"
                        :entry-index="0"
                        :container-last-values="containerLastPeriodValues[indicator.indicatorCode]"
                        :container-field-error="getContainerFieldError(entry, indicator.indicatorCode, field.fieldCode, 'conditional')"
                        @blur="() => onContainerFieldChange(indicator, entry, field)"
                        @field-change="() => onContainerFieldBasicChange(indicator, entry, field)"
                      />
                    </div>
                  </div>
                  <div
                    v-if="containerLastPeriodValues[indicator.indicatorCode]?.length"
                    class="container-last-period-summary"
                  >
                    共 {{ containerLastPeriodValues[indicator.indicatorCode]!.length }} 条上期数据
                  </div>
                </div>

                <!-- 自动条目容器 -->
                <div v-show="getContainerType(indicator.valueOptions) === 'autoEntry' && isAutoEntryVisible(indicator)" class="auto-entry-container-form">
                  <div v-for="(entry, entryIdx) in getContainerEntries(indicator.indicatorCode)" :key="entry.rowKey" class="dynamic-entry-row mb-4">
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
                        :disabled="isIndicatorDisabled(indicator.indicatorCode, readonly)"
                        :entry-index="entryIdx"
                        :container-last-values="containerLastPeriodValues[indicator.indicatorCode]"
                        :container-field-error="getContainerFieldError(entry, indicator.indicatorCode, field.fieldCode, 'autoEntry')"
                        @blur="() => onContainerFieldChange(indicator, entry, field)"
                        @field-change="() => onContainerFieldBasicChange(indicator, entry, field)"
                      />
                    </div>
                  </div>
                  <div
                    v-if="containerLastPeriodValues[indicator.indicatorCode]?.length"
                    class="container-last-period-summary"
                  >
                    共 {{ containerLastPeriodValues[indicator.indicatorCode]!.length }} 条上期数据
                  </div>
                  <div class="text-gray-400 text-xs mt-2">（由「{{ getLinkedIndicatorName(indicator) }}」指标自动生成，数量不可调整）</div>
                </div>

                <!-- 普通动态容器 -->
                <div v-show="getContainerType(indicator.valueOptions) === 'normal'" class="dynamic-container-form">
                  <div v-for="(entry, entryIdx) in getContainerEntries(indicator.indicatorCode)" :key="entry.rowKey" class="dynamic-entry-row mb-4">
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
                        :disabled="isIndicatorDisabled(indicator.indicatorCode, readonly)"
                        :entry-index="entryIdx"
                        :container-last-values="containerLastPeriodValues[indicator.indicatorCode]"
                        :container-field-error="getContainerFieldError(entry, indicator.indicatorCode, field.fieldCode, 'normal')"
                        @blur="() => onContainerFieldChange(indicator, entry, field)"
                        @field-change="() => onContainerFieldBasicChange(indicator, entry, field)"
                      />
                    </div>
                  </div>
                  <div
                    v-if="containerLastPeriodValues[indicator.indicatorCode]?.length"
                    class="container-last-period-summary"
                  >
                    共 {{ containerLastPeriodValues[indicator.indicatorCode]!.length }} 条上期数据
                  </div>
                  <a-button type="dashed" class="w-full mt-2" :disabled="readonly" @click="handleAddEntry(indicator.indicatorCode)">
                    <template #icon><PlusOutlined /></template>
                    添加条目
                  </a-button>
                  <div v-if="getContainerEntries(indicator.indicatorCode).length > 0" class="text-gray-400 text-xs mt-1 text-right">
                    共 {{ getContainerEntries(indicator.indicatorCode).length }} 个条目（上限 {{ MAX_CONTAINER_ENTRIES }}）
                  </div>
                </div>
              </template>

              <!-- 未知类型兜底 -->
              <span v-else class="text-gray-400 text-sm">暂不支持该类型</span>
            </div>
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
.checkbox-tight {
  margin-inline-start: 8px;
  margin-inline-end: 8px;
}

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
  flex: 0 0 80%;
  width: 80%;
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

/* 有上期值时，radio/checkbox 组宽度 auto，不占满一行 */
.input-row--inline :deep(.ant-radio-group),
.input-row--inline :deep(.ant-checkbox-group) {
  width: auto !important;
}

.input-row--inline :deep(.ant-radio-wrapper),
.input-row--inline :deep(.ant-checkbox-wrapper) {
  margin-inline-end: 0 !important;
  white-space: normal !important;
  word-break: break-word !important;
}

.inline-last-value {
  font-size: 14px;
  color: hsl(var(--muted-foreground));
  white-space: nowrap;
  margin: 4px 0px;
}

.last-value-prefix {
  font-weight: 600;
  color: hsl(var(--muted-foreground));
}

.last-value-text {
  color: hsl(var(--success));
  font-weight: 500;
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
  min-width: 140px;
  max-width: 280px;
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
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
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

.container-last-period-summary {
  font-size: 12px;
  color: hsl(var(--muted-foreground));
  padding: 4px 0 2px;
  text-align: right;
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
