/**
 * useContainerValues - 容器值管理
 */

import { reactive } from 'vue';
import { message } from 'ant-design-vue';
import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import type { DynamicField } from '../types';
import {
  getContainerType, getAutoEntryLink, parseDynamicFields,
  generateContainerFieldKey, generateContainerRowKey, generateConditionalRowKey,
  getMaxEntryIndex, MAX_CONTAINER_ENTRIES,
} from '../utils/container';
import { formValues } from './useFormValues';
import { indicators } from './useIndicatorData';

export const containerValues = reactive<Record<string, any[]>>({});

export function getContainerEntries(indicatorCode: string): any[] {
  return containerValues[indicatorCode] || [];
}

export function setEntryFieldValue(entry: any, indicatorCode: string, fieldCode: string, value: any) {
  const fullKey = generateContainerFieldKey(indicatorCode, entry.rowKey, fieldCode);
  entry[fullKey] = value;
}

export function getEntryFieldValue(entry: any, indicatorCode: string, fieldCode: string): any {
  const fullKey = generateContainerFieldKey(indicatorCode, entry.rowKey, fieldCode);
  return entry?.[fullKey];
}

function generateNextRowKey(indicatorCode: string): string | undefined {
  const entries = containerValues[indicatorCode] || [];
  const maxIndex = getMaxEntryIndex(entries);
  const nextIndex = maxIndex + 1;
  if (nextIndex > MAX_CONTAINER_ENTRIES) return undefined;
  return generateContainerRowKey(indicatorCode, nextIndex);
}

function renumberEntries(indicatorCode: string) {
  const entries = containerValues[indicatorCode] || [];
  entries.forEach((entry, idx) => {
    entry.rowKey = generateContainerRowKey(indicatorCode, idx + 1);
  });
}

export function handleAddEntry(indicatorCode: string) {
  const entries = containerValues[indicatorCode] || [];
  const firstEntry = entries[0];
  const valueOptions = firstEntry?.['__valueOptions__'] || '';
  const containerType = getContainerType(valueOptions);
  if (containerType === 'conditional') return;
  if (!containerValues[indicatorCode]) containerValues[indicatorCode] = [];
  const newRowKey = generateNextRowKey(indicatorCode);
  if (!newRowKey) {
    message.warning(`容器 ${indicatorCode} 的条目数量已达到上限（99）`);
    return;
  }
  containerValues[indicatorCode].push({ rowKey: newRowKey });
}

export function handleRemoveEntry(indicatorCode: string, rowKey: string) {
  const entries = containerValues[indicatorCode] || [];
  const index = entries.findIndex((e: any) => e.rowKey === rowKey);
  if (index !== -1) {
    containerValues[indicatorCode]!.splice(index, 1);
    renumberEntries(indicatorCode);
  }
}

export function isFieldVisible(entry: any, indicatorCode: string, field: DynamicField, allFields: DynamicField[]): boolean {
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
      if (isBooleanWatch) result = watchVal === (value === 'true' || value === '1' || value === true);
      else result = watchVal === value; break;
    case 'neq':
      if (isBooleanWatch) result = watchVal !== (value === 'true' || value === '1' || value === true);
      else result = watchVal !== value; break;
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

export function getVisibleFields(indicatorCode: string, valueOptions: string, entry: any): DynamicField[] {
  const fields = parseDynamicFields(valueOptions);
  return fields.filter((f) => isFieldVisible(entry, indicatorCode, f, fields));
}

function syncAutoEntryCount(containerCode: string, _linkedCode: string, linkedValue: any) {
  const targetCount = Math.min(99, Math.max(0, Math.floor(Number(linkedValue)) || 0));
  if (targetCount > 99) message.warning(`自动条目容器最多支持 99 个条目，已截断`);
  if (!containerValues[containerCode]) containerValues[containerCode] = [];
  const currentEntries = containerValues[containerCode];
  if (targetCount <= 0) { if (currentEntries.length > 0) containerValues[containerCode] = []; return; }
  if (currentEntries.length < targetCount) {
    for (let i = currentEntries.length + 1; i <= targetCount; i++) {
      currentEntries.push({ rowKey: generateContainerRowKey(containerCode, i) });
    }
  } else if (currentEntries.length > targetCount) {
    currentEntries.splice(targetCount);
  }
  renumberEntries(containerCode);
}

export function initializeAutoEntryContainers(indicators: DeclareIndicatorApi.Indicator[]) {
  for (const indicator of indicators) {
    if (indicator.valueType !== 12) continue;
    const link = getAutoEntryLink(indicator.valueOptions);
    if (link) {
      const linkedValue = formValues[link];
      if (linkedValue !== undefined) syncAutoEntryCount(indicator.indicatorCode, link, linkedValue);
    }
  }
}

export function checkAndSyncLinkedAutoContainers(changedCode: string, indicators: DeclareIndicatorApi.Indicator[]) {
  for (const indicator of indicators) {
    if (indicator.valueType !== 12) continue;
    const link = getAutoEntryLink(indicator.valueOptions);
    if (link === changedCode) {
      const linkedValue = formValues[changedCode];
      if (linkedValue !== undefined) syncAutoEntryCount(indicator.indicatorCode, changedCode, linkedValue);
    }
  }
}

export function syncAllAutoEntryContainers(_indicators?: DeclareIndicatorApi.Indicator[]) {
  const allIndicators = _indicators ?? indicators.value;
  for (const indicator of allIndicators) {
    if (indicator.valueType !== 12) continue;
    const link = getAutoEntryLink(indicator.valueOptions);
    if (link) {
      const linkedValue = formValues[link];
      if (linkedValue !== undefined) syncAutoEntryCount(indicator.indicatorCode, link, linkedValue);
    }
  }
}

export function getLinkedIndicatorName(indicator: any): string {
  const link = getAutoEntryLink(indicator.valueOptions);
  if (!link) return '';
  const linkedIndicator = indicators.value.find((i) => i.indicatorCode === link);
  return linkedIndicator?.indicatorName || link;
}

export function isAutoEntryVisible(indicator: any): boolean {
  const link = getAutoEntryLink(indicator.valueOptions);
  if (!link) return false;
  const linkedValue = formValues[link];
  return Math.max(0, Math.floor(Number(linkedValue))) > 0;
}

export function initializeNormalContainers(indicators: DeclareIndicatorApi.Indicator[]) {
  for (const ind of indicators) {
    if (ind.valueType !== 12) continue;
    const link = getAutoEntryLink(ind.valueOptions);
    if (link) continue;
    if (!containerValues[ind.indicatorCode] || (containerValues[ind.indicatorCode] as any[]).length === 0) {
      containerValues[ind.indicatorCode] = [{
        rowKey: getContainerType(ind.valueOptions) === 'conditional'
          ? generateConditionalRowKey(ind.indicatorCode)
          : generateContainerRowKey(ind.indicatorCode, 1),
      }];
    }
  }
}

export function isEntryBasedContainer(indicator: DeclareIndicatorApi.Indicator): boolean {
  if (indicator.valueType !== 12) return false;
  return ['normal', 'autoEntry'].includes(getContainerType(indicator.valueOptions));
}

export function getContainerEntryCount(indicatorCode: string): number {
  return (containerValues[indicatorCode] || []).length;
}
