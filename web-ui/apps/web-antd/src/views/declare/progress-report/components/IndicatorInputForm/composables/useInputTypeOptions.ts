/**
 * useInputTypeOptions - 输入型选项逻辑
 */

import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import { parseOptions } from '../utils/options';
import { toTopLevelKey, setFieldError, clearFieldError } from './useErrorKeys';
import {
  formValues, inputTypeValues, serializeInputTypeValue,
  deserializeInputTypeValue, validateInputContent,
  getInputTypeInputFieldName, isInputTypeOptionSelected,
} from './useFormValues';
import { lastPeriodRawValues } from './useIndicatorData';

let onMultiSelectChangeCallback: ((indicator: DeclareIndicatorApi.Indicator) => void) | null = null;
let onIndicatorChangeCallback: ((indicator: DeclareIndicatorApi.Indicator) => void) | null = null;

export function setOnMultiSelectChangeCallback(cb: (indicator: DeclareIndicatorApi.Indicator) => void) { onMultiSelectChangeCallback = cb; }
export function setOnIndicatorChangeCallback(cb: (indicator: DeclareIndicatorApi.Indicator) => void) { onIndicatorChangeCallback = cb; }

export function getInputTypeLastPeriodContent(lastPeriodRawValues: Record<string, string>, indicatorCode: string, optionValue: string): string {
  const raw = lastPeriodRawValues[indicatorCode];
  if (!raw) return '';
  try {
    return parseInputTypeLastPeriodRaw(raw, 6)[optionValue] || '';
  } catch { return ''; }
}

function parseInputTypeLastPeriodRaw(raw: string, valueType: number): Record<string, string> {
  const result: Record<string, string> = {};
  if (!raw) return result;
  const parts = valueType === 7 ? raw.split(',') : [raw];
  for (const part of parts) {
    const idx = part.indexOf('∵');
    if (idx !== -1) result[part.substring(0, idx)] = part.substring(idx + 1);
  }
  return result;
}

export function onInputTypeChange(indicator: DeclareIndicatorApi.Indicator, opt: any, content: string) {
  const inputKey = indicator.indicatorCode + '_' + opt.value;
  inputTypeValues[inputKey] = content;
  const raw = formValues[indicator.indicatorCode];
  if (raw) {
    const deserialized = deserializeInputTypeValue(raw);
    formValues[indicator.indicatorCode] = serializeInputTypeValue(deserialized.value, content);
  }
}

export function handleInputTypeRadioChange(indicator: DeclareIndicatorApi.Indicator, val: string) {
  const options = parseOptions(indicator.valueOptions);
  options.forEach((opt) => {
    const inputFieldName = getInputTypeInputFieldName(indicator.indicatorCode, opt.value);
    const inputKey = indicator.indicatorCode + '_' + opt.value;
    if (opt.value === val) {
      const inputContent = inputTypeValues[inputKey] || '';
      inputTypeValues[inputKey] = inputContent;
      formValues[indicator.indicatorCode] = serializeInputTypeValue(val, inputContent);
      formValues[inputFieldName] = inputContent;
    } else {
      delete formValues[inputFieldName];
    }
  });
}

export function handleCheckboxInputClick(indicator: DeclareIndicatorApi.Indicator, clickedValue: string, _event: MouseEvent) {
  const options = parseOptions(indicator.valueOptions);
  const exclusiveValues = new Set(options.filter((o) => o.exclusive).map((o) => o.value));
  const currentRaw = formValues[indicator.indicatorCode];
  const currentValues = currentRaw
    ? (Array.isArray(currentRaw) ? currentRaw : [currentRaw]).map((v: any) => deserializeInputTypeValue(v).value)
    : [];

  const wasSelected = isOptionSelectedWithLastPeriod(indicator.indicatorCode, clickedValue, currentRaw, lastPeriodRawValues.value[indicator.indicatorCode]);
  const isExclusive = exclusiveValues.has(clickedValue);

  let result: string[];
  if (isExclusive) {
    result = wasSelected ? [] : [clickedValue];
  } else {
    result = wasSelected
      ? currentValues.filter((v: string) => v !== clickedValue)
      : [...currentValues.filter((v: string) => !exclusiveValues.has(v)), clickedValue];
  }

  handleInputTypeCheckboxChange(indicator, result);
  if (onIndicatorChangeCallback) onIndicatorChangeCallback(indicator);
}

export function handleInputTypeCheckboxChange(indicator: DeclareIndicatorApi.Indicator, vals: string[]) {
  const options = parseOptions(indicator.valueOptions);
  const exclusiveValues = new Set(options.filter((o) => o.exclusive).map((o) => o.value));

  let result: string[];
  if (vals.some((v) => exclusiveValues.has(v))) result = [vals.find((v) => exclusiveValues.has(v))!];
  else result = vals;

  const serialized = result.map((v) => {
    const inputKey = indicator.indicatorCode + '_' + v;
    const inputContent = inputTypeValues[inputKey] || '';
    return serializeInputTypeValue(v, inputContent);
  });
  formValues[indicator.indicatorCode] = serialized;

  options.forEach((opt) => {
    const inputFieldName = getInputTypeInputFieldName(indicator.indicatorCode, opt.value);
    if (result.includes(opt.value)) {
      formValues[inputFieldName] = inputTypeValues[indicator.indicatorCode + '_' + opt.value] || '';
    } else {
      delete formValues[inputFieldName];
    }
  });

  if (onMultiSelectChangeCallback) onMultiSelectChangeCallback(indicator);
}

