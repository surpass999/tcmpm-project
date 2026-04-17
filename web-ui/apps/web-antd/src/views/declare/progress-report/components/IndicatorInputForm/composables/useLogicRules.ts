/**
 * useLogicRules - 逻辑规则校验
 */

import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import { validate as validateJointRule, parseLogicRule, isContainerFieldShortcut, parseContainerFieldShortcut } from '#/utils/indicatorValidator';
import type { ValidationError, FieldError } from '../types';
import { formValues } from './useFormValues';
import { containerValues, isEntryBasedContainer, getContainerEntryCount } from './useContainerValues';
import { parseDynamicFields, getContainerType } from '../utils/container';
import { fieldErrors } from './useErrorKeys';

function isComputedIndicator(indicator: DeclareIndicatorApi.Indicator): boolean {
  return !!(indicator.calculationRule && indicator.calculationRule.trim());
}

function buildEntryValueMap(indicator: DeclareIndicatorApi.Indicator, entryNumber: number): Record<string, any> {
  const map: Record<string, any> = {};
  const containerCode = indicator.indicatorCode;
  const entryKey = `${containerCode}${String(entryNumber).padStart(2, '0')}`;
  const entries = containerValues[containerCode] || [];
  const entry = entries.find((e: any) => e.rowKey === entryKey);
  if (entry) {
    const fields = parseDynamicFields(indicator.valueOptions) || [];
    for (const field of fields) {
      const fullKey = `${entryKey}${field.fieldCode}`;
      map[fullKey] = entry[fullKey];
    }
  }
  return map;
}

function buildEntryValueMapForConditional(indicator: DeclareIndicatorApi.Indicator): Record<string, any> {
  const map: Record<string, any> = {};
  const entries = containerValues[indicator.indicatorCode] || [];
  if (entries.length === 0) return map;
  const entry = entries[0];
  const fields = parseDynamicFields(indicator.valueOptions);
  for (const field of fields) {
    map[field.fieldCode] = entry[field.fieldCode];
  }
  return map;
}

function buildCodeValueMap(): Record<string, any> {
  const map: Record<string, any> = {};
  for (const [code, value] of Object.entries(formValues)) map[code] = value;
  return map;
}

