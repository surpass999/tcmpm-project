/**
 * useValidation - 统一错误结构 + 校验逻辑
 *
 * 替代原来分散的 7 个错误状态 + 2 个脏追踪状态：
 * - logicRuleErrors / indicatorErrors / containerFieldErrors
 * - containerFieldInstantErrors / inputTypeInstantErrors
 * - topLevelFieldDirty / containerFieldDirty
 *
 * 统一为 1 个 fieldErrors + 1 个 getFieldError 入口
 */

import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import { isEmpty, checkRange, checkPrecision, checkSelectCount, checkRequired } from '../utils/validators';
import { parseDynamicFields, generateContainerFieldKey, isFieldVisible } from '../utils/container';
import { parseExtraConfig } from '../utils/indicator';
import { parseOptions } from '../utils/options';
import type { DynamicField, ValidationError, FieldError } from '../types';
import { formValues, inputTypeValues } from './useFormValues';
import { containerValues } from './useContainerValues';
import { INPUT_VALUE_SEPARATOR, deserializeInputTypeValue } from './useFormValues';
import {
  fieldErrors,
  toTopLevelKey,
  toContainerKey,
  toInputTypeKey,
  getFieldError,
  setFieldError,
  clearFieldError,
  setDirty,
} from './useErrorKeys';

// ==================== 核心校验逻辑 ====================

/**
 * 单个容器字段校验（供即时校验和提交校验共用）
 * 消除 validateContainerFieldInstant 和 validateType12_Container 的重复
 *
 * 注意：读取 entry 中的值时使用 generateContainerFieldKey（与存储 key 一致）
 *      错误 key 使用 toContainerKey（与 UI 组件统一）
 */
export function validateSingleContainerField(
  field: DynamicField,
  fieldValue: any,
  indicator: DeclareIndicatorApi.Indicator,
  entry: any,
  allEntries: any[]
): FieldError | null {
  // 必填校验
  if (field.required && isEmpty(fieldValue)) {
    return {
      message: `${indicator.indicatorName}「${field.fieldLabel}」为必填`,
      errorType: 'required',
      dirty: true,
    };
  }

  // 非空时校验格式
  if (!isEmpty(fieldValue)) {
    // 数字类型
    if (field.fieldType === 'number') {
      const numVal = Number(fieldValue);
      if (isNaN(numVal) || !isFinite(numVal)) {
        return {
          message: `${indicator.indicatorName}「${field.fieldLabel}」：请输入有效数字`,
          errorType: 'format',
          dirty: true,
        };
      }
      const rangeErr = checkRange(numVal, field.minValue ?? null, field.maxValue ?? null);
      if (rangeErr) {
        return {
          message: `${indicator.indicatorName}「${field.fieldLabel}」：${rangeErr}`,
          errorType: 'range',
          dirty: true,
        };
      }
      const precErr = checkPrecision(numVal, field.precision);
      if (precErr) {
        return {
          message: `${indicator.indicatorName}「${field.fieldLabel}」：${precErr}`,
          errorType: 'format',
          dirty: true,
        };
      }
    }

    // 文本类型
    if (field.fieldType === 'text' || field.fieldType === 'textarea') {
      const trimmed = String(fieldValue).trim();
      if (field.fieldType === 'text' && /^-?\d+(\.\d+)?$/.test(trimmed)) {
        return {
          message: `${indicator.indicatorName}「${field.fieldLabel}」：不能输入纯数字`,
          errorType: 'format',
          dirty: true,
        };
      }
    }

    // checkbox 类型
    if (field.fieldType === 'checkbox') {
      const countErr = checkSelectCount(fieldValue, field.minSelect, field.maxSelect);
      if (countErr) {
        return {
          message: `${indicator.indicatorName}「${field.fieldLabel}」：${countErr}`,
          errorType: 'required',
          dirty: true,
        };
      }
    }

    // noRepeat 排重校验
    if (field.noRepeat && !isEmpty(fieldValue)) {
      const trimmed = String(fieldValue).trim();
      for (let j = 0; j < allEntries.length; j++) {
        if (allEntries[j] === entry) continue;
        // 读取其他条目的值时使用正确的 key 格式
        const otherFullKey = generateContainerFieldKey(indicator.indicatorCode, allEntries[j].rowKey, field.fieldCode);
        const otherVal = allEntries[j]?.[otherFullKey];
        const otherIndex = extractEntryIndex(allEntries[j].rowKey);
        if (String(otherVal ?? '').trim() === trimmed) {
          return {
            message: `${indicator.indicatorName}「${field.fieldLabel}」：该值与「第${otherIndex}个条目」重复`,
            errorType: 'range',
            dirty: true,
          };
        }
      }
    }
  }

  return null;
}

