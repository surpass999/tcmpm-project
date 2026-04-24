/**
 * 指标验证引擎
 *
 * 职责：结构化 ParsedRule → ValidationResult[] + 错误消息
 *
 * 子模块：
 * - compareEngine    — 纯函数，比较操作符逻辑（跨 IF/SIMPLE 复用）
 * - evaluateRule     — 单规则执行（分派到条件/动作求值）
 * - evaluateCondition — 条件求值
 * - evaluateAction    — 动作求值
 * - evaluateCC        — CC 规则执行
 * - evaluateFORMATS   — FORMATS 规则执行
 * - buildRuleMessage  — 统一错误消息构建
 */

import type {
  ParsedRule,
  ParsedCCRule,
  ParsedFORMATSRule,
  JointRule,
  RuleConfig,
  ValidationResult,
  EngineOptions,
  IndicatorValuesMap,
  DynamicField,
  CompareSource,
} from './types';
import { ComparisonOperator, COND_OPERATOR_TEXT, OPERATOR_TEXT } from './types';

// ==================== compareEngine（比较引擎子模块） ====================

/**
 * 纯函数：数值比较（用于 >/>=/</<= 操作符）
 */
function compareNumeric(left: number, operator: string, right: number): boolean {
  switch (operator) {
    case ComparisonOperator.GT:  return left > right;
    case ComparisonOperator.GTE: return left >= right;
    case ComparisonOperator.LT:  return left < right;
    case ComparisonOperator.LTE: return left <= right;
    case ComparisonOperator.EQ:  return left === right;
    case ComparisonOperator.NE:  return left !== right;
    default: return true;
  }
}

/**
 * 纯函数：列表成员判断（用于 IN/NOT_IN 操作符）
 */
function compareListValue(left: number, operator: string, list: number[]): boolean {
  const inList = list.includes(left);
  return operator === ComparisonOperator.IN ? inList : !inList;
}

/**
 * 纯函数：主比较入口——根据比较源类型分发
 * @param left 当前值（数值）
 * @param operator 比较操作符
 * @param action 动作配置（包含比较源信息）
 * @param values 指标值映射
 * @param idToCode id → code 映射
 */
function resolveCompareSource(
  left: number,
  operator: string,
  action: { compareType?: CompareSource; compareValue?: number; compareFormula?: any[]; compareIndicatorCode?: string; compareValues?: number[] },
  values: IndicatorValuesMap,
  idToCode?: Map<number, string>,
): boolean {
  if (operator === ComparisonOperator.IN || operator === ComparisonOperator.NOT_IN) {
    return compareListValue(left, operator, action.compareValues ?? []);
  }
  if (operator === ComparisonOperator.EQ || operator === ComparisonOperator.NE) {
    // ==/!= 支持：固定值 | 指标引用 | 公式
    if (action.compareFormula && action.compareFormula.length > 0) {
      const computed = calculateFormula(action.compareFormula, values, idToCode);
      if (computed === undefined) return true;
      return compareNumeric(left, operator, computed);
    }
    if (action.compareIndicatorCode !== undefined) {
      const right = values[action.compareIndicatorCode];
      const rightNum = Array.isArray(right) ? (right.length > 0 ? Number(right[0]) : undefined) : (right !== undefined && right !== null ? Number(right) : undefined);
      if (rightNum === undefined) return true;
      return compareNumeric(left, operator, rightNum);
    }
    return compareNumeric(left, operator, action.compareValue ?? 0);
  }
  // >= / <= / > / <
  if (action.compareFormula && action.compareFormula.length > 0) {
    const computed = calculateFormula(action.compareFormula, values, idToCode);
    if (computed === undefined) return false;
    return compareNumeric(left, operator, computed);
  }
  if (action.compareIndicatorCode !== undefined) {
    const right = values[action.compareIndicatorCode];
    const rightNum = Array.isArray(right) ? (right.length > 0 ? Number(right[0]) : undefined) : (right !== undefined && right !== null ? Number(right) : undefined);
    if (rightNum === undefined) return false;
    return compareNumeric(left, operator, rightNum);
  }
  return compareNumeric(left, operator, action.compareValue ?? 0);
}

// ==================== 公式计算 ====================

/**
 * 计算公式（支持加、减、乘、除）
 * @param formula 公式项数组
 * @param values 指标值Map
 * @param idToCode id → code 映射
 * @returns 计算结果
 */
