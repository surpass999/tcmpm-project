/**
 * useContainerValues - 容器值管理
 *
 * 负责：
 * - 容器条目值（containerValues）
 * - 容器增删改操作
 * - 自动条目容器同步
 * - 字段可见性判断
 */

import { reactive } from 'vue';
import { message } from 'ant-design-vue';
import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import type { DynamicField } from '../types';
import {
  getContainerType,
  getAutoEntryLink,
  parseDynamicFields,
  generateContainerFieldKey,
  generateContainerRowKey,
  generateConditionalRowKey,
  getMaxEntryIndex,
} from '../utils/container';
import { convertContainerEntryDates, migrateContainerEntryToFullKey, migrateRowKeyToNewFormat } from '../utils/extractors';

// ==================== 容器值状态 ====================

/** 容器值（key: indicatorCode → entries[]） */
export const containerValues = reactive<Record<string, any[]>>({});

// ==================== 条目操作 ====================

function getContainerEntries(indicatorCode: string): any[] {
  const entriesMap = containerValues[indicatorCode] || {};
  return Object.values(entriesMap);
}

function setEntryFieldValue(entry: any, indicatorCode: string, fieldCode: string, value: any) {
  const fullKey = generateContainerFieldKey(indicatorCode, entry.rowKey, fieldCode);
  entry[fullKey] = value;
}

function getEntryFieldValue(entry: any, indicatorCode: string, fieldCode: string, isArray = false): any {
  const fullKey = generateContainerFieldKey(indicatorCode, entry.rowKey, fieldCode);
  const val = entry?.[fullKey];
  return isArray ? (val || []) : val;
}

function generateNextContainerRowKey(indicatorCode: string): string | undefined {
  const entries = containerValues[indicatorCode] || [];
  const maxIndex = getMaxEntryIndex(entries);
  const nextIndex = maxIndex + 1;
  if (nextIndex > 99) return undefined;
  return generateContainerRowKey(indicatorCode, nextIndex);
}

function renumberContainerEntries(indicatorCode: string) {
  const entries = containerValues[indicatorCode] || [];
  entries.forEach((entry, idx) => {
    entry.rowKey = generateContainerRowKey(indicatorCode, idx + 1);
  });
}

function getContainerFieldFullErrorKey(entry: any, indicatorCode: string, fieldCode: string): string {
  return generateContainerFieldKey(indicatorCode, entry.rowKey, fieldCode);
}

/** 新增条目 */
function handleAddEntry(indicatorCode: string) {
  const containerType = getContainerType(
    containerValues[indicatorCode] ? (containerValues[indicatorCode] as any[])?.[0]?.['__valueOptions__'] || '' : ''
  );
  if (containerType === 'conditional') return;
  if (!containerValues[indicatorCode]) containerValues[indicatorCode] = [];
  const newRowKey = generateNextContainerRowKey(indicatorCode);
  if (!newRowKey) {
    message.warning(`容器 ${indicatorCode} 的条目数量已达到上限（99）`);
    return;
  }
  containerValues[indicatorCode].push({ rowKey: newRowKey });
}

/** 移除条目（需要指标列表来清除错误） */
function handleRemoveEntry(indicatorCode: string, rowKey: string) {
  const entries = containerValues[indicatorCode] || [];
  const index = entries.findIndex((e: any) => e.rowKey === rowKey);
  if (index !== -1) {
    containerValues[indicatorCode]!.splice(index, 1);
    renumberContainerEntries(indicatorCode);
  }
}

// ==================== 字段可见性 ====================

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

// ==================== 自动条目容器 ====================

