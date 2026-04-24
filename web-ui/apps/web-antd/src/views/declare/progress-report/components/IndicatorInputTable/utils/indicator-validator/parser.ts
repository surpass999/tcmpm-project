/**
 * 指标验证规则字符串解析器
 *
 * 职责：规则字符串 → 结构化 ParsedRule/ParsedCCRule/ParsedFORMATSRule
 *
 * 解析流程：
 *   raw string
 *     ↓ normalize()   — 归一化（空格、TRUE 大小写）
 *     ↓ detectRuleType() — 自动检测规则类型
 *     ↓
 *   ┌─ 'IF'  → parseIF()   → ParsedRule[]
 *   ├─ 'CC'  → parseCC()   → ParsedCCRule
 *   ├─ 'FORMATS' → parseFORMATS() → ParsedFORMATSRule
 *   └─ 'SIMPLE' → parseSimple() → ParsedRule[]
 */

import type { ParsedRule, ParsedCCRule, ParsedFORMATSRule } from './types';

// ==================== 容器字段简写 ====================

/**
 * 解析容器字段简写格式 (如 602_01)
 * @param shortcut 简写格式，如 "602_01"
 * @param entryNumber 条目编号（从1开始）
 * @returns 完整字段Key，如 "6020101"（容器+条目编号+字段编号），或 null（不是简写格式）
 */
export function parseContainerFieldShortcut(shortcut: string, entryNumber: number): string | null {
  const match = shortcut.match(/^(\d+)_(\d+)$/);
  if (!match) return null;
  const containerCode = match[1]!;
  const fieldCode = match[2]!;
  return `${containerCode}${String(entryNumber).padStart(2, '0')}${fieldCode}`;
}

/**
 * 判断是否为容器字段简写格式
 */
export function isContainerFieldShortcut(code: string): boolean {
  return /^\d+_\d+$/.test(code);
}

// ==================== 归一化 ====================

/** 归一化规则字符串：去除多余空格，统一格式 */
function normalize(raw: string): string {
  let s = raw.trim();
  // 统一去掉所有比较操作符和值之间的空格，便于后续一致处理
  // "> 0" → ">0"；"== 11" → "==11" 等
  s = s.replace(/(==|!=|<=|>=|<|>)\s+/g, '$1');
  // 归一化 IF 规则 true 分支的空格：", TRUE)" → ",TRUE)"
  s = s.replace(/,\s*TRUE\)/gi, ',TRUE)');
  // 归一化 true/false 的大小写（统一为 TRUE）
  s = s.replace(/\bTRUE\b/gi, 'TRUE');
  return s;
}

// ==================== 规则类型检测 ====================

