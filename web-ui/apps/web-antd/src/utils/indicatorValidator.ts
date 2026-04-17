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
  ruleName?: string;
  projectType?: number;
  triggerTiming?: 'FILL' | 'PROCESS_SUBMIT';
  processNode?: string;
  ruleConfig: string;
  status?: number;
  createTime?: string;
}

// 指标值Map: key 为 indicatorId（数字）或 indicatorCode（字符串）
export type IndicatorValuesMap = Record<string | number, any>;

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
  /** id → code 的映射，用于将 indicatorId 翻成 code 来查 values */
  idToCode?: Map<number, string>;
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
  // 当前指标（后端配置用 indicatorCode 字符串）
  indicatorCode?: string;
  indicatorId?: number;
  indicatorName?: string;
  // 被比较指标（后端配置用 compareIndicatorCode 字符串）
  compareIndicatorCode?: string;
  compareIndicatorId?: number;
  compareIndicatorName?: string;
  formula?: FormulaItem[];
  /** 被比较侧的公式（用于 logicRule 字符串解析） */
  compareFormula?: FormulaItem[];
}

// 单条规则配置
export interface RuleConfigItem {
  name: string;
  ruleType: number;
  condition?: {
    indicatorCode?: string;
    indicatorId?: number;
    operator?: string;
    value?: number;
  };
  action: RuleAction;
}

// 规则配置（解析后的JSON）
export interface RuleConfig {
  groupName?: string;
  priority?: number;
  rules: RuleConfigItem[];
}

// ==================== 容器字段简写格式支持 ====================

/**
 * 解析容器字段简写格式 (如 602_01)
 * @param shortcut 简写格式，如 "602_01"
 * @param entryNumber 条目编号（从1开始）
 * @returns 完整字段Key，如 "6020101"（容器+条目编号+字段编号），或 null（不是简写格式）
 */
export function parseContainerFieldShortcut(shortcut: string, entryNumber: number): string | null {
  const match = shortcut.match(/^(\d+)_(\d+)$/);
  if (!match) return null;
  const containerCode = match[1];  // 容器编码
  const fieldCode = match[2];      // 字段编码
  return `${containerCode}${String(entryNumber).padStart(2, '0')}${fieldCode}`;
}

/**
 * 判断是否为容器字段简写格式
 */
export function isContainerFieldShortcut(code: string): boolean {
  return /^\d+_\d+$/.test(code);
}

/**
 * 判断目标值是否在数组中
 * - 用于多选题的条件判断
 * - 如果是数组：检查数组中是否存在目标值
 * - 如果是普通值：直接比较是否相等
 * - 空数组视为"未填"，返回 false（跳过验证）
 */
