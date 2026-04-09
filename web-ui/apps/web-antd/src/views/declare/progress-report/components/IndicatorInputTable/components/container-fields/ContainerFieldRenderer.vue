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
  showError?: boolean;
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
  // 从 DOM 原生 input 元素读取当前值
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

/** 选择类字段的变化处理 */
function handleSelectChange(value: any) {
  updateValue(value);
  emit('blur');
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
    <span class="dynamic-field-label">
      {{ field.fieldLabel }}
      <span v-if="field.required" class="text-red-500">*</span>
    </span>

    <!-- 文本类型 -->
    <a-input
      v-if="field.fieldType === 'text'"
      :value="fieldValue"
      :disabled="disabled"
      :placeholder="field.placeholder || `请输入${field.fieldLabel}`"
      :maxlength="field.maxLength"
      class="w-full"
      @update:value="updateValue"
      @change="() => emit('fieldChange')"
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
      @update:model-value="updateValue"
      @blur="handleBlur"
    />

    <!-- 多行文本类型 -->
    <a-textarea
      v-else-if="field.fieldType === 'textarea'"
      :value="fieldValue"
      :disabled="disabled"
      :placeholder="field.placeholder || `请输入${field.fieldLabel}`"
      :rows="field.rows || 3"
      :maxlength="field.maxLength"
      class="w-full"
      @update:value="updateValue"
      @change="() => emit('fieldChange')"
    />

    <!-- 单选类型 -->
    <div
      v-else-if="field.fieldType === 'radio'"
      class="flex flex-wrap gap-x-4 gap-y-2"
    >
      <a-radio
        v-for="opt in field.options"
        :key="String(opt.value)"
        :value="String(opt.value)"
        :checked="String(fieldValue ?? '') === String(opt.value)"
        :disabled="disabled"
        @change="updateValue(String(opt.value))"
      >
        {{ opt.label }}
      </a-radio>
    </div>

    <!-- 复选框类型 -->
    <a-checkbox-group
      v-else-if="field.fieldType === 'checkbox'"
      :model-value="getValue()"
      :disabled="disabled"
      class="flex flex-wrap gap-x-4 gap-y-2"
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

    <!-- 下拉单选类型 -->
    <a-select
      v-else-if="field.fieldType === 'select'"
      :model-value="fieldValue"
      :disabled="disabled"
      :placeholder="`请选择${field.fieldLabel}`"
      :show-search="field.showSearch"
      allow-clear
      class="w-full"
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
      :placeholder="`请选择${field.fieldLabel}`"
      mode="multiple"
      allow-clear
      class="w-full"
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
      class="w-full"
      @update:model-value="(val) => updateValue(val)"
      @change="handleSelectChange"
    />

    <!-- 日期区间类型 -->
    <a-range-picker
      v-else-if="field.fieldType === 'dateRange'"
      :model-value="fieldValue"
      :disabled="disabled"
      :show-time="field.format?.includes('HH')"
      :format="field.format || 'YYYY-MM-DD'"
      class="w-full"
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
      v-if="showError && containerFieldError"
      class="indicator-error"
    >
      {{ containerFieldError }}
    </div>
  </div>
</template>

<style scoped>
.dynamic-field-item {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  flex-wrap: wrap;
}

.dynamic-field-label {
  min-width: 120px;
  line-height: 32px;
  font-size: 14px;
  color: hsl(var(--foreground));
  flex-shrink: 0;
}

.dynamic-field-item .w-full {
  flex: 1;
  min-width: 160px;
}

.number-input-auto-width {
  width: auto !important;
  max-width: 16ch;
  min-width: 160px;
}

.indicator-error {
  display: flex;
  align-items: center;
  gap: 6px;
  color: hsl(var(--destructive));
  font-size: 12.5px;
  margin-top: 6px;
  padding: 6px 10px;
  background: hsl(var(--destructive) / 0.06);
  border: 1px solid hsl(var(--destructive) / 0.2);
  border-radius: 6px;
  line-height: 1.4;
  min-width: 160px;
  width: 100%;
}

.text-red-500 {
  color: hsl(var(--destructive));
  font-weight: 600;
}
</style>
