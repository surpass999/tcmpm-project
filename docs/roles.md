# IndicatorInputTable 指标校验体系

## 一、整体架构

指标校验体系分为 **3 层**，按顺序执行：

```
用户输入 → blur/change
   ↓
[STEP 1] 必填 + 格式校验   (validateIndicator)
   ↓
[STEP 2] 逻辑规则校验       (validateLogicRuleForBlur)
   ↓
[STEP 3] 上期值校验        (validatePositiveRuleForIndicator)
```

### 调用入口（`index.vue`）

```typescript
// handleNumberBlur 中 nextTick 内的 3 步顺序：
nextTick(() => {
  // STEP 1: 必填 + 格式
  validateIndicator(indicator);

  // STEP 2: 逻辑校验
  validateLogicRuleForBlur(indicator, indicators.value, setFieldError, clearFieldError, setDirty);

  // STEP 3: 上期值校验
  if (hasAnyLastPeriodValue()) {
    validatePositiveRuleForIndicator(indicator.indicatorCode, indicators.value);
  }
});
```

## 二、统一错误存储（`useErrorKeys.ts`）

所有类型的错误都存入同一个 reactive Map：`fieldErrors: Record<string, FieldError>`

### 2.1 错误 Key 体系

| Key 格式 | 用途 |
|---------|------|
| `t:${indicatorId}` | 顶层指标（如普通数字、文本、选项指标） |
| `c:${indicatorCode}:${rowKey}:${fieldCode}` | 容器内子字段（如自动录入表体字段） |
| `i:${indicatorCode}:${optionValue}` | 输入型选项（如选项 + 填写内容） |

### 2.2 错误类型（`errorType`）

每层校验写入时通过 `setFieldError` 的第三个参数**声明自己的类型**，共用一个 `fieldErrors` store。

| type | 含义 | 写入层 | 脏标记 |
|------|------|--------|--------|
| `required` | 必填为空 | STEP 1 | - |
| `format` | 格式错误 | STEP 1 | - |
| `range` | 范围/联动错误 | STEP 1 | - |
| `logic` | 逻辑规则错误 | STEP 2 | 不需要 |
| `joint` | 上期值校验错误 | STEP 3 | 不需要 |

**同一指标可以同时存在多种错误**，如 40202 可以同时报 `logic` 错和 `joint` 错，互不干扰。

### 2.3 按类型精确清除

```typescript
// clearFieldErrorIfType — 只清除指定类型，保留同指标其他类型的错误
export function clearFieldErrorIfType(key: string, targetType: FieldError['errorType']): void {
  if (fieldErrors[key]?.errorType === targetType) {
    delete fieldErrors[key];
  }
}
```

各层清除逻辑：
- STEP 1：`clearFieldError(key, true)` → 强制清除（可清 `required`）
- STEP 2：`clearFieldErrorIfType(key, 'logic')` → 只清 `logic` 类型
- STEP 3：`clearPositiveRuleError()` → 只清 `joint` 类型

### 2.4 脏标记（dirty）与显示策略

```typescript
// useErrorKeys.ts getFieldError()
export function getFieldError(key: string): string | undefined {
  const err = fieldErrors[key];
  if (!err) return undefined;
  // logic/joint 错误始终显示（无需脏追踪）
  // 其他错误需 dirty=true 才显示
  if (err.dirty || err.errorType === 'logic' || err.errorType === 'joint') {
    return err.message;
  }
  return undefined;
}
```

**`logic` 和 `joint` 类型的错误不需要脏标记**，填入瞬间立即显示；其他错误需用户操作过该字段后才显示。

## 三、STEP 1 — 必填 + 格式校验

**函数**：`validateIndicator(indicator)` — 来自 `useValidation.ts`

**校验内容**：
- 必填检查（`isRequired`）
- 格式检查（`inputType` 如日期、邮箱等）
- 数值范围（`minValue` / `maxValue`）

**错误写入**：`setFieldError(key, msg, 'required' | 'format' | 'range')`

**清除策略**：blur 时先清除旧错误（除非是 `required`），再重新写入。

## 四、STEP 2 — 逻辑规则校验

**文件**：`useLogicRules.ts`
**入口函数**：`validateLogicRuleForBlur(changedIndicator, allIndicators, setFieldError, clearFieldError, setDirty)`

### 4.1 提前退出条件

以下情况**跳过逻辑校验**（直接 return），让其他层处理：
1. 被修改字段本身为空 → 交给 `required` 校验
2. 指标是容器类型（`valueType === 12`）→ 容器校验由 `validateContainerFieldOnBlur` 处理
3. 指标是自动计算指标 → 无需逻辑校验

### 4.2 规则来源

来自 `indicator.logicRule` 字段（JSON 字符串），格式示例：

```json
{
  "operator": "&&",
  "conditions": [
    {
      "indicatorCode": "40101",
      "operator": ">=",
      "value": 0
    },
    {
      "indicatorCode": "40102",
      "operator": "notEmpty"
    }
  ]
}
```

### 4.3 引用字段空值处理（关键！）

当逻辑规则引用了其他字段（A → 校验 B 的规则），如果被引用字段为空，**不应该清除已有的逻辑错误**。

