/**
 * usePositiveRules - 上期对比规则校验
 *
 * 职责：
 * 1. 解析 jointRules.ruleConfig JSON
 * 2. 按 triggerTiming === 'FILL' 过滤规则
 * 3. 逐指标校验（基础校验通过后）
 * 4. 将错误写入 fieldErrors
 *
 * 重构点：
 * - 提取 makePositiveErrorKey()：统一错误 key 生成
 * - 提取 compareByType()：统一比较框架
 */

import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import type { PositiveRuleConfig, PositiveRuleItem, PositiveRuleError } from '../types';
import { jointRules, lastPeriodValues, lastPeriodRawValues } from './useIndicatorData';
import { formValues } from './useFormValues';
import { containerValues } from './useContainerValues';
import { setFieldError, clearFieldErrorByType } from './useErrorKeys';

// ==================== 辅助函数 ====================

/** 提取纯 value（去掉 inputType 的 ∵ 分隔符） */
function extractPureValue(value: any): string {
  if (value === null || value === undefined) return '';
  if (typeof value === 'object' && 'value' in value) value = (value as any).value;
  const str = String(value);
  const idx = str.indexOf('∵');
  return idx >= 0 ? str.substring(0, idx) : str;
}

/** 获取指标的当前值 */
function getCurrentValue(indicatorCode: string, valueType: number): any {
  if (valueType === 12) {
    const entries = containerValues[indicatorCode];
    if (!entries || !entries.length) return null;
    return JSON.stringify(entries);
  }
  return formValues[indicatorCode];
}

/** 获取指标的上期值（用于数值对比） */
function getLastPeriodValue(indicatorCode: string): string {
  return lastPeriodValues.value[indicatorCode] || '';
}

/** 获取指标的上期原始值（包含 inputType 内容，用于多选对比） */
function getLastPeriodRawValue(indicatorCode: string): string {
  return lastPeriodRawValues.value[indicatorCode] || '';
}

/** 获取选项的等级（位置从1开始） */
function getOptionLevel(options: PositiveRuleItem['options'], value: string): number | null {
  if (!options) return null;
  const index = options.findIndex((o) => String(o.value) === String(value));
  return index >= 0 ? index + 1 : null;
}

// ==================== 统一比较框架 ====================

type ComparisonResult = { valid: boolean; message?: string };

/** 统一比较入口 */
function compareByType(
  valueType: number,
  current: any,
  last: any,
  lastRaw: string,
  item: PositiveRuleItem,
): ComparisonResult {
  switch (valueType) {
    case 1:  return compareNumbers(current, last, item);
    case 6:  return compareRadio(current, last, item);
    case 7:  return compareMultiSelect(current, lastRaw, item);
    case 12: return compareContainer(current, last, item);
    default: return { valid: true };
  }
}

function compareNumbers(currentValue: any, lastValue: string, item: PositiveRuleItem): ComparisonResult {
  const current = Number(currentValue);
  const last = Number(lastValue);
  if (isNaN(current) || isNaN(last)) return { valid: true };

  const mode = item.compareMode || 'positive';
  if (mode === 'positive') {
    if (current < last) return { valid: false, message: `当前值 ${current} 不能小于上期值 ${last}` };
  } else {
    if (current > last) return { valid: false, message: `当前值 ${current} 不能大于上期值 ${last}` };
  }
  return { valid: true };
}

