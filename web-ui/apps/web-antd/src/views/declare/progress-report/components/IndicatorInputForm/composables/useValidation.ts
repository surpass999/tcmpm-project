/**
 * useValidation - 统一错误结构 + 校验逻辑
 */

import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import { isEmpty, checkRange, checkPrecision, checkSelectCount, checkRequired } from '../utils/validators';
import { parseDynamicFields, generateContainerFieldKey, isFieldVisible, getContainerType } from '../utils/container';
import { parseExtraConfig } from '../utils/indicator';
import { parseOptions } from '../utils/options';
import type { DynamicField, ValidationError, FieldError } from '../types';
import { formValues, inputTypeValues, INPUT_VALUE_SEPARATOR, deserializeInputTypeValue } from './useFormValues';
import { containerValues } from './useContainerValues';
import { fieldErrors, toTopLevelKey, getFieldError, clearFieldError, setDirty } from './useErrorKeys';
import { getIndicatorsInDisplayOrder } from './useIndicatorData';
import { evaluateLinkage } from '../utils/linkageEvaluator';

function evaluateIndicatorLinkage(indicator: DeclareIndicatorApi.Indicator, formValues: Record<string, any>) {
  return evaluateLinkage(indicator, formValues);
}

function isComputedIndicator(indicator: DeclareIndicatorApi.Indicator): boolean {
  return !!(indicator.calculationRule && indicator.calculationRule.trim());
}

export function validateSingleContainerField(
  field: DynamicField,
  fieldValue: any,
  indicator: DeclareIndicatorApi.Indicator,
  entry: any,
  allEntries: any[],
): FieldError | null {
  const storageKey = `${entry.rowKey}${field.fieldCode}`;
  if (field.required && isEmpty(fieldValue)) {
    return { message: `「${storageKey} - ${field.fieldLabel}」为必填`, errorType: 'required', dirty: true };
  }
  if (!isEmpty(fieldValue)) {
    if (field.fieldType === 'number') {
      const numVal = Number(fieldValue);
      if (isNaN(numVal) || !isFinite(numVal)) {
        return { message: `「${storageKey} - ${field.fieldLabel}」：请输入有效数字`, errorType: 'format', dirty: true };
      }
      const rangeErr = checkRange(numVal, field.minValue ?? null, field.maxValue ?? null);
      if (rangeErr) return { message: `「${storageKey} - ${field.fieldLabel}」：${rangeErr}`, errorType: 'range', dirty: true };
      const precErr = checkPrecision(numVal, field.precision);
      if (precErr) return { message: `「${storageKey} - ${field.fieldLabel}」：${precErr}`, errorType: 'format', dirty: true };
    }
    if (field.fieldType === 'text' || field.fieldType === 'textarea') {
      const trimmed = String(fieldValue).trim();
      if (field.fieldType === 'text' && /^-?\d+(\.\d+)?$/.test(trimmed)) {
        return { message: `「${storageKey} - ${field.fieldLabel}」：不能输入纯数字`, errorType: 'format', dirty: true };
      }
    }
    if (field.fieldType === 'checkbox') {
      const countErr = checkSelectCount(fieldValue, field.minSelect, field.maxSelect);
      if (countErr) return { message: `「${storageKey} - ${field.fieldLabel}」：${countErr}`, errorType: 'required', dirty: true };
    }
    if (field.noRepeat && !isEmpty(fieldValue)) {
      const trimmed = String(fieldValue).trim();
      for (let j = 0; j < allEntries.length; j++) {
        if (allEntries[j] === entry) continue;
        const otherFullKey = generateContainerFieldKey(indicator.indicatorCode, allEntries[j].rowKey, field.fieldCode);
        const otherVal = allEntries[j]?.[otherFullKey];
        if (String(otherVal ?? '').trim() === trimmed) {
          return { message: `「${storageKey} - ${field.fieldLabel}」与「${otherFullKey} - ${field.fieldLabel}」重复`, errorType: 'range', dirty: true };
        }
      }
    }
  }
  return null;
}

