---
name: ""
overview: ""
todos: []
isProject: false
---

# 自动条目容器实现计划

## 概述

实现第三种容器类型：**自动条目容器**。通过绑定一个数值类型指标，自动生成对应数量的条目，条目不可手动增删。

**约束**：不修改数据库表结构，所有配置信息存储在 `valueOptions` JSON 中。

> **数据迁移说明**：当前均为测试数据，直接采用统一对象格式，无需兼容旧数组格式。

---

## 一、数据结构设计

### 1.1 `valueOptions` JSON 结构（统一对象格式）

```typescript
// 普通动态容器
{
  "mode": "normal",
  "fields": [
    { "fieldCode": "name", "fieldLabel": "名称", "fieldType": "text" }
  ]
}

// 条件容器
{
  "mode": "conditional",
  "fields": [
    { "fieldCode": "name", "fieldLabel": "名称", "fieldType": "text", "showCondition": { "watchField": "type", "operator": "eq", "value": "1" } }
  ]
}

// 自动条目容器
{
  "mode": "autoEntry",
  "link": "project_count",
  "fields": [
    { "fieldCode": "db_name", "fieldLabel": "数据库名称", "fieldType": "text" },
    { "fieldCode": "db_type", "fieldLabel": "数据库类型", "fieldType": "select", "options": [...] }
  ]
}
```

### 1.2 容器类型判断逻辑


| 类型     | 判断条件                               | 说明           |
| ------ | ---------------------------------- | ------------ |
| 普通动态容器 | `mode === 'normal'`                | 用户手动增删条目     |
| 条件容器   | `mode === 'conditional'`           | 条目固定，字段按条件显示 |
| 自动条目容器 | `mode === 'autoEntry'` 且 `link` 存在 | 条目数量由关联指标控制  |


### 1.3 统一容器配置接口

```typescript
interface ContainerConfig {
  mode: 'normal' | 'conditional' | 'autoEntry';
  link?: string;       // 自动条目容器：关联的指标代码
  fields: DynamicField[];
}
```

---

## 二、前端类型定义

### 2.1 新增类型（IndicatorInputTable.vue 第 688-720 行附近）

```typescript
// 容器类型枚举
type ContainerType = 'normal' | 'conditional' | 'autoEntry';

// 容器配置（解析后的 valueOptions）
interface ContainerConfig {
  mode: ContainerType;
  link?: string;        // 自动条目容器：关联的指标代码
  fields: DynamicField[];
}

// DynamicField 已存在，无需修改
```

### 2.2 解析函数

```typescript
function parseContainerConfig(valueOptions: string): ContainerConfig {
  if (!valueOptions) return { mode: 'normal', fields: [] };

  try {
    const parsed = JSON.parse(valueOptions);

    // 数组格式（旧数据迁移，兼容）→ 默认普通容器
    if (Array.isArray(parsed)) {
      return { mode: 'normal', fields: parsed };
    }

    // 对象格式（新数据）
    return {
      mode: parsed.mode || 'normal',
      link: parsed.link,
      fields: parsed.fields || [],
    };
  } catch {
    return { mode: 'normal', fields: [] };
  }
}

function getContainerType(valueOptions: string): ContainerType {
  const config = parseContainerConfig(valueOptions);
  return config.mode;
}

function getAutoEntryLink(valueOptions: string): string | undefined {
  const config = parseContainerConfig(valueOptions);
  return config.link;
}

// 修改现有的 parseDynamicFields()
function parseDynamicFields(valueOptions: string): DynamicField[] {
  const config = parseContainerConfig(valueOptions);
  return config.fields;
}

// 修改现有的 isConditionalContainer()
function isConditionalContainer(valueOptions: string): boolean {
  return getContainerType(valueOptions) === 'conditional';
}
```

---

## 三、核心逻辑

### 3.1 条目数量同步函数

