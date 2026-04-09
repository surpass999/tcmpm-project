/**
 * 计算指标逻辑 composable
 */

import type { DeclareIndicatorApi } from '#/api/declare/indicator';

export interface UseComputedIndicatorsOptions {
  formValues: Record<string, any>;
  indicators: typeof import('vue').ref<DeclareIndicatorApi.Indicator[]>;
}

export function useComputedIndicators() {
  /** 获取指标值 */
  function getIndicatorValue(formValues: Record<string, any>, indicatorCode: string): number | undefined {
    const value = formValues[indicatorCode];
    if (value === undefined || value === null || value === '') {
      return undefined;
    }
    return Number(value);
  }

  /** 计算指标值 */
  function calculateIndicatorValue(
    indicator: DeclareIndicatorApi.Indicator,
    formValues: Record<string, any>,
  ): number | undefined {
    if (!indicator.calculationRule) return undefined;

    try {
      let formula = indicator.calculationRule;
      const indicatorMatches = formula.match(/\[[^\]]+\]/g) || [];

      let hasAllValues = true;
      let processedFormula = formula;

      for (const match of indicatorMatches) {
        const code = match.replace(/[\[\]]/g, '');
        const value = getIndicatorValue(formValues, code);

        if (value === undefined) {
          hasAllValues = false;
          break;
        }
        processedFormula = processedFormula.replace(match, String(value));
      }

      if (!hasAllValues) return undefined;

      const safeFormula = processedFormula.replace(/[^0-9+\-*/.()%]/g, '');
      if (!safeFormula || safeFormula.trim() === '') return undefined;

      const result = new Function(`return ${safeFormula}`)();
      if (isNaN(result) || !isFinite(result)) return undefined;

      return result;
    } catch {
      return undefined;
    }
  }

  /** 解析指标公式中的依赖指标代码列表 */
  function parseCalcDependencies(calcRule: string): string[] {
    if (!calcRule) return [];
    const matches = calcRule.match(/\[[^\]]+\]/g) || [];
    return matches.map((m) => m.replace(/[\[\]]/g, ''));
  }

  /** 拓扑排序 */
  function sortIndicatorsTopological(
    computedOnes: DeclareIndicatorApi.Indicator[],
  ): DeclareIndicatorApi.Indicator[] {
    const codeToInd = new Map<string, DeclareIndicatorApi.Indicator>();
    computedOnes.forEach((ind) => codeToInd.set(ind.indicatorCode, ind));

    const inDegree = new Map<string, number>();
    const deps = new Map<string, string[]>();

    computedOnes.forEach((ind) => {
      const depsCodes = parseCalcDependencies(ind.calculationRule || '');
      inDegree.set(ind.indicatorCode, 0);
      deps.set(ind.indicatorCode, []);
      depsCodes.forEach((depCode) => {
        if (codeToInd.has(depCode)) {
          deps.get(ind.indicatorCode)!.push(depCode);
          inDegree.set(ind.indicatorCode, (inDegree.get(ind.indicatorCode) || 0) + 1);
        }
      });
    });

    const queue: string[] = [];
    inDegree.forEach((deg, code) => {
      if (deg === 0) queue.push(code);
    });

    const sorted: DeclareIndicatorApi.Indicator[] = [];
    while (queue.length > 0) {
      const code = queue.shift()!;
      const ind = codeToInd.get(code)!;
      sorted.push(ind);
      const myDeps = deps.get(code) || [];
      myDeps.forEach((depCode) => {
        const newDeg = (inDegree.get(depCode) || 1) - 1;
        inDegree.set(depCode, newDeg);
        if (newDeg === 0) queue.push(depCode);
      });
    }

    computedOnes.forEach((ind) => {
      if (!sorted.includes(ind)) sorted.push(ind);
    });

    return sorted;
  }

  /** 重新计算所有计算指标 */
  function recalculateComputedIndicators(
    indicators: DeclareIndicatorApi.Indicator[],
    formValues: Record<string, any>,
    getNumberPrecision?: (indicator: DeclareIndicatorApi.Indicator) => number | undefined,
  ) {
    const computedOnes = indicators.filter(
      (ind) => ind.calculationRule && ind.calculationRule.trim(),
    );
    if (computedOnes.length === 0) return;

    const MAX_PASSES = computedOnes.length + 1;
    let changed = true;
    let pass = 0;

    while (changed && pass < MAX_PASSES) {
      changed = false;
      pass++;

      const sorted = sortIndicatorsTopological(computedOnes);
      for (const ind of sorted) {
        const calculatedValue = calculateIndicatorValue(ind, formValues);
        if (calculatedValue !== undefined) {
          let finalValue: number = calculatedValue;

          if (getNumberPrecision) {
            const precision = getNumberPrecision(ind);
            if (precision !== undefined) {
              finalValue = Number(calculatedValue.toFixed(precision));
            }
          }

          formValues[ind.indicatorCode] = finalValue;
          changed = true;
        }
      }
    }
  }

  return {
    getIndicatorValue,
    calculateIndicatorValue,
    parseCalcDependencies,
    sortIndicatorsTopological,
    recalculateComputedIndicators,
  };
}
