<script lang="ts" setup>
/**
 * IndicatorInputTable 主入口组件
 * 整合所有 composables 和子组件，提供完整的指标填报功能
 */

import { reactive, ref, computed, onMounted, watch, nextTick } from 'vue';
import dayjs from 'dayjs';

import { IconifyIcon, PlusOutlined } from '@vben/icons';
import { message } from 'ant-design-vue';
import { Upload } from 'ant-design-vue';
import type { UploadFile } from 'ant-design-vue/es/upload/interface';

import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import {
  getIndicatorsByBusinessType,
  getLastPeriodValues,
  getProgressReportIndicatorValues,
} from '#/api/declare/indicator';
import { getIndicatorGroupTreeByProjectType } from '#/api/declare/indicator-group';
import {
  getEnabledJointRules,
  type DeclareIndicatorJointRuleApi,
} from '#/api/declare/jointRule';
import { uploadFile } from '#/api/infra/file';
import { validate as validateJointRule, parseLogicRule } from '#/utils/indicatorValidator';
import { SafeNumberInput } from '#/components/safe-number-input';

import type {
  IndicatorGroup,
  DynamicField,
  ValidationError,
  ContainerType,
  ContainerConfig,
} from './types';

// 子组件
import IndicatorGroupSection from './components/IndicatorGroupSection.vue';
import ContainerFieldRenderer from './components/container-fields/ContainerFieldRenderer.vue';

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
}>();

// ==================== 状态定义 ====================

const mounted = ref(false);
const groupInfoMap = ref<Record<number, { groupName: string; groupPrefix: string; parentId: number; groupLevel: number }>>({});
const indicators = ref<DeclareIndicatorApi.Indicator[]>([]);
const formValues = reactive<Record<string, any>>({});
const isDirty = ref(false);
const fileListMap = reactive<Record<string, UploadFile[]>>({});
const containerValues = reactive<Record<string, Record<string, any>>>({});
const containerFieldDirty = reactive<Record<string, boolean>>({});
/** 容器字段即时错误（失焦时显示） */
const containerFieldInstantErrors = reactive<Record<string, string>>({});
const topLevelFieldDirty = reactive<Record<string, boolean>>({});
const lastPeriodValues = ref<Record<string, string>>({});
const jointRules = ref<DeclareIndicatorJointRuleApi.JointRule[]>([]);
const logicRuleErrors = reactive<Record<string, string>>({});

// ==================== 输入型选项相关 ====================
/** 输入型选项的分隔符 */
const INPUT_VALUE_SEPARATOR = '∵';

/** 输入型选项的值存储（key: indicatorCode + '_' + optionValue） */
const inputTypeValues = reactive<Record<string, string>>({});

/** 输入型选项即时错误（失焦时显示） */
const inputTypeInstantErrors = reactive<Record<string, string>>({});

// ==================== Composable 函数 ====================

/** 指标分组计算属性 */
const indicatorGroups = computed<IndicatorGroup[]>(() => {
  const levelOneMap = new Map<number, IndicatorGroup>();
  const levelTwoMap = new Map<number, IndicatorGroup>();

  for (const [gid, info] of Object.entries(groupInfoMap.value)) {
    if (info.parentId === 0) {
      levelOneMap.set(Number(gid), {
        groupId: Number(gid),
        groupName: info.groupName,
        groupPrefix: info.groupPrefix,
        parentId: 0,
        groupLevel: 1,
        indicators: [],
        children: [],
      });
    }
  }

  for (const [gid, info] of Object.entries(groupInfoMap.value)) {
    if (info.parentId !== 0) {
      const lvl2: IndicatorGroup = {
        groupId: Number(gid),
        groupName: info.groupName,
        groupPrefix: info.groupPrefix,
        parentId: info.parentId,
        groupLevel: 2,
        indicators: [],
        children: [],
      };
      levelTwoMap.set(Number(gid), lvl2);
      const parent = levelOneMap.get(info.parentId);
      if (parent) parent.children.push(lvl2);
    }
  }

  for (const ind of indicators.value) {
    const gid = ind.groupId || 0;
    const info = groupInfoMap.value[gid];
    if (!info) continue;
    if (info.parentId === 0) {
      levelOneMap.get(gid)?.indicators.push(ind);
    } else {
      levelTwoMap.get(gid)?.indicators.push(ind);
    }
  }

  const sortInds = (g: IndicatorGroup) => {
    g.indicators.sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0));
    g.children.forEach(sortInds);
  };
  for (const [, lvl1] of levelOneMap) {
    sortInds(lvl1);
    lvl1.children.sort((a, b) => a.groupId - b.groupId);
  }

  const result: IndicatorGroup[] = [];
  for (const [, lvl1] of levelOneMap) {
    result.push(lvl1);
    result.push(...lvl1.children);
  }
  return result;
});

// ==================== 工具函数 ====================

const MAX_CONTAINER_ENTRIES = 99;

function markDirty() {
  isDirty.value = true;
}

function resetDirty() {
  isDirty.value = false;
}

function markTopLevelDirty(indicatorId: number | undefined) {
  if (indicatorId !== undefined) topLevelFieldDirty['in_' + indicatorId] = true;
}

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

function parseOptions(valueOptions: string): Array<{ value: string; label: string; exclusive?: boolean; inputType?: boolean }> {
  if (!valueOptions) return [];
  try {
    const parsed = JSON.parse(valueOptions);
    return Array.isArray(parsed)
      ? parsed.map((item: any) => ({
          value: String(item.value),
          label: item.label ?? item.value,
          exclusive: item.exclusive == true,
          inputType: Number(item.value) >= 1000,
        }))
      : [];
  } catch {
    return [];
  }
}

function parseExtraConfig(extraConfig: string | undefined): Record<string, any> {
  if (!extraConfig) return {};
  try {
    return JSON.parse(extraConfig);
  } catch {
    return {};
  }
}

// ==================== 输入型选项辅助函数 ====================
/** 序列化输入型选项值（保存时调用） */
function serializeInputTypeValue(value: string, inputContent: string): string {
  if (!inputContent) return value;
  return `${value}${INPUT_VALUE_SEPARATOR}${inputContent}`;
}

/** 反序列化输入型选项值（读取时调用） */
function deserializeInputTypeValue(optionValue: string): { value: string; input: string } {
  const idx = optionValue.indexOf(INPUT_VALUE_SEPARATOR);
  if (idx === -1) {
    return { value: optionValue, input: '' };
  }
  return {
    value: optionValue.substring(0, idx),
    input: optionValue.substring(idx + INPUT_VALUE_SEPARATOR.length),
  };
}

/** 校验输入内容 */
function validateInputContent(content: string): string | null {
  if (content.includes(INPUT_VALUE_SEPARATOR)) {
    return `输入内容不能包含特殊符号 "∵"`;
  }
  if (content.trim() !== '' && /^\d+$/.test(content)) {
    return '输入内容不能是纯数字';
  }
  return null;
}

/** 获取单选的原始值 */
function getInputTypeRawValue(indicator: DeclareIndicatorApi.Indicator): string | undefined {
  return formValues[indicator.indicatorCode];
}

/** 获取单选的纯选项值（用于 radio :value） */
function getInputTypeOptionValue(rawValue: string | undefined): string {
  if (!rawValue) return '';
  return deserializeInputTypeValue(rawValue).value;
}

/** 获取多选的原始值数组 */
function getInputTypeRawValues(indicator: DeclareIndicatorApi.Indicator): string[] {
  const raw = formValues[indicator.indicatorCode];
  if (!raw) return [];
  if (Array.isArray(raw)) return raw;
  return [raw];
}

/** 获取多选的纯选项值数组（用于 checkbox :value） */
function getInputTypeOptionValues(rawValues: string[]): string[] {
  return rawValues.map(v => deserializeInputTypeValue(v).value);
}

/** 输入框的 formValues key（用于 v:if 判断输入框是否显示） */
function getInputTypeInputFieldName(code: string, optionValue: string): string {
  return `${code}_input_${optionValue}`;
}

/** 是否选中了某个输入型选项 */
function isInputTypeOptionSelected(indicator: DeclareIndicatorApi.Indicator, optionValue: string): boolean {
  const raw = formValues[indicator.indicatorCode];
  if (!raw) return false;
  const values = Array.isArray(raw) ? raw : [raw];
  return values.some(v => deserializeInputTypeValue(v).value === optionValue);
}

/** 获取输入型选项的错误信息 */
function getInputTypeError(indicator: DeclareIndicatorApi.Indicator, opt: any): string | null {
  const inputKey = indicator.indicatorCode + '_' + opt.value;
  const indicatorId = indicator.id;
  const content = inputTypeValues[inputKey] || '';

  // 优先返回即时错误（失焦时设置的）
  if (indicatorId !== undefined && logicRuleErrors[String(indicatorId)]) {
    return logicRuleErrors[String(indicatorId)];
  }

  // 返回格式错误
  return validateInputContent(content);
}

/** 单选变化处理 */
function handleInputTypeRadioChange(indicator: DeclareIndicatorApi.Indicator, val: string) {
  const options = parseOptions(indicator.valueOptions);
  // 遍历所有选项：选中的设置子字段，取消选中的删除子字段
  options.forEach((opt) => {
    const inputFieldName = getInputTypeInputFieldName(indicator.indicatorCode, opt.value);
    if (opt.value === val) {
      const inputKey = indicator.indicatorCode + '_' + val;
      const inputContent = inputTypeValues[inputKey] || '';
      const serialized = serializeInputTypeValue(val, inputContent);
      formValues[indicator.indicatorCode] = serialized;
      formValues[inputFieldName] = inputContent;
    } else {
      delete formValues[inputFieldName];
    }
  });
  // 清除该指标的即时错误
  if (indicator.id !== undefined) delete logicRuleErrors[String(indicator.id)];
}

