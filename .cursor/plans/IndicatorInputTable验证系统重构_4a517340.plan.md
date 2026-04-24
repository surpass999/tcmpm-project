---
name: IndicatorInputTable 验证系统重构
overview: 将验证引擎迁移到组件内并拆分为 3 个专用模块（types/parser/engine），内置操作符枚举 + compareEngine 比较引擎，精简 composables 层，清晰化 3 层验证流水线职责边界
todos:
  - id: 43fb714d-7fbd-4993-8511-3415df4df9f9
    content: "Phase 1: 迁移 indicatorValidator.ts 到组件目录，更新 import 路径，清理框架层旧文件"
    status: pending
  - id: 183e2bfe-bac5-4692-9cce-c65b9825e644
    content: "Phase 2: 新建 indicator-validator/ 目录，创建 types.ts（含操作符枚举）+ 重写 parser.ts（含枚举集成）"
    status: pending
  - id: 0f29d6d9-fe17-4c46-9609-c714667c49e3
    content: "Phase 3: 重写 engine.ts（含 compareEngine 子模块），将验证/消息构建函数从 legacy validator.ts 迁入"
    status: pending
  - id: da2a74ba-35e2-4960-b6ed-a2320f0bc7ec
    content: "Phase 4: 重构 useLogicRules.ts（合并 build* 函数/提取 validateFORMATS/精简 4 个 validate* 函数/删除 validateContainerConstraint/buildLogicRuleMsg）"
    status: pending
  - id: 61734ba2-3db6-42fe-8f65-c3bc501cd3f7
    content: "Phase 5: 优化 usePositiveRules.ts（提取 makePositiveErrorKey/compareByType）"
    status: pending
  - id: 1e679990-39bf-4468-bce2-c23e02976cc0
    content: "Phase 6: 优化 useValidation.ts（拆分 validateAll 为 validateTopLevel/validateContainer）"
    status: pending
  - id: d2edb8c9-d29d-47c8-877e-e53386da908c
    content: "Phase 7: 验证测试（IF/CC/FORMATS/普通规则/3层流水线/提交校验）"
    status: pending
isProject: true
---

# IndicatorInputTable 验证系统重构详细计划

## 一、当前问题总结

### 1.1 `indicatorValidator.ts` — 职责膨胀（1216行）


| 问题          | 说明                                                      |
| ----------- | ------------------------------------------------------- |
| **类型定义散落**  | 全部 interface 混在一起，没有独立文件                                |
| **字符串解析**   | 380行复杂手写解析器（括号匹配、深度计数）                                  |
| **验证引擎**    | 核心 `validateRule` 函数过长（250行+），读/值/比较逻辑全挤在一起             |
| **错误消息构建**  | `buildLogicRuleMsgForSingleRule` 162行全靠5个正则分支硬编码        |
| **工具函数混杂**  | `parseContainerFieldShortcut` 等边界不清                     |
| **CC 规则分散** | 解析在 `indicatorValidator.ts`，执行在 `useLogicRules.ts:1026` |


### 1.2 `useLogicRules.ts` — 逻辑泄漏（1132行）


| 问题                 | 说明                                                                                           |
| ------------------ | -------------------------------------------------------------------------------------------- |
| **构建值映射重复**        | `buildEntryValueMap`、`buildEntryValueMapForConditional`、`buildCodeValueMap` 三个函数功能相近         |
| **错误消息构建重复**       | `buildLogicRuleMsg`（composable内）与 `buildLogicRuleMsgForSingleRule`（引擎内）完全重复                  |
| **CC 规则执行逻辑泄漏**    | `validateContainerConstraint`（106行）应该属于引擎，不属于 composable                                     |
| **FORMATS 规则重复实现** | `validateLogicRules`、`validateLogicRuleForBlur`、`validateFilledLogicRules` 各写一套 FORMATS 验证逻辑 |
| *validate 函数职责重叠** | 4个校验函数，逻辑相似度高，大量重复分支                                                                         |


---

## 二、目标架构：5模块 + 3层流水线

```
┌─────────────────────────────────────────────────────────────┐
│                    验证系统架构                               │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│  IndicatorInputTable/utils/indicator-validator/              │
│  组件内聚验证引擎：仅供 IndicatorInputTable 使用              │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    模块 ① types.ts                          │
│  单一类型定义文件：所有验证相关类型（引擎 + composable 共享）    │
│  路径：`IndicatorInputTable/utils/indicator-validator/types.ts` │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    模块 ② parser.ts                          │
│  字符串解析器：规则字符串 → 结构化对象                         │
│  路径：`IndicatorInputTable/utils/indicator-validator/parser.ts` │
│  - normalize()                归一化（空格、TRUE大小写）        │
│  - detectRuleType()           自动检测 IF/SIMPLE/CC/FORMATS   │
│  - parseLogicRule()           主入口：按类型分发解析            │
│  - parseFormulaSide()          公式解析（[802]+[803]）        │
│  - parseConditionOperator()     条件操作符（IN/NOT_IN/比较符）  │
│  - parseCC()                   CC 规则解析                   │
│  - parseFORMATS()             FORMATS 规则解析              │
│  - parseContainerFieldShortcut() 容器字段简写展开（从 validator.ts 迁移）│
│  - isContainerFieldShortcut()  判断简写格式（从 validator.ts 迁移）│
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    模块 ③ engine.ts                          │
│  验证执行引擎：结构化规则对象 → ValidationResult[]             │
│  路径：`IndicatorInputTable/utils/indicator-validator/engine.ts` │
│  - validate()                  规则执行主入口（过滤 + 遍历）    │
│  - evaluateRule()              单规则执行（分派到条件/动作）     │
│  - evaluateCondition()         条件求值                        │
│  - evaluateAction()            动作求值                        │
│  - evaluateFormula()           公式计算                        │
│  - compareValues()             值比较（保留现有逻辑）            │
│  - evaluateCC()                CC 规则执行（从 composable 移入）│
│  - evaluateFORMATS()           FORMATS 规则执行               │
│  - buildRuleMessage()          统一错误消息构建（基于 ParsedRule）│
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│              模块 ⑤ composable/useLogicRules.ts              │
│  集成层：Vue 上下文 + 值映射 + 错误上报（1132行 → ~300行）     │
│  - buildValueMap()            统一值映射（合并 3 个 build*）   │
│  - validateFORMATS()           FORMATS 入口（合并 3 处逻辑）   │
│  - validateCC()               CC 规则入口（调用 engine）      │
│  - validateLogicRules()        提交校验（精简）                 │
│  - validateLogicRuleForBlur()  失焦校验（精简）                │
│  - validateFilledLogicRules()  已填数据校验（精简）             │
│  - validateSingleContainerLogicRule() 容器单行校验（精简）      │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│           上期值验证：usePositiveRules.ts（独立优化）           │
│  - makePositiveErrorKey()       统一错误 key 生成             │
│  - compareByType()              统一比较框架                  │
│  保持独立，重构点：                                          │
│  - 提取公共字段：parseRuleConfig()                          │
│  - 错误 key 生成：统一到 useErrorKeys                        │
│  - 数字/单选/多选/容器四函数提取公共比较框架                   │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    基础校验：useValidation.ts                 │
│  保持独立（已相对清晰），重构点：                               │
│  - validateAll 拆分：拆成 validateTopLevel / validateContainer│
│  - 减少重复的容器/顶层分支                                    │
└─────────────────────────────────────────────────────────────┘
```

