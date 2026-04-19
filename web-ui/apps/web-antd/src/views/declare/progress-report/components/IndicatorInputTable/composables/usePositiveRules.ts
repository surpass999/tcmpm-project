/**
 * usePositiveRules - 上期对比规则校验
 *
 * 职责：
 * 1. 解析 jointRules.ruleConfig JSON
 * 2. 按 triggerTiming === 'FILL' 过滤规则
 * 3. 逐指标校验（基础校验通过后）
 * 4. 将错误写入 fieldErrors
 *
 * 校验顺序：
 * 1. 提取当前值和上期值
 * 2. 根据 valueType 和 compareType 调用对应校验函数
 * 3. 返回错误信息
 */

import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import type { PositiveRuleConfig, PositiveRuleItem, PositiveRuleError } from '../types';
import { jointRules, lastPeriodValues, lastPeriodRawValues } from './useIndicatorData';
import { formValues } from './useFormValues';
import { containerValues } from './useContainerValues';
import { setFieldError, clearFieldErrorIfType } from './useErrorKeys';

// ==================== 核心校验函数 ====================

/**
 * 检查是否有上期值（决定是否加载校验逻辑）
 */
export function hasAnyLastPeriodValue(): boolean {
  const vals = lastPeriodValues.value;
  return vals && Object.keys(vals).length > 0;
}

/**
 * 检查特定指标是否有上期值
 */
export function hasLastPeriodValue(indicatorCode: string): boolean {
  const val = lastPeriodValues.value[indicatorCode];
  return val !== undefined && val !== null && val !== '';
}

/**
 * 校验所有上期对比规则（用于提交时）
 * @returns 错误列表
 */
