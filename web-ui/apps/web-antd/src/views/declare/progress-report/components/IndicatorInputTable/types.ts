/**
 * IndicatorInputTable 公共类型定义
 */

import type { DeclareIndicatorApi } from '#/api/declare/indicator';

/** 分组信息 */
export interface GroupInfo {
  groupName: string;
  groupPrefix: string;
  parentId: number;
  groupLevel: number;
}

/** 指标分组（一级 → 二级 → 指标） */
export interface IndicatorGroup {
  groupId: number;
  groupName: string;
  groupPrefix: string;
  parentId: number;
  groupLevel: number;
  indicators: DeclareIndicatorApi.Indicator[];
  children: IndicatorGroup[];
}

/** 动态容器子字段条件显示配置 */
export interface ShowCondition {
  watchField: string;
  operator: 'eq' | 'neq' | 'gt' | 'gte' | 'lt' | 'lte' | 'in' | 'notEmpty' | 'isEmpty';
  value?: any;
}

/** 动态容器子字段定义 */
export interface DynamicField {
  fieldCode: string;
  fieldLabel: string;
  fieldType: string;
  required?: boolean;
  sort?: number;
  options?: { value: string; label: string }[];
  maxLength?: number;
  placeholder?: string;
  showSearch?: boolean;
  minSelect?: number;
  maxSelect?: number;
  format?: string;
  layout?: string;
  rows?: number;
  precision?: number;
  prefix?: string;
  suffix?: string;
  minValue?: number;
  maxValue?: number;
  showCondition?: ShowCondition;
  defaultValue?: any;
  logicRule?: string;
  noRepeat?: boolean;
}

/** 容器类型 */
export type ContainerType = 'normal' | 'conditional' | 'autoEntry';

/** 容器配置 */
export interface ContainerConfig {
  mode: ContainerType;
  link?: string;
  fields: DynamicField[];
}

/** 统一错误（替代 7 个分散错误状态 + 2 个脏追踪状态） */
export interface FieldError {
  message: string
  errorType: 'required' | 'format' | 'range' | 'logic' | 'joint'
  dirty: boolean
}

/** 校验错误 */
export interface ValidationError {
  indicatorId?: number;
  indicatorCode: string;
  message: string;
  containerFieldKey?: string;
  errorType?: 'required' | 'format' | 'range' | 'logic' | 'joint';
  indicatorName?: string;
  fieldLabel?: string;
  dirty?: boolean;
}

/** 容器字段校验上下文 */
export interface ContainerFieldContext {
  indicatorId: number;
  containerCode: string;
  entryIndex: number;
  entry: Record<string, any>;
  field: DynamicField;
  fieldCode: string;
  fieldValue: any;
  fieldLabel: string;
  indicatorName: string;
  errors: ValidationError[];
}

/** 指标值（保存时使用） */
export interface IndicatorValue {
  indicatorId: number;
  indicatorCode: string;
  valueType: number;
  valueStr?: string;
  valueNum?: string;
  valueText?: string;
  valueBool?: boolean;
  valueDate?: string;
  valueDateStart?: string;
  valueDateEnd?: string;
}

/** 选项类型 */
export interface OptionItem {
  value: string;
  label: string;
  exclusive?: boolean;
}

// ==================== 上期对比规则类型 ====================

/** 上期对比规则配置 */
export interface PositiveRuleConfig {
  groupName?: string;
  priority?: number;
  rules: PositiveRuleItem[];
}

/** 单条上期对比规则 */
export interface PositiveRuleItem {
  name: string;
  indicatorCode: string;
  valueType: number;
  compareMode: string;
  compareType?: string;
  options?: Array<{ value: string; label: string }>;
  excludeOptions?: string[];
  minNewCount?: number;
}

/** 上期规则校验错误 */
export interface PositiveRuleError {
  indicatorCode: string;
  indicatorId?: number;
  ruleName: string;
  message: string;
  errorKey: string;
}

// ==================== 联动类型 ====================

/** 联动操作符 */
export type LinkageOperator = 'eq' | 'neq' | 'gt' | 'gte' | 'lt' | 'lte' | 'in' | 'notEmpty' | 'isEmpty';

/** 联动类型 */
export type LinkageType = 'show' | 'disabled' | 'required';

/** 联动触发条件 */
export interface LinkageTrigger {
  indicatorCode: string;
  operator: LinkageOperator;
  value?: any;
}

/** 联动配置 */
export interface IndicatorLinkageConfig {
  enabled: boolean;
  type: LinkageType;
  trigger: LinkageTrigger;
}

/** 联动评估结果 */
export interface LinkageEvaluationResult {
  indicatorCode: string;
  type: LinkageType;
  enabled: boolean;
}