```typescript
function syncAutoEntryContainerCount(
  containerCode: string,
  linkedIndicatorCode: string
) {
  const linkedValue = formValues.value[linkedIndicatorCode];
  const targetCount = Math.max(0, Math.floor(Number(linkedValue)) || 0);

  if (!containerValues[containerCode]) {
    containerValues[containerCode] = [];
  }

  const currentEntries = containerValues[containerCode];

  if (currentEntries.length < targetCount) {
    // 不足：新增条目
    while (currentEntries.length < targetCount) {
      currentEntries.push({ rowKey: generateRowKey() });
    }
  } else if (currentEntries.length > targetCount) {
    // 超出：删除末尾条目
    const indicator = indicators.value.find(i => i.indicatorCode === containerCode);
    if (indicator) {
      cleanupEntryErrors(containerCode, currentEntries.length - 1, targetCount);
    }
    currentEntries.splice(targetCount);
  }
}

function cleanupEntryErrors(containerCode: string, fromIndex: number, toIndex: number) {
  const indicator = indicators.value.find(i => i.indicatorCode === containerCode);
  if (!indicator) return;

  const fields = parseDynamicFields(indicator.valueOptions);
  for (let i = toIndex; i < fromIndex; i++) {
    for (const field of fields) {
      const key = `${containerCode}:${i}:${field.fieldCode}`;
      delete containerFieldDirty[key];
      delete jointRuleErrors[key];
      delete logicRuleErrors[key];
    }
  }
}
```

### 3.2 关联指标监听

```typescript
// 在指标值变化时调用
function checkAndSyncLinkedAutoContainers(changedCode: string) {
  for (const indicator of indicators.value) {
    if (indicator.valueType !== 12) continue;

    const link = getAutoEntryLink(indicator.valueOptions);
    if (link === changedCode) {
      syncAutoEntryContainerCount(indicator.indicatorCode, changedCode);
    }
  }
}
```

### 3.3 初始化同步

```typescript
function initializeAutoEntryContainers() {
  for (const indicator of indicators.value) {
    if (indicator.valueType !== 12) continue;

    const link = getAutoEntryLink(indicator.valueOptions);
    if (link) {
      syncAutoEntryContainerCount(indicator.indicatorCode, link);
    }
  }
}
```

---

## 四、模板渲染

### 4.1 三种容器分支

```vue
<!-- 动态容器类型（普通 vs 条件 vs 自动条目） -->
<div v-else-if="indicator.valueType === 12">

  <!-- 普通动态容器：有 header，有添加/删除按钮 -->
  <div v-show="getContainerType(indicator.valueOptions) === 'normal'" class="dynamic-container-form">
    <!-- 复用现有模板（第 401-580 行） -->
  </div>

  <!-- 条件容器：无 header，无添加/删除按钮 -->
  <div v-show="getContainerType(indicator.valueOptions) === 'conditional'" class="conditional-container-form">
    <!-- 复用现有模板（第 274-400 行） -->
  </div>

  <!-- 自动条目容器：有 header，有来源说明，无添加/删除按钮 -->
  <div v-show="getContainerType(indicator.valueOptions) === 'autoEntry'" class="auto-entry-container-form">
    <div
      v-for="(entry, entryIndex) in getContainerEntries(indicator.indicatorCode)"
      :key="entry.rowKey"
      class="dynamic-entry-row mb-4"
    >
      <div class="entry-header flex items-center justify-between mb-2">
        <span class="entry-label text-gray-500 text-sm font-medium">
          条目 {{ entryIndex + 1 }}
        </span>
      </div>
      <!-- 字段渲染（复用现有代码） -->
    </div>

    <!-- 底部来源说明 -->
    <div class="auto-entry-source-hint text-gray-400 text-xs mt-2">
      （由「{{ getLinkedIndicatorName(indicator) }}」指标自动生成，数量不可调整）
    </div>
  </div>
</div>
```

### 4.2 UI 约束


| 特征            | 普通动态容器 | 条件容器 | 自动条目容器 |
| ------------- | ------ | ---- | ------ |
| Header 显示条目数量 | ✓      | ✗    | ✓      |
| 添加按钮          | ✓      | ✗    | ✗      |
| 删除按钮          | ✓      | ✗    | ✗      |
| 来源说明          | -      | -    | ✓      |
| 条目数量          | 用户控制   | 固定   | 关联指标控制 |


---

## 五、后台配置界面

### 5.1 配置流程

1. 选择指标类型为"动态容器"（`valueType=12`）
2. 选择容器类型：普通 / 条件 / 自动条目
3. 配置子字段（与现有方式相同）

### 5.2 容器类型配置