/** 从 rowKey 提取序号 */
function extractEntryIndex(rowKey: string): number {
  const match = rowKey.match(/(\d+)$/);
  return match ? parseInt(match[1]!, 10) : 1;
}

// ==================== 容器校验 ====================

/**
 * 容器整表校验（遍历所有条目）
 * 调用 validateSingleContainerField 遍历所有字段
 *
 * 注意：读取 entry 中的值时使用 generateContainerFieldKey（与存储 key 一致）
 *      错误 key 也使用 generateContainerFieldKey（与 validateContainerFieldOnBlur 统一）
 */
export function validateContainer(
  indicator: DeclareIndicatorApi.Indicator,
  entries: any[]
): ValidationError[] {
  const errors: ValidationError[] = [];
  const fields = parseDynamicFields(indicator.valueOptions);

  for (const entry of entries) {
    for (const field of fields) {
      if (!isFieldVisible(entry, indicator.indicatorCode, field, fields)) continue;
      // 使用 generateContainerFieldKey 读取 entry 中的值（与存储 key 一致）
      const storageKey = generateContainerFieldKey(indicator.indicatorCode, entry.rowKey, field.fieldCode);
      const fieldValue = entry[storageKey];
      // 使用 generateContainerFieldKey 作为错误 key（与 validateContainerFieldOnBlur 统一）
      const fullKey = storageKey;
      const err = validateSingleContainerField(field, fieldValue, indicator, entry, entries);

      if (err) {
        fieldErrors[fullKey] = err;
        errors.push({
          message: err.message,
          errorType: err.errorType,
          indicatorCode: indicator.indicatorCode,
          indicatorId: indicator.id,
          containerFieldKey: fullKey,
          fieldLabel: field.fieldLabel,
        });
      } else {
        delete fieldErrors[fullKey];
      }
    }
  }

  return errors;
}

// ==================== 顶层指标校验 ====================

/** 判断是否为自动计算指标 */
function isComputedIndicator(indicator: DeclareIndicatorApi.Indicator): boolean {
  return !!(indicator.calculationRule && indicator.calculationRule.trim());
}

/**
 * 顶层指标校验（必填 + 格式 + 范围）
 */
export function validateIndicator(indicator: DeclareIndicatorApi.Indicator): FieldError[] {
  const errors: FieldError[] = [];
  const key = toTopLevelKey(indicator.id!);
  const value = (indicator as any)._formValue;
  const isEmptyVal = isEmpty(value);

  // 必填校验
  const isRequiredAndNotComputed = indicator.isRequired && !isComputedIndicator(indicator);
  if (isRequiredAndNotComputed && isEmptyVal) {
    const err = { message: '此项为必填', errorType: 'required' as const, dirty: true };
    fieldErrors[key] = err;
    errors.push(err);
    return errors;
  }

  // 格式校验
  if (!isEmptyVal) {
    // 数字类型
    if (indicator.valueType === 1) {
      const numVal = Number(value);
      if (isNaN(numVal) || !isFinite(numVal)) {
        const err = { message: '请输入有效数字', errorType: 'format' as const, dirty: true };
        fieldErrors[key] = err;
        errors.push(err);
      } else {
        const rangeErr = checkRange(numVal, indicator.minValue ?? null, indicator.maxValue ?? null);
        if (rangeErr) {
          const err = { message: rangeErr, errorType: 'range' as const, dirty: true };
          fieldErrors[key] = err;
          errors.push(err);
        }
        const cfg = parseExtraConfig(indicator.extraConfig);
        const precErr = checkPrecision(numVal, cfg.precision);
        if (precErr) {
          const err = { message: precErr, errorType: 'format' as const, dirty: true };
          fieldErrors[key] = err;
          errors.push(err);
        }
      }
    }

    // 文本类型
    if (indicator.valueType === 2) {
      const trimmed = String(value).trim();
      if (/^-?\d+(\.\d+)?$/.test(trimmed)) {
        const err = { message: '不能输入纯数字', errorType: 'format' as const, dirty: true };
        fieldErrors[key] = err;
        errors.push(err);
      }
    }
  }

  // 清除无错误时的错误
  if (errors.length === 0) {
    delete fieldErrors[key];
  }

  return errors;
}