/** 处理多选输入型 checkbox 点击（用于互斥逻辑） */
function handleCheckboxInputClick(indicator: DeclareIndicatorApi.Indicator, clickedValue: string, _event: MouseEvent) {
  const options = parseOptions(indicator.valueOptions);
  const exclusiveValues = new Set(options.filter((o) => o.exclusive).map((o) => o.value));
  const currentRaw = formValues[indicator.indicatorCode];
  const currentValues = currentRaw ? (Array.isArray(currentRaw) ? currentRaw.map(v => deserializeInputTypeValue(v).value) : [deserializeInputTypeValue(currentRaw).value]) : [];

  // 判断用户是选中还是取消
  const wasSelected = currentValues.includes(clickedValue);
  const isExclusive = exclusiveValues.has(clickedValue);

  let result: string[];

  if (isExclusive) {
    // 点击互斥选项：直接设置/取消互斥选项
    if (wasSelected) {
      // 取消互斥选项
      result = [];
    } else {
      // 选中互斥选项，清空其他所有选项
      result = [clickedValue];
    }
  } else {
    // 点击非互斥选项
    if (wasSelected) {
      // 取消该选项
      result = currentValues.filter(v => v !== clickedValue);
    } else {
      // 选中该选项，同时移除所有互斥选项
      result = [...currentValues.filter(v => !exclusiveValues.has(v)), clickedValue];
    }
  }

  // 调用 handleInputTypeCheckboxChange 处理结果
  handleInputTypeCheckboxChange(indicator, result);
}

/** 多选变化处理 */
function handleInputTypeCheckboxChange(indicator: DeclareIndicatorApi.Indicator, vals: string[]) {
  const options = parseOptions(indicator.valueOptions);
  const exclusiveValues = new Set(options.filter((o) => o.exclusive).map((o) => o.value));

  // 互斥逻辑：如果 vals 包含互斥选项，只保留该互斥选项
  let result: string[];
  if (vals.some((v) => exclusiveValues.has(v))) {
    result = [vals.find((v) => exclusiveValues.has(v))!];
  } else {
    result = vals;
  }

  // 序列化并更新 formValues 主字段
  const serialized = result.map((v) => {
    const inputKey = indicator.indicatorCode + '_' + v;
    const inputContent = inputTypeValues[inputKey] || '';
    return serializeInputTypeValue(v, inputContent);
  });
  formValues[indicator.indicatorCode] = serialized;

  // 同步更新 formValues 子字段（用于 v:if 判断输入框是否显示）
  options.forEach((opt) => {
    const inputFieldName = getInputTypeInputFieldName(indicator.indicatorCode, opt.value);
    if (result.includes(opt.value)) {
      formValues[inputFieldName] = inputTypeValues[indicator.indicatorCode + '_' + opt.value] || '';
    } else {
      delete formValues[inputFieldName];
    }
  });

  // 清除该指标的即时错误
  if (indicator.id !== undefined) delete logicRuleErrors[String(indicator.id)];
}

/** 输入框失焦时的校验 */
function onInputTypeBlur(indicator: DeclareIndicatorApi.Indicator, opt: any, value?: string) {
  const indicatorId = indicator.id;
  const inputKey = indicator.indicatorCode + '_' + opt.value;
  const content = (value !== undefined ? value : inputTypeValues[inputKey]) || '';
  const inputFieldName = getInputTypeInputFieldName(indicator.indicatorCode, opt.value);

  // 同步更新 formValues 主字段（保存时使用）以及 formValues 子字段（v:if 判断用）
  const raw = formValues[indicator.indicatorCode];
  if (raw) {
    const deserialized = deserializeInputTypeValue(raw);
    const serialized = serializeInputTypeValue(deserialized.value, content);
    formValues[indicator.indicatorCode] = serialized;
  }
  formValues[inputFieldName] = content;

  // 清除之前的即时错误
  delete inputTypeInstantErrors[inputKey];
  if (indicatorId !== undefined) delete logicRuleErrors[String(indicatorId)];

  // 必填校验
  if (!content.trim()) {
    if (indicatorId !== undefined) {
      logicRuleErrors[String(indicatorId)] = `请填写"${opt.label}"的补充内容`;
      markTopLevelDirty(indicatorId);
    }
    return;
  }

  // 格式校验
  const error = validateInputContent(content);
  if (error && indicatorId !== undefined) {
    logicRuleErrors[String(indicatorId)] = `"${opt.label}"的补充内容：${error}`;
    markTopLevelDirty(indicatorId);
  }
}

/** 校验输入型选项是否必填（选中时必须填写内容） */
function validateInputTypeRequired(indicator: DeclareIndicatorApi.Indicator): string | null {
  const options = parseOptions(indicator.valueOptions);
  const raw = formValues[indicator.indicatorCode];
  const values = raw ? (Array.isArray(raw) ? raw : [raw]) : [];

  for (const opt of options) {
    if (opt.inputType) {
      const isSelected = values.some(v => deserializeInputTypeValue(v).value === opt.value);
      if (isSelected) {
        const inputKey = indicator.indicatorCode + '_' + opt.value;
        const content = inputTypeValues[inputKey] || '';
        if (!content.trim()) {
          return `请填写"${opt.label}"的补充内容`;
        }
        const formatError = validateInputContent(content);
        if (formatError) {
          return `"${opt.label}"的补充内容：${formatError}`;
        }
      }
    }
  }
  return null;
}

function parseContainerConfig(valueOptions: string): ContainerConfig {
  if (!valueOptions) return { mode: 'normal', fields: [] };
  try {
    const parsed = JSON.parse(valueOptions);
    if (Array.isArray(parsed)) return { mode: 'normal', fields: parsed };
    return {
      mode: (parsed.mode as ContainerType) || 'normal',
      link: parsed.link,
      fields: parsed.fields || [],
    };
  } catch {
    return { mode: 'normal', fields: [] };
  }
}

function getContainerType(valueOptions: string): ContainerType {
  return parseContainerConfig(valueOptions).mode;
}

function getAutoEntryLink(valueOptions: string): string | undefined {
  return parseContainerConfig(valueOptions).link;
}

function parseDynamicFields(valueOptions: string): DynamicField[] {
  return parseContainerConfig(valueOptions).fields;
}

function getNumberPrecision(indicator: DeclareIndicatorApi.Indicator): number | undefined {
  const cfg = parseExtraConfig(indicator.extraConfig);
  return cfg.precision !== undefined ? Number(cfg.precision) : undefined;
}

function getDateFormat(indicator: DeclareIndicatorApi.Indicator): string {
  return parseExtraConfig(indicator.extraConfig).format || 'YYYY-MM-DD';
}

function getBooleanLabels(indicator: DeclareIndicatorApi.Indicator): { true: string; false: string } {
  const cfg = parseExtraConfig(indicator.extraConfig);
  return { true: cfg.trueLabel || '是', false: cfg.falseLabel || '否' };
}

function getMaxLength(indicator: DeclareIndicatorApi.Indicator): number | undefined {
  const cfg = parseExtraConfig(indicator.extraConfig);
  return cfg.maxLength !== undefined ? Number(cfg.maxLength) : undefined;
}

function getShowSearch(indicator: DeclareIndicatorApi.Indicator): boolean {
  return parseExtraConfig(indicator.extraConfig).showSearch === true;
}

function getAcceptTypes(indicator: DeclareIndicatorApi.Indicator): string {
  return parseExtraConfig(indicator.extraConfig).accept || '';
}

function getMaxFileCount(indicator: DeclareIndicatorApi.Indicator): number {
  const cfg = parseExtraConfig(indicator.extraConfig);
  if (cfg.maxCount !== undefined) return Number(cfg.maxCount);
  return indicator.maxValue ? Number(indicator.maxValue) : 5;
}

function getFileList(indicatorCode: string): UploadFile[] {
  return fileListMap[indicatorCode] || [];
}

function toNumber(val: any): number | undefined {
  if (val == null || val === '') return undefined;
  const n = Number(val);
  return isNaN(n) ? undefined : n;
}

function isRichText(indicator: DeclareIndicatorApi.Indicator): boolean {
  return parseExtraConfig(indicator.extraConfig).richText === true;
}

// ==================== 容器值管理 ====================

function getContainerEntries(indicatorCode: string): any[] {
  const entriesMap = containerValues[indicatorCode] || {};
  return Object.values(entriesMap);
}

function getEntryFieldValue(entry: any, indicatorCode: string, fieldCode: string, isArray = false): any {
  const fullKey = generateContainerFieldKey(indicatorCode, entry.rowKey, fieldCode);
  const val = entry?.[fullKey];
  return isArray ? (val || []) : val;
}

function setEntryFieldValue(entry: any, indicatorCode: string, fieldCode: string, value: any) {
  const fullKey = generateContainerFieldKey(indicatorCode, entry.rowKey, fieldCode);
  entry[fullKey] = value;
}

function generateContainerFieldKey(_indicatorCode: string, rowKey: string, fieldCode: string): string {
  return `${rowKey}${fieldCode}`;
}

function generateContainerRowKey(indicatorCode: string, index: number): string {
  return `${indicatorCode}${String(index).padStart(2, '0')}`;
}

function generateConditionalRowKey(indicatorCode: string): string {
  return indicatorCode;
}

function getMaxEntryIndex(entries: any[]): number {
  let maxIndex = 0;
  for (const entry of entries) {
    const match = entry.rowKey.match(/(\d+)$/);
    if (match) {
      const idx = parseInt(match[1]!, 10);
      if (idx > maxIndex) maxIndex = idx;
    }
  }
  return maxIndex;
}

function generateNextContainerRowKey(indicatorCode: string): string | undefined {
  const entries = containerValues[indicatorCode] || [];
  const maxIndex = getMaxEntryIndex(entries);
  const nextIndex = maxIndex + 1;
  if (nextIndex > MAX_CONTAINER_ENTRIES) return undefined;
  return generateContainerRowKey(indicatorCode, nextIndex);
}

function renumberContainerEntries(indicatorCode: string) {
  const entries = containerValues[indicatorCode] || [];
  entries.forEach((entry, idx) => {
    entry.rowKey = generateContainerRowKey(indicatorCode, idx + 1);
  });
}

function extractEntryIndex(rowKey: string): number {
  const match = rowKey.match(/(\d+)$/);
  return match ? parseInt(match[1]!, 10) : 1;
}