function inArray(value: any, target: any): boolean {
  if (Array.isArray(value)) {
    // 空数组：视为"未填"，跳过验证
    if (value.length === 0) return false;
    const targetStr = String(target);
    return value.some((item) => String(item) === targetStr);
  }
  // 普通值：直接比较（走原有逻辑，不应调用此函数）
  return value == target;
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

function validateRule(
  rule: JointRule,
  values: IndicatorValuesMap,
  options?: ValidatorOptions
): ValidationResult {
  try {
    const idToCode = options?.idToCode;
    const config: RuleConfig = JSON.parse(rule.ruleConfig);
    const ruleItems = config.rules || [];

    for (const ruleItem of ruleItems) {
      const action = ruleItem.action || {};
      const condition = ruleItem.condition;

      // 条件判断：若配置了条件且条件不满足，则跳过验证
      if (condition?.operator) {
        let condValue: any;
        if (condition.indicatorCode !== undefined) {
          condValue = values[condition.indicatorCode];
        } else if (condition.indicatorId !== undefined) {
          const code = idToCode ? idToCode.get(condition.indicatorId) : undefined;
          condValue = code !== undefined ? values[code] : values[condition.indicatorId];
        }

        const condNum = Number(condValue);
        let conditionMet = false;

        switch (condition.operator) {
          case 'not_empty':
            const isEmptyArray = Array.isArray(condValue) && condValue.length === 0;
            const isEmptyObject = condValue !== null && typeof condValue === 'object' && !Array.isArray(condValue) && Object.keys(condValue).length === 0;
            const isEmpty = condValue === undefined
                || condValue === null
                || condValue === ''
                || isEmptyArray
                || isEmptyObject
                || (typeof condValue === 'string' && condValue.trim() === '');
            conditionMet = !isEmpty;
            break;
          case '>':
            conditionMet = !isNaN(condNum) && condNum > (condition.value ?? 0);
            break;
          case '>=':
            conditionMet = !isNaN(condNum) && condNum >= (condition.value ?? 0);
            break;
          case '<':
            conditionMet = !isNaN(condNum) && condNum < (condition.value ?? 0);
            break;
          case '<=':
            conditionMet = !isNaN(condNum) && condNum <= (condition.value ?? 0);
            break;
          case '==':
            if (Array.isArray(condValue)) {
              // 数组值：使用 inArray 查询
              conditionMet = inArray(condValue, condition.value);
            } else {
              // 普通值：使用原有逻辑
              const condNum = Number(condValue);
              conditionMet = !isNaN(condNum) && condNum === (condition.value ?? 0);
            }
            break;
          case '!=':
            if (Array.isArray(condValue)) {
              // 数组值：数组中不包含该值
              conditionMet = !inArray(condValue, condition.value);
            } else {
              // 普通值：使用原有逻辑
              const condNum = Number(condValue);
              conditionMet = !isNaN(condNum) && condNum !== (condition.value ?? 0);
            }
            break;
        }

        // 条件不满足时，跳过验证
        if (!conditionMet) {
          return { valid: true, ruleId: rule.id, ruleName: rule.ruleName ?? '', message: '' };
        }
      }

      // 获取当前指标的值（保持 undefined/NaN 不被吞掉，交给 compareValues 判断）
      let currentValue: any;
      if (action.formula && action.formula.length > 0) {
        const formulaResult = calculateFormula(action.formula, values, idToCode);
        // 如果公式结果包含数组值（多选题），取第一个元素
        if (Array.isArray(formulaResult)) {
          currentValue = formulaResult.length > 0 ? formulaResult[0] : undefined;
        } else {
          currentValue = formulaResult;
        }
      } else if (action.indicatorCode !== undefined) {
        const v = values[action.indicatorCode];
        // 数组值（多选题）取第一个元素
        if (Array.isArray(v)) {
          currentValue = v.length > 0 ? v[0] : undefined;
        } else {
          currentValue = v === undefined || v === null ? undefined : Number(v);
        }
      } else if (action.indicatorId !== undefined) {
        const code = idToCode ? idToCode.get(action.indicatorId) : undefined;
        const v = code !== undefined ? values[code] : values[action.indicatorId];
        // 数组值（多选题）取第一个元素
        if (Array.isArray(v)) {
          currentValue = v.length > 0 ? v[0] : undefined;
        } else {
          currentValue = v === undefined || v === null ? undefined : Number(v);
        }
      } else {
        currentValue = undefined;
      }

      // 获取被比较指标的值
      let compareValue: any;
      if (action.compareType === 'fixed') {
        compareValue = Number(action.compareValue);
      } else if (action.compareFormula && action.compareFormula.length > 0) {
        const compareFormulaResult = calculateFormula(action.compareFormula, values, idToCode);
        // 如果公式结果包含数组值（多选题），取第一个元素
        if (Array.isArray(compareFormulaResult)) {
          compareValue = compareFormulaResult.length > 0 ? compareFormulaResult[0] : undefined;
        } else {
          compareValue = compareFormulaResult;
        }
      } else if (action.compareIndicatorCode !== undefined) {
        const v = values[action.compareIndicatorCode];
        // 数组值（多选题）取第一个元素
        if (Array.isArray(v)) {
          compareValue = v.length > 0 ? v[0] : undefined;
        } else {
          compareValue = v === undefined || v === null ? undefined : Number(v);
        }
      } else if (action.compareIndicatorId !== undefined) {
        const code = idToCode ? idToCode.get(action.compareIndicatorId) : undefined;
        const v = code !== undefined ? values[code] : values[action.compareIndicatorId];
        // 数组值（多选题）取第一个元素
        if (Array.isArray(v)) {
          compareValue = v.length > 0 ? v[0] : undefined;
        } else {
          compareValue = v === undefined || v === null ? undefined : Number(v);
        }
      } else {
        compareValue = undefined;
      }

      // 如果任一操作数为空（且非固定值），跳过验证
      const currentIsEmpty = currentValue === undefined || currentValue === null || (action.formula?.length === 0 && action.indicatorCode === undefined && action.indicatorId === undefined);
      const rightIsDynamic = !!(action.compareFormula || action.compareIndicatorCode !== undefined || action.compareIndicatorId !== undefined);
      const rightIsEmpty = rightIsDynamic && (compareValue === undefined || compareValue === null || (isNaN(Number(compareValue)) && rightIsDynamic));

      if (currentIsEmpty || rightIsEmpty) {
        return { valid: true, ruleId: rule.id, ruleName: rule.ruleName ?? '', message: '' };
      }

      // 执行比较
      const operator = action.operator;
      const compareResult = compareValues(currentValue, operator, compareValue);

      if (!compareResult) {
        // 从 formula / compareFormula 提取所有涉及的指标代码和 ID
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
          ruleId: rule.id,
          ruleName: rule.ruleName ?? '',
          message: action.message || '验证失败',
          indicatorId: action.indicatorId ?? undefined,
          indicatorCode: action.indicatorCode ?? action.compareIndicatorCode ?? undefined,
          involvedIndicatorIds,
          involvedIndicatorCodes,
        };
      }
    }

    return { valid: true, ruleId: rule.id, ruleName: rule.ruleName ?? '', message: '' };
  } catch (e) {
    console.error('规则解析错误:', e);
    return { valid: true, ruleId: rule.id, ruleName: rule.ruleName ?? '', message: '' };
  }
}

