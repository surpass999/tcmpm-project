<script setup lang="ts">
/**
 * 文本输入组件
 * 使用 Vue 3 v-model 语法
 */

import { computed } from 'vue';

interface Props {
  modelValue?: string;
  disabled?: boolean;
  placeholder?: string;
  maxlength?: number;
  showCount?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  disabled: false,
  placeholder: '请输入文本',
  showCount: true,
});

const emit = defineEmits<{
  'update:modelValue': [value: string];
  'change': [value: string];
}>();

function handleChange(e: Event) {
  const value = (e.target as HTMLInputElement).value;
  emit('change', value);
}
</script>

<template>
  <a-input
    :model-value="modelValue"
    :disabled="disabled"
    :placeholder="placeholder"
    :maxlength="maxlength"
    :show-count="showCount && !!maxlength"
    class="w-full"
    @update:model-value="emit('update:modelValue', $event)"
    @change="handleChange"
  />
</template>
