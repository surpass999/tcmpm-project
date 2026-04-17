<script setup lang="ts">
/**
 * InputTypeRadioGroup - 单选输入型
 *
 * VBenForm 规范：value + update:value
 * - 解析 options，获取输入型选项（value >= 1000）
 * - 选中输入型选项后右侧显示输入框
 * - 序列化格式："1001∵用户输入内容"
 */
import { computed, ref } from 'vue';
import { Radio, RadioGroup, Input } from 'ant-design-vue';

interface Option {
  value: string;
  label: string;
  exclusive?: boolean;
  inputType?: boolean;
}

interface Props {
  value?: string | null;
  options?: Option[];
  disabled?: boolean;
  placeholder?: string;
  lastPeriodContents?: Record<string, string>;
}

const props = withDefaults(defineProps<Props>(), {
  value: null,
  options: () => [],
  disabled: false,
  placeholder: undefined,
  lastPeriodContents: () => ({}),
});

const emit = defineEmits<{
  'update:value': [value: string];
  change: [value: string];
}>();

const inputContents = ref<Record<string, string>>({});

const selectedPureValue = computed(() => {
  if (!props.value) return '';
  const idx = props.value.indexOf('∵');
  return idx >= 0 ? props.value.substring(0, idx) : props.value;
});

function isInputType(opt: Option): boolean {
  return !!opt.inputType || Number(opt.value) >= 1000;
}

function getInputContent(optValue: string): string {
  if (props.value?.startsWith(optValue + '∵')) {
    return props.value.substring(optValue.length + 1);
  }
  return inputContents.value[optValue] || '';
}

function handleGroupChange(val: string) {
  let newValue: string;
  const opt = props.options?.find((o) => o.value === val);
  if (!opt) { newValue = val; }
  else if (isInputType(opt)) {
    const existingContent = inputContents.value[opt.value] || '';
    newValue = existingContent ? `${opt.value}∵${existingContent}` : opt.value;
  } else {
    newValue = opt.value;
  }
  emit('update:value', newValue);
  emit('change', newValue);
}

function handleInputChange(opt: Option, e: Event) {
  const content = (e.target as HTMLInputElement).value;
  inputContents.value[opt.value] = content;
  if (selectedPureValue.value === opt.value) {
    const newValue = content ? `${opt.value}∵${content}` : opt.value;
    emit('update:value', newValue);
    emit('change', newValue);
  }
}
</script>

<template>
  <RadioGroup :value="selectedPureValue" :disabled="disabled" @change="(e: any) => handleGroupChange(e.target.value)">
    <template v-for="opt in options" :key="opt.value">
      <Radio :value="opt.value">{{ opt.label }}</Radio>
      <Input
        v-if="isInputType(opt) && selectedPureValue === opt.value"
        :value="getInputContent(opt.value)"
        :disabled="disabled"
        :placeholder="lastPeriodContents?.[opt.value] ? '上期：' + lastPeriodContents[opt.value] : (placeholder || '请输入')"
        style="display: inline-block; width: 200px; padding: 0 0 0 4px; vertical-align: middle;"
        @input="(e: Event) => handleInputChange(opt, e)"
      />
    </template>
  </RadioGroup>
</template>