function getContainerFieldFullErrorKey(entry: any, indicatorCode: string, fieldCode: string): string {
  return generateContainerFieldKey(indicatorCode, entry.rowKey, fieldCode);
}

function handleAddEntry(indicatorCode: string) {
  const indicator = indicators.value.find((i) => i.indicatorCode === indicatorCode);
  if (!indicator) return;
  const containerType = getContainerType(indicator.valueOptions);
  if (containerType === 'conditional') return;
  if (!containerValues[indicatorCode]) containerValues[indicatorCode] = [];
  const newRowKey = generateNextContainerRowKey(indicatorCode);
  if (!newRowKey) {
    message.warning(`容器 ${indicatorCode} 的条目数量已达到上限（${MAX_CONTAINER_ENTRIES}）`);
    return;
  }
  containerValues[indicatorCode].push({ rowKey: newRowKey });
}

function handleRemoveEntry(indicatorCode: string, rowKey: string) {
  const entries = containerValues[indicatorCode] || [];
  const index = entries.findIndex((e: any) => e.rowKey === rowKey);
  if (index !== -1) {
    const entry = entries[index];
    const indicator = indicators.value.find((i) => i.indicatorCode === indicatorCode);
    if (indicator && entry) {
      const fields = parseDynamicFields(indicator.valueOptions);
      for (const field of fields) {
        const key = generateContainerFieldKey(indicatorCode, entry.rowKey, field.fieldCode);
        delete containerFieldDirty[key];
        delete logicRuleErrors[key];
      }
    }
    containerValues[indicatorCode]!.splice(index, 1);
    renumberContainerEntries(indicatorCode);
  }
  onIndicatorChange({ indicatorCode, valueType: 12 } as any);
}

function isFieldVisible(entry: any, indicatorCode: string, field: DynamicField, allFields: DynamicField[]): boolean {
  if (!field.showCondition) return true;
  const cond = field.showCondition;
  const watchFieldFullKey = generateContainerFieldKey(indicatorCode, entry.rowKey, cond.watchField);
  const watchVal = entry?.[watchFieldFullKey];
  const { operator, value } = cond;
  const watchedField = allFields.find((f) => f.fieldCode === cond.watchField);
  const isBooleanWatch = watchedField?.fieldType === 'boolean';
  let result: boolean;
  switch (operator) {
    case 'eq':
      if (isBooleanWatch) {
        const boolVal = value === 'true' || value === '1' || value === true;
        result = watchVal === boolVal;
      } else {
        result = watchVal === value;
      }
      break;
    case 'neq':
      if (isBooleanWatch) {
        const boolVal = value === 'true' || value === '1' || value === true;
        result = watchVal !== boolVal;
      } else {
        result = watchVal !== value;
      }
      break;
    case 'gt': result = Number(watchVal) > Number(value); break;
    case 'gte': result = Number(watchVal) >= Number(value); break;
    case 'lt': result = Number(watchVal) < Number(value); break;
    case 'lte': result = Number(watchVal) <= Number(value); break;
    case 'in': result = Array.isArray(value) && value.includes(watchVal); break;
    case 'notEmpty': result = watchVal !== undefined && watchVal !== null && watchVal !== ''; break;
    case 'isEmpty': result = watchVal === undefined || watchVal === null || watchVal === ''; break;
    default: result = true;
  }
  return result;
}

function getVisibleFields(indicatorCode: string, valueOptions: string, entry: any): DynamicField[] {
  const fields = parseDynamicFields(valueOptions);
  return fields.filter((f) => isFieldVisible(entry, indicatorCode, f, fields));
}

function syncAutoEntryContainerCount(containerCode: string, linkedIndicatorCode: string) {
  const linkedValue = formValues[linkedIndicatorCode];
  const targetCount = Math.max(0, Math.floor(Number(linkedValue)) || 0);
  if (targetCount > MAX_CONTAINER_ENTRIES) {
    message.warning(`自动条目容器最多支持 ${MAX_CONTAINER_ENTRIES} 个条目，已截断`);
  }
  const effectiveTarget = Math.min(targetCount, MAX_CONTAINER_ENTRIES);
  if (!containerValues[containerCode]) containerValues[containerCode] = [];
  const currentEntries = containerValues[containerCode];
  if (effectiveTarget <= 0) {
    // 目标数量为 0 时，清理已有的空条目
    if (currentEntries.length > 0) {
      containerValues[containerCode] = [];
    }
    return;
  }
  if (currentEntries.length < effectiveTarget) {
    const startIndex = currentEntries.length + 1;
    for (let i = startIndex; i <= effectiveTarget; i++) {
      const rowKey = generateContainerRowKey(containerCode, i);
      currentEntries.push({ rowKey });
    }
  } else if (currentEntries.length > effectiveTarget) {
    currentEntries.splice(effectiveTarget);
  }
  renumberContainerEntries(containerCode);
}

function initializeAutoEntryContainers() {
  for (const indicator of indicators.value) {
    if (indicator.valueType !== 12) continue;
    const link = getAutoEntryLink(indicator.valueOptions);
    if (link) syncAutoEntryContainerCount(indicator.indicatorCode, link);
  }
}

function checkAndSyncLinkedAutoContainers(changedCode: string) {
  for (const indicator of indicators.value) {
    if (indicator.valueType !== 12) continue;
    const link = getAutoEntryLink(indicator.valueOptions);
    if (link === changedCode) syncAutoEntryContainerCount(indicator.indicatorCode, changedCode);
  }
}

/** 同步所有自动条目容器（在保存前调用） */
function syncAllAutoEntryContainers() {
  for (const indicator of indicators.value) {
    if (indicator.valueType !== 12) continue;
    const link = getAutoEntryLink(indicator.valueOptions);
    if (link) syncAutoEntryContainerCount(indicator.indicatorCode, link);
  }
}

function getLinkedIndicatorName(indicator: any): string {
  const link = getAutoEntryLink(indicator.valueOptions);
  if (!link) return '';
  const linkedIndicator = indicators.value.find((i) => i.indicatorCode === link);
  return linkedIndicator?.indicatorName || link;
}

function isAutoEntryVisible(indicator: any): boolean {
  const link = getAutoEntryLink(indicator.valueOptions);
  if (!link) return false;
  const linkedValue = formValues[link];
  return Math.max(0, Math.floor(Number(linkedValue))) > 0;
}

// ==================== 校验逻辑 ====================

/** 数据库 decimal(18,4) 的范围限制 */
const DECIMAL_18_4_MAX = 90000000000.00;
const DECIMAL_18_4_MIN = -90000000000.00;

function isEmpty(value: any): boolean {
  if (value === undefined || value === null || value === '') return true;
  if (typeof value === 'string') {
    const t = value.trim();
    if (t === '' || t === '[]' || t === '[ ]') return true;
  }
  return false;
}

function checkRequired(value: any, isRequired: boolean): string | null {
  if (isRequired && isEmpty(value)) return '此项为必填';
  return null;
}

function checkRange(value: number, min: number | null, max: number | null): string | null {
  // 优先使用配置的 min/max，否则使用 decimal(18,4) 的范围限制
  if (min != null) {
    if (value < min) return `不能小于 ${min}`;
  } else if (value < DECIMAL_18_4_MIN) {
    return `不能小于 ${DECIMAL_18_4_MIN}`;
  }

  if (max != null) {
    if (value > max) return `不能大于 ${max}`;
  } else if (value > DECIMAL_18_4_MAX) {
    return `不能大于 ${DECIMAL_18_4_MAX}`;
  }

  return null;
}

function checkPrecision(value: number, precision: number | undefined): string | null {
  if (precision === undefined) return null;
  if (value !== Number(value.toFixed(precision))) return `小数位数不能超过 ${precision} 位`;
  return null;
}

function checkSelectCount(value: any, minSelect?: number, maxSelect?: number): string | null {
  const arr = Array.isArray(value) ? value : [];
  if (minSelect != null && arr.length < minSelect) return `至少选择 ${minSelect} 项`;
  if (maxSelect != null && arr.length > maxSelect) return `最多选择 ${maxSelect} 项`;
  return null;
}

function pushError(errors: ValidationError[], code: string, message: string, id?: number, containerFieldKey?: string, errorType?: ValidationError['errorType']) {
  errors.push({
    indicatorId: id,
    indicatorCode: code,
    message,
    containerFieldKey,
    errorType,
    indicatorName: undefined,
  });
}

// 按类型校验函数
function validateType1_Number(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
  const errors: ValidationError[] = [];
  const { indicatorCode: code, id, isRequired, minValue, maxValue } = indicator;
  const value = formValues[code];
  console.log('[topLevel number] code:', code, 'value:', value, 'min:', minValue, 'max:', maxValue);
  const isComputed = isComputedIndicator(indicator);
  const reqErr = checkRequired(value, !!isRequired && !isComputed);
  if (reqErr) { pushError(errors, code, `${indicator.indicatorCode} - ${indicator.indicatorName}：${reqErr}`, id, undefined, 'required'); return errors; }
  if (!isEmpty(value)) {
    const numVal = Number(value);
    console.log('[topLevel number] numVal:', numVal, 'checkRange result:', checkRange(numVal, minValue ?? null, maxValue ?? null));
    // 检查是否为有效数字（非 NaN 且非 Infinity）
    if (isNaN(numVal) || !isFinite(numVal)) { pushError(errors, code, `${indicator.indicatorCode} - ${indicator.indicatorName}：请输入有效数字`, id, undefined, 'format'); return errors; }
    const rangeErr = checkRange(numVal, minValue ?? null, maxValue ?? null);
    if (rangeErr) pushError(errors, code, `${indicator.indicatorCode} - ${indicator.indicatorName}：${rangeErr}`, id, undefined, 'range');
    const cfg = parseExtraConfig(indicator.extraConfig);
    const precErr = checkPrecision(numVal, cfg.precision);
    if (precErr) pushError(errors, code, `${indicator.indicatorCode} - ${indicator.indicatorName}：${precErr}`, id, undefined, 'format');
  }
  return errors;
}

