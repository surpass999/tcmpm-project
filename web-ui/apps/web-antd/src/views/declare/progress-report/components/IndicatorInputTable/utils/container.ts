/**
 * 容器工具函数
 * 提供动态容器相关的辅助函数
 */
import type { DynamicField, ContainerType, ContainerConfig } from '../types';

export const MAX_CONTAINER_ENTRIES = 99;

/** 解析容器配置 */
export function parseContainerConfig(valueOptions: string): ContainerConfig {
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

/** 获取容器类型 */
export function getContainerType(valueOptions: string): ContainerType {
  return parseContainerConfig(valueOptions).mode;
}

/** 获取自动条目关联的指标编码 */
export function getAutoEntryLink(valueOptions: string): string | undefined {
  return parseContainerConfig(valueOptions).link;
}

/** 解析动态字段列表 */
export function parseDynamicFields(valueOptions: string): DynamicField[] {
  return parseContainerConfig(valueOptions).fields;
}

/** 生成容器字段完整 key */
export function generateContainerFieldKey(_indicatorCode: string, rowKey: string, fieldCode: string): string {
  return `${rowKey}${fieldCode}`;
}

/** 生成容器条目行 key */
export function generateContainerRowKey(indicatorCode: string, index: number): string {
  return `${indicatorCode}${String(index).padStart(2, '0')}`;
}

/** 生成条件容器的行 key */
export function generateConditionalRowKey(indicatorCode: string): string {
  return indicatorCode;
}

/** 从 rowKey 提取序号 */
export function extractEntryIndex(rowKey: string): number {
  const match = rowKey.match(/(\d+)$/);
  return match ? parseInt(match[1]!, 10) : 1;
}

/** 获取最大条目序号 */
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

/** 生成下一个条目的 rowKey */
export function generateNextContainerRowKey(
  indicatorCode: string,
  containerValues: Record<string, any[]>,
  getMaxEntryIndexFn: (entries: any[]) => number
): string | undefined {
  const entries = containerValues[indicatorCode] || [];
  const maxIndex = getMaxEntryIndexFn(entries);
  const nextIndex = maxIndex + 1;
  if (nextIndex > MAX_CONTAINER_ENTRIES) return undefined;
  return generateContainerRowKey(indicatorCode, nextIndex);
}

/** 重新编号容器条目 */
export function renumberContainerEntries(
  indicatorCode: string,
  containerValues: Record<string, any[]>,
  generateContainerRowKeyFn: (indicatorCode: string, index: number) => string
) {
  const entries = containerValues[indicatorCode] || [];
  entries.forEach((entry, idx) => {
    entry.rowKey = generateContainerRowKeyFn(indicatorCode, idx + 1);
  });
}

/** 判断字段是否可见（基于 showCondition） */
export function isFieldVisible(
  entry: any,
  indicatorCode: string,
  field: DynamicField,
  allFields: DynamicField[],
  generateContainerFieldKeyFn: (indicatorCode: string, rowKey: string, fieldCode: string) => string
): boolean {
  if (!field.showCondition) return true;
  const cond = field.showCondition;
  const watchFieldFullKey = generateContainerFieldKeyFn(indicatorCode, entry.rowKey, cond.watchField);
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

/** 获取可见字段列表 */
export function getVisibleFields(
  indicatorCode: string,
  valueOptions: string,
  entry: any,
  parseDynamicFieldsFn: (valueOptions: string) => DynamicField[],
  isFieldVisibleFn: (entry: any, indicatorCode: string, field: DynamicField, allFields: DynamicField[]) => boolean
): DynamicField[] {
  const fields = parseDynamicFieldsFn(valueOptions);
  return fields.filter((f) => isFieldVisibleFn(entry, indicatorCode, f, fields));
}

/** 是否为支持逐条目验证的容器类型（排除 conditional） */
export function isEntryBasedContainer(indicator: { valueType: number; valueOptions: string }): boolean {
  if (indicator.valueType !== 12) return false;
  const containerType = getContainerType(indicator.valueOptions);
  return ['normal', 'autoEntry'].includes(containerType);
}

/** 获取容器条目数量 */
export function getContainerEntryCount(containerValues: Record<string, any[]>, indicatorCode: string): number {
  return (containerValues[indicatorCode] || []).length;
}
