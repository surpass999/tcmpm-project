/**
 * useLogicRules - 逻辑规则校验
 *
 * 负责：
 * - 逻辑规则解析
 * - 逻辑规则校验（逐条目 + 顶层）
 * - 错误消息构建
 */

import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import { validate as validateJointRule, parseLogicRule, parseContainerFieldShortcut, isContainerFieldShortcut } from '#/utils/indicatorValidator';
import type { ValidationError, FieldError } from '../types';
import { formValues } from './useFormValues';
import { containerValues, isEntryBasedContainer, getContainerEntryCount } from './useContainerValues';
import { parseDynamicFields } from '../utils/container';

// ==================== 构建值映射 ====================

/** 构建单个条目的完整值Map（用于逐条目验证） */
function buildEntryValueMap(
  indicator: DeclareIndicatorApi.Indicator,
  entryNumber: number
): Record<string, any> {
  const map: Record<string, any> = {};
  const containerCode = indicator.indicatorCode;
  const entryKey = `${containerCode}${String(entryNumber).padStart(2, '0')}`;

  const entries = containerValues[containerCode] || [];
  const entry = entries.find((e: any) => e.rowKey === entryKey);

  if (entry) {
    const config = parseDynamicFields(indicator.valueOptions);
    const fields = config || [];

    for (const field of fields) {
      const fullKey = `${entryKey}${field.fieldCode}`;
      map[fullKey] = entry[fullKey];
    }
  }

  return map;
}

/** 构建指标编码到值的映射 */
function buildCodeValueMap(): Record<string, any> {
  const map: Record<string, any> = {};
  for (const [code, value] of Object.entries(formValues)) {
    map[code] = value;
  }
  for (const indicator of Object.values(containerValues)) {
    // containerValues 中的每个容器指标
  }
  return map;
}

// ==================== 错误消息构建 ====================

function buildLogicRuleMsg(
  logicRule: string,
  allIndicators: DeclareIndicatorApi.Indicator[],
  codeValueMap: Record<string, any>,
  entryNumber?: number,
  containerIndicator?: DeclareIndicatorApi.Indicator
): string {
  if (!logicRule) return '校验失败';

  const codeMap = new Map<string, string>();
  for (const ind of allIndicators) {
    if (!codeMap.has(ind.indicatorCode)) codeMap.set(ind.indicatorCode, ind.indicatorName || ind.indicatorCode);
  }

  const getContainerFieldLabel = (fullKey: string): { label: string; valueText: string } => {
    if (!containerIndicator) return { label: fullKey, valueText: codeValueMap[fullKey] !== undefined ? String(codeValueMap[fullKey]) : '未填' };

    const fields = parseDynamicFields(containerIndicator.valueOptions);
    const fieldCode = fullKey.slice(-2);
    const field = fields.find((f) => f.fieldCode === fieldCode);

    if (!field) return { label: fullKey, valueText: codeValueMap[fullKey] !== undefined ? String(codeValueMap[fullKey]) : '未填' };

    const val = codeValueMap[fullKey];
    let valueText = '未填';
    if (val !== undefined && val !== null && val !== '') {
      if (field.fieldType === 'radio' || field.fieldType === 'checkbox') {
        const options = field.options || [];
        const option = options.find((o) => o.value === String(val) || o.value === val);
        valueText = option ? option.label : String(val);
      } else {
        valueText = String(val);
      }
    }

    return { label: field.fieldLabel, valueText };
  };

  const replaceCode = (code: string): string => {
    let fullKey = code;
    if (isContainerFieldShortcut(code) && entryNumber !== undefined) {
      const parsed = parseContainerFieldShortcut(code, entryNumber);
      if (parsed) {
        fullKey = parsed;
      }
    }

    if (containerIndicator && fullKey.startsWith(containerIndicator.indicatorCode)) {
      const { label, valueText } = getContainerFieldLabel(fullKey);
      return `${label}(${valueText})`;
    }

    const name = codeMap.get(code) || code;
    const val = codeValueMap[code];
    const valText = val !== undefined && val !== null && val !== '' ? String(val) : '未填';
    return `${name}(${valText})`;
  };

  const ifMatch = logicRule.trim().match(/^IF\s*\(\s*\[([^\]]+)\]\s*([><]=?)\s*(\d+(?:\.\d+)?)\s*,\s*\[([^\]]+)\]\s*([><]=?)\s*(\d+(?:\.\d+)?)\s*,\s*TRUE\s*\)$/i);
  if (ifMatch) {
    const condCode = ifMatch[1]!.trim();
    const condOp = ifMatch[2]!;
    const condVal = ifMatch[3]!;
    const verifyCode = ifMatch[4]!.trim();
    const verifyOp = ifMatch[5]!;
    const verifyVal = ifMatch[6]!;

    const opText: Record<string, string> = { '>=': '应大于等于', '<=': '应小于等于', '>': '应大于', '<': '应小于' };

    const condMsg = replaceCode(condCode);
    const verifyMsg = replaceCode(verifyCode);

    return `当 ${condMsg} ${condOp} ${condVal} 时, ${verifyMsg} ${opText[verifyOp] || verifyOp} ${verifyVal}`;
  }

  const match = logicRule.trim().match(/^(.+?)\s*(>=|<=|>|<|==|!=)\s*(.+)$/);
  if (!match) return '校验失败';
  const leftRaw = match[1]!.trim();
  const operator = match[2]!;
  const rightRaw = match[3]!.trim();

  const opText: Record<string, string> = { '>=': '应大于等于', '<=': '应小于等于', '>': '应大于', '<': '应小于', '==': '应等于', '!=': '不应等于' };
  const msgLeft = leftRaw.replace(/\[([^\]]+)\]/g, (_, c) => replaceCode(c.trim()));
  const msgRight = rightRaw.replace(/\[([^\]]+)\]/g, (_, c) => replaceCode(c.trim()));
  return `${msgLeft} ${opText[operator] || operator} ${msgRight}`;
}