---

## 三、验证流程（重构后）

### 3.1 实时校验流程（blur / change）

```
用户操作 (blur / change)
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│ Layer 1: 基础校验 (useValidation.ts)                        │
│ - 必填 required                                             │
│ - 格式 format (数字/文本/日期)                                │
│ - 范围 range (minValue/maxValue)                           │
│ - 精度 precision                                           │
│ 写 fieldErrors[type='required/format/range']               │
└─────────────────────────────────────────────────────────────┘
    │ (有错误 → fail-fast，返回)
    ▼
┌─────────────────────────────────────────────────────────────┐
│ Layer 2: 逻辑规则校验 (useLogicRules.ts → engine.ts)         │
│                                                            │
│ 2a. 解析：engine.parseLogicRule(ruleString)                 │
│     → ParsedRule[] (IF / SIMPLE / CC / FORMATS)            │
│                                                            │
│ 2b. 执行：engine.evaluateRule(rule, values)                 │
│     IF:  evaluateCondition() → evaluateAction()              │
│     SIMPLE: evaluateFormula() op evaluateFormula()            │
│     CC:  evaluateCC()                                       │
│     FORMATS: evaluateFORMATS()                               │
│                                                            │
│ 2c. 消息：engine.buildRuleMessage(rule, values)             │
│                                                            │
│ 写 fieldErrors[type='logic']                               │
└─────────────────────────────────────────────────────────────┘
    │ (有错误 → fail-fast，返回)
    ▼
┌─────────────────────────────────────────────────────────────┐
│ Layer 3: 上期值校验 (usePositiveRules.ts)                   │
│ - 数字: current >=/<= last                                 │
│ - 单选: 等级对比                                            │
│ - 多选: 选项对比                                            │
│ - 容器: 逐 entry 对比                                       │
│ 写 fieldErrors[type='joint']                              │
└─────────────────────────────────────────────────────────────┘
```

### 3.2 提交校验流程

```
doValidateAll()
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│ Layer 1: 基础校验 validateAll()                              │
│     → allErrors[]                                          │
└─────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│ Layer 2: 逻辑规则校验 validateLogicRules()                   │
│     遍历所有指标 → 解析 → engine.evaluateRule                │
│     → logicErrors[]                                        │
└─────────────────────────────────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────────────────────────────────┐
│ Layer 3: 上期值校验 validateAllPositiveRules()               │
│     → positiveErrors[]                                     │
└─────────────────────────────────────────────────────────────┘
    │
    ▼
返回 allErrors (包含所有三级错误)
```

---

## 四、详细改造实现

### 4.1 模块①：`types.ts`（新建）

**文件路径：** `IndicatorInputTable/utils/indicator-validator/types.ts`

**内容：**

```typescript
// ========== 引擎核心类型 ==========

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
  operator: ComparisonOperator | SpecialOperator;
  value?: number;
  values?: number[];     // IN / NOT_IN 对应的值列表
}

/** 动作配置 */
export interface RuleAction {
  type: 'formula' | 'condition';
  operator: ComparisonOperator | SpecialOperator;
  formula?: FormulaItem[];
  compareType?: CompareSource;
  compareValue?: number;
  compareFormula?: FormulaItem[];
  compareValues?: number[];   // IN / NOT_IN 列表
  // 指标引用
  indicatorCode?: string;
  indicatorId?: number;
  compareIndicatorCode?: string;
  compareIndicatorId?: number;
  message?: string;
}

/** 单条规则配置 */
export interface RuleConfigItem {
  id: number;
  name: string;
  condition?: RuleCondition;
  action: RuleAction;
}

/** 规则配置（解析后） */
export interface RuleConfig {
  groupName?: string;
  priority?: number;
  rules: RuleConfigItem[];
}

// ========== 规则类型枚举 ==========

export type RuleType = 'IF' | 'SIMPLE' | 'CC' | 'FORMATS';

// ========== 操作符枚举（所有跨规则类型复用的操作符）==========

/** 条件/动作比较操作符 */
export enum ComparisonOperator {
  EQ = '==',
  NE = '!=',
  GT = '>',
  GTE = '>=',
  LT = '<',
  LTE = '<=',
  IN = 'IN',
  NOT_IN = 'NOT_IN',
}

/** 特殊条件操作符（非比较语义）*/
export enum SpecialOperator {
  NOT_EMPTY = 'not_empty',  // 非空条件
  FORMULA = '__formula__',  // 公式表达式（引擎内部标记）
}

/** 操作符并集（用于类型守卫）*/
export type AnyOperator = ComparisonOperator | SpecialOperator;

/** 操作符 → 友好文字映射 */
export const OPERATOR_TEXT: Record<ComparisonOperator | SpecialOperator, string> = {
  [ComparisonOperator.GTE]: '应大于等于',
  [ComparisonOperator.LTE]: '应小于等于',
  [ComparisonOperator.GT]:  '应大于',
  [ComparisonOperator.LT]:  '应小于',
  [ComparisonOperator.EQ]:  '应等于',
  [ComparisonOperator.NE]:   '不应等于',
  [ComparisonOperator.IN]:   '必须选择',
  [ComparisonOperator.NOT_IN]: '不能选择',
  [SpecialOperator.NOT_EMPTY]: '必须有值',
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

// ========== 比较源类型枚举 ==========

/** 动作右侧值的来源类型 */
export enum CompareSource {
  /** 固定常量值（如 `> 0`）*/
  FIXED = 'fixed',
  /** 另一个指标引用（如 `>= [901]`）*/
  INDICATOR = 'indicator',
  /** 公式表达式（如 `>= [90201] + [90202]`）*/
  FORMULA = 'formula',
  /** 多值列表（IN/NOT_IN）*/
  LIST = 'list',
}

/** 解析后的规则（引擎内部使用） */
export interface ParsedRule {
  type: RuleType;
  raw: string;
  ruleConfig: RuleConfig;
  // 便捷字段（由 parser 预填充，用于 engine 快速访问）
  conditionIndicator?: string;
  conditionOperator?: ComparisonOperator | SpecialOperator;
  conditionValue?: number;
  conditionValues?: number[];
  actions: ParsedAction[];
  // 错误消息用
  ruleName: string;
  verifyIndicatorCode?: string;
}

/** 解析后的动作 */
export interface ParsedAction {
  operator: ComparisonOperator | SpecialOperator;
  // 当前值
  currentFormula?: FormulaItem[];
  currentIndicatorCode?: string;
  // 比较值
  compareType?: CompareSource;
  compareValue?: number;
  compareFormula?: FormulaItem[];
  compareIndicatorCode?: string;
  compareValues?: number[];  // IN / NOT_IN 列表
}

/** CC 规则 */
export interface ParsedCCRule {
  type: 'CC';
  fieldCodes: string[];         // 展开后的完整 key
  originalFieldCodes: string[]; // 原始引用（用于错误消息）
  triggerOp: TriggerOperator;
  triggerVal: string;
  forbidOp: ForbidOperator;
  forbidVal: string;
}

/** FORMATS 规则 */
export interface ParsedFORMATSRule {
  type: 'FORMATS';
  typeGroups: Record<string, string[]>;
  requiredFormats: string[];
}

// ========== 验证结果类型 ==========

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
  fieldLabel: string;
  triggerFieldLabel: string;
  triggerValueLabel: string;
  forbidValueLabel: string;
  message: string;
}

// ========== 引擎选项 ==========

export interface EngineOptions {
  triggerTiming?: 'FILL' | 'PROCESS_SUBMIT';
  processNode?: string;
  changedIndicatorId?: number;
  idToCode?: Map<number, string>;
  allIndicators?: Record<string, any>[];
}

// ========== 原始规则类型（从后端获取） ==========

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

// ========== 值映射类型 ==========

export type IndicatorValuesMap = Record<string | number, any>;

// ========== 字段定义类型 ==========

export interface DynamicField {
  fieldCode: string;
  fieldLabel: string;
  fieldType: string;
  options?: Array<{ value: any; label: string }>;
}
```