/** 自动检测规则类型 */
export function detectRuleType(raw: string): 'IF' | 'CC' | 'FORMATS' | 'SIMPLE' {
  const trimmed = raw.trim();
  if (/^CC\s*\(/i.test(trimmed)) return 'CC';
  if (/^FORMATS\s*\(/i.test(trimmed)) return 'FORMATS';
  if (/^IF\s*\(/i.test(trimmed)) return 'IF';
  return 'SIMPLE';
}

// ==================== 条件操作符解析 ====================

/**
 * 解析条件操作符字符串
 * @param condStr 完整条件字符串，如 "[801] == 11" 或 "[801] IN(1,2,3)"
 * @returns 操作符类型和值数组
 */
export function parseConditionOperator(condStr: string): {
  operator: string;
  values: number[] | null;
  threshold: number;
} {
  // 归一化已在 normalize() 阶段处理，这里只需解析
  const opMatch = condStr.match(/\[([^\]]+)\]?\s*(.+)$/);
  if (!opMatch) return { operator: condStr.trim(), values: null, threshold: 0 };

  const opStr = opMatch[2]!.trim();

  // IN / NOT_IN 列表操作符
  const inMatch = opStr.match(/^(IN|NOT_IN)\(([^)]*)\)?$/);
  if (inMatch) {
    const op = inMatch[1]!;
    const values = inMatch[2]!.split(',').map((v) => Number(v.trim()));
    return { operator: op, values, threshold: 0 };
  }

  // 简单比较符：==  !=  >  >=  <  <=
  const simpleMatch = opStr.match(/^(==|!=|<=|>=|<|>)\s*(\d+(?:\.\d+)?)$/);
  if (simpleMatch) {
    return { operator: simpleMatch[1]!, values: null, threshold: Number(simpleMatch[2]) };
  }

  return { operator: opStr.trim(), values: null, threshold: 0 };
}

// ==================== 公式侧解析 ====================

/**
 * 解析公式一侧（如 '[201]' 或 '201' 或 '[80201]+[80202]'）
 * @param side 公式侧字符串
 * @param entryNumber 条目编号（从1开始），用于解析容器字段简写格式
 */
export function parseFormulaSide(side: string, entryNumber: number = 1): Array<{
  valueType: 'indicator' | 'fixed';
  indicatorId?: number;
  indicatorCode?: string;
  indicatorName?: string;
  mathOp?: '+' | '-' | '*' | '/';
  value?: number;
}> {
  const raw = side.trim();
  if (!raw) return [];

  // 使用全局匹配提取所有 [指标] 及其前后的操作符
  const bracketPattern = /\[\s*([^\]]+?)\s*\]|\b([+\-]?\d+(?:\.\d+)?)\b/g;
  const items: Array<{
    valueType: 'indicator' | 'fixed';
    indicatorCode?: string;
    mathOp: '+' | '-' | '*' | '/';
    value?: number;
  }> = [];
  let lastIndex = 0;
  let match: RegExpExecArray | null;
  let mathOp: '+' | '-' | '*' | '/' = '+';

  while ((match = bracketPattern.exec(raw)) !== null) {
    const beforeMatch = raw.slice(lastIndex, match.index);
    const lastOpMatch = beforeMatch.match(/[+\-*/]\s*$/);
    mathOp = (lastOpMatch?.[0]!.trim().startsWith('-') ? '-' : '+') as '+' | '-' | '*' | '/';

    if (match[1] !== undefined) {
      const code = match[1]!.trim();
      const containerKey = parseContainerFieldShortcut(code, entryNumber);
      items.push({
        valueType: 'indicator',
        indicatorCode: containerKey || code,
        mathOp,
      });
    } else if (match[2] !== undefined) {
      const numStr = match[2]!.replace(/^\+/, '');
      items.push({ valueType: 'fixed', value: Number(numStr), mathOp });
    }
    lastIndex = match.index + match[0].length;
  }

  if (items.length > 0) return items;

  // 无括号表达式，按原有逻辑处理
  const tokens = raw.split(/(?=[+\-*/])(?![+\-*\/])/);
  const fallbackItems: typeof items = [];

  for (const token of tokens) {
    const trimmed = token.trim();
    if (!trimmed) continue;

    if (/^\[.+\]$/.test(trimmed)) {
      const code = trimmed.replace(/^\[|\]$/g, '');
      const containerKey = parseContainerFieldShortcut(code, entryNumber);
      fallbackItems.push({
        valueType: 'indicator',
        indicatorCode: containerKey || code,
        mathOp: trimmed.startsWith('-') ? '-' : '+',
      });
    } else if (/^[+\-]?\d+(\.\d+)?$/.test(trimmed)) {
      fallbackItems.push({ valueType: 'fixed', value: Number(trimmed.replace(/^\+/, '')), mathOp: trimmed.startsWith('-') ? '-' : '+' });
    } else {
      const containerKey = parseContainerFieldShortcut(trimmed, entryNumber);
      fallbackItems.push({
        valueType: 'indicator',
        indicatorCode: containerKey || trimmed.replace(/^\[|\]$/g, ''),
        mathOp: trimmed.startsWith('-') ? '-' : '+',
      });
    }
  }

  return fallbackItems;
}

// ==================== CC 规则解析 ====================

const CC_TRIGGER_OPS = new Set(['FIRST_EQ', 'LAST_EQ', 'FIRST_GT', 'FIRST_GTE', 'FIRST_LT', 'FIRST_LTE', 'FIRST_IN', 'LAST_IN']);
const CC_FORBID_OPS = new Set(['REST_EQ', 'REST_NE']);

