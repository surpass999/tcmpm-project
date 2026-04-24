/**
 * useLogicRules - 逻辑规则校验
 *
 * 职责：
 * - 逻辑规则解析（调用 engine.parseLogicRule）
 * - 逻辑规则校验（逐条目 + 顶层）
 * - 错误消息构建（调用 engine.buildRuleMessage）
 * - CC 规则执行（调用 engine.evaluateCC）
 * - FORMATS 规则执行（调用 engine.evaluateFORMATS）
 *
 * 重构后架构：
 * - 本文件（~300行）→ 集成层：Vue 上下文 + 值映射 + 错误上报
 * - engine.ts        → 验证执行引擎
 * - parser.ts        → 规则字符串解析器
 */

import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import { validate as validateJointRule, parseLogicRule, isContainerFieldShortcut, buildRuleMessage, parseFORMATS, evaluateCC, evaluateFORMATS, parseCC } from '../utils/indicator-validator';
import type { ValidationError, FieldError } from '../types';
import { formValues } from './useFormValues';
import { containerValues, isEntryBasedContainer, getContainerEntryCount } from './useContainerValues';
import { parseDynamicFields, getContainerType } from '../utils/container';
import { fieldErrors, toContainerKey } from './useErrorKeys';
import { isIndicatorVisible } from './useLinkage';

// ==================== 辅助函数 ====================

/** 判断是否为自动计算指标 */
function isComputedIndicator(indicator: DeclareIndicatorApi.Indicator): boolean {
  return !!(indicator.calculationRule && indicator.calculationRule.trim());
}

// ==================== 值映射构建 ====================

/**
 * 统一值映射构建
 * 根据指标类型分发到对应的值映射函数
 */
function buildValueMap(
  indicator: DeclareIndicatorApi.Indicator,
  entryNumber: number,
): Record<string, any> {
  const containerType = getContainerType(indicator.valueOptions);

  if (containerType === 'conditional') {
    return buildConditionalValueMap(indicator);
  }

  if (isEntryBasedContainer(indicator)) {
    return buildEntryValueMap(indicator, entryNumber);
  }

  // 顶层指标：从 formValues 构建
  const map: Record<string, any> = {};
  map[indicator.indicatorCode] = formValues[indicator.indicatorCode];
  return map;
}

/** 构建条件容器的值映射 */
function buildConditionalValueMap(indicator: DeclareIndicatorApi.Indicator): Record<string, any> {
  const map: Record<string, any> = {};
  const entries = containerValues[indicator.indicatorCode] || [];
  if (entries.length === 0) return map;

  const entry = entries[0];
  const fields = parseDynamicFields(indicator.valueOptions);
  for (const field of fields) {
    const fullKey = `${entry.rowKey}${field.fieldCode}`;
    map[fullKey] = entry[fullKey];
  }
  return map;
}

