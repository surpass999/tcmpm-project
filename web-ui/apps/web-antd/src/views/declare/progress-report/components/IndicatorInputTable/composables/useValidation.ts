/**
 * 校验逻辑 composable
 */

import { computed, reactive } from 'vue';
import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import type { ValidationError, DynamicField, ContainerFieldContext } from '../types';
import { useContainerValues } from './useContainerValues';

/** 空值判断 */
function isEmpty(value: any): boolean {
  if (value === undefined || value === null || value === '') return true;
  if (typeof value === 'string') {
    const t = value.trim();
    if (t === '' || t === '[]' || t === '[ ]') return true;
  }
  return false;
}

/** 必填校验 */
function checkRequired(value: any, isRequired: boolean): string | null {
  if (isRequired && isEmpty(value)) return '此项为必填';
  return null;
}

/** 范围校验 */
function checkRange(value: number, min: number | null, max: number | null): string | null {
  if (min != null && value < min) return `不能小于 ${min}`;
  if (max != null && value > max) return `不能大于 ${max}`;
  return null;
}

/** 精度校验 */
function checkPrecision(value: number, precision: number | undefined): string | null {
  if (precision === undefined) return null;
  if (value !== Number(value.toFixed(precision))) return `小数位数不能超过 ${precision} 位`;
  return null;
}

/** 多选数量校验 */
function checkSelectCount(value: any, minSelect?: number, maxSelect?: number): string | null {
  const arr = Array.isArray(value) ? value : [];
  if (minSelect != null && arr.length < minSelect) return `至少选择 ${minSelect} 项`;
  if (maxSelect != null && arr.length > maxSelect) return `最多选择 ${maxSelect} 项`;
  return null;
}

export interface UseValidationOptions {
  formValues: Record<string, any>;
  indicators: typeof import('vue').ref<DeclareIndicatorApi.Indicator[]>;
  containerValues: Record<string, any>;
  containerFieldDirty: Record<string, boolean>;
}

