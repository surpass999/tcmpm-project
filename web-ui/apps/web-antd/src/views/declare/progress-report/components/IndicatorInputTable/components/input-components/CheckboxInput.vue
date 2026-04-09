<script setup lang="ts">
/**
 * 多选输入组件（使用 checkbox-group）
 * 使用 Vue 3 v-model 语法
 */

interface Option {
  value: string;
  label: string;
}

interface Props {
  modelValue?: string[];
  disabled?: boolean;
  options?: Option[];
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => [],
  disabled: false,
  options: () => [],
});

const emit = defineEmits<{
  'update:modelValue': [value: string[]];
  'change': [value: string[]];
}>();

function handleChange(values: string[]) {
  emit('change', values);
}
</script>

<template>
  <a-checkbox-group
    :model-value="modelValue"
    :disabled="disabled"
    class="flex flex-wrap gap-x-4 gap-y-2"
    @update:model-value="emit('update:modelValue', $event)"
    @change="handleChange"
  >
    <a-checkbox
      v-for="opt in options"
      :key="opt.value"
      :value="opt.value"
    >
      {{ opt.label }}
    </a-checkbox>
  </a-checkbox-group>
</template>