/** 构建条目容器的值映射 */
function buildEntryValueMap(
  indicator: DeclareIndicatorApi.Indicator,
  entryNumber: number,
): Record<string, any> {
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

/** 构建指标编码到值的映射（用于顶层指标验证） */
function buildTopLevelValueMap(allIndicators: DeclareIndicatorApi.Indicator[]): Record<string, any> {
  const map: Record<string, any> = {};
  for (const ind of allIndicators) {
    map[ind.indicatorCode] = formValues[ind.indicatorCode];
  }
  return map;
}

// ==================== FORMATS 规则验证 ====================

/**
 * 执行 FORMATS 规则验证
 * @returns 错误消息，或 null（通过/无需验证）
 */
function validateFORMATS(
  indicator: DeclareIndicatorApi.Indicator,
  setFieldError: (key: string, message: string, type: FieldError['errorType'], dirty?: boolean) => void,
  clearFieldError: (key: string) => void,
): string | null {
  const formatsRule = parseFORMATS(indicator.logicRule);
  if (!formatsRule) return null;

  let fileList: { name: string }[] = [];
  const rawValue = formValues[indicator.indicatorCode];
  if (rawValue && rawValue !== '[]' && rawValue !== '[ ]') {
    try {
      fileList = JSON.parse(rawValue as string);
    } catch { /* ignore */ }
  }

  if (fileList.length === 0) {
    // 空文件列表时：不清除也不报错（由必填校验兜底）
    clearFieldError(indicator.id !== undefined ? `t:${indicator.id}` : indicator.indicatorCode);
    return null;
  }

  const missing = evaluateFORMATS(formatsRule, fileList);
  if (missing.length > 0) {
    const errMsg = `${indicator.indicatorName}：必须包含以下格式的文件：${missing.join('、')}`;
    const key = indicator.id !== undefined ? `t:${indicator.id}` : indicator.indicatorCode;
    setFieldError(key, errMsg, 'logic', false);
    return errMsg;
  }

  const key = indicator.id !== undefined ? `t:${indicator.id}` : indicator.indicatorCode;
  clearFieldError(key);
  return null;
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

function clearContainerLogicRuleErrors(
  indicator: DeclareIndicatorApi.Indicator,
  clearFieldError: (key: string) => void,
) {
  const containerType = getContainerType(indicator.valueOptions);
  const entries = containerValues[indicator.indicatorCode] || [];

  const shouldClear = (key: string) => {
    const err = fieldErrors[key];
    if (!err) return true;
    return err.errorType === 'logic' || err.errorType === 'joint';
  };

  if (containerType === 'conditional') {
    const fields = parseDynamicFields(indicator.valueOptions);
    const entries2 = containerValues[indicator.indicatorCode] || [];
    const entryRowKey = entries2[0]?.rowKey ?? indicator.indicatorCode;
    for (const field of fields) {
      const fieldKey = toContainerKey(entryRowKey, field.fieldCode);
      if (shouldClear(fieldKey)) clearFieldError(fieldKey);
    }
  } else {
    for (const entry of entries) {
      const fields = parseDynamicFields(indicator.valueOptions);
      for (const field of fields) {
        const fieldKey = toContainerKey(entry.rowKey, field.fieldCode);
        if (shouldClear(fieldKey)) clearFieldError(fieldKey);
      }
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
  const codeValueMap = buildTopLevelValueMap(allIndicators);

  for (const indicator of allIndicators) {
    if (!indicator.logicRule?.trim()) continue;
    if (!isIndicatorVisible(indicator.indicatorCode)) continue;

    // 1. FORMATS 规则
    const formatsErr = validateFORMATS(indicator, setFieldError, clearFieldError);
    if (formatsErr) {
      errors.push({
        indicatorId: indicator.id,
        indicatorCode: indicator.indicatorCode,
        message: formatsErr,
        errorType: 'logic',
        indicatorName: indicator.indicatorName,
      });
      continue;
    }

    const containerType = getContainerType(indicator.valueOptions);

    if (isEntryBasedContainer(indicator) || containerType === 'conditional') {
      // 条目容器 / 条件容器
      const entryCount = containerType === 'conditional'
        ? 1
        : getContainerEntryCount(indicator.indicatorCode);
      if (entryCount === 0) continue;

      for (let entryNum = 1; entryNum <= entryCount; entryNum++) {
        const entryKey = containerType === 'conditional'
          ? indicator.indicatorCode
          : `${indicator.indicatorCode}${String(entryNum).padStart(2, '0')}`;

        // CC 规则
        const ccRule = parseCC(indicator.logicRule, containerType === 'conditional' ? 1 : entryNum);
        if (ccRule) {
          const entries2 = containerValues[indicator.indicatorCode] || [];
          const entry = entries2.find((e: any) =>
            containerType === 'conditional' ? true : e.rowKey === entryKey
          );
          if (entry) {
            const fields = parseDynamicFields(indicator.valueOptions);
            const ccErrors = evaluateCC(ccRule, entry, fields);
            if (ccErrors.length > 0) {
              setFieldError(ccErrors[0]!.fieldKey, ccErrors[0]!.errMsg, 'logic', false);
              errors.push({
                indicatorId: indicator.id,
                indicatorCode: indicator.indicatorCode,
                message: ccErrors[0]!.errMsg,
                errorType: 'logic',
                indicatorName: indicator.indicatorName,
                containerFieldKey: ccErrors[0]!.fieldKey,
              });
              return errors; // CC 错误直接返回
            }
          }
        }

        // 普通 / IF 规则
        const rules = parseLogicRule(indicator.logicRule, containerType === 'conditional' ? 1 : entryNum);
        if (rules.length === 0) continue;

        const entryValueMap = buildValueMap(indicator, containerType === 'conditional' ? 1 : entryNum);
        const results = validateJointRule(rules as any, entryValueMap, { triggerTiming: 'FILL', allIndicators });
        if (results.length === 0) continue;

        const firstFailed = results[0]!;
        const allFields = parseDynamicFields(indicator.valueOptions);

        let fieldCode = '';
        if (firstFailed.involvedIndicatorCodes?.length) {
          const involvedCode = firstFailed.involvedIndicatorCodes.find((c) =>
            c.startsWith(indicator.indicatorCode)
          );
          if (involvedCode) fieldCode = involvedCode.slice(-2);
        }

        if (!fieldCode) {
          fieldCode = allFields?.[0]?.fieldCode ?? '';
        }

        const fullKey = containerType === 'conditional' ? fieldCode : `${entryKey}${fieldCode}`;
        const errMsg = firstFailed.ruleConfig
          ? buildRuleMessage(firstFailed as any, { values: codeValueMap, allIndicators, ruleConfigStr: firstFailed.ruleConfig })
          : firstFailed.message || firstFailed.ruleName;

        setFieldError(fullKey, `「${fullKey}」：${errMsg}`, 'logic', false);
        errors.push({
          indicatorId: indicator.id,
          indicatorCode: indicator.indicatorCode,
          message: `「${fullKey}」：${errMsg}`,
          errorType: 'logic',
          indicatorName: indicator.indicatorName,
          containerFieldKey: fullKey,
        });
        return errors;
      }
    } else {
      // 顶层指标
      const rules = parseLogicRule(indicator.logicRule, 1);
      if (rules.length === 0) continue;
      const results = validateJointRule(rules as any, codeValueMap, { triggerTiming: 'FILL', allIndicators });
      if (results.length === 0) {
        if (indicator.id !== undefined && fieldErrors[`t:${indicator.id}`]?.errorType !== 'required') {
          clearFieldError(`t:${indicator.id}`);
        }
        continue;
      }

      const firstFailed = results[0]!;
      const errMsg = firstFailed.ruleConfig
        ? buildRuleMessage(firstFailed as any, { values: codeValueMap, allIndicators, ruleConfigStr: firstFailed.ruleConfig })
        : firstFailed.message || firstFailed.ruleName;

      if (indicator.id !== undefined) setFieldError(`t:${indicator.id}`, errMsg, 'logic', false);
      errors.push({
        indicatorId: indicator.id,
        indicatorCode: indicator.indicatorCode,
        message: errMsg,
        errorType: 'logic',
        indicatorName: indicator.indicatorName,
      });
    }
  }

  return errors;
}

/**
 * 失焦时校验（只校验涉及当前指标的规则）
 * 基础验证权重更高：如果基础验证（必填）没通过，则跳过逻辑验证
 * @returns 'early-return' | 'proceeded'
 */
function validateLogicRuleForBlur(
  changedIndicator: DeclareIndicatorApi.Indicator,
  allIndicators: DeclareIndicatorApi.Indicator[],
  setFieldError: (key: string, message: string, type: FieldError['errorType'], dirty?: boolean) => void,
  clearFieldError: (key: string) => void,
  setDirty: (key: string) => void,
): 'early-return' | 'proceeded' {
  const changedCode = changedIndicator.indicatorCode;

  // 基础验证检查（仅对非容器指标生效）
  if (changedIndicator.valueType !== 12 && changedIndicator.isRequired && !isComputedIndicator(changedIndicator)) {
    const value = formValues[changedIndicator.indicatorCode];
    const isEmptyVal = value === undefined || value === null || value === ''
      || (typeof value === 'string' && value.trim() === '')
      || (Array.isArray(value) && value.length === 0);
    if (isEmptyVal) return 'early-return';
  }

  const codeValueMap = buildTopLevelValueMap(allIndicators);

  for (const indicator of allIndicators) {
    if (!indicator.logicRule?.trim()) continue;
    if (!isIndicatorVisible(indicator.indicatorCode)) continue;

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

    if (!involvesChanged && changedCode !== indicator.indicatorCode) continue;

    if (indicator.id !== undefined) setDirty(`t:${indicator.id}`);

    // FORMATS 规则
    const formatsErr = validateFORMATS(indicator, setFieldError, clearFieldError);
    if (formatsErr) continue;

    const containerType = getContainerType(indicator.valueOptions);

    if (isEntryBasedContainer(indicator) || containerType === 'conditional') {
      const entryCount = containerType === 'conditional'
        ? 1
        : getContainerEntryCount(indicator.indicatorCode);
      if (entryCount === 0) continue;

      interface ContainerFieldErr { entryNum: number; fieldCode: string; errMsg: string }
      const logicFieldErrors: ContainerFieldErr[] = [];

      for (let entryNum = 1; entryNum <= entryCount; entryNum++) {
        const rules = parseLogicRule(indicator.logicRule, containerType === 'conditional' ? 1 : entryNum);
        if (rules.length === 0) continue;

        const entryValueMap = buildValueMap(indicator, containerType === 'conditional' ? 1 : entryNum);
        const results = validateJointRule(rules as any, entryValueMap, { triggerTiming: 'FILL', allIndicators });

        if (results.length > 0) {
          const involvedCodesRes = results[0]?.involvedIndicatorCodes || [];
          let hasSpecificFieldError = false;

          for (const code of involvedCodesRes) {
            if (code.startsWith(indicator.indicatorCode)) {
              hasSpecificFieldError = true;
              const fieldCode = code.slice(-2);
              const parsed = parseLogicRule(indicator.logicRule, containerType === 'conditional' ? 1 : entryNum);
              const errMsg = parsed.length > 0
                ? buildRuleMessage(parsed[0]!, { values: entryValueMap, allIndicators, entryNumber: containerType === 'conditional' ? 1 : entryNum })
                : indicator.logicRule;
              logicFieldErrors.push({ entryNum, fieldCode, errMsg });
            }
          }

          if (!hasSpecificFieldError) {
            const parsed = parseLogicRule(indicator.logicRule, containerType === 'conditional' ? 1 : entryNum);
            const errMsg = parsed.length > 0
              ? buildRuleMessage(parsed[0]!, { values: entryValueMap, allIndicators, entryNumber: containerType === 'conditional' ? 1 : entryNum })
              : indicator.logicRule;
            const firstFieldCode = findFirstFilledFieldCode(indicator, entryNum);
            if (firstFieldCode) {
              logicFieldErrors.push({ entryNum, fieldCode: firstFieldCode, errMsg });
            }
          }
        }
      }

      clearContainerLogicRuleErrors(indicator, clearFieldError);

      if (logicFieldErrors.length > 0) {
        const firstError = logicFieldErrors[0]!;
        let errorKey: string;
        if (containerType === 'conditional') {
          errorKey = firstError.fieldCode;
        } else {
          const entryKey = `${indicator.indicatorCode}${String(firstError.entryNum).padStart(2, '0')}`;
          errorKey = `${entryKey}${firstError.fieldCode}`;
        }
        setFieldError(errorKey, firstError.errMsg, 'logic', false);
        setDirty(errorKey);
      }
    } else {
      // 顶层指标
      const rules = parseLogicRule(indicator.logicRule, 1);
      if (rules.length === 0) continue;

      const involvedCodes: string[] = [];
      for (const m of indicator.logicRule.matchAll(/\[([^\]]+)\]/g)) {
        involvedCodes.push(m[1]!.trim());
      }
      const allRefFieldsFilled = involvedCodes.every((code) => {
        const val = formValues[code];
        return val !== undefined && val !== null && val !== '';
      });

      const results = validateJointRule(rules as any, codeValueMap, { triggerTiming: 'FILL', allIndicators });

      if (results.length > 0) {
        const firstFailed = results[0]!;
        const parsed = parseLogicRule(firstFailed.ruleName, 1);
        const errMsg = parsed.length > 0
          ? buildRuleMessage(parsed[0]!, { values: codeValueMap, allIndicators, ruleConfigStr: firstFailed.ruleConfig })
          : firstFailed.message || indicator.logicRule;
        const targetCode = firstFailed.indicatorCode || indicator.indicatorCode;
        const targetIndicator = allIndicators.find((ind) => ind.indicatorCode === targetCode);
        if (targetIndicator?.id !== undefined) {
          setFieldError(`t:${targetIndicator.id}`, errMsg, 'logic', false);
        }
      } else if (allRefFieldsFilled) {
        const firstRule = (rules as any[])[0];
        const verifyCode = firstRule?.verifyIndicatorCode || indicator.indicatorCode;
        const verifyIndicator = allIndicators.find((ind) => ind.indicatorCode === verifyCode);
        if (verifyIndicator?.id !== undefined && fieldErrors[`t:${verifyIndicator.id}`]?.errorType !== 'required') {
          clearFieldError(`t:${verifyIndicator.id}`);
        }
      }
    }
  }

  return 'proceeded';
}

/**
 * 验证已填数据的逻辑规则（不做必填校验）
 */
function validateFilledLogicRules(
  indicatorsToValidate: DeclareIndicatorApi.Indicator[],
  allIndicators: DeclareIndicatorApi.Indicator[],
  setFieldError: (key: string, message: string, type: FieldError['errorType'], dirty?: boolean) => void,
  clearFieldError: (key: string) => void,
): ValidationError[] {
  const errors: ValidationError[] = [];
  const codeValueMap = buildTopLevelValueMap(allIndicators);

  for (const indicator of indicatorsToValidate) {
    if (!indicator.logicRule?.trim()) continue;
    if (!isIndicatorVisible(indicator.indicatorCode)) continue;

    // FORMATS 规则
    const formatsErr = validateFORMATS(indicator, setFieldError, clearFieldError);
    if (formatsErr) {
      errors.push({
        indicatorId: indicator.id,
        indicatorCode: indicator.indicatorCode,
        message: formatsErr,
        errorType: 'logic',
        indicatorName: indicator.indicatorName,
      });
      continue;
    }

    if (isEntryBasedContainer(indicator)) {
      const entryCount = getContainerEntryCount(indicator.indicatorCode);
      if (entryCount === 0) continue;

      for (let entryNum = 1; entryNum <= entryCount; entryNum++) {
        const rules = parseLogicRule(indicator.logicRule, entryNum);
        if (rules.length === 0) continue;

        const entryValueMap = buildEntryValueMap(indicator, entryNum);
        const results = validateJointRule(rules as any, entryValueMap, { triggerTiming: 'FILL', allIndicators });

        if (results.length > 0) {
          const firstFailed = results[0]!;
          const parsed = parseLogicRule(firstFailed.ruleName, 1);
          const errMsg = parsed.length > 0
            ? buildRuleMessage(parsed[0]!, { values: entryValueMap, allIndicators, entryNumber: entryNum, ruleConfigStr: firstFailed.ruleConfig })
            : firstFailed.message || firstFailed.ruleName;

          let fieldCode = '';
          if (firstFailed.involvedIndicatorCodes?.length) {
            const involvedCode = firstFailed.involvedIndicatorCodes.find((c) =>
              c.startsWith(indicator.indicatorCode)
            );
            if (involvedCode) fieldCode = involvedCode.slice(-2);
          }
          if (!fieldCode) fieldCode = findFirstFilledFieldCode(indicator, entryNum) || '';

          const entryKey = `${indicator.indicatorCode}${String(entryNum).padStart(2, '0')}`;
          const containerFieldKey = fieldCode ? toContainerKey(entryKey, fieldCode) : entryKey;
          setFieldError(containerFieldKey, `「${containerFieldKey}」：${errMsg}`, 'logic', false);

          errors.push({
            indicatorId: indicator.id,
            indicatorCode: indicator.indicatorCode,
            message: `「${containerFieldKey}」：${errMsg}`,
            errorType: 'logic',
            indicatorName: indicator.indicatorName,
            containerFieldKey,
          });
          return errors;
        }
      }
    } else {
      const rules = parseLogicRule(indicator.logicRule, 1);
      if (rules.length === 0) continue;

      const involvedCodes: string[] = [];
      for (const m of indicator.logicRule.matchAll(/\[([^\]]+)\]/g)) {
        involvedCodes.push(m[1]!.trim());
      }
      const allRefFieldsFilled = involvedCodes.every((code) => {
        const val = formValues[code];
        return val !== undefined && val !== null && val !== '';
      });

      if (!allRefFieldsFilled) continue;

      const results = validateJointRule(rules as any, codeValueMap, { triggerTiming: 'FILL', allIndicators });

      if (results.length > 0) {
        const firstFailed = results[0]!;
        const parsed = parseLogicRule(firstFailed.ruleName, 1);
        const errMsg = parsed.length > 0
          ? buildRuleMessage(parsed[0]!, { values: codeValueMap, allIndicators, ruleConfigStr: firstFailed.ruleConfig })
          : firstFailed.message || firstFailed.ruleName;

        errors.push({
          indicatorId: indicator.id,
          indicatorCode: indicator.indicatorCode,
          message: errMsg,
          errorType: 'logic',
          indicatorName: indicator.indicatorName,
        });
        if (indicator.id !== undefined) setFieldError(`t:${indicator.id}`, errMsg, 'logic', false);
      } else {
        if (indicator.id !== undefined && fieldErrors[`t:${indicator.id}`]?.errorType !== 'required') {
          clearFieldError(`t:${indicator.id}`);
        }
      }
    }
  }

  return errors;
}

/**
 * 单行容器逻辑校验（用于容器字段 blur 时的即时校验）
 * @returns 错误信息，无错返回 null
 */
function validateSingleContainerLogicRule(
  indicator: DeclareIndicatorApi.Indicator,
  entryNum: number,
  entryKey: string,
  allIndicators: DeclareIndicatorApi.Indicator[],
): { fieldKey: string; errMsg: string } | null {
  const containerType = getContainerType(indicator.valueOptions);
  const rules = parseLogicRule(indicator.logicRule, containerType === 'conditional' ? 1 : entryNum);
  if (rules.length === 0) return null;

  const entryValueMap = buildValueMap(indicator, containerType === 'conditional' ? 1 : entryNum);
  const results = validateJointRule(rules as any, entryValueMap, { triggerTiming: 'FILL', allIndicators });
  if (results.length === 0) return null;

  const firstFailed = results[0]!;
  const parsed = parseLogicRule(firstFailed.ruleName, containerType === 'conditional' ? 1 : entryNum);
  const errMsg = parsed.length > 0
    ? buildRuleMessage(parsed[0]!, { values: entryValueMap, allIndicators, entryNumber: containerType === 'conditional' ? 1 : entryNum, ruleConfigStr: firstFailed.ruleConfig })
    : indicator.logicRule;

  const involvedCodes = firstFailed.involvedIndicatorCodes || [];
  let fieldCode = '';

  for (const code of involvedCodes) {
    if (code.startsWith(indicator.indicatorCode)) {
      fieldCode = code.slice(-2);
      break;
    }
  }

  if (!fieldCode) {
    const firstFilled = findFirstFilledFieldCode(indicator, entryNum);
    if (firstFilled) fieldCode = firstFilled;
  }

  const fieldKey = containerType === 'conditional' ? fieldCode : toContainerKey(entryKey, fieldCode);
  return { fieldKey, errMsg };
}

// ==================== 导出 ====================

export {
  buildEntryValueMap,
  buildTopLevelValueMap,
  findFirstFilledFieldCode,
  clearContainerLogicRuleErrors,
  validateSingleContainerLogicRule,
  validateLogicRules,
  validateLogicRuleForBlur,
  validateFilledLogicRules,
};