```vue
<a-form-item label="容器类型">
  <a-radio-group v-model="formData.mode">
    <a-radio value="normal">普通动态容器</a-radio>
    <a-radio value="conditional">条件容器</a-radio>
    <a-radio value="autoEntry">自动条目容器</a-radio>
  </a-radio-group>
</a-form-item>

<!-- 自动条目容器：关联指标下拉 -->
<a-form-item v-if="formData.mode === 'autoEntry'" label="关联指标">
  <a-select
    v-model="formData.link"
    placeholder="选择数值类型指标"
    :options="getAvailableLinkIndicators()"
    @change="validateAutoEntryLink"
  />
  <span class="config-hint">选择用于控制条目数量的指标（仅显示同项目类型的普通指标）</span>
</a-form-item>

<!-- 条件容器：showCondition 字段选择 -->
<a-form-item v-if="formData.mode === 'conditional'" label="条件字段">
  <!-- 从已配置的 fields 中选择 watchField -->
</a-form-item>
```

### 5.3 配置校验规则


| 容器类型   | 依赖对象       | 校验规则                                    |
| ------ | ---------- | --------------------------------------- |
| 普通动态容器 | 无          | 无                                       |
| 条件容器   | 同一容器内的其他字段 | `watchField` 必须是 `fields` 中的字段          |
| 自动条目容器 | 同项目类型的普通指标 | `link` 必须是 `valueType !== 12` 且同项目类型的指标 |


### 5.4 校验函数

```typescript
interface ValidationResult {
  valid: boolean;
  message?: string;
}

function validateAutoEntryLink(
  link: string,
  projectType: string,
  allIndicators: Indicator[]
): ValidationResult {
  // 1. 必填校验
  if (!link) {
    return { valid: false, message: '请选择关联指标' };
  }

  // 2. 查找被关联的指标
  const targetIndicator = allIndicators.find(i => i.indicatorCode === link);
  if (!targetIndicator) {
    return { valid: false, message: '关联指标不存在' };
  }

  // 3. 不能是容器指标
  if (targetIndicator.valueType === 12) {
    return { valid: false, message: '关联指标不能是容器指标' };
  }

  // 4. 必须同项目类型
  if (targetIndicator.projectType !== projectType) {
    return { valid: false, message: '关联指标必须与当前指标属于同一项目类型' };
  }

  return { valid: true };
}

function validateConditionalLink(
  watchField: string,
  fields: DynamicField[]
): ValidationResult {
  // watchField 必须是 fields 中的字段
  if (!fields.find(f => f.fieldCode === watchField)) {
    return { valid: false, message: '条件字段必须是容器内的字段' };
  }
  return { valid: true };
}
```

### 5.5 可选指标列表

```typescript
function getAvailableLinkIndicators(
  projectType: string,
  allIndicators: Indicator[],
  excludeContainerCodes: string[]
): SelectOption[] {
  return allIndicators
    .filter(i =>
      i.projectType === projectType &&
      i.valueType !== 12 &&
      !excludeContainerCodes.includes(i.indicatorCode)
    )
    .map(i => ({
      label: `${i.indicatorName} (${i.indicatorCode})`,
      value: i.indicatorCode,
    }));
}
```

---

## 六、搜索功能

### 6.1 子字段解析

```typescript
function parseContainerFields(valueOptions: string): DynamicField[] {
  const config = parseContainerConfig(valueOptions);
  return config.fields;
}
```

### 6.2 搜索流程（与普通动态容器相同）

1. 用户选择指标代码（如 `d001`）
2. 解析 `valueOptions` 获取子字段列表
3. 用户选择子字段（如 `db_name`）
4. 构建搜索条件
5. 后端执行 JSON 字段搜索

---

## 七、导出功能

### 7.1 导出列格式


| 指标代码   | 列名                                       |
| ------ | ---------------------------------------- |
| `d001` | `d001-数据库名称`、`d001-数据库类型`、`d001-数据量(GB)` |
| `d002` | `d002-项目名称`、`d002-预算金额`                  |


### 7.2 导出数据展开

每个条目一行展开，例如：


| 医院名称 | d001-数据库名称 | d001-数据库类型 |
| ---- | ---------- | ---------- |
| 医院A  | 医院A主库      | MySQL      |
| 医院A  | 医院A备库      | PostgreSQL |
| 医院B  | 医院B主库      | Oracle     |


> **注**：自动条目容器不需额外汇总列，条目数量由关联指标体现。