export function calculateFormula(
  formula: Array<{ valueType: 'indicator' | 'fixed'; indicatorId?: number; indicatorCode?: string; mathOp?: string; value?: number }>,
  values: IndicatorValuesMap,
  idToCode?: Map<number, string>,
): number | undefined {
  const hasAnyItem = formula.some((item) => {
    if (item.valueType === 'fixed') return true;
    const code = item.indicatorCode !== undefined
      ? item.indicatorCode
      : (item.indicatorId !== undefined && idToCode ? idToCode.get(item.indicatorId) : undefined);
    return code !== undefined && values[code] !== undefined;
  });
  if (!hasAnyItem) return undefined;

  let result: number | undefined;

  for (let i = 0; i < formula.length; i++) {
    const item = formula[i]!;

    let itemValue: number | undefined;
    if (item.valueType === 'fixed') {
      itemValue = Number(item.value);
    } else {
      const code = item.indicatorCode !== undefined
        ? item.indicatorCode
        : (item.indicatorId !== undefined && idToCode ? idToCode.get(item.indicatorId) : undefined);
      const v = code !== undefined ? values[code] : undefined;
      const raw = Array.isArray(v) ? (v.length > 0 ? v[0] : undefined) : v;
      itemValue = raw === undefined || raw === null ? undefined : Number(raw);
    }

    if (i === 0) {
      result = itemValue;
    } else {
      const mathOp = item.mathOp || '+';
      switch (mathOp) {
        case '+': result = (result ?? 0) + (itemValue ?? 0); break;
        case '-': result = (result ?? 0) - (itemValue ?? 0); break;
        case '*': result = (result ?? 0) * (itemValue ?? 0); break;
        case '/': result = (itemValue ?? 0) !== 0 ? (result ?? 0) / (itemValue ?? 0) : 0; break;
      }
    }
  }

  return result as number;
}

// ==================== 条件求值 ====================

/**
 * 求值条件表达式
 * @returns true = 条件满足，false = 条件不满足
 */
function evaluateCondition(
  condition: { indicatorCode?: string; indicatorId?: number; operator?: string; value?: number; values?: number[] } | undefined,
  values: IndicatorValuesMap,
  idToCode?: Map<number, string>,
): boolean {
  if (!condition?.operator) return true;

  let condValue: any;
  if (condition.indicatorCode !== undefined) {
    condValue = values[condition.indicatorCode];
  } else if (condition.indicatorId !== undefined) {
    const code = idToCode ? idToCode.get(condition.indicatorId) : undefined;
    condValue = code !== undefined ? values[code] : values[condition.indicatorId];
  } else {
    return true;
  }

  const condNum = Number(condValue);

  switch (condition.operator) {
    case 'not_empty': {
      const isEmptyArray = Array.isArray(condValue) && condValue.length === 0;
      const isEmptyObject = condValue !== null && typeof condValue === 'object' && !Array.isArray(condValue) && Object.keys(condValue).length === 0;
      const isEmpty = condValue === undefined || condValue === null || condValue === ''
        || isEmptyArray || isEmptyObject
        || (typeof condValue === 'string' && condValue.trim() === '');
      return !isEmpty;
    }
    case '>':   return !isNaN(condNum) && condNum > (condition.value ?? 0);
    case '>=':  return !isNaN(condNum) && condNum >= (condition.value ?? 0);
    case '<':   return !isNaN(condNum) && condNum < (condition.value ?? 0);
    case '<=':  return !isNaN(condNum) && condNum <= (condition.value ?? 0);
    case '==': {
      if (Array.isArray(condValue)) {
        if (condValue.length === 0) return false;
        return condValue.some((v) => String(v) === String(condition.value));
      }
      return !isNaN(condNum) && condNum === (condition.value ?? 0);
    }
    case '!=': {
      if (Array.isArray(condValue)) {
        return !condValue.some((v) => String(v) === String(condition.value));
      }
      return !isNaN(condNum) && condNum !== (condition.value ?? 0);
    }
    case 'IN': {
      const targetValues = condition.values ?? [];
      if (Array.isArray(condValue)) {
        return condValue.some((v) => targetValues.includes(Number(v)));
      }
      return targetValues.includes(Number(condValue));
    }
    case 'NOT_IN': {
      const targetValues = condition.values ?? [];
      if (Array.isArray(condValue)) {
        if (condValue.length === 0) return false;
        return !condValue.some((v) => targetValues.includes(Number(v)));
      }
      return !targetValues.includes(Number(condValue));
    }
    default:
      return true;
  }
}

// ==================== 单规则执行 ====================

/**
 * 执行单条规则（IF / SIMPLE）
 * @param rule 解析后的规则
 * @param values 指标值映射
 * @param options 引擎选项
 * @returns 验证结果
 */