// ==================== 按类型校验 ====================

function validateType1_Number(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
  const errors: ValidationError[] = [];
  const { indicatorCode: code, id, isRequired, minValue, maxValue } = indicator;
  const value = formValues[code];
  const isComputed = isComputedIndicator(indicator);
  const reqErr = checkRequired(value, !!isRequired && !isComputed);
  if (reqErr) { errors.push({ indicatorId: id, indicatorCode: code, message: `${indicator.indicatorName}：${reqErr}`, errorType: 'required' }); return errors; }
  if (!isEmpty(value)) {
    const numVal = Number(value);
    if (isNaN(numVal) || !isFinite(numVal)) { errors.push({ indicatorId: id, indicatorCode: code, message: `${indicator.indicatorName}：请输入有效数字`, errorType: 'format' }); return errors; }
    const rangeErr = checkRange(numVal, minValue ?? null, maxValue ?? null);
    if (rangeErr) errors.push({ indicatorId: id, indicatorCode: code, message: `${indicator.indicatorName}：${rangeErr}`, errorType: 'range' });
    const cfg = parseExtraConfig(indicator.extraConfig);
    const precErr = checkPrecision(numVal, cfg.precision);
    if (precErr) errors.push({ indicatorId: id, indicatorCode: code, message: `${indicator.indicatorName}：${precErr}`, errorType: 'format' });
  }
  return errors;
}

function validateType2_Text(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
  const errors: ValidationError[] = [];
  const value = formValues[indicator.indicatorCode];
  const reqErr = checkRequired(value, !!indicator.isRequired);
  if (reqErr) { errors.push({ indicatorId: indicator.id, indicatorCode: indicator.indicatorCode, message: `${indicator.indicatorName}：${reqErr}`, errorType: 'required' }); return errors; }
  if (!isEmpty(value)) {
    const trimmed = String(value).trim();
    if (/^-?\d+(\.\d+)?$/.test(trimmed)) {
      errors.push({ indicatorId: indicator.id, indicatorCode: indicator.indicatorCode, message: `${indicator.indicatorName}：不能输入纯数字`, errorType: 'format' });
    }
  }
  return errors;
}

function validateType3_Boolean(_: DeclareIndicatorApi.Indicator): ValidationError[] { return []; }
function validateType4_Date(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
  const errors: ValidationError[] = [];
  const reqErr = checkRequired(formValues[indicator.indicatorCode], !!indicator.isRequired);
  if (reqErr) errors.push({ indicatorId: indicator.id, indicatorCode: indicator.indicatorCode, message: `${indicator.indicatorName}：${reqErr}`, errorType: 'required' });
  return errors;
}

/** 内联：输入型选项必填校验 */
function validateInputTypeRequired(indicator: DeclareIndicatorApi.Indicator): string | null {
  const options = parseOptions(indicator.valueOptions);
  const raw = formValues[indicator.indicatorCode];
  const values = raw ? (Array.isArray(raw) ? raw : [raw]) : [];
  for (const opt of options) {
    if (opt.inputType) {
      const isSelected = values.some(v => deserializeInputTypeValue(v).value === opt.value);
      if (isSelected) {
        const inputKey = indicator.indicatorCode + '_' + opt.value;
        const content = inputTypeValues[inputKey] || '';
        if (!content.trim()) return `请填写"${opt.label}"的补充内容`;
        const error = validateInputContentLocal(content);
        if (error) return `"${opt.label}"的补充内容：${error}`;
      }
    }
  }
  return null;
}

/** 内联：校验输入内容格式 */
function validateInputContentLocal(content: string): string | null {
  if (content.includes(INPUT_VALUE_SEPARATOR)) return `输入内容不能包含特殊符号 "∵"`;
  if (content.trim() !== '' && /^\d+$/.test(content)) return '输入内容不能是纯数字';
  return null;
}