---

## 八、关键文件清单


| 文件                                | 修改内容                       |
| --------------------------------- | -------------------------- |
| `IndicatorInputTable.vue`         | 类型定义、解析函数、容器类型判断、条目同步、模板渲染 |
| `NationalAdvancedSearchModal.vue` | 子字段解析、搜索 UI 完善             |
| 导出相关组件                            | 导出列处理                      |
| 指标配置页面（后端页面）                      | 容器类型选择、关联指标下拉、校验逻辑         |
| 后端搜索服务                            | SQL 条件构建                   |
| 后端导出服务                            | 数据展开逻辑                     |


---

## 九、实施优先级


| 优先级 | 任务                                                 | 说明                                   |
| --- | -------------------------------------------------- | ------------------------------------ |
| P0  | 前端 `parseContainerConfig()` + `getContainerType()` | 统一解析入口                               |
| P0  | 前端类型定义                                             | `ContainerType`、`ContainerConfig`    |
| P0  | 模板渲染                                               | 三种容器分支                               |
| P0  | 核心同步逻辑                                             | `syncAutoEntryContainerCount()`      |
| P0  | 配置校验                                               | 关联指标类型、项目类型校验                        |
| P1  | 后台配置界面                                             | 容器类型选择、关联指标下拉                        |
| P1  | 关联指标监听                                             | `checkAndSyncLinkedAutoContainers()` |
| P1  | 搜索功能完善                                             | 子字段解析                                |
| P2  | 导出功能适配                                             | 列格式 + 数据展开                           |


---

## 十、约束汇总（基于最新讨论）


| 规则     | 说明                                      |
| ------ | --------------------------------------- |
| 同项目类型  | 关联指标必须与容器指标属于同一 `projectType`           |
| 自动条目容器 | 只能依赖普通指标（`valueType !== 12`）            |
| 条件容器   | 只能依赖同一容器内的字段（`watchField` 在 `fields` 中） |
| 普通容器   | 不涉及依赖关系                                 |
| 空值处理   | 关联指标未填写时不生成条目                           |
| 无汇总列   | 导出时不需要条目总数列                             |


> **2026-04-05 补充约束**：
>
> 1. **配置时验证**：关联指标必须在配置时校验，保证关联的指标与容器在同一表单内（同项目类型）
> 2. **配置时校验**：在配置保存时就完成校验，无需等到运行时
> 3. **自动容器依赖限制**：自动条目容器依赖的只能是普通指标，不能是容器指标
> 4. **条件容器依赖限制**：条件容器依赖的只能是容器内的指标，普通容器不涉及依赖
> 5. **关联指标未填写时不生成条目**：当被关联的数值指标没有填写值时，自动条目容器不生成任何条目（targetCount = 0）

---

## 十一、后台配置页面修改

### 11.1 修改文件

- `web-ui/apps/web-antd/src/views/declare/indicator/modules/form.vue`

### 11.2 修改内容

#### 11.2.1 新增响应式变量

```typescript
// 容器类型（mode）：normal / conditional / autoEntry
const containerMode = ref<'normal' | 'conditional' | 'autoEntry'>('normal');

// 自动条目容器：关联的指标代码
const autoEntryLink = ref<string>('');

// 所有指标列表（用于下拉选择，排除自身）
const allIndicatorsForLink = ref<Indicator[]>([]);
```

#### 11.2.2 获取可选的关联指标

```typescript
function getAvailableLinkIndicators(): { label: string; value: string }[] {
  if (!formData.value?.projectType) return [];

  return allIndicatorsForLink.value
    .filter(i =>
      i.projectType === formData.value?.projectType &&
      i.valueType !== 12 &&
      i.indicatorCode !== formData.value?.indicatorCode
    )
    .map(i => ({
      label: `${i.indicatorName} (${i.indicatorCode})`,
      value: i.indicatorCode,
    }));
}
```

#### 11.2.3 关联指标校验（配置保存时调用）

