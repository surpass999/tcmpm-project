<script setup lang="ts">
/**
 * 富文本/多行文本输入组件
 * 使用 Vue 3 v-model 语法
 */

interface Props {
  modelValue?: string;
  disabled?: boolean;
  placeholder?: string;
  rows?: number;
  maxlength?: number;
  showCount?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  disabled: false,
  placeholder: '请输入文本',
  rows: 3,
  showCount: false,
});

const emit = defineEmits<{
  'update:modelValue': [value: string];
  'change': [value: string];
}>();

function handleChange(e: Event) {
  const value = (e.target as HTMLTextAreaElement).value;
  emit('change', value);
}
</script>

<template>
  <a-textarea
    :model-value="modelValue"
    :disabled="disabled"
    :placeholder="placeholder"
    :rows="rows"
    :maxlength="maxlength"
    :show-count="showCount && !!maxlength"
    class="w-full"
    @update:model-value="emit('update:modelValue', $event)"
    @change="handleChange"
  />
</template>