export function validateContainer(indicator: DeclareIndicatorApi.Indicator, entries: any[]): ValidationError[] {
  const errors: ValidationError[] = [];
  const fields = parseDynamicFields(indicator.valueOptions);
  const containerType = getContainerType(indicator.valueOptions);

  for (const entry of entries) {
    for (const field of fields) {
      if (!isFieldVisible(entry, indicator.indicatorCode, field, fields)) continue;
      const storageKey = generateContainerFieldKey(indicator.indicatorCode, entry.rowKey, field.fieldCode);
      const fieldValue = entry[storageKey];
      const fullKey = containerType === 'conditional' ? field.fieldCode : storageKey;
      const err = validateSingleContainerField(field, fieldValue, indicator, entry, entries);

      if (err) {
        fieldErrors[fullKey] = err;
        errors.push({ message: err.message, errorType: err.errorType, indicatorCode: indicator.indicatorCode, indicatorId: indicator.id, containerFieldKey: fullKey, fieldLabel: field.fieldLabel, dirty: true });
      } else {
        delete fieldErrors[fullKey];
      }
    }
  }
  return errors;
}

export function validateIndicator(indicator: DeclareIndicatorApi.Indicator): FieldError[] {
  const errors: FieldError[] = [];
  const key = toTopLevelKey(indicator.id!);

  const linkageState = evaluateIndicatorLinkage(indicator, formValues);
  if (linkageState?.type === 'show' && !linkageState.enabled) {
    clearFieldError(key, true);
    return [];
  }

  const value = formValues[indicator.indicatorCode];
  const isEmptyVal = isEmpty(value);

  let required = indicator.isRequired;
  if (linkageState?.type === 'required') required = linkageState.enabled;

  const isRequiredAndNotComputed = required && !isComputedIndicator(indicator);
  if (isRequiredAndNotComputed && isEmptyVal) {
    if (linkageState?.type === 'show') {
      clearFieldError(key, true);
      return [];
    }
    const err = { message: '此项为必填', errorType: 'required' as const, dirty: true };
    fieldErrors[key] = err;
    errors.push(err);
    return errors;
  }

  if (!isEmptyVal) {
    if (indicator.valueType === 1) {
      const numVal = Number(value);
      if (isNaN(numVal) || !isFinite(numVal)) {
        const err = { message: '请输入有效数字', errorType: 'format' as const, dirty: true };
        fieldErrors[key] = err;
        errors.push(err);
      } else {
        const rangeErr = checkRange(numVal, indicator.minValue ?? null, indicator.maxValue ?? null);
        if (rangeErr) { fieldErrors[key] = { message: rangeErr, errorType: 'range' as const, dirty: true }; errors.push(fieldErrors[key]); }
        const cfg = parseExtraConfig(indicator.extraConfig);
        const precErr = checkPrecision(numVal, cfg.precision);
        if (precErr) { fieldErrors[key] = { message: precErr, errorType: 'format' as const, dirty: true }; errors.push(fieldErrors[key]); }
      }
    }
    if (indicator.valueType === 2) {
      const trimmed = String(value).trim();
      if (/^-?\d+(\.\d+)?$/.test(trimmed)) {
        const err = { message: '不能输入纯数字', errorType: 'format' as const, dirty: true };
        fieldErrors[key] = err;
        errors.push(err);
      }
    }
    if (indicator.valueType === 6 || indicator.valueType === 7) {
      const raw = formValues[indicator.indicatorCode];
      const options = parseOptions(indicator.valueOptions);
      for (const opt of options) {
        if (opt.inputType) {
          const isSelected = indicator.valueType === 7
            ? (Array.isArray(raw) ? raw : (raw ? [raw] : [])).some((v: string) => deserializeInputTypeValue(v).value === opt.value)
            : (raw ? deserializeInputTypeValue(raw) : null)?.value === opt.value;
          if (isSelected) {
            const inputKey = indicator.indicatorCode + '_' + opt.value;
            const content = inputTypeValues[inputKey] || '';
            if (!content.trim()) {
              const err = { message: `请填写"${opt.label}"的补充内容`, errorType: 'required' as const, dirty: true };
              fieldErrors[key] = err;
              errors.push(err);
            } else if (/^\d+$/.test(content.trim())) {
              const err = { message: `"${opt.label}"的补充内容：输入内容不能是纯数字`, errorType: 'format' as const, dirty: true };
              fieldErrors[key] = err;
              errors.push(err);
            }
          }
        }
      }
    }
  }

  if (errors.length === 0) delete fieldErrors[key];
  return errors;
}

