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
  /** 文本 */
  TEXT = 2,
  /** 单选 */
  RADIO = 6,
  /** 多选 */
  MULTI_SELECT = 7,
  /** 容器（动态条目容器） */
  CONTAINER = 12,
}

/** 比较模式 */
export enum CompareMode {
  /** 正向：越大越好 */
  POSITIVE = 'positive',
  /** 负向：越小越好 */
  NEGATIVE = 'negative',
}

/** 文本比较类型 */
export enum TextCompareType {
  /** 必须相等 */
  EQUAL = 'equal',
  /** 必须包含 */
  CONTAIN = 'contain',
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

/** 容器字段值类型（与容器内字段类型对应） */
export enum ContainerFieldValueType {
  /** 文本 */
  TEXT = 2,
  /** 数字 */
  NUMBER = 1,
  /** 单选 */
  RADIO = 6,
  /** 下拉 */
  SELECT = 10,
  /** 多选 */
  MULTI_SELECT = 7,
}

/** 容器字段上期值验证配置 */
export interface ContainerPositiveRuleField {
  /** 字段编码 */
  fieldCode: string;
  /** 字段名称 */
  fieldLabel: string;
  /** 字段值类型 */
  fieldValueType: ContainerFieldValueType;
  /** 比较模式 */
  compareMode: CompareMode;
  /** 比较类型 */
  compareType?: 'number' | 'radio' | 'equal' | 'min_level' | 'max_level' | 'count' | 'new_count' | 'keep_required';
  /** 选项列表 */
  options?: PositiveRuleOption[];
  /** 排除选项（不参与比较） */
  excludeOptions?: string[];
  /** 最小新增数量（new_count 用） */
  minNewCount?: number;
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
  /** 值类型：1=数字, 2=文本, 6=单选, 7=多选, 12=容器 */
  valueType: ValueType;
  /** 比较模式：positive=正向, negative=负向 */
  compareMode: CompareMode;
  /** 比较子类型 */
  compareType?: 'number' | 'radio' | 'equal' | 'min_level' | 'max_level' | 'count' | 'new_count' | 'keep_required';
  /** 选项列表（单选/多选用） */
  options?: PositiveRuleOption[];
  /** 排除选项（不参与比较） */
  excludeOptions?: string[];
  /** 最小新增数量（new_count 用） */
  minNewCount?: number;
  /** 容器字段配置（仅 valueType === CONTAINER 时有效） */
  containerFields?: ContainerPositiveRuleField[];
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
  { label: '文本', value: ValueType.TEXT, description: '文本完全相等比较，如项目名称' },
  { label: '单选', value: ValueType.RADIO, description: '选项等级比较，如项目阶段' },
  { label: '多选', value: ValueType.MULTI_SELECT, description: '选项集合比较，如任务完成情况' },
  { label: '容器', value: ValueType.CONTAINER, description: '动态容器字段上期值比较' },
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

/** 容器字段默认值比较类型映射 */
export const CONTAINER_FIELD_DEFAULT_COMPARE_TYPE: Record<number, string> = {
  [ContainerFieldValueType.TEXT]: 'equal',
  [ContainerFieldValueType.NUMBER]: 'number',
  [ContainerFieldValueType.RADIO]: 'radio',
  [ContainerFieldValueType.SELECT]: 'radio',
  [ContainerFieldValueType.MULTI_SELECT]: 'count',
};

/** 文本比较类型选项 */
export const TEXT_COMPARE_TYPE_OPTIONS = [
  { label: '必须相等', value: TextCompareType.EQUAL, description: '本期值必须与上期完全相等' },
  { label: '必须包含', value: TextCompareType.CONTAIN, description: '本期值必须包含上期内容' },
];

/** 容器字段值类型选项 */
export const CONTAINER_FIELD_VALUE_TYPE_OPTIONS = [
  { label: '文本', value: ContainerFieldValueType.TEXT, description: '文本完全相等比较' },
  { label: '数字', value: ContainerFieldValueType.NUMBER, description: '数值比较' },
  { label: '单选', value: ContainerFieldValueType.RADIO, description: '选项等级比较' },
  { label: '下拉', value: ContainerFieldValueType.SELECT, description: '下拉单选' },
  { label: '多选', value: ContainerFieldValueType.MULTI_SELECT, description: '选项集合比较' },
];

/** 容器字段多选比较类型选项（与顶层多选一致） */
export const CONTAINER_FIELD_MULTI_COMPARE_TYPE_OPTIONS = MULTI_SELECT_COMPARE_TYPE_OPTIONS;