export function evaluateRule(
  rule: ParsedRule,
  values: IndicatorValuesMap,
  options?: EngineOptions,
): ValidationResult {
  const idToCode = options?.idToCode;
  const allIndicators = options?.allIndicators;
  const ruleItems = rule.ruleConfig?.rules ?? [];

  for (const ruleItem of ruleItems) {
    const action = ruleItem.action ?? {};
    const condition = ruleItem.condition;

    // 条件判断
    if (!evaluateCondition(condition, values, idToCode)) {
      return { valid: true, ruleId: rule.ruleId, ruleName: rule.ruleName, message: '' };
    }

    // 获取当前指标值
    let currentValue: number | undefined;
    if (action.formula && action.formula.length > 0) {
      const formulaResult = calculateFormula(action.formula, values, idToCode);
      currentValue = Array.isArray(formulaResult) ? (formulaResult.length > 0 ? formulaResult[0] as number : undefined) : formulaResult;
    } else if (action.indicatorCode !== undefined) {
      const v = values[action.indicatorCode];
      if (Array.isArray(v)) {
        currentValue = v.length > 0 ? Number(v[0]) : undefined;
      } else {
        currentValue = v === undefined || v === null ? undefined : Number(v);
      }
    } else if (action.indicatorId !== undefined) {
      const code = idToCode ? idToCode.get(action.indicatorId) : undefined;
      const v = code !== undefined ? values[code] : values[action.indicatorId];
      if (Array.isArray(v)) {
        currentValue = v.length > 0 ? Number(v[0]) : undefined;
      } else {
        currentValue = v === undefined || v === null ? undefined : Number(v);
      }
    } else {
      currentValue = undefined;
    }

    // 获取比较值
    let compareValue: any;
    if (Array.isArray((action as any).compareValues)) {
      compareValue = (action as any).compareValues;
    } else if (action.compareType === 'fixed') {
      compareValue = Number(action.compareValue);
    } else if (action.compareFormula && action.compareFormula.length > 0) {
      const compareFormulaResult = calculateFormula(action.compareFormula, values, idToCode);
      compareValue = Array.isArray(compareFormulaResult) ? (compareFormulaResult.length > 0 ? compareFormulaResult[0] : undefined) : compareFormulaResult;
    } else if (action.compareIndicatorCode !== undefined) {
      const v = values[action.compareIndicatorCode];
      compareValue = Array.isArray(v) ? (v.length > 0 ? v[0] : undefined) : (v === undefined || v === null ? undefined : v);
    } else if (action.compareIndicatorId !== undefined) {
      const code = idToCode ? idToCode.get(action.compareIndicatorId) : undefined;
      const v = code !== undefined ? values[code] : values[action.compareIndicatorId];
      compareValue = Array.isArray(v) ? (v.length > 0 ? v[0] : undefined) : (v === undefined || v === null ? undefined : v);
    } else {
      compareValue = undefined;
    }

    // 跳过空值场景
    const currentIsEmpty = currentValue === undefined || currentValue === null;
    const rightIsDynamic = !!(action.compareFormula || action.compareIndicatorCode !== undefined || action.compareIndicatorId !== undefined);
    const rightIsEmpty = rightIsDynamic && (compareValue === undefined || compareValue === null || (isNaN(Number(compareValue)) && rightIsDynamic));
    if (currentIsEmpty || rightIsEmpty) {
      return { valid: true, ruleId: rule.ruleId, ruleName: rule.ruleName, message: '' };
    }

    // 执行比较
    const operator = action.operator;
    const currentNum = Number(currentValue);

    // IN/NOT_IN 操作符
    if ((operator === 'IN' || operator === 'NOT_IN') && Array.isArray((action as any).compareValues)) {
      const listValues = (action as any).compareValues as number[];
      const valid = currentValue !== undefined && currentValue !== null && !isNaN(currentValue)
        ? (operator === 'NOT_IN' ? !listValues.includes(currentNum) : listValues.includes(currentNum))
        : true;
      if (!valid) {
        return {
          valid: false,
          ruleId: rule.ruleId,
          ruleName: rule.ruleName,
          message: action.message || buildRuleMessage(rule, { values, allIndicators }),
          indicatorCode: action.indicatorCode ?? action.compareIndicatorCode ?? undefined,
          indicatorId: action.indicatorId ?? undefined,
          ruleConfig: JSON.stringify(rule.ruleConfig),
        };
      }
      return { valid: true, ruleId: rule.ruleId, ruleName: rule.ruleName, message: '' };
    }

    // 通用比较
    const passed = resolveCompareSource(currentNum, operator, action as any, values, idToCode);
    if (!passed) {
      // 提取涉及的指标
      const formulaCodes: string[] = [];
      const formulaIds: number[] = [];
      if (action.formula) {
        for (const item of action.formula) {
          if (item.indicatorCode) formulaCodes.push(item.indicatorCode);
          if (item.indicatorId !== undefined) formulaIds.push(item.indicatorId);
        }
      }
      if (action.compareFormula) {
        for (const item of action.compareFormula) {
          if (item.indicatorCode) formulaCodes.push(item.indicatorCode);
          if (item.indicatorId !== undefined) formulaIds.push(item.indicatorId);
        }
      }
      const involvedIndicatorCodes = [
        action.indicatorCode,
        action.compareIndicatorCode,
        ...formulaCodes,
      ].filter(Boolean) as string[];
      const involvedIndicatorIds = [
        action.indicatorId,
        action.compareIndicatorId,
        ...formulaIds,
      ].filter((v): v is number => v !== undefined);

      return {
        valid: false,
        ruleId: rule.ruleId,
        ruleName: rule.ruleName,
        message: action.message || buildRuleMessage(rule, { values, allIndicators }),
        indicatorId: action.indicatorId ?? undefined,
        indicatorCode: action.indicatorCode ?? action.compareIndicatorCode ?? undefined,
        involvedIndicatorIds,
        involvedIndicatorCodes,
        ruleConfig: JSON.stringify(rule.ruleConfig),
      };
    }
  }

  return { valid: true, ruleId: rule.ruleId, ruleName: rule.ruleName, message: '' };
}

// ==================== 验证入口（兼容 JointRule[]） ====================

/**
 * 执行验证（兼容 legacy JointRule[] 输入）
 * @param rules 验证规则列表
 * @param values 指标值Map
 * @param options 验证选项
 * @returns 验证结果数组（只包含失败的）
 */