function syncAutoEntryContainerCount(containerCode: string, linkedIndicatorCode: string, linkedValue: any) {
  const targetCount = Math.max(0, Math.floor(Number(linkedValue)) || 0);
  if (targetCount > 99) {
    message.warning(`自动条目容器最多支持 99 个条目，已截断`);
  }
  const effectiveTarget = Math.min(targetCount, 99);
  if (!containerValues[containerCode]) containerValues[containerCode] = [];
  const currentEntries = containerValues[containerCode];
  if (effectiveTarget <= 0) {
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

function initializeAutoEntryContainers(indicators: DeclareIndicatorApi.Indicator[]) {
  for (const indicator of indicators) {
    if (indicator.valueType !== 12) continue;
    const link = getAutoEntryLink(indicator.valueOptions);
    if (link) {
      const linkedValue = (indicator as any).__linkedValue__;
      if (linkedValue !== undefined) {
        syncAutoEntryContainerCount(indicator.indicatorCode, link, linkedValue);
      }
    }
  }
}

function checkAndSyncLinkedAutoContainers(changedCode: string, indicators: DeclareIndicatorApi.Indicator[]) {
  for (const indicator of indicators) {
    if (indicator.valueType !== 12) continue;
    const link = getAutoEntryLink(indicator.valueOptions);
    if (link === changedCode) {
      const linkedIndicator = indicators.find((i) => i.indicatorCode === changedCode);
      if (linkedIndicator) {
        syncAutoEntryContainerCount(indicator.indicatorCode, changedCode, (linkedIndicator as any).__formValue__);
      }
    }
  }
}

/** 同步所有自动条目容器（在保存前调用） */
function syncAllAutoEntryContainers(indicators: DeclareIndicatorApi.Indicator[]) {
  for (const indicator of indicators) {
    if (indicator.valueType !== 12) continue;
    const link = getAutoEntryLink(indicator.valueOptions);
    if (link) {
      const linkedIndicator = indicators.find((i) => i.indicatorCode === link);
      if (linkedIndicator) {
        syncAutoEntryContainerCount(indicator.indicatorCode, link, (linkedIndicator as any).__formValue__);
      }
    }
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

// ==================== 容器初始化 ====================

/** 初始化普通容器（添加第一条空条目） */
function initializeNormalContainers(indicators: DeclareIndicatorApi.Indicator[]) {
  for (const ind of indicators) {
    if (ind.valueType !== 12) continue;
    const link = getAutoEntryLink(ind.valueOptions);
    if (link) continue;
    if (!containerValues[ind.indicatorCode] || containerValues[ind.indicatorCode].length === 0) {
      containerValues[ind.indicatorCode] = [
        {
          rowKey: getContainerType(ind.valueOptions) === 'conditional'
            ? generateConditionalRowKey(ind.indicatorCode)
            : generateContainerRowKey(ind.indicatorCode, 1)
        }
      ];
    }
  }
}

// ==================== 数据加载辅助 ====================

/** 从 API 记录加载容器值 */
function loadContainerValueFromRecord(
  record: any,
  ind: DeclareIndicatorApi.Indicator | undefined,
  vt: number,
  containerValuesMap: Record<string, any[]>,
) {
  if (vt !== 12 || !record.indicatorCode) return;
  const containerType = ind ? getContainerType(ind.valueOptions) : 'normal';
  const raw = extractContainerValueFromRecord(record);
  const indicatorCode = record.indicatorCode;

  if (Array.isArray(raw)) {
    containerValuesMap[indicatorCode] = raw.map((item: any, idx: number) => {
      const rowKey = item.rowKey || migrateRowKeyToNewFormat(indicatorCode, idx);
      const entryWithFullKey = migrateContainerEntryToFullKey(item, ind!.valueOptions, indicatorCode, rowKey);
      return { rowKey, ...item, ...convertContainerEntryDates(ind!.valueOptions, entryWithFullKey, indicatorCode, rowKey) };
    });
  } else if (raw && typeof raw === 'object' && Object.keys(raw).length > 0) {
    if (containerType === 'conditional') {
      const rowKey = generateConditionalRowKey(indicatorCode);
      const entryWithFullKey = migrateContainerEntryToFullKey(raw, ind!.valueOptions, indicatorCode, rowKey);
      containerValuesMap[indicatorCode] = [{ rowKey, ...raw, ...convertContainerEntryDates(ind!.valueOptions, entryWithFullKey, indicatorCode, rowKey) }];
    } else {
      const rowKey = generateContainerRowKey(indicatorCode, 1);
      const entryWithFullKey = migrateContainerEntryToFullKey(raw, ind!.valueOptions, indicatorCode, rowKey);
      containerValuesMap[indicatorCode] = [{ rowKey, ...raw, ...convertContainerEntryDates(ind!.valueOptions, entryWithFullKey, indicatorCode, rowKey) }];
    }
  } else {
    containerValuesMap[indicatorCode] = [];
  }
}

/** 从 record 提取容器值 */
function extractContainerValueFromRecord(record: any): Record<string, any> {
  if (!record || !record.valueStr) return {};
  try {
    const parsed = JSON.parse(record.valueStr);
    return typeof parsed === 'object' && parsed !== null ? parsed : {};
  } catch {
    return {};
  }
}

/** 获取容器条目数量 */
function getContainerEntryCount(indicatorCode: string): number {
  return (containerValues[indicatorCode] || []).length;
}

/** 判断是否为支持逐条目验证的容器类型 */
function isEntryBasedContainer(indicator: DeclareIndicatorApi.Indicator): boolean {
  if (indicator.valueType !== 12) return false;
  const containerType = getContainerType(indicator.valueOptions);
  return ['normal', 'autoEntry'].includes(containerType);
}

// ==================== 导出 ====================

export {
  getContainerEntries,
  setEntryFieldValue,
  getEntryFieldValue,
  generateNextContainerRowKey,
  renumberContainerEntries,
  getContainerFieldFullErrorKey,
  handleAddEntry,
  handleRemoveEntry,
  isFieldVisible,
  getVisibleFields,
  syncAutoEntryContainerCount,
  initializeAutoEntryContainers,
  checkAndSyncLinkedAutoContainers,
  syncAllAutoEntryContainers,
  getLinkedIndicatorName,
  isAutoEntryVisible,
  initializeNormalContainers,
  loadContainerValueFromRecord,
  extractContainerValueFromRecord,
  getContainerEntryCount,
  isEntryBasedContainer,
};
