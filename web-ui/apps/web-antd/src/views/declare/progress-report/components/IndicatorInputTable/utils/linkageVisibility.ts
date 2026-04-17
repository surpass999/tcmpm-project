/**
 * linkageVisibility - 联动可见性判断工具
 *
 * 职责：
 * - 解析指标的 linkage 配置（从 extraConfig 中读取）
 * - 评估联动条件是否满足
 * - 批量评估所有指标的联动可见性
 *
 * 与 linkageEvaluator.ts 的区别：
 * - 本模块是轻量级的，不依赖 DeclareIndicatorApi.Indicator 类型
 * - 适用于详情页和对比页等不需要完整指标信息的场景
 */

import type { LinkageEvaluationResult, LinkageOperator } from '../types';

/**
 * 联动配置（简化版）
 */
export interface LinkageConfig {
  enabled: boolean;
  type: string;
  trigger: {
    indicatorCode: string;
    operator: LinkageOperator;
    value: any;
  };
}

/**
 * 指标定义简化版（只需要 indicatorCode 和 extraConfig）
 */
export interface SimpleIndicatorDef {
  indicatorCode: string;
  extraConfig?: string;
}

/**
 * 解析指标的联动配置（从 extraConfig 中读取 linkage）
 */
export function parseLinkageConfigFromExtraConfig(
  extraConfig: string | undefined,
): LinkageConfig | null {
  if (!extraConfig) {
    return null;
  }

  try {
    const config = JSON.parse(extraConfig);
    if (config.linkage?.enabled) {
      return config.linkage as LinkageConfig;
    }
  } catch {
    // ignore parse error
  }

  return null;
}

/**
 * 评估单个条件
 */
function evaluateCondition(
  operator: LinkageOperator,
  triggerValue: any,
  configValue: any,
): boolean {
  switch (operator) {
    case 'eq':
      return triggerValue === configValue;

    case 'neq':
      return triggerValue !== configValue;

    case 'gt':
      return Number(triggerValue) > Number(configValue);

    case 'gte':
      return Number(triggerValue) >= Number(configValue);

    case 'lt':
      return Number(triggerValue) < Number(configValue);

    case 'lte':
      return Number(triggerValue) <= Number(configValue);

    case 'in':
      if (Array.isArray(configValue)) {
        return configValue.includes(triggerValue);
      }
      return false;

    case 'notEmpty':
      if (triggerValue === null || triggerValue === undefined) {
        return false;
      }
      if (typeof triggerValue === 'string') {
        return triggerValue.trim() !== '';
      }
      if (Array.isArray(triggerValue)) {
        return triggerValue.length > 0;
      }
      return true;

    case 'isEmpty':
      if (triggerValue === null || triggerValue === undefined) {
        return true;
      }
      if (typeof triggerValue === 'string') {
        return triggerValue.trim() === '';
      }
      if (Array.isArray(triggerValue)) {
        return triggerValue.length === 0;
      }
      return false;

    default:
      return false;
  }
}

/**
 * 批量评估所有指标的联动可见性
 * @param indicatorDefinitions - 指标定义数组，每个元素需要包含 indicatorCode 和 extraConfig
 * @param triggerValues - 触发指标的值映射 { indicatorCode: value }
 * @returns Map<indicatorCode, LinkageEvaluationResult>
 */
export function evaluateLinkageVisibility(
  indicatorDefinitions: SimpleIndicatorDef[],
  triggerValues: Record<string, any>,
): Map<string, LinkageEvaluationResult> {
  const results = new Map<string, LinkageEvaluationResult>();

  for (const indicator of indicatorDefinitions) {
    const linkage = parseLinkageConfigFromExtraConfig(indicator.extraConfig);
    if (!linkage?.enabled || !linkage.trigger?.indicatorCode) {
      continue;
    }

    const triggerValue = triggerValues[linkage.trigger.indicatorCode];
    const conditionMet = evaluateCondition(
      linkage.trigger.operator,
      triggerValue,
      linkage.trigger.value,
    );

    results.set(indicator.indicatorCode, {
      indicatorCode: indicator.indicatorCode,
      type: linkage.type as LinkageEvaluationResult['type'],
      enabled: conditionMet,
    });
  }

  return results;
}

/**
 * 判断指标是否可见
 */
export function isIndicatorVisibleByLinkage(
  indicatorCode: string,
  linkageResults: Map<string, LinkageEvaluationResult>,
): boolean {
  const state = linkageResults.get(indicatorCode);
  if (state?.type === 'show') {
    return state.enabled;
  }
  // 无联动配置，默认显示
  return true;
}