function buildLogicRuleMsg(logicRule: string, allIndicators: DeclareIndicatorApi.Indicator[], codeValueMap: Record<string, any>, entryNumber?: number, containerIndicator?: DeclareIndicatorApi.Indicator): string {
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
        const option = (field.options || []).find((o) => o.value === String(val) || o.value === val);
        valueText = option ? option.label : String(val);
      } else {
        valueText = String(val);
      }
    }
    return { label: field.fieldLabel, valueText };
  };

  const getFieldAndThresholdText = (fieldCode: string, threshold: string): { fieldLabel: string; thresholdText: string } => {
    let fieldLabel = fieldCode;
    let thresholdText = threshold;
    if (containerIndicator) {
      const fields = parseDynamicFields(containerIndicator.valueOptions);
      const shortCode = fieldCode.slice(-2);
      const field = fields.find((f) => f.fieldCode === shortCode || f.fieldCode === fieldCode);
      if (field) {
        fieldLabel = field.fieldLabel;
        if (field.fieldType === 'radio' || field.fieldType === 'select' || field.fieldType === 'checkbox') {
          const option = (field.options || []).find((o) => o.value === threshold || o.value === String(threshold));
          if (option) thresholdText = `"${option.label}"`;
        }
      }
    } else {
      const name = codeMap.get(fieldCode);
      fieldLabel = name ? `${fieldCode} - ${name}` : fieldCode;
    }
    return { fieldLabel, thresholdText };
  };

  const replaceCode = (code: string): string => {
    let fullKey = code;
    if (isContainerFieldShortcut(code) && entryNumber !== undefined) {
      const parsed = parseContainerFieldShortcut(code, entryNumber);
      if (parsed) fullKey = parsed;
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

  const ifMultiMatch = logicRule.trim().match(/^IF\s*\(\s*\[([^\]]+)\]\s*(==|!=|<=|>=|<|>)\s*(\d+(?:\.\d+)?)\s*,\s*(.+)\s*,\s*TRUE\s*\)$/i);
  if (ifMultiMatch) {
    const condCode = ifMultiMatch[1]!.trim();
    const condOp = ifMultiMatch[2]!;
    const condVal = ifMultiMatch[3]!;
    const verifyPart = ifMultiMatch[4]!;
    const condOpText: Record<string, string> = { '==': '等于', '!=': '不等于', '>': '大于', '>=': '大于等于', '<': '小于', '<=': '小于等于' };
    const opText: Record<string, string> = { '>=': '应大于等于', '<=': '应小于等于', '>': '应大于', '<': '应小于', '==': '应等于', '!=': '不应等于' };
    const { fieldLabel: condFieldLabel, thresholdText: condThresholdText } = getFieldAndThresholdText(condCode, condVal);
    const verifyExprs = verifyPart.split(/\s*,\s*(?=\[)/);
    const verifyMsgs = verifyExprs.map((expr) => {
      const vm = expr.trim().match(/^\[([^\]]+)\]\s*(==|!=|<=|>=|<|>)\s*(\d+(?:\.\d+)?)$/);
      if (!vm) return expr.trim();
      const fieldCode = vm[1]!.trim();
      const { fieldLabel, thresholdText } = getFieldAndThresholdText(fieldCode, vm[3]!);
      return `${fieldLabel} ${opText[vm[2]!] || vm[2]} ${thresholdText}`;
    });
    return `当 ${condFieldLabel} ${condOpText[condOp] || condOp} ${condThresholdText} 时, ${verifyMsgs.join('; ')}`;
  }

  const ifMatch = logicRule.trim().match(/^IF\s*\(\s*\[([^\]]+)\]\s*(==|!=|<=|>=|<|>)\s*(\d+(?:\.\d+)?)\s*,\s*\[([^\]]+)\]\s*(==|!=|<=|>=|<|>)\s*(\d+(?:\.\d+)?)\s*,\s*TRUE\s*\)$/i);
  if (ifMatch) {
    const condCode = ifMatch[1]!.trim();
    const condOp = ifMatch[2]!;
    const condVal = ifMatch[3]!;
    const verifyCode = ifMatch[4]!.trim();
    const verifyOp = ifMatch[5]!;
    const verifyVal = ifMatch[6]!;
    const condOpText: Record<string, string> = { '==': '等于', '!=': '不等于', '>': '大于', '>=': '大于等于', '<': '小于', '<=': '小于等于' };
    const opText: Record<string, string> = { '>=': '应大于等于', '<=': '应小于等于', '>': '应大于', '<': '应小于' };
    const { fieldLabel: condFieldLabel, thresholdText: condThresholdText } = getFieldAndThresholdText(condCode, condVal);
    const { fieldLabel: verifyFieldLabel, thresholdText: verifyThresholdText } = getFieldAndThresholdText(verifyCode, verifyVal);
    return `当 ${condFieldLabel} ${condOpText[condOp] || condOp} ${condThresholdText} 时, ${verifyFieldLabel} ${opText[verifyOp] || verifyOp} ${verifyThresholdText}`;
  }

  const match = logicRule.trim().match(/^(.+?)\s*(>=|<=|>|<|==|!=)\s*(.+)$/);
  if (!match) return '校验失败';
  const leftRaw = match[1]!.trim();
  const operator = match[2]!;
  const rightRaw = match[3]!.trim();
  const opText: Record<string, string> = { '>=': '应大于等于', '<=': '应小于等于', '>': '应大于', '<': '应小于', '==': '应等于', '!=': '不应等于' };
  const msgLeft = leftRaw.replace(/\[([^\]]+)\]/g, (_, c) => replaceCode(c.trim()));
  const msgRight = rightRaw.replace(/\[([^\]]+)\]/g, (_, c) => replaceCode(c.trim()));
  return `「${msgLeft}」 ${opText[operator] || operator} 「${msgRight}」`;
}