function compareRadio(currentValue: any, lastValue: string, item: PositiveRuleItem): ComparisonResult {
  const options = item.options || [];
  if (!options.length) return { valid: true };

  const levelMap = new Map<string, number>();
  options.forEach((opt, idx) => { levelMap.set(String(opt.value), idx + 1); });

  const pureValue = extractPureValue(currentValue);
  let currentLevel = levelMap.get(String(pureValue));
  if (currentLevel === undefined) {
    const opt = options.find((o) => o.label === pureValue || String(o.value) === pureValue);
    if (opt) currentLevel = levelMap.get(String(opt.value));
  }

  let lastLevel: number | undefined;
  const pureLast = extractPureValue(lastValue);
  let lastOpt = options.find((o) => o.label === pureLast);
  if (!lastOpt) lastOpt = options.find((o) => String(o.value) === pureLast);
  if (lastOpt) lastLevel = levelMap.get(String(lastOpt.value));

  if (currentLevel === undefined || lastLevel === undefined) return { valid: true };

  const mode = item.compareMode || 'positive';
  const currentLabel = options.find((o) => String(o.value) === String(extractPureValue(currentValue)))?.label || String(extractPureValue(currentValue));
  const lastLabel = lastOpt?.label || pureLast;

  if (mode === 'positive') {
    if (currentLevel < lastLevel) return { valid: false, message: `选项「${currentLabel}」不能优于上期「${lastLabel}」（等级从 ${lastLevel} -> ${currentLevel}）` };
  } else {
    if (currentLevel > lastLevel) return { valid: false, message: `选项「${currentLabel}」不能差于上期「${lastLabel}」（等级从 ${lastLevel} -> ${currentLevel}）` };
  }
  return { valid: true };
}

function compareMultiSelect(currentValue: any, lastRawValue: string, item: PositiveRuleItem): ComparisonResult {
  const options = item.options || [];
  const excludeSet = new Set(item.excludeOptions || []);

  let currentSet: Set<string>;
  if (Array.isArray(currentValue)) {
    currentSet = new Set(currentValue.map((v) => extractPureValue(v)));
  } else if (typeof currentValue === 'string') {
    currentSet = new Set(currentValue.split(',').map((v) => extractPureValue(v.trim())));
  } else {
    currentSet = new Set([extractPureValue(String(currentValue))]);
  }

  let lastSet: Set<string>;
  if (typeof lastRawValue === 'string' && lastRawValue) {
    lastSet = new Set(lastRawValue.split(',').map((v) => extractPureValue(v.trim())));
  } else {
    lastSet = new Set();
  }

  const filteredCurrent = new Set([...currentSet].filter((v) => !excludeSet.has(String(v))));
  const filteredLast = new Set([...lastSet].filter((v) => !excludeSet.has(String(v))));
  const compareType = item.compareType;

  if (compareType === 'keep_required') {
    const removed = [...filteredLast].filter((v) => !filteredCurrent.has(v));
    if (removed.length > 0) {
      const removedLabels = removed.map((v) => {
        const opt = options.find((o) => String(o.value) === String(v));
        return opt?.label || v;
      });
      return { valid: false, message: `上期选中的选项 [${removedLabels.join('、')}] 已取消选择，必须继续保持选中` };
    }
  } else if (compareType === 'new_count') {
    const minNewCount = item.minNewCount || 1;
    const newItems = [...filteredCurrent].filter((v) => !filteredLast.has(v));
    if (newItems.length < minNewCount) {
      return { valid: false, message: `本期需新增至少 ${minNewCount} 个选项（当前新增：${newItems.length}）` };
    }
  } else if (compareType === 'count') {
    const mode = item.compareMode || 'positive';
    if (mode === 'positive') {
      if (filteredCurrent.size < filteredLast.size) return { valid: false, message: `选中数量不能少于上期（上期：${filteredLast.size} 项，本期：${filteredCurrent.size} 项）` };
    } else {
      if (filteredCurrent.size > filteredLast.size) return { valid: false, message: `选中数量不能多于上期（上期：${filteredLast.size} 项，本期：${filteredCurrent.size} 项）` };
    }
  } else if (compareType === 'min_level' || compareType === 'max_level') {
    const currentLevels = [...filteredCurrent].map((v) => getOptionLevel(options, v)).filter((l) => l !== null) as number[];
    const lastLevels = [...filteredLast].map((v) => getOptionLevel(options, v)).filter((l) => l !== null) as number[];
    if (currentLevels.length === 0 || lastLevels.length === 0) return { valid: true };
    const currentExtreme = compareType === 'min_level' ? Math.min(...currentLevels) : Math.max(...currentLevels);
    const lastExtreme = compareType === 'min_level' ? Math.min(...lastLevels) : Math.max(...lastLevels);
    if (currentExtreme < lastExtreme) {
      const levelType = compareType === 'min_level' ? '最低' : '最高';
      return { valid: false, message: `${levelType}等级不能低于上期` };
    }
  }

  return { valid: true };
}

