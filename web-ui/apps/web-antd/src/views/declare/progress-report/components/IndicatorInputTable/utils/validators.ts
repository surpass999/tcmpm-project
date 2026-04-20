/**
 * 校验工具函数
 * 提供指标值校验的辅助函数
 */
import type { ValidationError } from '../types';

/** 数据库 decimal(18,4) 的范围限制 */
export const DECIMAL_18_4_MAX = 90000000000.0;
export const DECIMAL_18_4_MIN = -90000000000.0;

/** 判断值是否为空 */
export function isEmpty(value: any): boolean {
  if (value === undefined || value === null) return true;
  if (typeof value === 'number') return false;  // NaN/Infinity 也视为非空（交给格式校验处理）
  if (typeof value === 'string') {
    const t = value.trim();
    if (t === '' || t === '[]' || t === '[ ]') return true;
    return false;
  }
  if (Array.isArray(value)) return value.length === 0;
  return false;
}

/** 校验必填 */
export function checkRequired(value: any, isRequired: boolean): string | null {
  if (isRequired && isEmpty(value)) return '此项为必填';
  return null;
}

/** 校验数值范围 */
export function checkRange(value: number, min: number | null | undefined, max: number | null | undefined): string | null {
  if (min != null) {
    if (value < min) return `不能小于 ${min}`;
  } else if (value < DECIMAL_18_4_MIN) {
    return `不能小于 ${DECIMAL_18_4_MIN}`;
  }

  if (max != null) {
    if (value > max) return `不能大于 ${max}`;
  } else if (value > DECIMAL_18_4_MAX) {
    return `不能大于 ${DECIMAL_18_4_MAX}`;
  }

  return null;
}

/** 校验小数精度
 * @param value 原始值（数字或字符串），用于判断是否有小数点
 * @param precision 允许的小数位数
 * @param rawValue 可选，原始字符串，用于精度=0时精确判断是否输入了小数点
 */
export function checkPrecision(value: number | string, precision: number | undefined, rawValue?: string): string | null {
  if (precision === undefined) return null;
  if (precision === 0) {
    // 精度0：禁止任何小数点（必须用 rawValue 判断，因为 Number("22.0") === 22 会丢失小数点信息）
    if (rawValue !== undefined) {
      if (/\.\d/.test(rawValue)) return `小数位数不能超过 ${precision} 位`;
    } else if (!Number.isInteger(Number(value))) {
      return `小数位数不能超过 ${precision} 位`;
    }
    return null;
  }
  if (Number(value) !== Number(Number(value).toFixed(precision))) return `小数位数不能超过 ${precision} 位`;
  return null;
}

/** 校验选择数量 */
export function checkSelectCount(value: any, minSelect?: number, maxSelect?: number): string | null {
  const arr = Array.isArray(value) ? value : [];
  if (minSelect != null && arr.length < minSelect) return `至少选择 ${minSelect} 项`;
  if (maxSelect != null && arr.length > maxSelect) return `最多选择 ${maxSelect} 项`;
  return null;
}

/** 添加校验错误 */
export function pushError(
  errors: ValidationError[],
  code: string,
  message: string,
  id?: number,
  containerFieldKey?: string,
  errorType?: ValidationError['errorType']
) {
  errors.push({
    indicatorId: id,
    indicatorCode: code,
    message,
    containerFieldKey,
    errorType,
    indicatorName: undefined,
  });
}
