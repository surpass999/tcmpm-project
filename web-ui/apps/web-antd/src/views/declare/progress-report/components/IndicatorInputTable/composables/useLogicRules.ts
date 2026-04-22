/**
 * useLogicRules - 逻辑规则校验
 *
 * 负责：
 * - 逻辑规则解析
 * - 逻辑规则校验（逐条目 + 顶层）
 * - 错误消息构建
 */

import type { DeclareIndicatorApi } from '#/api/declare/indicator';
import { validate as validateJointRule, parseLogicRule, parseContainerFieldShortcut, isContainerFieldShortcut, buildLogicRuleMsgForSingleRule, parseFORMATS } from '#/utils/indicatorValidator';
import { isEmpty } from '../utils/validators';
import type { ValidationError, FieldError, DynamicField } from '../types';
import { formValues } from './useFormValues';
import { containerValues, isEntryBasedContainer, getContainerEntryCount } from './useContainerValues';
import { parseDynamicFields, generateContainerFieldKey, getContainerType } from '../utils/container';
import {
  fieldErrors,
  toContainerKey,
} from './useErrorKeys';
import { evaluateLinkage } from '../utils/linkageEvaluator';
import { isIndicatorVisible } from './useLinkage';

/** 判断是否为自动计算指标 */
function isComputedIndicator(indicator: DeclareIndicatorApi.Indicator): boolean {
  return !!(indicator.calculationRule && indicator.calculationRule.trim());
}

// ==================== 构建值映射 ====================

/** 构建单个条目的完整值Map（用于逐条目验证） */
function buildEntryValueMap(
  indicator: DeclareIndicatorApi.Indicator,
  entryNumber: number
): Record<string, any> {
  const map: Record<string, any> = {};
  const containerCode = indicator.indicatorCode;
  const entryKey = `${containerCode}${String(entryNumber).padStart(2, '0')}`;

  const entries = containerValues[containerCode] || [];
  const entry = entries.find((e: any) => e.rowKey === entryKey);

  if (entry) {
    const config = parseDynamicFields(indicator.valueOptions);
    const fields = config || [];

    for (const field of fields) {
      const fullKey = `${entryKey}${field.fieldCode}`;
      map[fullKey] = entry[fullKey];
    }
  }

  return map;
}

/**
 * 构建条件容器的值映射
 * 条件容器字段值存储在 entry[rowKey+fieldCode] 中，如 entry["60303"]
 * logicRule 格式：[603_03] > 0（容器+字段简写，展开为 rowKey+fieldCode）
 */
function buildEntryValueMapForConditional(
  indicator: DeclareIndicatorApi.Indicator
): Record<string, any> {
  const map: Record<string, any> = {};
  const entries = containerValues[indicator.indicatorCode] || [];
  if (entries.length === 0) return map;

  const entry = entries[0]; // 条件容器只有一条
  const fields = parseDynamicFields(indicator.valueOptions);

  for (const field of fields) {
    const fullKey = `${entry.rowKey}${field.fieldCode}`;
    map[fullKey] = entry[fullKey];
  }

  return map;
}

/** 构建指标编码到值的映射 */
function buildCodeValueMap(): Record<string, any> {
  const map: Record<string, any> = {};
  for (const [code, value] of Object.entries(formValues)) {
    map[code] = value;
  }
  for (const indicator of Object.values(containerValues)) {
    // containerValues 中的每个容器指标
  }
  return map;
}

// ==================== 错误消息构建 ====================

