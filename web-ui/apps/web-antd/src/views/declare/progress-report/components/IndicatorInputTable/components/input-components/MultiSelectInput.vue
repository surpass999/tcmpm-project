<script setup lang="ts">
/**
 * 下拉多选输入组件
 * 使用 Vue 3 v-model 语法
 */

interface Option {
  value: string;
  label: string;
}

interface Props {
  modelValue?: string[];
  disabled?: boolean;
  placeholder?: string;
  options?: Option[];
  showSearch?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => [],
  disabled: false,
  placeholder: '请选择',
  options: () => [],
  showSearch: false,
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
  <a-select
    :model-value="modelValue"
    :disabled="disabled"
    :placeholder="placeholder"
    mode="multiple"
    :show-search="showSearch"
    allow-clear
    class="w-full"
    @update:model-value="emit('update:modelValue', $event)"
    @change="handleChange"
  >
    <a-select-option
      v-for="opt in options"
      :key="opt.value"
      :value="opt.value"
    >
      {{ opt.label }}
    </a-select-option>
  </a-select>
</template>