/**
 * 解析 CC 公式
 * 格式: CC([602_07,602_06,602_05], FIRST_EQ(2), REST_NE(1))
 */
export function parseCC(logicRule: string, entryNumber: number = 1): ParsedCCRule | null {
  const rule = logicRule.trim();
  const match = rule.match(
    /^CC\s*\(\s*\[([^\]]+)\]\s*,\s*([\w_]+)\s*\(\s*([^\)]+)\s*\)\s*,\s*([\w_]+)\s*\(\s*([^\)]+)\s*\)\s*\)\s*$/i,
  );

  if (!match) return null;

  const rawFieldCodes = match[1]!.split(',').map((s) => s.trim()).filter(Boolean);
  const triggerOp = match[2]!.toUpperCase();
  const triggerVal = match[3]!.trim();
  const forbidOp = match[4]!.toUpperCase();
  const forbidVal = match[5]!.trim();

  if (!CC_TRIGGER_OPS.has(triggerOp)) {
    console.warn(`[parser] CC: unsupported trigger op "${triggerOp}"`);
    return null;
  }
  if (!CC_FORBID_OPS.has(forbidOp)) {
    console.warn(`[parser] CC: unsupported forbid op "${forbidOp}"`);
    return null;
  }

  // 展开容器字段简写格式（如 602_07 → 6020107）
  const fieldCodes: string[] = [];
  for (const raw of rawFieldCodes) {
    if (isContainerFieldShortcut(raw)) {
      const expanded = parseContainerFieldShortcut(raw, entryNumber);
      fieldCodes.push(expanded || raw);
    } else {
      fieldCodes.push(raw);
    }
  }

  return {
    type: 'CC',
    fieldCodes,
    originalFieldCodes: rawFieldCodes,
    triggerOp,
    triggerVal,
    forbidOp,
    forbidVal,
  };
}

// ==================== FORMATS 规则解析 ====================

/**
 * 解析 FORMATS 公式
 * 格式: FORMATS(["word","pdf"])
 * 完整示例: FORMATS(["word","pdf"], {word:["doc","docx"], pdf:["pdf"]})
 */