function validateType5_RichText(indicator: DeclareIndicatorApi.Indicator): ValidationError[] { return validateType2_Text(indicator); }
function validateType6_Select(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
  const errors: ValidationError[] = [];
  const reqErr = checkRequired(formValues[indicator.indicatorCode], !!indicator.isRequired);
  if (reqErr) { errors.push({ indicatorId: indicator.id, indicatorCode: indicator.indicatorCode, message: `${indicator.indicatorName}：${reqErr}`, errorType: 'required' }); return errors; }
  const inputTypeErr = validateInputTypeRequired(indicator);
  if (inputTypeErr) errors.push({ indicatorId: indicator.id, indicatorCode: indicator.indicatorCode, message: `${indicator.indicatorName}：${inputTypeErr}`, errorType: 'required' });
  return errors;
}
function validateType7_MultiSelect(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
  const errors: ValidationError[] = [];
  const value = formValues[indicator.indicatorCode];
  const arr = Array.isArray(value) ? value : [];
  if (indicator.isRequired && arr.length === 0) errors.push({ indicatorId: indicator.id, indicatorCode: indicator.indicatorCode, message: `${indicator.indicatorName}：此项为必填`, errorType: 'required' });
  if (arr.length > 0) {
    const inputTypeErr = validateInputTypeRequired(indicator);
    if (inputTypeErr) errors.push({ indicatorId: indicator.id, indicatorCode: indicator.indicatorCode, message: `${indicator.indicatorName}：${inputTypeErr}`, errorType: 'required' });
  }
  return errors;
}
function validateType8_DateRange(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
  const errors: ValidationError[] = [];
  const reqErr = checkRequired(formValues[indicator.indicatorCode], !!indicator.isRequired);
  if (reqErr) errors.push({ indicatorId: indicator.id, indicatorCode: indicator.indicatorCode, message: `${indicator.indicatorName}：${reqErr}`, errorType: 'required' });
  return errors;
}
function validateType9_File(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
  const errors: ValidationError[] = [];
  const value = formValues[indicator.indicatorCode];
  const isFileEmpty = isEmpty(value) || value === '[]' || value === '[ ]';
  if (indicator.isRequired && isFileEmpty) errors.push({ indicatorId: indicator.id, indicatorCode: indicator.indicatorCode, message: `${indicator.indicatorName}：此项为必填`, errorType: 'required' });
  return errors;
}
function validateType10_Dept(indicator: DeclareIndicatorApi.Indicator): ValidationError[] { return validateType6_Select(indicator); }
function validateType11_User(indicator: DeclareIndicatorApi.Indicator): ValidationError[] { return validateType6_Select(indicator); }

function validateType12_Container(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
  const errors: ValidationError[] = [];
  const { indicatorCode: code, id, indicatorName } = indicator;
  const entries = containerValues[code] || [];
  const fields = parseDynamicFields(indicator.valueOptions);

  for (let i = 0; i < entries.length; i++) {
    const entry = entries[i];
    for (const field of fields) {
      if (!isFieldVisible(entry, code, field, fields)) continue;
      const fullKey = generateContainerFieldKey(code, entry.rowKey, field.fieldCode);
      const fieldValue = entry[fullKey];
      const err = validateSingleContainerField(field, fieldValue, indicator, entry, entries);
      if (err) {
        errors.push({
          indicatorId: id,
          indicatorCode: code,
          message: `${indicatorName} 第${i + 1}个条目「${field.fieldLabel}」：${err.message}`,
          containerFieldKey: fullKey,
          errorType: err.errorType,
        });
      }
    }
  }
  return errors;
}

export const validators: Record<number, (ind: DeclareIndicatorApi.Indicator) => ValidationError[]> = {
  1: validateType1_Number, 2: validateType2_Text, 3: validateType3_Boolean,
  4: validateType4_Date, 5: validateType5_RichText, 6: validateType6_Select,
  7: validateType7_MultiSelect, 8: validateType8_DateRange, 9: validateType9_File,
  10: validateType10_Dept, 11: validateType11_User, 12: validateType12_Container,
};

