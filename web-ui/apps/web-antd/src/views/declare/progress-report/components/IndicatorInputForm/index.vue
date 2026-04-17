<script setup lang="ts">
/**
 * IndicatorInputForm 主入口组件
 * 使用标准 VbenForm 组件渲染指标填报表单
 */
import { ref, reactive, watch, onMounted, nextTick, computed, h } from 'vue';
import { message } from 'ant-design-vue';
import dayjs from 'dayjs';

import type { VbenFormSchema } from './adapter/form';

import { initSetupVbenForm, useVbenForm } from './adapter/form';

import type { DeclareIndicatorApi } from '#/api/declare/indicator';

import {
  formValues, markDirty, isDirty, resetDirty,
  INPUT_VALUE_SEPARATOR,
} from './composables/useFormValues';
import {
  clearAllErrors, setFieldError, clearFieldError,
} from './composables/useErrorKeys';
import {
  validateAll,
  validateFilledData,
  setDirty,
} from './composables/useValidation';
import {
  indicators, lastPeriodValues, lastPeriodRawValues,
  loadIndicatorData, loadJointRules,
  loadLastPeriodValues,
  indicatorGroups,
} from './composables/useIndicatorData';
import {
  isIndicatorVisible, isIndicatorDisabled, isIndicatorRequired,
  recalculateLinkage,
} from './composables/useLinkage';
import {
  validateLogicRules,
  validateLogicRuleForBlur,
} from './composables/useLogicRules';
import {
  validateAllPositiveRules,
  validatePositiveRuleForIndicator,
  hasAnyLastPeriodValue,
} from './composables/usePositiveRules';
import {
  containerValues,
  checkAndSyncLinkedAutoContainers,
  syncAllAutoEntryContainers,
} from './composables/useContainerValues';
import {
  parseDynamicFields, getContainerType,
} from './utils/container';
import {
  parseOptions,
} from './utils/options';
import {
  parseExtraConfig, getNumberPrecision, getDateFormat,
  getBooleanLabels, getMaxLength, getShowSearch,
  getAcceptTypes, getMaxFileCount, isRichText,
} from './utils/indicator';

// Props
const props = withDefaults(defineProps<{
  hospitalId: number;
  realHospitalId?: number;
  projectType?: number;
  reportId?: number;
  reportYear?: number;
  reportBatch?: number;
  readonly?: boolean;
}>(), {
  projectType: undefined,
  readonly: false,
});

const emit = defineEmits<{
  loadingChange: [loading: boolean];
  update: [];
}>();

const initialized = ref(false);
const isLoadingData = ref(false);

// 将 useVbenForm 的配置改为响应式对象，这样可以动态更新 schema
const formConfig = reactive({
  showDefaultActions: false,
  layout: 'vertical' as const,
  schema: [] as VbenFormSchema[],
});

const [Form, formApi] = useVbenForm(formConfig);

onMounted(async () => {
  await initSetupVbenForm();
  await doLoadData();
});

async function doLoadData() {
  if (props.projectType === undefined) return;
  if (isLoadingData.value) return;
  isLoadingData.value = true;
  emit('loadingChange', true);
  try {
    clearAllErrors();
    await loadIndicatorData(
      props.projectType,
      props.reportId,
      props.hospitalId,
      props.reportYear,
      props.reportBatch,
    );
    await loadJointRules(props.projectType);

    formConfig.schema = formSchema.value;

    initialized.value = true;

    await nextTick();
    await new Promise<void>((resolve) => {
      const check = async () => {
        if (formApi.isMounted) {
          if (formApi.form?.setValues) {
            await formApi.form.setValues(formValues);
          } else {
            await formApi.setValues(formValues, false);
          }

          await nextTick();
          await nextTick();
          await nextTick();
          resolve();
        } else {
          setTimeout(check, 20);
        }
      };
      check();
    });
  } catch (error) {
    console.error('[doLoadData] 加载失败:', error);
    message.error('加载指标数据失败');
  } finally {
    emit('loadingChange', false);
    isLoadingData.value = false;
  }
}