function findFirstFilledFieldCode(indicator: DeclareIndicatorApi.Indicator, entryNumber: number): string | undefined {
  const entryKey = `${indicator.indicatorCode}${String(entryNumber).padStart(2, '0')}`;
  const entries = containerValues[indicator.indicatorCode] || [];
  const entry = entries.find((e: any) => e.rowKey === entryKey);
  if (!entry) return undefined;
  const fields = parseDynamicFields(indicator.valueOptions);
  for (const field of fields) {
    const fullKey = `${entryKey}${field.fieldCode}`;
    const value = entry[fullKey];
    if (value !== undefined && value !== null && value !== '') return field.fieldCode;
  }
  return fields[0]?.fieldCode;
}

function clearContainerLogicRuleErrors(indicator: DeclareIndicatorApi.Indicator, clearFieldError: (key: string) => void) {
  const containerType = getContainerType(indicator.valueOptions);
  const entries = containerValues[indicator.indicatorCode] || [];
  const shouldClear = (key: string) => {
    const err = fieldErrors[key];
    if (!err) return true;
    return err.errorType === 'logic' || err.errorType === 'joint';
  };
  if (containerType === 'conditional') {
    const fields = parseDynamicFields(indicator.valueOptions);
    for (const field of fields) {
      if (shouldClear(field.fieldCode)) clearFieldError(field.fieldCode);
    }
  } else {
    for (const entry of entries) {
      const fields = parseDynamicFields(indicator.valueOptions);
      for (const field of fields) {
        const fieldKey = `${entry.rowKey}${field.fieldCode}`;
        if (shouldClear(fieldKey)) clearFieldError(fieldKey);
      }
    }
  }
}

export function validateLogicRules(
  allIndicators: DeclareIndicatorApi.Indicator[],
  setFieldError: (key: string, message: string, type: FieldError['errorType'], dirty?: boolean) => void,
  clearFieldError: (key: string) => void,
): ValidationError[] {
  const errors: ValidationError[] = [];
  const codeValueMap: Record<string, any> = {};
  for (const ind of allIndicators) codeValueMap[ind.indicatorCode] = formValues[ind.indicatorCode];

  for (const indicator of allIndicators) {
    if (!indicator.logicRule?.trim()) continue;

    const containerType = getContainerType(indicator.valueOptions);
    if (isEntryBasedContainer(indicator) || containerType === 'conditional') {
      const entryCount = containerType === 'conditional' ? 1 : getContainerEntryCount(indicator.indicatorCode);
      if (entryCount === 0) continue;

      for (let entryNum = 1; entryNum <= entryCount; entryNum++) {
        const entryKey = containerType === 'conditional' ? indicator.indicatorCode : `${indicator.indicatorCode}${String(entryNum).padStart(2, '0')}`;
        const rules = parseLogicRule(indicator.logicRule, containerType === 'conditional' ? 1 : entryNum);
        if (rules.length === 0) continue;

        const entryValueMap = containerType === 'conditional' ? buildEntryValueMapForConditional(indicator) : buildEntryValueMap(indicator, entryNum);
        const results = validateJointRule(rules as any, entryValueMap, { triggerTiming: 'FILL' });
        if (results.length === 0) continue;

        const errMsg = buildLogicRuleMsg(indicator.logicRule, allIndicators, entryValueMap, containerType === 'conditional' ? 1 : entryNum, indicator);

        let fieldCode = '';
        if (results[0]?.involvedIndicatorCodes?.length) {
          const involvedCode = results[0].involvedIndicatorCodes.find((c) => c.startsWith(indicator.indicatorCode));
          if (involvedCode) fieldCode = involvedCode.slice(-2);
        }
        const allFields = parseDynamicFields(indicator.valueOptions);
        if (!fieldCode && containerType !== 'conditional') {
          for (const field of allFields) {
            const fullKey = `${entryKey}${field.fieldCode}`;
            const val = entryValueMap[fullKey];
            if (val !== undefined && val !== null && val !== '') { fieldCode = field.fieldCode; break; }
          }
          if (!fieldCode && allFields.length > 0) fieldCode = allFields[0]?.fieldCode ?? '';
        }

        let finalFieldCode = fieldCode;
        if (!finalFieldCode) finalFieldCode = allFields?.[0]?.fieldCode ?? '';
        const fullKey = containerType === 'conditional' ? finalFieldCode : `${entryKey}${finalFieldCode}`;
        const errMsgWithKey = `「${fullKey}」：${errMsg}`;
        setFieldError(fullKey, errMsgWithKey, 'logic', false);
        errors.push({ indicatorId: indicator.id, indicatorCode: indicator.indicatorCode, message: errMsgWithKey, errorType: 'logic', indicatorName: indicator.indicatorName, containerFieldKey: fullKey });
      }
    } else {
      const rules = parseLogicRule(indicator.logicRule, 1);
      if (rules.length === 0) continue;
      const results = validateJointRule(rules as any, codeValueMap, { triggerTiming: 'FILL' });
      if (results.length === 0) {
        if (indicator.id !== undefined && fieldErrors[`t:${indicator.id}`]?.errorType !== 'required') clearFieldError(`t:${indicator.id}`);
        continue;
      }
      const errMsg = buildLogicRuleMsg(indicator.logicRule, allIndicators, codeValueMap);
      if (indicator.id !== undefined) setFieldError(`t:${indicator.id}`, errMsg, 'logic', false);
      errors.push({ indicatorId: indicator.id, indicatorCode: indicator.indicatorCode, message: errMsg, errorType: 'logic', indicatorName: indicator.indicatorName });
    }
  }
  return errors;
}

