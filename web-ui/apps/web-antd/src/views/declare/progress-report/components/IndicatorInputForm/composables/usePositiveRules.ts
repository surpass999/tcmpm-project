/**
 * usePositiveRules - 上期对比规则校验
 */

import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import type { PositiveRuleConfig, PositiveRuleItem, PositiveRuleError } from '../types';
import { jointRules, lastPeriodRawValues } from './useIndicatorData';
import { formValues } from './useFormValues';
import { containerValues } from './useContainerValues';
import { setFieldError, clearFieldError } from './useErrorKeys';
import { deserializeInputTypeValue } from './useFormValues';

export function hasAnyLastPeriodValue(): boolean {
  const vals = lastPeriodRawValues.value;
  return !!(vals && Object.keys(vals).length > 0);
}

export function hasLastPeriodValue(indicatorCode: string): boolean {
  const val = lastPeriodRawValues.value[indicatorCode];
  return val !== undefined && val !== null && val !== '';
}

export function validateAllPositiveRules(indicators: DeclareIndicatorApi.Indicator[]): PositiveRuleError[] {
  const errors: PositiveRuleError[] = [];
  if (!hasAnyLastPeriodValue()) return errors;

  const fillRules = jointRules.value.filter((r) => r.triggerTiming === 'FILL');
  for (const rule of fillRules) {
    if (!rule.ruleConfig) continue;
    try {
      const config: PositiveRuleConfig = JSON.parse(rule.ruleConfig);
      if (!config.rules?.length) continue;
      for (const item of config.rules) {
        const error = validateSinglePositiveRule(item, indicators);
        if (error) errors.push(error);
        else clearPositiveRuleError(item.indicatorCode, indicators);
      }
    } catch (e) { console.error('[validateAllPositiveRules] 解析规则配置失败:', rule.id, e); }
  }
  return errors;
}

export function validatePositiveRuleForIndicator(changedIndicatorCode: string, indicators: DeclareIndicatorApi.Indicator[]): PositiveRuleError | null {
  if (!hasAnyLastPeriodValue()) return null;

  const fillRules = jointRules.value.filter((r) => r.triggerTiming === 'FILL');
  for (const rule of fillRules) {
    if (!rule.ruleConfig) continue;
    try {
      const config: PositiveRuleConfig = JSON.parse(rule.ruleConfig);
      if (!config.rules?.length) continue;
      for (const item of config.rules) {
        if (item.indicatorCode !== changedIndicatorCode) continue;
        const error = validateSinglePositiveRule(item, indicators);
        if (error) { setPositiveRuleError(error); return error; }
        else clearPositiveRuleError(item.indicatorCode, indicators);
      }
    } catch (e) { console.error('[validatePositiveRuleForIndicator] 解析规则配置失败:', rule.id, e); }
  }
  return null;
}

function validateSinglePositiveRule(item: PositiveRuleItem, indicators: DeclareIndicatorApi.Indicator[]): PositiveRuleError | null {
  const { indicatorCode, valueType } = item;
  if (!hasLastPeriodValue(indicatorCode)) return null;

  const currentValue = getCurrentValue(indicatorCode, valueType);
  const lastValue = getLastPeriodValue(indicatorCode);

  if (currentValue === null || currentValue === undefined || currentValue === '') return null;

  let errorMsg: string | null = null;
  switch (valueType) {
    case 1: errorMsg = validateNumberRule(item, currentValue, lastValue); break;
    case 6: errorMsg = validateRadioRule(item, currentValue, lastValue); break;
    case 7: errorMsg = validateMultiSelectRule(item, currentValue, lastValue); break;
    case 12: errorMsg = validateContainerRule(item, currentValue, lastValue); break;
    default: console.warn('[validateSinglePositiveRule] 不支持的 valueType:', valueType);
  }

  if (errorMsg) {
    const indicator = indicators.find((i) => i.indicatorCode === indicatorCode);
    return { indicatorCode, indicatorId: indicator?.id, ruleName: item.name, message: errorMsg, errorKey: indicator?.id ? `t:${indicator.id}` : indicatorCode };
  }
  return null;
}

function validateNumberRule(item: PositiveRuleItem, currentValue: any, lastValue: string): string | null {
  const current = Number(currentValue);
  const last = Number(lastValue);
  if (isNaN(current) || isNaN(last)) return null;
  const compareMode = item.compareMode || 'positive';
  if (compareMode === 'positive') {
    if (current < last) return `当前值 ${current} 不能小于上期值 ${last}`;
  } else {
    if (current > last) return `当前值 ${current} 不能大于上期值 ${last}`;
  }
  return null;
}