function buildLogicRuleMsg(
  logicRule: string,
  allIndicators: DeclareIndicatorApi.Indicator[],
  codeValueMap: Record<string, any>,
  entryNumber?: number,
  containerIndicator?: DeclareIndicatorApi.Indicator
): string {
  if (!logicRule) return '校验失败';

  const codeMap = new Map<string, string>();
  for (const ind of allIndicators) {
    if (!codeMap.has(ind.indicatorCode)) codeMap.set(ind.indicatorCode, ind.indicatorName || ind.indicatorCode);
  }

  const getContainerFieldLabel = (fullKey: string): { label: string; valueText: string } => {
    if (!containerIndicator) return { label: fullKey, valueText: codeValueMap[fullKey] !== undefined ? String(codeValueMap[fullKey]) : '未填' };

    const fields = parseDynamicFields(containerIndicator.valueOptions);
    const fieldCode = fullKey.slice(-2);
    const field = fields.find((f) => f.fieldCode === fieldCode);

    if (!field) return { label: fullKey, valueText: codeValueMap[fullKey] !== undefined ? String(codeValueMap[fullKey]) : '未填' };

    const val = codeValueMap[fullKey];
    let valueText = '未填';
    if (val !== undefined && val !== null && val !== '') {
      if (field.fieldType === 'radio' || field.fieldType === 'checkbox') {
        const options = field.options || [];
        // 处理数组值（多选题）
        if (Array.isArray(val)) {
          const labels = val.map((v) => {
            const option = options.find((o) => String(o.value) === String(v) || o.value === v);
            return option ? option.label : String(v);
          });
          valueText = labels.join(', ');
        } else {
          const option = options.find((o) => String(o.value) === String(val) || o.value === val);
          valueText = option ? option.label : String(val);
        }
      } else {
        valueText = String(val);
      }
    }

    return { label: field.fieldLabel, valueText };
  };

  /** 根据字段代码和阈值获取字段信息 + 阈值文本（label for select/radio/checkbox） */
  const getFieldAndThresholdText = (fieldCode: string, threshold: string): { fieldLabel: string; thresholdText: string } => {
    let fieldLabel = fieldCode;
    let thresholdText = threshold;

    if (containerIndicator) {
      const fields = parseDynamicFields(containerIndicator.valueOptions);
      // fieldCode 可能是简写（502_05）也可能是完整 key（5020105）
      const shortCode = fieldCode.slice(-2);
      const field = fields.find((f) => f.fieldCode === shortCode || f.fieldCode === fieldCode);

      if (field) {
        fieldLabel = field.fieldLabel;
        if (field.fieldType === 'radio' || field.fieldType === 'select' || field.fieldType === 'checkbox') {
          const options = field.options || [];
          const option = options.find((o) => o.value === threshold || o.value === String(threshold));
          if (option) {
            thresholdText = `"${option.label}"`;
          }
        }
      }
    } else {
      // 顶层指标：用 codeMap 查指标名称，始终附加代码前缀（如 603 → 603 - 完成交易活动的数据产品数）
      const name = codeMap.get(fieldCode);
      fieldLabel = name ? `${fieldCode} - ${name}` : fieldCode;

      // 顶层指标如果是多选题/单选/下拉，也需要将阈值转换为 label
      const indicator = allIndicators.find((ind) => ind.indicatorCode === fieldCode);
      if (indicator) {
        // 尝试解析 valueOptions：可能是直接 options 数组，也可能是包装在 fields 中
        let options: Array<{ value: any; label: string }> = [];
        
        // 先检查是否为直接的 options 数组格式
        try {
          const parsed = JSON.parse(indicator.valueOptions);
          if (Array.isArray(parsed) && parsed.length > 0 && parsed[0]?.value !== undefined && parsed[0]?.label !== undefined) {
            options = parsed;
          }
        } catch {
          // 不是直接的 JSON 数组，尝试解析为 fields 格式
        }
        
        if (options.length === 0) {
          const fields = parseDynamicFields(indicator.valueOptions);
          if (fields.length === 1 && fields[0]?.options) {
            options = fields[0].options;
          } else {
            // 也可能第一个字段本身就是 options（虽然字段结构但没有 fieldCode）
            for (const field of fields) {
              if (field.options && field.options.length > 0) {
                options = field.options;
                break;
              }
            }
          }
        }
        
        // 查找匹配的 option
        const option = options.find((o) => o.value === threshold || o.value === String(threshold) || String(o.value) === String(threshold));
        
        if (option) {
          thresholdText = `"${option.label}"`;
        }
      }
    }

    return { fieldLabel, thresholdText };
  };

  const replaceCode = (code: string): string => {
    let fullKey = code;
    if (isContainerFieldShortcut(code) && entryNumber !== undefined) {
      const parsed = parseContainerFieldShortcut(code, entryNumber);
      if (parsed) {
        fullKey = parsed;
      }
    }

    if (containerIndicator && fullKey.startsWith(containerIndicator.indicatorCode)) {
      const { label, valueText } = getContainerFieldLabel(fullKey);
      return `${label}(${valueText})`;
    }

    const name = codeMap.get(code) || code;
    const val = codeValueMap[code];
    let valText = '未填';
    if (val !== undefined && val !== null && val !== '') {
      // 检查是否为 select/radio/checkbox 类型
      const indicator = allIndicators.find((ind) => ind.indicatorCode === code);
      if (indicator) {
        const fields = parseDynamicFields(indicator.valueOptions);
        if (fields.length === 1 && fields[0] && ['select', 'radio', 'checkbox'].includes(fields[0].fieldType)) {
          const options = fields[0].options || [];
          if (Array.isArray(val)) {
            valText = val.map((v) => {
              const opt = options.find((o) => String(o.value) === String(v) || o.value === v);
              return opt ? opt.label : String(v);
            }).join(', ');
          } else {
            const opt = options.find((o) => String(o.value) === String(val) || o.value === val);
            valText = opt ? opt.label : String(val);
          }
        } else {
          valText = String(val);
        }
      } else {
        valText = String(val);
      }
    }
    return `${name}(${valText})`;
  };

  // 支持多 verify 表达式的 IF 函数: IF([cond] op val, [v1] op1 v1, [v2] op2 v2, ..., TRUE)
  // 条件操作符支持基本比较符和 IN()/NOT_IN()，验证操作符同样支持
  const ifMultiMatch = logicRule.trim().match(
    /^IF\s*\(\s*\[([^\]]+)\]\s*(==|!=|<=|>=|<|>|(NOT_IN|IN)\([^)]+\))\s*,\s*(.+)\s*,\s*TRUE\s*\)$/i
  );
  if (ifMultiMatch) {
    const condCode = ifMultiMatch[1]!.trim();
    const condOp = ifMultiMatch[2]!;
    // IN/NOT_IN 时值嵌在操作符字符串中，需要单独解析；基本比较符用第 4 组
    let condVal = '';
    let condValues: string[] = [];
    const listOpMatch = condOp.match(/^(IN|NOT_IN)\(([^)]+)\)$/);
    if (listOpMatch) {
      condValues = listOpMatch[2]!.split(',').map(v => v.trim());
      condVal = condValues.join(',');
    } else {
      condVal = ifMultiMatch[4] ?? '';
    }
    const verifyPart = ifMultiMatch[3]!;
    const condOpText: Record<string, string> = {
      '==': '等于', '!=': '不等于', '>': '大于', '>=': '大于等于', '<': '小于', '<=': '小于等于',
      'IN': '在列表中', 'NOT_IN': '不在列表中',
    };
    const opText: Record<string, string> = {
      '>=': '应大于等于', '<=': '应小于等于', '>': '应大于', '<': '应小于',
      '==': '应等于', '!=': '不应等于',
      'IN': '应为', 'NOT_IN': '不应为',
    };

    const { fieldLabel: condFieldLabel, thresholdText: condThresholdText } = getFieldAndThresholdText(condCode, condVal);
    const verifyExprs = verifyPart.split(/\s*,\s*(?=\[)/);
    const verifyMsgs = verifyExprs.map((expr) => {
      const vm = expr.trim().match(
        /^\[([^\]]+)\]\s*(==|!=|<=|>=|<|>|(NOT_IN|IN)\([^)]+\))$/
      );
      if (!vm) return expr.trim();
      const fieldCode = vm[1]!.trim();
      const rawVerifyOp = vm[2]!;
      // 列表操作符的值嵌在括号中（如 NOT_IN(7)），需要单独解析
      const listOpMatch = rawVerifyOp.match(/^(NOT_IN|IN)\(([^)]+)\)$/);
      const thresholdText = listOpMatch
        ? listOpMatch[2]!
        : vm[3]!;
      const { fieldLabel, thresholdText: thText } = getFieldAndThresholdText(fieldCode, thresholdText);
      return `${fieldLabel} ${opText[rawVerifyOp] || rawVerifyOp} ${thText}`;
    });

    return `当 ${condFieldLabel} ${condOpText[condOp] || condOp} ${condThresholdText} 时, ${verifyMsgs.join('; ')}`;
  }

  // 原有单 verify IF (兼容)
  const ifMatch = logicRule.trim().match(/^IF\s*\(\s*\[([^\]]+)\]\s*(==|!=|<=|>=|<|>)\s*(\d+(?:\.\d+)?)\s*,\s*\[([^\]]+)\]\s*(==|!=|<=|>=|<|>)\s*(\d+(?:\.\d+)?)\s*,\s*TRUE\s*\)$/i);
  if (ifMatch) {
    const condCode = ifMatch[1]!.trim();
    const condOp = ifMatch[2]!;
    const condVal = ifMatch[3]!;
    const verifyCode = ifMatch[4]!.trim();
    const verifyOp = ifMatch[5]!;
    const verifyVal = ifMatch[6]!;

    const condOpText: Record<string, string> = { '==': '等于', '!=': '不等于', '>': '大于', '>=': '大于等于', '<': '小于', '<=': '小于等于' };
    const opText: Record<string, string> = { '>=': '应大于等于', '<=': '应小于等于', '>': '应大于', '<': '应小于' };

    // 条件阈值也需要显示 label
    const { fieldLabel: condFieldLabel, thresholdText: condThresholdText } = getFieldAndThresholdText(condCode, condVal);
    const { fieldLabel: verifyFieldLabel, thresholdText: verifyThresholdText } = getFieldAndThresholdText(verifyCode, verifyVal);

    return `当 ${condFieldLabel} ${condOpText[condOp] || condOp} ${condThresholdText} 时, ${verifyFieldLabel} ${opText[verifyOp] || verifyOp} ${verifyThresholdText}`;
  }

  const match = logicRule.trim().match(/^(.+?)\s*(>=|<=|>|<|==|!=)\s*(.+)$/);
  if (!match) return '校验失败';
  const leftRaw = match[1]!.trim();
  const operator = match[2]!;
  const rightRaw = match[3]!.trim();

  const opText: Record<string, string> = { '>=': '应大于等于', '<=': '应小于等于', '>': '应大于', '<': '应小于', '==': '应等于', '!=': '不应等于' };
  const msgLeft = leftRaw.replace(/\[([^\]]+)\]/g, (_, c) => replaceCode(c.trim()));
  const msgRight = rightRaw.replace(/\[([^\]]+)\]/g, (_, c) => replaceCode(c.trim()));
  return `「${msgLeft}」 ${opText[operator] || operator} 「${msgRight}」`;
}