// ==================== 提交校验（对外接口） ====================

/**
 * 验证已填数据（不做必填校验，用于保存草稿）
 */
export function validateFilledData(
  indicators: DeclareIndicatorApi.Indicator[],
  _setFieldErrorFn: (key: string, message: string, type: FieldError['errorType'], dirty?: boolean) => void,
  _clearFieldErrorFn: (key: string) => void,
): ValidationError[] {
  const errors: ValidationError[] = [];

  // 1. 验证顶层指标
  for (const indicator of indicators) {
    if (indicator.valueType === 12) continue;
    (indicator as any)._formValue = formValues[indicator.indicatorCode];
    const errs = validateFilledIndicator(indicator);
    errors.push(...errs);
  }

  // 2. 验证容器指标
  for (const indicator of indicators) {
    if (indicator.valueType !== 12) continue;
    const entries = containerValues[indicator.indicatorCode] || [];
    const errs = validateFilledContainer(indicator, entries);
    errors.push(...errs);
  }

  return errors;
}

/** 验证已填的顶层指标（不做必填校验） */
function validateFilledIndicator(
  indicator: DeclareIndicatorApi.Indicator,
): ValidationError[] {
  const errors: ValidationError[] = [];
  const { indicatorCode: code, id, valueType } = indicator;

  if (valueType === 12) return errors;

  const value = formValues[code];
  const isEmptyVal = isEmpty(value);
  if (isEmptyVal) return errors;

  switch (valueType) {
    case 1: {
      const numVal = Number(value);
      if (isNaN(numVal) || !isFinite(numVal)) {
        errors.push({ indicatorId: id, indicatorCode: code, message: `${indicator.indicatorName}：请输入有效数字`, errorType: 'format' });
      } else {
        const rangeErr = checkRange(numVal, indicator.minValue ?? null, indicator.maxValue ?? null);
        if (rangeErr) errors.push({ indicatorId: id, indicatorCode: code, message: `${indicator.indicatorName}：${rangeErr}`, errorType: 'range' });
        const cfg = parseExtraConfig(indicator.extraConfig);
        const precErr = checkPrecision(numVal, cfg.precision);
        if (precErr) errors.push({ indicatorId: id, indicatorCode: code, message: `${indicator.indicatorName}：${precErr}`, errorType: 'format' });
      }
      break;
    }
    case 2: {
      const trimmed = String(value).trim();
      if (/^-?\d+(\.\d+)?$/.test(trimmed)) {
        errors.push({ indicatorId: id, indicatorCode: code, message: `${indicator.indicatorName}：不能输入纯数字`, errorType: 'format' });
      }
      break;
    }
  }

  return errors;
}

/** 验证已填的容器指标（不做必填校验） */
function validateFilledContainer(
  indicator: DeclareIndicatorApi.Indicator,
  entries: any[]
): ValidationError[] {
  const errors: ValidationError[] = [];
  const { indicatorCode: code, id, indicatorName } = indicator;
  const fields = parseDynamicFields(indicator.valueOptions);

  for (let i = 0; i < entries.length; i++) {
    const entry = entries[i];

    for (const field of fields) {
      if (!isFieldVisible(entry, code, field, fields)) continue;
      const fullKey = generateContainerFieldKey(code, entry.rowKey, field.fieldCode);
      const fieldValue = entry[fullKey];
      const isEmptyVal = fieldValue === undefined || fieldValue === null || fieldValue === '';

      if (isEmptyVal) continue;

      if (field.fieldType === 'number') {
        const numVal = Number(fieldValue);
        if (isNaN(numVal) || !isFinite(numVal)) {
          errors.push({ indicatorId: id, indicatorCode: code, message: `${indicatorName} 第${i + 1}条「${field.fieldLabel}」：请输入有效数字`, containerFieldKey: fullKey, errorType: 'format' });
        } else {
          const rangeErr = checkRange(numVal, field.minValue ?? undefined, field.maxValue ?? undefined);
          if (rangeErr) errors.push({ indicatorId: id, indicatorCode: code, message: `${indicatorName} 第${i + 1}条「${field.fieldLabel}」：${rangeErr}`, containerFieldKey: fullKey, errorType: 'range' });
          const precErr = checkPrecision(numVal, field.precision);
          if (precErr) errors.push({ indicatorId: id, indicatorCode: code, message: `${indicatorName} 第${i + 1}条「${field.fieldLabel}」：${precErr}`, containerFieldKey: fullKey, errorType: 'format' });
        }
      } else if (field.fieldType === 'text' || field.fieldType === 'textarea') {
        const trimmed = String(fieldValue).trim();
        if (field.fieldType === 'text' && /^-?\d+(\.\d+)?$/.test(trimmed)) {
          errors.push({ indicatorId: id, indicatorCode: code, message: `${indicatorName} 第${i + 1}条「${field.fieldLabel}」：不能输入纯数字`, containerFieldKey: fullKey, errorType: 'format' });
        }
      }
    }
  }

  return errors;
}

