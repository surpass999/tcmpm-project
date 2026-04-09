<script setup lang="ts">
/**
 * 日期选择组件
 * 使用 Vue 3 v-model 语法
 */

import type { Dayjs } from 'dayjs';

interface Props {
  modelValue?: Dayjs | null;
  disabled?: boolean;
  placeholder?: string;
  format?: string;
  showTime?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  disabled: false,
  format: 'YYYY-MM-DD',
});

const emit = defineEmits<{
  'update:modelValue': [value: Dayjs | null];
  'change': [value: Dayjs | null];
}>();

function handleChange(value: Dayjs | null) {
  emit('change', value);
}
</script>

<template>
  <a-date-picker
    :model-value="modelValue"
    :disabled="disabled"
    :placeholder="placeholder || '请选择日期'"
    :format="format"
    :show-time="showTime"
    class="w-full"
    @update:model-value="emit('update:modelValue', $event)"
    @change="handleChange"
  />
</template>
