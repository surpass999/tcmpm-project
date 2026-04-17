/**
 * useErrorKeys - 错误 Key 生成和统一错误存储
 */

import { reactive } from 'vue';
import type { FieldError } from '../types';

export const fieldErrors = reactive<Record<string, FieldError>>({});

export function toTopLevelKey(indicatorId: number): string {
  return `t:${indicatorId}`;
}

export function toContainerKey(indicatorCode: string, rowKey: string, fieldCode: string): string {
  return `c:${indicatorCode}:${rowKey}:${fieldCode}`;
}

export function toInputTypeKey(indicatorCode: string, optionValue: string): string {
  return `i:${indicatorCode}:${optionValue}`;
}

export function setFieldError(key: string, message: string, errorType: FieldError['errorType'], dirty = true): void {
  fieldErrors[key] = { message, errorType, dirty };
}

export function clearFieldError(key: string, force = false): void {
  if (fieldErrors[key]) {
    if (fieldErrors[key].errorType === 'required' && !force) return;
    delete fieldErrors[key];
  }
}

export function setDirty(key: string): void {
  if (fieldErrors[key]) fieldErrors[key].dirty = true;
}

export function clearAllErrors(): void {
  Object.keys(fieldErrors).forEach((k) => delete fieldErrors[k]);
}

export function getFieldError(key: string): string | undefined {
  const err = fieldErrors[key];
  if (!err) return undefined;
  if (err.dirty || err.errorType === 'logic' || err.errorType === 'joint') {
    return err.message;
  }
  return undefined;
}
