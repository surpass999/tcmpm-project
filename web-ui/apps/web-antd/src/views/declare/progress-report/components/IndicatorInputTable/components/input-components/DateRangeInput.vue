<script setup lang="ts">
/**
 * 日期区间输入组件
 * 使用 Vue 3 v-model 语法
 */

import type { Dayjs } from 'dayjs';

interface Props {
  modelValue?: [Dayjs | null, Dayjs | null] | null;
  disabled?: boolean;
  format?: string;
  showTime?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  disabled: false,
  format: 'YYYY-MM-DD',
});

const emit = defineEmits<{
  'update:modelValue': [value: [Dayjs | null, Dayjs | null] | null];
  'change': [value: [Dayjs | null, Dayjs | null] | null];
}>();

function handleChange(value: [Dayjs, Dayjs] | null) {
  emit('change', value);
}
</script>

<template>
  <a-range-picker
    :model-value="modelValue"
    :disabled="disabled"
    :format="format"
    :show-time="showTime"
    class="w-full"
    @update:model-value="emit('update:modelValue', $event)"
    @change="handleChange"
  />
</template>