watch(
  () => [props.realHospitalId, props.reportYear, props.reportBatch],
  ([newRealHospitalId, newReportYear, newReportBatch]) => {
    if (!newRealHospitalId || newReportYear === undefined || newReportBatch === undefined) return;
    loadLastPeriodValues(newRealHospitalId as number, newReportYear as number, newReportBatch as number);
  },
  { immediate: true },
);

watch(
  () => props.projectType,
  async (newProjectType, oldProjectType) => {
    if (newProjectType !== undefined && newProjectType !== oldProjectType) {
      if (initialized.value) initialized.value = false;
      await doLoadData();
    }
  },
);

// ==================== 辅助函数 ====================
function evaluateCondition(operator: string, triggerValue: any, configValue: any): boolean {
  switch (operator) {
    case 'eq': return triggerValue === configValue;
    case 'neq': return triggerValue !== configValue;
    case 'gt': return Number(triggerValue) > Number(configValue);
    case 'gte': return Number(triggerValue) >= Number(configValue);
    case 'lt': return Number(triggerValue) < Number(configValue);
    case 'lte': return Number(triggerValue) <= Number(configValue);
    case 'in': return Array.isArray(configValue) ? configValue.includes(triggerValue) : false;
    case 'notEmpty': return triggerValue !== undefined && triggerValue !== null && triggerValue !== '';
    case 'isEmpty': return triggerValue === undefined || triggerValue === null || triggerValue === '';
    default: return false;
  }
}

function buildDependencies(indicator: DeclareIndicatorApi.Indicator): any {
  let linkage = null;
  try {
    const cfg = JSON.parse(indicator.extraConfig || '{}');
    linkage = cfg.linkage?.enabled ? cfg.linkage : null;
  } catch { /* ignore */ }
  if (!linkage || !linkage.trigger?.indicatorCode) return undefined;
  const { type, trigger } = linkage;
  const triggerFields = [trigger.indicatorCode];
  if (type === 'show') {
    const fn = (values: any) => evaluateCondition(trigger.operator, values[trigger.indicatorCode], trigger.value);
    return { triggerFields, if: fn };
  }
  if (type === 'disabled') {
    return { triggerFields, disabled(values: any) { return !evaluateCondition(trigger.operator, values[trigger.indicatorCode], trigger.value); } };
  }
  if (type === 'required') {
    return { triggerFields, required(values: any) { return evaluateCondition(trigger.operator, values[trigger.indicatorCode], trigger.value); } };
  }
  return undefined;
}

function hasIndicatorSpec(indicator: DeclareIndicatorApi.Indicator): boolean {
  return !!(indicator.definition || indicator.statisticScope || indicator.dataSource || indicator.fillRequire || indicator.calculationExample);
}

function buildSpecHelp(indicator: DeclareIndicatorApi.Indicator): string {
  const parts: string[] = [];
  if (indicator.definition) parts.push(`指标定义：${indicator.definition}`);
  if (indicator.statisticScope) parts.push(`统计范围：${indicator.statisticScope}`);
  if (indicator.dataSource) parts.push(`数据来源：${indicator.dataSource}`);
  if (indicator.fillRequire) parts.push(`填报要求：${indicator.fillRequire}`);
  if (indicator.calculationExample) parts.push(`计算示例：${indicator.calculationExample}`);
  return parts.join('\n');
}

function buildLastPeriodHtml(indicatorCode: string): string {
  const val = lastPeriodValues.value[indicatorCode];
  if (!val) return '';
  return `上期：${val}`;
}

function parseLastPeriodRaw(indicatorCode: string): Record<string, string> {
  const raw = lastPeriodRawValues.value[indicatorCode];
  if (!raw) return {};
  const result: Record<string, string> = {};
  const parts = raw.includes(',') ? raw.split(',') : [raw];
  for (const part of parts) {
    const idx = part.indexOf(INPUT_VALUE_SEPARATOR);
    if (idx !== -1) result[part.substring(0, idx)] = part.substring(idx + 1);
  }
  return result;
}