```typescript
function validateAutoEntryLink(): boolean {
  if (containerMode.value !== 'autoEntry') return true;
  if (!autoEntryLink.value) {
    message.error('自动条目容器必须选择关联指标');
    return false;
  }

  const target = allIndicatorsForLink.value.find(i => i.indicatorCode === autoEntryLink.value);
  if (!target) {
    message.error('关联指标不存在');
    return false;
  }
  if (target.valueType === 12) {
    message.error('关联指标不能是容器指标');
    return false;
  }
  if (target.projectType !== formData.value?.projectType) {
    message.error('关联指标必须与当前指标属于同一项目类型');
    return false;
  }
  return true;
}
```

#### 11.2.4 序列化 `valueOptions`（改为对象格式）

```typescript
const convertOptionsToJson = () => {
  // 动态容器类型：序列化为统一对象格式
  if (currentValueType.value === 12) {
    if (dynamicFields.length === 0) {
      formData.value!.valueOptions = '';
      return;
    }
    const validFields = dynamicFields
      .filter((field) => field.fieldCode && field.fieldLabel && field.fieldType)
      .map((field) => ({
        fieldCode: field.fieldCode,
        fieldLabel: field.fieldLabel,
        fieldType: field.fieldType,
        required: field.required ?? false,
        sort: field.sort ?? 0,
        options: field.options?.filter((opt) => opt.value && opt.label) ?? [],
        maxLength: field.maxLength != null ? Number(field.maxLength) : undefined,
        placeholder: field.placeholder,
        showSearch: field.showSearch,
        minSelect: field.minSelect != null ? Number(field.minSelect) : undefined,
        maxSelect: field.maxSelect != null ? Number(field.maxSelect) : undefined,
        format: field.format,
        layout: field.layout,
        rows: field.rows != null ? Number(field.rows) : undefined,
        precision: field.precision != null ? Number(field.precision) : undefined,
        prefix: field.prefix,
        suffix: field.suffix,
        showCondition: field.showCondition?.watchField
          ? Object.fromEntries(Object.entries({ ...field.showCondition }).filter(([, v]) => v !== undefined))
          : undefined,
        defaultValue: field.defaultValue,
      }));

    // 统一对象格式：包含 mode 和 fields
    const config: Record<string, any> = {
      mode: containerMode.value,
      fields: validFields,
    };
    // 自动条目容器：增加 link 字段
    if (containerMode.value === 'autoEntry' && autoEntryLink.value) {
      config.link = autoEntryLink.value;
    }
    formData.value!.valueOptions = JSON.stringify(config);
    return;
  }

  // 普通选项类型（保持现有逻辑）
  if (optionsList.length === 0) {
    formData.value!.valueOptions = '';
    return;
  }
  const validOptions = optionsList.filter((opt) => opt.value && opt.label);
  formData.value!.valueOptions = JSON.stringify(
    validOptions.map((opt) => ({ value: opt.value, label: opt.label, exclusive: opt.exclusive ?? false })),
  );
};
```

#### 11.2.5 反序列化 `valueOptions`（支持对象格式回显）

```typescript
const parseJsonToOptions = (jsonStr: string) => {
  optionsList.length = 0;
  dynamicFields.length = 0;
  containerMode.value = 'normal';
  autoEntryLink.value = '';

  if (!jsonStr) return;
  try {
    const parsed = JSON.parse(jsonStr);

    // 对象格式（新数据）
    if (!Array.isArray(parsed)) {
      containerMode.value = parsed.mode || 'normal';
      autoEntryLink.value = parsed.link || '';

      const fields = parsed.fields || [];
      fields.forEach((item: any) => {
        dynamicFields.push({
          key: `field_${Date.now()}_${Math.random()}`,
          fieldCode: item.fieldCode ?? '',
          fieldLabel: item.fieldLabel ?? '',
          fieldType: item.fieldType ?? 'text',
          required: item.required ?? false,
          sort: item.sort ?? 0,
          options: item.options ?? [],
          maxLength: item.maxLength,
          placeholder: item.placeholder,
          showSearch: item.showSearch,
          minSelect: item.minSelect,
          maxSelect: item.maxSelect,
          format: item.format,
          layout: item.layout,
          rows: item.rows,
          precision: item.precision,
          prefix: item.prefix,
          suffix: item.suffix,
          showCondition: item.showCondition,
          defaultValue: item.defaultValue,
        });
      });
      return;
    }

    // 数组格式（旧数据）→ 默认普通容器
    const firstItem = parsed[0];
    if (firstItem && 'fieldCode' in firstItem) {
      parsed.forEach((item: any) => {
        dynamicFields.push({
          key: `field_${Date.now()}_${Math.random()}`,
          fieldCode: item.fieldCode ?? '',
          fieldLabel: item.fieldLabel ?? '',
          fieldType: item.fieldType ?? 'text',
          required: item.required ?? false,
          sort: item.sort ?? 0,
          options: item.options ?? [],
          maxLength: item.maxLength,
          placeholder: item.placeholder,
          showSearch: item.showSearch,
          minSelect: item.minSelect,
          maxSelect: item.maxSelect,
          format: item.format,
          layout: item.layout,
          rows: item.rows,
          precision: item.precision,
          prefix: item.prefix,
          suffix: item.suffix,
          showCondition: item.showCondition,
          defaultValue: item.defaultValue,
        });
      });
    } else {
      parsed.forEach((item: any) => {
        optionsList.push({
          value: String(item.value),
          label: item.label,
          key: `option_${Date.now()}_${Math.random()}`,
          exclusive: item.exclusive === true,
        });
      });
    }
  } catch {
    // 解析失败，忽略
  }
};
```