export function validate(
  rules: JointRule[],
  values: IndicatorValuesMap,
  options: EngineOptions = {},
): ValidationResult[] {
  const results: ValidationResult[] = [];

  const filteredRules = rules.filter((rule) => {
    if (options.triggerTiming && rule.triggerTiming !== undefined && rule.triggerTiming !== options.triggerTiming) return false;
    if (options.processNode) {
      if (rule.processNode && rule.processNode !== options.processNode) return false;
    }
    return true;
  });

  for (const rule of filteredRules) {
    // 将 JointRule 转为 ParsedRule
    try {
      let config: RuleConfig;
      if (typeof rule.ruleConfig === 'string') {
        // legacy JointRule[]：ruleConfig 是 JSON 字符串
        config = JSON.parse(rule.ruleConfig);
      } else {
        // parseLogicRule 返回的 ParsedRule[]：ruleConfig 已是对象
        config = rule.ruleConfig as unknown as RuleConfig;
      }
      const parsedRule: ParsedRule = {
        type: config.rules?.[0]?.condition ? 'IF' : 'SIMPLE',
        raw: '',
        ruleConfig: config,
        ruleName: rule.ruleName ?? '',
        ruleId: rule.id,
        verifyIndicatorCode: rule.verifyIndicatorCode,
      };
      const result = evaluateRule(parsedRule, values, options);
      if (!result.valid) {
        results.push({ ...result, ruleConfig: rule.ruleConfig });
      }
    } catch (e) {
      console.error('[engine] validate: parse error', e);
    }
  }

  return results;
}

/**
 * 验证单条规则（导出供外部直接调用）
 */
export function validateSingleRule(
  rule: JointRule,
  values: IndicatorValuesMap,
): ValidationResult {
  const config: RuleConfig = JSON.parse(rule.ruleConfig);
  const parsedRule: ParsedRule = {
    type: 'SIMPLE',
    raw: '',
    ruleConfig: config,
    ruleName: rule.ruleName ?? '',
    ruleId: rule.id,
  };
  return evaluateRule(parsedRule, values, {});
}

// ==================== CC 规则执行 ====================

/**
 * 验证容器级联互斥约束
 *
 * 公式：CC([602_07,602_06,602_05], FIRST_EQ(2), REST_NE(1))
 * 语义：完整扫描字段列表，找到第一个满足 triggerOp(triggerVal) 的字段，
 *       约束其之后所有字段不能选 forbidVal。
 *
 * @param rule 解析后的 CC 规则
 * @param entry 条目数据
 * @param fieldDefs 字段定义列表
 * @returns 违规字段的错误列表
 */
export function evaluateCC(
  rule: ParsedCCRule,
  entry: Record<string, any>,
  fieldDefs: DynamicField[],
): Array<{ fieldKey: string; errMsg: string }> {
  const { fieldCodes, originalFieldCodes, triggerOp, triggerVal } = rule;
  const errors: Array<{ fieldKey: string; errMsg: string }> = [];

  const fieldMap: Record<string, DynamicField> = {};
  for (const f of fieldDefs) fieldMap[f.fieldCode] = f;

  // 收集有效字段（过滤空值）
  const validFields: { code: string; originalCode: string; value: string }[] = [];
  for (let i = 0; i < fieldCodes.length; i++) {
    let val: any = entry[fieldCodes[i]!];
    if (Array.isArray(val)) val = val.length > 0 ? val[0] : undefined;
    if (val === undefined || val === null || val === '') continue;
    validFields.push({ code: fieldCodes[i]!, originalCode: originalFieldCodes[i]!, value: String(val) });
  }

  if (validFields.length === 0) return [];

  // 判断触发条件
  const matchesTrigger = (val: string): boolean => {
    switch (triggerOp) {
      case 'FIRST_EQ': case 'LAST_EQ': return val === triggerVal;
      case 'FIRST_GT': return !isNaN(Number(val)) && Number(val) > Number(triggerVal);
      case 'FIRST_GTE': return !isNaN(Number(val)) && Number(val) >= Number(triggerVal);
      case 'FIRST_LT': return !isNaN(Number(val)) && Number(val) < Number(triggerVal);
      case 'FIRST_LTE': return !isNaN(Number(val)) && Number(val) <= Number(triggerVal);
      case 'FIRST_IN': case 'LAST_IN': return triggerVal.split(',').map((v) => v.trim()).includes(val);
      default: return false;
    }
  };

  // 找触发字段索引
  let triggerIdx = -1;
  if (triggerOp.startsWith('FIRST_')) {
    for (let i = 0; i < validFields.length; i++) {
      if (matchesTrigger(validFields[i]!.value)) { triggerIdx = i; break; }
    }
  } else {
    for (let i = validFields.length - 1; i >= 0; i--) {
      if (matchesTrigger(validFields[i]!.value)) { triggerIdx = i; break; }
    }
  }
  if (triggerIdx === -1) return [];

  // 锁定值：后续所有字段必须与触发值一致
  for (let i = triggerIdx + 1; i < validFields.length; i++) {
    const { code, originalCode, value } = validFields[i]!;
    if (value !== triggerVal) {
      const triggerOrigCode = validFields[triggerIdx]!.originalCode;
      const triggerFieldCode = triggerOrigCode.split('_').pop() ?? triggerOrigCode;
      const forbidFieldCode = originalCode.split('_').pop() ?? originalCode;
      const triggerFieldLabel = fieldMap[triggerFieldCode]?.fieldLabel ?? triggerOrigCode;
      const forbidFieldLabel = fieldMap[forbidFieldCode]?.fieldLabel ?? originalCode;
      const triggerValueLabel = (() => {
        const field = fieldMap[triggerFieldCode];
        if (field?.options) {
          const found = field.options.find((o) => o.value === triggerVal || String(o.value) === triggerVal);
          if (found) return found.label;
        }
        return triggerVal;
      })();
      errors.push({
        fieldKey: code,
        errMsg: `「${triggerFieldLabel}」选择了「${triggerValueLabel}」，「${forbidFieldLabel}」必须选择「${triggerValueLabel}」`,
      });
      return errors;
    }
  }

  return errors;
}