function isComputedIndicator(indicator: DeclareIndicatorApi.Indicator): boolean {
  return !!(indicator.calculationRule && indicator.calculationRule.trim());
}

// ==================== Schema 生成 ====================
function buildSchemaForIndicator(indicator: DeclareIndicatorApi.Indicator): VbenFormSchema {
  const code = indicator.indicatorCode;
  const vt = indicator.valueType;
  const deps = buildDependencies(indicator);
  const cfg = parseExtraConfig(indicator.extraConfig);
  const isRequired = indicator.isRequired;

  let component = 'Input';
  let componentProps: Record<string, any> = {};
  let rules: any = undefined;

  switch (vt) {
    case 1: {
      component = 'VbenNumberInput';
      componentProps = { min: indicator.minValue ?? 0, max: indicator.maxValue, precision: getNumberPrecision(indicator), suffix: indicator.unit || cfg.suffix };
      break;
    }
    case 2: {
      component = 'Input';
      componentProps = { placeholder: `请输入${indicator.indicatorName}`, maxlength: getMaxLength(indicator), showCount: true };
      rules = isRequired ? 'required' : undefined;
      break;
    }
    case 3: {
      component = 'VbenSwitch';
      componentProps = { checkedChildren: getBooleanLabels(indicator).true, unCheckedChildren: getBooleanLabels(indicator).false };
      break;
    }
    case 4: {
      component = 'DatePicker';
      componentProps = { format: getDateFormat(indicator), showTime: getDateFormat(indicator).includes('HH') };
      rules = isRequired ? 'selectRequired' : undefined;
      break;
    }
    case 5: {
      component = 'Textarea';
      componentProps = { placeholder: `请输入${indicator.indicatorName}`, rows: isRichText(indicator) ? 4 : 2, maxlength: getMaxLength(indicator), showCount: !!getMaxLength(indicator) };
      break;
    }
    case 6: {
      component = 'InputTypeRadioGroup';
      componentProps = { options: parseOptions(indicator.valueOptions), lastPeriodContents: parseLastPeriodRaw(code) };
      rules = isRequired ? 'selectRequired' : undefined;
      break;
    }
    case 7: {
      component = 'InputTypeCheckboxGroup';
      componentProps = { options: parseOptions(indicator.valueOptions), lastPeriodContents: parseLastPeriodRaw(code) };
      rules = isRequired ? 'selectRequired' : undefined;
      break;
    }
    case 8: {
      component = 'VbenRangePicker';
      componentProps = { format: getDateFormat(indicator), showTime: getDateFormat(indicator).includes('HH') };
      rules = isRequired ? 'selectRequired' : undefined;
      break;
    }
    case 9: {
      component = 'FileUpload';
      const acceptStr = getAcceptTypes(indicator);
      const acceptArray = acceptStr ? acceptStr.split(',').map((ext: string) => ext.trim().toLowerCase().replace(/^\./, '')) : undefined;
      componentProps = { accept: acceptArray, maxCount: getMaxFileCount(indicator) };
      break;
    }
    case 10: {
      component = 'Select';
      componentProps = { placeholder: `请选择${indicator.indicatorName}`, options: parseOptions(indicator.valueOptions), showSearch: getShowSearch(indicator), allowClear: true };
      rules = isRequired ? 'selectRequired' : undefined;
      break;
    }
    case 11: {
      component = 'VbenMultiSelect';
      componentProps = { placeholder: `请选择${indicator.indicatorName}`, options: parseOptions(indicator.valueOptions), showSearch: getShowSearch(indicator) };
      rules = isRequired ? 'selectRequired' : undefined;
      break;
    }
    case 12: {
      component = 'IndicatorContainer';
      componentProps = { valueOptions: indicator.valueOptions, indicatorCode: code, lastPeriodValues: lastPeriodValues.value[code], containerType: getContainerType(indicator.valueOptions) };
      break;
    }
  }

  void formValues[code];

  const label = `${indicator.indicatorCode} - ${indicator.indicatorName}`;
  const help = hasIndicatorSpec(indicator) ? buildSpecHelp(indicator) : undefined;
  const description = buildLastPeriodHtml(code);

  const isComputed = isComputedIndicator(indicator);
  const suffix = isComputed
    ? h('span', {
        style: 'font-size:11px;padding:1px 6px;background:#fff7e6;color:#fa8c16;border:1px solid #ffd591;border-radius:4px;font-weight:500;white-space:nowrap;',
      }, '自动计算') as any
    : undefined;

  return {
    component: component as any,
    fieldName: code,
    label,
    componentProps,
    rules,
    dependencies: deps,
    help,
    description: description || undefined,
    suffix,
    defaultValue: formValues[code],
  };
}