#### 11.2.6 模板新增容器类型选择和关联指标下拉

在"值类型"为 12（动态容器）时，"子字段定义"上方增加：

```vue
<!-- 容器类型选择（仅动态容器显示） -->
<a-form-item v-if="isDynamicContainer" label="容器类型">
  <a-radio-group v-model="containerMode">
    <a-radio value="normal">普通动态容器</a-radio>
    <a-radio value="conditional">条件容器</a-radio>
    <a-radio value="autoEntry">自动条目容器</a-radio>
  </a-radio-group>
</a-form-item>

<!-- 关联指标下拉（仅自动条目容器显示） -->
<a-form-item v-if="isDynamicContainer && containerMode === 'autoEntry'" label="关联指标">
  <a-select
    v-model="autoEntryLink"
    placeholder="请选择控制条目数量的指标"
    :options="getAvailableLinkIndicators()"
    allow-clear
  />
  <div class="text-gray-400 text-xs mt-1">
    提示：仅显示同项目类型的普通指标（不含容器指标）
  </div>
</a-form-item>
```

#### 11.2.7 保存时增加校验

在 `onConfirm` 中，提交前调用校验逻辑：

```typescript
// 自动条目容器：关联指标校验
if (currentValueType.value === 12 && containerMode.value === 'autoEntry') {
  if (!autoEntryLink.value) {
    message.error('自动条目容器必须选择关联指标');
    return;
  }
  const target = allIndicatorsForLink.value.find(i => i.indicatorCode === autoEntryLink.value);
  if (!target) {
    message.error('关联指标不存在');
    return;
  }
  if (target.valueType === 12) {
    message.error('关联指标不能是容器指标');
    return;
  }
  if (target.projectType !== formData.value?.projectType) {
    message.error('关联指标必须与当前指标属于同一项目类型');
    return;
  }
}
```

#### 11.2.8 加载时获取所有指标列表

在 `onOpenChange` 编辑模式下获取指标列表，用于关联指标下拉：

```typescript
// 获取所有指标列表（用于关联指标下拉）
const { getIndicatorList } = await import('#/api/declare/indicator');
const list = await getIndicatorList({ pageNo: 1, pageSize: 9999 });
allIndicatorsForLink.value = list || [];
```

---

## 十二、前端 IndicatorInputTable.vue 修改

### 12.1 修改文件

- `web-ui/apps/web-antd/src/views/declare/progress-report/components/IndicatorInputTable.vue`

### 12.2 修改内容

#### 12.2.1 新增解析函数（替换现有 parseDynamicFields）

```typescript
// 容器配置（统一对象格式）
interface ContainerConfig {
  mode: 'normal' | 'conditional' | 'autoEntry';
  link?: string;
  fields: DynamicField[];
}

function parseContainerConfig(valueOptions: string): ContainerConfig {
  if (!valueOptions) return { mode: 'normal', fields: [] };
  try {
    const parsed = JSON.parse(valueOptions);
    if (Array.isArray(parsed)) {
      return { mode: 'normal', fields: parsed };
    }
    return {
      mode: parsed.mode || 'normal',
      link: parsed.link,
      fields: parsed.fields || [],
    };
  } catch {
    return { mode: 'normal', fields: [] };
  }
}

function getContainerType(valueOptions: string): 'normal' | 'conditional' | 'autoEntry' {
  return parseContainerConfig(valueOptions).mode;
}

function getAutoEntryLink(valueOptions: string): string | undefined {
  return parseContainerConfig(valueOptions).link;
}

function parseDynamicFields(valueOptions: string): DynamicField[] {
  return parseContainerConfig(valueOptions).fields;
}
```