function validateType2_Text(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
  const errors: ValidationError[] = [];
  const value = formValues[indicator.indicatorCode];
  const isEmptyVal = value === undefined || value === null || value === '';
  const reqErr = checkRequired(value, !!indicator.isRequired);
  if (reqErr) { pushError(errors, indicator.indicatorCode, `${indicator.indicatorCode} - ${indicator.indicatorName}：${reqErr}`, indicator.id, undefined, 'required'); return errors; }
  if (!isEmptyVal) {
    const trimmed = String(value).trim();
    if (/^-?\d+(\.\d+)?$/.test(trimmed)) {
      pushError(errors, indicator.indicatorCode, `${indicator.indicatorCode} - ${indicator.indicatorName}：不能输入纯数字`, indicator.id, undefined, 'format');
    }
  }
  return errors;
}

function validateType3_Boolean(_: DeclareIndicatorApi.Indicator): ValidationError[] { return []; }
function validateType4_Date(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
  const errors: ValidationError[] = [];
  const reqErr = checkRequired(formValues[indicator.indicatorCode], !!indicator.isRequired);
  if (reqErr) pushError(errors, indicator.indicatorCode, `${indicator.indicatorCode} - ${indicator.indicatorName}：${reqErr}`, indicator.id, undefined, 'required');
  return errors;
}
function validateType5_RichText(indicator: DeclareIndicatorApi.Indicator): ValidationError[] { return validateType2_Text(indicator); }
function validateType6_Select(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
  const errors: ValidationError[] = [];
  const reqErr = checkRequired(formValues[indicator.indicatorCode], !!indicator.isRequired);
  if (reqErr) pushError(errors, indicator.indicatorCode, `${indicator.indicatorCode} - ${indicator.indicatorName}：${reqErr}`, indicator.id, undefined, 'required');
  // 输入型选项必填校验
  if (!reqErr) {
    const inputTypeErr = validateInputTypeRequired(indicator);
    if (inputTypeErr) pushError(errors, indicator.indicatorCode, `${indicator.indicatorCode} - ${indicator.indicatorName}：${inputTypeErr}`, indicator.id, undefined, 'required');
  }
  return errors;
}
function validateType7_MultiSelect(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
  const errors: ValidationError[] = [];
  const value = formValues[indicator.indicatorCode];
  const arr = Array.isArray(value) ? value : [];
  if (indicator.isRequired && arr.length === 0) pushError(errors, indicator.indicatorCode, `${indicator.indicatorCode} - ${indicator.indicatorName}：此项为必填`, indicator.id, undefined, 'required');
  // 输入型选项必填校验
  if (arr.length > 0) {
    const inputTypeErr = validateInputTypeRequired(indicator);
    if (inputTypeErr) pushError(errors, indicator.indicatorCode, `${indicator.indicatorCode} - ${indicator.indicatorName}：${inputTypeErr}`, indicator.id, undefined, 'required');
  }
  return errors;
}
function validateType8_DateRange(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
  const errors: ValidationError[] = [];
  const reqErr = checkRequired(formValues[indicator.indicatorCode], !!indicator.isRequired);
  if (reqErr) pushError(errors, indicator.indicatorCode, `${indicator.indicatorCode} - ${indicator.indicatorName}：${reqErr}`, indicator.id, undefined, 'required');
  return errors;
}
function validateType9_File(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
  const errors: ValidationError[] = [];
  const value = formValues[indicator.indicatorCode];
  const isFileEmpty = isEmpty(value) || value === '[]' || value === '[ ]';
  if (indicator.isRequired && isFileEmpty) pushError(errors, indicator.indicatorCode, `${indicator.indicatorCode} - ${indicator.indicatorName}：此项为必填`, indicator.id, undefined, 'required');
  return errors;
}
function validateType10_Dept(indicator: DeclareIndicatorApi.Indicator): ValidationError[] { return validateType6_Select(indicator); }
function validateType11_User(indicator: DeclareIndicatorApi.Indicator): ValidationError[] { return validateType6_Select(indicator); }

function validateType12_Container(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
  const errors: ValidationError[] = [];
  const { indicatorCode: code, id, indicatorName } = indicator;
  const entries = containerValues[code] || [];
  const fields = parseDynamicFields(indicator.valueOptions);

  for (let i = 0; i < entries.length; i++) {
    const entry = entries[i];
    for (const field of fields) {
      if (!isFieldVisible(entry, code, field, fields)) continue;
      const fullKey = generateContainerFieldKey(code, entry.rowKey, field.fieldCode);
      const fieldValue = entry[fullKey];
      const entryLabel = `第${i + 1}个条目`;
      if (field.required && isEmpty(fieldValue)) {
        pushError(errors, code, `${indicatorName} ${entryLabel}「${field.fieldLabel}」为必填`, id, fullKey, 'required');
        continue;
      }
      if (!isEmpty(fieldValue)) {
        if (field.fieldType === 'number') {
          const numVal = Number(fieldValue);
          if (isNaN(numVal) || !isFinite(numVal)) pushError(errors, code, `${indicatorName} ${entryLabel}「${field.fieldLabel}」：请输入有效数字`, id, fullKey, 'format');
          else {
            const rangeErr = checkRange(numVal, field.minValue ?? 0, field.maxValue);
            if (rangeErr) pushError(errors, code, `${indicatorName} ${entryLabel}「${field.fieldLabel}」：${rangeErr}`, id, fullKey, 'range');
            const precErr = checkPrecision(numVal, field.precision);
            if (precErr) pushError(errors, code, `${indicatorName} ${entryLabel}「${field.fieldLabel}」：${precErr}`, id, fullKey, 'format');
          }
        }
        if (field.fieldType === 'checkbox') {
          const countErr = checkSelectCount(fieldValue, field.minSelect, field.maxSelect);
          if (countErr) pushError(errors, code, `${indicatorName} ${entryLabel}「${field.fieldLabel}：${countErr}`, id, fullKey, 'required');
        }
        if ((field.fieldType === 'text' || field.fieldType === 'textarea') && /^-?\d+(\.\d+)?$/.test(String(fieldValue).trim())) {
          if (field.fieldType === 'text') pushError(errors, code, `${indicatorName} ${entryLabel}「${field.fieldLabel}」：不能输入纯数字`, id, fullKey, 'format');
        }
        // noRepeat 排重验证（遍历其他条目检查是否重复）
        if (field.noRepeat && !isEmpty(fieldValue)) {
          const trimmed = String(fieldValue).trim();
          for (let j = 0; j < entries.length; j++) {
            if (j === i) continue;
            const otherFullKey = generateContainerFieldKey(code, entries[j].rowKey, field.fieldCode);
            const otherVal = entries[j]?.[otherFullKey];
            if (String(otherVal ?? '').trim() === trimmed) {
              pushError(errors, code, `${indicatorName} ${entryLabel}「${field.fieldLabel}」：该值与「第${j + 1}个条目」重复`, id, fullKey, 'range');
              break;
            }
          }
        }
      }
    }
  }
  return errors;
}

/**
 * 容器字段即时验证（用于失焦时即时显示错误）
 * 复用 validateType12_Container 中的单字段验证逻辑
 */
function validateContainerFieldInstant(entry: any, indicator: DeclareIndicatorApi.Indicator, field: DynamicField): ValidationError | null {
  const { indicatorCode: code, id, indicatorName } = indicator;
  const fullKey = generateContainerFieldKey(code, entry.rowKey, field.fieldCode);
  const fieldValue = entry[fullKey];

  // 清除之前的即时错误
  delete containerFieldInstantErrors[fullKey];

  // 必填校验
  if (field.required && isEmpty(fieldValue)) {
    const err = `${indicatorName}「${field.fieldLabel}」为必填`;
    containerFieldInstantErrors[fullKey] = err;
    return {
      indicatorId: id,
      indicatorCode: code,
      message: err,
      containerFieldKey: fullKey,
      errorType: 'required',
    };
  }

  // 非空时校验格式
  if (!isEmpty(fieldValue)) {
    if (field.fieldType === 'number') {
      const numVal = Number(fieldValue);
      // 检查是否为有效数字（非 NaN 且非 Infinity）
      if (isNaN(numVal) || !isFinite(numVal)) {
        const err = `${indicatorName}「${field.fieldLabel}」：请输入有效数字`;
        containerFieldInstantErrors[fullKey] = err;
        return {
          indicatorId: id,
          indicatorCode: code,
          message: err,
          containerFieldKey: fullKey,
          errorType: 'format',
        };
      }
      const rangeErr = checkRange(numVal, field.minValue ?? 0, field.maxValue);
      if (rangeErr) {
        const err = `${indicatorName}「${field.fieldLabel}」：${rangeErr}`;
        containerFieldInstantErrors[fullKey] = err;
        return {
          indicatorId: id,
          indicatorCode: code,
          message: err,
          containerFieldKey: fullKey,
          errorType: 'range',
        };
      }
      const precErr = checkPrecision(numVal, field.precision);
      if (precErr) {
        const err = `${indicatorName}「${field.fieldLabel}」：${precErr}`;
        containerFieldInstantErrors[fullKey] = err;
        return {
          indicatorId: id,
          indicatorCode: code,
          message: err,
          containerFieldKey: fullKey,
          errorType: 'format',
        };
      }
    }
    if (field.fieldType === 'text' || field.fieldType === 'textarea') {
      const trimmed = String(fieldValue).trim();
      if (field.fieldType === 'text' && /^-?\d+(\.\d+)?$/.test(trimmed)) {
        const err = `${indicatorName}「${field.fieldLabel}」：不能输入纯数字`;
        containerFieldInstantErrors[fullKey] = err;
        return {
          indicatorId: id,
          indicatorCode: code,
          message: err,
          containerFieldKey: fullKey,
          errorType: 'format',
        };
      }
      // noRepeat 排重验证（遍历其他条目检查是否重复）
      if (field.noRepeat && trimmed) {
        const entries = containerValues[code] || [];
        for (let j = 0; j < entries.length; j++) {
          if (entries[j] === entry) continue;
          const otherFullKey = generateContainerFieldKey(code, entries[j].rowKey, field.fieldCode);
          const otherVal = entries[j]?.[otherFullKey];
          if (String(otherVal ?? '').trim() === trimmed) {
            const err = `${indicatorName}「${field.fieldLabel}」：该值与「第${j + 1}个条目」重复`;
            containerFieldInstantErrors[fullKey] = err;
            return {
              indicatorId: id,
              indicatorCode: code,
              message: err,
              containerFieldKey: fullKey,
              errorType: 'range',
            };
          }
        }
      }
    }
    if (field.fieldType === 'checkbox') {
      const countErr = checkSelectCount(fieldValue, field.minSelect, field.maxSelect);
      if (countErr) {
        const err = `${indicatorName}「${field.fieldLabel}」：${countErr}`;
        containerFieldInstantErrors[fullKey] = err;
        return {
          indicatorId: id,
          indicatorCode: code,
          message: err,
          containerFieldKey: fullKey,
          errorType: 'required',
        };
      }
    }
  }

  return null;
}