export function validateLogicRuleForBlur(
  changedIndicator: DeclareIndicatorApi.Indicator,
  allIndicators: DeclareIndicatorApi.Indicator[],
  setFieldError: (key: string, message: string, type: FieldError['errorType'], dirty?: boolean) => void,
  clearFieldError: (key: string) => void,
  setDirty: (key: string) => void,
) {
  const changedCode = changedIndicator.indicatorCode;

  if (changedIndicator.valueType !== 12 && changedIndicator.isRequired && !isComputedIndicator(changedIndicator)) {
    const value = formValues[changedIndicator.indicatorCode];
    const isEmptyVal = value === undefined || value === null || value === '' || (typeof value === 'string' && value.trim() === '') || (Array.isArray(value) && value.length === 0);
    if (isEmptyVal) return;
  }

  const codeValueMap: Record<string, any> = {};
  for (const ind of allIndicators) codeValueMap[ind.indicatorCode] = formValues[ind.indicatorCode];

  for (const indicator of allIndicators) {
    if (!indicator.logicRule?.trim()) continue;

    const involvedCodes = new Set<string>();
    for (const m of indicator.logicRule.matchAll(/\[([^\]]+)\]/g)) involvedCodes.add(m[1]!.trim());

    let involvesChanged = involvedCodes.has(changedCode);
    if (!involvesChanged && changedIndicator.valueType === 12) {
      for (const code of involvedCodes) {
        if (isContainerFieldShortcut(code) && code.startsWith(changedCode + '_')) { involvesChanged = true; break; }
      }
    }
    if (!involvesChanged) continue;

    if (indicator.id !== undefined) setDirty(`t:${indicator.id}`);

    const containerType = getContainerType(indicator.valueOptions);
    if (isEntryBasedContainer(indicator) || containerType === 'conditional') {
      const entryCount = containerType === 'conditional' ? 1 : getContainerEntryCount(indicator.indicatorCode);
      if (entryCount === 0) continue;

      interface ContainerFieldErr { entryNum: number; fieldCode: string; errMsg: string }
      const logicFieldErrors: ContainerFieldErr[] = [];

      for (let entryNum = 1; entryNum <= entryCount; entryNum++) {
        const rules = parseLogicRule(indicator.logicRule, containerType === 'conditional' ? 1 : entryNum);
        if (rules.length === 0) continue;

        const entryValueMap = containerType === 'conditional' ? buildEntryValueMapForConditional(indicator) : buildEntryValueMap(indicator, entryNum);
        const results = validateJointRule(rules as any, entryValueMap, { triggerTiming: 'FILL' });

        if (results.length > 0) {
          const involvedCodesRes = results[0]?.involvedIndicatorCodes || [];
          let hasSpecificFieldError = false;
          for (const code of involvedCodesRes) {
            if (code.startsWith(indicator.indicatorCode)) {
              hasSpecificFieldError = true;
              const fieldCode = code.slice(-2);
              const errMsg = buildLogicRuleMsg(indicator.logicRule, allIndicators, entryValueMap, containerType === 'conditional' ? 1 : entryNum, indicator);
              logicFieldErrors.push({ entryNum, fieldCode, errMsg });
            }
          }
          if (!hasSpecificFieldError) {
            const errMsg = buildLogicRuleMsg(indicator.logicRule, allIndicators, entryValueMap, containerType === 'conditional' ? 1 : entryNum, indicator);
            const firstFieldCode = findFirstFilledFieldCode(indicator, entryNum);
            if (firstFieldCode) logicFieldErrors.push({ entryNum, fieldCode: firstFieldCode, errMsg });
          }
        }
      }

      clearContainerLogicRuleErrors(indicator, clearFieldError);
      if (logicFieldErrors.length > 0) {
        const firstError = logicFieldErrors[0]!;
        const errorKey = containerType === 'conditional' ? firstError.fieldCode : `${indicator.indicatorCode}${String(firstError.entryNum).padStart(2, '0')}${firstError.fieldCode}`;
        setFieldError(errorKey, firstError.errMsg, 'logic', false);
        setDirty(errorKey);
      }
    } else {
      const rules = parseLogicRule(indicator.logicRule, 1);
      if (rules.length === 0) continue;
      const results = validateJointRule(rules as any, codeValueMap, { triggerTiming: 'FILL' });
      if (results.length > 0) {
        const errMsg = buildLogicRuleMsg(indicator.logicRule, allIndicators, codeValueMap);
        if (indicator.id !== undefined) setFieldError(`t:${indicator.id}`, errMsg, 'logic', false);
      } else {
        if (indicator.id !== undefined && fieldErrors[`t:${indicator.id}`]?.errorType !== 'required') clearFieldError(`t:${indicator.id}`);
      }
    }
  }
}