// ==================== 查找字段 ====================

/** 查找条目中第一个有值的字段代码 */
function findFirstFilledFieldCode(indicator: DeclareIndicatorApi.Indicator, entryNumber: number): string | undefined {
  const entryKey = `${indicator.indicatorCode}${String(entryNumber).padStart(2, '0')}`;
  const entries = containerValues[indicator.indicatorCode] || [];
  const entry = entries.find((e: any) => e.rowKey === entryKey);
  if (!entry) return undefined;

  const fields = parseDynamicFields(indicator.valueOptions);
  for (const field of fields) {
    const fullKey = `${entryKey}${field.fieldCode}`;
    const value = entry[fullKey];
    if (value !== undefined && value !== null && value !== '') {
      return field.fieldCode;
    }
  }
  return fields[0]?.fieldCode;
}

// ==================== 清除容器逻辑规则错误 ====================

function clearContainerLogicRuleErrors(indicator: DeclareIndicatorApi.Indicator, clearFieldError: (key: string) => void) {
  const entries = containerValues[indicator.indicatorCode] || [];
  for (const entry of entries) {
    const fields = parseDynamicFields(indicator.valueOptions);
    for (const field of fields) {
      const fieldKey = `${indicator.indicatorCode}${entry.rowKey}${field.fieldCode}`;
      clearFieldError(fieldKey);
    }
  }
}

// ==================== 逻辑规则校验 ====================

/**
 * 完整逻辑规则校验（用于提交）
 */
function validateLogicRules(
  allIndicators: DeclareIndicatorApi.Indicator[],
  setFieldError: (key: string, message: string, type: FieldError['errorType'], dirty?: boolean) => void,
  clearFieldError: (key: string) => void,
): ValidationError[] {
  const errors: ValidationError[] = [];
  const codeValueMap: Record<string, any> = {};
  for (const ind of allIndicators) codeValueMap[ind.indicatorCode] = formValues[ind.indicatorCode];

  for (const indicator of allIndicators) {
    if (!indicator.logicRule?.trim()) continue;

    if (isEntryBasedContainer(indicator)) {
      const entryCount = getContainerEntryCount(indicator.indicatorCode);
      if (entryCount === 0) continue;

      for (let entryNum = 1; entryNum <= entryCount; entryNum++) {
        const rules = parseLogicRule(indicator.logicRule, entryNum);
        if (rules.length === 0) continue;

        const entryValueMap = buildEntryValueMap(indicator, entryNum);
        const results = validateJointRule(rules as any, entryValueMap, { triggerTiming: 'FILL' });

        if (results.length === 0) continue;

        const errMsg = buildLogicRuleMsg(indicator.logicRule, allIndicators, entryValueMap, entryNum, indicator);
        const entryKey = `${indicator.indicatorCode}${String(entryNum).padStart(2, '0')}`;

        let fieldCode = '';
        if (results[0]?.involvedIndicatorCodes?.length) {
          const involvedCode = results[0].involvedIndicatorCodes.find((c) =>
            c.startsWith(indicator.indicatorCode)
          );
          if (involvedCode) {
            fieldCode = involvedCode.slice(-2);
          }
        }

        if (!fieldCode) {
          const fields = parseDynamicFields(indicator.valueOptions);
          for (const field of fields) {
            const fullKey = `${entryKey}${field.fieldCode}`;
            const val = entryValueMap[fullKey];
            if (val !== undefined && val !== null && val !== '') {
              fieldCode = field.fieldCode;
              break;
            }
          }
          if (!fieldCode && fields.length > 0) {
            fieldCode = fields[0]?.fieldCode ?? '';
          }
        }

        const containerFieldKey = fieldCode ? `${entryKey}${fieldCode}` : entryKey;
        const fullKey = `c:${containerFieldKey}`;
        setFieldError(fullKey, `${errMsg}（第${entryNum}个条目）`, 'logic', false);

        errors.push({
          indicatorId: indicator.id,
          indicatorCode: indicator.indicatorCode,
          message: `${errMsg}（第${entryNum}个条目）`,
          errorType: 'logic',
          indicatorName: indicator.indicatorName,
          containerFieldKey,
        });
      }
    } else {
      const rules = parseLogicRule(indicator.logicRule, 1);
      if (rules.length === 0) continue;
      const results = validateJointRule(rules as any, codeValueMap, { triggerTiming: 'FILL' });
      if (results.length === 0) {
        if (indicator.id !== undefined) clearFieldError(`t:${indicator.id}`);
        continue;
      }
      const errMsg = buildLogicRuleMsg(indicator.logicRule, allIndicators, codeValueMap);
      if (indicator.id !== undefined) setFieldError(`t:${indicator.id}`, errMsg, 'logic', false);
      errors.push({ indicatorId: indicator.id, indicatorCode: indicator.indicatorCode, message: errMsg, errorType: 'logic', indicatorName: indicator.indicatorName });
    }
  }
  return errors;
}

