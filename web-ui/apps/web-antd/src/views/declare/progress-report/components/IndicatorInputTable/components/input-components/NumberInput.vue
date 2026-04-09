<script setup lang="ts">
/**
 * 数字输入组件
 * 使用 SafeNumberInput，统一使用 Vue 3 v-model 语法
 */

import { SafeNumberInput } from '#/components/safe-number-input';

interface Props {
  modelValue?: number | string | null;
  disabled?: boolean;
  placeholder?: string;
  min?: number;
  max?: number;
  precision?: number;
  suffix?: string;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  disabled: false,
  placeholder: '请输入数字',
});

const emit = defineEmits<{
  'update:modelValue': [value: number | string | null];
  'blur': [event: FocusEvent];
}>();

function handleBlur(e: FocusEvent) {
  emit('blur', e);
}
</script>

<template>
  <SafeNumberInput
    :model-value="modelValue"
    :disabled="disabled"
    :placeholder="placeholder"
    :min="min"
    :max="max"
    :precision="precision"
    :suffix="suffix"
    class="w-full number-input-auto-width"
    @update:model-value="emit('update:modelValue', $event)"
    @blur="handleBlur"
  />
</template>
