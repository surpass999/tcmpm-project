/**
 * 容器工具函数
 */
import type { DynamicField, ContainerType, ContainerConfig } from '../types';

export const MAX_CONTAINER_ENTRIES = 99;

export function parseContainerConfig(valueOptions: string): ContainerConfig {
  if (!valueOptions) return { mode: 'normal', fields: [] };
  try {
    const parsed = JSON.parse(valueOptions);
    if (Array.isArray(parsed)) return { mode: 'normal', fields: parsed };
    return { mode: (parsed.mode as ContainerType) || 'normal', link: parsed.link, fields: parsed.fields || [] };
  } catch {
    return { mode: 'normal', fields: [] };
  }
}

export function getContainerType(valueOptions: string): ContainerType {
  return parseContainerConfig(valueOptions).mode;
}

export function getAutoEntryLink(valueOptions: string): string | undefined {
  return parseContainerConfig(valueOptions).link;
}

export function parseDynamicFields(valueOptions: string): DynamicField[] {
  return parseContainerConfig(valueOptions).fields;
}

export function generateContainerFieldKey(_indicatorCode: string, rowKey: string, fieldCode: string): string {
  return `${rowKey}${fieldCode}`;
}

export function generateContainerRowKey(indicatorCode: string, index: number): string {
  return `${indicatorCode}${String(index).padStart(2, '0')}`;
}

export function generateConditionalRowKey(indicatorCode: string): string {
  return indicatorCode;
}

export function extractEntryIndex(rowKey: string): number {
  const match = rowKey.match(/(\d+)$/);
  return match ? parseInt(match[1]!, 10) : 1;
}

export function getMaxEntryIndex(entries: any[]): number {
  let maxIndex = 0;
  for (const entry of entries) {
    const match = entry.rowKey?.match(/(\d+)$/);
    if (match) {
      const idx = parseInt(match[1]!, 10);
      if (idx > maxIndex) maxIndex = idx;
    }
  }
  return maxIndex;
}

export function generateNextContainerRowKey(
  indicatorCode: string,
  containerValuesMap: Record<string, any[]>,
): string | undefined {
  const entries = containerValuesMap[indicatorCode] || [];
  const maxIndex = getMaxEntryIndex(entries);
  const nextIndex = maxIndex + 1;
  if (nextIndex > MAX_CONTAINER_ENTRIES) return undefined;
  return generateContainerRowKey(indicatorCode, nextIndex);
}

export function renumberContainerEntries(indicatorCode: string, containerValuesMap: Record<string, any[]>) {
  const entries = containerValuesMap[indicatorCode] || [];
  entries.forEach((entry, idx) => {
    entry.rowKey = generateContainerRowKey(indicatorCode, idx + 1);
  });
}

export function isFieldVisible(
  entry: any,
  indicatorCode: string,
  field: DynamicField,
  allFields: DynamicField[],
): boolean {
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

export function getVisibleFields(indicatorCode: string, valueOptions: string, entry: any): DynamicField[] {
  const fields = parseDynamicFields(valueOptions);
  return fields.filter((f) => isFieldVisible(entry, indicatorCode, f, fields));
}

export function isEntryBasedContainer(indicator: { valueType: number; valueOptions: string }): boolean {
  if (indicator.valueType !== 12) return false;
  const containerType = getContainerType(indicator.valueOptions);
  return ['normal', 'autoEntry'].includes(containerType);
}

export function getContainerEntryCount(containerValuesMap: Record<string, any[]>, indicatorCode: string): number {
  return (containerValuesMap[indicatorCode] || []).length;
}