---

### 4.2 模块②：`parser.ts`（新建）

**文件路径：** `IndicatorInputTable/utils/indicator-validator/parser.ts`

**导出函数：**


| 函数                       | 签名                                                         | 说明                         |
| ------------------------ | ---------------------------------------------------------- | -------------------------- |
| `normalize`              | `(raw: string) => string`                                  | 归一化：去空格、统一 TRUE            |
| `detectRuleType`         | `(raw: string) => RuleType`                                | 自动检测规则类型                   |
| `parseLogicRule`         | `(raw: string, entryNumber: number) => ParsedRule[]`       | 主入口                        |
| `parseFormulaSide`       | `(side: string, entryNumber: number) => FormulaItem[]`     | 解析公式一侧                     |
| `parseConditionOperator` | `(condStr: string) => { operator: string; values: number[] | null; threshold: number }` |
| `parseCC`                | `(raw: string, entryNumber: number) => ParsedCCRule        | null`                      |
| `parseFORMATS`           | `(raw: string) => ParsedFORMATSRule                        | null`                      |


**关键改造点：**

1. `parseLogicRule` 拆成两步：`normalize` → `detectRuleType` → 按类型分发
2. `detectRuleType` 通过正则自动识别：IF / CC / FORMATS / SIMPLE
3. 不再直接输出 `JointRule[]`（那是旧格式），改为输出 `ParsedRule[]`

---

### 4.3 模块③：`engine.ts`（新建）

**文件路径：** `IndicatorInputTable/utils/indicator-validator/engine.ts`

**导出函数：**


| 函数                  | 签名                                                                                                    | 说明         |
| ------------------- | ----------------------------------------------------------------------------------------------------- | ---------- |
| `validate`          | `(rules: ParsedRule[], values: IndicatorValuesMap, opts?: EngineOptions) => ValidationResult[]`       | 主入口        |
| `evaluateRule`      | `(rule: ParsedRule, values: IndicatorValuesMap, opts?: EngineOptions) => ValidationResult`            | 单规则执行      |
| `evaluateCondition` | `(cond: RuleCondition, values: IndicatorValuesMap, idToCode?: Map<number,string>) => boolean`         | 条件求值       |
| `evaluateAction`    | `(action: ParsedAction, values: IndicatorValuesMap, idToCode?: Map<number,string>) => boolean`        | 动作求值       |
| `evaluateFormula`   | `(formula: FormulaItem[], values: IndicatorValuesMap, idToCode?: Map<number,string>) => number        | undefined` |
| `compareValues`     | `(current: any, operator: string, target: any) => boolean`                                            | 值比较（保留）    |
| `evaluateCC`        | `(rule: ParsedCCRule, entrySnapshot: Record<string,any>, fieldDefs: DynamicField[]) => CCViolation[]` | CC 执行      |
| `evaluateFORMATS`   | `(rule: ParsedFORMATSRule, files: {name:string}[]) => string[]`                                       | FORMATS 执行 |
| `buildRuleMessage`  | `(rule: ParsedRule, values: IndicatorValuesMap, allIndicators?: any[]) => string`                     | 统一消息构建     |


**子模块：`compareEngine`（比较引擎——核心扩容点）**

```typescript
// ========== compareEngine ==========
// 职责：纯函数，所有操作符比较逻辑
// 特点：与规则类型无关，跨 IF/SIMPLE 复用
// 扩容：新增操作符只改这里

/** 比较两个数值（用于 >/>=/</<= 操作符） */
function compareNumeric(
  left: number,
  operator: ComparisonOperator,
  right: number,
): boolean {
  switch (operator) {
    case ComparisonOperator.GT:  return left > right;
    case ComparisonOperator.GTE: return left >= right;
    case ComparisonOperator.LT:  return left < right;
    case ComparisonOperator.LTE: return left <= right;
    case ComparisonOperator.EQ:  return left === right;
    case ComparisonOperator.NE:  return left !== right;
    default: return false;
  }
}

/** 比较两个指标值（从 values 中取） */
function compareIndicatorValues(
  leftCode: string,
  operator: ComparisonOperator,
  rightCode: string,
  values: IndicatorValuesMap,
): boolean {
  const left  = values[leftCode]  as number;
  const right = values[rightCode] as number;
  if (left === undefined || right === undefined) return false;
  return compareNumeric(Number(left), operator, Number(right));
}

/** 固定值比较 */
function compareFixedValue(
  left: number,
  operator: ComparisonOperator,
  fixedValue: number,
): boolean {
  return compareNumeric(left, operator, fixedValue);
}

/** 列表成员判断（用于 IN/NOT_IN 操作符） */
function compareListValue(
  left: number,
  operator: ComparisonOperator,
  list: number[],
): boolean {
  const inList = list.includes(left);
  return operator === ComparisonOperator.IN ? inList : !inList;
}

/** 主入口：resolveCompareSource —— 根据 CompareSource 类型分发 */
function resolveCompareSource(
  left: number,
  action: ParsedAction,
  operator: ComparisonOperator,
  values: IndicatorValuesMap,
): boolean {
  if (operator === ComparisonOperator.IN || operator === ComparisonOperator.NOT_IN) {
    return compareListValue(left, operator, action.compareValues ?? []);
  }
  if (action.compareFormula) {
    const computed = evaluateFormula(action.compareFormula, values);
    if (computed === undefined) return false;
    return compareNumeric(left, operator, computed);
  }
  if (action.compareIndicatorCode) {
    return compareIndicatorValues(
      action.currentIndicatorCode ?? '', operator,
      action.compareIndicatorCode, values,
    );
  }
  return compareFixedValue(left, operator, action.compareValue ?? 0);
}
```