// ==================== 查找字段 ====================

/** 查找条目中第一个有值的字段代码 */
function findFirstFilledFieldCode(indicator: DeclareIndicatorApi.Indicator, entryNumber: number): string | undefined {
  const entryKey = `${indicator.indicatorCode}${String(entryNumber).padStart(2, '0')}`;
  const entries = containerValues[indicator.indicatorCode] || [];
  const entry = entries.find((e: any) => e.rowKey === entryKey);
  if (!entry) return undefined;

  const fields = parseDynamicFields(indicator.valueOptions);
  for (const field of fields) {
    const fullKey = `${entryKey}${field.fieldCode}`;
    const value = entry[fullKey];
    if (value !== undefined && value !== null && value !== '') {
      return field.fieldCode;
    }
  }
  return fields[0]?.fieldCode;
}

// ==================== 清除容器逻辑规则错误 ====================

function clearContainerLogicRuleErrors(indicator: DeclareIndicatorApi.Indicator, clearFieldError: (key: string) => void) {
  const containerType = getContainerType(indicator.valueOptions);
  const entries = containerValues[indicator.indicatorCode] || [];

  // 只清除逻辑规则类型的错误，不清除基础验证错误（required/format/range）
  const shouldClear = (key: string) => {
    const err = fieldErrors[key];
    // 只清除逻辑规则错误；基础验证错误（required/format/range）由基础验证逻辑自行管理
    if (!err) return true;
    return err.errorType === 'logic' || err.errorType === 'joint';
  };

  if (containerType === 'conditional') {
    const fields = parseDynamicFields(indicator.valueOptions);
    const entries2 = containerValues[indicator.indicatorCode] || [];
    const entryRowKey = entries2[0]?.rowKey ?? indicator.indicatorCode;
    for (const field of fields) {
      const fieldKey = toContainerKey(entryRowKey, field.fieldCode);
      if (shouldClear(fieldKey)) {
        clearFieldError(fieldKey);
      }
    }
  } else {
    for (const entry of entries) {
      const fields = parseDynamicFields(indicator.valueOptions);
      for (const field of fields) {
        const fieldKey = toContainerKey(entry.rowKey, field.fieldCode);
        if (shouldClear(fieldKey)) {
          clearFieldError(fieldKey);
        }
      }
    }
  }
}