export function useValidation(options: UseValidationOptions) {
  const { formValues, indicators, containerValues, containerFieldDirty } = options;
  const containerValuesUtils = useContainerValues();

  /** 联合校验错误 Map */
  const jointRuleErrors = reactive<Record<string, string>>({});

  /** 逻辑规则错误 Map */
  const logicRuleErrors = reactive<Record<string, string>>({});

  /** 解析扩展配置 */
  function parseExtraConfig(extraConfig: string | undefined): Record<string, any> {
    if (!extraConfig) return {};
    try {
      return JSON.parse(extraConfig);
    } catch {
      return {};
    }
  }

  /** 解析 valueOptions JSON */
  function parseOptions(valueOptions: string): Array<{ value: string; label: string; exclusive?: boolean }> {
    if (!valueOptions) return [];
    try {
      const parsed = JSON.parse(valueOptions);
      return Array.isArray(parsed)
        ? parsed.map((item: any) => ({
            value: String(item.value),
            label: item.label ?? item.value,
            exclusive: item.exclusive == true,
          }))
        : [];
    } catch {
      return [];
    }
  }

  /** 判断指标是否为计算指标 */
  function isComputedIndicator(indicator: DeclareIndicatorApi.Indicator): boolean {
    return !!(indicator.calculationRule && indicator.calculationRule.trim());
  }

  // ==================== 按类型校验函数 ====================

  /** valueType=1 数字 */
  function validateType1_Number(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
    const errors: ValidationError[] = [];
    const { indicatorCode: code, id, isRequired, minValue, maxValue } = indicator;
    const value = formValues[code];

    const isComputed = isComputedIndicator(indicator);
    const reqErr = checkRequired(value, !!isRequired && !isComputed);
    if (reqErr) {
      errors.push({
        indicatorId: id,
        indicatorCode: code,
        message: `${indicator.indicatorCode} - ${indicator.indicatorName}：${reqErr}`,
        errorType: 'required',
        indicatorName: indicator.indicatorName,
      });
      return errors;
    }

    if (!isEmpty(value)) {
      const numVal = Number(value);
      if (isNaN(numVal)) {
        errors.push({
          indicatorId: id,
          indicatorCode: code,
          message: `${indicator.indicatorCode} - ${indicator.indicatorName}：请输入有效数字`,
          errorType: 'format',
          indicatorName: indicator.indicatorName,
        });
        return errors;
      }
      const rangeErr = checkRange(numVal, minValue ?? null, maxValue ?? null);
      if (rangeErr) {
        errors.push({
          indicatorId: id,
          indicatorCode: code,
          message: `${indicator.indicatorCode} - ${indicator.indicatorName}：${rangeErr}`,
          errorType: 'range',
          indicatorName: indicator.indicatorName,
        });
      }
      const cfg = parseExtraConfig(indicator.extraConfig);
      const precErr = checkPrecision(numVal, cfg.precision);
      if (precErr) {
        errors.push({
          indicatorId: id,
          indicatorCode: code,
          message: `${indicator.indicatorCode} - ${indicator.indicatorName}：${precErr}`,
          errorType: 'format',
          indicatorName: indicator.indicatorName,
        });
      }
    }
    return errors;
  }

  /** valueType=2 文本 */
  function validateType2_Text(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
    const errors: ValidationError[] = [];
    const value = formValues[indicator.indicatorCode];
    const isEmptyVal = value === undefined || value === null || value === '';

    const reqErr = checkRequired(value, !!indicator.isRequired);
    if (reqErr) {
      errors.push({
        indicatorId: indicator.id,
        indicatorCode: indicator.indicatorCode,
        message: `${indicator.indicatorCode} - ${indicator.indicatorName}：${reqErr}`,
        errorType: 'required',
        indicatorName: indicator.indicatorName,
      });
      return errors;
    }

    if (!isEmptyVal) {
      const trimmed = String(value).trim();
      if (/^-?\d+(\.\d+)?$/.test(trimmed)) {
        errors.push({
          indicatorId: indicator.id,
          indicatorCode: indicator.indicatorCode,
          message: `${indicator.indicatorCode} - ${indicator.indicatorName}：不能输入纯数字`,
          errorType: 'format',
          indicatorName: indicator.indicatorName,
        });
      }
    }
    return errors;
  }

  /** valueType=3 布尔：无需校验 */
  function validateType3_Boolean(_: DeclareIndicatorApi.Indicator): ValidationError[] {
    return [];
  }

  /** valueType=4 日期 */
  function validateType4_Date(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
    const errors: ValidationError[] = [];
    const reqErr = checkRequired(formValues[indicator.indicatorCode], !!indicator.isRequired);
    if (reqErr) {
      errors.push({
        indicatorId: indicator.id,
        indicatorCode: indicator.indicatorCode,
        message: `${indicator.indicatorCode} - ${indicator.indicatorName}：${reqErr}`,
        errorType: 'required',
        indicatorName: indicator.indicatorName,
      });
    }
    return errors;
  }

  /** valueType=5 富文本 */
  function validateType5_RichText(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
    return validateType2_Text(indicator);
  }

  /** valueType=6 单选 */
  function validateType6_Select(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
    const errors: ValidationError[] = [];
    const reqErr = checkRequired(formValues[indicator.indicatorCode], !!indicator.isRequired);
    if (reqErr) {
      errors.push({
        indicatorId: indicator.id,
        indicatorCode: indicator.indicatorCode,
        message: `${indicator.indicatorCode} - ${indicator.indicatorName}：${reqErr}`,
        errorType: 'required',
        indicatorName: indicator.indicatorName,
      });
    }
    return errors;
  }

  /** valueType=7 多选 */
  function validateType7_MultiSelect(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
    const errors: ValidationError[] = [];
    const value = formValues[indicator.indicatorCode];
    const arr = Array.isArray(value) ? value : [];
    if (indicator.isRequired && arr.length === 0) {
      errors.push({
        indicatorId: indicator.id,
        indicatorCode: indicator.indicatorCode,
        message: `${indicator.indicatorCode} - ${indicator.indicatorName}：此项为必填`,
        errorType: 'required',
        indicatorName: indicator.indicatorName,
      });
    }
    return errors;
  }

  /** valueType=8 日期区间 */
  function validateType8_DateRange(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
    const errors: ValidationError[] = [];
    const reqErr = checkRequired(formValues[indicator.indicatorCode], !!indicator.isRequired);
    if (reqErr) {
      errors.push({
        indicatorId: indicator.id,
        indicatorCode: indicator.indicatorCode,
        message: `${indicator.indicatorCode} - ${indicator.indicatorName}：${reqErr}`,
        errorType: 'required',
        indicatorName: indicator.indicatorName,
      });
    }
    return errors;
  }

  /** valueType=9 文件上传 */
  function validateType9_File(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
    const errors: ValidationError[] = [];
    const value = formValues[indicator.indicatorCode];
    const isFileEmpty = isEmpty(value) || value === '[]' || value === '[ ]';
    if (indicator.isRequired && isFileEmpty) {
      errors.push({
        indicatorId: indicator.id,
        indicatorCode: indicator.indicatorCode,
        message: `${indicator.indicatorCode} - ${indicator.indicatorName}：此项为必填`,
        errorType: 'required',
        indicatorName: indicator.indicatorName,
      });
    }
    return errors;
  }

  /** valueType=10 部门选择 */
  function validateType10_Dept(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
    return validateType6_Select(indicator);
  }

  /** valueType=11 用户选择 */
  function validateType11_User(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
    return validateType6_Select(indicator);
  }

  // ==================== 容器校验 ====================

  /** 校验容器内字段 */
  function validateContainerField(ctx: ContainerFieldContext): ValidationError[] {
    const { field, fieldValue, indicatorId, containerCode, entryIndex, fieldCode, fieldLabel, indicatorName, entry, errors } = ctx;
    const entryLabel = `第${entryIndex + 1}个条目`;
    const containerFieldKey = containerValuesUtils.generateContainerFieldKey(containerCode, entry.rowKey, fieldCode);

    // 必填校验
    if (field.required) {
      const err = checkRequired(fieldValue, true);
      if (err) {
        errors.push({
          indicatorId,
          indicatorCode: containerCode,
          message: `${indicatorName} ${entryLabel}「${fieldLabel}」为必填`,
          containerFieldKey,
          errorType: 'required',
          indicatorName,
          fieldLabel,
        });
        return errors;
      }
    }

    // 非空时校验格式
    if (!isEmpty(fieldValue)) {
      if (field.fieldType === 'number') {
        const numVal = Number(fieldValue);
        if (isNaN(numVal)) {
          errors.push({
            indicatorId,
            indicatorCode: containerCode,
            message: `${indicatorName} ${entryLabel}「${fieldLabel}：请输入有效数字`,
            containerFieldKey,
            errorType: 'format',
            indicatorName,
            fieldLabel,
          });
          return errors;
        }
        const precErr = checkPrecision(numVal, field.precision);
        if (precErr) {
          errors.push({
            indicatorId,
            indicatorCode: containerCode,
            message: `${indicatorName} ${entryLabel}「${fieldLabel}」：${precErr}`,
            containerFieldKey,
            errorType: 'format',
            indicatorName,
            fieldLabel,
          });
          return errors;
        }
      }

      if (field.fieldType === 'checkbox') {
        const countErr = checkSelectCount(fieldValue, field.minSelect, field.maxSelect);
        if (countErr) {
          errors.push({
            indicatorId,
            indicatorCode: containerCode,
            message: `${indicatorName} ${entryLabel}「${fieldLabel}」：${countErr}`,
            containerFieldKey,
            errorType: 'required',
            indicatorName,
            fieldLabel,
          });
          return errors;
        }
      }

      if (field.fieldType === 'text' || field.fieldType === 'textarea') {
        const trimmed = String(fieldValue).trim();
        if (field.fieldType === 'text' && /^-?\d+(\.\d+)?$/.test(trimmed)) {
          errors.push({
            indicatorId,
            indicatorCode: containerCode,
            message: `${indicatorName} ${entryLabel}「${fieldLabel}：不能输入纯数字`,
            containerFieldKey,
            errorType: 'format',
            indicatorName,
            fieldLabel,
          });
          return errors;
        }
      }
    }

    return errors;
  }

  /** valueType=12 动态容器 */
  function validateType12_Container(indicator: DeclareIndicatorApi.Indicator): ValidationError[] {
    const errors: ValidationError[] = [];
    const { indicatorCode: code, id, indicatorName } = indicator;
    const entries = containerValues[code] || [];
    const fields = containerValuesUtils.parseDynamicFields(indicator.valueOptions);

    for (let i = 0; i < entries.length; i++) {
      const entry = entries[i];
      for (const field of fields) {
        const fullKey = containerValuesUtils.generateContainerFieldKey(code, entry.rowKey, field.fieldCode);
        const fieldValue = entry[fullKey];
        const ctx: ContainerFieldContext = {
          indicatorId: id!,
          containerCode: code,
          entryIndex: i,
          entry,
          field,
          fieldCode: field.fieldCode,
          fieldValue,
          fieldLabel: field.fieldLabel,
          indicatorName,
          errors,
        };
        validateContainerField(ctx);
      }
    }

    return errors;
  }

  /** 主校验入口 */
  function validateAll(indicatorsToValidate: DeclareIndicatorApi.Indicator[]): ValidationError[] {
    const errors: ValidationError[] = [];

    const validators: Record<number, (ind: DeclareIndicatorApi.Indicator) => ValidationError[]> = {
      1: validateType1_Number,
      2: validateType2_Text,
      3: validateType3_Boolean,
      4: validateType4_Date,
      5: validateType5_RichText,
      6: validateType6_Select,
      7: validateType7_MultiSelect,
      8: validateType8_DateRange,
      9: validateType9_File,
      10: validateType10_Dept,
      11: validateType11_User,
      12: validateType12_Container,
    };

    for (const indicator of indicatorsToValidate) {
      const v = validators[indicator.valueType];
      if (v) errors.push(...v(indicator));
    }

    // 标记脏
    for (const error of errors) {
      if (error.containerFieldKey) {
        containerFieldDirty[error.containerFieldKey] = true;
      }
    }

    return errors;
  }

  /** 顶级指标错误 computed */
  const indicatorErrors = computed(() => {
    const errors: Record<string, string> = {};
    for (const ind of indicators.value) {
      const code = ind.indicatorCode;
      const value = formValues[code];
      const isEmptyVal = value === undefined || value === null || value === '';
      const isComputed = isComputedIndicator(ind);

      const idKey = ind.id !== undefined ? String(ind.id) : code;

      if (ind.isRequired && isEmptyVal && !isComputed) {
        errors[idKey] = '此项为必填';
        continue;
      }

      if (!isEmptyVal) {
        if (ind.valueType === 2) {
          const trimmed = String(value).trim();
          if (/^-?\d+(\.\d+)?$/.test(trimmed)) {
            errors[idKey] = '不能输入纯数字';
            continue;
          }
        }
        if (ind.valueType === 1) {
          const numVal = Number(value);
          if (isNaN(numVal)) {
            errors[idKey] = '请输入有效数字';
            continue;
          }
        }
      }
    }
    return errors;
  });

  /** 容器字段错误 computed */
  const containerFieldErrors = computed(() => {
    const errors: Record<string, string> = {};
    for (const indicator of indicators.value) {
      if (indicator.valueType !== 12) continue;
      const code = indicator.indicatorCode;
      const entries = containerValues[code] || [];
      const fields = containerValuesUtils.parseDynamicFields(indicator.valueOptions);

      for (let i = 0; i < entries.length; i++) {
        const entry = entries[i];
        for (const field of fields) {
          const fullKey = containerValuesUtils.generateContainerFieldKey(code, entry.rowKey, field.fieldCode);
          const fieldValue = entry[fullKey];
          const isEmptyVal = fieldValue === undefined || fieldValue === null || fieldValue === '';

          if (field.required && isEmptyVal) {
            errors[fullKey] = '此项为必填';
            continue;
          }

          if (!isEmptyVal) {
            if (field.fieldType === 'number') {
              const numVal = Number(fieldValue);
              if (isNaN(numVal)) {
                errors[fullKey] = '请输入有效数字';
                continue;
              }
            }
          }
        }
      }
    }
    return errors;
  });

  return {
    jointRuleErrors,
    logicRuleErrors,
    validateAll,
    indicatorErrors,
    containerFieldErrors,
    parseExtraConfig,
    parseOptions,
    isComputedIndicator,
  };
}
