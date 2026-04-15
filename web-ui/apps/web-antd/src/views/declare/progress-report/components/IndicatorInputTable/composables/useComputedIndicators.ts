/**
 * useComputedIndicators - 自动计算指标
 *
 * 负责：
 * - 自动计算指标值
 * - 重新计算触发
 */

import { nextTick } from 'vue';
import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import { formValues } from './useFormValues';
import { getNumberPrecision } from '../utils/indicator';
import { setDirty, validateTopLevelOnBlur } from './useValidation';
import { indicators } from './useIndicatorData';

// ==================== 辅助函数 ====================

function getIndicatorValue(indicatorCode: string): number | undefined {
  const value = formValues[indicatorCode];
  if (value === undefined || value === null || value === '') return undefined;
  return Number(value);
}

function calculateIndicatorValue(indicator: DeclareIndicatorApi.Indicator): number | undefined | 'CLEAR' {
  if (!indicator.calculationRule) return undefined;
  try {
    let formula = indicator.calculationRule;
    const indicatorMatches = formula.match(/\[[^\]]+\]/g) || [];
    let hasAllValues = true;
    let processedFormula = formula;

    for (const match of indicatorMatches) {
      const code = match.replace(/[\[\]]/g, '');
      const value = getIndicatorValue(code);
      if (value === undefined) { hasAllValues = false; break; }
      processedFormula = processedFormula.replace(match, String(value));
    }

    if (!hasAllValues) return 'CLEAR';

    // 将 % 作为百分比后缀处理：X% → X/100
    processedFormula = processedFormula.replace(/(\d+\.?\d*)%/g, '$1/100');

    const safeFormula = processedFormula.replace(/[^0-9+\-*/.()]/g, '');
    if (!safeFormula || safeFormula.trim() === '') return 'CLEAR';

    const result = new Function(`return ${safeFormula}`)();
    if (isNaN(result) || !isFinite(result)) return 'CLEAR';

    return result;
  } catch {
    return 'CLEAR';
  }
}

// ==================== 重新计算 ====================

function recalculateComputedIndicators(_indicators?: DeclareIndicatorApi.Indicator[]) {
  const allIndicators = _indicators ?? indicators.value;
  const computedOnes = allIndicators.filter((ind) => {
    return !!(ind.calculationRule && ind.calculationRule.trim());
  });

  const MAX_PASSES = computedOnes.length + 1;
  let changed = true;
  let pass = 0;

  while (changed && pass < MAX_PASSES) {
    changed = false;
    pass++;
    for (const ind of computedOnes) {
      const calculatedValue = calculateIndicatorValue(ind);
      if (calculatedValue === 'CLEAR') {
        // 源指标为空时，清空计算指标的值
        if (formValues[ind.indicatorCode] !== undefined) {
          formValues[ind.indicatorCode] = undefined;
          changed = true;
        }
      } else if (calculatedValue !== undefined) {
        const precision = getNumberPrecision(ind);
        const effectivePrecision = precision !== undefined ? precision : 2;
        const fixedStr = calculatedValue.toFixed(effectivePrecision);
        const finalValue = parseFloat(fixedStr);
        formValues[ind.indicatorCode] = finalValue;
        changed = true;
      }
    }
  }

  // 更新 DOM 中的输入框显示值，并触发范围校验以更新 fieldErrors
  nextTick(() => {
    for (const ind of computedOnes) {
      const el = document.querySelector(`[data-indicator-code="${ind.indicatorCode}"] .safe-number-input-inner`) as HTMLInputElement | null;
      if (formValues[ind.indicatorCode] !== undefined) {
        if (el) {
          const precision = getNumberPrecision(ind);
          const effectivePrecision = precision !== undefined ? precision : 2;
          el.value = formValues[ind.indicatorCode]!.toFixed(effectivePrecision);
        }
      } else {
        // 源指标为空时，清空计算指标的显示
        if (el) {
          el.value = '';
        }
      }
      if (ind.id !== undefined) {
        setDirty(`t:${ind.id}`);
        // 触发范围校验，让 fieldErrors 写入错误信息，UI 立即显示
        validateTopLevelOnBlur(ind, formValues[ind.indicatorCode]);
      }
    }
  });
}

// ==================== 导出 ====================

export {
  getIndicatorValue,
  calculateIndicatorValue,
  recalculateComputedIndicators,
};
