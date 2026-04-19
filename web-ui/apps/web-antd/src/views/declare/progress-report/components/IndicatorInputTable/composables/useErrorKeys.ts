/**
 * useErrorKeys - 错误 Key 生成和统一错误存储
 *
 * 提供统一的错误 key 生成和错误存储
 * 作为 useValidation.ts 和 useInputTypeOptions.ts 之间的共享基础
 */

import { reactive } from 'vue';
import type { FieldError } from '../types';

// ==================== 统一错误存储 ====================

/** 统一错误存储（替代 7 个分散错误状态 + 2 个脏追踪） */
export const fieldErrors = reactive<Record<string, FieldError>>({});

// ==================== Key 生成函数 ====================

/** 生成顶层指标错误 key */
export function toTopLevelKey(indicatorId: number): string {
  return `t:${indicatorId}`;
}

/** 动态容器的字段错误 key：${rowKey}${fieldCode}（如 7020101field001）
 * rowKey 本身由 generateContainerRowKey 生成，已包含 indicatorCode 前缀
 * 条件容器的 rowKey = indicatorCode（如 "603"），同样适用此格式 */
export function toContainerKey(rowKey: string, fieldCode: string): string {
  return `${rowKey}${fieldCode}`;
}

// ==================== 错误操作函数 ====================

/** 设置错误 */
export function setFieldError(
  key: string,
  message: string,
  errorType: FieldError['errorType'],
  dirty = true
): void {
  fieldErrors[key] = { message, errorType, dirty };
}

/** 清除错误（可选强制清除必填错误，用于失焦重新验证场景） */
export function clearFieldError(key: string, force = false): void {
  if (fieldErrors[key]) {
    // 必填错误不能被清除（必填错误优先级最高），除非 force=true
    if (fieldErrors[key].errorType === 'required' && !force) return;
    delete fieldErrors[key];
  }
}

/** 仅清除指定类型的错误，不碰其他类型的错误 */
export function clearFieldErrorIfType(key: string, targetType: FieldError['errorType']): void {
  if (fieldErrors[key]?.errorType === targetType) {
    delete fieldErrors[key];
  }
}

/** 设置脏标记 */
export function setDirty(key: string): void {
  if (fieldErrors[key]) {
    fieldErrors[key].dirty = true;
  }
}

/** 清除所有错误 */
export function clearAllErrors(): void {
  Object.keys(fieldErrors).forEach((k) => delete fieldErrors[k]);
}

/**
 * 统一错误读取入口
 * - 逻辑规则错误和联动校验错误无需脏追踪，始终显示
 * - 其他错误需要 dirty=true 才显示
 */
export function getFieldError(key: string): string | undefined {
  const err = fieldErrors[key];
  if (!err) return undefined;
  if (err.dirty || err.errorType === 'logic' || err.errorType === 'joint') {
    return err.message;
  }
  return undefined;
}