export function onInputTypeBlur(indicator: DeclareIndicatorApi.Indicator, opt: any, value?: string) {
  const inputKey = indicator.indicatorCode + '_' + opt.value;
  const content = (value !== undefined ? value : inputTypeValues[inputKey]) || '';
  const inputFieldName = getInputTypeInputFieldName(indicator.indicatorCode, opt.value);
  const raw = formValues[indicator.indicatorCode];

  if (raw) {
    if (Array.isArray(raw)) {
      formValues[indicator.indicatorCode] = raw.map((item: string) => {
        const d = deserializeInputTypeValue(item);
        return d.value === opt.value ? serializeInputTypeValue(opt.value, content) : item;
      });
    } else {
      formValues[indicator.indicatorCode] = serializeInputTypeValue(deserializeInputTypeValue(raw).value, content);
    }
  }
  formValues[inputFieldName] = content;

  const isSelected = isInputTypeOptionSelected(indicator.indicatorCode, raw, opt.value);
  if (!isSelected) { clearFieldError(toTopLevelKey(indicator.id!)); return; }

  if (!content.trim()) {
    setFieldError(toTopLevelKey(indicator.id!), `请填写"${opt.label}"的补充内容`, 'required', true);
  } else {
    const formatErr = validateInputContent(content);
    if (formatErr) setFieldError(toTopLevelKey(indicator.id!), `"${opt.label}"的补充内容：${formatErr}`, 'required', true);
    else clearFieldError(toTopLevelKey(indicator.id!));
  }
}

export function validateInputTypeRequired(indicator: DeclareIndicatorApi.Indicator): string | null {
  const options = parseOptions(indicator.valueOptions);
  const raw = formValues[indicator.indicatorCode];
  const values = raw ? (Array.isArray(raw) ? raw : [raw]) : [];

  for (const opt of options) {
    if (opt.inputType) {
      const isSelected = values.some((v: any) => deserializeInputTypeValue(v).value === opt.value);
      if (isSelected) {
        const inputKey = indicator.indicatorCode + '_' + opt.value;
        const content = inputTypeValues[inputKey] || '';
        if (!content.trim()) return `请填写"${opt.label}"的补充内容`;
        const formatError = validateInputContent(content);
        if (formatError) return `"${opt.label}"的补充内容：${formatError}`;
      }
    }
  }
  return null;
}

function isOptionSelectedWithLastPeriod(
  indicatorCode: string, optionValue: string,
  currentFormValues: any, lastPeriodRaw: string | undefined,
): boolean {
  if (currentFormValues && Array.isArray(currentFormValues)) {
    return currentFormValues.some((v: any) => deserializeInputTypeValue(v).value === optionValue);
  }
  if (currentFormValues && typeof currentFormValues === 'string') {
    return deserializeInputTypeValue(currentFormValues).value === optionValue;
  }
  if (lastPeriodRaw) {
    return lastPeriodRaw.split(',').some((p) => deserializeInputTypeValue(p.trim()).value === optionValue);
  }
  return false;
}

export function restoreInputTypeValues(vt: number, valueStr: string | undefined, indicatorCode: string, valueOptions?: string) {
  if ((vt === 6 || vt === 7) && valueStr) {
    const rawValues = vt === 7 ? valueStr.split(',') : [valueStr];
    const options = valueOptions ? parseOptions(valueOptions) : [];

    for (const v of rawValues) {
      const deserialized = deserializeInputTypeValue(v);
      if (deserialized.input) {
        // 标准格式：选项值∴输入内容
        inputTypeValues[indicatorCode + '_' + deserialized.value] = deserialized.input;
        formValues[indicatorCode + '_input_' + deserialized.value] = deserialized.input;
      } else if (options.length > 0) {
        // 旧数据格式：只有选项值或只有输入内容
        // 尝试匹配选项值
        const matchedOption = options.find((opt) => opt.value === v);
        if (matchedOption) {
          // 值本身就是选项值（如 '1001'），输入内容为空
          // 不需要额外处理，formValues 中已存储选项值
        } else if (v && v.length > 0) {
          // 只有输入内容（如 'Qwen'），没有选项值
          // 查找第一个输入型选项作为默认值
          const inputTypeOpt = options.find((opt) => opt.inputType);
          if (inputTypeOpt) {
            inputTypeValues[indicatorCode + '_' + inputTypeOpt.value] = v;
            formValues[indicatorCode + '_input_' + inputTypeOpt.value] = v;
            // 同时更新主值为标准格式
            formValues[indicatorCode] = inputTypeOpt.value + INPUT_VALUE_SEPARATOR + v;
          }
        }
      }
    }
  }
}

export { parseOptions };
export { getInputTypeRawValue, getInputTypeRawValues, getInputTypeOptionValue, getInputTypeOptionValues, isInputTypeOptionSelected } from './useFormValues';
