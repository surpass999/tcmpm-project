/**
 * 联动条件评估引擎
 */
import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import type { IndicatorLinkageConfig, LinkageEvaluationResult, LinkageOperator } from '../types';

export function parseLinkageConfig(indicator: DeclareIndicatorApi.Indicator): IndicatorLinkageConfig | null {
  if (!indicator.extraConfig) return null;
  try {
    const config = JSON.parse(indicator.extraConfig);
    if (config.linkage?.enabled) {
      return config.linkage as IndicatorLinkageConfig;
    }
  } catch { /* ignore */ }
  return null;
}

function evaluateCondition(operator: LinkageOperator, triggerValue: any, configValue: any): boolean {
  switch (operator) {
    case 'eq': return triggerValue === configValue;
    case 'neq': return triggerValue !== configValue;
    case 'gt': return Number(triggerValue) > Number(configValue);
    case 'gte': return Number(triggerValue) >= Number(configValue);
    case 'lt': return Number(triggerValue) < Number(configValue);
    case 'lte': return Number(triggerValue) <= Number(configValue);
    case 'in': return Array.isArray(configValue) ? configValue.includes(triggerValue) : false;
    case 'notEmpty':
      if (triggerValue === null || triggerValue === undefined) return false;
      if (typeof triggerValue === 'string') return triggerValue.trim() !== '';
      if (Array.isArray(triggerValue)) return triggerValue.length > 0;
      return true;
    case 'isEmpty':
      if (triggerValue === null || triggerValue === undefined) return true;
      if (typeof triggerValue === 'string') return triggerValue.trim() === '';
      if (Array.isArray(triggerValue)) return triggerValue.length === 0;
      return false;
    default: return false;
  }
}

export function evaluateLinkage(indicator: DeclareIndicatorApi.Indicator, formValues: Record<string, any>): LinkageEvaluationResult | null {
  const linkage = parseLinkageConfig(indicator);
  if (!linkage || !linkage.enabled || !linkage.trigger?.indicatorCode) return null;

  const triggerValue = formValues[linkage.trigger.indicatorCode];
  const conditionMet = evaluateCondition(linkage.trigger.operator, triggerValue, linkage.trigger.value);

  return { indicatorCode: indicator.indicatorCode, type: linkage.type, enabled: conditionMet };
}

export function evaluateAllLinkages(indicators: DeclareIndicatorApi.Indicator[], formValues: Record<string, any>): Map<string, LinkageEvaluationResult> {
  const results = new Map<string, LinkageEvaluationResult>();
  for (const indicator of indicators) {
    const result = evaluateLinkage(indicator, formValues);
    if (result) results.set(result.indicatorCode, result);
  }
  return results;
}
