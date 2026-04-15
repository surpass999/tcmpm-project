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
  let normalizedValue: any;

  if (props.field.fieldType === 'number') {
    const trimmed = rawValue.trim();
    normalizedValue = trimmed === '' ? null : Number(trimmed);
  } else {
    normalizedValue = rawValue;
  }

  if (normalizedValue !== props.entry[fullKey.value]) {
    props.entry[fullKey.value] = normalizedValue;
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
</script>

<template>
  <div
    class="dynamic-field-item"
    :data-container-field-key="fullKey"
  >
    <!-- 标签 -->
    <span class="dynamic-field-label">
      {{ fieldLabel }}
      <span v-if="field.required" class="text-red-500">*</span>
    </span>

    <!-- 输入控件 -->
    <!-- 文本类型 -->
    <a-input
      v-if="field.fieldType === 'text'"
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
      :model-value="fieldValue"
      :disabled="disabled"
      :min="field.minValue ?? 0"
      :precision="field.precision"
      :prefix="field.prefix"
      :suffix="field.suffix"
      class="number-input-auto-width"
      :placeholder="fieldPlaceholder"
      @update:model-value="updateValue"
      @blur="handleBlur"
    />

    <!-- 多行文本类型 -->
    <a-textarea
      v-else-if="field.fieldType === 'textarea'"
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
        @update:model-value="updateValue"
        @change="handleSelectChange"
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
      :model-value="fieldValue"
      :disabled="disabled"
      :placeholder="`请选择${fieldLabel}`"
      :show-search="field.showSearch"
      allow-clear
      @update:model-value="updateValue"
      @change="handleSelectChange"
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
      :model-value="getValue()"
      :disabled="disabled"
      :placeholder="`请选择${fieldLabel}`"
      mode="multiple"
      allow-clear
      @update:model-value="updateValue"
      @change="handleSelectChange"
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
      :checked="fieldValue"
      :disabled="disabled"
      @update:checked="updateValue"
      @change="handleSelectChange"
    />

    <!-- 不支持的类型 -->
    <span v-else class="text-gray-400 text-sm">不支持的类型: {{ field.fieldType }}</span>

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
.dynamic-field-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.dynamic-field-label {
  line-height: 1.5;
  font-size: 14px;
  color: hsl(var(--foreground));
}

.dynamic-field-item :deep(.ant-input),
.dynamic-field-item :deep(.ant-input-number),
.dynamic-field-item :deep(.ant-picker),
.dynamic-field-item :deep(.ant-select),
.dynamic-field-item :deep(.ant-input-textarea),
.dynamic-field-item :deep(.ant-switch) {
  width: 100%;
}

/* 数字输入框：内容撑开，最大 8 个字符宽度（与外层保持一致） */
.number-input-auto-width {
  width: auto;
  max-width: 8ch;
  min-width: 160px;
}

/* 容器内 SafeNumberInput 的输入框本身也要跟随外层宽度 */
.number-input-auto-width :deep(.safe-number-input) {
  width: auto !important;
  max-width: 8ch !important;
  min-width: 160px !important;
}

.radio-group-wrapper,
.checkbox-group-wrapper {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  padding-top: 4px;
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
</style>
