<script setup lang="ts">
/**
 * VbenRangePicker - 日期区间选择器
 *
 * VBenForm 规范：baseModelPropName/value+update:value
 */
import { computed } from 'vue';
import { DatePicker } from 'ant-design-vue';

interface Props {
  value?: [any, any] | null;
  disabled?: boolean;
  format?: string;
  showTime?: boolean;
  class?: string;
}

const props = withDefaults(defineProps<Props>(), {
  value: undefined,
  disabled: false,
  format: undefined,
  showTime: undefined,
  class: '',
});

const emit = defineEmits<{
  'update:value': [value: [any, any] | null];
  change: [value: [any, any] | null];
}>();

const DateRangePicker = DatePicker.RangePicker;

function handleChange(value: [any, any] | null) {
  emit('update:value', value ?? null);
  emit('change', value ?? null);
}
</script>

<template>
  <DateRangePicker
    :value="value"
    :disabled="disabled"
    :format="format"
    :show-time="showTime"
    :class="class"
    @change="handleChange"
  />
</template>
