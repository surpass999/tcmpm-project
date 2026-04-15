/**
 * 指标辅助工具函数
 * 提供指标属性解析的辅助函数
 */
import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import type { UploadFile } from 'ant-design-vue/es/upload/interface';

/** 解析额外配置 */
export function parseExtraConfig(extraConfig: string | undefined): Record<string, any> {
  if (!extraConfig) return {};
  try {
    return JSON.parse(extraConfig);
  } catch {
    return {};
  }
}

/** 获取数字精度 */
export function getNumberPrecision(indicator: DeclareIndicatorApi.Indicator): number | undefined {
  const cfg = parseExtraConfig(indicator.extraConfig);
  return cfg.precision !== undefined ? Number(cfg.precision) : undefined;
}

/** 获取日期格式 */
export function getDateFormat(indicator: DeclareIndicatorApi.Indicator): string {
  return parseExtraConfig(indicator.extraConfig).format || 'YYYY-MM-DD';
}

/** 获取布尔值标签 */
export function getBooleanLabels(indicator: DeclareIndicatorApi.Indicator): { true: string; false: string } {
  const cfg = parseExtraConfig(indicator.extraConfig);
  return { true: cfg.trueLabel || '是', false: cfg.falseLabel || '否' };
}

/** 获取最大长度 */
export function getMaxLength(indicator: DeclareIndicatorApi.Indicator): number | undefined {
  const cfg = parseExtraConfig(indicator.extraConfig);
  return cfg.maxLength !== undefined ? Number(cfg.maxLength) : undefined;
}

/** 获取是否显示搜索 */
export function getShowSearch(indicator: DeclareIndicatorApi.Indicator): boolean {
  return parseExtraConfig(indicator.extraConfig).showSearch === true;
}

/** 获取文件类型 */
export function getAcceptTypes(indicator: DeclareIndicatorApi.Indicator): string {
  return parseExtraConfig(indicator.extraConfig).accept || '';
}

/** 获取最大文件数量 */
export function getMaxFileCount(indicator: DeclareIndicatorApi.Indicator): number {
  const cfg = parseExtraConfig(indicator.extraConfig);
  if (cfg.maxCount !== undefined) return Number(cfg.maxCount);
  return indicator.maxValue ? Number(indicator.maxValue) : 5;
}

/** 获取文件列表 */
export function getFileList(fileListMap: Record<string, UploadFile[]>, indicatorCode: string): UploadFile[] {
  return fileListMap[indicatorCode] || [];
}

/** 判断是否为富文本 */
export function isRichText(indicator: DeclareIndicatorApi.Indicator): boolean {
  return parseExtraConfig(indicator.extraConfig).richText === true;
}

/** 转为数字（安全） */
export function toNumber(val: any): number | undefined {
  if (val == null || val === '') return undefined;
  const n = Number(val);
  return isNaN(n) ? undefined : n;
}
