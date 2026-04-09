/**
 * 逻辑规则校验 composable
 */

import { reactive } from 'vue';
import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import { validate as validateJointRule, parseLogicRule } from '#/utils/indicatorValidator';
import type { ValidationError } from '../types';

export interface UseLogicRulesOptions {
  formValues: Record<string, any>;
  indicators: typeof import('vue').ref<DeclareIndicatorApi.Indicator[]>;
}

export function useLogicRules() {
  /** 逻辑规则错误 Map */
  const logicRuleErrors = reactive<Record<string, string>>({});

  /** 解析 valueOptions JSON */
  function parseOptions(valueOptions: string): Array<{ value: string; label: string }> {
    if (!valueOptions) return [];
    try {
      const parsed = JSON.parse(valueOptions);
      return Array.isArray(parsed) ? parsed : [];
    } catch {
      return [];
    }
  }

  /** 生成逻辑规则错误提示 */
  function buildLogicRuleMsg(
    logicRule: string,
    allIndicators: DeclareIndicatorApi.Indicator[],
    codeValueMap: Record<string, any>,
  ): string {
    if (!logicRule) return '校验失败';

    const match = logicRule.trim().match(/^(.+?)\s*(>=|<=|>|<|==|!=)\s*(.+)$/);
    if (!match) return '校验失败';

    const leftRaw = match[1]!.trim();
    const operator = match[2]!;
    const rightRaw = match[3]!.trim();

    const codeMap = new Map<string, string>();
    for (const ind of allIndicators) {
      if (!codeMap.has(ind.indicatorCode)) {
        codeMap.set(ind.indicatorCode, ind.indicatorName || ind.indicatorCode);
      }
    }

    const opText: Record<string, string> = {
      '>=': '应大于等于',
      '<=': '应小于等于',
      '>': '应大于',
      '<': '应小于',
      '==': '应等于',
      '!=': '不应等于',
    };

    const replaceCode = (code: string): string => {
      const name = codeMap.get(code) || code;
      const val = codeValueMap[code];
      const valText = val !== undefined && val !== null && val !== '' ? String(val) : '未填';
      return `${name}(${valText})`;
    };

    const msgLeft = leftRaw.replace(/\[([^\]]+)\]/g, (_, c) => replaceCode(c.trim()));
    const msgRight = rightRaw.replace(/\[([^\]]+)\]/g, (_, c) => replaceCode(c.trim()));

    return `${msgLeft} ${opText[operator] || operator} ${msgRight}`;
  }

  /** 逻辑规则校验 */
  function validateLogicRules(allIndicators: DeclareIndicatorApi.Indicator[]): ValidationError[] {
    const errors: ValidationError[] = [];

    const codeToIndicator = new Map<string, DeclareIndicatorApi.Indicator>();
    for (const ind of allIndicators) {
      codeToIndicator.set(ind.indicatorCode, ind);
    }

    const codeValueMap: Record<string, any> = {};
    for (const ind of allIndicators) {
      codeValueMap[ind.indicatorCode] = formValues[ind.indicatorCode];
    }

    for (const indicator of allIndicators) {
      if (!indicator.logicRule?.trim()) continue;

      const rules = parseLogicRule(indicator.logicRule);
      if (rules.length === 0) continue;

      const results = validateJointRule(rules as any, codeValueMap, { triggerTiming: 'FILL' });
      const errMsg = buildLogicRuleMsg(indicator.logicRule, allIndicators, codeValueMap);

      if (results.length === 0) {
        // 校验通过：清除错误
        const involvedCodes = new Set<string>();
        for (const m of indicator.logicRule.matchAll(/\[([^\]]+)\]/g)) {
          involvedCodes.add(m[1]!.trim());
        }
        for (const code of involvedCodes) {
          for (const ind of allIndicators) {
            if (ind.indicatorCode === code && ind.id !== undefined) {
              delete logicRuleErrors[String(ind.id)];
            }
          }
          delete logicRuleErrors[code];
        }
        continue;
      }

      // 校验失败
      errors.push({
        indicatorId: indicator.id,
        indicatorCode: indicator.indicatorCode,
        message: errMsg,
        errorType: 'logic',
        indicatorName: indicator.indicatorName,
      });
      if (indicator.id !== undefined) logicRuleErrors[String(indicator.id)] = errMsg;
    }

    return errors;
  }

  /** 实时逻辑规则校验（blur 时调用） */
  function validateLogicRuleForBlur(changedIndicator: DeclareIndicatorApi.Indicator) {
    const allIndicators = indicators.value;
    const changedCode = changedIndicator.indicatorCode;

    const involvedIndicatorCodes = new Set<string>();
    for (const indicator of allIndicators) {
      if (!indicator.logicRule?.trim()) continue;
      for (const m of indicator.logicRule.matchAll(/\[([^\]]+)\]/g)) {
        if (m[1]!.trim() === changedCode) {
          involvedIndicatorCodes.add(indicator.indicatorCode);
        }
      }
    }

    const codeValueMap: Record<string, any> = {};
    for (const ind of allIndicators) {
      codeValueMap[ind.indicatorCode] = formValues[ind.indicatorCode];
    }

    for (const code of involvedIndicatorCodes) {
      const indicator = allIndicators.find((i) => i.indicatorCode === code);
      if (!indicator || !indicator.logicRule?.trim()) continue;

      // 清除旧错误
      for (const ind of allIndicators) {
        if (ind.indicatorCode === code && ind.id !== undefined) {
          delete logicRuleErrors[String(ind.id)];
        }
      }

      const rules = parseLogicRule(indicator.logicRule);
      const results = validateJointRule(rules as any, codeValueMap, { triggerTiming: 'FILL' });

      if (results.length > 0) {
        const errMsg = buildLogicRuleMsg(indicator.logicRule, allIndicators, codeValueMap);
        if (indicator.id !== undefined) logicRuleErrors[String(indicator.id)] = errMsg;
      }
    }
  }

  return {
    logicRuleErrors,
    validateLogicRules,
    validateLogicRuleForBlur,
  };
}