export function parseFORMATS(logicRule: string): ParsedFORMATSRule | null {
  const match = logicRule.match(/FORMATS\s*\(\s*\[([^\]]+)\]\s*(?:,\s*(\{[^}]+\}))?\s*\)/i);
  if (!match) return null;

  const formatsStr = match[1]!;
  const requiredFormats = formatsStr
    .split(',')
    .map((f) => f.trim().replace(/['"]/g, '').toLowerCase())
    .filter(Boolean);

  let typeGroups: Record<string, string[]> = {};
  const typeGroupsStr = match[2];
  if (typeGroupsStr) {
    try {
      typeGroups = JSON.parse(
        typeGroupsStr.replace(/([{,]\s*)(\w+)\s*:/g, '$1"$2":'),
      );
    } catch {
      typeGroups = {};
    }
  }

  return { type: 'FORMATS', typeGroups, requiredFormats };
}

// ==================== IF 规则解析 ====================

/**
 * 解析 IF 规则字符串
 * 格式: IF([cond] op condVal, [verify1] op1 val1, [verify2] op2 val2, ..., TRUE)
 */
function parseIF(raw: string, entryNumber: number): ParsedRule[] {
  const results: ParsedRule[] = [];
  const upper = raw.toUpperCase();
  let trueEndPos = upper.lastIndexOf(',TRUE)');
  let ruleIdx = 0;

  while (trueEndPos !== -1) {
    const beforeTrue = raw.slice(0, trueEndPos);
    const ifStart = beforeTrue.toUpperCase().lastIndexOf('IF(');
    if (ifStart === -1) { trueEndPos = upper.lastIndexOf(',TRUE)', trueEndPos - 1); continue; }

    const inner = beforeTrue.slice(ifStart + 3);
    const content = inner.replace(/\)\s*$/, '').trim();

    // 找第一个深度为0的逗号 → 分割条件/动作
    let depth = 0;
    let splitPos = -1;
    for (let i = content.length - 1; i >= 0; i--) {
      const ch = content[i]!;
      if (ch === ')') depth++;
      else if (ch === '(') depth = Math.max(0, depth - 1);
      else if (ch === ',' && depth === 0) { splitPos = i; break; }
    }

    if (splitPos === -1) { trueEndPos = upper.lastIndexOf(',TRUE)', trueEndPos - 1); continue; }

    const condRaw = content.slice(0, splitPos).trim();
    const actionPart = content.slice(splitPos + 1).trim();

    // 解析条件部分
    const condMatch = condRaw.match(/^\[([^\]]+)\]?\s*(.+)$/);
    if (!condMatch) { trueEndPos = upper.lastIndexOf(',TRUE)', trueEndPos - 1); continue; }

    const condIndicatorRaw = condMatch[1]!.trim();
    const condOpStr = condMatch[2]!.trim();
    const { operator: conditionOperator, values: conditionValues, threshold: conditionThreshold } =
      parseConditionOperator(`[${condIndicatorRaw}] ${condOpStr}`);

    const conditionIndicator = isContainerFieldShortcut(condIndicatorRaw)
      ? parseContainerFieldShortcut(condIndicatorRaw, entryNumber) || condIndicatorRaw
      : condIndicatorRaw;

    // 解析动作部分（可能有多个子动作，用深度为0的逗号分隔）
    const actionParts: string[] = [];
    let curStart = 0;
    depth = 0;
    for (let i = 0; i <= actionPart.length; i++) {
      if (i === actionPart.length || (actionPart[i] === ',' && depth === 0)) {
        const part = actionPart.slice(curStart, i === actionPart.length ? undefined : i).trim();
        if (part) actionParts.push(part);
        curStart = i + 1;
      } else if (actionPart[i] === '(') depth++;
      else if (actionPart[i] === ')') depth = Math.max(0, depth - 1);
    }

    for (const expr of actionParts) {
      const verifyMatch = expr.match(/^\[([^\]]+)\]?\s*(.+)$/);
      if (!verifyMatch) continue;

      const verifyIndicatorRaw = verifyMatch[1]!.trim();
      const verifyOpStr = verifyMatch[2]!.trim();
      const verifyIndicator = isContainerFieldShortcut(verifyIndicatorRaw)
        ? parseContainerFieldShortcut(verifyIndicatorRaw, entryNumber) || verifyIndicatorRaw
        : verifyIndicatorRaw;

      const verifyInMatch = verifyOpStr.match(/^(IN|NOT_IN)\(([^)]*)\)?$/);
      const isListOp = verifyInMatch !== null;

      const action: Parameters<typeof buildActionObj>[0] = {
        type: 'formula',
        formula: [{ valueType: 'indicator' as const, indicatorCode: verifyIndicator, mathOp: '+' as const }],
      };

      if (isListOp) {
        const op = verifyInMatch![1]!;
        const listValues = verifyInMatch![2]!.split(',').map((v) => Number(v.trim()));
        action.operator = op;
        action.compareValues = listValues;
        action.compareType = 'fixed';
        // 不设置 compareValue，避免与 buildMsgFromConfig 的 ==/!= 分支冲突
      } else {
        const simpleMatch = verifyOpStr.match(/^(>=|<=|==|!=|<|>)\s*(\d+(?:\.\d+)?)$/);
        if (!simpleMatch) continue;
        action.operator = simpleMatch[1]!;
        action.compareType = 'fixed';
        action.compareValue = Number(simpleMatch[2]!);
      }

      const condition: Record<string, any> = {
        indicatorCode: conditionIndicator,
        operator: conditionOperator,
      };
      if (conditionValues !== null) {
        condition.values = conditionValues;
        condition.value = 0;
      } else {
        condition.value = conditionThreshold;
      }

      const condLabel = conditionValues !== null
        ? `${conditionOperator}(${conditionValues.join(',')})`
        : `${conditionOperator} ${conditionThreshold}`;

      const ruleId = ruleIdx++;
      results.push({
        type: 'IF',
        raw,
        ruleConfig: {
          rules: [{
            id: ruleId,
            condition,
            action: buildActionObj(action),
            name: `[${conditionIndicator}] ${condLabel} 时, ${expr}`,
          }],
        },
        ruleName: `[${conditionIndicator}] ${condLabel} 时, ${expr}`,
        ruleId,
        verifyIndicatorCode: verifyIndicator,
      });
    }

    trueEndPos = upper.lastIndexOf(',TRUE)', trueEndPos - 1);
  }

  return results;
}