const validators: Record<number, (ind: DeclareIndicatorApi.Indicator) => ValidationError[]> = {
  1: validateType1_Number, 2: validateType2_Text, 3: validateType3_Boolean,
  4: validateType4_Date, 5: validateType5_RichText, 6: validateType6_Select,
  7: validateType7_MultiSelect, 8: validateType8_DateRange, 9: validateType9_File,
  10: validateType10_Dept, 11: validateType11_User, 12: validateType12_Container,
};

function validateAll(indicatorsToValidate: DeclareIndicatorApi.Indicator[]): ValidationError[] {
  const errors: ValidationError[] = [];
  for (const indicator of indicatorsToValidate) {
    const v = validators[indicator.valueType];
    if (v) errors.push(...v(indicator));
  }
  errors.push(...validateLogicRules(indicatorsToValidate));
  // 将容器字段错误同步到即时错误状态（保存时显示）
  for (const error of errors) {
    if (error.containerFieldKey) {
      containerFieldDirty[error.containerFieldKey] = true;
      containerFieldInstantErrors[error.containerFieldKey] = error.message;
    } else if (error.indicatorId !== undefined) {
      topLevelFieldDirty['in_' + error.indicatorId] = true;
    }
  }
  return errors;
}

/**
 * 仅验证已填数据的格式、范围、精度（不做必填验证）
 * 用于保存草稿时检查已填数据的有效性
 */
function validateFilledData(): ValidationError[] {
  const errors: ValidationError[] = [];

  // 1. 验证顶层指标（非容器类型）
  for (const indicator of indicators.value) {
    if (indicator.valueType === 12) continue; // 容器类型单独处理
    const v = validateFilledIndicator;
    if (v) errors.push(...v(indicator));
  }

  // 2. 验证容器指标
  for (const indicator of indicators.value) {
    if (indicator.valueType !== 12) continue;
    const containerErrors = validateFilledContainer(indicator);
    errors.push(...containerErrors);
  }

  // 3. 验证逻辑规则（只验证已填数据的关系）
  const codeValueMap = buildCodeValueMap();
  errors.push(...validateFilledLogicRules(indicators.value, codeValueMap));

  // 将容器字段错误同步到即时错误状态
  for (const error of errors) {
    if (error.containerFieldKey) {
      containerFieldDirty[error.containerFieldKey] = true;
      containerFieldInstantErrors[error.containerFieldKey] = error.message;
    } else if (error.indicatorId !== undefined) {
      topLevelFieldDirty['in_' + error.indicatorId] = true;
    }
  }

  return errors;
}

/** 验证已填的顶层指标（不做必填校验） */
function validateFilledIndicator(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
  const errors: ValidationError[] = [];
  const { indicatorCode: code, id, valueType } = indicator;

  // 跳过容器类型
  if (valueType === 12) return errors;

  const value = formValues[code];
  const isEmptyVal = isEmpty(value);

  // 空值不验证
  if (isEmptyVal) return errors;

  const isComputed = isComputedIndicator(indicator);

  switch (valueType) {
    case 1: { // 数字类型
      const numVal = Number(value);
      if (isNaN(numVal) || !isFinite(numVal)) {
        pushError(errors, code, `${indicator.indicatorName}：请输入有效数字`, id, undefined, 'format');
      } else {
        const rangeErr = checkRange(numVal, indicator.minValue ?? null, indicator.maxValue ?? null);
        if (rangeErr) pushError(errors, code, `${indicator.indicatorName}：${rangeErr}`, id, undefined, 'range');
        const cfg = parseExtraConfig(indicator.extraConfig);
        const precErr = checkPrecision(numVal, cfg.precision);
        if (precErr) pushError(errors, code, `${indicator.indicatorName}：${precErr}`, id, undefined, 'format');
      }
      break;
    }
    case 2: { // 文本类型
      const trimmed = String(value).trim();
      if (/^-?\d+(\.\d+)?$/.test(trimmed)) {
        pushError(errors, code, `${indicator.indicatorName}：不能输入纯数字`, id, undefined, 'format');
      }
      break;
    }
    // 其他类型暂不做额外验证
  }

  return errors;
}

/** 验证已填的容器指标（不做必填校验） */
function validateFilledContainer(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
  const errors: ValidationError[] = [];
  const { indicatorCode: code, id, indicatorName } = indicator;
  const entries = containerValues[code] || [];
  const fields = parseDynamicFields(indicator.valueOptions);

  for (let i = 0; i < entries.length; i++) {
    const entry = entries[i];
    const entryLabel = `第${i + 1}条`;

    for (const field of fields) {
      if (!isFieldVisible(entry, code, field, fields)) continue;
      const fullKey = generateContainerFieldKey(code, entry.rowKey, field.fieldCode);
      const fieldValue = entry[fullKey];
      const isEmptyVal = fieldValue === undefined || fieldValue === null || fieldValue === '';

      // 空值不验证
      if (isEmptyVal) continue;

      if (field.fieldType === 'number') {
        const numVal = Number(fieldValue);
        if (isNaN(numVal) || !isFinite(numVal)) {
          pushError(errors, code, `${indicatorName} ${entryLabel}「${field.fieldLabel}」：请输入有效数字`, id, fullKey, 'format');
        } else {
          const rangeErr = checkRange(numVal, field.minValue ?? 0, field.maxValue);
          if (rangeErr) pushError(errors, code, `${indicatorName} ${entryLabel}「${field.fieldLabel}」：${rangeErr}`, id, fullKey, 'range');
          const precErr = checkPrecision(numVal, field.precision);
          if (precErr) pushError(errors, code, `${indicatorName} ${entryLabel}「${field.fieldLabel}」：${precErr}`, id, fullKey, 'format');
        }
      } else if (field.fieldType === 'text' || field.fieldType === 'textarea') {
        const trimmed = String(fieldValue).trim();
        if (field.fieldType === 'text' && /^-?\d+(\.\d+)?$/.test(trimmed)) {
          pushError(errors, code, `${indicatorName} ${entryLabel}「${field.fieldLabel}」：不能输入纯数字`, id, fullKey, 'format');
        }
      }
    }
  }

  return errors;
}

/** 构建指标编码到值的映射 */
function buildCodeValueMap(): Record<string, any> {
  const map: Record<string, any> = {};
  // 顶层指标
  for (const [code, value] of Object.entries(formValues)) {
    map[code] = value;
  }
  // 容器指标（取第一个条目的值作为代表）
  for (const indicator of indicators.value) {
    if (indicator.valueType !== 12) continue;
    const entries = containerValues[indicator.indicatorCode] || [];
    if (entries.length > 0) {
      const entry = entries[0];
      const fields = parseDynamicFields(indicator.valueOptions);
      for (const field of fields) {
        const fullKey = generateContainerFieldKey(indicator.indicatorCode, entry.rowKey, field.fieldCode);
        map[field.fieldCode] = entry[fullKey];
      }
    }
  }
  return map;
}

/** 验证已填数据的逻辑规则（只验证有值的指标之间的关系） */
function validateFilledLogicRules(indicatorsToValidate: DeclareIndicatorApi.Indicator[], codeValueMap: Record<string, any>): ValidationError[] {
  const errors: ValidationError[] = [];

  for (const indicator of indicatorsToValidate) {
    if (!indicator.logicRule?.trim()) continue;
    const rules = parseLogicRule(indicator.logicRule);
    if (rules.length === 0) continue;

    // 执行逻辑规则验证
    const results = validateJointRule(rules as any, codeValueMap, { triggerTiming: 'FILL' });

    // 如果验证通过（results 为空），不添加错误
    if (results.length === 0) {
      // 规则通过：只清空当前指标的 logicRule 错误（使用指标 id 作为 key）
      if (indicator.id !== undefined) delete logicRuleErrors[String(indicator.id)];
      continue;
    }

    // 验证失败，生成错误消息
    const errMsg = buildLogicRuleMsg(indicator.logicRule, indicatorsToValidate, codeValueMap);
    errors.push({
      indicatorId: indicator.id,
      indicatorCode: indicator.indicatorCode,
      message: errMsg,
      containerFieldKey: undefined,
      errorType: 'logic',
      indicatorName: indicator.indicatorName,
    });
    if (indicator.id !== undefined) logicRuleErrors[String(indicator.id)] = errMsg;
  }

  return errors;
}

