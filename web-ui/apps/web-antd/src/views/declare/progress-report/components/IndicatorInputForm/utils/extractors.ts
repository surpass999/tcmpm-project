/**
 * 值提取工具函数
 */
import dayjs from 'dayjs';
import type { UploadFile } from 'ant-design-vue/es/upload/interface';
import { parseContainerConfig, generateContainerFieldKey } from './container';

function extractPureValue(value: string | undefined): string | undefined {
  if (!value) return undefined;
  const idx = value.indexOf('∵');
  return idx >= 0 ? value.substring(0, idx) : value;
}

export function extractValue(record: any, valueType: number): any {
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
    case 7: return record.valueStr ? record.valueStr.split(',').map((v: string) => extractPureValue(v.trim())!).filter(Boolean) : undefined;
    case 8: {
      const start = record.valueDateStart ? dayjs(record.valueDateStart) : null;
      const end = record.valueDateEnd ? dayjs(record.valueDateEnd) : null;
      return start || end ? [start, end] : undefined;
    }
    case 9: case 10: return record.valueStr || undefined;
    case 11: return record.valueStr ? record.valueStr.split(',').map((v: string) => extractPureValue(v.trim())!).filter(Boolean) : undefined;
    case 12: return record.valueStr ? JSON.parse(record.valueStr) : undefined;
    default: return record.valueStr || undefined;
  }
}

export function extractContainerValue(record: any): Record<string, any> {
  if (!record || !record.valueStr) return {};
  try {
    const parsed = JSON.parse(record.valueStr);
    return typeof parsed === 'object' && parsed !== null ? parsed : {};
  } catch { return {}; }
}

export const extractContainerValueFromRecord = extractContainerValue;

export function parseStoredFileList(value: string | undefined): UploadFile[] {
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

export function migrateContainerEntryToFullKey(
  item: Record<string, any>,
  valueOptions: string,
  indicatorCode: string,
  rowKey: string,
): Record<string, any> {
  const fields = parseContainerConfig(valueOptions).fields;
  const result: Record<string, any> = { rowKey: item.rowKey };
  for (const field of fields) {
    const fullKey = generateContainerFieldKey(indicatorCode, rowKey, field.fieldCode);
    if (item[fullKey] !== undefined) result[fullKey] = item[fullKey];
    else if (item[field.fieldCode] !== undefined) result[fullKey] = item[field.fieldCode];
  }
  return result;
}

export function convertContainerEntryDates(
  valueOptions: string,
  item: Record<string, any>,
  indicatorCode?: string,
  rowKey?: string,
): Record<string, any> {
  const fields = parseContainerConfig(valueOptions).fields;
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

export function migrateRowKeyToNewFormat(indicatorCode: string, entryIndex: number): string {
  return `${indicatorCode}${String(entryIndex + 1).padStart(2, '0')}`;
}