export function validateFilledLogicRules(
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
            const involvedCode = results[0].involvedIndicatorCodes.find((c) => c.startsWith(indicator.indicatorCode));
            if (involvedCode) fieldCode = involvedCode.slice(-2);
          }
          if (!fieldCode) fieldCode = findFirstFilledFieldCode(indicator, entryNum) || '';

          const containerFieldKey = fieldCode ? `${entryKey}${fieldCode}` : entryKey;
          const fullKey = `c:${indicator.indicatorCode}:${containerFieldKey}`;
          const errMsgWithKey = `「${containerFieldKey}」：${errMsg}`;
          setFieldError(fullKey, errMsgWithKey, 'logic', false);
          errors.push({ indicatorId: indicator.id, indicatorCode: indicator.indicatorCode, message: errMsgWithKey, errorType: 'logic', indicatorName: indicator.indicatorName, containerFieldKey });
        }
      }
    } else {
      const rules = parseLogicRule(indicator.logicRule, 1);
      if (rules.length === 0) continue;
      const results = validateJointRule(rules as any, codeValueMap, { triggerTiming: 'FILL' });
      if (results.length === 0) {
        if (indicator.id !== undefined && fieldErrors[`t:${indicator.id}`]?.errorType !== 'required') clearFieldError(`t:${indicator.id}`);
        continue;
      }
      const errMsg = buildLogicRuleMsg(indicator.logicRule, indicatorsToValidate, codeValueMap);
      errors.push({ indicatorId: indicator.id, indicatorCode: indicator.indicatorCode, message: errMsg, errorType: 'logic', indicatorName: indicator.indicatorName });
      if (indicator.id !== undefined) setFieldError(`t:${indicator.id}`, errMsg, 'logic', false);
    }
  }
  return errors;
}