#### 12.2.2 新增自动条目容器同步函数

```typescript
function syncAutoEntryContainerCount(
  containerCode: string,
  linkedIndicatorCode: string
) {
  const linkedValue = formValues.value[linkedIndicatorCode];
  // 关联指标未填写时：targetCount = 0，不生成条目
  const targetCount = Math.max(0, Math.floor(Number(linkedValue)) || 0);

  if (!containerValues[containerCode]) {
    containerValues[containerCode] = [];
  }

  const currentEntries = containerValues[containerCode];

  if (currentEntries.length < targetCount) {
    while (currentEntries.length < targetCount) {
      currentEntries.push({ rowKey: generateRowKey() });
    }
  } else if (currentEntries.length > targetCount) {
    cleanupEntryErrors(containerCode, currentEntries.length - 1, targetCount);
    currentEntries.splice(targetCount);
  }
}
```

#### 12.2.3 新增关联指标监听

```typescript
function checkAndSyncLinkedAutoContainers(changedCode: string) {
  for (const indicator of indicators.value) {
    if (indicator.valueType !== 12) continue;
    const link = getAutoEntryLink(indicator.valueOptions);
    if (link === changedCode) {
      syncAutoEntryContainerCount(indicator.indicatorCode, changedCode);
    }
  }
}
```

#### 12.2.4 初始化同步

```typescript
function initializeAutoEntryContainers() {
  for (const indicator of indicators.value) {
    if (indicator.valueType !== 12) continue;
    const link = getAutoEntryLink(indicator.valueOptions);
    if (link) {
      syncAutoEntryContainerCount(indicator.indicatorCode, link);
    }
  }
}
```

#### 12.2.5 模板修改：三种容器分支

将现有的 `v-else-if="indicator.valueType === 12"` 分支修改为三个子分支：

```vue
<!-- 动态容器类型（普通 vs 条件 vs 自动条目） -->
<div v-else-if="indicator.valueType === 12">

  <!-- 普通动态容器：有 header，有添加/删除按钮 -->
  <div v-show="getContainerType(indicator.valueOptions) === 'normal'" class="dynamic-container-form">
    <!-- 复用现有模板 -->
  </div>

  <!-- 条件容器：无 header，无添加/删除按钮 -->
  <div v-show="getContainerType(indicator.valueOptions) === 'conditional'" class="conditional-container-form">
    <!-- 复用现有模板 -->
  </div>

  <!-- 自动条目容器：有 header，有来源说明，无添加/删除按钮 -->
  <div v-show="getContainerType(indicator.valueOptions) === 'autoEntry'" class="auto-entry-container-form">
    <div
      v-for="(entry, entryIndex) in getContainerEntries(indicator.indicatorCode)"
      :key="entry.rowKey"
      class="dynamic-entry-row mb-4"
    >
      <div class="entry-header flex items-center justify-between mb-2">
        <span class="entry-label text-gray-500 text-sm font-medium">
          条目 {{ entryIndex + 1 }}
        </span>
      </div>
      <!-- 字段渲染（复用现有代码） -->
    </div>

    <!-- 底部来源说明 -->
    <div class="auto-entry-source-hint text-gray-400 text-xs mt-2">
      （由「{{ getLinkedIndicatorName(indicator) }}」指标自动生成，数量不可调整）
    </div>
  </div>
</div>
```

#### 12.2.6 指标值变化时触发关联容器同步

在 `handleIndicatorValueChange` 或类似的指标值变更处理函数中，调用 `checkAndSyncLinkedAutoContainers`。

#### 12.2.7 新增获取关联指标名称辅助函数

```typescript
function getLinkedIndicatorName(indicator: Indicator): string {
  const link = getAutoEntryLink(indicator.valueOptions);
  if (!link) return '';
  const linkedIndicator = indicators.value.find(i => i.indicatorCode === link);
  return linkedIndicator?.indicatorName || link;
}
```