**扩容示例：新增 `BETWEEN(a, b)` 操作符只需改 2 行**

```typescript
// 1. 在 ComparisonOperator 枚举加条目
enum ComparisonOperator {
  BETWEEN = 'BETWEEN', // ← 新增
}

// 2. 在 resolveCompareSource 加一个 case
case ComparisonOperator.BETWEEN:
  return action.compareValues?.length === 2
    && left >= action.compareValues[0]
    && left <= action.compareValues[1];
// → 无需改任何其他函数


**关键改造点：**

1. `**validateRule`（旧）→ `evaluateRule`（新）**：函数从 250行拆成：
  - `evaluateCondition`：条件求值（80行）
  - `evaluateAction`：动作求值（120行）
  - 主循环只负责分派和结果收集（30行）
2. `**buildLogicRuleMsgForSingleRule`（旧）→ `buildRuleMessage`（新）**：
  - 输入从 `ruleConfig string` 改为 `ParsedRule` 对象（直接访问结构化数据，不用正则）
  - 错误消息构建从 5 个正则分支改为 `rule.type` 分发
3. **CC 规则执行从 composable 移入**：
  - `evaluateCC` 接收 `ParsedCCRule` + `entrySnapshot` + `fieldDefs`
  - `validateContainerConstraint`（composable 内，106行）删除
4. `calculateFormula`（旧）→ `evaluateFormula`（新）：
  - 返回类型更精确：`number | undefined`
  - 增加类型守卫

#### 4.3.5 `buildRuleMessage` 错误消息构建（重构重点）

**当前问题：**

- `buildLogicRuleMsgForSingleRule`（validator.ts，162行）全靠5个正则分支硬编码
- `buildLogicRuleMsg`（useLogicRules.ts，~120行）与上面完全重复
- 规则格式变化时需要同时修改两处

**重构目标：**

- 输入从 `ruleConfig string` 改为 `ParsedRule` 结构化对象
- 按 `rule.type` 分发，不再依赖正则
- 合并两个重复实现

**重构前后的消息格式对比：**


| 规则类型         | 当前消息格式                                                                                            | 重构后消息格式                                      |
| ------------ | ------------------------------------------------------------------------------------------------- | -------------------------------------------- |
| IF-IN/NOT_IN | `当 ${condLabel} 选择 ${condLabelStr} 时，${verifyLabel} ${verifyOpText} ${verifyValsList}`            | `当 [指标名] 选择 [选中值] 时，[指标名] 必须选择/不能选择 [可选项列表]` |
| IF-==/!=     | `当 ${condLabel} ${condOpText} ${condValLabel} 时，${verifyLabel} ${verifyOpText} ${verifyValLabel}` | `当 [指标名] 等于/不等于 [值] 时，[指标名] 应等于/不应等于 [值]`    |
| IF-基本比较      | `当 ${condLabel} ${condOpText} ${condVal} 时，${verifyLabel} ${verifyOpText} ${verifyVal}`           | `当 [指标名] 大于等于 [值] 时，[指标名] 应大于等于 [值]`         |
| 普通-单指标       | `${leftLabel} ${opText} ${rightLabel}`                                                            | `[指标名] 应大于等于 [指标名]`                          |
| 普通-公式        | `${leftLabel}(${leftValText}) ${opText} ${rightFriendlyText}`                                     | `[指标名](值) 应大于等于 [指标名](值) + [指标名](值) = 总和`    |
| 普通-固定值       | `${label} ${opText} ${fixedVal}`                                                                  | `[指标名] 应大于等于 100`                            |
| CC           | `「${triggerFieldLabel}」选择了「${triggerValueLabel}」，「${forbidFieldLabel}」必须选择「${triggerValueLabel}」` | 同左（已足够清晰，保持不变）                               |
| FORMATS      | `${indicator.indicatorName}：必须包含以下格式的文件：${missing.join('、')}`                                     | 同左（已足够清晰，保持不变）                               |


`**buildRuleMessage` 实现框架：**

```typescript
interface MessageContext {
  values: IndicatorValuesMap;
  allIndicators?: Record<string, any>[];
  fieldDefs?: DynamicField[];
  entryNumber?: number;
}

function buildRuleMessage(rule: ParsedRule, ctx: MessageContext): string {
  switch (rule.type) {
    case 'IF':
      return buildIFMessage(rule, ctx);
    case 'SIMPLE':
      return buildSimpleMessage(rule, ctx);
    case 'CC':
      return buildCCMessage(rule, ctx);
    case 'FORMATS':
      return buildFORMATSMessage(rule, ctx);
    default:
      return rule.ruleName || '校验失败';
  }
}

function buildIFMessage(rule: ParsedRule, ctx: MessageContext): string {
  const { conditionOperator, conditionValues } = rule;
  const isListOp = conditionOperator === ComparisonOperator.IN
                || conditionOperator === ComparisonOperator.NOT_IN;
  return isListOp
    ? buildIF_ListMessage(rule, ctx)  // IF-THEN with IN/NOT_IN
    : buildIF_CompareMessage(rule, ctx); // IF-THEN with ==/!=/>/>=/</<=
}

function buildIF_ListMessage(rule: ParsedRule, ctx: MessageContext): string {
  // 从 rule.conditionValues 获取条件值列表
  // 从 rule.actions[0] 获取验证操作符和值列表
  // 查找指标 label 和选项 label
  // 返回：`当 [指标名] 选择 [选中值] 时，[指标名] 必须选择/不能选择 [可选项列表]`
}

function buildIF_CompareMessage(rule: ParsedRule, ctx: MessageContext): string {
  // 条件值可能是固定值，也可能是列表（==/!= 时）
  // 验证值一定是固定值
  // 返回：`当 [指标名] [等于/大于/小于...] [值] 时，[指标名] [应等于/应大于...] [值]`
}

function buildSimpleMessage(rule: ParsedRule, ctx: MessageContext): string {
  const action = rule.actions[0];
  const opText = getOpText(action.operator); // >= → 应大于等于

  // 判断右侧类型
  if (action.compareFormula && action.compareFormula.length > 1) {
    return buildSimpleFormulaMessage(rule, ctx); // 带公式的右侧
  } else if (action.compareIndicatorCode) {
    return buildSimpleIndicatorMessage(rule, ctx); // 单指标右侧
  } else {
    return buildSimpleFixedMessage(rule, ctx); // 固定值右侧
  }
}

function buildSimpleFormulaMessage(rule: ParsedRule, ctx: MessageContext): string {
  // 提取左侧指标 label
  // 提取右侧公式中的所有指标 label 和当前值
  // 计算公式当前总和
  // 返回：`[指标名](值) [应大于等于] [指标名](值) + [指标名](值) = 总和`
}