/**
 * 全部校验（必填 + 格式 + 范围）
 * 返回所有错误消息，用于提交时弹出
 */
export function validateAll(
  indicators: DeclareIndicatorApi.Indicator[],
  _setFieldErrorFn: (key: string, message: string, type: FieldError['errorType'], dirty?: boolean) => void,
  _clearFieldErrorFn: (key: string) => void,
): { messages: ValidationError[]; hasErrors: boolean } {
  const messages: ValidationError[] = [];

  // 1. 顶层指标校验
  for (const indicator of indicators) {
    if (indicator.valueType === 12) continue;
    (indicator as any)._formValue = formValues[indicator.indicatorCode];
    const errs = validateIndicator(indicator);
    if (errs.length > 0) {
      // 转换 FieldError[] 为 ValidationError[]
      for (const e of errs) {
        messages.push({
          message: e.message,
          errorType: e.errorType,
          indicatorId: indicator.id,
          indicatorCode: indicator.indicatorCode,
          indicatorName: indicator.indicatorName,
        });
      }
    }
  }

  // 2. 容器指标校验
  for (const indicator of indicators) {
    if (indicator.valueType !== 12) continue;
    const entries = containerValues[indicator.indicatorCode] || [];
    const errs = validateContainer(indicator, entries);
    if (errs.length > 0) {
      messages.push(...errs);
    }
  }

  return { messages, hasErrors: messages.length > 0 };
}

/**
 * 完整校验（调用校验函数 + 逻辑规则）
 */
export function validateAllWithLogicRules(
  indicators: DeclareIndicatorApi.Indicator[],
  setFieldErrorFn: (key: string, message: string, type: FieldError['errorType'], dirty?: boolean) => void,
  clearFieldErrorFn: (key: string) => void,
  validateLogicRulesFn: (
    allIndicators: DeclareIndicatorApi.Indicator[],
    setFieldError: (key: string, message: string, type: FieldError['errorType'], dirty?: boolean) => void,
    clearFieldError: (key: string) => void,
  ) => ValidationError[],
): { messages: string[]; hasErrors: boolean } {
  const messages: string[] = [];

  // 1. 顶层指标校验
  for (const indicator of indicators) {
    if (indicator.valueType === 12) continue;
    (indicator as any)._formValue = formValues[indicator.indicatorCode];
    const errs = validateIndicator(indicator);
    if (errs.length > 0) {
      messages.push(...errs.map((e) => `${indicator.indicatorName}：${e.message}`));
    }
  }

  // 2. 容器指标校验
  for (const indicator of indicators) {
    if (indicator.valueType !== 12) continue;
    const entries = containerValues[indicator.indicatorCode] || [];
    const errs = validateContainer(indicator, entries);
    if (errs.length > 0) {
      messages.push(...errs.map((e) => e.message));
    }
  }

  // 3. 逻辑规则校验
  const logicErrors = validateLogicRulesFn(indicators, setFieldErrorFn, clearFieldErrorFn);
  messages.push(...logicErrors.map((e: any) => e.message || e));

  return { messages, hasErrors: messages.length > 0 };
}

// ==================== 即时校验 ====================

/**
 * 容器字段失焦即时校验
 * 返回错误消息（用于显示），同时写入 fieldErrors
 *
 * 注意：读取 entry 中的值时使用 generateContainerFieldKey（与存储 key 一致）
 *      错误 key 也使用 generateContainerFieldKey（与 validateContainer 统一）
 */
