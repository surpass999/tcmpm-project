/**
 * 动态容器值管理 composable
 */

import { reactive, ref } from 'vue';
import { message } from 'ant-design-vue';
import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import type { ContainerConfig, DynamicField, ContainerType } from '../types';

const MAX_CONTAINER_ENTRIES = 99;

/** 生成容器字段的完整 key */
function generateContainerFieldKey(
  _indicatorCode: string,
  rowKey: string,
  fieldCode: string,
): string {
  return `${rowKey}${fieldCode}`;
}

/** 为普通/自动条目容器生成带序号的 rowKey */
function generateContainerRowKey(indicatorCode: string, index: number): string {
  const paddedIndex = String(index).padStart(2, '0');
  return `${indicatorCode}${paddedIndex}`;
}

/** 为条件容器生成 rowKey */
function generateConditionalRowKey(indicatorCode: string): string {
  return indicatorCode;
}

export interface UseContainerValuesOptions {
  indicators: typeof ref<DeclareIndicatorApi.Indicator[]>;
}

export function useContainerValues() {
  /** 动态容器值 Map */
  const containerValues = reactive<Record<string, Record<string, any>>>({});

  /** 容器字段脏标记 Map */
  const containerFieldDirty = reactive<Record<string, boolean>>({});

  /** 解析容器配置 */
  function parseContainerConfig(valueOptions: string): ContainerConfig {
    if (!valueOptions) return { mode: 'normal', fields: [] };
    try {
      const parsed = JSON.parse(valueOptions);
      if (Array.isArray(parsed)) {
        return { mode: 'normal', fields: parsed };
      }
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
  function getContainerType(valueOptions: string): ContainerType {
    return parseContainerConfig(valueOptions).mode;
  }

  /** 获取自动条目容器的关联指标代码 */
  function getAutoEntryLink(valueOptions: string): string | undefined {
    return parseContainerConfig(valueOptions).link;
  }

  /** 获取容器内所有子字段 */
  function parseDynamicFields(valueOptions: string): DynamicField[] {
    return parseContainerConfig(valueOptions).fields;
  }

  /** 获取容器条目数组 */
  function getContainerEntries(indicatorCode: string): any[] {
    const entriesMap = containerValues[indicatorCode] || {};
    return Object.values(entriesMap);
  }

  /** 获取当前容器中已有的最大序号 */
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

  /** 重新编排容器内所有条目的 rowKey */
  function renumberContainerEntries(indicatorCode: string) {
    const entries = containerValues[indicatorCode] || [];
    entries.forEach((entry, idx) => {
      entry.rowKey = generateContainerRowKey(indicatorCode, idx + 1);
    });
  }

  /** 添加条目 */
  function addEntry(indicatorCode: string, indicator?: DeclareIndicatorApi.Indicator) {
    const containerType = indicator ? getContainerType(indicator.valueOptions) : 'normal';

    if (containerType === 'conditional') return;

    if (!containerValues[indicatorCode]) {
      containerValues[indicatorCode] = [];
    }

    const entries = containerValues[indicatorCode];
    const maxIndex = getMaxEntryIndex(entries);
    const nextIndex = maxIndex + 1;

    if (nextIndex > MAX_CONTAINER_ENTRIES) {
      message.warning(`容器 ${indicatorCode} 的条目数量已达到上限（${MAX_CONTAINER_ENTRIES}）`);
      return;
    }

    const newRowKey = generateContainerRowKey(indicatorCode, nextIndex);
    entries.push({ rowKey: newRowKey });
  }

  /** 删除条目 */
  function removeEntry(indicatorCode: string, rowKey: string, indicator?: DeclareIndicatorApi.Indicator) {
    const entries = containerValues[indicatorCode] || [];
    const index = entries.findIndex((e: any) => e.rowKey === rowKey);

    if (index !== -1) {
      // 清理脏标记
      if (indicator) {
        const fields = parseDynamicFields(indicator.valueOptions);
        const entry = entries[index];
        for (const field of fields) {
          const key = generateContainerFieldKey(indicatorCode, entry.rowKey, field.fieldCode);
          delete containerFieldDirty[key];
        }
      }

      entries.splice(index, 1);
      renumberContainerEntries(indicatorCode);
    }
  }

  /** 获取容器条目中指定字段的值 */
  function getEntryFieldValue(
    entry: any,
    indicatorCode: string,
    fieldCode: string,
    isArray = false,
  ): any {
    const fullKey = generateContainerFieldKey(indicatorCode, entry.rowKey, fieldCode);
    const val = entry?.[fullKey];
    return isArray ? (val || []) : val;
  }

  /** 设置容器条目中指定字段的值 */
  function setEntryFieldValue(
    entry: any,
    indicatorCode: string,
    fieldCode: string,
    value: any,
  ) {
    const fullKey = generateContainerFieldKey(indicatorCode, entry.rowKey, fieldCode);
    entry[fullKey] = value;
  }

  /** 获取容器字段的完整 key */
  function getContainerFieldFullErrorKey(entry: any, indicatorCode: string, fieldCode: string): string {
    return generateContainerFieldKey(indicatorCode, entry.rowKey, fieldCode);
  }

  /** 初始化容器值（用于已有数据加载） */
  function initializeContainerValue(
    indicatorCode: string,
    entries: any[],
    indicator?: DeclareIndicatorApi.Indicator,
  ) {
    if (entries.length > 0) {
      containerValues[indicatorCode] = entries;
    }
  }

  /** 同步自动条目容器的条目数量 */
  function syncAutoEntryContainerCount(containerCode: string, linkedIndicatorCode: string, linkedValue: any) {
    const targetCount = Math.max(0, Math.floor(Number(linkedValue)) || 0);

    if (targetCount <= 0) return;
    if (targetCount > MAX_CONTAINER_ENTRIES) {
      message.warning(`自动条目容器最多支持 ${MAX_CONTAINER_ENTRIES} 个条目`);
    }

    if (!containerValues[containerCode]) {
      containerValues[containerCode] = [];
    }

    const currentEntries = containerValues[containerCode];
    const effectiveTarget = Math.min(targetCount, MAX_CONTAINER_ENTRIES);

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

  /** 初始化所有自动条目容器 */
  function initializeAutoEntryContainers(indicators: DeclareIndicatorApi.Indicator[]) {
    for (const indicator of indicators) {
      if (indicator.valueType !== 12) continue;
      const link = getAutoEntryLink(indicator.valueOptions);
      if (link) {
        syncAutoEntryContainerCount(indicator.indicatorCode, link, undefined);
      }
    }
  }

  return {
    containerValues,
    containerFieldDirty,
    parseContainerConfig,
    getContainerType,
    getAutoEntryLink,
    parseDynamicFields,
    getContainerEntries,
    addEntry,
    removeEntry,
    getEntryFieldValue,
    setEntryFieldValue,
    getContainerFieldFullErrorKey,
    initializeContainerValue,
    syncAutoEntryContainerCount,
    initializeAutoEntryContainers,
    generateContainerFieldKey,
    renumberContainerEntries,
  };
}