// 逻辑规则校验
function buildLogicRuleMsg(logicRule: string, allIndicators: typeof indicators.value, codeValueMap: Record<string, any>): string {
  if (!logicRule) return '校验失败';

  const codeMap = new Map<string, string>();
  for (const ind of allIndicators) {
    if (!codeMap.has(ind.indicatorCode)) codeMap.set(ind.indicatorCode, ind.indicatorName || ind.indicatorCode);
  }
  const replaceCode = (code: string): string => {
    const name = codeMap.get(code) || code;
    const val = codeValueMap[code];
    const valText = val !== undefined && val !== null && val !== '' ? String(val) : '未填';
    return `${name}(${valText})`;
  };

  // 处理 IF 函数格式: IF([指标1] > 阈值1, [指标2] > 阈值2, TRUE)
  const ifMatch = logicRule.trim().match(/^IF\s*\(\s*\[([^\]]+)\]\s*([><]=?)\s*(\d+(?:\.\d+)?)\s*,\s*\[([^\]]+)\]\s*([><]=?)\s*(\d+(?:\.\d+)?)\s*,\s*TRUE\s*\)$/i);
  if (ifMatch) {
    const condCode = ifMatch[1]!.trim();
    const condOp = ifMatch[2]!;
    const condVal = ifMatch[3]!;
    const verifyCode = ifMatch[4]!.trim();
    const verifyOp = ifMatch[5]!;
    const verifyVal = ifMatch[6]!;

    const opText: Record<string, string> = { '>=': '应大于等于', '<=': '应小于等于', '>': '应大于', '<': '应小于' };

    const condMsg = replaceCode(condCode);
    const verifyMsg = replaceCode(verifyCode);

    return `当 ${condMsg} ${condOp} ${condVal} 时, ${verifyMsg} ${opText[verifyOp] || verifyOp} ${verifyVal}`;
  }

  // 普通规则格式处理
  const match = logicRule.trim().match(/^(.+?)\s*(>=|<=|>|<|==|!=)\s*(.+)$/);
  if (!match) return '校验失败';
  const leftRaw = match[1]!.trim();
  const operator = match[2]!;
  const rightRaw = match[3]!.trim();

  const opText: Record<string, string> = { '>=': '应大于等于', '<=': '应小于等于', '>': '应大于', '<': '应小于', '==': '应等于', '!=': '不应等于' };
  const msgLeft = leftRaw.replace(/\[([^\]]+)\]/g, (_, c) => replaceCode(c.trim()));
  const msgRight = rightRaw.replace(/\[([^\]]+)\]/g, (_, c) => replaceCode(c.trim()));
  return `${msgLeft} ${opText[operator] || operator} ${msgRight}`;
}

function validateLogicRules(allIndicators: DeclareIndicatorApi.Indicator[]): ValidationError[] {
  const errors: ValidationError[] = [];
  const codeValueMap: Record<string, any> = {};
  for (const ind of allIndicators) codeValueMap[ind.indicatorCode] = formValues[ind.indicatorCode];
  for (const indicator of allIndicators) {
    if (!indicator.logicRule?.trim()) continue;
    const rules = parseLogicRule(indicator.logicRule);
    if (rules.length === 0) continue;
    const results = validateJointRule(rules as any, codeValueMap, { triggerTiming: 'FILL' });
    if (results.length === 0) {
      // 规则通过：清空该指标的 logicRule 错误（使用指标 id 作为 key）
      if (indicator.id !== undefined) delete logicRuleErrors[String(indicator.id)];
      continue;
    }
    const errMsg = buildLogicRuleMsg(indicator.logicRule, allIndicators, codeValueMap);
    errors.push({ indicatorId: indicator.id, indicatorCode: indicator.indicatorCode, message: errMsg, errorType: 'logic', indicatorName: indicator.indicatorName });
    if (indicator.id !== undefined) logicRuleErrors[String(indicator.id)] = errMsg;
  }
  return errors;
}

function validateLogicRuleForBlur(changedIndicator: DeclareIndicatorApi.Indicator) {
  const allIndicators = indicators.value;
  const changedCode = changedIndicator.indicatorCode;
  const codeValueMap: Record<string, any> = {};
  for (const ind of allIndicators) codeValueMap[ind.indicatorCode] = formValues[ind.indicatorCode];
  for (const indicator of allIndicators) {
    if (!indicator.logicRule?.trim()) continue;
    const involvedCodes = new Set<string>();
    for (const m of indicator.logicRule.matchAll(/\[([^\]]+)\]/g)) involvedCodes.add(m[1]!.trim());
    if (!involvedCodes.has(changedCode)) continue;
    markTopLevelDirty(indicator.id);
    const rules = parseLogicRule(indicator.logicRule);
    const results = validateJointRule(rules as any, codeValueMap, { triggerTiming: 'FILL' });
    if (results.length === 0) {
      // 规则通过：只清空当前指标的 logicRule 错误（使用指标 id 作为 key）
      if (indicator.id !== undefined) delete logicRuleErrors[String(indicator.id)];
      continue;
    }
    const errMsg = buildLogicRuleMsg(indicator.logicRule, allIndicators, codeValueMap);
    if (indicator.id !== undefined) logicRuleErrors[String(indicator.id)] = errMsg;
  }
}

function getContainerFieldError(entry: any, indicatorCode: string, fieldCode: string): string | undefined {
  const key = generateContainerFieldKey(indicatorCode, entry.rowKey, fieldCode);
  // 优先返回即时错误（失焦时显示）
  if (containerFieldInstantErrors[key]) return containerFieldInstantErrors[key];
  if (logicRuleErrors[key]) return logicRuleErrors[key];
  return containerFieldErrors[key];
}

function getTopLevelError(indicator: DeclareIndicatorApi.Indicator): string | undefined {
  if (indicator.id === undefined) return undefined;
  const idKey = String(indicator.id);
  if (logicRuleErrors[idKey]) return logicRuleErrors[idKey];
  if (indicatorErrors.value[idKey]) return indicatorErrors.value[idKey];
  return undefined;
}

const indicatorErrors = computed(() => {
  const errors: Record<string, string> = {};
  for (const ind of indicators.value) {
    const code = ind.indicatorCode;
    const value = formValues[code];
    const isEmptyVal = value === undefined || value === null || value === '';
    const isComputed = isComputedIndicator(ind);
    const idKey = ind.id !== undefined ? String(ind.id) : code;
    if (ind.isRequired && isEmptyVal && !isComputed) { errors[idKey] = '此项为必填'; continue; }
    if (!isEmptyVal) {
      if (ind.valueType === 2) {
        const trimmed = String(value).trim();
        if (/^-?\d+(\.\d+)?$/.test(trimmed)) { errors[idKey] = '不能输入纯数字'; continue; }
      }
      if (ind.valueType === 1) {
        const numVal = Number(value);
        // 检查是否为有效数字（非 NaN 且非 Infinity）
        if (isNaN(numVal) || !isFinite(numVal)) { errors[idKey] = '请输入有效数字'; continue; }
        // 范围校验（含负数检查）
        const rangeErr = checkRange(numVal, ind.minValue ?? null, ind.maxValue ?? null);
        if (rangeErr) { errors[idKey] = rangeErr; continue; }
        // 小数位精度校验
        const cfg = parseExtraConfig(ind.extraConfig);
        const precErr = checkPrecision(numVal, cfg.precision);
        if (precErr) { errors[idKey] = precErr; continue; }
      }
    }
  }
  return errors;
});

const containerFieldErrors = computed(() => {
  const errors: Record<string, string> = {};
  for (const indicator of indicators.value) {
    if (indicator.valueType !== 12) continue;
    const code = indicator.indicatorCode;
    const entries = containerValues[code] || [];
    const fields = parseDynamicFields(indicator.valueOptions);

    // 先收集所有需要 noRepeat 检查的字段值
    const noRepeatFieldValues: Record<string, Map<string, string>> = {};
    for (const field of fields) {
      if (field.noRepeat) {
        noRepeatFieldValues[field.fieldCode] = new Map();
      }
    }

    for (let i = 0; i < entries.length; i++) {
      const entry = entries[i];
      for (const field of fields) {
        if (!isFieldVisible(entry, code, field, fields)) continue;
        const fullKey = generateContainerFieldKey(code, entry.rowKey, field.fieldCode);
        const fieldValue = entry[fullKey];
        const isEmptyVal = fieldValue === undefined || fieldValue === null || fieldValue === '';
        if (field.required && isEmptyVal) { errors[fullKey] = '此项为必填'; continue; }
        if (!isEmptyVal) {
          if (field.fieldType === 'number') {
            const numVal = Number(fieldValue);
            // 检查是否为有效数字（非 NaN 且非 Infinity）
            if (isNaN(numVal) || !isFinite(numVal)) { errors[fullKey] = '请输入有效数字'; continue; }
            const precErr = checkPrecision(numVal, field.precision);
            if (precErr) { errors[fullKey] = precErr; continue; }
            const rangeErr = checkRange(numVal, field.minValue ?? 0, field.maxValue);
            if (rangeErr) { errors[fullKey] = rangeErr; continue; }
          } else if (field.fieldType === 'text' || field.fieldType === 'textarea') {
            const trimmed = String(fieldValue).trim();
            if (field.fieldType === 'text' && /^-?\d+(\.\d+)?$/.test(trimmed)) {
              errors[fullKey] = '不能输入纯数字';
              continue;
            }
            // noRepeat 排重验证（text 和 textarea 都支持）
            if (field.noRepeat && trimmed) {
              const existing = noRepeatFieldValues[field.fieldCode]?.get(trimmed);
              if (existing && existing !== fullKey) {
                const existingEntryIndex = entries.findIndex(e => generateContainerFieldKey(code, e.rowKey, field.fieldCode) === existing);
                if (existingEntryIndex !== -1 && existingEntryIndex !== i) {
                  errors[fullKey] = `不能与条目${existingEntryIndex + 1}重复`;
                }
              } else {
                noRepeatFieldValues[field.fieldCode]?.set(trimmed, fullKey);
              }
            }
          }
      }
    }
  }
  return errors;
}});

// ==================== 计算指标 ====================

function getIndicatorValue(indicatorCode: string): number | undefined {
  const value = formValues[indicatorCode];
  if (value === undefined || value === null || value === '') return undefined;
  return Number(value);
}

function calculateIndicatorValue(indicator: DeclareIndicatorApi.Indicator): number | undefined {
  if (!indicator.calculationRule) return undefined;
  try {
    let formula = indicator.calculationRule;
    const indicatorMatches = formula.match(/\[[^\]]+\]/g) || [];
    let hasAllValues = true;
    let processedFormula = formula;
    for (const match of indicatorMatches) {
      const code = match.replace(/[\[\]]/g, '');
      const value = getIndicatorValue(code);
      if (value === undefined) { hasAllValues = false; break; }
      processedFormula = processedFormula.replace(match, String(value));
    }
    if (!hasAllValues) return undefined;
    // 将 % 作为百分比后缀处理：X% → X/100
    processedFormula = processedFormula.replace(/(\d+\.?\d*)%/g, '$1/100');
    const safeFormula = processedFormula.replace(/[^0-9+\-*/.()]/g, '');
    if (!safeFormula || safeFormula.trim() === '') return undefined;
    const result = new Function(`return ${safeFormula}`)();
    if (isNaN(result) || !isFinite(result)) return undefined;
    return result;
  } catch {
    return undefined;
  }
}

