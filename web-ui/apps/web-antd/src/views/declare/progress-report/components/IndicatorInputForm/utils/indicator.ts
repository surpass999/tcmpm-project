/**
 * 指标辅助工具函数
 */
import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import type { UploadFile } from 'ant-design-vue/es/upload/interface';

export function parseExtraConfig(extraConfig: string | undefined): Record<string, any> {
  if (!extraConfig) return {};
  try {
    return JSON.parse(extraConfig);
  } catch {
    return {};
  }
}

export function getNumberPrecision(indicator: DeclareIndicatorApi.Indicator): number | undefined {
  const cfg = parseExtraConfig(indicator.extraConfig);
  return cfg.precision !== undefined ? Number(cfg.precision) : undefined;
}

export function getDateFormat(indicator: DeclareIndicatorApi.Indicator): string {
  return parseExtraConfig(indicator.extraConfig).format || 'YYYY-MM-DD';
}

export function getBooleanLabels(indicator: DeclareIndicatorApi.Indicator): { true: string; false: string } {
  const cfg = parseExtraConfig(indicator.extraConfig);
  return { true: cfg.trueLabel || '是', false: cfg.falseLabel || '否' };
}

export function getMaxLength(indicator: DeclareIndicatorApi.Indicator): number | undefined {
  const cfg = parseExtraConfig(indicator.extraConfig);
  return cfg.maxLength !== undefined ? Number(cfg.maxLength) : undefined;
}

export function getShowSearch(indicator: DeclareIndicatorApi.Indicator): boolean {
  return parseExtraConfig(indicator.extraConfig).showSearch === true;
}

export function getAcceptTypes(indicator: DeclareIndicatorApi.Indicator): string {
  return parseExtraConfig(indicator.extraConfig).accept || '';
}

export function getMaxFileCount(indicator: DeclareIndicatorApi.Indicator): number {
  const cfg = parseExtraConfig(indicator.extraConfig);
  if (cfg.maxCount !== undefined) return Number(cfg.maxCount);
  return indicator.maxValue ? Number(indicator.maxValue) : 5;
}

export function getFileList(fileListMap: Record<string, UploadFile[]>, indicatorCode: string): UploadFile[] {
  return fileListMap[indicatorCode] || [];
}

export function isRichText(indicator: DeclareIndicatorApi.Indicator): boolean {
  return parseExtraConfig(indicator.extraConfig).richText === true;
}

export function toNumber(val: any): number | undefined {
  if (val == null || val === '') return undefined;
  const n = Number(val);
  return isNaN(n) ? undefined : n;
}