function buildCCMessage(rule: ParsedCCRule, ctx: MessageContext): string {
  // 已在 validateContainerConstraint 中实现，直接迁移
  return `「${triggerFieldLabel}」选择了「${triggerValueLabel}」，「${forbidFieldLabel}」必须选择「${triggerValueLabel}」`;
}
```

`**buildRuleMessage` 辅助函数：**

```typescript
// 从 types.ts 导入：OPERATOR_TEXT, COND_OPERATOR_TEXT, ComparisonOperator
import { OPERATOR_TEXT, COND_OPERATOR_TEXT, ComparisonOperator } from './types';

// 操作符文本映射改为引用枚举版本（OPERATOR_TEXT 在 types.ts 中定义）
const COND_OPERATOR_TEXT: Record<string, string> = {
  '==':  '等于',
  '!=':  '不等于',
  '>':   '大于',
  '>=':  '大于等于',
  '<':   '小于',
  '<=':  '小于等于',
  'IN':        '在列表中',
  'NOT_IN':    '不在列表中',
};

/** 查找指标的 label（指标名或字段名） */
function findIndicatorLabel(code: string, ctx: MessageContext): string {
  // 1. 先从 fieldDefs 查找（容器场景）
  if (ctx.fieldDefs) {
    const fieldCode = code.slice(-2);
    const field = ctx.fieldDefs.find(f => f.fieldCode === fieldCode);
    if (field) return field.fieldLabel;
  }
  // 2. 从 allIndicators 查找（顶层场景）
  if (ctx.allIndicators) {
    const ind = ctx.allIndicators.find(i => i.indicatorCode === code);
    if (ind?.indicatorName) {
      return `${code} - ${ind.indicatorName}`;
    }
  }
  return code;
}

/** 查找选项的 label */
function findOptionLabel(
  code: string,
  value: string | number,
  ctx: MessageContext
): string {
  // 1. 先从 fieldDefs 查找（容器场景）
  if (ctx.fieldDefs) {
    const fieldCode = code.slice(-2);
    const field = ctx.fieldDefs.find(f => f.fieldCode === fieldCode);
    if (field?.options) {
      const opt = field.options.find(o =>
        String(o.value) === String(value) || o.value === value
      );
      if (opt) return opt.label;
    }
  }
  // 2. 从 allIndicators 查找（顶层场景）
  if (ctx.allIndicators) {
    const ind = ctx.allIndicators.find(i => i.indicatorCode === code);
    if (ind?.valueOptions) {
      try {
        const opts = JSON.parse(ind.valueOptions);
        if (Array.isArray(opts) && opts[0]?.label !== undefined) {
          const opt = opts.find(o =>
            String(o.value) === String(value) || o.value === value
          );
          if (opt) return opt.label;
        }
      } catch { /* ignore */ }
    }
  }
  return String(value);
}

/** 获取当前值（用于公式右侧展示） */
function getCurrentValue(code: string, ctx: MessageContext): string {
  const val = ctx.values[code];
  if (val === undefined || val === null || val === '') return '未填';
  if (Array.isArray(val)) return val.join('、');
  return String(val);
}
```

**合并点：**

- `validator.ts` 中的 `buildLogicRuleMsgForSingleRule` → 删除，改为调用 `engine.buildRuleMessage`
- `useLogicRules.ts` 中的 `buildLogicRuleMsg` → 删除，改为调用 `engine.buildRuleMessage`
- 所有调用点统一使用：`const msg = engine.buildRuleMessage(parsedRule, { values, allIndicators, fieldDefs });`

---

### 4.4 模块④：内部工具函数（合并到 engine.ts）

**工具函数位置：** `engine.ts` 内部或 `utils/` 目录

`**utils.ts` 中的辅助函数（从 validator.ts 迁移）：**

```typescript
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
  if (Array.isArray(value)) {
    return value.length > 0 ? Number(value[0]) : undefined;
  }
  return value === undefined || value === null ? undefined : Number(value);
}
```

---

### 4.5 模块⑤：`useLogicRules.ts`（重构，精简）

**文件路径：** `composables/useLogicRules.ts`

**目标：1132行 → ~300行**

**精简策略：**

1. **合并值映射构建**：3个 `build`* 函数 → 1个 `buildValueMap()`

```typescript
// 重构前（1132行）：
buildEntryValueMap()                  // 54行
buildEntryValueMapForConditional()     // 17行
buildCodeValueMap()                   // 9行
// 总计：80行，3个函数

// 重构后（~300行）：
function buildValueMap(
  indicator: DeclareIndicatorApi.Indicator,
  entryNumber?: number
): Record<string, any> {
  // 统一入口，内部根据容器类型分发
  // - conditional 容器：buildConditionalValueMap
  // - 顶层指标：buildTopLevelValueMap
  // - 条目容器：buildEntryValueMap
}
```

1. **提取公共 FORMATS 验证**：

```typescript
// 重构前：3个函数各有 30+ 行 FORMATS 逻辑
// 重构后：1个内部函数
function validateFORMATS(
  indicator: DeclareIndicatorApi.Indicator,
  setFieldError, clearFieldError
): ValidationError | null {
  const rule = parseFORMATS(indicator.logicRule);
  if (!rule) return null;
  // engine.evaluateFORMATS() → 返回 missing[]
  // 封装错误消息构建
}
```

1. **CC 规则调用 engine**：

```typescript
// 重构前：validateContainerConstraint 106行（含 matchesTrigger/formatFieldValue）
// 重构后：
function validateCC(
  indicator: DeclareIndicatorApi.Indicator,
  entry: any,
  setFieldError, clearFieldError
): CCViolation[] {
  const ccRule = parseCC(indicator.logicRule, entryNumber);
  if (!ccRule) return [];
  const fields = parseDynamicFields(indicator.valueOptions);
  return engine.evaluateCC(ccRule, entry, fields);
}
```

1. **校验函数精简**：


| 函数                                 | 重构前行数 | 重构后行数 | 精简方式                              |
| ---------------------------------- | ----- | ----- | --------------------------------- |
| `validateLogicRules`               | 173行  | ~60行  | 提取 `validateFORMATS`、`validateCC` |
| `validateLogicRuleForBlur`         | 224行  | ~80行  | 提取公共值映射、FORMATS、CC                |
| `validateFilledLogicRules`         | 159行  | ~60行  | 复用 `validateLogicRules` 结构        |
| `validateSingleContainerLogicRule` | 43行   | ~30行  | 调用 `engine.evaluateCC`            |


**重构后的 `validateLogicRules` 核心逻辑：**

```typescript
function validateLogicRules(
  allIndicators: DeclareIndicatorApi.Indicator[],
  setFieldError, clearFieldError,
): ValidationError[] {
  const errors: ValidationError[] = [];
  const topLevelMap = buildTopLevelValueMap(allIndicators);
  const indicatorMap = buildIndicatorCodeMap(allIndicators);

  for (const indicator of allIndicators) {
    if (!indicator.logicRule?.trim()) continue;
    if (!isIndicatorVisible(indicator.indicatorCode)) continue;

    // 1. FORMATS 规则 → 提取为独立函数
    const formatsErr = validateFORMATS(indicator, setFieldError, clearFieldError);
    if (formatsErr) { errors.push(formatsErr); continue; }

    // 2. CC 规则 → 提取为独立函数
    const ccErrs = validateCC(indicator, setFieldError, clearFieldError);
    if (ccErrs.length > 0) { errors.push(...ccErrs); continue; }

    // 3. 普通/IF 规则 → 调用 engine
    const parsedRules = engine.parseLogicRule(indicator.logicRule, entryNumber);
    const results = engine.validate(parsedRules, valueMap, { allIndicators: indicatorMap });
    // 处理结果...
  }
  return errors;
}
```

---

### 4.6 `usePositiveRules.ts`（独立优化）

**目标：~500行 → ~300行**

**改造点：**

1. **统一错误 key 生成**：

```typescript
// 重构前：错误 key 分散在各函数中
// 重构后：
function makePositiveErrorKey(indicator: DeclareIndicatorApi.Indicator, fieldCode?: string): string {
  if (fieldCode) return toContainerKey(fieldCode);
  return `t:${indicator.id}`;
}
```

1. **提取公共比较框架**：

```typescript
// 重构前：validateNumberRule、validateRadioRule、validateMultiSelectRule 各自独立
// 重构后：
type ComparisonResult = { valid: boolean; message?: string };

