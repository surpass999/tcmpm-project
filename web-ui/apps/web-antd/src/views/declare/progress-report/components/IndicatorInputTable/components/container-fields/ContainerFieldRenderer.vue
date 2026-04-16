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
  /** 容器指标的上期值字段映射 */
  containerLastValues?: Record<string, string>;
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

/** 当前字段的上期参考值（raw value → label 格式化） */
const fieldLastValue = computed(() => {
  const raw = props.containerLastValues?.[props.field.fieldCode];
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
});

/** 控件区是否有上期值（用于条件 class） */
const hasLastValue = computed(() => !!fieldLastValue.value);

/** 控件区 class */
const controlAreaClass = computed(() => ({
  'field-control-area': true,
  'field-control-area--has-last': hasLastValue.value,
}));
</script>

<template>
  <div
    class="field-row"
    :data-container-field-key="fullKey"
  >
    <span class="dynamic-field-label">
      {{ fieldLabel }}
      <span v-if="field.required" class="text-red-500">*</span>
    </span>

    <div :class="controlAreaClass">
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
        class="cf-control cf-number"
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
        class="cf-control radio-group-wrapper"
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
        class="cf-control checkbox-group-wrapper"
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
        class="cf-control"
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
        class="cf-control"
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

      <!-- 字段级上期参考值（显示在控件右侧） -->
      <span
        v-if="fieldLastValue"
        class="field-last-value-inline"
      >
        上期：{{ fieldLastValue }}
      </span>
    </div>

    <!-- 错误提示：在 field-row 末尾，占满整行 -->
    <div
      v-if="containerFieldError"
      class="indicator-error"
      style="width: 100%;"
    >
      {{ containerFieldError }}
    </div>
  </div>
</template>

<style scoped>
/* 容器内字段同行排列：标签 + 控件区 */
.field-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  width: 100%;
}

/* 控件区：flex-grow 占据剩余空间 */
.field-control-area {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
  flex-wrap: wrap;
}

/* 无上期值 → 控件占满整行（除开关外） */
.field-control-area:not(.field-control-area--has-last) .cf-control:not(.ant-switch) {
  width: 100%;
}

/* 有上期值 → 控件宽度 auto，上期值紧跟其后（覆盖全局 width:100%!important） */
.field-control-area--has-last .cf-control {
  width: auto !important;
  flex-shrink: 0;
}

/* 有上期值 → radio/checkbox 组宽度 auto */
.field-control-area--has-last .radio-group-wrapper,
.field-control-area--has-last .checkbox-group-wrapper {
  width: auto !important;
  flex-shrink: 0;
}

.dynamic-field-label {
  font-size: 14px;
  color: hsl(var(--foreground));
  white-space: nowrap;
  flex-shrink: 0;
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

.field-last-value-inline {
  font-size: 12px;
  color: hsl(var(--success));
  opacity: 0.75;
  white-space: nowrap;
  flex-shrink: 0;
}
</style>
