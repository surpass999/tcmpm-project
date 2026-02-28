/**
 * 指标联合验证引擎
 *
 * 支持：
 * - FILL: 填报时实时验证
 * - PROCESS_SUBMIT: 流程提交时验证
 * - 支持多指标公式计算（加、减、乘、除）
 * - 支持多流程节点验证
 */

// ==================== 类型定义 ====================

// 验证规则（从后端获取）
export interface JointRule {
  id: number;
  ruleName: string;
  projectType: number;
  triggerTiming: 'FILL' | 'PROCESS_SUBMIT';
  processNode?: string;
  ruleConfig: string;
  status: number;
  createTime?: string;
}

// 指标值Map: key 为 indicatorId，value 为指标值
export type IndicatorValuesMap = Record<number, any>;

// 验证结果
export interface ValidationResult {
  valid: boolean;
  ruleId: number;
  ruleName: string;
  message: string;
  indicatorId?: number;
  indicatorCode?: string;
  // 新增：涉及的所有指标ID
  involvedIndicatorIds?: number[];
  involvedIndicatorCodes?: string[];
}

// 验证引擎选项
export interface ValidatorOptions {
  triggerTiming?: 'FILL' | 'PROCESS_SUBMIT';
  processNode?: string;
  changedIndicatorId?: number;
}

// 规则配置中的公式项
export interface FormulaItem {
  valueType: 'indicator' | 'fixed';
  indicatorId?: number;
  indicatorCode?: string;
  indicatorName?: string;
  mathOp?: '+' | '-' | '*' | '/';
  value?: number;
}

// 规则配置中的动作
export interface RuleAction {
  type: 'formula' | 'condition';
  level?: number;
  message: string;
  operator: string;
  compareType?: 'indicator' | 'fixed';
  compareValue?: number;
  compareIndicatorId?: number;
  compareIndicatorName?: string;
  formula?: FormulaItem[];
}

// 单条规则配置
export interface RuleConfigItem {
  name: string;
  ruleType: number;
  condition?: {
    indicatorName?: string;
    operator?: string;
  };
  action: RuleAction;
}

// 规则配置（解析后的JSON）
export interface RuleConfig {
  groupName?: string;
  priority?: number;
  rules: RuleConfigItem[];
}

// ==================== 验证实现 ====================

/**
 * 执行验证
 * @param rules 验证规则列表
 * @param values 指标值Map
 * @param options 验证选项
 * @returns 验证结果数组
 */
export function validate(
  rules: JointRule[],
  values: IndicatorValuesMap,
  options: ValidatorOptions = {}
): ValidationResult[] {
  const results: ValidationResult[] = [];

  // 1. 根据触发时机和流程节点过滤规则
  const filteredRules = rules.filter((rule) => {
    // 过滤触发时机
    if (options.triggerTiming && rule.triggerTiming !== options.triggerTiming) {
      return false;
    }
    // 过滤流程节点
    if (options.processNode) {
      // 如果规则指定了流程节点，则必须匹配
      if (rule.processNode && rule.processNode !== options.processNode) {
        return false;
      }
      // 如果规则没有指定流程节点（空字符串），则不匹配具体节点
      // 这种情况规则适用于所有节点或无流程场景
    }
    return true;
  });

  // 2. 遍历规则执行验证
  for (const rule of filteredRules) {
    const result = validateRule(rule, values, options);
    if (!result.valid) {
      results.push(result);
    }
  }

  return results;
}

/**
 * 执行单条规则验证
 */
