/**
 * indicator-validator 模块导出
 *
 * 子模块：
 * - types.ts  — 操作符枚举 + 类型定义
 * - parser.ts — 规则字符串解析器
 * - engine.ts — 验证执行引擎
 */

// 类型导出（供外部引用）
export * from './types';

// parser 导出
export {
  parseContainerFieldShortcut,
  isContainerFieldShortcut,
  detectRuleType,
  parseConditionOperator,
  parseFormulaSide,
  parseCC,
  parseFORMATS,
  parseLogicRule,
} from './parser';

// engine 导出
export {
  calculateFormula,
  evaluateRule,
  evaluateCC,
  evaluateFORMATS,
  validate,
  validateSingleRule,
  buildRuleMessage,
  isEmptyValue,
  extractFirstNumber,
} from './engine';
export type { MessageContext } from './engine';