function validateRadioRule(item: PositiveRuleItem, currentValue: any, lastValue: string): string | null {
  const options = item.options || [];
  if (!options.length) return null;
  const levelMap = new Map<string, number>();
  options.forEach((opt, idx) => levelMap.set(String(opt.value), idx + 1));
  const currentPureValue = extractPureValue(currentValue);
  const lastPureValue = extractPureValue(lastValue);
  const currentLevel = levelMap.get(String(currentPureValue));
  const lastLevel = levelMap.get(String(lastPureValue));
  if (currentLevel === undefined || lastLevel === undefined) return null;
  const compareMode = item.compareMode || 'positive';
  if (compareMode === 'positive') {
    if (currentLevel < lastLevel) {
      const currentLabel = options.find((o) => String(o.value) === String(currentPureValue))?.label || currentPureValue;
      const lastLabel = options.find((o) => String(o.value) === String(lastPureValue))?.label || lastPureValue;
      return `选项「${currentLabel}」不能优于上期「${lastLabel}」（等级从 ${lastLevel} 降至 ${currentLevel}）`;
    }
  } else {
    if (currentLevel > lastLevel) {
      const currentLabel = options.find((o) => String(o.value) === String(currentPureValue))?.label || currentPureValue;
      const lastLabel = options.find((o) => String(o.value) === String(lastPureValue))?.label || lastPureValue;
      return `选项「${currentLabel}」不能差于上期「${lastLabel}」（等级从 ${lastLevel} 升至 ${currentLevel}）`;
    }
  }
  return null;
}

function validateMultiSelectRule(item: PositiveRuleItem, currentValue: any, lastValue: string): string | null {
  const options = item.options || [];
  const excludeSet = new Set(item.excludeOptions || []);

  let currentSet: Set<string>;
  if (Array.isArray(currentValue)) currentSet = new Set(currentValue.map((v: any) => extractPureValue(v)));
  else if (typeof currentValue === 'string') currentSet = new Set(currentValue.split(',').map((v: string) => extractPureValue(v.trim())));
  else currentSet = new Set([extractPureValue(String(currentValue))]);

  let lastSet: Set<string>;
  if (typeof lastValue === 'string') lastSet = new Set(lastValue.split(',').map((v: string) => extractPureValue(v.trim())));
  else lastSet = new Set([extractPureValue(String(lastValue))]);

  const filteredCurrent = new Set([...currentSet].filter((v) => !excludeSet.has(String(v))));
  const filteredLast = new Set([...lastSet].filter((v) => !excludeSet.has(String(v))));
  const compareType = item.compareType;

  if (compareType === 'keep_required') {
    const removed = [...filteredLast].filter((v) => !filteredCurrent.has(v));
    if (removed.length > 0) {
      const removedLabels = removed.map((v) => (options.find((o) => String(o.value) === String(v))?.label || v));
      return `上期选中的选项 [${removedLabels.join('、')}] 已取消选择，必须继续保持选中`;
    }
  } else if (compareType === 'new_count') {
    const minNewCount = item.minNewCount || 1;
    const newItems = [...filteredCurrent].filter((v) => !filteredLast.has(v));
    if (newItems.length < minNewCount) return `本期需新增至少 ${minNewCount} 个选项（当前新增：${newItems.length}）`;
  } else if (compareType === 'count') {
    const compareMode = item.compareMode || 'positive';
    if (compareMode === 'positive') {
      if (filteredCurrent.size < filteredLast.size) return `选中数量不能少于上期（上期：${filteredLast.size} 项，本期：${filteredCurrent.size} 项）`;
    } else {
      if (filteredCurrent.size > filteredLast.size) return `选中数量不能多于上期（上期：${filteredLast.size} 项，本期：${filteredCurrent.size} 项）`;
    }
  } else if (compareType === 'min_level' || compareType === 'max_level') {
    const currentLevels = [...filteredCurrent].map((v) => getOptionLevel(options, v)).filter((l) => l !== null) as number[];
    const lastLevels = [...filteredLast].map((v) => getOptionLevel(options, v)).filter((l) => l !== null) as number[];
    if (currentLevels.length === 0 || lastLevels.length === 0) return null;
    const currentExtreme = compareType === 'min_level' ? Math.min(...currentLevels) : Math.max(...currentLevels);
    const lastExtreme = compareType === 'min_level' ? Math.min(...lastLevels) : Math.max(...lastLevels);
    if (currentExtreme < lastExtreme) return `${compareType === 'min_level' ? '最低' : '最高'}等级不能低于上期`;
  }
  return null;
}

function validateContainerRule(_item: PositiveRuleItem, _currentValue: any, _lastValue: string): string | null {
  return null;
}

function getCurrentValue(indicatorCode: string, valueType: number): any {
  if (valueType === 12) {
    const entries = containerValues[indicatorCode];
    if (!entries || !entries.length) return null;
    return JSON.stringify(entries);
  }
  return formValues[indicatorCode];
}

function getLastPeriodValue(indicatorCode: string): string {
  return lastPeriodRawValues.value[indicatorCode] || '';
}

function extractPureValue(value: any): string {
  if (value === null || value === undefined) return '';
  if (typeof value === 'object' && 'value' in value) value = value.value;
  const str = String(value);
  const idx = str.indexOf('∵');
  return idx >= 0 ? str.substring(0, idx) : str;
}

function getOptionLevel(options: PositiveRuleItem['options'], value: string): number | null {
  if (!options) return null;
  const index = options.findIndex((o) => String(o.value) === String(value));
  return index >= 0 ? index + 1 : null;
}

function setPositiveRuleError(error: PositiveRuleError): void {
  setFieldError(error.errorKey, `${error.ruleName}：${error.message}`, 'joint', false);
}

function clearPositiveRuleError(indicatorCode: string, indicators: DeclareIndicatorApi.Indicator[]): void {
  const indicator = indicators.find((i) => i.indicatorCode === indicatorCode);
  if (indicator?.id) clearFieldError(`t:${indicator.id}`);
}
