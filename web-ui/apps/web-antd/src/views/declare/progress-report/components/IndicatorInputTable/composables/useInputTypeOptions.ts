/**
 * useInputTypeOptions - 输入型选项逻辑
 *
 * 负责：
 * - 解析选项（parseOptions）
 * - 单选/多选变化处理
 * - 输入型选项失焦校验
 * - 输入型选项必填校验
 */

import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import { parseOptions } from '../utils/options';
import {
  toTopLevelKey,
  setFieldError,
  clearFieldError,
} from './useErrorKeys';
import {
  formValues,
  inputTypeValues,
  serializeInputTypeValue,
  deserializeInputTypeValue,
  validateInputContent,
  getInputTypeInputFieldName,
  isInputTypeOptionSelected,
  isOptionSelectedWithLastPeriod,
} from './useFormValues';
import { lastPeriodRawValues } from './useIndicatorData';

/** 多选变化后的回调（由 index.vue 设置） */
let onMultiSelectChangeCallback: ((indicator: DeclareIndicatorApi.Indicator) => void) | null = null;

/** click 事件直接触发的回调（确保 click 事件直接调用验证） */
let onIndicatorChangeCallback: ((indicator: DeclareIndicatorApi.Indicator) => void) | null = null;

export function setOnMultiSelectChangeCallback(cb: (indicator: DeclareIndicatorApi.Indicator) => void) {
  onMultiSelectChangeCallback = cb;
}

export function setOnIndicatorChangeCallback(cb: (indicator: DeclareIndicatorApi.Indicator) => void) {
  onIndicatorChangeCallback = cb;
}

// ==================== 获取上期输入内容 ====================

/** 获取某个选项的上期输入内容 */
export function getInputTypeLastPeriodContent(
  lastPeriodRawValues: Record<string, string>,
  indicatorCode: string,
  optionValue: string,
): string {
  const raw = lastPeriodRawValues[indicatorCode];
  if (!raw) return '';
  try {
    return parseInputTypeLastPeriodRaw(raw, 6)[optionValue] || '';
  } catch {
    return '';
  }
}

/** 解析 raw valueStr 为 inputType content map：{ optionValue: content } */
function parseInputTypeLastPeriodRaw(raw: string, valueType: number): Record<string, string> {
  const result: Record<string, string> = {};
  if (!raw) return result;
  const parts = valueType === 7 ? raw.split(',') : [raw];
  for (const part of parts) {
    const idx = part.indexOf('∵');
    if (idx !== -1) {
      const value = part.substring(0, idx);
      const input = part.substring(idx + 1);
      result[value] = input;
    }
  }
  return result;
}

/** 输入型选项内容变化处理（同步到 inputTypeValues 和 formValues） */
export function onInputTypeChange(indicator: DeclareIndicatorApi.Indicator, opt: any, content: string) {
  const inputKey = indicator.indicatorCode + '_' + opt.value;
  inputTypeValues[inputKey] = content;
  const raw = formValues[indicator.indicatorCode];
  if (raw) {
    const deserialized = deserializeInputTypeValue(raw);
    formValues[indicator.indicatorCode] = serializeInputTypeValue(deserialized.value, content);
  }
}

/** 单选变化处理 */
export function handleInputTypeRadioChange(indicator: DeclareIndicatorApi.Indicator, val: string) {
  const options = parseOptions(indicator.valueOptions);
  options.forEach((opt) => {
    const inputFieldName = getInputTypeInputFieldName(indicator.indicatorCode, opt.value);
    if (opt.value === val) {
      const inputKey = indicator.indicatorCode + '_' + val;
      const inputContent = inputTypeValues[inputKey] || '';
      const serialized = serializeInputTypeValue(val, inputContent);
      formValues[indicator.indicatorCode] = serialized;
      formValues[inputFieldName] = inputContent;
    } else {
      delete formValues[inputFieldName];
    }
  });
  // 清除该指标的逻辑规则错误（在 index.vue 中通过事件处理）
}

// ==================== 多选变化处理 ====================

/** 处理多选输入型 checkbox 点击（用于互斥逻辑） */
export function handleCheckboxInputClick(indicator: DeclareIndicatorApi.Indicator, clickedValue: string, _event: MouseEvent) {
  const options = parseOptions(indicator.valueOptions);
  const exclusiveValues = new Set(options.filter((o) => o.exclusive).map((o) => o.value));
  const currentRaw = formValues[indicator.indicatorCode];
  const currentValues = currentRaw
    ? Array.isArray(currentRaw)
      ? currentRaw.map(v => deserializeInputTypeValue(v).value)
      : [deserializeInputTypeValue(currentRaw).value]
    : [];

  // 考虑上期值来判断是否已选中，避免只依赖 formValues 导致的误判
  const wasSelected = isOptionSelectedWithLastPeriod(
    indicator.indicatorCode,
    clickedValue,
    currentRaw,
    lastPeriodRawValues.value[indicator.indicatorCode],
  );
  const isExclusive = exclusiveValues.has(clickedValue);

  let result: string[];

  if (isExclusive) {
    if (wasSelected) {
      result = [];
    } else {
      result = [clickedValue];
    }
  } else {
    if (wasSelected) {
      result = currentValues.filter(v => v !== clickedValue);
    } else {
      result = [...currentValues.filter(v => !exclusiveValues.has(v)), clickedValue];
    }
  }

  handleInputTypeCheckboxChange(indicator, result);

  // click 事件直接触发指标变化回调（包含所有三层验证：基础验证、逻辑验证、上期值验证）
  if (onIndicatorChangeCallback) {
    onIndicatorChangeCallback(indicator);
  }
}

