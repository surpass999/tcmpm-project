/**
 * 上期对比规则类型定义
 * 用于指标上期值校验规则配置
 */

/** 规则级别 */
export enum RuleLevel {
  /** 强校验：不通过禁止提交/流程流转 */
  ERROR = 1,
  /** 弱校验：仅提示不拦截 */
  WARNING = 2,
}

/** 值类型（与后端 DeclareIndicatorDO.valueType 保持一致） */
export enum ValueType {
  /** 数字 */
  NUMBER = 1,
  /** 单选 */
  RADIO = 6,
  /** 多选 */
  MULTI_SELECT = 7,
}

/** 比较模式 */
export enum CompareMode {
  /** 正向：越大越好 */
  POSITIVE = 'positive',
  /** 负向：越小越好 */
  NEGATIVE = 'negative',
}

/** 多选比较类型 */
export enum MultiSelectCompareType {
  /** 最低等级 */
  MIN_LEVEL = 'min_level',
  /** 最高等级 */
  MAX_LEVEL = 'max_level',
  /** 选项数量 */
  COUNT = 'count',
  /** 新增数量 */
  NEW_COUNT = 'new_count',
  /** 上期必须保持 */
  KEEP_REQUIRED = 'keep_required',
}

/** 规则选项 */
export interface PositiveRuleOption {
  /** 选项值 */
  value: string;
  /** 选项标签 */
  label: string;
}

/** 单条上期对比规则 */
export interface PositiveRuleItem {
  /** 规则名称 */
  name: string;
  /** 指标编码 */
  indicatorCode: string;
  /** 值类型：1=数字, 2=单选, 3=多选 */
  valueType: ValueType;
  /** 比较模式：positive=正向, negative=负向 */
  compareMode: CompareMode;
  /** 比较子类型 */
  compareType?: 'number' | 'radio' | 'min_level' | 'max_level' | 'count' | 'new_count' | 'keep_required';
  /** 选项列表（单选/多选用） */
  options?: PositiveRuleOption[];
  /** 排除选项（不参与比较） */
  excludeOptions?: string[];
  /** 最小新增数量（new_count 用） */
  minNewCount?: number;
}

/** 上期对比规则配置 */
export interface PositiveRuleConfig {
  /** 规则组名称 */
  groupName?: string;
  /** 优先级 */
  priority?: number;
  /** 规则列表 */
  rules: PositiveRuleItem[];
}

/** 规则级别选项 */
export const RULE_LEVEL_OPTIONS = [
  { label: '错误（阻止提交）', value: RuleLevel.ERROR },
  { label: '警告（仅提示）', value: RuleLevel.WARNING },
];

/** 值类型选项 */
export const VALUE_TYPE_OPTIONS = [
  { label: '数字', value: ValueType.NUMBER, description: '数值比较，如项目进度、资金数额' },
  { label: '单选', value: ValueType.RADIO, description: '选项等级比较，如项目阶段' },
  { label: '多选', value: ValueType.MULTI_SELECT, description: '选项集合比较，如任务完成情况' },
];

/** 比较模式选项 */
export const COMPARE_MODE_OPTIONS = [
  { label: '正向', value: CompareMode.POSITIVE, description: '数值越大越好 / 选项等级越高越好（只能升级）' },
  { label: '负向', value: CompareMode.NEGATIVE, description: '数值越小越好 / 选项等级越低越好（只能降级）' },
];

/** 正向模式说明 */
export const POSITIVE_MODE_HELP = {
  NUMBER: '数值必须 >= 上期值',
  RADIO: '所选选项等级必须 >= 上期等级（只能升级，不能降级）',
  MULTI_SELECT: '选中选项的等级/数量必须满足比较类型要求',
};

/** 负向模式说明 */
export const NEGATIVE_MODE_HELP = {
  NUMBER: '数值必须 <= 上期值',
  RADIO: '所选选项等级必须 <= 上期等级（只能降级，不能升级）',
  MULTI_SELECT: '选中选项的等级/数量必须满足比较类型要求',
};

/** 多选比较类型选项 */
export const MULTI_SELECT_COMPARE_TYPE_OPTIONS = [
  { label: '最低等级', value: MultiSelectCompareType.MIN_LEVEL, description: '选中选项的最低等级不能低于上期' },
  { label: '最高等级', value: MultiSelectCompareType.MAX_LEVEL, description: '选中选项的最高等级不能低于上期' },
  { label: '选项数量', value: MultiSelectCompareType.COUNT, description: '选中数量不能少于上期' },
  { label: '新增数量', value: MultiSelectCompareType.NEW_COUNT, description: '本期新增选项数量不能少于指定值' },
  { label: '上期必须保持', value: MultiSelectCompareType.KEEP_REQUIRED, description: '上期选中的选项本期必须继续保持选中' },
];