export function validateFilledData(
  _indicators: DeclareIndicatorApi.Indicator[],
  _setFieldErrorFn: any,
  _clearFieldErrorFn: any,
): ValidationError[] {
  const errors: ValidationError[] = [];
  const orderedIndicators = getIndicatorsInDisplayOrder();

  for (const indicator of orderedIndicators) {
    if (indicator.valueType === 12) {
      const entries = containerValues[indicator.indicatorCode] || [];
      const errs = validateContainer(indicator, entries);
      if (errs.length > 0) errors.push(...errs);
    } else {
      const value = formValues[indicator.indicatorCode];
      if (isEmpty(value)) continue;
      (indicator as any)._formValue = value;
      const errs = validateIndicator(indicator);
      if (errs.length > 0) errors.push(...errs.map((e) => ({ message: e.message, errorType: e.errorType, indicatorId: indicator.id, indicatorCode: indicator.indicatorCode, indicatorName: indicator.indicatorName })));
    }
  }
  return errors;
}

export function validateAll(
  _indicators: DeclareIndicatorApi.Indicator[],
  _setFieldErrorFn: any,
  _clearFieldErrorFn: any,
): { messages: ValidationError[]; hasErrors: boolean } {
  const messages: ValidationError[] = [];
  const orderedIndicators = getIndicatorsInDisplayOrder();

  for (const indicator of orderedIndicators) {
    if (indicator.valueType === 12) {
      const entries = containerValues[indicator.indicatorCode] || [];
      const errs = validateContainer(indicator, entries);
      if (errs.length > 0) messages.push(...errs);
    } else {
      (indicator as any)._formValue = formValues[indicator.indicatorCode];
      const errs = validateIndicator(indicator);
      if (errs.length > 0) messages.push(...errs.map((e) => ({ message: e.message, errorType: e.errorType, indicatorId: indicator.id, indicatorCode: indicator.indicatorCode, indicatorName: indicator.indicatorName })));
    }
  }
  return { messages, hasErrors: messages.length > 0 };
}

export function validateContainerFieldOnBlur(entry: any, indicator: DeclareIndicatorApi.Indicator, field: DynamicField, allEntries: any[]): string | null {
  const containerType = getContainerType(indicator.valueOptions);
  const storageKey = generateContainerFieldKey(indicator.indicatorCode, entry.rowKey, field.fieldCode);
  const fieldValue = entry[storageKey];
  const fullKey = containerType === 'conditional' ? field.fieldCode : storageKey;

  const err = validateSingleContainerField(field, fieldValue, indicator, entry, allEntries);
  if (err) { fieldErrors[fullKey] = err; return err.message; }
  else { delete fieldErrors[fullKey]; return null; }
}

export function getContainerFieldError(
  entryOrCode: any | string,
  _indicatorCode: string,
  fieldCode: string,
  containerType: 'normal' | 'autoEntry' | 'conditional' = 'normal',
): string | undefined {
  const rowKey = typeof entryOrCode === 'string' ? entryOrCode : entryOrCode?.rowKey;
  if (!rowKey) return undefined;
  const key = containerType === 'conditional' ? fieldCode : `${rowKey}${fieldCode}`;
  return getFieldError(key);
}

export function getTopLevelError(indicatorId: number): string | undefined {
  return getFieldError(toTopLevelKey(indicatorId));
}

export function markContainerFieldDirty(
  _indicatorCode: string,
  rowKey: string,
  fieldCode: string,
  containerType: 'normal' | 'autoEntry' | 'conditional' = 'normal',
): void {
  const key = containerType === 'conditional' ? fieldCode : `${rowKey}${fieldCode}`;
  setDirty(key);
}

export function markTopLevelDirty(indicatorId: number): void {
  setDirty(toTopLevelKey(indicatorId));
}

export { fieldErrors, toTopLevelKey, getFieldError, setFieldError, clearFieldError, setDirty, clearAllErrors };
