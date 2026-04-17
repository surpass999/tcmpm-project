<script setup lang="ts">
/**
 * VbenSwitch - 开关组件
 *
 * VBenForm 规范：baseModelPropName/value+update:value
 */
import { Switch } from 'ant-design-vue';

interface Props {
  value?: boolean;
  disabled?: boolean;
  checkedChildren?: string;
  unCheckedChildren?: string;
  class?: string;
}

const props = withDefaults(defineProps<Props>(), {
  value: false,
  disabled: false,
  checkedChildren: undefined,
  unCheckedChildren: undefined,
  class: '',
});

const emit = defineEmits<{
  'update:value': [value: boolean];
  change: [value: boolean];
}>();

function handleChange(checked: boolean) {
  emit('update:value', checked);
  emit('change', checked);
}
</script>

<template>
  <Switch
    :checked="value"
    :disabled="disabled"
    :checked-children="checkedChildren"
    :un-checked-children="unCheckedChildren"
    :class="class"
    @change="handleChange"
  />
</template>