function compareByType(
  valueType: number,
  current: any,
  last: any,
  rule: PositiveRuleConfig
): ComparisonResult {
  switch (valueType) {
    case 1: return compareNumbers(current, last, rule);
    case 5: return compareRadio(current, last, rule);
    case 8: return compareMultiSelect(current, last, rule);
    default: return { valid: true };
  }
}
```

---

### 4.7 `useValidation.ts`（独立优化）

**改造点：**

1. `**validateAll` 拆分**：拆成 `validateTopLevel` 和 `validateContainer`

```typescript
function validateAll(indicators: DeclareIndicatorApi.Indicator[]): ValidationError[] {
  const topLevel = indicators.filter(ind => ind.valueType !== 12);
  const containers = indicators.filter(ind => ind.valueType === 12);
  return [
    ...validateTopLevel(topLevel),
    ...validateContainer(containers),
  ];
}
```

---

### 4.8 兼容层：`indicatorValidator.ts`（保留，委托新模块）

```typescript
// indicatorValidator.ts（最终状态，仅做兼容导出）
export * from './indicator-validator/types';
export * from './indicator-validator/parser';
export * from './indicator-validator/engine';
export { validate, calculateFormula, validateSingleRule } from './indicator-validator/engine';
// 内部复用 parser/engine，不再重复实现
```

---

## 五、文件变更总览


| 操作  | 文件路径                                                      | 行数变化                     |
| --- | --------------------------------------------------------- | ------------------------ |
| 新建  | `IndicatorInputTable/utils/indicator-validator/types.ts`  | +220行                    |
| 新建  | `IndicatorInputTable/utils/indicator-validator/parser.ts` | +380行（从 validator.ts 迁移） |
| 新建  | `IndicatorInputTable/utils/indicator-validator/engine.ts` | +400行（从 validator.ts 迁移） |
| 移动  | `src/utils/indicatorValidator.ts` → `IndicatorInputTable/utils/indicatorValidator.ts` | 0行（迁移，无行数变化）    |
| 修改  | `IndicatorInputTable/composables/useLogicRules.ts`         | -830行（1132行 → ~300行）     |
| 修改  | `IndicatorInputTable/composables/usePositiveRules.ts`      | -200行                    |
| 修改  | `IndicatorInputTable/composables/useValidation.ts`         | -100行                    |
| 删除  | `src/utils/indicatorValidator.ts`                         | -1216行                  |


**总计：减少 ~1400 行重复/混乱代码**
（新建 +1000 行，框架层 -1216 行，composable 层 -1130 行，净减少 ~1400 行）

---

## 六、代码地图参考

### 6.1 调用链

```
index.vue
├── handleNumberBlur (311)         → validateLogicRuleForBlur
├── onIndicatorChange (375)       → validateLogicRuleForBlur
├── onContainerFieldChange (407)  → validateSingleContainerLogicRule
├── onContainerFieldChange (428)  → clearContainerLogicRuleErrors
├── onMounted (459)               → validateLogicRules
├── watch(projectType) (476)      → validateLogicRules
├── doValidateAll (645)          → validateLogicRules + validateAll (基础校验)
└── defineExpose (685)            → 暴露 validateLogicRuleForBlur 给父组件

useValidation.ts (690)            → validateLogicRules 作为参数传入
```

### 6.2 数据依赖

```
lastPeriodValues / lastPeriodRawValues (useIndicatorData.ts)
    ↓ 读取
usePositiveRules.ts → setFieldError(type='joint')
    ↓ 写入
fieldErrors (useErrorKeys.ts)
    ↓ 读取/写入
index.vue → 显示/清除错误

formValues / containerValues (用户输入)
    ↓ 读取
useValidation.ts / useLogicRules.ts / usePositiveRules.ts
    ↓ 调用
indicatorValidator.ts (兼容层) → indicator-validator/engine.ts (新)
    ↓ 输出
