<script setup lang="ts">
/**
 * 开关输入组件
 * 使用 Vue 3 v-model 语法
 */

interface Props {
  modelValue?: boolean;
  disabled?: boolean;
  trueLabel?: string;
  falseLabel?: string;
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: false,
  disabled: false,
  trueLabel: '是',
  falseLabel: '否',
});

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  'change': [value: boolean];
}>();

function handleChange(checked: boolean) {
  emit('change', checked);
}
</script>

<template>
  <a-switch
    :checked="modelValue"
    :disabled="disabled"
    :checked-children="trueLabel"
    :un-checked-children="falseLabel"
    class="switch-auto-width"
    @update:checked="emit('update:modelValue', $event)"
    @change="handleChange"
  />
</template>