// ==================== 逻辑规则校验 ====================

/**
 * 完整逻辑规则校验（用于提交）
 */
function validateLogicRules(
  allIndicators: DeclareIndicatorApi.Indicator[],
  setFieldError: (key: string, message: string, type: FieldError['errorType'], dirty?: boolean) => void,
  clearFieldError: (key: string) => void,
): ValidationError[] {
  const errors: ValidationError[] = [];
  const codeValueMap: Record<string, any> = {};
  for (const ind of allIndicators) codeValueMap[ind.indicatorCode] = formValues[ind.indicatorCode];

  for (const indicator of allIndicators) {
    if (!indicator.logicRule?.trim()) continue;

    // 2. 新增：FORMATS 文件格式多样性验证
    const formatsRule = parseFORMATS(indicator.logicRule);
    if (formatsRule) {
      let fileList: { name: string }[] = [];
      const rawValue = formValues[indicator.indicatorCode];
      if (rawValue) {
        try {
          fileList = JSON.parse(rawValue);
        } catch { /* ignore */ }
      }

      const extToGroup: Record<string, string> = {};
      for (const [groupName, extensions] of Object.entries(formatsRule.typeGroups)) {
        for (const ext of extensions) {
          extToGroup[ext.toLowerCase()] = groupName.toLowerCase();
        }
      }

      const uploadedGroups = new Set<string>();
      for (const file of fileList) {
        const ext = file.name?.split('.').pop()?.toLowerCase() || '';
        if (extToGroup[ext]) {
          uploadedGroups.add(extToGroup[ext]);
        } else if (Object.keys(formatsRule.typeGroups).length === 0) {
          uploadedGroups.add(ext);
        }
      }

      const missing = formatsRule.requiredFormats
        .map((f) => f.toLowerCase())
        .filter((f) => !uploadedGroups.has(f));

      if (missing.length > 0) {
        const errMsg = `${indicator.indicatorName}：必须包含以下格式的文件：${missing.join('、')}`;
        if (indicator.id !== undefined) {
          setFieldError(`t:${indicator.id}`, errMsg, 'logic', false);
        }
        errors.push({
          indicatorId: indicator.id,
          indicatorCode: indicator.indicatorCode,
          message: errMsg,
          errorType: 'logic',
          indicatorName: indicator.indicatorName,
        });
      } else {
        if (indicator.id !== undefined) {
          clearFieldError(`t:${indicator.id}`);
        }
      }
      continue; // FORMATS 规则已处理，跳过后续的 joint rule 解析
    }

    // 检查联动状态：隐藏的指标不验证
    if (!isIndicatorVisible(indicator.indicatorCode)) {
      continue;
    }

    const containerType = getContainerType(indicator.valueOptions);

    if (isEntryBasedContainer(indicator) || containerType === 'conditional') {
      // 条件容器的 entryCount = 1（固定一条）
      const entryCount = containerType === 'conditional'
        ? 1
        : getContainerEntryCount(indicator.indicatorCode);
      if (entryCount === 0) continue;

      for (let entryNum = 1; entryNum <= entryCount; entryNum++) {
        // 条件容器的 entryKey 直接用 rowKey（= indicatorCode）
        const entryKey = containerType === 'conditional'
          ? indicator.indicatorCode
          : `${indicator.indicatorCode}${String(entryNum).padStart(2, '0')}`;

        const rules = parseLogicRule(indicator.logicRule, containerType === 'conditional' ? 1 : entryNum);
        if (rules.length === 0) continue;

        // 条件容器用不同的值映射函数
        const entryValueMap = containerType === 'conditional'
          ? buildEntryValueMapForConditional(indicator)
          : buildEntryValueMap(indicator, entryNum);

        const results = validateJointRule(rules as any, entryValueMap, { triggerTiming: 'FILL' });

        if (results.length === 0) continue;

        // 只处理第一个失败的 IF，修复后再处理下一个
        const firstFailed = results[0]!;

        // 根据 firstFailed 的 involvedIndicatorCodes 确定是哪个字段报错
        let fieldCode = '';
        if (firstFailed.involvedIndicatorCodes?.length) {
          const involvedCode = firstFailed.involvedIndicatorCodes.find((c) =>
            c.startsWith(indicator.indicatorCode)
          );
          if (involvedCode) {
            fieldCode = involvedCode.slice(-2);
          }
        }

        const allFields = parseDynamicFields(indicator.valueOptions);
        if (!fieldCode && containerType !== 'conditional') {
          for (const field of allFields) {
            const fullKey = `${entryKey}${field.fieldCode}`;
            const val = entryValueMap[fullKey];
            if (val !== undefined && val !== null && val !== '') {
              fieldCode = field.fieldCode;
              break;
            }
          }
          if (!fieldCode && allFields.length > 0) {
            fieldCode = allFields[0]?.fieldCode ?? '';
          }
        }

        // ========== 根据容器类型决定错误 key ==========
        let finalFieldCode = fieldCode;
        if (!finalFieldCode) {
          finalFieldCode = allFields?.[0]?.fieldCode ?? '';
        }
        const fullKey = containerType === 'conditional'
          ? finalFieldCode
          : `${entryKey}${finalFieldCode}`;
        // =============================================

        const errMsg = buildLogicRuleMsgForSingleRule(firstFailed);
        const errMsgWithKey = `「${fullKey}」：${errMsg}`;
        setFieldError(fullKey, errMsgWithKey, 'logic', false);

        errors.push({
          indicatorId: indicator.id,
          indicatorCode: indicator.indicatorCode,
          message: errMsgWithKey,
          errorType: 'logic',
          indicatorName: indicator.indicatorName,
          containerFieldKey: fullKey,
        });

        break; // 只报第一个，修复后再报下一个
      }
    } else {
      const rules = parseLogicRule(indicator.logicRule, 1);
      if (rules.length === 0) continue;
      const results = validateJointRule(rules as any, codeValueMap, { triggerTiming: 'FILL' });
      if (results.length === 0) {
        if (indicator.id !== undefined) {
          // 不能清除必填错误
          if (fieldErrors[`t:${indicator.id}`]?.errorType !== 'required') {
            clearFieldError(`t:${indicator.id}`);
          }
        }
        continue;
      }
      // 只处理第一个失败的 IF，修复后再处理下一个
      const firstFailed = results[0]!;
      const errMsg = buildLogicRuleMsgForSingleRule(firstFailed);
      if (indicator.id !== undefined) setFieldError(`t:${indicator.id}`, errMsg, 'logic', false);
      errors.push({ indicatorId: indicator.id, indicatorCode: indicator.indicatorCode, message: errMsg, errorType: 'logic', indicatorName: indicator.indicatorName });
    }
  }
  return errors;
}

