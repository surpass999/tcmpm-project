<script setup lang="ts">
/**
 * 容器内字段统一渲染器
 * 消除 conditional / autoEntry / normal 三种容器类型的重复代码
 */

import { computed } from 'vue';
import dayjs from 'dayjs';

import { SafeNumberInput } from '#/components/safe-number-input';
import type { DynamicField } from '../../types';

interface Props {
  entry: any;
  indicatorCode: string;
  rowKey: string;
  field: DynamicField;
  disabled?: boolean;
  containerFieldError?: string;
  /** 当前条目在列表中的索引（从0开始） */
  entryIndex?: number;
  /** 容器上期值列表（支持多条目） */
  containerLastValues?: ContainerEntryLastValue[];
}

export interface ContainerEntryLastValue {
  rowKey: string;
  fieldValues: Record<string, string>;
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
});

const emit = defineEmits<{
  'update': [value: any];
  'blur': [];
  'fieldChange': [];
}>();

/** 计算字段的完整 key */
const fullKey = computed(() => `${props.rowKey}${props.field.fieldCode}`);

/** 容器字段的显示标签 */
const fieldLabel = computed(() => `${fullKey.value} - ${props.field.fieldLabel}`);

/** 容器字段的 placeholder */
const fieldPlaceholder = computed(() => `请输入${fieldLabel.value}`);

/** 读取字段值 */
const fieldValue = computed(() => {
  return props.entry?.[fullKey.value];
});

/** 更新字段值 */
function updateValue(value: any) {
  props.entry[fullKey.value] = value;
  emit('update', value);
  emit('fieldChange');
}

/** 失焦处理 */
function handleBlur(e: FocusEvent) {
  const target = e.target as HTMLInputElement;
  const rawValue = target?.value ?? '';

  if (rawValue !== props.entry[fullKey.value]) {
    props.entry[fullKey.value] = rawValue;
  }

  emit('blur');
  emit('fieldChange');
}

/** 文本类型失焦时验证必填 */
function handleTextBlur(e: FocusEvent) {
  const target = e.target as HTMLInputElement;
  const rawValue = target?.value ?? '';

  if (rawValue !== props.entry[fullKey.value]) {
    props.entry[fullKey.value] = rawValue;
  }

  emit('blur');
  emit('fieldChange');
}

/** 多行文本失焦时验证必填 */
function handleTextareaBlur(e: FocusEvent) {
  const target = e.target as HTMLInputElement;
  const rawValue = target?.value ?? '';

  if (rawValue !== props.entry[fullKey.value]) {
    props.entry[fullKey.value] = rawValue;
  }

  emit('blur');
  emit('fieldChange');
}

/** 选择类字段的变化处理 */
function handleSelectChange(value: any) {
  updateValue(value);
  emit('blur');
}

/** 单选类型的变化处理（需要触发 blur 以触发逻辑验证） */
function handleRadioChange(value: string) {
  updateValue(value);
  emit('blur');
  emit('fieldChange');
}

/** 判断是否为数组类型（复选框/多选） */
function isArrayField(): boolean {
  return props.field.fieldType === 'checkbox' || props.field.fieldType === 'multiSelect';
}

/** 获取字段值（支持数组类型） */
function getValue(): any {
  const val = fieldValue.value;
  return isArrayField() ? (val || []) : val;
}

/** 当前字段的上期参考值（按 entryIndex 取对应条目） */
const fieldLastValue = computed(() => {
  if (
    props.entryIndex !== undefined &&
    props.containerLastValues &&
    props.entryIndex < props.containerLastValues.length
  ) {
    const entry = props.containerLastValues[props.entryIndex];
    // 使用 fullKey（rowKey+fieldCode）精确匹配，与 containerLastPeriodValues 的 fieldValues key 对应
    const raw = entry?.fieldValues?.[fullKey.value];
    if (!raw) return null;
    const { fieldType, options } = props.field;
    if (!options || options.length === 0) return raw;
    if (fieldType === 'checkbox' || fieldType === 'multiSelect') {
      const parts = raw.split(',');
      const labels = parts.map((v) => {
        const opt = options.find((o) => String(o.value) === v.trim());
        return opt ? opt.label : v;
      });
      return labels.join('、');
    } else {
      const opt = options.find((o) => String(o.value) === raw);
      return opt ? opt.label : raw;
    }
  }
  return null;
});

/** 控件区是否有上期值 */
const hasLastValue = computed(() => !!fieldLastValue.value);

/** 控件区 class（垂直堆叠：控件 + 上期值独立一行） */
const controlAreaClass = computed(() => ({
  'field-control-row': true,
  'field-control-row--has-last': hasLastValue.value,
}));
</script>