function recalculateComputedIndicators() {
  const computedOnes = indicators.value.filter((ind) => isComputedIndicator(ind));
  if (computedOnes.length === 0) return;
  const MAX_PASSES = computedOnes.length + 1;
  let changed = true;
  let pass = 0;
  while (changed && pass < MAX_PASSES) {
    changed = false;
    pass++;
    for (const ind of computedOnes) {
      const calculatedValue = calculateIndicatorValue(ind);
      if (calculatedValue !== undefined) {
        const precision = getNumberPrecision(ind);
        const effectivePrecision = precision !== undefined ? precision : 2;
        // 使用字符串格式化避免浮点数精度问题
        let finalValue: number;
        const fixedStr = calculatedValue.toFixed(effectivePrecision);
        finalValue = parseFloat(fixedStr);
        formValues[ind.indicatorCode] = finalValue;
        changed = true;
      }
    }
  }
  nextTick(() => {
    for (const ind of computedOnes) {
      if (formValues[ind.indicatorCode] !== undefined) {
        const el = document.querySelector(`[data-indicator-code="${ind.indicatorCode}"] .safe-number-input-inner`) as HTMLInputElement | null;
        if (el) {
          const precision = getNumberPrecision(ind);
          const effectivePrecision = precision !== undefined ? precision : 2;
          el.value = formValues[ind.indicatorCode]!.toFixed(effectivePrecision);
        }
      }
      markTopLevelDirty(ind.id);
    }
  });
}

// ==================== 值提取 ====================

function extractValue(record: any, valueType: number): any {
  if (!record) return undefined;
  switch (valueType) {
    case 1: return record.valueNum !== null && record.valueNum !== undefined ? Number(record.valueNum) : undefined;
    case 2: case 6: return record.valueStr || undefined;
    case 3:
      if (record.valueBool !== undefined && record.valueBool !== null) return record.valueBool;
      if (record.valueStr === 'true') return true;
      if (record.valueStr === 'false') return false;
      return undefined;
    case 4: return record.valueDate ? dayjs(record.valueDate) : undefined;
    case 5: return record.valueText || undefined;
    case 7: return record.valueStr ? record.valueStr.split(',') : undefined;
    case 8: {
      const start = record.valueDateStart ? dayjs(record.valueDateStart) : null;
      const end = record.valueDateEnd ? dayjs(record.valueDateEnd) : null;
      return start || end ? [start, end] : undefined;
    }
    case 9: return record.valueStr || undefined;
    case 10: return record.valueStr || undefined;
    case 11: return record.valueStr ? record.valueStr.split(',') : undefined;
    case 12: return record.valueStr ? JSON.parse(record.valueStr) : undefined;
    default: return record.valueStr || undefined;
  }
}

function extractContainerValue(record: any): Record<string, any> {
  if (!record || !record.valueStr) return {};
  try {
    const parsed = JSON.parse(record.valueStr);
    return typeof parsed === 'object' && parsed !== null ? parsed : {};
  } catch {
    return {};
  }
}

function parseStoredFileList(value: string | undefined): UploadFile[] {
  if (!value) return [];
  try {
    const parsed = JSON.parse(value);
    if (Array.isArray(parsed)) {
      return parsed.map((item: any, index: number) => ({
        uid: String(index),
        name: item.name || item.url?.split('/').pop() || '文件',
        url: item.url,
        status: 'done' as const,
      }));
    }
  } catch {
    if (value.includes(',')) {
      return value.split(',').map((url, index) => ({
        uid: String(index),
        name: url.split('/').pop() || url,
        url: url.trim(),
        status: 'done' as const,
      }));
    }
    if (value.startsWith('http') || value.startsWith('/')) {
      return [{ uid: '0', name: value.split('/').pop() || value, url: value, status: 'done' as const }];
    }
  }
  return [];
}

function migrateContainerEntryToFullKey(item: Record<string, any>, valueOptions: string, indicatorCode: string, rowKey: string): Record<string, any> {
  const fields = parseDynamicFields(valueOptions);
  const result: Record<string, any> = { rowKey: item.rowKey };
  for (const field of fields) {
    const fullKey = generateContainerFieldKey(indicatorCode, rowKey, field.fieldCode);
    if (item[fullKey] !== undefined) result[fullKey] = item[fullKey];
    else if (item[field.fieldCode] !== undefined) result[fullKey] = item[field.fieldCode];
  }
  return result;
}

function convertContainerEntryDates(valueOptions: string, item: Record<string, any>, indicatorCode?: string, rowKey?: string): Record<string, any> {
  const fields = parseDynamicFields(valueOptions);
  const result: Record<string, any> = { ...item };
  if (indicatorCode && rowKey) {
    for (const field of fields) {
      const fullKey = generateContainerFieldKey(indicatorCode, rowKey, field.fieldCode);
      if (field.fieldType === 'date' && item[fullKey]) {
        const d = dayjs(item[fullKey]);
        if (d.isValid()) result[fullKey] = d;
      } else if (field.fieldType === 'dateRange') {
        const [start, end] = Array.isArray(item[fullKey]) ? item[fullKey] : [null, null];
        result[fullKey] = [start ? dayjs(start) : null, end ? dayjs(end) : null];
      }
    }
  }
  return result;
}

function migrateRowKeyToNewFormat(indicatorCode: string, entryIndex: number): string {
  return generateContainerRowKey(indicatorCode, entryIndex + 1);
}

// ==================== 事件处理 ====================

function handleNumberBlur(indicator: DeclareIndicatorApi.Indicator, _event: Event) {
  markTopLevelDirty(indicator.id);
  checkAndSyncLinkedAutoContainers(indicator.indicatorCode);
  nextTick(() => {
    recalculateComputedIndicators();
    validateLogicRuleForBlur(indicator);
  });
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
  markTopLevelDirty(indicator.id);
  if (indicator.valueType === 12) {
    const entries = containerValues[indicator.indicatorCode] || [];
    formValues[indicator.indicatorCode] = JSON.stringify(entries);
  }
  checkAndSyncLinkedAutoContainers(indicator.indicatorCode);
  nextTick(() => {
    validateLogicRuleForBlur(indicator);
  });
}

function onFieldBlur(indicator: DeclareIndicatorApi.Indicator, entry: any, field: DynamicField, event: FocusEvent) {
  if (['radio', 'checkbox', 'select', 'cascader', 'treeSelect'].includes(field.fieldType)) return;
  const key = generateContainerFieldKey(indicator.indicatorCode, entry.rowKey, field.fieldCode);
  const target = event.target as HTMLInputElement;
  const rawValue = target?.value ?? '';
  let normalizedValue: any;
  if (field.fieldType === 'number') {
    const trimmed = rawValue.trim();
    normalizedValue = trimmed === '' ? null : Number(trimmed);
  } else {
    normalizedValue = rawValue;
  }
  if (normalizedValue !== entry[key]) entry[key] = normalizedValue;
  containerFieldDirty[key] = true;
  // 触发容器字段即时验证
  validateContainerFieldInstant(entry, indicator, field);
  nextTick(() => {
    recalculateComputedIndicators();
    validateLogicRuleForBlur(indicator);
  });
}

function onContainerFieldChange(indicator: DeclareIndicatorApi.Indicator, entry: any, field: DynamicField) {
  const key = generateContainerFieldKey(indicator.indicatorCode, entry.rowKey, field.fieldCode);
  containerFieldDirty[key] = true;
  // 触发容器字段即时验证
  validateContainerFieldInstant(entry, indicator, field);
  nextTick(() => {
    recalculateComputedIndicators();
    validateLogicRuleForBlur(indicator);
  });
}

async function handleFileUpload(file: File, indicator: DeclareIndicatorApi.Indicator) {
  const indicatorCode = indicator.indicatorCode;
  const maxCount = getMaxFileCount(indicator);
  const acceptTypes = getAcceptTypes(indicator);
  if (acceptTypes) {
    const fileExt = file.name.split('.').pop()?.toLowerCase() || '';
    const allowedExts = acceptTypes.split(',').map((ext: string) => ext.trim().toLowerCase());
    if (!allowedExts.includes(fileExt)) {
      message.warning(`仅支持上传以下格式：${acceptTypes}`);
      return false;
    }
  }
  try {
    const result = await uploadFile({ file: file as any, directory: 'declare/indicator' });
    const fileUrl = result.url || result;
    const fileName = file.name;
    const currentList = fileListMap[indicatorCode] || [];
    if (currentList.some((f) => f.name === fileName)) { message.warning('文件已存在'); return false; }
    if (currentList.length >= maxCount) { message.warning(`最多上传${maxCount}个文件`); return false; }
    const newFile: UploadFile = { uid: Date.now().toString(), name: fileName, url: fileUrl, status: 'done' };
    fileListMap[indicatorCode] = [...currentList, newFile];
    formValues[indicatorCode] = JSON.stringify(fileListMap[indicatorCode]);
    message.success('文件上传成功');
  } catch (error) {
    console.error('文件上传错误:', error);
    message.error('文件上传失败');
  }
  return false;
}

function handleFileRemove(indicatorCode: string, file: UploadFile) {
  const currentList = fileListMap[indicatorCode] || [];
  fileListMap[indicatorCode] = currentList.filter((f) => f.uid !== file.uid);
  formValues[indicatorCode] = JSON.stringify(fileListMap[indicatorCode]);
  message.success('文件已删除');
}

// ==================== 数据加载 ====================