const formSchema = computed<VbenFormSchema[]>(() => {
  const schemas: VbenFormSchema[] = [];
  for (const group of indicatorGroups.value) {
    const groupLabel = `${group.groupPrefix ? group.groupPrefix + ' ' : ''}${group.groupName}`;
    schemas.push({
      component: 'Divider',
      fieldName: `__group_${group.groupId}`,
      label: '',
      description: groupLabel,
      defaultValue: undefined,
    } as any);
    for (const ind of group.indicators) {
      if (isIndicatorVisible(ind.indicatorCode)) {
        schemas.push(buildSchemaForIndicator(ind));
      }
    }
  }
  return schemas;
});

// ==================== 事件处理 ====================
async function onValuesChange(values: Record<string, any>) {
  Object.assign(formValues, values);
  markDirty();
  emit('update');

  if (isLoadingData.value) return;

  for (const code of Object.keys(values)) {
    const indicator = indicators.value.find((i) => i.indicatorCode === code);
    if (!indicator) continue;
    checkAndSyncLinkedAutoContainers(code, indicators.value);
    recalculateLinkage();
    validateLogicRuleForBlur(indicator, indicators.value, setFieldError, clearFieldError, setDirty);
    if (hasAnyLastPeriodValue()) {
      validatePositiveRuleForIndicator(code, indicators.value);
    }
  }
}

async function onSubmit(_values: Record<string, any>) {
  const allErrors = doValidateAll();
  if (allErrors.length > 0) {
    message.error(allErrors[0]!.message);
    return;
  }
  message.success('提交成功');
}

// ==================== 校验 ====================
function doValidateAll() {
  const allErrors: any[] = [];

  const { messages } = validateAll(indicators.value, setFieldError, clearFieldError);
  for (const m of messages) {
    allErrors.push({ indicatorId: m.indicatorId, indicatorCode: m.indicatorCode, indicatorName: m.indicatorName || '', message: m.message, errorType: m.errorType || 'format' });
  }

  const logicErrors = validateLogicRules(indicators.value, setFieldError, clearFieldError);
  for (const err of logicErrors) {
    const key = err.containerFieldKey || `t:${err.indicatorId}`;
    setFieldError(key, err.message, 'logic', false);
    allErrors.push({ indicatorCode: err.indicatorCode, indicatorName: err.indicatorName || '', message: err.message, errorType: 'logic' });
  }

  if (hasAnyLastPeriodValue()) {
    const positiveErrors = validateAllPositiveRules(indicators.value);
    for (const e of positiveErrors) {
      setFieldError(e.errorKey, `${e.ruleName}：${e.message}`, 'joint', false);
      allErrors.push({ indicatorCode: e.indicatorCode, indicatorName: '', message: `${e.ruleName}：${e.message}`, errorType: 'joint' });
    }
  }

  return allErrors;
}