// ==================== FORMATS 规则执行 ====================

/**
 * 验证文件格式多样性规则
 * @param rule 解析后的 FORMATS 规则
 * @param files 文件列表
 * @returns 缺失的格式类别列表
 */
export function evaluateFORMATS(
  rule: ParsedFORMATSRule,
  files: Array<{ name: string }>,
): string[] {
  if (files.length === 0) return [];

  const extToGroup: Record<string, string> = {};
  for (const [groupName, extensions] of Object.entries(rule.typeGroups)) {
    for (const ext of extensions) extToGroup[ext.toLowerCase()] = groupName.toLowerCase();
  }

  const uploadedGroups = new Set<string>();
  for (const file of files) {
    const ext = file.name?.split('.').pop()?.toLowerCase() || '';
    if (extToGroup[ext]) {
      uploadedGroups.add(extToGroup[ext]);
    } else if (Object.keys(rule.typeGroups).length === 0) {
      uploadedGroups.add(ext);
    }
  }

  return rule.requiredFormats
    .map((f) => f.toLowerCase())
    .filter((f) => !uploadedGroups.has(f));
}

// ==================== 辅助函数 ====================

/** 判断值是否为空 */
export function isEmptyValue(value: any): boolean {
  if (value === undefined || value === null || value === '') return true;
  if (Array.isArray(value) && value.length === 0) return true;
  if (typeof value === 'object' && !Array.isArray(value) && Object.keys(value).length === 0) return true;
  if (typeof value === 'string' && value.trim() === '') return true;
  return false;
}

/** 从数组值中提取第一个数值 */
export function extractFirstNumber(value: any): number | undefined {
  if (Array.isArray(value)) return value.length > 0 ? Number(value[0]) : undefined;
  return value === undefined || value === null ? undefined : Number(value);
}

/** 查找指标的 label */
function findIndicatorLabel(code: string, allIndicators?: Array<Record<string, any>>): string {
  if (!allIndicators) return code;
  const ind = allIndicators.find((i) => i.indicatorCode === code);
  if (!ind) return code;
  // 优先用 indicatorName，否则尝试 title/name 等常见字段
  const name = ind.indicatorName ?? ind.name ?? ind.title ?? ind.label ?? '';
  return name ? `${code} - ${name}` : code;
}

/** 查找选项的 label */
function findOptionLabel(
  code: string,
  value: string | number,
  allIndicators?: Array<Record<string, any>>,
): string {
  if (!allIndicators) return String(value);
  const ind = allIndicators.find((i) => i.indicatorCode === code);
  if (ind?.valueOptions) {
    try {
      const opts = JSON.parse(ind.valueOptions);
      if (Array.isArray(opts) && opts[0]?.label !== undefined) {
        const opt = opts.find((o: any) => String(o.value) === String(value) || o.value === value);
        if (opt) return opt.label;
      }
    } catch { /* ignore */ }
  }
  return String(value);
}

/** 获取当前值文本 */
function getCurrentValueText(code: string, values: IndicatorValuesMap): string {
  const val = values[code];
  if (val === undefined || val === null || val === '') return '未填';
  if (Array.isArray(val)) return val.join('、');
  return String(val);
}

// ==================== 错误消息构建 ====================

export interface MessageContext {
  values: IndicatorValuesMap;
  allIndicators?: Array<Record<string, any>>;
  entryNumber?: number;
  /** 原始 ruleConfig JSON 字符串（优先使用，用于生成友好消息） */
  ruleConfigStr?: string;
}

/**
 * 统一错误消息构建（基于 ParsedRule 结构化数据）
 * @param rule 解析后的规则
 * @param ctx 消息构建上下文
 * @returns 错误消息文本
 */
export function buildRuleMessage(
  rule: ParsedRule,
  ctx: MessageContext,
): string {
  const { ruleConfigStr } = ctx;

  // 如果有原始 JSON，优先使用（使用 ctx.ruleConfigStr）
  const rawCfg = ruleConfigStr;
  if (rawCfg) {
    try {
      const config = typeof rawCfg === 'string' ? JSON.parse(rawCfg) : rawCfg;
      const result = buildMsgFromConfig(config, rule as ParsedRule, ctx);
      if (result) return result;
    } catch { /* ignore */ }
  }

  if (rule.type === 'IF') return buildIFMessage(rule, ctx);
  if (rule.type === 'SIMPLE') return buildSimpleMessage(rule, ctx);
  return rule.ruleName || '校验失败';
}

/**
 * 从原始 ruleConfig JSON 构建错误消息（使用旧正则逻辑，保持消息格式一致）
 */