ValidationResult[] → setFieldError(type='logic/required/format/range')
```

---

## 七、任务清单

### Phase 1：迁移 + 基础设施


| 序号  | 任务                                                    | 文件                                                       | 说明                |
| --- | ----------------------------------------------------- | -------------------------------------------------------- | ----------------- |
| 1.1 | 新建目录 `IndicatorInputTable/utils/indicator-validator/` | -                                                        | 放置所有验证引擎模块        |
| 1.2 | 创建 `types.ts`                                         | `IndicatorInputTable/utils/indicator-validator/types.ts` | 包含所有验证相关类型定义      |
| 1.3 | 迁移 `indicatorValidator.ts`                            | `src/utils/indicatorValidator.ts` → `IndicatorInputTable/utils/indicatorValidator.ts` | 迁移到组件目录（纯移动，无代码改动） |
| 1.4 | 更新 `useLogicRules.ts` import 路径                     | `IndicatorInputTable/composables/useLogicRules.ts`      | 改为 `../utils/indicatorValidator` |
| 1.5 | 更新 `index.vue` import 路径                           | `IndicatorInputTable/index.vue`                          | 改为 `./utils/indicatorValidator` |
| 1.6 | 删除 `src/utils/indicatorValidator.ts`                   | `src/utils/indicatorValidator.ts`                        | 清理框架层旧文件              |


### Phase 2：Parser 层


| 序号  | 任务                      | 文件                                                        | 说明                        |
| --- | ----------------------- | --------------------------------------------------------- | ------------------------- |
| 2.1 | 创建 `parser.ts`          | `IndicatorInputTable/utils/indicator-validator/parser.ts` | 重写所有解析函数                  |
| 2.2 | 实现 `detectRuleType()`   | `IndicatorInputTable/utils/indicator-validator/parser.ts` | 自动检测 IF/SIMPLE/CC/FORMATS |
| 2.3 | 实现 `normalize()`        | `IndicatorInputTable/utils/indicator-validator/parser.ts` | 归一化函数                     |
| 2.4 | 重写 `parseLogicRule()`   | `IndicatorInputTable/utils/indicator-validator/parser.ts` | 按类型分发，不再输出 JointRule[]    |
| 2.5 | 重写 `parseFormulaSide()` | `IndicatorInputTable/utils/indicator-validator/parser.ts` | 支持容器简写展开                  |
| 2.6 | 重写 `parseCC()`          | `IndicatorInputTable/utils/indicator-validator/parser.ts` | 保持现有逻辑                    |
| 2.7 | 重写 `parseFORMATS()`     | `IndicatorInputTable/utils/indicator-validator/parser.ts` | 保持现有逻辑                    |
| 2.8 | 重构 `indicatorValidator.ts` 为兼容导出层 | `IndicatorInputTable/utils/indicatorValidator.ts` | 改为从 `indicator-validator/` 模块导入并重新导出，删除已迁移代码 |
| **2.9** | **集成操作符枚举到 parser** | `IndicatorInputTable/utils/indicator-validator/parser.ts` | `parseConditionOperator()` 返回 `ComparisonOperator`，不再返回裸字符串 |
| **2.10** | **更新 parser 输出类型引用枚举** | `IndicatorInputTable/utils/indicator-validator/parser.ts` | `RuleCondition.operator` / `RuleAction.operator` / `compareType` / `triggerOp` / `forbidOp` 全部使用枚举 |
| **2.11** | **集成 `OPERATOR_TEXT` 映射到 parser** | `IndicatorInputTable/utils/indicator-validator/parser.ts` | 操作符 → 友好文字映射表在 types.ts 定义，parser 仅引用不重复定义 |


### Phase 3：Engine 层


| 序号   | 任务                                                               | 文件                                                        | 说明                          |
| ---- | ---------------------------------------------------------------- | --------------------------------------------------------- | --------------------------- |
| 3.1  | 创建 `engine.ts`                                                   | `IndicatorInputTable/utils/indicator-validator/engine.ts` | 核心验证引擎                      |
| 3.2  | 实现 `evaluateCondition()`                                         | `IndicatorInputTable/utils/indicator-validator/engine.ts` | 条件求值（从 validateRule 提取）     |
| 3.3  | 实现 `evaluateAction()`                                            | `IndicatorInputTable/utils/indicator-validator/engine.ts` | 动作求值（从 validateRule 提取）     |
| 3.4  | 实现 `evaluateFormula()`                                           | `IndicatorInputTable/utils/indicator-validator/engine.ts` | 重构 calculateFormula         |
| 3.5  | 实现 `compareValues()`                                             | `IndicatorInputTable/utils/indicator-validator/engine.ts` | 保留现有逻辑                      |
| 3.6  | 实现 `evaluateCC()`                                                | `IndicatorInputTable/utils/indicator-validator/engine.ts` | 从 composable 移入 CC 执行逻辑     |
| 3.7  | 实现 `evaluateFORMATS()`                                           | `IndicatorInputTable/utils/indicator-validator/engine.ts` | FORMATS 验证执行                |
| 3.8  | 定义 `MessageContext` 接口                                           | `IndicatorInputTable/utils/indicator-validator/engine.ts` | 错误消息构建上下文类型                 |
| 3.9  | 实现 `buildRuleMessage()` 主函数                                      | `IndicatorInputTable/utils/indicator-validator/engine.ts` | 按 rule.type 分发              |
| 3.10 | 实现 `buildIFMessage()`                                            | `IndicatorInputTable/utils/indicator-validator/engine.ts` | IF 规则消息（区分列表/比较操作符）         |
| 3.11 | 实现 `buildIF_ListMessage()`                                       | `IndicatorInputTable/utils/indicator-validator/engine.ts` | IF-IN/NOT_IN 消息             |
| 3.12 | 实现 `buildIF_CompareMessage()`                                    | `IndicatorInputTable/utils/indicator-validator/engine.ts` | IF-比较操作符消息                  |
| 3.13 | 实现 `buildSimpleMessage()`                                        | `IndicatorInputTable/utils/indicator-validator/engine.ts` | 普通规则消息（区分公式/指标/固定值）         |
| 3.14 | 实现 `buildSimpleFormulaMessage()`                                 | `IndicatorInputTable/utils/indicator-validator/engine.ts` | 普通规则-公式消息（含当前值汇总）           |
| 3.15 | 实现 `buildSimpleIndicatorMessage()`                               | `IndicatorInputTable/utils/indicator-validator/engine.ts` | 普通规则-单指标消息                  |
| 3.16 | 实现 `buildSimpleFixedMessage()`                                   | `IndicatorInputTable/utils/indicator-validator/engine.ts` | 普通规则-固定值消息                  |
| 3.17 | 实现 `buildCCMessage()`                                            | `IndicatorInputTable/utils/indicator-validator/engine.ts` | CC 规则消息                     |
| 3.18 | 实现辅助函数（`findIndicatorLabel`/`findOptionLabel`/`getCurrentValue`） | `IndicatorInputTable/utils/indicator-validator/engine.ts` | 标签查找和值获取工具                  |
| 3.19 | 实现 `validate()`                                                  | `IndicatorInputTable/utils/indicator-validator/engine.ts` | 主入口：过滤 + 遍历                 |
| 3.20 | 迁移 `buildLogicRuleMsgForSingleRule` 到 engine              | `IndicatorInputTable/utils/indicatorValidator.ts` → `IndicatorInputTable/utils/indicator-validator/engine.ts` | 合并到 engine.buildRuleMessage |
| 3.21 | 迁移 `validateRule` 到 engine                                | `IndicatorInputTable/utils/indicatorValidator.ts` → `IndicatorInputTable/utils/indicator-validator/engine.ts` | 合并到 engine.evaluateRule     |
| 3.22 | 迁移其他辅助函数（`compareValues`/`parseConditionOperator`/`parseFormulaSide` 等） | `IndicatorInputTable/utils/indicatorValidator.ts` → `IndicatorInputTable/utils/indicator-validator/engine.ts` | 合并到 engine                |
| **3.23** | **抽取 `compareEngine` 子模块** | `IndicatorInputTable/utils/indicator-validator/engine.ts` | 将 `compareValues` 中的 `compareType` 三态分发逻辑重构为 `compareNumeric`/`compareIndicatorValues`/`compareFixedValue`/`compareListValue`/`resolveCompareSource` 5 个纯函数 |
| **3.24** | **更新 `evaluateCondition` / `evaluateAction` 使用 compareEngine** | `IndicatorInputTable/utils/indicator-validator/engine.ts` | `evaluateCondition`/`evaluateAction` 调用 `resolveCompareSource`，不再直接处理 `compareType` 分支 |


### Phase 4：Composable 层


| 序号  | 任务                                      | 文件                                                 | 说明                       |
| --- | --------------------------------------- | -------------------------------------------------- | ------------------------ |
| 4.1 | 重写 `buildValueMap()`                    | `IndicatorInputTable/composables/useLogicRules.ts` | 合并 3 个 build* 函数         |
| 4.2 | 提取 `validateFORMATS()`                  | `IndicatorInputTable/composables/useLogicRules.ts` | 合并 3 处 FORMATS 逻辑        |
| 4.3 | 重写 `validateLogicRules()`               | `IndicatorInputTable/composables/useLogicRules.ts` | 调用 engine.validate       |
| 4.4 | 重写 `validateLogicRuleForBlur()`         | `IndicatorInputTable/composables/useLogicRules.ts` | 调用 engine.validate       |
| 4.5 | 重写 `validateFilledLogicRules()`         | `IndicatorInputTable/composables/useLogicRules.ts` | 复用 validateLogicRules 结构 |
| 4.6 | 重写 `validateSingleContainerLogicRule()` | `IndicatorInputTable/composables/useLogicRules.ts` | 调用 engine.evaluateCC     |
| 4.7 | 删除 `validateContainerConstraint()`      | `IndicatorInputTable/composables/useLogicRules.ts` | 已移入 engine               |
| 4.8 | 删除 `buildLogicRuleMsg()`                | `IndicatorInputTable/composables/useLogicRules.ts` | 已合并到 engine              |
| 4.9 | 清理注释和 agent log                         | `IndicatorInputTable/composables/useLogicRules.ts` | 删除所有 `#region agent log` |


