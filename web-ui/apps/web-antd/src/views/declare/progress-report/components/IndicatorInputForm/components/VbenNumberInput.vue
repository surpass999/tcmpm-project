<script setup lang="ts">
/**
 * VbenNumberInput - 数字输入组件
 *
 * VBenForm 适配器规范：使用 value/update:value（而非 modelValue）
 * 内部封装 SafeNumberInput，保留所有原有行为
 */
import { computed } from 'vue';
import { SafeNumberInput } from '#/components/safe-number-input';

interface Props {
  modelValue?: number | string | null;
  min?: number;
  max?: number;
  precision?: number;
  disabled?: boolean;
  placeholder?: string;
  prefix?: string;
  suffix?: string;
  errorMsg?: string;
  class?: string;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: null,
  min: undefined,
  precision: undefined,
  disabled: false,
  placeholder: undefined,
  prefix: undefined,
  suffix: undefined,
  errorMsg: undefined,
  class: '',
});

const emit = defineEmits<{
  'update:value': [value: number | string | null];
  blur: [event: FocusEvent];
}>();

function handleUpdate(value: number | string | null) {
  emit('update:value', value);
}

function handleBlur(event: FocusEvent) {
  emit('blur', event);
}
</script>

<template>
  <SafeNumberInput
    :model-value="modelValue"
    :min="min"
    :max="max"
    :precision="precision"
    :disabled="disabled"
    :placeholder="placeholder"
    :prefix="prefix"
    :suffix="suffix"
    :error-msg="errorMsg"
    :class="class"
    @update:model-value="handleUpdate"
    @blur="handleBlur"
  />
</template>