/**
 * 失焦时校验（只校验涉及当前指标的规则）
 * 基础验证权重更高：如果基础验证（必填）没通过，则跳过逻辑验证
 * @returns 'early-return' | 'proceeded'
 */
function validateLogicRuleForBlur(
  changedIndicator: DeclareIndicatorApi.Indicator,
  allIndicators: DeclareIndicatorApi.Indicator[],
  setFieldError: (key: string, message: string, type: FieldError['errorType'], dirty?: boolean) => void,
  clearFieldError: (key: string) => void,
  setDirty: (key: string) => void,
): 'early-return' | 'proceeded' {
  const changedCode = changedIndicator.indicatorCode;

  // ========== 基础验证检查（仅对非容器指标生效） ==========
  // 容器指标的值在 containerValues 中，不在 formValues 中，跳过此检查
  // 容器字段的必填校验由 validateContainerFieldOnBlur 单独处理
  if (changedIndicator.valueType !== 12 && changedIndicator.isRequired && !isComputedIndicator(changedIndicator)) {
    const value = formValues[changedIndicator.indicatorCode];
    const isEmpty = value === undefined || value === null || value === '' ||
      (typeof value === 'string' && value.trim() === '') ||
      (Array.isArray(value) && value.length === 0);
    if (isEmpty) {
      return 'early-return'; // ← 被修改字段本身为空时，跳过逻辑验证（让必填来处理）
    }
  }
  // ========== 基础验证检查结束 ==========


  const codeValueMap: Record<string, any> = {};
  for (const ind of allIndicators) codeValueMap[ind.indicatorCode] = formValues[ind.indicatorCode];

  for (const indicator of allIndicators) {
    if (!indicator.logicRule?.trim()) continue;

    // 检查联动状态：隐藏的指标不验证
    const visible = isIndicatorVisible(indicator.indicatorCode);
    if (!visible) {
      // console.log('[LogicRuleBlur] 跳过隐藏指标:', indicator.indicatorCode);
      continue;
    }

    const involvedCodes = new Set<string>();
    for (const m of indicator.logicRule.matchAll(/\[([^\]]+)\]/g)) {
      involvedCodes.add(m[1]!.trim());
    }

    let involvesChanged = involvedCodes.has(changedCode);
    if (!involvesChanged && changedIndicator.valueType === 12) {
      for (const code of involvedCodes) {
        // 容器字段简写格式：502_07，检查 changedCode（容器code）是否为前缀
        // 例如 changedCode='502'，code='502_07' → '502_07'.startsWith('502_') → true
        if (isContainerFieldShortcut(code) && code.startsWith(changedCode + '_')) {
          involvesChanged = true;
          break;
        }
      }
    }

    if (!involvesChanged) {
      // FORMATS 规则特殊处理：检查 changedCode 是否就是本指标的 indicatorCode
      if (changedCode !== indicator.indicatorCode) continue;
      // changedCode === indicatorCode，说明本指标就是文件上传指标，需要重新验证 FORMATS
    }

    if (indicator.id !== undefined) setDirty(`t:${indicator.id}`);

    // FORMATS 规则：直接验证，不需要走 joint rule 引擎
    const formatsRule = parseFORMATS(indicator.logicRule);
    if (formatsRule) {
      let fileList: { name: string }[] = [];
      const rawValue = formValues[indicator.indicatorCode];
      if (rawValue) {
        try {
          fileList = JSON.parse(rawValue);
        } catch { /* ignore */ }
      }

      // 空文件列表时跳过验证（不报错，等用户上传）
      if (fileList.length === 0) {
        // 清除之前的错误
        if (indicator.id !== undefined) clearFieldError(`t:${indicator.id}`);
        continue;
      }

      const extToGroup: Record<string, string> = {};
      for (const [groupName, extensions] of Object.entries(formatsRule.typeGroups)) {
        for (const ext of extensions) {
          extToGroup[ext.toLowerCase()] = groupName.toLowerCase();
        }
      }

      const uploadedGroups = new Set<string>();
      for (const file of fileList) {
        const ext = file.name?.split('.').pop()?.toLowerCase() || '';
        if (extToGroup[ext]) {
          uploadedGroups.add(extToGroup[ext]);
        } else if (Object.keys(formatsRule.typeGroups).length === 0) {
          uploadedGroups.add(ext);
        }
      }

      const missing = formatsRule.requiredFormats
        .map((f) => f.toLowerCase())
        .filter((f) => !uploadedGroups.has(f));

      if (missing.length > 0) {
        const errMsg = `${indicator.indicatorName}：必须包含以下格式的文件：${missing.join('、')}`;
        if (indicator.id !== undefined) setFieldError(`t:${indicator.id}`, errMsg, 'logic', false);
      } else {
        if (indicator.id !== undefined) clearFieldError(`t:${indicator.id}`);
      }
      continue;
    }

    const containerType = getContainerType(indicator.valueOptions);

    if (isEntryBasedContainer(indicator) || containerType === 'conditional') {
      // 条件容器的 entryCount = 1（固定一条）
      const entryCount = containerType === 'conditional'
        ? 1
        : getContainerEntryCount(indicator.indicatorCode);
      if (entryCount === 0) continue;

      interface ContainerFieldErr { entryNum: number; fieldCode: string; errMsg: string }
      const logicFieldErrors: ContainerFieldErr[] = [];

      for (let entryNum = 1; entryNum <= entryCount; entryNum++) {
        // 条件容器的 entryKey 直接用 rowKey（= indicatorCode）
        const entryKey = containerType === 'conditional'
          ? indicator.indicatorCode
          : `${indicator.indicatorCode}${String(entryNum).padStart(2, '0')}`;

        const rules = parseLogicRule(indicator.logicRule, containerType === 'conditional' ? 1 : entryNum);
        if (rules.length === 0) continue;


        // 条件容器用不同的值映射函数
        const entryValueMap = containerType === 'conditional'
          ? buildEntryValueMapForConditional(indicator)
          : buildEntryValueMap(indicator, entryNum);

        const results = validateJointRule(rules as any, entryValueMap, { triggerTiming: 'FILL' });

        if (results.length > 0) {
          const involvedCodesRes = results[0]?.involvedIndicatorCodes || [];
          let hasSpecificFieldError = false;
          for (const code of involvedCodesRes) {
            if (code.startsWith(indicator.indicatorCode)) {
              hasSpecificFieldError = true;
              const fieldCode = code.slice(-2);
              const errMsg = buildLogicRuleMsg(indicator.logicRule, allIndicators, entryValueMap,
                containerType === 'conditional' ? 1 : entryNum, indicator);
              logicFieldErrors.push({ entryNum, fieldCode, errMsg });
            }
          }

          if (!hasSpecificFieldError) {
            const errMsg = buildLogicRuleMsg(indicator.logicRule, allIndicators, entryValueMap,
              containerType === 'conditional' ? 1 : entryNum, indicator);
            const firstFieldCode = findFirstFilledFieldCode(indicator, entryNum);
            if (firstFieldCode) {
              logicFieldErrors.push({ entryNum, fieldCode: firstFieldCode, errMsg });
            }
          }
        }
      }

      clearContainerLogicRuleErrors(indicator, clearFieldError);

      if (logicFieldErrors.length > 0) {
        const firstError = logicFieldErrors[0]!;
        let errorKey: string;
        if (containerType === 'conditional') {
          errorKey = firstError.fieldCode;
        } else {
          const entryKey = `${indicator.indicatorCode}${String(firstError.entryNum).padStart(2, '0')}`;
          errorKey = `${entryKey}${firstError.fieldCode}`;
        }
        setFieldError(errorKey, firstError.errMsg, 'logic', false);
        setDirty(errorKey);
      }
    } else {
      const rules = parseLogicRule(indicator.logicRule, 1);
      if (rules.length === 0) continue;

      // 检查被引用字段是否都有值——只有全部有值才能判断"通过"
      const involvedCodes: string[] = [];
      for (const m of indicator.logicRule.matchAll(/\[([^\]]+)\]/g)) {
        involvedCodes.push(m[1]!.trim());
      }
      const allRefFieldsFilled = involvedCodes.every((code) => {
        const val = formValues[code];
        return val !== undefined && val !== null && val !== '';
      });

      const results = validateJointRule(rules as any, codeValueMap, { triggerTiming: 'FILL' });
      if (results.length > 0) {
        const errMsg = buildLogicRuleMsg(indicator.logicRule, allIndicators, codeValueMap);
        if (indicator.id !== undefined) setFieldError(`t:${indicator.id}`, errMsg, 'logic', false);
      } else if (allRefFieldsFilled) {
        // 被引用字段全都有值，验证真正通过 → 清除逻辑错误
        if (indicator.id !== undefined && fieldErrors[`t:${indicator.id}`]?.errorType !== 'required') {
          clearFieldError(`t:${indicator.id}`);
        }
      } else {
        // 被引用字段中有空值，数据不完整 → 保留（不设置也不清除）
      }
    }
  }
  return 'proceeded';
}

