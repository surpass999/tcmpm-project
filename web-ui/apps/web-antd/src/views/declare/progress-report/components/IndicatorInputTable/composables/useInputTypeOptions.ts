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
  formValues,
  inputTypeValues,
  INPUT_VALUE_SEPARATOR,
  serializeInputTypeValue,
  deserializeInputTypeValue,
  validateInputContent,
  getInputTypeInputFieldName,
} from './useFormValues';

// ==================== 单选变化处理 ====================

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
  console.log('[DEBUG handleCheckboxInputClick] indicatorId:', indicator.id, 'clickedValue:', clickedValue, 'formValue before:', formValues[indicator.indicatorCode]);
  const options = parseOptions(indicator.valueOptions);
  const exclusiveValues = new Set(options.filter((o) => o.exclusive).map((o) => o.value));
  const currentRaw = formValues[indicator.indicatorCode];
  const currentValues = currentRaw
    ? Array.isArray(currentRaw)
      ? currentRaw.map(v => deserializeInputTypeValue(v).value)
      : [deserializeInputTypeValue(currentRaw).value]
    : [];

  const wasSelected = currentValues.includes(clickedValue);
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
  console.log('[DEBUG handleCheckboxInputClick] formValue after:', formValues[indicator.indicatorCode]);
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
    const deserialized = deserializeInputTypeValue(raw);
    const serialized = serializeInputTypeValue(deserialized.value, content);
    formValues[indicator.indicatorCode] = serialized;
  }
  formValues[inputFieldName] = content;
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