function compareContainer(_currentValue: any, _lastValue: string, _item: PositiveRuleItem): ComparisonResult {
  return { valid: true }; // 容器类型暂不支持
}

// ==================== 核心校验函数 ====================

export function hasAnyLastPeriodValue(): boolean {
  const vals = lastPeriodValues.value;
  return vals && Object.keys(vals).length > 0;
}

export function hasLastPeriodValue(indicatorCode: string): boolean {
  const val = lastPeriodValues.value[indicatorCode];
  return val !== undefined && val !== null && val !== '';
}

/**
 * 校验单条上期对比规则
 */
function validateSinglePositiveRule(
  item: PositiveRuleItem,
  indicators: DeclareIndicatorApi.Indicator[],
): PositiveRuleError | null {
  const { indicatorCode, valueType } = item;

  if (!hasLastPeriodValue(indicatorCode)) return null;

  const currentValue = getCurrentValue(indicatorCode, valueType);
  const lastValue = getLastPeriodValue(indicatorCode);
  const lastRawValue = getLastPeriodRawValue(indicatorCode);

  if (currentValue === null || currentValue === undefined || currentValue === '') return null;

  const result = compareByType(valueType, currentValue, lastValue, lastRawValue, item);
  if (!result.valid && result.message) {
    const indicator = indicators.find((i) => i.indicatorCode === indicatorCode);
    return {
      indicatorCode,
      indicatorId: indicator?.id,
      ruleName: item.name,
      message: result.message,
      errorKey: indicator?.id ? `t:${indicator.id}` : indicatorCode,
    };
  }

  return null;
}

/**
 * 校验特定指标的上期对比规则（用于实时校验）
 */
export function validatePositiveRuleForIndicator(
  changedIndicatorCode: string,
  indicators: DeclareIndicatorApi.Indicator[],
): PositiveRuleError | null {
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
        if (error) {
          setPositiveRuleError(error, item.indicatorCode);
          return error;
        } else {
          clearPositiveRuleError(item.indicatorCode, indicators);
        }
      }
    } catch (e) {
      console.error('[validatePositiveRuleForIndicator] 解析规则配置失败:', rule.id, e);
    }
  }
  return null;
}

/**
 * 校验所有上期对比规则（用于提交时）
 */
export function validateAllPositiveRules(
  indicators: DeclareIndicatorApi.Indicator[],
): PositiveRuleError[] {
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
        if (error) {
          setPositiveRuleError(error, item.indicatorCode);
          errors.push(error);
        } else {
          clearPositiveRuleError(item.indicatorCode, indicators);
        }
      }
    } catch (e) {
      console.error('[validateAllPositiveRules] 解析规则配置失败:', rule.id, e);
    }
  }
  return errors;
}

// ==================== 辅助函数 ====================

function setPositiveRuleError(error: PositiveRuleError, indicatorCode: string): void {
  const ruleId = `positive:${indicatorCode}`;
  setFieldError(error.errorKey, `${error.ruleName}：${error.message}`, 'joint', ruleId);
}

function clearPositiveRuleError(indicatorCode: string, indicators: DeclareIndicatorApi.Indicator[]): void {
  const indicator = indicators.find((i) => i.indicatorCode === indicatorCode);
  if (indicator?.id) {
    clearFieldErrorByType(`t:${indicator.id}`, 'joint');
  }
}