/**
 * 验证已填数据的逻辑规则（不做必填校验）
 */
function validateFilledLogicRules(
  indicatorsToValidate: DeclareIndicatorApi.Indicator[],
  setFieldError: (key: string, message: string, type: FieldError['errorType'], dirty?: boolean) => void,
  clearFieldError: (key: string) => void,
): ValidationError[] {
  const errors: ValidationError[] = [];
  const codeValueMap = buildCodeValueMap();

  for (const indicator of indicatorsToValidate) {
    if (!indicator.logicRule?.trim()) continue;

    // 检查联动状态：隐藏的指标不验证
    if (!isIndicatorVisible(indicator.indicatorCode)) {
      continue;
    }

    // FORMATS 文件格式多样性验证
    const formatsRule = parseFORMATS(indicator.logicRule);
    if (formatsRule) {
      let fileList: { name: string }[] = [];
      const rawValue = formValues[indicator.indicatorCode];
      if (rawValue) {
        try {
          fileList = JSON.parse(rawValue);
        } catch { /* ignore */ }
      }

      if (fileList.length === 0) continue; // 空文件不报错

      const extToGroup: Record<string, string> = {};
      for (const [groupName, extensions] of Object.entries(formatsRule.typeGroups)) {
        for (const ext of extensions) {
          extToGroup[ext.toLowerCase()] = groupName.toLowerCase();
        }
      }

      const uploadedGroups = new Set<string>();
      for (const file of fileList) {
        const ext = file.name?.split('.').pop()?.toLowerCase() || '';
        if (extToGroup[ext]) {
          uploadedGroups.add(extToGroup[ext]);
        } else if (Object.keys(formatsRule.typeGroups).length === 0) {
          uploadedGroups.add(ext);
        }
      }

      const missing = formatsRule.requiredFormats
        .map((f) => f.toLowerCase())
        .filter((f) => !uploadedGroups.has(f));

      if (missing.length > 0) {
        const errMsg = `${indicator.indicatorName}：必须包含以下格式的文件：${missing.join('、')}`;
        errors.push({
          indicatorId: indicator.id,
          indicatorCode: indicator.indicatorCode,
          message: errMsg,
          errorType: 'logic',
          indicatorName: indicator.indicatorName,
        });
        if (indicator.id !== undefined) setFieldError(`t:${indicator.id}`, errMsg, 'logic', false);
      } else {
        if (indicator.id !== undefined) clearFieldError(`t:${indicator.id}`);
      }
      continue;
    }

    if (isEntryBasedContainer(indicator)) {
      const entryCount = getContainerEntryCount(indicator.indicatorCode);
      if (entryCount === 0) continue;

      for (let entryNum = 1; entryNum <= entryCount; entryNum++) {
        const rules = parseLogicRule(indicator.logicRule, entryNum);
        if (rules.length === 0) continue;

        const entryValueMap = buildEntryValueMap(indicator, entryNum);
        const results = validateJointRule(rules as any, entryValueMap, { triggerTiming: 'FILL' });

        if (results.length > 0) {
          // 只处理第一个失败的 IF，修复后再处理下一个
          const firstFailed = results[0]!;
          const errMsg = buildLogicRuleMsgForSingleRule(firstFailed);
          const entryKey = `${indicator.indicatorCode}${String(entryNum).padStart(2, '0')}`;

          let fieldCode = '';
          if (firstFailed.involvedIndicatorCodes?.length) {
            const involvedCode = firstFailed.involvedIndicatorCodes.find((c) =>
              c.startsWith(indicator.indicatorCode)
            );
            if (involvedCode) {
              fieldCode = involvedCode.slice(-2);
            }
          }

          if (!fieldCode) {
            fieldCode = findFirstFilledFieldCode(indicator, entryNum) || '';
          }

          const containerFieldKey = fieldCode ? toContainerKey(entryKey, fieldCode) : entryKey;
          const fullKey = containerFieldKey;
          const errMsgWithKey = `「${containerFieldKey}」：${errMsg}`;
          setFieldError(fullKey, errMsgWithKey, 'logic', false);

          errors.push({
            indicatorId: indicator.id,
            indicatorCode: indicator.indicatorCode,
            message: errMsgWithKey,
            errorType: 'logic',
            indicatorName: indicator.indicatorName,
            containerFieldKey,
          });

          break; // 只报第一个，修复后再报下一个
        }
      }
    } else {
      const rules = parseLogicRule(indicator.logicRule, 1);
      if (rules.length === 0) continue;

      const involvedCodes: string[] = [];
      for (const m of indicator.logicRule.matchAll(/\[([^\]]+)\]/g)) {
        involvedCodes.push(m[1]!.trim());
      }
      const allRefFieldsFilled = involvedCodes.every((code) => {
        const val = formValues[code];
        return val !== undefined && val !== null && val !== '';
      });

      if (!allRefFieldsFilled) {
        // 数据不完整，跳过
        continue;
      }

      const results = validateJointRule(rules as any, codeValueMap, { triggerTiming: 'FILL' });

      if (results.length > 0) {
        // 只处理第一个失败的 IF，修复后再处理下一个
        const firstFailed = results[0]!;
        const errMsg = buildLogicRuleMsgForSingleRule(firstFailed);
        errors.push({
          indicatorId: indicator.id,
          indicatorCode: indicator.indicatorCode,
          message: errMsg,
          containerFieldKey: undefined,
          errorType: 'logic',
          indicatorName: indicator.indicatorName,
        });
        if (indicator.id !== undefined) setFieldError(`t:${indicator.id}`, errMsg, 'logic', false);
      } else {
        // 验证通过
        if (indicator.id !== undefined && fieldErrors[`t:${indicator.id}`]?.errorType !== 'required') {
          clearFieldError(`t:${indicator.id}`);
        }
      }
    }
  }

  return errors;
}

