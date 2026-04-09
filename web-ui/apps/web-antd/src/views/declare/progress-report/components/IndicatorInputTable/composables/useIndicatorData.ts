/**
 * 指标数据加载 composable
 */

import { ref, computed } from 'vue';

import {
  getIndicatorsByBusinessType,
  getLastPeriodValues,
  getProgressReportIndicatorValues,
} from '#/api/declare/indicator';
import { getIndicatorGroupTreeByProjectType } from '#/api/declare/indicator-group';
import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import type { GroupInfo, IndicatorGroup } from '../types';

export interface UseIndicatorDataOptions {
  hospitalId: number;
  projectType?: number;
  reportId?: number;
  reportYear?: number;
  reportBatch?: number;
}

export function useIndicatorData() {
  /** 分组信息映射 */
  const groupInfoMap = ref<Record<number, GroupInfo>>({});

  /** 指标列表 */
  const indicators = ref<DeclareIndicatorApi.Indicator[]>([]);

  /** 上期值 Map */
  const lastPeriodValues = ref<Record<string, string>>({});

  /** 指标分组（树形结构） */
  const indicatorGroups = computed<IndicatorGroup[]>(() => {
    const levelOneMap = new Map<number, IndicatorGroup>();
    const levelTwoMap = new Map<number, IndicatorGroup>();

    // 注册所有一级分组
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

    // 注册所有二级分组
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
        if (parent) {
          parent.children.push(lvl2);
        }
      }
    }

    // 将指标分配到对应分组
    for (const ind of indicators.value) {
      const gid = ind.groupId || 0;
      const info = groupInfoMap.value[gid];
      if (!info) continue;

      if (info.parentId === 0) {
        const lvl1 = levelOneMap.get(gid);
        if (lvl1) lvl1.indicators.push(ind);
      } else {
        const lvl2 = levelTwoMap.get(gid);
        if (lvl2) lvl2.indicators.push(ind);
      }
    }

    // 排序
    const sortInds = (g: IndicatorGroup) => {
      g.indicators.sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0));
      g.children.forEach(sortInds);
    };
    for (const [, lvl1] of levelOneMap) {
      sortInds(lvl1);
      lvl1.children.sort((a, b) => a.groupId - b.groupId);
    }

    // 组装结果
    const result: IndicatorGroup[] = [];
    for (const [, lvl1] of levelOneMap) {
      result.push(lvl1);
      result.push(...lvl1.children);
    }

    return result;
  });

  /** 加载数据 */
  async function loadData(options: UseIndicatorDataOptions) {
    const { hospitalId, projectType, reportId, reportYear, reportBatch } = options;

    // 1. 加载分组信息
    if (projectType) {
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
    }

    // 2. 加载指标列表
    if (projectType) {
      const indicatorData = await getIndicatorsByBusinessType('process', projectType);
      indicators.value = indicatorData;
    }

    // 3. 加载已有填报值
    if (reportId && indicators.value.length > 0) {
      const savedValues = await getProgressReportIndicatorValues(reportId);
      return savedValues;
    }

    // 4. 加载上期参考值
    if (hospitalId && reportYear !== undefined && reportBatch !== undefined) {
      const lastValues = await getLastPeriodValues(hospitalId, reportYear, reportBatch);
      lastPeriodValues.value = lastValues || {};
    }

    return [];
  }

  /** 重新加载数据（切换 projectType 时） */
  async function reloadData(options: UseIndicatorDataOptions) {
    const { hospitalId, projectType, reportId, reportYear, reportBatch } = options;

    groupInfoMap.value = {};

    if (projectType) {
      const groupTree = await getIndicatorGroupTreeByProjectType(projectType);
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
    }

    if (projectType) {
      const indicatorData = await getIndicatorsByBusinessType('process', projectType);
      indicators.value = indicatorData;
    }

    if (hospitalId && reportYear !== undefined && reportBatch !== undefined) {
      const lastValues = await getLastPeriodValues(hospitalId, reportYear, reportBatch);
      lastPeriodValues.value = lastValues || {};
    }
  }

  return {
    groupInfoMap,
    indicators,
    indicatorGroups,
    lastPeriodValues,
    loadData,
    reloadData,
  };
}
