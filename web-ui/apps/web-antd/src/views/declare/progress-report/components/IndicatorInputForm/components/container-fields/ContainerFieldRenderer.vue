<script setup lang="ts">
/**
 * ContainerFieldRenderer - 容器内字段统一渲染器
 * 支持容器内部所有字段类型的渲染
 */
import { computed } from 'vue';
import { Input, Select, Checkbox, DatePicker, Switch } from 'ant-design-vue';
import { SafeNumberInput } from '#/components/safe-number-input';
import type { DynamicField } from '../../types';

interface ContainerEntryLastValue { rowKey: string; fieldValues: Record<string, string>; }

interface Props {
  entry: any;
  indicatorCode: string;
  rowKey: string;
  field: DynamicField;
  disabled?: boolean;
  containerFieldError?: string;
  entryIndex?: number;
  containerLastValues?: ContainerEntryLastValue[];
}

const props = withDefaults(defineProps<Props>(), {
  disabled: false,
  containerFieldError: undefined,
  entryIndex: 0,
  containerLastValues: () => [],
});

const emit = defineEmits<{ 'update': [value: any]; 'blur': []; 'fieldChange': [] }>();

const fullKey = computed(() => `${props.rowKey}${props.field.fieldCode}`);

const fieldValue = computed(() => props.entry?.[fullKey.value]);

const lastValueForField = computed(() => {
  for (const lv of props.containerLastValues || []) {
    if (lv.fieldValues[props.field.fieldCode]) return lv.fieldValues[props.field.fieldCode];
    if (lv.fieldValues[fullKey.value]) return lv.fieldValues[fullKey.value];
  }
  return undefined;
});

function updateValue(value: any) {
  props.entry[fullKey.value] = value;
  emit('update', value);
  emit('fieldChange');
}

function handleInputChange(e: any) {
  updateValue(e.target.value);
}
function handleSelectChange(val: any) { updateValue(val); }
function handleDateChange(val: any) { updateValue(val); }
function handleSwitchChange(val: boolean) { updateValue(val); }
function handleNumberChange(val: any) { updateValue(val ?? null); }
function handleCheckboxChange(val: any) { updateValue(val); }
function handleTextareaChange(e: any) { updateValue(e.target.value); }

function handleInputBlur() { emit('blur'); emit('fieldChange'); }

const showConditionMet = computed(() => {
  if (!props.field.showCondition) return true;
  const cond = props.field.showCondition;
  const watchKey = `${props.rowKey}${cond.watchField}`;
  const watchVal = props.entry?.[watchKey];
  switch (cond.operator) {
    case 'eq': return watchVal == cond.value;
    case 'neq': return watchVal != cond.value;
    case 'gt': return Number(watchVal) > Number(cond.value);
    case 'gte': return Number(watchVal) >= Number(cond.value);
    case 'lt': return Number(watchVal) < Number(cond.value);
    case 'lte': return Number(watchVal) <= Number(cond.value);
    case 'in': return Array.isArray(cond.value) && cond.value.includes(watchVal);
    case 'notEmpty': return watchVal !== undefined && watchVal !== null && watchVal !== '';
    case 'isEmpty': return watchVal === undefined || watchVal === null || watchVal === '';
    default: return true;
  }
});

const precision = computed(() => props.field.precision ?? props.field.maxValue);
const showCount = computed(() => (props.field.maxLength ?? 0) > 0);
</script>

<template>
  <div
    v-if="showConditionMet"
    :data-container-field-key="fullKey"
    class="container-field-wrapper"
    :class="{ 'has-error': !!containerFieldError }"
  >
    <!-- 数字类型 -->
    <template v-if="field.fieldType === 'number'">
      <SafeNumberInput
        :model-value="fieldValue"
        :disabled="disabled"
        :min="field.minValue"
        :max="field.maxValue"
        :precision="precision"
        :suffix="field.suffix"
        :placeholder="field.placeholder || `请输入数字`"
        :error-msg="containerFieldError"
        class="number-input-auto-width"
        @update:model-value="handleNumberChange"
        @blur="handleInputBlur"
      />
    </template>

    <!-- 文本类型 -->
    <template v-else-if="field.fieldType === 'text'">
      <Input
        :value="fieldValue"
        :disabled="disabled"
        :placeholder="field.placeholder || `请输入`"
        :maxlength="field.maxLength"
        :show-count="showCount"
        @input="handleInputChange"
        @blur="handleInputBlur"
      />
    </template>

    <!-- 多行文本 -->
    <template v-else-if="field.fieldType === 'textarea'">
      <Input.TextArea
        :value="fieldValue"
        :disabled="disabled"
        :placeholder="field.placeholder || `请输入`"
        :rows="field.rows || 3"
        :maxlength="field.maxLength"
        :show-count="showCount"
        @input="handleTextareaChange"
        @blur="handleInputBlur"
      />
    </template>

    <!-- 单选类型 -->
    <template v-else-if="field.fieldType === 'radio'">
      <Select
        :value="fieldValue"
        :disabled="disabled"
        :placeholder="field.placeholder || `请选择`"
        :options="field.options"
        allow-clear
        @change="handleSelectChange"
      />
    </template>

    <!-- 多选类型 -->
    <template v-else-if="field.fieldType === 'checkbox'">
      <Checkbox.Group
        :value="fieldValue || []"
        :disabled="disabled"
        :options="field.options"
        @change="handleCheckboxChange"
      />
    </template>

    <!-- 日期类型 -->
    <template v-else-if="field.fieldType === 'date'">
      <DatePicker
        :value="fieldValue"
        :disabled="disabled"
        :format="field.format || 'YYYY-MM-DD'"
        :show-time="(field.format || '').includes('HH')"
        style="width: 100%"
        @change="handleDateChange"
      />
    </template>

    <!-- 日期区间 -->
    <template v-else-if="field.fieldType === 'dateRange'">
      <DatePicker.RangePicker
        :value="fieldValue"
        :disabled="disabled"
        :format="field.format || 'YYYY-MM-DD'"
        :show-time="(field.format || '').includes('HH')"
        style="width: 100%"
        @change="handleDateChange"
      />
    </template>

    <!-- 布尔类型 -->
    <template v-else-if="field.fieldType === 'boolean'">
      <Switch
        :checked="fieldValue"
        :disabled="disabled"
        @change="(val: any) => handleSwitchChange(!!val)"
      />
    </template>

    <!-- 未知类型兜底 -->
    <template v-else>
      <Input
        :value="fieldValue"
        :disabled="disabled"
        :placeholder="field.placeholder || `请输入`"
        @input="handleInputChange"
        @blur="handleInputBlur"
      />
    </template>

    <!-- 上期值提示 -->
    <div v-if="lastValueForField" class="field-last-value">
      上期：{{ lastValueForField }}
    </div>
  </div>
</template>

<style scoped>
.container-field-wrapper {
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
}
.container-field-wrapper.has-error :deep(.ant-input),
.container-field-wrapper.has-error :deep(.ant-select-selector) {
  border-color: hsl(var(--destructive));
}
.field-last-value {
  font-size: 12px;
  color: hsl(var(--muted-foreground));
}
.number-input-auto-width {
  width: auto;
  max-width: 8ch;
  min-width: 160px;
}
</style>