```typescript
// useLogicRules.ts
const results = evaluateLogicRules(changedCode, allIndicators, formValues, containerValues);

// ❌ 旧逻辑：results.length === 0 就清除
// ✅ 新逻辑：只有确认全部通过才清除
if (results.length === 0) {
  // 不清除！可能是因为引用字段为空而跳过，不代表验证通过
  return 'proceeded';
}
```

## 五、STEP 3 — 上期值校验（正向校验）

**文件**：`usePositiveRules.ts`
**入口函数**：`validatePositiveRuleForIndicator(changedIndicatorCode, indicators)`

### 5.1 数据源

上期值从 **`lastPeriodValues`**（display 值）读取，而非 `lastPeriodRawValues`（raw 值）。

- `lastPeriodValues['40202']` = `'20.0000'` → 用于页面显示和校验
- `lastPeriodRawValues['40202']` = `undefined` → 用于解析输入型选项的子内容

两者区别：
- `display`：格式化后的展示值（如 `'20.0000'`）
- `raw`：原始输入内容（如 `'20'`），用于选项 + 输入场景

### 5.2 规则来源

来自后端接口 `getEnabledJointRules({ projectType, triggerTiming: 'FILL' })`，每个规则的 `ruleConfig` 是 JSON 字符串，格式示例：

```json
{
  "groupName": "上期值对比",
  "rules": [
    {
      "name": "上期值校验",
      "indicatorCode": "40202",
      "valueType": 1,
      "compareMode": "positive",
      "compareType": "number"
    }
  ]
}
```

- `compareMode: 'positive'` → 正向校验，当前值不能小于上期值
- `compareType: 'number'` → 数值比较
- `valueType: 1` → 数字类型

### 5.3 校验流程

```typescript
// usePositiveRules.ts
export function validatePositiveRuleForIndicator(changedIndicatorCode, indicators) {
  // 1. 检查是否有上期值数据
  if (!hasAnyLastPeriodValue()) return null;

  // 2. 获取所有 FILL 时机的联合规则
  const fillRules = jointRules.value.filter((r) => r.triggerTiming === 'FILL');

  for (const rule of fillRules) {
    const config: PositiveRuleConfig = JSON.parse(rule.ruleConfig);

    for (const item of config.rules) {
      // 3. 只处理本次修改的指标
      if (item.indicatorCode !== changedIndicatorCode) continue;

      // 4. 如果该指标没有上期值，跳过（不由上期值规则处理）
      if (!hasLastPeriodValue(item.indicatorCode)) return null;

      // 5. 单规则校验
      const error = validateSinglePositiveRule(item, indicators);

      if (error) {
        // 写入错误，类型为 'joint'
        setFieldError(error.errorKey, `${error.ruleName}：${error.message}`, 'joint', false);
        return error;
      } else {
        // 验证通过，清除该指标的 joint 错误
        clearPositiveRuleError(item.indicatorCode, indicators);
      }
    }
  }
  return null;
}
```

### 5.4 数值比较逻辑（`validateNumberRule`）

```typescript
function validateNumberRule(item, currentValue, lastValue): string | null {
  const current = Number(currentValue);
  const last = Number(lastValue);

  // positive 模式：当前值不能小于上期值
  if (item.compareMode === 'positive') {
    if (current < last) {
      return `当前值 ${current} 不能小于上期值 ${last}`;
    }
  }

  return null;
}
```

## 六、分层验证设计思路

### 为什么要分层？

1. **职责分离**：每层只处理一种类型的校验，逻辑清晰
2. **优先级控制**：`required > format/range > logic > joint`
3. **错误不互相覆盖**：通过 `errorType` 隔离，clear 时只清除自己类型的错误

### 如何扩展新的校验类型？

1. 在 `types.ts` 的 `FieldError['errorType']` 联合类型中添加新类型
2. 在对应 composable 中调用 `setFieldError(key, msg, 'newType')`
3. 如需立即显示，在 `getFieldError` 的判断中添加 `|| err.errorType === 'newType'`
4. 如需阻止其他错误覆盖，在 `clearFieldErrorIfType` 中确保只清除对应类型

### 清除错误的三种方式

| 函数 | 行为 |
|------|------|
| `clearFieldError(key, force)` | 清除所有类型（`force=true` 才可清除 `required`） |
| `clearFieldErrorIfType(key, targetType)` | 只清除指定类型的错误 |
| `clearAllErrors()` | 清除所有错误 |

## 七、相关文件索引

| 文件 | 职责 |
|------|------|
| `index.vue` | 主入口，调度三层校验 |
| `useErrorKeys.ts` | 统一错误存储、Key 生成、读写函数 |
| `useValidation.ts` | 必填/格式/范围校验 |
| `useLogicRules.ts` | 逻辑规则校验 |
| `usePositiveRules.ts` | 上期值校验 |
| `useIndicatorData.ts` | 指标数据 + 上期值加载 |
| `useFormValues.ts` | 普通指标值存储 |
| `useContainerValues.ts` | 容器指标值存储 |
| `types.ts` | 所有类型定义 |