function validateRule(
  rule: JointRule,
  values: IndicatorValuesMap,
  options: ValidatorOptions
): ValidationResult {
  try {
    const config: RuleConfig = JSON.parse(rule.ruleConfig);
    const ruleItems = config.rules || [];

    for (const ruleItem of ruleItems) {
      const action = ruleItem.action || {};

      // 如果指定了变化的指标ID，且当前规则的公式不涉及该指标，则跳过（优化）
      if (options.changedIndicatorId && action.formula) {
        const involvesChangedIndicator = action.formula.some(
          (f) => f.indicatorId === options.changedIndicatorId
        );
        // 如果是被比较的指标变化，也需要验证
        const involvesCompareIndicator =
          action.compareIndicatorId === options.changedIndicatorId;

        if (!involvesChangedIndicator && !involvesCompareIndicator) {
          continue;
        }
      }

      // 获取当前指标的值（可能是公式计算结果）
      const currentValue = calculateFormula(action.formula || [], values);

      // 获取被比较指标的值
      let compareValue: number;
      if (action.compareType === 'fixed') {
        // 固定值比较
        compareValue = Number(action.compareValue) || 0;
      } else {
        // 指标值比较
        compareValue = Number(values[action.compareIndicatorId]) || 0;
      }

      // 执行比较
      const operator = action.operator;
      if (!compareValues(currentValue, operator, compareValue)) {
        // 收集所有涉及的指标ID
        const involvedIndicatorIds: number[] = [];
        const involvedIndicatorCodes: string[] = [];
        
        // 添加公式中的所有指标
        (action.formula || []).forEach((f) => {
          if (f.indicatorId) {
            involvedIndicatorIds.push(f.indicatorId);
            if (f.indicatorCode) involvedIndicatorCodes.push(f.indicatorCode);
          }
        });
        // 添加被比较的指标
        if (action.compareIndicatorId) {
          involvedIndicatorIds.push(action.compareIndicatorId);
          if (action.compareIndicatorName) involvedIndicatorCodes.push(action.compareIndicatorName);
        }

        return {
          valid: false,
          ruleId: rule.id,
          ruleName: rule.ruleName,
          message: action.message || '验证失败',
          indicatorId: action.formula?.[0]?.indicatorId,
          indicatorCode: action.formula?.[0]?.indicatorCode,
          involvedIndicatorIds,
          involvedIndicatorCodes,
        };
      }
    }

    return { valid: true, ruleId: rule.id, ruleName: rule.ruleName, message: '' };
  } catch (e) {
    console.error('规则解析错误:', e);
    // 解析错误时不阻断流程
    return { valid: true, ruleId: rule.id, ruleName: rule.ruleName, message: '' };
  }
}

/**
 * 计算公式（支持加、减、乘、除）
 * @param formula 公式项数组
 * @param values 指标值Map
 * @returns 计算结果
 */
export function calculateFormula(
  formula: FormulaItem[],
  values: IndicatorValuesMap
): number {
  if (!formula || formula.length === 0) {
    return 0;
  }

  let result: number | undefined;

  for (let i = 0; i < formula.length; i++) {
    const item = formula[i];

    // 获取当前项的值
    let itemValue: number;
    if (item.valueType === 'fixed') {
      itemValue = Number(item.value) || 0;
    } else {
      itemValue = Number(values[item.indicatorId!]) || 0;
    }

    // 第一个数直接赋值，后续根据操作符计算
    if (i === 0) {
      result = itemValue;
    } else {
      const mathOp = item.mathOp || '+';
      switch (mathOp) {
        case '+':
          result = (result || 0) + itemValue;
          break;
        case '-':
          result = (result || 0) - itemValue;
          break;
        case '*':
          result = (result || 0) * itemValue;
          break;
        case '/':
          result = itemValue !== 0 ? (result || 0) / itemValue : 0;
          break;
      }
    }
  }

  return result || 0;
}

/**
 * 比较两个值
 */
function compareValues(current: number, operator: string, target: number): boolean {
  // 处理空值情况
  if (current === undefined || current === null || isNaN(current)) {
    return true; // 空值不阻断，等待用户填写
  }
  if (target === undefined || target === null || isNaN(target)) {
    return true; // 被比较值为空，不阻断
  }

  switch (operator) {
    case '>':
      return current > target;
    case '>=':
      return current >= target;
    case '<':
      return current < target;
    case '<=':
      return current <= target;
    case '==':
      return current === target;
    case '!=':
      return current !== target;
    default:
      return true;
  }
}

/**
 * 验证单条规则（导出供外部直接调用）
 */
export function validateSingleRule(
  rule: JointRule,
  values: IndicatorValuesMap
): ValidationResult {
  return validateRule(rule, values, {});
}

export default {
  validate,
  calculateFormula,
  validateSingleRule,
};
