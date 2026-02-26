/**
 * 规则引擎类型定义
 * 支持多种规则类型的可视化配置
 */

/** 规则类型枚举 */
export enum RuleType {
  /** 公式验证：指标A + 指标B > 指标C */
  FORMULA = 1,
  /** 必填验证：有指标A时，指标B必填 */
  REQUIRED = 2,
  /** 互斥验证：不能同时选择指标A和指标B */
  MUTUAL_EXCLUDE = 3,
  /** 依赖验证：填指标A时建议填指标B */
  DEPENDENCY = 4,
  /** 范围验证：指标值必须在指定范围内 */
  RANGE = 5,
}

/** 规则级别 */
export enum RuleLevel {
  /** 强校验：不通过禁止提交/流程流转 */
  ERROR = 1,
  /** 弱校验：仅提示不拦截 */
  WARNING = 2,
}

/** 指标项配置 */
export interface IndicatorItem {
  /** 指标ID */
  indicatorId: number;
  /** 指标名称（冗余显示用） */
  indicatorName: string;
  /** 指标编码（冗余显示用） */
  indicatorCode?: string;
  /** 运算符（用于公式中间项） */
  operator?: '+' | '-' | '*' | '/';
}

/** 表达式配置 */
export interface ExpressionConfig {
  /** 表达式类型：single=单指标, formula=多指标公式, fixed=固定值 */
  type: 'single' | 'formula' | 'fixed';
  /** 单指标或公式的指标列表 */
  items?: IndicatorItem[];
  /** 固定值 */
  value?: string | number;
  /** 指标乘数（如：病房数 × 3） */
  multiplier?: number;
}

/** 单条规则配置 */
export interface RuleItem {
  /** 规则ID（用于编辑时识别） */
  id?: string | number;
  /** 规则名称 */
  name: string;
  /** 规则类型 */
  ruleType: RuleType;
  /** 条件表达式（前置条件） */
  condition?: {
    /** 条件指标 */
    indicatorId: number;
    /** 条件指标名称 */
    indicatorName?: string;
    /** 条件运算符 */
    operator: 'empty' | 'not_empty' | 'eq' | 'ne' | 'gt' | 'ge' | 'lt' | 'le';
    /** 比较值 */
    value?: string | number;
    /** 关联指标ID（用于与某指标比较） */
    compareIndicatorId?: number;
  };
  /** 动作配置 */
  action: {
    /** 动作类型 */
    type: 'formula' | 'required' | 'mutual_exclude' | 'dependency' | 'range';
    /** 公式表达式列表（简化的链式设计） */
    formula?: {
      /** 值类型：indicator=指标，fixed=固定值 */
      valueType: 'indicator' | 'fixed';
      /** 指标ID */
      indicatorId?: number;
      /** 指标名称 */
      indicatorName?: string;
      /** 指标编码 */
      indicatorCode?: string;
      /** 固定值 */
      fixedValue?: number;
      /** 数学运算符（连接下一个元素）：+、-、×、÷ */
      mathOp?: '+' | '-' | '*' | '/';
    }[];
    /** 比较运算符 */
    operator?: '>' | '<' | '>=' | '<=' | '==' | '!=';
    /** 比较值类型 */
    compareType?: 'fixed' | 'indicator';
    /** 比较固定值 */
    compareValue?: number;
    /** 比较指标ID */
    compareIndicatorId?: number;
    /** 比较指标名称 */
    compareIndicatorName?: string;
    /** 左侧表达式（兼容旧数据） */
    left?: ExpressionConfig;
    /** 右侧表达式（兼容旧数据） */
    right?: ExpressionConfig;
    /** 目标指标ID（用于必填/互斥/依赖） */
    targetIndicatorId?: number;
    /** 目标指标名称 */
    targetIndicatorName?: string;
    /** 范围下限 */
    minValue?: number;
    /** 范围上限 */
    maxValue?: number;
    /** 规则级别 */
    level: RuleLevel;
    /** 提示信息 */
    message: string;
  };
}

/** 规则组配置（存储在 ruleConfig 字段中） */
export interface RuleGroupConfig {
  /** 规则列表 */
  rules: RuleItem[];
}

/** 规则配置的 JSON Schema 示例 */
// {
  // "rules": [
  //   {
  //     "id": 1,
//       "name": "床位数与病房数关系验证",
//       "ruleType": 1,
//       "condition": {
//         "indicatorId": 101,
//         "operator": "not_empty"
//       },
//       "action": {
//         "type": "formula",
//         "left": {
//           "type": "single",
//           "items": [{ "indicatorId": 101, "indicatorName": "床位数" }]
//         },
//         "operator": ">=",
//         "right": {
//           "type": "formula",
//           "items": [
//             { "indicatorId": 102, "indicatorName": "病房数", "operator": "*" },
//             { "indicatorId": 0, "value": 3 }
//           ]
//         },
//         "level": 1,
//         "message": "床位数必须大于等于病房数×3"
//       }
//     }
//   ]
// }

/** 触发时机选项 */
export const TRIGGER_TIMING_OPTIONS = [
  { label: '填报时（实时验证）', value: 'FILL' },
  { label: '流程提交时', value: 'PROCESS_SUBMIT' },
];

/** 规则类型选项 */
export const RULE_TYPE_OPTIONS = [
  { label: '公式验证', value: RuleType.FORMULA, description: '指标A + 指标B > 指标C' },
  { label: '必填验证', value: RuleType.REQUIRED, description: '有指标A时，指标B必填' },
  { label: '互斥验证', value: RuleType.MUTUAL_EXCLUDE, description: '不能同时选择指标A和指标B' },
  { label: '依赖验证', value: RuleType.DEPENDENCY, description: '填指标A时建议填指标B' },
  { label: '范围验证', value: RuleType.RANGE, description: '指标值必须在指定范围内' },
];

/** 规则级别选项 */
export const RULE_LEVEL_OPTIONS = [
  { label: '错误（阻止提交）', value: RuleLevel.ERROR },
  { label: '警告（仅提示）', value: RuleLevel.WARNING },
];

/** 条件运算符选项 */
export const CONDITION_OPERATOR_OPTIONS = [
  { label: '为空', value: 'empty' },
  { label: '不为空', value: 'not_empty' },
  { label: '等于', value: 'eq' },
  { label: '不等于', value: 'ne' },
  { label: '大于', value: 'gt' },
  { label: '大于等于', value: 'ge' },
  { label: '小于', value: 'lt' },
  { label: '小于等于', value: 'le' },
];

/** 比较运算符选项 */
export const COMPARE_OPERATOR_OPTIONS = [
  { label: '>', value: '>' },
  { label: '<', value: '<' },
  { label: '>=', value: '>=' },
  { label: '<=', value: '<=' },
  { label: '=', value: '==' },
  { label: '≠', value: '!=' },
];

/** 公式运算符选项 */
export const FORMULA_OPERATOR_OPTIONS = [
  { label: '+', value: '+' },
  { label: '-', value: '-' },
  { label: '×', value: '*' },
  { label: '÷', value: '/' },
];
