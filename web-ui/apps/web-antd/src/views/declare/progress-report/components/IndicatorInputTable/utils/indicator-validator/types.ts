/**
 * IndicatorInputTable 验证引擎类型系统
 *
 * 模块划分：
 * - types.ts    → 所有验证相关类型 + 操作符枚举
 * - parser.ts   → 字符串解析器：规则字符串 → 结构化对象
 * - engine.ts   → 验证执行引擎：结构化对象 → ValidationResult[]
 */

// ==================== 操作符枚举 ====================

/** 条件/动作比较操作符 */
export enum ComparisonOperator {
  EQ    = '==',
  NE    = '!=',
  GT    = '>',
  GTE   = '>=',
  LT    = '<',
  LTE   = '<=',
  IN    = 'IN',
  NOT_IN = 'NOT_IN',
}

/** 特殊条件操作符（非比较语义）*/
export enum SpecialOperator {
  NOT_EMPTY = 'not_empty',
  FORMULA   = '__formula__',
}

/** 操作符并集（用于类型守卫）*/
export type AnyOperator = ComparisonOperator | SpecialOperator;

/** 操作符 → 友好文字映射（用于错误消息） */
export const OPERATOR_TEXT: Record<AnyOperator, string> = {
  [ComparisonOperator.GTE]:    '应大于等于',
  [ComparisonOperator.LTE]:    '应小于等于',
  [ComparisonOperator.GT]:     '应大于',
  [ComparisonOperator.LT]:     '应小于',
  [ComparisonOperator.EQ]:     '应等于',
  [ComparisonOperator.NE]:     '不应等于',
  [ComparisonOperator.IN]:     '必须选择',
  [ComparisonOperator.NOT_IN]: '不能选择',
  [SpecialOperator.NOT_EMPTY]: '必须有值',
};

/** 条件操作符 → 友好文字映射（用于条件描述） */
export const COND_OPERATOR_TEXT: Record<string, string> = {
  '==':    '等于',
  '!=':    '不等于',
  '>':     '大于',
  '>=':    '大于等于',
  '<':     '小于',
  '<=':    '小于等于',
  'IN':    '在列表中',
  'NOT_IN': '不在列表中',
};

/** CC 触发器操作符 */
export enum TriggerOperator {
  FIRST_EQ  = 'FIRST_EQ',
  LAST_EQ   = 'LAST_EQ',
  FIRST_GT  = 'FIRST_GT',
  FIRST_GTE = 'FIRST_GTE',
  FIRST_LT  = 'FIRST_LT',
  FIRST_LTE = 'FIRST_LTE',
  FIRST_IN  = 'FIRST_IN',
  LAST_IN   = 'LAST_IN',
}

/** CC 禁止操作符 */
export enum ForbidOperator {
  REST_EQ = 'REST_EQ',
  REST_NE = 'REST_NE',
}

// ==================== 比较源类型枚举 ====================

/** 动作右侧值的来源类型 */
export enum CompareSource {
  FIXED    = 'fixed',
  INDICATOR = 'indicator',
  FORMULA  = 'formula',
  LIST     = 'list',
}

// ==================== 规则类型枚举 ====================

/** 规则类型 */
export type RuleType = 'IF' | 'SIMPLE' | 'CC' | 'FORMATS';

// ==================== 引擎核心类型 ====================

/** 公式项 */
export interface FormulaItem {
  valueType: 'indicator' | 'fixed';
  indicatorId?: number;
  indicatorCode?: string;
  indicatorName?: string;
  mathOp?: '+' | '-' | '*' | '/';
  value?: number;
}

/** 条件配置 */
export interface RuleCondition {
  indicatorCode?: string;
  indicatorId?: number;
  operator: string;
  value?: number;
  values?: number[];
}

/** 动作配置 */
export interface RuleAction {
  type: 'formula' | 'condition';
  level?: number;
  message: string;
  operator: string;
  compareType?: 'indicator' | 'fixed';
  compareValue?: number;
  indicatorCode?: string;
  indicatorId?: number;
  indicatorName?: string;
  compareIndicatorCode?: string;
  compareIndicatorId?: number;
  compareIndicatorName?: string;
  formula?: FormulaItem[];
  compareFormula?: FormulaItem[];
  /** IN/NOT_IN 操作符对应的值列表 */
  compareValues?: number[];
}

/** 单条规则配置 */
export interface RuleConfigItem {
  id?: number;
  name: string;
  ruleType?: number;
  condition?: RuleCondition;
  action: RuleAction;
}

/** 规则配置（解析后的 JSON） */
export interface RuleConfig {
  groupName?: string;
  priority?: number;
  rules: RuleConfigItem[];
}

/** 解析后的规则（parser 输出，engine 输入） */
export interface ParsedRule {
  type: RuleType;
  raw: string;
  ruleConfig: RuleConfig;
  /** 原始 ruleConfig JSON 字符串（用于错误消息构建） */
  ruleConfigStr?: string;
  ruleName: string;
  ruleId: number;
  verifyIndicatorCode?: string;
}

/** 解析后的动作 */
export interface ParsedAction {
  operator: string;
  formula?: FormulaItem[];
  currentIndicatorCode?: string;
  compareType?: CompareSource;
  compareValue?: number;
  compareFormula?: FormulaItem[];
  compareIndicatorCode?: string;
  compareValues?: number[];
}

/** CC 规则 */
export interface ParsedCCRule {
  type: 'CC';
  fieldCodes: string[];
  originalFieldCodes: string[];
  triggerOp: string;
  triggerVal: string;
  forbidOp: string;
  forbidVal: string;
}

/** FORMATS 规则 */
export interface ParsedFORMATSRule {
  type: 'FORMATS';
  typeGroups: Record<string, string[]>;
  requiredFormats: string[];
}

// ==================== 验证结果类型 ====================

/** 验证结果 */
export interface ValidationResult {
  valid: boolean;
  ruleId: number;
  ruleName: string;
  message: string;
  indicatorId?: number;
  indicatorCode?: string;
  involvedIndicatorIds?: number[];
  involvedIndicatorCodes?: string[];
  ruleConfig?: string;
}

/** CC 规则违规 */
export interface CCViolation {
  fieldKey: string;
  errMsg: string;
}

// ==================== 引擎选项 ====================

export interface EngineOptions {
  triggerTiming?: 'FILL' | 'PROCESS_SUBMIT';
  processNode?: string;
  changedIndicatorId?: number;
  idToCode?: Map<number, string>;
  allIndicators?: Array<Record<string, any>>;
}

// ==================== 原始规则类型（从后端获取） ====================

export interface JointRule {
  id: number;
  ruleName?: string;
  projectType?: number;
  triggerTiming?: 'FILL' | 'PROCESS_SUBMIT';
  processNode?: string;
  ruleConfig: string;
  status?: number;
  createTime?: string;
  verifyIndicatorCode?: string;
}

// ==================== 值映射类型 ====================

export type IndicatorValuesMap = Record<string | number, any>;

// ==================== 字段定义类型 ====================

export interface DynamicField {
  fieldCode: string;
  fieldLabel: string;
  fieldType: string;
  options?: Array<{ value: any; label: string }>;
}