/**
 * 失焦时校验（只校验涉及当前指标的规则）
 */
function validateLogicRuleForBlur(
  changedIndicator: DeclareIndicatorApi.Indicator,
  allIndicators: DeclareIndicatorApi.Indicator[],
  setFieldError: (key: string, message: string, type: FieldError['errorType'], dirty?: boolean) => void,
  clearFieldError: (key: string) => void,
  setDirty: (key: string) => void,
) {
  const changedCode = changedIndicator.indicatorCode;
  const codeValueMap: Record<string, any> = {};
  for (const ind of allIndicators) codeValueMap[ind.indicatorCode] = formValues[ind.indicatorCode];

  for (const indicator of allIndicators) {
    if (!indicator.logicRule?.trim()) continue;

    const involvedCodes = new Set<string>();
    for (const m of indicator.logicRule.matchAll(/\[([^\]]+)\]/g)) {
      involvedCodes.add(m[1]!.trim());
    }

    let involvesChanged = involvedCodes.has(changedCode);
    if (!involvesChanged && changedIndicator.valueType === 12) {
      for (const code of involvedCodes) {
        if (isContainerFieldShortcut(code) && code.startsWith(changedCode + '_')) {
          involvesChanged = true;
          break;
        }
      }
    }

    if (!involvesChanged) continue;

    if (indicator.id !== undefined) setDirty(`t:${indicator.id}`);

    if (isEntryBasedContainer(indicator)) {
      const entryCount = getContainerEntryCount(indicator.indicatorCode);
      if (entryCount === 0) continue;

      interface FieldErr { entryNum: number; fieldCode: string; errMsg: string }
      const fieldErrors: FieldErr[] = [];

      for (let entryNum = 1; entryNum <= entryCount; entryNum++) {
        const rules = parseLogicRule(indicator.logicRule, entryNum);
        if (rules.length === 0) continue;

        const entryValueMap = buildEntryValueMap(indicator, entryNum);
        const results = validateJointRule(rules as any, entryValueMap, { triggerTiming: 'FILL' });

        if (results.length > 0) {
          const involvedCodesRes = results[0]?.involvedIndicatorCodes || [];
          for (const code of involvedCodesRes) {
            if (code.startsWith(indicator.indicatorCode)) {
              const fieldCode = code.slice(-2);
              const errMsg = buildLogicRuleMsg(indicator.logicRule, allIndicators, entryValueMap, entryNum, indicator);
              fieldErrors.push({ entryNum, fieldCode, errMsg });
            }
          }

          if (fieldErrors.length === 0 || fieldErrors[fieldErrors.length - 1]?.entryNum !== entryNum) {
            const errMsg = buildLogicRuleMsg(indicator.logicRule, allIndicators, entryValueMap, entryNum, indicator);
            const firstFieldCode = findFirstFilledFieldCode(indicator, entryNum);
            if (firstFieldCode) {
              fieldErrors.push({ entryNum, fieldCode: firstFieldCode, errMsg });
            }
          }
        }
      }

      clearContainerLogicRuleErrors(indicator, clearFieldError);

      if (fieldErrors.length > 0) {
        const firstError = fieldErrors[0]!;
        const entryKey = `${indicator.indicatorCode}${String(firstError.entryNum).padStart(2, '0')}`;
        const fieldKey = `c:${entryKey}${firstError.fieldCode}`;
        setFieldError(fieldKey, firstError.errMsg, 'logic', false);
        setDirty(fieldKey);
      }
    } else {
      const rules = parseLogicRule(indicator.logicRule, 1);
      if (rules.length === 0) continue;

      const results = validateJointRule(rules as any, codeValueMap, { triggerTiming: 'FILL' });
      if (results.length > 0) {
        const errMsg = buildLogicRuleMsg(indicator.logicRule, allIndicators, codeValueMap);
        if (indicator.id !== undefined) setFieldError(`t:${indicator.id}`, errMsg, 'logic', false);
      } else {
        if (indicator.id !== undefined) clearFieldError(`t:${indicator.id}`);
      }
    }
  }
}