/**
 * 计算公式（支持加、减、乘、除）
 * @param formula 公式项数组
 * @param values 指标值Map
 * @param idToCode id → code 映射
 * @returns 计算结果
 */
export function calculateFormula(
  formula: FormulaItem[],
  values: IndicatorValuesMap,
  idToCode?: Map<number, string>
): number | undefined {
  if (!formula || formula.length === 0) {
    return undefined;
  }

  let result: number | undefined;

  for (let i = 0; i < formula.length; i++) {
    const item = formula[i]!;

    // 获取当前项的值（同时支持 indicatorId 数字和 indicatorCode 字符串两种 key）
    let itemValue: any;
    if (item.valueType === 'fixed') {
      itemValue = Number(item.value);
    } else {
      // 优先用 code 字符串查，再用 id 数字翻成 code 查
      const code = item.indicatorCode !== undefined
        ? item.indicatorCode
        : (item.indicatorId !== undefined && idToCode ? idToCode.get(item.indicatorId) : undefined);
      const v = code !== undefined ? values[code] : undefined;
      itemValue = v === undefined || v === null ? undefined : Number(v);
    }

    // 第一个数直接赋值，后续根据操作符计算
    if (i === 0) {
      result = itemValue;
    } else {
      const mathOp = item.mathOp || '+';
      switch (mathOp) {
        case '+':
          result = (result ?? 0) + (itemValue ?? 0);
          break;
        case '-':
          result = (result ?? 0) - (itemValue ?? 0);
          break;
        case '*':
          result = (result ?? 0) * (itemValue ?? 0);
          break;
        case '/':
          result = itemValue !== 0 ? (result ?? 0) / itemValue : 0;
          break;
      }
    }
  }

  return result as number;
}

/**
 * 比较两个值
 * - 两边都有值：正常比较
 * - 两边都空：返回 true（不阻断，等待填）
 * - 左有右空：>=  >  <  <= 返回 false（填了左就不许右为空）
 * - 左空右有：返回 true（右没填不阻断左）
 * - 支持数组值：多选题取第一个元素进行比较
 */