async function loadIndicatorData(projectType: number, reportId?: number) {
  // 加载分组
  const groupTree = await getIndicatorGroupTreeByProjectType(projectType);
  groupInfoMap.value = {};
  groupTree.forEach((item) => {
    if (item.id) {
      groupInfoMap.value[item.id] = { groupName: item.groupName, groupPrefix: item.groupPrefix || '', parentId: item.parentId ?? 0, groupLevel: 1 };
      (item.children || []).forEach((child: any) => {
        if (child.id) groupInfoMap.value[child.id] = { groupName: child.groupName, groupPrefix: child.groupPrefix || '', parentId: child.parentId ?? item.id, groupLevel: 2 };
      });
    }
  });

  // 加载指标
  const indicatorData = await getIndicatorsByBusinessType('process', projectType);
  indicators.value = indicatorData;

  // 加载已有值
  if (reportId) {
    const savedValues = await getProgressReportIndicatorValues(reportId);
    for (const record of savedValues) {
      const ind = indicatorData.find((i) => i.id === record.indicatorId);
      const vt = record.valueType ?? ind?.valueType ?? 1;
      const value = extractValue(record, vt);
      formValues[record.indicatorCode!] = value;
      if (vt === 9 && value && record.indicatorCode) {
        fileListMap[record.indicatorCode] = parseStoredFileList(value);
      }
      if (vt === 12 && record.indicatorCode) {
        const containerType = ind ? getContainerType(ind.valueOptions) : 'normal';
        const raw = extractContainerValue(record);
        const indicatorCode = record.indicatorCode;
        if (Array.isArray(raw)) {
          containerValues[indicatorCode] = raw.map((item: any, idx: number) => {
            const rowKey = item.rowKey || migrateRowKeyToNewFormat(indicatorCode, idx);
            const entryWithFullKey = migrateContainerEntryToFullKey(item, ind!.valueOptions, indicatorCode, rowKey);
            return { rowKey, ...item, ...convertContainerEntryDates(ind!.valueOptions, entryWithFullKey, indicatorCode, rowKey) };
          });
        } else if (raw && typeof raw === 'object') {
          if (containerType === 'conditional') {
            const rowKey = generateConditionalRowKey(indicatorCode);
            const entryWithFullKey = migrateContainerEntryToFullKey(raw, ind!.valueOptions, indicatorCode, rowKey);
            containerValues[indicatorCode] = [{ rowKey, ...raw, ...convertContainerEntryDates(ind!.valueOptions, entryWithFullKey, indicatorCode, rowKey) }];
          } else {
            const rowKey = generateContainerRowKey(indicatorCode, 1);
            const entryWithFullKey = migrateContainerEntryToFullKey(raw, ind!.valueOptions, indicatorCode, rowKey);
            containerValues[indicatorCode] = [{ rowKey, ...raw, ...convertContainerEntryDates(ind!.valueOptions, entryWithFullKey, indicatorCode, rowKey) }];
          }
        } else {
          containerValues[indicatorCode] = [{ rowKey: containerType === 'conditional' ? generateConditionalRowKey(indicatorCode) : generateContainerRowKey(indicatorCode, 1) }];
        }
      }
      // 解析输入型选项的值
      if ((vt === 6 || vt === 7) && record.valueStr) {
        const rawValues = vt === 7 ? record.valueStr.split(',') : [record.valueStr];
        for (const v of rawValues) {
          const deserialized = deserializeInputTypeValue(v);
          if (deserialized.input && record.indicatorCode) {
            inputTypeValues[record.indicatorCode + '_' + deserialized.value] = deserialized.input;
            // 同步设置 formValues 子字段（用于 v:if 判断输入框是否显示）
            formValues[record.indicatorCode + '_input_' + deserialized.value] = deserialized.input;
          }
        }
      }
    }
  }

  // 初始化容器
  initializeAutoEntryContainers();
  for (const ind of indicatorData) {
    if (ind.valueType === 12) {
      const link = getAutoEntryLink(ind.valueOptions);
      if (link) continue;
      if (!containerValues[ind.indicatorCode]) {
        containerValues[ind.indicatorCode] = [{ rowKey: getContainerType(ind.valueOptions) === 'conditional' ? generateConditionalRowKey(ind.indicatorCode) : generateContainerRowKey(ind.indicatorCode, 1) }];
      }
    }
  }

  // 加载上期值
  if (props.hospitalId && props.reportYear !== undefined && props.reportBatch !== undefined) {
    const lastValues = await getLastPeriodValues(props.hospitalId, props.reportYear, props.reportBatch);
    lastPeriodValues.value = lastValues || {};
  }

  // 重新计算
  recalculateComputedIndicators();
  validateLogicRules(indicators.value);
}

async function reloadIndicatorData(projectType: number, reportId?: number) {
  groupInfoMap.value = {};
  Object.keys(formValues).forEach((key) => delete formValues[key]);
  await loadIndicatorData(projectType, reportId);
}

// ==================== 生命周期 ====================

onMounted(async () => {
  mounted.value = true;
  if (props.projectType === undefined) return;
  emit('loadingChange', true);
  try {
    await loadIndicatorData(props.projectType, props.reportId);
    if (jointRules.value.length === 0) {
      try {
        const rules = await getEnabledJointRules({ projectType: props.projectType, triggerTiming: 'FILL' });
        jointRules.value = rules || [];
      } catch (error) {
        console.error('加载联合规则失败:', error);
      }
    }
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
    await reloadIndicatorData(newProjectType, props.reportId);
    if (jointRules.value.length === 0) {
      try {
        const rules = await getEnabledJointRules({ projectType: newProjectType, triggerTiming: 'FILL' });
        jointRules.value = rules || [];
      } catch (error) {
        console.error('加载联合规则失败:', error);
      }
    }
  } catch (error) {
    console.error('加载指标数据失败:', error);
    message.error('加载指标数据失败');
  } finally {
    emit('loadingChange', false);
  }
}, { immediate: false });

watch(() => formValues, () => {
  const hasAnyValue = Object.values(formValues).some((v) => v !== undefined && v !== null && v !== '');
  if (hasAnyValue) markDirty();
}, { deep: true });

// ==================== 暴露 API ====================

function getContainerValues(): Record<string, string> {
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
    if (rawValue === undefined || rawValue === null || rawValue === '') continue;
    const item: any = { indicatorId: ind.id!, indicatorCode: code, valueType: vt };
    if (vt === 12) item.valueStr = JSON.stringify(containerValues[code] || []);
    else if (vt === 1) item.valueNum = String(rawValue);
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

defineExpose({
  getContainerValues,
  getAllIndicatorValues,
  getAllIndicators: () => indicators.value,
  validateAll,
  validateFilledData,
  recalculateComputedIndicators,
  syncContainerValuesToForm,
  syncAllAutoEntryContainers,
  containerValues,
  isDirty,
  resetDirty,
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
                :value="getInputTypeOptionValue(getInputTypeRawValue(indicator))"
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
                      v-if="opt.inputType && isInputTypeOptionSelected(indicator, opt.value)"
                      size="small"
                      class="w-36 ml-2"
                      placeholder="请输入"
                      :value="inputTypeValues[indicator.indicatorCode + '_' + opt.value]"
                      :status="isInputTypeOptionSelected(indicator, opt.value) ? getInputTypeError(indicator, opt) : null"
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
                :value="getInputTypeOptionValues(getInputTypeRawValues(indicator))"
                :disabled="readonly"
                class="flex flex-wrap gap-x-4 gap-y-2"
              >
                <template v-for="opt in parseOptions(indicator.valueOptions)" :key="opt.value">
                  <div class="flex items-center">
                    <a-checkbox :value="opt.value" :disabled="readonly" @click="(e: MouseEvent) => handleCheckboxInputClick(indicator, opt.value, e)">
                      {{ opt.label }}
                    </a-checkbox>
                    <a-input
                      v-if="opt.inputType && isInputTypeOptionSelected(indicator, opt.value)"
                      size="small"
                      class="w-36 ml-2"
                      placeholder="请输入"
                      :value="inputTypeValues[indicator.indicatorCode + '_' + opt.value]"
                      :status="isInputTypeOptionSelected(indicator, opt.value) ? getInputTypeError(indicator, opt) : null"
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
                <div v-if="getFileList(indicator.indicatorCode).length > 0" class="file-list">
                  <div v-for="(file, index) in getFileList(indicator.indicatorCode)" :key="index" class="file-item">
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
                  :disabled="getFileList(indicator.indicatorCode).length >= getMaxFileCount(indicator)"
                  :accept="getAcceptTypes(indicator) ? '.' + getAcceptTypes(indicator).replace(/,/g, ',.') : undefined"
                  multiple
                >
                  <a-button type="dashed" :disabled="getFileList(indicator.indicatorCode).length >= getMaxFileCount(indicator)">
                    <IconifyIcon icon="lucide:plus" />
                    上传文件
                  </a-button>
                </Upload>
                <div v-if="!readonly" class="upload-hint">
                  <span v-if="getAcceptTypes(indicator)">支持 {{ getAcceptTypes(indicator) }}</span>
                  <span v-if="getAcceptTypes(indicator) && getMaxFileCount(indicator)">，</span>
                  <span v-if="getMaxFileCount(indicator)">最多 {{ getMaxFileCount(indicator) }} 个</span>
                  <span class="upload-count">({{ getFileList(indicator.indicatorCode).length }}/{{ getMaxFileCount(indicator) }})</span>
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
                        :container-field-error="getContainerFieldError(entry, indicator.indicatorCode, field.fieldCode)"
                        @blur="() => onContainerFieldChange(indicator, entry, field)"
                        @field-change="() => onContainerFieldChange(indicator, entry, field)"
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
                        :container-field-error="getContainerFieldError(entry, indicator.indicatorCode, field.fieldCode)"
                        @blur="() => onContainerFieldChange(indicator, entry, field)"
                        @field-change="() => onContainerFieldChange(indicator, entry, field)"
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
                        :container-field-error="getContainerFieldError(entry, indicator.indicatorCode, field.fieldCode)"
                        @blur="() => onContainerFieldChange(indicator, entry, field)"
                        @field-change="() => onContainerFieldChange(indicator, entry, field)"
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
                v-if="indicator.valueType !== 12 && indicator.id && getTopLevelError(indicator) && topLevelFieldDirty['in_' + indicator.id]"
                class="indicator-error"
              >
                {{ getTopLevelError(indicator) }}
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
