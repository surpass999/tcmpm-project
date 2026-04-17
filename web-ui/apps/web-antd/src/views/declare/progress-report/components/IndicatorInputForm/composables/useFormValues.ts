/**
 * useFormValues - 表单值状态管理
 */

import { reactive, ref } from 'vue';

export const INPUT_VALUE_SEPARATOR = '∵';

export const formValues = reactive<Record<string, any>>({});

export const isDirty = ref(false);

export function markDirty() {
  isDirty.value = true;
}

export function resetDirty() {
  isDirty.value = false;
}

export const inputTypeValues = reactive<Record<string, string>>({});

export function serializeInputTypeValue(value: string, inputContent: string): string {
  if (!inputContent) return value;
  return `${value}${INPUT_VALUE_SEPARATOR}${inputContent}`;
}

export function deserializeInputTypeValue(optionValue: string): { value: string; input: string } {
  const idx = optionValue.indexOf(INPUT_VALUE_SEPARATOR);
  if (idx === -1) return { value: optionValue, input: '' };
  return { value: optionValue.substring(0, idx), input: optionValue.substring(idx + INPUT_VALUE_SEPARATOR.length) };
}

export function validateInputContent(content: string): string | null {
  if (content.includes(INPUT_VALUE_SEPARATOR)) return `输入内容不能包含特殊符号 "∵"`;
  if (content.trim() !== '' && /^\d+$/.test(content)) return '输入内容不能是纯数字';
  return null;
}

export function getInputTypeRawValue(indicatorCode: string): string | undefined {
  return formValues[indicatorCode];
}

export function getInputTypeOptionValue(rawValue: string | undefined): string {
  if (!rawValue) return '';
  return deserializeInputTypeValue(rawValue).value;
}

export function getInputTypeRawValues(indicatorCode: string): string[] {
  const raw = formValues[indicatorCode];
  if (!raw) return [];
  if (Array.isArray(raw)) return raw;
  return [raw];
}

export function getInputTypeOptionValues(rawValues: string[]): string[] {
  return rawValues.map((v) => deserializeInputTypeValue(v).value);
}

export function getPureCheckboxValues(indicatorCode: string): string[] {
  const raw = formValues[indicatorCode];
  if (!raw) return [];
  if (Array.isArray(raw)) return raw.map((v) => deserializeInputTypeValue(v).value);
  return [deserializeInputTypeValue(raw).value];
}

export function getInputTypeInputFieldName(code: string, optionValue: string): string {
  return `${code}_input_${optionValue}`;
}

export function isInputTypeOptionSelected(_indicatorCode: string, raw: string | undefined, optionValue: string): boolean {
  if (!raw) return false;
  const values = Array.isArray(raw) ? raw : [raw];
  return values.some((v) => deserializeInputTypeValue(v).value === optionValue);
}

export function isOptionSelectedWithLastPeriod(
  _indicatorCode: string,
  optionValue: string,
  currentFormValues: any,
  lastPeriodRaw: string | undefined,
): boolean {
  if (currentFormValues && Array.isArray(currentFormValues)) {
    return currentFormValues.some((v: string) => deserializeInputTypeValue(v).value === optionValue);
  }
  if (currentFormValues && typeof currentFormValues === 'string') {
    return deserializeInputTypeValue(currentFormValues).value === optionValue;
  }
  if (lastPeriodRaw) {
    const lastParts = lastPeriodRaw.split(',');
    return lastParts.some((p) => deserializeInputTypeValue(p.trim()).value === optionValue);
  }
  return false;
}