// ==================== 暴露 API ====================
function getContainerValuesResult(): Record<string, string> {
  const result: Record<string, string> = {};
  for (const ind of indicators.value) {
    if (ind.valueType === 12) result[ind.indicatorCode] = JSON.stringify(containerValues[ind.indicatorCode] || []);
  }
  return result;
}

function getAllIndicatorValues(): Array<{
  indicatorId: number; indicatorCode: string; valueType: number;
  valueStr?: string; valueNum?: string; valueText?: string; valueBool?: boolean;
  valueDate?: string; valueDateStart?: string; valueDateEnd?: string;
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

function getFillProgress(): { total: number; filled: number; percentage: number } {
  const all = indicators.value;
  let filled = 0;
  for (const ind of all) {
    if (ind.valueType === 12) {
      const entries = containerValues[ind.indicatorCode] || [];
      if (entries.length > 0) {
        const fields = parseDynamicFields(ind.valueOptions);
        const hasAllRequired = fields.filter((f) => f.required).every((f) => {
          const val = entries[0][`${entries[0].rowKey}${f.fieldCode}`];
          return val !== undefined && val !== null && val !== '';
        });
        if (hasAllRequired) filled++;
      }
    } else {
      const val = formValues[ind.indicatorCode];
      if (val !== undefined && val !== null && val !== '') filled++;
    }
  }
  return { total: all.length, filled, percentage: all.length > 0 ? Math.round((filled / all.length) * 100) : 0 };
}

defineExpose({
  getContainerValues: getContainerValuesResult,
  getAllIndicatorValues,
  getAllIndicators: () => indicators.value,
  validateAll: doValidateAll,
  validateFilledData,
  recalculateComputedIndicators: () => {},
  syncContainerValuesToForm: () => {
    for (const ind of indicators.value) {
      if (ind.valueType === 12) formValues[ind.indicatorCode] = JSON.stringify(containerValues[ind.indicatorCode] || []);
    }
  },
  syncAllAutoEntryContainers,
  containerValues,
  isDirty,
  resetDirty,
  getFillProgress,
  scrollToField: () => {},
  isIndicatorVisible,
  isIndicatorDisabled,
  isIndicatorRequired,
});
</script>

<template>
  <div v-if="initialized" class="indicator-form-wrapper">
    <!-- schema 现在通过 formConfig 响应式传入，不再需要 prop -->
    <Form
      layout="vertical"
      @submit="onSubmit"
      @values-change="onValuesChange"
    />
  </div>
  <div v-else class="indicator-form-loading">
    加载中...
  </div>
</template>

<style scoped>
.indicator-form-wrapper {
  padding: 4px 0;
}

/* 分组标题 Divider 美化 */
.indicator-form-wrapper :deep(.vben-divider) {
  font-size: 15px;
  font-weight: 600;
  color: hsl(var(--foreground));
  border-color: hsl(var(--primary));
  margin: 24px 0 16px;
}

/* 分组标题 description 文字 */
.indicator-form-wrapper :deep(.vben-divider ~ div [class*="description"]),
.indicator-form-wrapper :deep([class*="divider"] + div [class*="description"]) {
  display: none;
}

/* label 文字样式 */
.indicator-form-wrapper :deep(.form-label) {
  font-size: 14px !important;
  font-weight: 500 !important;
  color: hsl(var(--foreground)) !important;
}

/* 强制 description（上期值）换行到下一行 */
.indicator-form-wrapper :deep(.flex-auto > div:first-child) {
  flex-direction: column !important;
  align-items: stretch !important;
  gap: 4px;
}

/* description 样式 - 上期值绿色 */
.indicator-form-wrapper :deep(.flex-auto .ml-1) {
  margin-left: 0 !important;
  font-size: 14px;
  font-weight: 500;
  color: hsl(var(--success)) !important;
}

/* 自动计算标签 */
.indicator-form-wrapper :deep(.flex-auto .ml-1 + span) {
  margin-left: 0 !important;
}
.indicator-form-loading {
  text-align: center;
  color: #999;
  padding: 40px;
}
</style>
