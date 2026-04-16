import { parseOptions } from './options';
import { deserializeInputTypeValue } from '../composables/useFormValues';

/**
 * 格式化上期数字值，按指标精度显示小数位
 */
export function formatLastValueNumber(rawValue: string | undefined, precision: number | undefined): string {
  if (!rawValue) return '';
  const num = Number(rawValue);
  if (isNaN(num)) return rawValue;
  if (precision === undefined || precision === null) return rawValue;
  return num.toFixed(precision);
}

/**
 * 将上期原始值转换为可读文本
 * 后端 getDisplayValue 已经做了 label 映射：
 * - 单选（valueType=6）：直接返回 label
 * - 多选（valueType=7）：用顿号连接 label
 * - 容器（valueType=12）：返回 JSON { fieldCode: label }
 * 函数只需要处理普通值的截断/格式化
 */
export function getLastValueDisplayText(
  rawValue: string | undefined,
  valueType: number,
  valueOptions: string,
): string {
  if (!rawValue) return '';

  // 后端已映射的直接返回（用于单选6、多选7、容器12）
  // 数字/字符串等普通值直接返回
  return rawValue;
}

/**
 * 解析原始 valueStr，提取每个选项的输入内容
 * 单选 raw: "选项值∵输入内容"
 * 多选 raw: "选项值1∵输入1,选项值2∵输入2,选项值3"
 * 返回: { [optionValue]: inputContent }
 */
export function parseInputTypeLastPeriodRaw(
  rawValue: string | undefined,
  valueType: number, // 6=单选, 7=多选
): Record<string, string> {
  if (!rawValue) return {};
  if (valueType === 6) {
    // 单选：直接解析
    const { value, input } = deserializeInputTypeValue(rawValue);
    return { [value]: input || '' };
  } else if (valueType === 7) {
    // 多选：用逗号分隔，每段各自解析
    const result: Record<string, string> = {};
    const parts = rawValue.split(',');
    for (const part of parts) {
      const trimmed = part.trim();
      if (!trimmed) continue;
      const { value, input } = deserializeInputTypeValue(trimmed);
      result[value] = input || '';
    }
    return result;
  }
  return {};
}

/**
 * 将容器字段的上期值转为可读文本
 * 后端 convertDynamicFieldValue 已将字段原始值转为 label
 * 容器 JSON 格式：{"fieldCode1": "label1", "fieldCode2": "label2"}
 * 函数只需要从 JSON 中取对应字段的值即可
 */
export function getContainerFieldLastText(
  fieldCode: string,
  containerLastValues: Record<string, string> | undefined,
): string {
  if (!containerLastValues) return '';
  return containerLastValues[fieldCode] ?? '';
}

/**
 * 获取容器上期条目摘要（第一条 + 条目总数）
 */
export function getContainerLastPeriodSummary(
  indicatorCode: string,
  containerLastValues: Record<string, string>[] | undefined,
): string {
  if (!containerLastValues || containerLastValues.length === 0) return '';
  return `共 ${containerLastValues.length} 条上期数据`;
}
