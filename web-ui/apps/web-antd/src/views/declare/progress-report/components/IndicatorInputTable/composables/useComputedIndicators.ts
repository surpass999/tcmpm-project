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
import { setDirty } from './useValidation';

// ==================== 辅助函数 ====================

function getIndicatorValue(indicatorCode: string): number | undefined {
  const value = formValues[indicatorCode];
  if (value === undefined || value === null || value === '') return undefined;
  return Number(value);
}

function calculateIndicatorValue(indicator: DeclareIndicatorApi.Indicator): number | undefined {
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

    if (!hasAllValues) return undefined;

    // 将 % 作为百分比后缀处理：X% → X/100
    processedFormula = processedFormula.replace(/(\d+\.?\d*)%/g, '$1/100');

    const safeFormula = processedFormula.replace(/[^0-9+\-*/.()]/g, '');
    if (!safeFormula || safeFormula.trim() === '') return undefined;

    const result = new Function(`return ${safeFormula}`)();
    if (isNaN(result) || !isFinite(result)) return undefined;

    return result;
  } catch {
    return undefined;
  }
}

// ==================== 重新计算 ====================

function recalculateComputedIndicators(indicators: DeclareIndicatorApi.Indicator[]) {
  const computedOnes = indicators.filter((ind) => {
    return !!(ind.calculationRule && ind.calculationRule.trim());
  });

  if (computedOnes.length === 0) return;

  const MAX_PASSES = computedOnes.length + 1;
  let changed = true;
  let pass = 0;

  while (changed && pass < MAX_PASSES) {
    changed = false;
    pass++;
    for (const ind of computedOnes) {
      const calculatedValue = calculateIndicatorValue(ind);
      if (calculatedValue !== undefined) {
        const precision = getNumberPrecision(ind);
        const effectivePrecision = precision !== undefined ? precision : 2;
        const fixedStr = calculatedValue.toFixed(effectivePrecision);
        const finalValue = parseFloat(fixedStr);
        formValues[ind.indicatorCode] = finalValue;
        changed = true;
      }
    }
  }

  // 更新 DOM 中的输入框显示值
  nextTick(() => {
    for (const ind of computedOnes) {
      if (formValues[ind.indicatorCode] !== undefined) {
        const el = document.querySelector(`[data-indicator-code="${ind.indicatorCode}"] .safe-number-input-inner`) as HTMLInputElement | null;
        if (el) {
          const precision = getNumberPrecision(ind);
          const effectivePrecision = precision !== undefined ? precision : 2;
          el.value = formValues[ind.indicatorCode]!.toFixed(effectivePrecision);
        }
      }
      if (ind.id !== undefined) setDirty(`t:${ind.id}`);
    }
  });
}

// ==================== 导出 ====================

export {
  getIndicatorValue,
  calculateIndicatorValue,
  recalculateComputedIndicators,
};