/**
 * 单行容器逻辑校验（用于容器字段 blur 时的即时校验）
 * 容器内逻辑验证：只针对当前 entry 行，不做跨行验证
 * @returns 错误信息，无错返回 null
 */
function validateSingleContainerLogicRule(
  indicator: DeclareIndicatorApi.Indicator,
  entryNum: number,
  entryKey: string,
  allIndicators: DeclareIndicatorApi.Indicator[],
): { fieldKey: string; errMsg: string } | null {
  const containerType = getContainerType(indicator.valueOptions);
  const rules = parseLogicRule(indicator.logicRule, containerType === 'conditional' ? 1 : entryNum);
  if (rules.length === 0) return null;

  const entryValueMap = containerType === 'conditional'
    ? buildEntryValueMapForConditional(indicator)
    : buildEntryValueMap(indicator, entryNum);

  const results = validateJointRule(rules as any, entryValueMap, { triggerTiming: 'FILL' });
  if (results.length === 0) return null;

  const errMsg = buildLogicRuleMsg(
    indicator.logicRule,
    allIndicators,
    entryValueMap,
    containerType === 'conditional' ? 1 : entryNum,
    indicator,
  );

  const involvedCodes = results[0]?.involvedIndicatorCodes || [];
  let fieldCode = '';

  for (const code of involvedCodes) {
    if (code.startsWith(indicator.indicatorCode)) {
      fieldCode = code.slice(-2);
      break;
    }
  }

  if (!fieldCode) {
    const firstFilled = findFirstFilledFieldCode(indicator, entryNum);
    if (firstFilled) fieldCode = firstFilled;
  }

  const fieldKey = containerType === 'conditional' ? fieldCode : toContainerKey(entryKey, fieldCode);
  return { fieldKey, errMsg };
}