function buildMsgFromConfig(
  config: any,
  rule: ParsedRule,
  ctx: MessageContext,
): string | null {
  const { values, allIndicators } = ctx;

  const findIndicator = (code: string) => allIndicators?.find((i: any) => i.indicatorCode === code);

  const getIndicatorLabel = (code: string): string => {
    const ind = findIndicator(code);
    if (!ind) return code;
    return ind?.indicatorName ? `${code} - ${ind.indicatorName}` : code;
  };
  const getOptionLabel = (indicator: any, val: string): string => {
    if (!indicator) return val;
    try {
      const opts = JSON.parse(indicator.valueOptions || '[]');
      if (Array.isArray(opts) && opts[0]?.label !== undefined) {
        const opt = opts.find((o: any) => String(o.value) === val || o.value === val);
        return opt?.label ?? val;
      }
    } catch { /* ignore */ }
    return val;
  };
  const getMultiOptionLabels = (indicator: any, valList: string[]): string => {
    if (!indicator) return valList.map((v) => `(${v})`).join('、');
    const optsRaw = indicator.valueOptions;
    // 如果没有选项，格式化为 code(val)
    if (optsRaw === null || optsRaw === undefined || optsRaw === '') {
      return valList.map((v) => `(${v})`).join('、');
    }
    return valList.map((v) => getOptionLabel(indicator, v)).join('、');
  };

  const rules = config?.rules ?? [];
  if (!rules.length) return null;
  const item = rules[0];
  const action = item?.action;
  const condition = item?.condition;
  if (!action) return null;

  // 分支一：IN/NOT_IN 格式的 IF 规则
  if (condition && (action.operator === 'IN' || action.operator === 'NOT_IN')) {
    const condCode = condition.indicatorCode ?? '';
    const condIndicator = findIndicator(condCode);
    // 用用户的实际选择值，而非规则中定义的全部条件值
    const rawUserVal = values?.[condCode];
    const userVals = Array.isArray(rawUserVal) ? rawUserVal
      : (rawUserVal !== undefined && rawUserVal !== null ? [rawUserVal] : []);
    const condLabelStr = userVals.length > 0
      ? getMultiOptionLabels(condIndicator, userVals.map(String))
      : '选择值';
    // action.indicatorCode 可能为空（如 802），用 rule.verifyIndicatorCode fallback
    let verifyCode = action.indicatorCode || action.compareIndicatorCode || rule.verifyIndicatorCode || '';
    // fallback：verifyCode 为空时，从 ruleName 解析 [verifyCode][condCode]
    if (!verifyCode) {
      const ruleName = item?.name ?? rule.ruleName ?? '';
      const m = ruleName.match(/\[(\w+)\]/);
      if (m) verifyCode = m[1] ?? '';
    }
    const verifyVals = (action.compareValues ?? []).map(String).join(',');
    const verifyValsList = verifyVals.split(',').map((v: string) => v.trim());
    const condLabel = getIndicatorLabel(condCode);
    const verifyLabel = getIndicatorLabel(verifyCode);
    const verifyOpText = action.operator === 'NOT_IN' ? '不能选择' : '必须选择';
    // 优先用 allIndicators 查找 verifyIndicator，找不到时从 values keys 中推断
    const verifyIndicator = findIndicator(verifyCode) ?? (Object.keys(values ?? {}).includes(verifyCode)
      ? { indicatorCode: verifyCode, valueOptions: null }
      : { indicatorCode: verifyCode, valueOptions: null });
    return `当 ${condLabel}选中了 ${condLabelStr} 时，${verifyLabel} ${verifyOpText} ${getMultiOptionLabels(verifyIndicator, verifyValsList)}`;
  }

  // 分支二：==/!= 格式的 IF 规则
  if (condition && (action.operator === '==' || action.operator === '!=')) {
    const condCode = condition.indicatorCode ?? '';
    const condVal = String(condition.value ?? '');
    const condIndicator = findIndicator(condCode);
    // 用 || 而非 ??，确保空字符串也能回退
    let verifyCode = action.indicatorCode || action.compareIndicatorCode || rule.verifyIndicatorCode || '';
    let verifyVal = String(action.compareValue ?? '');
    // fallback：code/value 为空时，从 ruleName 解析 "[verifyCode] == val 时, [condCode] == val" 或 "[condCode] == val 时, [verifyCode] == val"
    if (!verifyCode || !verifyVal) {
      const ruleName = item?.name ?? rule.ruleName ?? '';
      // 格式: "[code1] == val1 时, [code2] == val2"
      const parts = ruleName.split(/时,\s*/);
      if (parts.length >= 2) {
        const lastPart = parts[parts.length - 1]!.trim(); // [verifyCode] ==7
        const condPart = parts[0]!.trim();                 // [801] == 11
        const verifyMatch = lastPart.match(/^\[(\w+)\]\s*(==|!=)\s*(\S+)/);
        const condMatch = condPart.match(/^\[(\w+)\]\s*(==|!=)\s*(\S+)/);
        if (verifyMatch) {
          if (!verifyCode) verifyCode = verifyMatch[1] ?? '';
          if (!verifyVal) verifyVal = (verifyMatch[3] ?? '').replace(/^=+/, '');
        }
        if (condMatch) {
          // 如果 action 里 condCode 为空，也从 ruleName 拿
          if (!condCode && condition) condition.indicatorCode = condMatch[1] ?? '';
        }
      }
    }
    const condValLabel = getOptionLabel(condIndicator, condVal);
    const verifyValLabel = getOptionLabel(findIndicator(verifyCode), verifyVal);
    const condLabel = getIndicatorLabel(condCode);
    const verifyLabel = getIndicatorLabel(verifyCode);
    const condOpText = action.operator === '==' ? '等于' : '不等于';
    const verifyOpText = action.operator === '==' ? '必须等于' : '不能等于';
    return `当 ${condLabel} ${condOpText} ${condValLabel} 时，${verifyLabel} ${verifyOpText} ${verifyValLabel}`;
  }

  // 分支三：基本比较操作符（>= / <= / > / <）
  if (['>=', '<=', '>', '<'].includes(action.operator)) {
    const leftCode = action.formula?.[0]?.indicatorCode ?? '';
    const rightFixed = action.compareValue;
    const rightFormula = action.compareFormula;
    const leftLabel = getIndicatorLabel(leftCode);
    const opTextMap: Record<string, string> = {
      '>=': '应大于等于',
      '<=': '应小于等于',
      '>': '应大于',
      '<': '应小于',
    };
    const opText = opTextMap[action.operator] ?? action.operator;

    if (rightFormula && rightFormula.length > 1) {
      // 公式右侧
      const rightParts: string[] = [];
      let rightTotal = 0;
      for (const item of rightFormula) {
        if (item.indicatorCode) {
          const code = item.indicatorCode;
          const label = getIndicatorLabel(code);
          const numVal = values[code] !== undefined && values[code] !== null ? Number(values[code]) : 0;
          const prefix = item.mathOp === '-' ? '-' : (rightParts.length > 0 ? '+' : '');
          rightParts.push(`${prefix}${label}(${numVal})`);
          rightTotal += numVal;
        }
      }
      const rightFriendlyText = rightParts.join('') + ` = ${rightTotal}`;
      const leftVal = values[leftCode];
      const leftValText = leftVal !== undefined && leftVal !== null ? String(leftVal) : '未填';
      return `${leftLabel}(${leftValText}) ${opText} ${rightFriendlyText}`;
    }

    if (rightFormula && rightFormula.length === 1) {
      // 单指标右侧引用
      const rightItem = rightFormula[0]!;
      if (rightItem.indicatorCode) {
        const rightCode = rightItem.indicatorCode;
        const rightLabel = getIndicatorLabel(rightCode);
        const rightVal = values[rightCode];
        const rightValText = rightVal !== undefined && rightVal !== null ? String(rightVal) : '未填';
        const leftVal = values[leftCode];
        const leftValText = leftVal !== undefined && leftVal !== null ? String(leftVal) : '未填';
        return `${leftLabel}(${leftValText}) ${opText} ${rightLabel}(${rightValText})`;
      }
      if (rightItem.value !== undefined) {
        const leftVal = values[leftCode];
        const leftValText = leftVal !== undefined && leftVal !== null ? String(leftVal) : '未填';
        return `${leftLabel}(${leftValText}) ${opText} ${rightItem.value}`;
      }
    }

    if (rightFixed !== undefined) {
      const leftVal = values[leftCode];
      const leftValText = leftVal !== undefined && leftVal !== null ? String(leftVal) : '未填';
      return `${leftLabel}(${leftValText}) ${opText} ${rightFixed}`;
    }

    // 右侧是指标引用
    const rightCode = action.compareIndicatorCode || rule.verifyIndicatorCode || '';
    const rightLabel = getIndicatorLabel(rightCode);
    return `${leftLabel} ${opText} ${rightLabel}`;
  }

  // 分支四：== / != 带固定值
  if (action.operator === '==' || action.operator === '!=') {
    const leftCode = action.formula?.[0]?.indicatorCode ?? '';
    const label = getIndicatorLabel(leftCode);
    const opText = action.operator === '==' ? '应等于' : '应不等于';
    let compareVal = action.compareValue ?? 0;
    // fallback：compareValue 可能为 0，从 ruleName 解析
    if (compareVal === 0) {
      const ruleName = item?.name ?? '';
      const actionMatch = ruleName.match(/\]?\s*(==|!=)\s*(\d+)/);
      if (actionMatch) {
        compareVal = Number(actionMatch[2] ?? 0);
      }
    }
    return `${label} ${opText} ${compareVal}`;
  }

  return null;
}

