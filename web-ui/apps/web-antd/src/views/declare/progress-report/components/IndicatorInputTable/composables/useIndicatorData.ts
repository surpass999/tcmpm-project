/**
 * useIndicatorData - 指标数据加载
 *
 * 负责：
 * - 指标列表状态（indicators）
 * - 分组信息状态（groupInfoMap）
 * - 上期值状态（lastPeriodValues）
 * - 联合规则状态（jointRules）
 * - 数据加载逻辑
 */

import { ref } from 'vue';
import dayjs from 'dayjs';
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
import type { GroupInfo, IndicatorGroup } from '../types';
import { formValues } from './useFormValues';
import { fileListMap } from './useFileUpload';
import { containerValues, initializeAutoEntryContainers, initializeNormalContainers } from './useContainerValues';
import { restoreInputTypeValues } from './useInputTypeOptions';
import { clearAllErrors } from './useValidation';
import { extractValue, parseStoredFileList } from '../utils/extractors';
import { getContainerType, getAutoEntryLink, parseDynamicFields, generateContainerRowKey, generateConditionalRowKey } from '../utils/container';
import { migrateContainerEntryToFullKey, migrateRowKeyToNewFormat, convertContainerEntryDates, extractContainerValueFromRecord } from '../utils/extractors';

// ==================== 状态 ====================

/** 分组信息 Map */
export const groupInfoMap = ref<Record<number, GroupInfo>>({});

/** 指标列表 */
export const indicators = ref<DeclareIndicatorApi.Indicator[]>([]);

/** 上期值 */
export const lastPeriodValues = ref<Record<string, string>>({});

/** 联合规则 */
export const jointRules = ref<DeclareIndicatorJointRuleApi.JointRule[]>([]);

// ==================== 指标分组计算属性 ====================

export const indicatorGroups = ref<IndicatorGroup[]>([]);

/** 重新计算分组 */
function recalcIndicatorGroups() {
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
  indicatorGroups.value = result;
}

// ==================== 数据加载 ====================

async function loadIndicatorData(
  projectType: number,
  reportId?: number,
  hospitalId?: number,
  reportYear?: number,
  reportBatch?: number,
) {
  // 加载分组
  const groupTree = await getIndicatorGroupTreeByProjectType(projectType);
  groupInfoMap.value = {};
  groupTree.forEach((item) => {
    if (item.id) {
      groupInfoMap.value[item.id] = {
        groupName: item.groupName,
        groupPrefix: item.groupPrefix || '',
        parentId: item.parentId ?? 0,
        groupLevel: 1,
      };
      (item.children || []).forEach((child: any) => {
        if (child.id) {
          groupInfoMap.value[child.id] = {
            groupName: child.groupName,
            groupPrefix: child.groupPrefix || '',
            parentId: child.parentId ?? item.id,
            groupLevel: 2,
          };
        }
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

      // 文件类型
      if (vt === 9 && value && record.indicatorCode) {
        fileListMap[record.indicatorCode] = parseStoredFileList(value);
      }

      // 容器类型
      if (vt === 12 && record.indicatorCode) {
        const containerType = ind ? getContainerType(ind.valueOptions) : 'normal';
        const raw = extractContainerValueFromRecord(record);
        const indicatorCode = record.indicatorCode;

        if (Array.isArray(raw)) {
          containerValues[indicatorCode] = raw.map((item: any, idx: number) => {
            const rowKey = item.rowKey || migrateRowKeyToNewFormat(indicatorCode, idx);
            const entryWithFullKey = migrateContainerEntryToFullKey(item, ind!.valueOptions, indicatorCode, rowKey);
            return { rowKey, ...item, ...convertContainerEntryDates(ind!.valueOptions, entryWithFullKey, indicatorCode, rowKey) };
          });
        } else if (raw && typeof raw === 'object' && Object.keys(raw).length > 0) {
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
          containerValues[indicatorCode] = [];
        }
      }

      // 输入型选项
      restoreInputTypeValues(vt, record.valueStr, record.indicatorCode!);
    }
  }

  // 初始化容器
  initializeAutoEntryContainers(indicatorData);
  initializeNormalContainers(indicatorData);

  // 加载上期值
  if (hospitalId && reportYear !== undefined && reportBatch !== undefined) {
    const lastValues = await getLastPeriodValues(hospitalId, reportYear, reportBatch);
    lastPeriodValues.value = lastValues || {};
  }

  // 重新计算分组
  recalcIndicatorGroups();
}

async function reloadIndicatorData(
  projectType: number,
  reportId?: number,
  hospitalId?: number,
  reportYear?: number,
  reportBatch?: number,
) {
  groupInfoMap.value = {};
  Object.keys(formValues).forEach((key) => delete formValues[key]);
  await loadIndicatorData(projectType, reportId, hospitalId, reportYear, reportBatch);
}

async function loadJointRules(projectType: number) {
  if (jointRules.value.length === 0) {
    try {
      const rules = await getEnabledJointRules({ projectType, triggerTiming: 'FILL' });
      jointRules.value = rules || [];
    } catch (error) {
      console.error('加载联合规则失败:', error);
    }
  }
}

// ==================== 导出 ====================

export {
  recalcIndicatorGroups,
  loadIndicatorData,
  reloadIndicatorData,
  loadJointRules,
};
