<script setup lang="ts">
/**
 * VbenMultiSelect - 多选下拉组件
 *
 * VBenForm 规范：value+update:value
 */
import { computed } from 'vue';
import { Select } from 'ant-design-vue';

interface Option {
  label: string;
  value: string | number;
}

interface Props {
  value?: (string | number)[];
  options?: Option[];
  disabled?: boolean;
  placeholder?: string;
  showSearch?: boolean;
  allowClear?: boolean;
  class?: string;
}

const props = withDefaults(defineProps<Props>(), {
  value: () => [],
  options: () => [],
  disabled: false,
  placeholder: undefined,
  showSearch: false,
  allowClear: true,
  class: '',
});

const emit = defineEmits<{
  'update:value': [value: (string | number)[]];
  change: [value: (string | number)[]];
}>();

function handleChange(selectedValues: (string | number)[]) {
  emit('update:value', selectedValues);
  emit('change', selectedValues);
}
</script>

<template>
  <Select
    :value="value"
    :disabled="disabled"
    :placeholder="placeholder"
    :show-search="showSearch"
    :allow-clear="allowClear"
    mode="multiple"
    :options="options"
    :class="class"
    @change="handleChange"
  />
</template>