function buildIFMessage(rule: ParsedRule, ctx: MessageContext): string {
  const { values, allIndicators } = ctx;
  const ruleItem = rule.ruleConfig?.rules?.[0];
  if (!ruleItem) return rule.ruleName;

  const condition = ruleItem.condition;
  const action = ruleItem.action;
  if (!condition || !action) return rule.ruleName;

  const condCode = condition.indicatorCode ?? '';
  const condOp = condition.operator ?? '';
  const isListOp = condOp === 'IN' || condOp === 'NOT_IN';
  const condLabel = findIndicatorLabel(condCode, allIndicators);
  const condOpText = COND_OPERATOR_TEXT[condOp] ?? condOp;

  if (isListOp) {
    const rawUserVal = values?.[condCode];
    const userVals = Array.isArray(rawUserVal) ? rawUserVal
      : (rawUserVal !== undefined && rawUserVal !== null ? [rawUserVal] : []);
    const condVals = userVals.length > 0 ? userVals : (condition.values ?? []);
    const condValText = condVals.map((v) => findOptionLabel(condCode, Number(v), allIndicators)).join('、');
    const verifyCode = action.indicatorCode || action.compareIndicatorCode || rule.verifyIndicatorCode || '';
    const verifyLabel = findIndicatorLabel(verifyCode, allIndicators);
    const verifyOpText = action.operator === 'NOT_IN' ? '不能选择' : '必须选择';
    const actionValues = (action as any).compareValues as number[] | undefined;
    const actionValText = actionValues
      ? actionValues.map((v) => findOptionLabel(verifyCode, v, allIndicators)).join('、')
      : '';
    return `当 ${condLabel} ${condOpText} ${condValText} 时，${verifyLabel} ${verifyOpText} ${actionValText}`;
  }

  // 基本比较
  const condVal = condition.value ?? 0;
  const condValText = findOptionLabel(condCode, condVal, allIndicators);
  const verifyCode = action.indicatorCode || action.compareIndicatorCode || rule.verifyIndicatorCode || '';
  const verifyLabel = findIndicatorLabel(verifyCode, allIndicators);
  const verifyOpText = COND_OPERATOR_TEXT[action.operator] ?? action.operator;
  const verifyVal = action.compareValue ?? 0;
  const verifyValText = findOptionLabel(verifyCode, verifyVal, allIndicators);
  return `当 ${condLabel} ${condOpText} ${condValText} 时，${verifyLabel} ${verifyOpText} ${verifyValText}`;
}