// 辅助：构建 action 对象的工具类型
function buildActionObj(p: {
  type: 'formula' | 'condition';
  formula?: Array<{ valueType: 'indicator' | 'fixed'; indicatorCode?: string; mathOp?: string; value?: number }>;
  operator?: string;
  compareValues?: number[];
  compareType?: string;
  compareValue?: number;
}): Record<string, any> {
  const action: Record<string, any> = {
    type: p.type,
    operator: p.operator ?? '',
  };
  if (p.formula) action.formula = p.formula;
  if (p.compareValues !== undefined) action.compareValues = p.compareValues;
  if (p.compareType) action.compareType = p.compareType;
  if (p.compareValue !== undefined) action.compareValue = p.compareValue;
  return action;
}

// ==================== 普通规则解析 ====================

/**
 * 解析普通（SIMPLE）规则
 * 格式: [802]>=[80201]+[80202]
 */
function parseSimple(raw: string, entryNumber: number): ParsedRule[] {
  const results: ParsedRule[] = [];
  const parts = raw.split(/\n|；|;|AND|and|And/).filter(Boolean);

  for (let i = 0; i < parts.length; i++) {
    const part = parts[i]!.trim();
    if (!part || /^\s*IF\s*\(/i.test(part)) continue;

    const opMatch = part.match(/^(.+?)(>=|<=|>|<|==|!=)(.+)$/);
    if (!opMatch) continue;

    const left = opMatch[1]!.trim();
    const operator = opMatch[2]!;
    const right = opMatch[3]!.trim();

    const leftFormula = parseFormulaSide(left, entryNumber);
    const rightFormula = parseFormulaSide(right, entryNumber);

    const isPureFixed = rightFormula.length === 1 && rightFormula[0]!.valueType === 'fixed';

    const action: Record<string, any> = {
      type: 'formula',
      operator,
      formula: leftFormula,
      compareType: isPureFixed ? 'fixed' : 'indicator',
      // 设置 indicatorCode（供 evaluateRule 读取当前指标值）
      ...(leftFormula.length === 1 && leftFormula[0]!.valueType === 'indicator'
        ? { indicatorCode: leftFormula[0]!.indicatorCode }
        : {}),
    };
    if (isPureFixed) {
      action.compareValue = rightFormula[0]!.value;
    } else {
      action.compareFormula = rightFormula;
    }

    results.push({
      type: 'SIMPLE',
      raw,
      ruleConfig: {
        rules: [{ id: 1, action, name: part }],
      },
      ruleName: part,
      ruleId: i + 1,
    });
  }

  return results;
}

// ==================== 主入口 ====================

/**
 * 解析逻辑规则字符串
 * @param logicRule 规则字符串
 * @param entryNumber 条目编号（从1开始），用于展开容器字段简写格式
 * @returns 解析后的规则数组（IF/SIMPLE 规则）
 */
export function parseLogicRule(logicRule: string, entryNumber: number = 1): ParsedRule[] {
  if (!logicRule || !logicRule.trim()) return [];

  const normalized = normalize(logicRule);
  const ruleType = detectRuleType(normalized);

  switch (ruleType) {
    case 'IF':
      return parseIF(normalized, entryNumber);
    case 'SIMPLE':
      return parseSimple(normalized, entryNumber);
    case 'CC':
    case 'FORMATS':
      return [];
    default:
      return [];
  }
}