// ==================== 容器级联互斥约束校验 ====================

/**
 * 校验容器级联互斥约束
 *
 * 公式：CC([602_07,602_06,602_05,602_04,602_03], LAST_EQ(2), REST_NE(1))
 * 语义：完整扫描字段列表，找到第一个满足 triggerOp(triggerVal) 的字段，
 *       约束其之后所有字段不能选 forbidVal。
 *
 * @param rule  解析后的 CC 规则（fieldCodes 已是展开后的完整 key，如 6020107）
 * @param entry 条目数据
 * @param indicator 指标配置，用于获取字段选项以展示友好文字
 * @returns 违规字段的错误列表
 */
function validateContainerConstraint(
  rule: { fieldCodes: string[]; originalFieldCodes: string[]; triggerOp: string; triggerVal: string; forbidOp: string; forbidVal: string },
  entry: any,
  indicator: DeclareIndicatorApi.Indicator,
): { fieldKey: string; errMsg: string }[] {
  const { fieldCodes, originalFieldCodes, triggerOp, triggerVal, forbidOp, forbidVal } = rule;
  const errors: { fieldKey: string; errMsg: string }[] = [];

  // 构建 fieldCode（两位，如 07）→ DynamicField 映射，用于查找选项 label
  const fields = parseDynamicFields(indicator.valueOptions);
  const fieldMap: Record<string, DynamicField> = {};
  for (const f of fields) {
    fieldMap[f.fieldCode] = f;
  }

  /**
   * 将字段值转换为友好显示文字
   * 单选/下拉类字段：返回 options 中的 label；其他类型：返回原始值
   */
  const formatFieldValue = (fieldCode: string, val: any): string => {
    const field = fieldMap[fieldCode];
    if (field?.options && Array.isArray(field.options)) {
      const found = field.options.find((o: { value: string; label: string }) => String(o.value) === String(val ?? ''));
      if (found) return found.label;
    }
    return String(val ?? '');
  };

  /** 判断单个字段值是否满足触发条件 */
  const matchesTrigger = (val: any): boolean => {
    const s = String(val ?? '');
    switch (triggerOp) {
      case 'FIRST_EQ': case 'LAST_EQ': return s === triggerVal;
      case 'FIRST_GT': return !isNaN(Number(val)) && Number(val) > Number(triggerVal);
      case 'FIRST_GTE': return !isNaN(Number(val)) && Number(val) >= Number(triggerVal);
      case 'FIRST_LT': return !isNaN(Number(val)) && Number(val) < Number(triggerVal);
      case 'FIRST_LTE': return !isNaN(Number(val)) && Number(val) <= Number(triggerVal);
      case 'FIRST_IN': case 'LAST_IN': return triggerVal.split(',').map((v) => v.trim()).includes(s);
      default: return false;
    }
  };

  /** 判断单个字段值是否违反约束条件 */
  const violatesForbid = (val: any): boolean => {
    const s = String(val ?? '');
    switch (forbidOp) {
      case 'REST_EQ': return s === forbidVal;
      case 'REST_NE': return s === forbidVal;
      default: return false;
    }
  };

  // 找触发字段索引（FIRST_* 从前扫，LAST_* 从后扫）
  // fieldCodes 已是展开后的完整 key（如 6020107），直接用 fieldCodes[i] 访问 entry
  let triggerIdx = -1;
  if (triggerOp.startsWith('FIRST_')) {
    for (let i = 0; i < fieldCodes.length; i++) {
      if (matchesTrigger(entry[fieldCodes[i]!])) { triggerIdx = i; break; }
    }
  } else {
    for (let i = fieldCodes.length - 1; i >= 0; i--) {
      if (matchesTrigger(entry[fieldCodes[i]!])) { triggerIdx = i; break; }
    }
  }

  if (triggerIdx === -1) return []; // 无触发 → 通过

  // 检查触发字段之后的字段
  for (let i = triggerIdx + 1; i < fieldCodes.length; i++) {
    const code = fieldCodes[i]!;
    const originalCode = originalFieldCodes[i]!;
    if (violatesForbid(entry[code])) {
      const triggerFieldCode = fieldCodes[triggerIdx]!.slice(-2);
      const forbidFieldCode = code.slice(-2);
      const triggerFieldLabel = fieldMap[triggerFieldCode]?.fieldLabel ?? originalFieldCodes[triggerIdx]!;
      const forbidFieldLabel = fieldMap[forbidFieldCode]?.fieldLabel ?? originalCode;
      // 获取选项文字
      const triggerValueLabel = formatFieldValue(triggerFieldCode, triggerVal);
      const forbidValueLabel = formatFieldValue(forbidFieldCode, forbidVal);
      const fieldKey = code;
      errors.push({
        fieldKey,
        errMsg: `「${triggerFieldLabel}」选择了「${triggerValueLabel}」，「${forbidFieldLabel}」不能选择「${forbidValueLabel}」`,
      });
    }
  }

  return errors;
}

// ==================== 导出 ====================

export {
  buildEntryValueMap,
  buildCodeValueMap,
  buildLogicRuleMsg,
  findFirstFilledFieldCode,
  clearContainerLogicRuleErrors,
  validateSingleContainerLogicRule,
  validateLogicRules,
  validateLogicRuleForBlur,
  validateFilledLogicRules,
  validateContainerConstraint,
};