function buildSimpleMessage(rule: ParsedRule, ctx: MessageContext): string {
  const { values, allIndicators } = ctx;
  const ruleItem = rule.ruleConfig?.rules?.[0];
  if (!ruleItem) return rule.ruleName;

  const action = ruleItem.action ?? {};
  const leftCode = action.formula?.[0]?.indicatorCode ?? '';
  const leftLabel = findIndicatorLabel(leftCode, allIndicators);
  const leftValText = getCurrentValueText(leftCode, values);
  const opText = OPERATOR_TEXT[action.operator as keyof typeof OPERATOR_TEXT] ?? action.operator;

  // 公式右侧
  if (action.compareFormula && action.compareFormula.length > 1) {
    const rightParts: string[] = [];
    let rightTotal = 0;
    for (const item of action.compareFormula) {
      if (item.indicatorCode) {
        const code = item.indicatorCode;
        const label = findIndicatorLabel(code, allIndicators);
        const val = values[code];
        const numVal = val !== undefined && val !== null && val !== '' ? Number(val) : 0;
        const prefix = item.mathOp === '-' ? '-' : (rightParts.length > 0 ? '+' : '');
        rightParts.push(`${prefix}${label}(${numVal})`);
        rightTotal += numVal;
      } else if (item.value !== undefined) {
        const prefix = item.mathOp === '-' ? '-' : (rightParts.length > 0 ? '+' : '');
        rightParts.push(`${prefix}${item.value}`);
        rightTotal += item.value;
      }
    }
    return `${leftLabel}(${leftValText}) ${opText} ${rightParts.join(' ')} = ${rightTotal}`;
  }

  // 单指标右侧（compareFormula 长度=1 或 compareIndicatorCode）
  if (action.compareFormula && action.compareFormula.length === 1) {
    const item = action.compareFormula[0]!;
    if (item.indicatorCode) {
      const rightCode = item.indicatorCode;
      const rightLabel = findIndicatorLabel(rightCode, allIndicators);
      const rightValText = getCurrentValueText(rightCode, values);
      return `${leftLabel}(${leftValText}) ${opText} ${rightLabel}(${rightValText})`;
    }
    if (item.value !== undefined) {
      return `${leftLabel}(${leftValText}) ${opText} ${item.value}`;
    }
  }

  // 指标右侧（带当前值）
  if (action.compareIndicatorCode) {
    const rightLabel = findIndicatorLabel(action.compareIndicatorCode, allIndicators);
    const rightValText = getCurrentValueText(action.compareIndicatorCode, values);
    return `${leftLabel}(${leftValText}) ${opText} ${rightLabel}(${rightValText})`;
  }

  // 指标右侧（compareIndicatorId，未转换为 code 时）
  if (action.compareIndicatorId !== undefined && allIndicators) {
    const idToCode = new Map<string, string>();
    for (const ind of allIndicators) {
      if (ind.indicatorId !== undefined && ind.indicatorCode) {
        idToCode.set(String(ind.indicatorId), ind.indicatorCode);
      }
    }
    const rightCode = idToCode.get(String(action.compareIndicatorId));
    if (rightCode) {
      const rightLabel = findIndicatorLabel(rightCode, allIndicators);
      const rightValText = getCurrentValueText(rightCode, values);
      return `${leftLabel}(${leftValText}) ${opText} ${rightLabel}(${rightValText})`;
    }
  }

  // 固定值右侧
  const fixedVal = action.compareValue ?? 0;
  return `${leftLabel}(${leftValText}) ${opText} ${fixedVal}`;
}