### Phase 5：PositiveRules + Validation 优化


| 序号  | 任务                              | 文件                    | 说明                                    |
| --- | ------------------------------- | --------------------- | ------------------------------------- |
| 5.1 | 提取 `makePositiveErrorKey()`     | `usePositiveRules.ts` | 统一错误 key 生成                           |
| 5.2 | 提取 `compareByType()`            | `usePositiveRules.ts` | 统一比较框架                                |
| 5.3 | 重写 `validateAllPositiveRules()` | `usePositiveRules.ts` | 使用统一比较框架                              |
| 5.4 | 拆分 `validateAll()`              | `useValidation.ts`    | 拆成 validateTopLevel/validateContainer |


### Phase 6：测试与验证


| 序号       | 任务              | 说明                                                    |
| -------- | --------------- | ----------------------------------------------------- |
| 6.1      | IF 规则：基础        | `IF([703] > 0, [704] > 0, TRUE)`                      |
| 6.2      | IF 规则：IN/NOT_IN | `IF([801] IN(1,2,3), [802] NOT_IN(7), TRUE)`          |
| 6.3      | IF 规则：多动作       | `IF([602_03]==1, [602_04]==1, [602_05]==1, TRUE)`     |
| 6.4      | CC 规则           | `CC([602_03,602_04,602_05], FIRST_EQ(2), REST_NE(1))` |
| 6.5      | FORMATS 规则      | `FORMATS(["word","pdf"])`                             |
| 6.6      | 普通规则：公式         | `[802]>=[80201]+[80202]`                              |
| 6.7      | 普通规则：固定值        | `[901] >= 100`                                        |
| 6.8      | 3层验证流程          | 必填 → 逻辑 → 上期值 按序执行                                    |
| 6.9      | 提交校验            | doValidateAll 返回所有错误                                  |
| **6.10** | **错误消息格式对比**    | **对比新旧 `buildRuleMessage` 输出，确保格式一致**                 |


**任务 6.10 详细说明：**

- 收集线上真实规则样本（从 jointRules 中抽取各类规则格式）
- 对每种格式运行 `IndicatorInputTable/utils/indicatorValidator.ts` 中的 legacy `buildLogicRuleMsgForSingleRule` 和新的 `engine.buildRuleMessage`
- 对比输出是否一致，重点关注：
  - IF-IN/NOT_IN 格式：`当 [指标名] 选择 [值] 时，[指标名] 不能选择 [值]`
  - IF-基本比较格式：`当 [指标名] 等于 [值] 时，[指标名] 应等于 [值]`
  - 普通-公式格式：`[指标名](当前值) [应大于等于] [指标名](值) + [指标名](值) = 总和`
  - 普通-固定值格式：`[指标名] [应大于等于] 100`

---

## 八、风险评估与缓解


| 风险                          | 级别  | 缓解措施                                           |
| --------------------------- | --- | ---------------------------------------------- |
| Parser 重写破坏现有解析             | 高   | Phase 2 后跑完单元测试（任务 6.1-6.7）再继续                 |
| Engine 逻辑与旧 validateRule 差异 | 中   | 保留 `IndicatorInputTable/utils/indicatorValidator.ts` 中的 legacy 函数作为对比，直到 Phase 4 完成 |
| Composables 引用断裂            | 中   | 使用 TypeScript 严格模式，编译期发现断裂                     |
| 错误消息格式变化                    | 低   | 对比新旧 buildRuleMessage 输出，确保格式一致                |


---

## 九、现有调用点参考

### 9.1 `useLogicRules.ts` 导出函数被调用位置


| 函数                                 | 调用文件        | 行号            | 场景             |
| ---------------------------------- | ----------- | ------------- | -------------- |
| `validateLogicRules`               | `index.vue` | 459, 476, 645 | 加载完成、切换类型、提交校验 |
| `validateLogicRuleForBlur`         | `index.vue` | 311, 375, 685 | 失焦、变更、暴露给父组件   |
| `validateSingleContainerLogicRule` | `index.vue` | 407           | 容器字段变更         |
| `clearContainerLogicRuleErrors`    | `index.vue` | 428           | 容器字段变更后        |


### 9.2 `indicatorValidator.ts` 被调用位置


| 函数                                  | 调用文件                   | 说明            |
| ----------------------------------- | ---------------------- | ------------- |
| `validate` (别名 `validateJointRule`) | `useLogicRules.ts:432` | 执行联合规则验证      |
| `parseLogicRule`                    | `useLogicRules.ts` 多处  | 解析逻辑规则字符串     |
| `parseContainerFieldShortcut`       | `useLogicRules.ts`     | 解析容器字段简写      |
| `isContainerFieldShortcut`          | `useLogicRules.ts`     | 判断简写格式        |
| `buildLogicRuleMsgForSingleRule`    | `useLogicRules.ts` 多处  | 构建错误消息        |
| `parseFORMATS`                      | `useLogicRules.ts` 多处  | 解析 FORMATS 规则 |
| `parseCC`                           | `index.vue:415`        | 解析 CC 规则      |