/** 多选变化处理 */
export function handleInputTypeCheckboxChange(indicator: DeclareIndicatorApi.Indicator, vals: string[]) {
  const options = parseOptions(indicator.valueOptions);
  const exclusiveValues = new Set(options.filter((o) => o.exclusive).map((o) => o.value));

  let result: string[];
  if (vals.some((v) => exclusiveValues.has(v))) {
    result = [vals.find((v) => exclusiveValues.has(v))!];
  } else {
    result = vals;
  }

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

  // 触发回调以执行校验
  if (onMultiSelectChangeCallback) {
    onMultiSelectChangeCallback(indicator);
  }
}

// ==================== 输入框失焦 ====================

/** 输入框失焦时的校验 */
export function onInputTypeBlur(indicator: DeclareIndicatorApi.Indicator, opt: any, value?: string) {
  const inputKey = indicator.indicatorCode + '_' + opt.value;
  const content = (value !== undefined ? value : inputTypeValues[inputKey]) || '';
  const inputFieldName = getInputTypeInputFieldName(indicator.indicatorCode, opt.value);

  // 同步更新 formValues 主字段和子字段
  const raw = formValues[indicator.indicatorCode];
  if (raw) {
    if (Array.isArray(raw)) {
      // 多选类型：更新对应选项的输入内容
      const newRaw = raw.map((item) => {
        const deserialized = deserializeInputTypeValue(item);
        if (deserialized.value === opt.value) {
          return serializeInputTypeValue(opt.value, content);
        }
        return item;
      });
      formValues[indicator.indicatorCode] = newRaw;
    } else {
      // 单选类型
      const deserialized = deserializeInputTypeValue(raw);
      const serialized = serializeInputTypeValue(deserialized.value, content);
      formValues[indicator.indicatorCode] = serialized;
    }
  }
  formValues[inputFieldName] = content;

  // 只有选项被选中时才校验：必填 + 格式
  const isSelected = isInputTypeOptionSelected(indicator.indicatorCode, raw, opt.value);

  if (!isSelected) {
    clearFieldError(toTopLevelKey(indicator.id!));
    return;
  }

  if (!content.trim()) {
    setFieldError(toTopLevelKey(indicator.id!), `请填写"${opt.label}"的补充内容`, 'required', true);
  } else {
    const formatErr = validateInputContent(content);
    if (formatErr) {
      setFieldError(toTopLevelKey(indicator.id!), `"${opt.label}"的补充内容：${formatErr}`, 'required', true);
    } else {
      clearFieldError(toTopLevelKey(indicator.id!));
    }
  }
}

// ==================== 输入型选项必填校验 ====================

/** 校验输入型选项是否必填（选中时必须填写内容） */
export function validateInputTypeRequired(indicator: DeclareIndicatorApi.Indicator): string | null {
  const options = parseOptions(indicator.valueOptions);
  const raw = formValues[indicator.indicatorCode];
  const values = raw ? (Array.isArray(raw) ? raw : [raw]) : [];

  for (const opt of options) {
    if (opt.inputType) {
      const isSelected = values.some(v => deserializeInputTypeValue(v).value === opt.value);
      if (isSelected) {
        const inputKey = indicator.indicatorCode + '_' + opt.value;
        const content = inputTypeValues[inputKey] || '';
        if (!content.trim()) {
          return `请填写"${opt.label}"的补充内容`;
        }
        const formatError = validateInputContent(content);
        if (formatError) {
          return `"${opt.label}"的补充内容：${formatError}`;
        }
      }
    }
  }
  return null;
}

// ==================== 获取输入型选项错误 ====================

/** 获取输入型选项的错误信息 */
export function getInputTypeError(indicator: DeclareIndicatorApi.Indicator, opt: any): string | null {
  const inputKey = indicator.indicatorCode + '_' + opt.value;
  const content = inputTypeValues[inputKey] || '';
  return validateInputContent(content);
}

// ==================== 从 useFormValues 和 utils/options 重新导出（供 index.vue 统一导入）====================
export { parseOptions } from '../utils/options';
export {
  getInputTypeRawValue,
  getInputTypeRawValues,
  getInputTypeOptionValue,
  getInputTypeOptionValues,
  isInputTypeOptionSelected,
} from './useFormValues';
export function restoreInputTypeValues(
  vt: number,
  valueStr: string | undefined,
  indicatorCode: string
) {
  if ((vt === 6 || vt === 7) && valueStr) {
    const rawValues = vt === 7 ? valueStr.split(',') : [valueStr];
    for (const v of rawValues) {
      const deserialized = deserializeInputTypeValue(v);
      if (deserialized.input) {
        inputTypeValues[indicatorCode + '_' + deserialized.value] = deserialized.input;
        formValues[indicatorCode + '_input_' + deserialized.value] = deserialized.input;
      }
    }
  }
}
