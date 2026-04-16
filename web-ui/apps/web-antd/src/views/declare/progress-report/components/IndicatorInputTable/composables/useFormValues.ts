/**
 * useFormValues - 表单值状态管理
 *
 * 负责：
 * - 顶层指标值（formValues）
 * - 脏标记状态（isDirty）
 * - 输入型选项值（inputTypeValues）
 */

import { reactive, ref } from 'vue';

// ==================== 表单值状态 ====================

/** 指标表单值（key: indicatorCode → value） */
export const formValues = reactive<Record<string, any>>({});

/** 脏标记 */
export const isDirty = ref(false);

/** 标记为脏 */
export function markDirty() {
  isDirty.value = true;
}

/** 重置脏标记 */
export function resetDirty() {
  isDirty.value = false;
}

// ==================== 输入型选项值状态 ====================

/** 输入型选项的分隔符 */
export const INPUT_VALUE_SEPARATOR = '∵';

/** 输入型选项的值存储（key: indicatorCode + '_' + optionValue） */
export const inputTypeValues = reactive<Record<string, string>>({});

// ==================== 输入型选项辅助函数 ====================

/** 序列化输入型选项值（保存时调用） */
export function serializeInputTypeValue(value: string, inputContent: string): string {
  if (!inputContent) return value;
  return `${value}${INPUT_VALUE_SEPARATOR}${inputContent}`;
}

/** 反序列化输入型选项值（读取时调用） */
export function deserializeInputTypeValue(optionValue: string): { value: string; input: string } {
  const idx = optionValue.indexOf(INPUT_VALUE_SEPARATOR);
  if (idx === -1) {
    return { value: optionValue, input: '' };
  }
  return {
    value: optionValue.substring(0, idx),
    input: optionValue.substring(idx + INPUT_VALUE_SEPARATOR.length),
  };
}

/** 校验输入内容 */
export function validateInputContent(content: string): string | null {
  if (content.includes(INPUT_VALUE_SEPARATOR)) {
    return `输入内容不能包含特殊符号 "∵"`;
  }
  if (content.trim() !== '' && /^\d+$/.test(content)) {
    return '输入内容不能是纯数字';
  }
  return null;
}

/** 获取单选的原始值 */
export function getInputTypeRawValue(indicatorCode: string): string | undefined {
  return formValues[indicatorCode];
}

/** 获取单选的纯选项值（用于 radio :value） */
export function getInputTypeOptionValue(rawValue: string | undefined): string {
  if (!rawValue) return '';
  return deserializeInputTypeValue(rawValue).value;
}

/** 获取多选的原始值数组 */
export function getInputTypeRawValues(indicatorCode: string): string[] {
  const raw = formValues[indicatorCode];
  if (!raw) return [];
  if (Array.isArray(raw)) return raw;
  return [raw];
}

/** 获取多选的纯选项值数组（用于 checkbox :value） */
export function getInputTypeOptionValues(rawValues: string[]): string[] {
  return rawValues.map(v => deserializeInputTypeValue(v).value);
}

/** 获取 checkbox 绑定的纯值数组（从 formValues 中提取，用于 a-checkbox-group :value） */
export function getPureCheckboxValues(indicatorCode: string): string[] {
  const raw = formValues[indicatorCode];
  if (!raw) return [];
  if (Array.isArray(raw)) {
    return raw.map(v => deserializeInputTypeValue(v).value);
  }
  return [deserializeInputTypeValue(raw).value];
}

/** 输入框的 formValues key（用于 v:if 判断输入框是否显示） */
export function getInputTypeInputFieldName(code: string, optionValue: string): string {
  return `${code}_input_${optionValue}`;
}

/** 是否选中了某个输入型选项 */
export function isInputTypeOptionSelected(indicatorCode: string, raw: string | undefined, optionValue: string): boolean {
  if (!raw) return false;
  const values = Array.isArray(raw) ? raw : [raw];
  return values.some(v => deserializeInputTypeValue(v).value === optionValue);
}