function compareValues(current: any, operator: string, target: any): boolean {
  // 处理数组值：取第一个元素进行比较
  if (Array.isArray(current)) {
    current = current.length > 0 ? current[0] : undefined;
  }
  if (Array.isArray(target)) {
    target = target.length > 0 ? target[0] : undefined;
  }
  // 左操作数有具体值、右操作数缺失：>= > < <= 必须同时有值才让过
  if (
    current !== undefined && current !== null && !isNaN(current) &&
    (target === undefined || target === null || isNaN(target))
  ) {
    if (['>=', '>', '<', '<='].includes(operator)) {
      return false; // 填了左就不许右为空
    }
    return true; // == != 左空右空 两种情况都不阻断
  }

  let result: boolean;
  switch (operator) {
    case '>':
      result = current > target;
      break;
    case '>=':
      result = current >= target;
      break;
    case '<':
      result = current < target;
      break;
    case '<=':
      result = current <= target;
      break;
    case '==':
      result = current === target;
      break;
    case '!=':
      result = current !== target;
      break;
    default:
      result = true;
  }

  return result;
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

/** 解析逻辑校验字符串，返回 engine 认识的 JointRule 格式
 * @param logicRule 逻辑规则字符串
 * @param entryNumber 条目编号（从1开始），用于解析容器字段简写格式（如 602_01）
 */
export function parseLogicRule(logicRule: string, entryNumber: number = 1): JointRule[] {
  if (!logicRule || !logicRule.trim()) return [];

  const results: JointRule[] = [];

  // 先检测是否有 IF 函数需要处理
  // 格式: IF([indicator] condOp condVal, [verify] verifyOp verifyVal, [verify] verifyOp verifyVal, ..., TRUE)
  // 使用非贪婪匹配中间部分，然后按 ", [" 分割多个 verify 表达式
  const ifMatches = [...logicRule.matchAll(/IF\s*\(\s*\[([^\]]+)\]\s*(==|!=|<=|>=|<|>)\s*(\d+(?:\.\d+)?)\s*,\s*(.+?)\s*,\s*TRUE\s*\)/gi)];
  // console.log('[indicatorValidator] IF regex matched count:', ifMatches.length, '| rule:', logicRule);

  if (ifMatches.length > 0) {
    // 处理 IF 函数
    for (const match of ifMatches) {
      const conditionIndicator = match[1]!;
      const conditionOperator = match[2]!;  // ==, !=, >, >=, <, <=
      const conditionThreshold = match[3]!;  // 条件阈值
      // 解析多个 verify 表达式: 按 ", [" 分割，然后每个解析为 [indicator] op value
      const verifyPart = match[4]!;
      const verifyExprs = verifyPart.split(/\s*,\s*(?=\[)/);

      // 展开容器简写格式（如 502_07 → 5020107）
      const conditionIndicatorExpanded = isContainerFieldShortcut(conditionIndicator)
        ? parseContainerFieldShortcut(conditionIndicator, entryNumber) || conditionIndicator
        : conditionIndicator;

      for (const expr of verifyExprs) {
        const verifyMatch = expr.trim().match(/^\[([^\]]+)\]\s*(==|!=|<=|>=|<|>)\s*(\d+(?:\.\d+)?)$/);
        if (!verifyMatch) continue;

        const verifyIndicatorRaw = verifyMatch[1]!;
        const verifyOperator = verifyMatch[2]!;
        const verifyThreshold = verifyMatch[3]!;

        // 展开容器简写格式（如 502_07 → 5020107）
        const verifyIndicator = isContainerFieldShortcut(verifyIndicatorRaw)
          ? parseContainerFieldShortcut(verifyIndicatorRaw, entryNumber) || verifyIndicatorRaw
          : verifyIndicatorRaw;

        const action: any = {
          type: 'formula',
          operator: verifyOperator,
          formula: [{ valueType: 'indicator', indicatorCode: verifyIndicator, mathOp: '+' }],
          compareType: 'fixed',
          compareValue: Number(verifyThreshold),
        };

        results.push({
          id: results.length + 1,
          ruleName: `[${conditionIndicatorExpanded}] ${conditionOperator} ${conditionThreshold} 时, ${expr.trim()}`,
          triggerTiming: 'FILL',
          status: 1,
          projectType: 0,
          ruleConfig: JSON.stringify({
            rules: [{
              id: 1,
              condition: {
                indicatorCode: conditionIndicatorExpanded,
                operator: conditionOperator,
                value: Number(conditionThreshold),
              },
              action,
              name: `[${conditionIndicatorExpanded}] ${conditionOperator} ${conditionThreshold} 时, ${expr.trim()}`,
            }],
          }),
        });
      }
    }

    return results;
  }

  // 普通规则解析
  const parts = logicRule.split(/\n|；|;|AND|and|And/).filter(Boolean);

  for (let i = 0; i < parts.length; i++) {
    const part = parts[i]!.trim();

    const opMatch = part.match(/^(.+?)(>=|<=|>|<|==|!=)(.+)$/);
    if (!opMatch) continue;

    const left = opMatch[1]!.trim();
    const operator = opMatch[2]!;
    const right = opMatch[3]!.trim();

    // 传入 entryNumber 用于解析容器字段简写格式
    const leftFormula = parseFormulaSide(left, entryNumber);
    const rightFormula = parseFormulaSide(right, entryNumber);

    // 构造 engine action
    const action: any = {
      type: 'formula',
      operator,
      formula: leftFormula,
      compareType: 'indicator',
      compareFormula: rightFormula,
    };

    results.push({
      id: results.length + 1,
      ruleName: part,
      triggerTiming: 'FILL',
      status: 1,
      projectType: 0,
      ruleConfig: JSON.stringify({
        rules: [{ id: 1, action, name: part }],
      }),
    });
  }

  return results;
}

