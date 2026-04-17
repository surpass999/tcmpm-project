<script setup lang="ts">
/**
 * InputTypeCheckboxGroup - 多选输入型
 *
 * VBenForm 规范：value + update:value
 * - value: string[]（多个选项的序列化值数组）
 * - 互斥逻辑在组件内部处理
 * - 输入型选项（value >= 1000）选中后显示输入框
 */
import { computed, ref } from 'vue';
import { Checkbox, Input } from 'ant-design-vue';

interface Option {
  value: string;
  label: string;
  exclusive?: boolean;
  inputType?: boolean;
}

interface Props {
  value?: string[];
  options?: Option[];
  disabled?: boolean;
  placeholder?: string;
  lastPeriodContents?: Record<string, string>;
}

const props = withDefaults(defineProps<Props>(), {
  value: () => [],
  options: () => [],
  disabled: false,
  placeholder: undefined,
  lastPeriodContents: () => ({}),
});

const emit = defineEmits<{
  'update:value': [value: string[]];
  change: [value: string[]];
}>();

const inputContents = ref<Record<string, string>>({});

/** 纯选项值数组（去掉 ∵ 内容） */
const pureValues = computed<string[]>(() => {
  return (props.value || []).map((v) => {
    const idx = v.indexOf('∵');
    return idx >= 0 ? v.substring(0, idx) : v;
  });
});

function isInputType(opt: Option): boolean {
  return !!opt.inputType || Number(opt.value) >= 1000;
}

function isSelected(optValue: string): boolean {
  return pureValues.value.includes(optValue);
}

function getInputContent(optValue: string): string {
  const found = (props.value || []).find((v) => v.startsWith(optValue + '∵'));
  if (found) return found.substring(optValue.length + 1);
  return inputContents.value[optValue] || '';
}

function handleCheckboxChange(opt: Option, checked: boolean) {
  const exclusiveValues = new Set(
    (props.options || []).filter((o) => o.exclusive).map((o) => o.value),
  );

  let newPure: string[];
  if (opt.exclusive) {
    newPure = checked ? [opt.value] : [];
  } else {
    if (checked) {
      newPure = [...pureValues.value.filter((v) => !exclusiveValues.has(v)), opt.value];
    } else {
      newPure = pureValues.value.filter((v) => v !== opt.value);
    }
  }

  const serialized = newPure.map((v) => {
    const content = inputContents.value[v];
    return content ? `${v}∵${content}` : v;
  });

  emit('update:value', serialized);
  emit('change', serialized);
}

function handleInputChange(opt: Option, e: Event) {
  const content = (e.target as HTMLInputElement).value;
  inputContents.value[opt.value] = content;

  if (isSelected(opt.value)) {
    const newPure = pureValues.value.filter((v) => v !== opt.value);
    newPure.push(opt.value);
    const serialized = newPure.map((v) => {
      const c = v === opt.value ? content : inputContents.value[v];
      return c ? `${v}∵${c}` : v;
    });
    emit('update:value', serialized);
    emit('change', serialized);
  }
}
</script>

<template>
  <div class="input-type-checkbox-group">
    <template v-for="opt in options" :key="opt.value">
      <Checkbox
        :checked="isSelected(opt.value)"
        :disabled="disabled"
        @change="(e: any) => handleCheckboxChange(opt, e.target.checked)"
      >
        {{ opt.label }}
      </Checkbox>
      <Input
        v-if="isInputType(opt) && isSelected(opt.value)"
        :value="getInputContent(opt.value)"
        :disabled="disabled"
        :placeholder="lastPeriodContents?.[opt.value] ? '上期：' + lastPeriodContents[opt.value] : (placeholder || '请输入')"
        style="display: inline-block; width: 140px; padding: 0 0 0 4px; vertical-align: middle;"
        @input="(e: Event) => handleInputChange(opt, e)"
      />
    </template>
  </div>
</template>

<style scoped>
.input-type-checkbox-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 16px;
  align-items: center;
}
</style>