<template>
  <div
    class="field-row"
    :data-container-field-key="fullKey"
  >
    <div class="field-control-col">
      <div class="field-label-row">{{ fieldLabel }}<span v-if="field.required" class="text-red-500">*</span></div>
      <!-- 文本类型 -->
      <a-input
        v-if="field.fieldType === 'text'"
        class="cf-control"
        :value="fieldValue"
        :disabled="disabled"
        :placeholder="fieldPlaceholder"
        :maxlength="field.maxLength"
        @update:value="updateValue"
        @blur="handleTextBlur"
      />

      <!-- 数字类型 -->
      <SafeNumberInput
        v-else-if="field.fieldType === 'number'"
        class="cf-control cf-number number-input-auto-width"
        :model-value="fieldValue"
        :disabled="disabled"
        :min="field.minValue ?? 0"
        :precision="field.precision"
        :prefix="field.prefix"
        :suffix="field.suffix"
        :placeholder="fieldPlaceholder"
        @update:model-value="updateValue"
        @blur="handleBlur"
      />

      <!-- 多行文本类型 -->
      <a-textarea
        v-else-if="field.fieldType === 'textarea'"
        class="cf-control"
        :value="fieldValue"
        :disabled="disabled"
        :placeholder="fieldPlaceholder"
        :rows="field.rows || 3"
        :maxlength="field.maxLength"
        @update:value="updateValue"
        @blur="handleTextareaBlur"
      />

      <!-- 单选类型 -->
      <div
        v-else-if="field.fieldType === 'radio'"
        class="radio-group-wrapper"
      >
        <a-radio
          v-for="opt in field.options"
          :key="String(opt.value)"
          :value="String(opt.value)"
          :checked="String(fieldValue ?? '') === String(opt.value)"
          :disabled="disabled"
          @change="handleRadioChange(String(opt.value))"
        >
          {{ opt.label }}
        </a-radio>
      </div>

      <!-- 复选框类型 -->
      <div
        v-else-if="field.fieldType === 'checkbox'"
        class="checkbox-group-wrapper"
      >
        <a-checkbox-group
          :model-value="getValue()"
          :disabled="disabled"
          @update:model-value="(v) => updateValue(v)"
          @change="(v) => handleSelectChange(v)"
        >
          <a-checkbox
            v-for="opt in field.options"
            :key="opt.value"
            :value="opt.value"
          >
            {{ opt.label }}
          </a-checkbox>
        </a-checkbox-group>
      </div>

      <!-- 下拉单选类型 -->
      <a-select
        v-else-if="field.fieldType === 'select'"
        class="cf-control"
        :model-value="fieldValue"
        :disabled="disabled"
        :placeholder="`请选择${fieldLabel}`"
        :show-search="field.showSearch"
        allow-clear
        @update:model-value="(v) => updateValue(v)"
        @change="(v) => handleSelectChange(v)"
      >
        <a-select-option
          v-for="opt in field.options"
          :key="opt.value"
          :value="opt.value"
        >
          {{ opt.label }}
        </a-select-option>
      </a-select>

      <!-- 下拉多选类型 -->
      <a-select
        v-else-if="field.fieldType === 'multiSelect'"
        class="cf-control"
        :model-value="getValue()"
        :disabled="disabled"
        :placeholder="`请选择${fieldLabel}`"
        mode="multiple"
        allow-clear
        @update:model-value="(v) => updateValue(v)"
        @change="(v) => handleSelectChange(v)"
      >
        <a-select-option
          v-for="opt in field.options"
          :key="opt.value"
          :value="opt.value"
        >
          {{ opt.label }}
        </a-select-option>
      </a-select>

      <!-- 日期类型 -->
      <a-date-picker
        v-else-if="field.fieldType === 'date'"
        class="cf-control"
        :model-value="fieldValue ? dayjs(fieldValue) : null"
        :disabled="disabled"
        :show-time="field.format?.includes('HH')"
        :format="field.format || 'YYYY-MM-DD'"
        @update:model-value="(val: any) => updateValue(val)"
        @change="handleSelectChange"
      />

      <!-- 日期区间类型 -->
      <a-range-picker
        v-else-if="field.fieldType === 'dateRange'"
        class="cf-control"
        :model-value="fieldValue"
        :disabled="disabled"
        :show-time="field.format?.includes('HH')"
        :format="field.format || 'YYYY-MM-DD'"
        @update:model-value="updateValue"
        @change="handleSelectChange"
      />

      <!-- 开关类型 -->
      <a-switch
        v-else-if="field.fieldType === 'boolean'"
        class="cf-control"
        :checked="fieldValue"
        :disabled="disabled"
        @update:checked="updateValue"
        @change="handleSelectChange"
      />

      <!-- 不支持的类型 -->
      <span v-else class="text-gray-400 text-sm">不支持的类型: {{ field.fieldType }}</span>

      <!-- 上期参考值：显示在控件下方，与普通字段上期值样式一致 -->
      <div
        v-if="fieldLastValue"
        class="inline-last-value"
      >
        <span class="last-value-prefix">上期：</span>
        <span class="last-value-text">{{ fieldLastValue }}</span>
      </div>
    </div>

    <!-- 错误提示 -->
    <div
      v-if="containerFieldError"
      class="indicator-error"
    >
      {{ containerFieldError }}
    </div>
  </div>
</template>

<style scoped>
/* 容器内字段：垂直堆叠（标签行 → 控件行 → 上期值行 → 错误提示行） */
.field-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

/* 控件列：垂直堆叠 */
.field-control-col {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
  min-width: 200px;
}

.field-label-row {
  font-size: 14px;
  color: hsl(var(--foreground));
  font-weight: 500;
  line-height: 1.4;
  margin-bottom: 2px;
}

.radio-group-wrapper,
.checkbox-group-wrapper {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
}

.indicator-error {
  color: hsl(var(--destructive));
  font-size: 12.5px;
  padding: 4px 8px;
  background: hsl(var(--destructive) / 0.06);
  border: 1px solid hsl(var(--destructive) / 0.2);
  border-radius: 4px;
  line-height: 1.4;
  word-wrap: break-word;
  width: fit-content;
  max-width: 100%;
}

.text-red-500 {
  color: hsl(var(--destructive));
  font-weight: 600;
}

/* 上期值样式：与 index.vue 普通字段上期值一致 */
.inline-last-value {
  font-size: 14px;
  color: hsl(var(--muted-foreground));
  white-space: nowrap;
  margin: 4px 0px;
}

.last-value-prefix {
  font-weight: 600;
  color: hsl(var(--muted-foreground));
}

.last-value-text {
  color: hsl(var(--success));
  font-weight: 500;
}
</style>