export function validateAllPositiveRules(
  indicators: DeclareIndicatorApi.Indicator[],
): PositiveRuleError[] {
  const errors: PositiveRuleError[] = [];

  // 1. 检查是否有上期值
  if (!hasAnyLastPeriodValue()) {
    return errors;
  }

  // 2. 获取 FILL 时机的规则
  const fillRules = jointRules.value.filter((r) => r.triggerTiming === 'FILL');

  for (const rule of fillRules) {
    if (!rule.ruleConfig) continue;

    try {
      const config: PositiveRuleConfig = JSON.parse(rule.ruleConfig);
      if (!config.rules?.length) continue;

      for (const item of config.rules) {
        const error = validateSinglePositiveRule(item, indicators);
        if (error) {
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

/**
 * 校验特定指标的上期对比规则（用于实时校验）
 * 仅校验涉及指定指标的规则
 */
export function validatePositiveRuleForIndicator(
  changedIndicatorCode: string,
  indicators: DeclareIndicatorApi.Indicator[],
): PositiveRuleError | null {

  if (!hasAnyLastPeriodValue()) {
    return null;
  }

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
          setPositiveRuleError(error);
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
 * 校验单条上期对比规则
 */
function validateSinglePositiveRule(
  item: PositiveRuleItem,
  indicators: DeclareIndicatorApi.Indicator[],
): PositiveRuleError | null {
  const { indicatorCode, valueType } = item;

  if (!hasLastPeriodValue(indicatorCode)) {
    return null;
  }

  const currentValue = getCurrentValue(indicatorCode, valueType);
  const lastValue = getLastPeriodValue(indicatorCode);
  const lastRawValue = getLastPeriodRawValue(indicatorCode);

  // 如果当前值为空，跳过校验（基础校验会处理必填）
  if (currentValue === null || currentValue === undefined || currentValue === '') {
    return null;
  }

  let errorMsg: string | null = null;

  switch (valueType) {
    case 1: // 数字
      errorMsg = validateNumberRule(item, currentValue, lastValue);
      break;
    case 6: // 单选
      errorMsg = validateRadioRule(item, currentValue, lastValue);
      break;
    case 7: // 多选
      errorMsg = validateMultiSelectRule(item, currentValue, lastRawValue);
      break;
    case 12: // 容器
      errorMsg = validateContainerRule(item, currentValue, lastValue);
      break;
    default:
      console.warn('[validateSinglePositiveRule] 不支持的 valueType:', valueType);
  }

  if (errorMsg) {
    const indicator = indicators.find((i) => i.indicatorCode === indicatorCode);
    return {
      indicatorCode,
      indicatorId: indicator?.id,
      ruleName: item.name,
      message: errorMsg,
      errorKey: indicator?.id ? `t:${indicator.id}` : indicatorCode,
    };
  }

  return null;
}

// ==================== 各类校验函数 ====================

/**
 * 校验数字类型规则
 * compareMode: positive → current >= last
 *             negative → current <= last
 */
function validateNumberRule(
  item: PositiveRuleItem,
  currentValue: any,
  lastValue: string,
): string | null {
  const current = Number(currentValue);
  const last = Number(lastValue);

  if (isNaN(current) || isNaN(last)) {
    return null;
  }

  const compareMode = item.compareMode || 'positive';

  if (compareMode === 'positive') {
    if (current < last) {
      return `当前值 ${current} 不能小于上期值 ${last}`;
    }
  } else {
    if (current > last) {
      return `当前值 ${current} 不能大于上期值 ${last}`;
    }
  }

  return null;
}

/**
 * 校验单选类型规则
 * 等级 = 选项在配置中的位置（从1开始）
 * compareMode: positive → currentLevel >= lastLevel（只能升级）
 *             negative → currentLevel <= lastLevel（只能降级）
 *
 * 注意：
 * - currentValue 可能是 value（如 "02"）或 label（从下拉选中的显示值）
 * - lastPeriodValues[indicatorCode] 存的是 display label（如 "选项B"）
 *   因为后端 getDisplayValue 已将 value 映射为 label
 * - 因此需要先用 lastValue（label）查找对应 option，再获取 level
 */
function validateRadioRule(
  item: PositiveRuleItem,
  currentValue: any,
  lastValue: string,
): string | null {
  const options = item.options || [];
  if (!options.length) return null;

  const levelMap = new Map<string, number>();
  options.forEach((opt, idx) => {
    levelMap.set(String(opt.value), idx + 1);
  });
  const currentPureValue = extractPureValue(currentValue);

  // currentValue 可能是 value 也可能是 label
  // 优先用 value 查 levelMap，找不到再用 label 查找
  let currentLevel = levelMap.get(String(currentPureValue));
  if (currentLevel === undefined) {
    const currentOpt = options.find((o) => o.label === currentPureValue || String(o.value) === currentPureValue);
    if (currentOpt) currentLevel = levelMap.get(String(currentOpt.value));
  }

  // lastPeriodValues 存的是 display label（如 "选项B"），需要先找到对应 option 的 value
  let lastLevel: number | undefined;
  const lastPureValue = extractPureValue(lastValue);
  // 优先用 label 查找（lastValue 通常是 label）
  let lastOpt = options.find((o) => o.label === lastPureValue);
  if (!lastOpt) {
    // fallback：尝试用 value 查找
    lastOpt = options.find((o) => String(o.value) === lastPureValue);
  }
  if (lastOpt) {
    lastLevel = levelMap.get(String(lastOpt.value));
  }

  if (currentLevel === undefined || lastLevel === undefined) {
    return null;
  }

  const compareMode = item.compareMode || 'positive';

  if (compareMode === 'positive') {
    if (currentLevel < lastLevel) {
      const currentLabel = options.find((o) => String(o.value) === String(extractPureValue(currentValue)))?.label || String(extractPureValue(currentValue));
      const lastLabel = lastOpt?.label || lastPureValue;
      return `选项「${currentLabel}」不能优于上期「${lastLabel}」（等级从 ${lastLevel} -> ${currentLevel}）`;
    }
  } else {
    if (currentLevel > lastLevel) {
      const currentLabel = options.find((o) => String(o.value) === String(extractPureValue(currentValue)))?.label || String(extractPureValue(currentValue));
      const lastLabel = lastOpt?.label || lastPureValue;
      return `选项「${currentLabel}」不能差于上期「${lastLabel}」（等级从 ${lastLevel} -> ${currentLevel}）`;
    }
  }

  return null;
}

/**
 * 校验多选类型规则
 * lastRawValue = lastPeriodRawValues[indicatorCode]，格式为 "1,2,3" 或 "1∵内容,2∵内容"
 */
function validateMultiSelectRule(
  item: PositiveRuleItem,
  currentValue: any,
  lastRawValue: string,
): string | null {
  const options = item.options || [];
  const excludeSet = new Set(item.excludeOptions || []);

  // 解析当前值（已序列化为数组 ["1∵内容1", "2∵内容2"]）
  let currentSet: Set<string>;
  if (Array.isArray(currentValue)) {
    currentSet = new Set(currentValue.map((v) => extractPureValue(v)));
  } else if (typeof currentValue === 'string') {
    currentSet = new Set(currentValue.split(',').map((v) => extractPureValue(v.trim())));
  } else {
    currentSet = new Set([extractPureValue(String(currentValue))]);
  }

  // 解析上期原始值（"1,2,3" 或 "1∵内容,2∵内容"，用英文逗号分隔）
  let lastSet: Set<string>;
  if (typeof lastRawValue === 'string' && lastRawValue) {
    lastSet = new Set(lastRawValue.split(',').map((v) => extractPureValue(v.trim())));
  } else {
    lastSet = new Set();
  }

  // 排除指定选项
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
      return `上期选中的选项 [${removedLabels.join('、')}] 已取消选择，必须继续保持选中`;
    }
  } else if (compareType === 'new_count') {
    const minNewCount = item.minNewCount || 1;
    const newItems = [...filteredCurrent].filter((v) => !filteredLast.has(v));
    if (newItems.length < minNewCount) {
      return `本期需新增至少 ${minNewCount} 个选项（当前新增：${newItems.length}）`;
    }
  } else if (compareType === 'count') {
    const compareMode = item.compareMode || 'positive';
    if (compareMode === 'positive') {
      if (filteredCurrent.size < filteredLast.size) {
        return `选中数量不能少于上期（上期：${filteredLast.size} 项，本期：${filteredCurrent.size} 项）`;
      }
    } else {
      if (filteredCurrent.size > filteredLast.size) {
        return `选中数量不能多于上期（上期：${filteredLast.size} 项，本期：${filteredCurrent.size} 项）`;
      }
    }
  } else if (compareType === 'min_level' || compareType === 'max_level') {
    const currentLevels = [...filteredCurrent].map((v) => getOptionLevel(options, v)).filter((l) => l !== null) as number[];
    const lastLevels = [...filteredLast].map((v) => getOptionLevel(options, v)).filter((l) => l !== null) as number[];

    if (currentLevels.length === 0 || lastLevels.length === 0) {
      return null;
    }

    const currentExtreme = compareType === 'min_level' ? Math.min(...currentLevels) : Math.max(...currentLevels);
    const lastExtreme = compareType === 'min_level' ? Math.min(...lastLevels) : Math.max(...lastLevels);

    if (currentExtreme < lastExtreme) {
      const levelType = compareType === 'min_level' ? '最低' : '最高';
      return `${levelType}等级不能低于上期`;
    }
  }

  return null;
}

/**
 * 校验容器类型规则
 * 容器值序列化为 JSON 后比较
 */
function validateContainerRule(
  item: PositiveRuleItem,
  currentValue: any,
  lastValue: string,
): string | null {
  // 容器类型暂不支持上期对比校验
  // 如需支持，可根据 compareType 实现具体逻辑
  return null;
}

// ==================== 辅助函数 ====================

/**
 * 获取指标的当前值
 */
function getCurrentValue(indicatorCode: string, valueType: number): any {
  if (valueType === 12) {
    const entries = containerValues[indicatorCode];
    if (!entries || !entries.length) return null;
    return JSON.stringify(entries);
  }
  return formValues[indicatorCode];
}

/**
 * 获取指标的上期值（用于数值对比）
 */
function getLastPeriodValue(indicatorCode: string): string {
  return lastPeriodValues.value[indicatorCode] || '';
}

/**
 * 获取指标的上期原始值（包含 inputType 内容，用于多选对比）
 * rawValue 格式："1,2,3" 或 "1∵输入内容1,2∵输入内容2,3"
 */
function getLastPeriodRawValue(indicatorCode: string): string {
  return lastPeriodRawValues.value[indicatorCode] || '';
}

/**
 * 提取纯 value（去掉 inputType 的 ∵ 分隔符）
 */
function extractPureValue(value: any): string {
  if (value === null || value === undefined) return '';

  // 处理反序列化后的对象 { value, input }
  if (typeof value === 'object' && 'value' in value) {
    value = value.value;
  }

  const str = String(value);
  const idx = str.indexOf('∵');
  return idx >= 0 ? str.substring(0, idx) : str;
}

/**
 * 获取选项的等级（位置从1开始）
 */
function getOptionLevel(options: PositiveRuleItem['options'], value: string): number | null {
  if (!options) return null;
  const index = options.findIndex((o) => String(o.value) === String(value));
  return index >= 0 ? index + 1 : null;
}

/**
 * 设置上期规则错误到 fieldErrors
 */
function setPositiveRuleError(error: PositiveRuleError): void {
  setFieldError(error.errorKey, `${error.ruleName}：${error.message}`, 'joint', false);
}

/**
 * 清除上期规则错误
 */
function clearPositiveRuleError(indicatorCode: string, indicators: DeclareIndicatorApi.Indicator[]): void {
  const indicator = indicators.find((i) => i.indicatorCode === indicatorCode);
  if (indicator?.id) {
    clearFieldErrorIfType(`t:${indicator.id}`, 'joint');
  }
}