/** 解析公式一侧（如 '[201]' 或 '201' 或 '[80201]+[80202]'），返回 FormulaItem[]
 * @param side 公式侧字符串
 * @param entryNumber 条目编号（从1开始），用于解析容器字段简写格式
 */
function parseFormulaSide(side: string, entryNumber: number = 1): FormulaItem[] {
  const raw = side.trim();
  if (!raw) return [];

  // 使用全局匹配提取所有 [指标] 及其前后的操作符
  // 匹配模式：[\s*内容\s*] 捕获内容，或 +/- 数字
  const bracketPattern = /\[\s*([^\]]+?)\s*\]|\b([+\-]?\d+(?:\.\d+)?)\b/g;
  const items: FormulaItem[] = [];
  let lastIndex = 0;
  let match: RegExpExecArray | null;
  let mathOp: '+' | '-' | '*' | '/' = '+';

  while ((match = bracketPattern.exec(raw)) !== null) {
    // 计算操作符：检查当前匹配之前的内容中最后一个操作符
    const beforeMatch = raw.slice(lastIndex, match.index);
    const lastOpMatch = beforeMatch.match(/[+\-*/]\s*$/);
    mathOp = (lastOpMatch ? (lastOpMatch[0]!.trim().startsWith('-') ? '-' : '+') : '+') as '+' | '-' | '*' | '/';

    if (match[1] !== undefined) {
      // 匹配到带括号的指标引用
      const code = match[1]!.trim();
      // 检查是否为容器字段简写格式（如 602_01）
      const containerKey = parseContainerFieldShortcut(code, entryNumber);
      if (containerKey) {
        // 使用解析后的 fullKey（如 6020101）
        items.push({ valueType: 'indicator', indicatorCode: containerKey, mathOp });
      } else {
        items.push({ valueType: 'indicator', indicatorCode: code, mathOp });
      }
    } else if (match[2] !== undefined) {
      // 匹配到数字
      const numStr = match[2]!.replace(/^\+/, '');
      items.push({ valueType: 'fixed', value: Number(numStr), mathOp });
    }
    lastIndex = match.index + match[0].length;
  }

  // 如果匹配到了括号表达式，直接返回
  if (items.length > 0) {
    return items;
  }

  // 无括号表达式，按原有逻辑处理
  const tokens = raw.split(/(?=[+\-*/])(?![+\-*\/])/);
  const fallbackItems: FormulaItem[] = [];

  for (const token of tokens) {
    const trimmed = token.trim();
    if (!trimmed) continue;

    if (/^\[.+\]$/.test(trimmed)) {
      // 带括号的指标引用
      const code = trimmed.replace(/^\[|\]$/g, '');
      const containerKey = parseContainerFieldShortcut(code, entryNumber);
      fallbackItems.push({
        valueType: 'indicator',
        indicatorCode: containerKey || code,
        mathOp: trimmed.startsWith('-') ? '-' : '+'
      });
    } else if (/^[+\-]?\d+(\.\d+)?$/.test(trimmed)) {
      fallbackItems.push({ valueType: 'fixed', value: Number(trimmed.replace(/^\+/, '')), mathOp: trimmed.startsWith('-') ? '-' : '+' });
    } else {
      const containerKey = parseContainerFieldShortcut(trimmed, entryNumber);
      fallbackItems.push({
        valueType: 'indicator',
        indicatorCode: containerKey || trimmed.replace(/^\[|\]$/g, ''),
        mathOp: trimmed.startsWith('-') ? '-' : '+'
      });
    }
  }

  return fallbackItems;
}

export default {
  validate,
  calculateFormula,
  validateSingleRule,
};