export function validateContainerFieldOnBlur(
  entry: any,
  indicator: DeclareIndicatorApi.Indicator,
  field: DynamicField,
  allEntries: any[]
): string | null {
  // 使用 generateContainerFieldKey 读取 entry 中的值（与存储 key 一致）
  const storageKey = generateContainerFieldKey(indicator.indicatorCode, entry.rowKey, field.fieldCode);
  const fieldValue = entry[storageKey];
  // 使用 generateContainerFieldKey 作为错误 key（与 validateContainer 统一）
  const fullKey = storageKey;

  const err = validateSingleContainerField(field, fieldValue, indicator, entry, allEntries);

  if (err) {
    fieldErrors[fullKey] = err;
    return err.message;
  } else {
    delete fieldErrors[fullKey];
    return null;
  }
}

/**
 * 顶层指标即时校验
 */
export function validateTopLevelOnBlur(indicator: DeclareIndicatorApi.Indicator, value: any): string | null {
  toTopLevelKey(indicator.id!); // 初始化脏标记
  (indicator as any)._formValue = value;
  const errs = validateIndicator(indicator);
  return errs.length > 0 ? errs[0]!.message : null;
}

// ==================== 兼容旧接口 ====================

/** 获取容器字段错误（兼容旧接口，支持 entry 对象形式） */
export function getContainerFieldError(
  entryOrCode: any | string,
  indicatorCode: string,
  fieldCode: string
): string | undefined {
  const rowKey = typeof entryOrCode === 'string' ? entryOrCode : entryOrCode?.rowKey;
  if (!rowKey) return undefined;
  // 使用 generateContainerFieldKey 生成错误 key（与校验函数统一）
  const fullKey = generateContainerFieldKey(indicatorCode, String(rowKey), fieldCode);
  return getFieldError(fullKey);
}

/** 获取顶层指标错误（兼容旧接口） */
export function getTopLevelError(indicatorId: number): string | undefined {
  return getFieldError(toTopLevelKey(indicatorId));
}

/** 获取输入型选项错误（支持两种调用方式：旧版 indicator+opt 对象，新版 indicatorCode+optionValue） */
export function getInputTypeError(
  entry: any,
  indicatorCode: string,
  fieldCode?: string
): string | undefined {
  // 如果第一个参数有 rowKey，说明是旧版容器字段调用
  if (entry && typeof entry === 'object' && 'rowKey' in entry) {
    return getFieldError(toInputTypeKey(indicatorCode, fieldCode || ''));
  }
  // 标准调用：getInputTypeError(indicatorCode, optionValue)
  return getFieldError(toInputTypeKey(String(entry), indicatorCode));
}

/** 设置输入型选项错误（兼容旧接口） */
export function setInputTypeError(indicatorCode: string, optionValue: string, message: string): void {
  setFieldError(toInputTypeKey(indicatorCode, optionValue), message, 'required', true);
}

/** 清除输入型选项错误（兼容旧接口） */
export function clearInputTypeError(indicatorCode: string, optionValue: string): void {
  clearFieldError(toInputTypeKey(indicatorCode, optionValue));
}

/** 清除容器字段错误（兼容旧接口） */
export function clearContainerFieldError(indicatorCode: string, rowKey: string, fieldCode: string): void {
  const fullKey = generateContainerFieldKey(indicatorCode, rowKey, fieldCode);
  clearFieldError(fullKey);
}

/** 设置容器字段脏标记（兼容旧接口） */
export function markContainerFieldDirty(indicatorCode: string, rowKey: string, fieldCode: string): void {
  const fullKey = generateContainerFieldKey(indicatorCode, rowKey, fieldCode);
  setDirty(fullKey);
}

/** 设置顶层指标脏标记（兼容旧接口） */
export function markTopLevelDirty(indicatorId: number): void {
  setDirty(toTopLevelKey(indicatorId));
}

// ==================== 统一错误结构导出（供 index.vue 使用） ====================
// 使用 export ... from 语法，将 useErrorKeys 的符号直接转发，不产生重复导出
export { fieldErrors, toTopLevelKey, toContainerKey, getFieldError, setFieldError, clearFieldError, setDirty, clearAllErrors } from './useErrorKeys';