/**
 * 验证已填数据的逻辑规则（不做必填校验）
 */
function validateFilledLogicRules(
  indicatorsToValidate: DeclareIndicatorApi.Indicator[],
  setFieldError: (key: string, message: string, type: FieldError['errorType'], dirty?: boolean) => void,
  clearFieldError: (key: string) => void,
): ValidationError[] {
  const errors: ValidationError[] = [];
  const codeValueMap = buildCodeValueMap();

  for (const indicator of indicatorsToValidate) {
    if (!indicator.logicRule?.trim()) continue;

    if (isEntryBasedContainer(indicator)) {
      const entryCount = getContainerEntryCount(indicator.indicatorCode);
      if (entryCount === 0) continue;

      for (let entryNum = 1; entryNum <= entryCount; entryNum++) {
        const rules = parseLogicRule(indicator.logicRule, entryNum);
        if (rules.length === 0) continue;

        const entryValueMap = buildEntryValueMap(indicator, entryNum);
        const results = validateJointRule(rules as any, entryValueMap, { triggerTiming: 'FILL' });

        if (results.length > 0) {
          const errMsg = buildLogicRuleMsg(indicator.logicRule, indicatorsToValidate, entryValueMap, entryNum, indicator);
          const entryKey = `${indicator.indicatorCode}${String(entryNum).padStart(2, '0')}`;

          let fieldCode = '';
          if (results[0]?.involvedIndicatorCodes?.length) {
            const involvedCode = results[0].involvedIndicatorCodes.find((c) =>
              c.startsWith(indicator.indicatorCode)
            );
            if (involvedCode) {
              fieldCode = involvedCode.slice(-2);
            }
          }

          if (!fieldCode) {
            fieldCode = findFirstFilledFieldCode(indicator, entryNum) || '';
          }

          const containerFieldKey = fieldCode ? `${entryKey}${fieldCode}` : entryKey;
          const fullKey = `c:${containerFieldKey}`;
          setFieldError(fullKey, `${errMsg}（第${entryNum}个条目）`, 'logic', false);

          errors.push({
            indicatorId: indicator.id,
            indicatorCode: indicator.indicatorCode,
            message: `${errMsg}（第${entryNum}个条目）`,
            errorType: 'logic',
            indicatorName: indicator.indicatorName,
            containerFieldKey,
          });
        }
      }
    } else {
      const rules = parseLogicRule(indicator.logicRule, 1);
      if (rules.length === 0) continue;

      const results = validateJointRule(rules as any, codeValueMap, { triggerTiming: 'FILL' });

      if (results.length === 0) {
        if (indicator.id !== undefined) clearFieldError(`t:${indicator.id}`);
        continue;
      }

      const errMsg = buildLogicRuleMsg(indicator.logicRule, indicatorsToValidate, codeValueMap);
      errors.push({
        indicatorId: indicator.id,
        indicatorCode: indicator.indicatorCode,
        message: errMsg,
        containerFieldKey: undefined,
        errorType: 'logic',
        indicatorName: indicator.indicatorName,
      });
      if (indicator.id !== undefined) setFieldError(`t:${indicator.id}`, errMsg, 'logic', false);
    }
  }

  return errors;
}

// ==================== 导出 ====================

export {
  buildEntryValueMap,
  buildCodeValueMap,
  buildLogicRuleMsg,
  findFirstFilledFieldCode,
  clearContainerLogicRuleErrors,
  validateLogicRules,
  validateLogicRuleForBlur,
  validateFilledLogicRules,
};
